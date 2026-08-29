package de.yoshlix.bingobackpack.client;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

import java.util.List;

/** Minimal Unicode Win32 binding for changing a monitor's display mode. */
interface WindowsDisplayApi extends StdCallLibrary {
    WindowsDisplayApi INSTANCE = Native.load("user32", WindowsDisplayApi.class, W32APIOptions.UNICODE_OPTIONS);

    boolean EnumDisplayDevices(String device, int index, DisplayDevice displayDevice, int flags);
    boolean EnumDisplaySettingsEx(String deviceName, int modeNum, DevMode devMode, int flags);
    int ChangeDisplaySettingsEx(String deviceName, DevMode devMode, Pointer hwnd, int flags, Pointer lParam);

    /**
     * Grants the next SetForegroundWindow call a one-time pass through the
     * focus-stealing prevention. Called with ASFW_ANY (-1) right before we
     * spawn the browser, since Windows otherwise sometimes leaves the new
     * tab in the background while fullscreen Minecraft keeps focus.
     */
    boolean AllowSetForegroundWindow(int dwProcessId);

    class DisplayDevice extends Structure {
        // DISPLAY_DEVICEW is always 840 bytes (32 + 128 + 128 + 128 UTF-16 chars).
        // Do not call size() in the constructor: Structure's superclass runs
        // before Java initializes these fixed-size array fields.
        public int cb = 840;
        public char[] deviceName = new char[32];
        public char[] deviceString = new char[128];
        public int stateFlags;
        public char[] deviceId = new char[128];
        public char[] deviceKey = new char[128];
        @Override protected List<String> getFieldOrder() { return List.of("cb", "deviceName", "deviceString", "stateFlags", "deviceId", "deviceKey"); }
        String name() { return Native.toString(deviceName); }
    }

    class DevMode extends Structure {
        public char[] dmDeviceName = new char[32];
        public short dmSpecVersion, dmDriverVersion, dmSize, dmDriverExtra;
        public int dmFields, dmPositionX, dmPositionY, dmDisplayOrientation, dmDisplayFixedOutput;
        public short dmColor, dmDuplex, dmYResolution, dmTTOption, dmCollate;
        public char[] dmFormName = new char[32];
        public short dmLogPixels;
        public int dmBitsPerPel, dmPelsWidth, dmPelsHeight, dmDisplayFlags, dmDisplayFrequency;
        public int dmICMMethod, dmICMIntent, dmMediaType, dmDitherType, dmReserved1, dmReserved2, dmPanningWidth, dmPanningHeight;
        @Override protected List<String> getFieldOrder() { return List.of("dmDeviceName", "dmSpecVersion", "dmDriverVersion", "dmSize", "dmDriverExtra", "dmFields", "dmPositionX", "dmPositionY", "dmDisplayOrientation", "dmDisplayFixedOutput", "dmColor", "dmDuplex", "dmYResolution", "dmTTOption", "dmCollate", "dmFormName", "dmLogPixels", "dmBitsPerPel", "dmPelsWidth", "dmPelsHeight", "dmDisplayFlags", "dmDisplayFrequency", "dmICMMethod", "dmICMIntent", "dmMediaType", "dmDitherType", "dmReserved1", "dmReserved2", "dmPanningWidth", "dmPanningHeight"); }
    }
}
