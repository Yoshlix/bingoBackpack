package de.yoshlix.bingobackpack.client;

import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runs the actual PC pranks on this client. Everything here is best-effort and
 * fully wrapped in try/catch: a prank that fails must never crash the game.
 *
 * The OS-touching pranks (browser, monitor) only do anything on Windows; on
 * other platforms they quietly no-op. The fake-shutdown overlay is pure
 * in-game rendering and works everywhere.
 */
public final class PcPrankHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("bingobackpack-client");

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "bingobackpack-prank");
                t.setDaemon(true);
                return t;
            });

    // 0 while not flipped; otherwise the timestamp the flip should end at. Kept
    // as an end time rather than a plain flag so a second hit while already
    // flipped extends the window instead of racing the first hit's timer: with
    // a plain boolean, the *first* timer would still fire and revert early,
    // cutting the second hit's duration short instead of extending it.
    private static final AtomicLong flipEndTimeMillis = new AtomicLong(0);
    private static final Map<String, Integer> originalOrientations = new HashMap<>();
    private static boolean shutdownHookInstalled = false;

    private PcPrankHandler() {
    }

    /** Dispatch a payload. Always called on the client (render) thread. */
    public static void handle(PcPrankPayload payload) {
        try {
            switch (payload.action()) {
                case PcPrankPayload.ACTION_SHUTDOWN_SCREEN ->
                        Minecraft.getInstance().setScreenAndShow(new FakeShutdownScreen());
                case PcPrankPayload.ACTION_OPEN_URL -> openUrl(payload.arg());
                case PcPrankPayload.ACTION_FLIP_MONITOR -> flipMonitor(payload.durationSeconds());
                default -> LOGGER.warn("Unknown PC prank action: {}", payload.action());
            }
        } catch (Throwable t) {
            LOGGER.error("PC prank '{}' failed", payload.action(), t);
        }
    }

    // ------------------------------------------------------------------ browser

    private static void openUrl(String url) {
        // Only ever open plain web links — never let the server hand us a file:,
        // javascript: or command-style URI to launch.
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            LOGGER.warn("Refusing to open non-web URL: {}", url);
            return;
        }

        try {
            if (isWindows()) {
                // "start" launches the protocol handler as a foreground process;
                // Desktop.browse frequently leaves the new tab behind fullscreen Minecraft.
                // Even "start" isn't enough on its own: if the browser is already
                // running, "start" just hands the URL to that existing process, and
                // Windows' focus-stealing prevention then keeps its window behind
                // fullscreen Minecraft since we (not the browser) are the process
                // with recent input. AllowSetForegroundWindow(ASFW_ANY) grants a
                // one-time pass around that, as long as we (the foreground process)
                // request it right before spawning.
                WindowsDisplayApi.INSTANCE.AllowSetForegroundWindow(-1); // ASFW_ANY
                new ProcessBuilder("cmd.exe", "/c", "start", "", url).start();
                return;
            }
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to open URL {}", url, t);
        }
    }

    // ------------------------------------------------------------------ monitor

    private static void flipMonitor(int durationSeconds) {
        if (!isWindows()) {
            return;
        }

        int secs = durationSeconds > 0 ? durationSeconds : 120;
        long now = System.currentTimeMillis();
        long requestedEnd = now + secs * 1000L;
        long previousEnd = flipEndTimeMillis.getAndUpdate(current -> Math.max(current, requestedEnd));

        if (previousEnd <= now) {
            // Not currently flipped — actually flip the displays now.
            if (!setOrientation(2, true)) { // DMDO_180
                flipEndTimeMillis.compareAndSet(requestedEnd, previousEnd);
                return;
            }
            installShutdownHook();
        }
        // else: already flipped and running — the window above was just
        // extended, the displays themselves don't need touching again.

        SCHEDULER.schedule(PcPrankHandler::revertMonitorIfExpired, secs, TimeUnit.SECONDS);
    }

    /**
     * Fired by the scheduler once per flip, `secs` seconds after that flip.
     * Only actually reverts if the window hasn't been extended by a later hit
     * in the meantime — that later hit's own timer will do the real revert.
     */
    private static void revertMonitorIfExpired() {
        if (System.currentTimeMillis() >= flipEndTimeMillis.get()) {
            revertMonitorIfActive();
        }
    }

    /** Put every display back to normal orientation if we flipped it — unconditionally, for disconnect/shutdown. */
    public static void revertMonitorIfActive() {
        if (flipEndTimeMillis.getAndSet(0) > 0) {
            setOrientation(0, false);
        }
    }

    private static void installShutdownHook() {
        if (shutdownHookInstalled) {
            return;
        }
        shutdownHookInstalled = true;
        Runtime.getRuntime().addShutdownHook(new Thread(PcPrankHandler::revertMonitorIfActive));
    }

    private static final int CDS_UPDATEREGISTRY = 0x00000001;
    private static final int CDS_NORESET = 0x10000000;
    private static final int CDS_RESET = 0x40000000;

    /**
     * Rotate all displays through the native Win32 API.  JNA loads user32.dll
     * directly, avoiding PowerShell policies and temporary script files.
     *
     * Each display is applied with CDS_NORESET (queued, not yet visible) and
     * only committed together at the end via a single ChangeDisplaySettingsEx
     * (NULL, ..., CDS_RESET) call. Applying each monitor immediately and
     * individually — the previous approach — is what Microsoft's docs warn
     * against for multi-monitor setups: the driver can be caught mid-mode-switch
     * on one display while we hit it again for the next, which is the likely
     * source of the flakiness. A single retry per display absorbs the
     * occasional transient refusal from the driver.
     */
    private static boolean setOrientation(int orientation, boolean rememberOriginal) {
        try {
            boolean changed = false;
            for (int index = 0; ; index++) {
                WindowsDisplayApi.DisplayDevice display = new WindowsDisplayApi.DisplayDevice();
                if (!WindowsDisplayApi.INSTANCE.EnumDisplayDevices(null, index, display, 0)) break;
                if ((display.stateFlags & 0x1) == 0) continue; // not attached to desktop
                String name = display.name();
                WindowsDisplayApi.DevMode mode = new WindowsDisplayApi.DevMode();
                mode.dmSize = (short) mode.size();
                if (!WindowsDisplayApi.INSTANCE.EnumDisplaySettingsEx(name, -1, mode, 0)) continue;
                if (rememberOriginal) originalOrientations.putIfAbsent(name, mode.dmDisplayOrientation);
                mode.dmDisplayOrientation = rememberOriginal ? orientation
                        : originalOrientations.getOrDefault(name, orientation);
                mode.dmFields |= 0x80; // DM_DISPLAYORIENTATION

                int result = WindowsDisplayApi.INSTANCE.ChangeDisplaySettingsEx(
                        name, mode, null, CDS_UPDATEREGISTRY | CDS_NORESET, null);
                if (result != 0) {
                    // One retry: a busy/transient driver refusal is common and
                    // usually gone half a beat later.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    result = WindowsDisplayApi.INSTANCE.ChangeDisplaySettingsEx(
                            name, mode, null, CDS_UPDATEREGISTRY | CDS_NORESET, null);
                }
                if (result == 0) changed = true;
                else LOGGER.warn("Display rotation for {} failed with Win32 result {} (0=success, -2=mode unsupported)", name, result);
            }
            if (changed) {
                // Commit every queued display change together.
                WindowsDisplayApi.INSTANCE.ChangeDisplaySettingsEx(null, null, null, CDS_RESET, null);
            }
            if (!rememberOriginal && changed) originalOrientations.clear();
            return changed;
        } catch (Throwable t) {
            LOGGER.error("Failed to set display orientation {}", orientation, t);
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

}
