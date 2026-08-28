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

    class DisplayDevice extends Structure {
        public int cb = size();
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
