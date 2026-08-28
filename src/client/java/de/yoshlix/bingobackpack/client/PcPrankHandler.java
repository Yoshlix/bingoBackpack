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
import java.util.concurrent.atomic.AtomicBoolean;

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

    // True while displays are flipped, so we can revert on disconnect / JVM exit.
    private static final AtomicBoolean monitorFlipped = new AtomicBoolean(false);
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
        if (!setOrientation(2, true)) { // DMDO_180
            return;
        }
        monitorFlipped.set(true);
        installShutdownHook();

        int secs = durationSeconds > 0 ? durationSeconds : 120;
        SCHEDULER.schedule(PcPrankHandler::revertMonitorIfActive, secs, TimeUnit.SECONDS);
    }

    /** Put every display back to normal orientation if we flipped it. */
    public static void revertMonitorIfActive() {
        if (monitorFlipped.compareAndSet(true, false)) {
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

    /**
     * Rotate all displays through the native Win32 API.  JNA loads user32.dll
     * directly, avoiding PowerShell policies and temporary script files.
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
                int result = WindowsDisplayApi.INSTANCE.ChangeDisplaySettingsEx(name, mode, null, 0x1, null);
                if (result == 0) changed = true;
                else LOGGER.warn("Display rotation for {} failed with Win32 result {} (0=success, -2=mode unsupported)", name, result);
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
