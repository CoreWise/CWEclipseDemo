package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Vector;


/**
 * 获取硬件地址的类（Find SerialPort hardware adress class）
 */
public class SerialPortFinder {

    private static final String m_TAG = "cy";

    private Vector<Driver> m_Drivers = null;

    /**
     * 设备类(Device Class)
     */
    public class Driver {
        private String m_DriverName;
        private String m_DeviceRoot;
        Vector<File> m_Devices = null;

        public Driver(String name, String root) {
            m_DriverName = name;
            m_DeviceRoot = root;
        }

        public Vector<File> getDevices() {
            if (m_Devices == null) {
                m_Devices = new Vector<File>();
                File dev = new File("/dev");
                File[] files = dev.listFiles();
                int i;
                for (i = 0; i < files.length; i++) {
                    if (files[i].getAbsolutePath().startsWith(m_DeviceRoot)) {
                        Log.d(m_TAG, "Found new device: " + files[i]);
                        m_Devices.add(files[i]);
                    }
                }
            }

            return m_Devices;
        }

        public String getName() {
            return m_DriverName;
        }
    }

    /**
     * 获取设备(Get Device)
     * @return
     * @throws IOException
     */
    private Vector<Driver> getDrivers() throws IOException {
        if (m_Drivers == null) {
            m_Drivers = new Vector<Driver>();
            LineNumberReader r = new LineNumberReader(new FileReader("/proc/tty/drivers"));
            String l;
            while ((l = r.readLine()) != null) {
                // Since driver name may contain spaces, we do not extract driver name with split()
                String drivername = l.substring(0, 0x15).trim();
                String[] w = l.split(" +");
                if ((w.length >= 5) && (w[w.length - 1].equals("serial"))) {
                    Log.d(m_TAG, "Found new driver " + drivername + " on " + w[w.length - 4]);
                    m_Drivers.add(new Driver(drivername, w[w.length - 4]));
                }
            }
            r.close();
        }
        return m_Drivers;
    }

    /**
     * 获取所有设备名称 (get all device name)
     *
     * @return
     */
    public String[] getAllDevices() {
        Vector<String> devices = new Vector<String>();
        // Parse each driver
        Iterator<Driver> itdriv;
        try {
            itdriv = getDrivers().iterator();
            while (itdriv.hasNext()) {
                Driver driver = itdriv.next();
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getName();
                    String value = String.format("%s (%s)", device, driver.getName());
                    devices.add(value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return devices.toArray(new String[devices.size()]);
    }

    /**
     * 获取所有设备路径（get all device path）
     *
     * @return
     */
    public String[] getAllDevicesPath() {
        Vector<String> devices = new Vector<String>();
        // Parse each driver
        Iterator<Driver> itdriv;
        try {
            itdriv = getDrivers().iterator();
            while (itdriv.hasNext()) {
                Driver driver = itdriv.next();
                Iterator<File> itdev = driver.getDevices().iterator();
                while (itdev.hasNext()) {
                    String device = itdev.next().getAbsolutePath();
                    devices.add(device);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return devices.toArray(new String[devices.size()]);
    }
}
