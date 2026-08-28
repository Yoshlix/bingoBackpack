package de.yoshlix.bingobackpack.client;

import de.yoshlix.bingobackpack.net.PcPrankPayload;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
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
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Throwable t) {
            LOGGER.debug("Desktop.browse failed, trying OS fallback", t);
        }

        try {
            if (isWindows()) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
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
        setOrientation(2); // DMDO_180
        monitorFlipped.set(true);
        installShutdownHook();

        int secs = durationSeconds > 0 ? durationSeconds : 120;
        SCHEDULER.schedule(PcPrankHandler::revertMonitorIfActive, secs, TimeUnit.SECONDS);
    }

    /** Put every display back to normal orientation if we flipped it. */
    public static void revertMonitorIfActive() {
        if (monitorFlipped.compareAndSet(true, false)) {
            setOrientation(0); // DMDO_DEFAULT
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
     * Rotate all displays to the given orientation (0 = normal, 2 = 180°) via a
     * PowerShell script that P/Invokes ChangeDisplaySettingsEx. 180° keeps the
     * resolution, so no width/height swap is needed.
     */
    private static void setOrientation(int orientation) {
        try {
            File script = File.createTempFile("bbp-rotate", ".ps1");
            script.deleteOnExit();
            Files.writeString(script.toPath(), POWERSHELL_ROTATE, StandardCharsets.UTF_8);

            new ProcessBuilder("powershell.exe",
                    "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script.getAbsolutePath(),
                    "-Orientation", Integer.toString(orientation))
                    .start();
        } catch (Throwable t) {
            LOGGER.error("Failed to set display orientation {}", orientation, t);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    // PowerShell that rotates every attached display to the requested orientation.
    private static final String POWERSHELL_ROTATE = """
            param([int]$Orientation = 0)
            $signature = @'
            using System;
            using System.Runtime.InteropServices;
            public class BbpDisp {
                [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Ansi)]
                public struct DEVMODE {
                    [MarshalAs(UnmanagedType.ByValTStr, SizeConst=32)] public string dmDeviceName;
                    public short dmSpecVersion; public short dmDriverVersion; public short dmSize;
                    public short dmDriverExtra; public int dmFields;
                    public int dmPositionX; public int dmPositionY; public int dmDisplayOrientation; public int dmDisplayFixedOutput;
                    public short dmColor; public short dmDuplex; public short dmYResolution; public short dmTTOption;
                    public short dmCollate; [MarshalAs(UnmanagedType.ByValTStr, SizeConst=32)] public string dmFormName;
                    public short dmLogPixels; public int dmBitsPerPel; public int dmPelsWidth; public int dmPelsHeight;
                    public int dmDisplayFlags; public int dmDisplayFrequency;
                    public int dmICMMethod; public int dmICMIntent; public int dmMediaType; public int dmDitherType;
                    public int dmReserved1; public int dmReserved2; public int dmPanningWidth; public int dmPanningHeight;
                }
                [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Ansi)]
                public struct DISPLAY_DEVICE {
                    public int cb;
                    [MarshalAs(UnmanagedType.ByValTStr, SizeConst=32)] public string DeviceName;
                    [MarshalAs(UnmanagedType.ByValTStr, SizeConst=128)] public string DeviceString;
                    public int StateFlags;
                    [MarshalAs(UnmanagedType.ByValTStr, SizeConst=128)] public string DeviceID;
                    [MarshalAs(UnmanagedType.ByValTStr, SizeConst=128)] public string DeviceKey;
                }
                [DllImport("user32.dll")] public static extern bool EnumDisplayDevices(string dev, uint id, ref DISPLAY_DEVICE info, uint flags);
                [DllImport("user32.dll", CharSet=CharSet.Ansi)] public static extern bool EnumDisplaySettings(string dev, int mode, ref DEVMODE dm);
                [DllImport("user32.dll", CharSet=CharSet.Ansi)] public static extern int ChangeDisplaySettingsEx(string dev, ref DEVMODE dm, IntPtr hwnd, uint flags, IntPtr param);
                public const int ENUM_CURRENT_SETTINGS = -1;
                public const int DM_DISPLAYORIENTATION = 0x00000080;
                public const uint CDS_UPDATEREGISTRY = 0x00000001;
            }
            '@
            Add-Type -TypeDefinition $signature -ErrorAction SilentlyContinue

            $i = 0
            while ($true) {
                $dd = New-Object BbpDisp+DISPLAY_DEVICE
                $dd.cb = [System.Runtime.InteropServices.Marshal]::SizeOf($dd)
                if (-not [BbpDisp]::EnumDisplayDevices($null, [uint32]$i, [ref]$dd, 0)) { break }
                $i++
                if (($dd.StateFlags -band 0x1) -eq 0) { continue }  # not attached to desktop

                $dm = New-Object BbpDisp+DEVMODE
                $dm.dmSize = [System.Runtime.InteropServices.Marshal]::SizeOf($dm)
                if ([BbpDisp]::EnumDisplaySettings($dd.DeviceName, [BbpDisp]::ENUM_CURRENT_SETTINGS, [ref]$dm)) {
                    $dm.dmDisplayOrientation = $Orientation
                    $dm.dmFields = $dm.dmFields -bor [BbpDisp]::DM_DISPLAYORIENTATION
                    [void][BbpDisp]::ChangeDisplaySettingsEx($dd.DeviceName, [ref]$dm, [IntPtr]::Zero, [BbpDisp]::CDS_UPDATEREGISTRY, [IntPtr]::Zero)
                }
            }
            """;
}
