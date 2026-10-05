package com.car.autocarplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.IBinder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class CarPlayHelper {

    public static synchronized String ensureMtkSu(Context context) {
        try {
            File localSu = new File(context.getFilesDir(), "mtk-su");
            if (!localSu.exists() || localSu.length() == 0) {
                InputStream in = context.getAssets().open("mtk-su64");
                FileOutputStream out = new FileOutputStream(localSu);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
            if (localSu.exists()) {
                localSu.setExecutable(true, false);
                localSu.setReadable(true, false);
                return localSu.getAbsolutePath();
            }
        } catch (Exception ignored) {}

        File tmpSu = new File("/data/local/tmp/mtk-su");
        if (tmpSu.exists() && tmpSu.canExecute()) {
            return tmpSu.getAbsolutePath();
        }
        return null;
    }

    public static synchronized void ensureTestApJar(Context context) {
        try {
            File dest = new File("/data/local/tmp/testap.jar");
            if (!dest.exists() || dest.length() == 0) {
                InputStream in = context.getAssets().open("testap.jar");
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                dest.setReadable(true, false);
            }
        } catch (Exception ignored) {}
    }

    public static synchronized String runRootCommand(Context context, String shScript) {
        StringBuilder sb = new StringBuilder();
        try {
            String binaryPath = ensureMtkSu(context);
            if (binaryPath == null) {
                return "Error: mtk-su root binary unavailable";
            }

            File scriptFile = new File(context.getFilesDir(), "exec_cmd.sh");
            FileOutputStream fos = new FileOutputStream(scriptFile);
            String fullScript = "#!/system/bin/sh\n" + shScript + "\necho __CMD_FINISHED__\nexit\n";
            fos.write(fullScript.getBytes("UTF-8"));
            fos.flush();
            fos.close();
            scriptFile.setExecutable(true, false);
            scriptFile.setReadable(true, false);

            String cmd = "cat " + scriptFile.getAbsolutePath() + " | " + binaryPath + " -Z u:r:shell:s0";
            ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try {
                p.getOutputStream().close();
            } catch (Exception ignored) {}

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("UID:") && !line.startsWith("selinux:") && !line.equals("__CMD_FINISHED__")) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append("Root exec error: ").append(t.getMessage());
        }
        return sb.toString().trim();
    }

    public static synchronized String enableHotspot(Context context) {
        ensureTestApJar(context);
        String netScript =
            "CLASSPATH=/data/local/tmp/testap.jar app_process /system/bin TestAp enable MNZCARPLAY\n" +
            "sleep 1\n" +
            "ip link set ap0 up 2>/dev/null\n" +
            "ip addr add 192.168.43.1/24 dev ap0 2>/dev/null\n" +
            "ip rule add from all lookup main pref 99 2>/dev/null\n" +
            "ip -6 rule add from all lookup main pref 99 2>/dev/null\n" +
            "ip -6 rule add from all lookup 1014 pref 98 2>/dev/null\n" +
            "ip -6 route add fe80::/64 dev ap0 table main 2>/dev/null\n" +
            "ip route add default dev ap0 table main 2>/dev/null\n" +
            "pkill dnsmasq 2>/dev/null\n" +
            "nohup /system/bin/dnsmasq --no-daemon --no-resolv --no-poll --dhcp-range=192.168.43.10,192.168.43.99,1h --interface=ap0 >/dev/null 2>&1 &\n" +
            "echo 'ap0 ready: ' $(ip addr show ap0 2>/dev/null | grep 'inet ')\n";
        return runRootCommand(context, netScript);
    }

    public static synchronized String disableHotspot(Context context) {
        ensureTestApJar(context);
        String restoreWifi =
            "CLASSPATH=/data/local/tmp/testap.jar app_process /system/bin TestAp disable\n" +
            "pkill dnsmasq 2>/dev/null\n" +
            "svc wifi enable\n" +
            "echo 'SoftAP disabled & Wi-Fi client restored.'\n";
        return runRootCommand(context, restoreWifi);
    }

    public static synchronized String enableWirelessAdb(Context context) {
        String script = "setprop service.adb.tcp.port 5555\n" +
                        "for p in $(pidof adbd); do kill -9 $p 2>/dev/null; done\n" +
                        "echo 'Wireless ADB active on port 5555.'";
        return runRootCommand(context, script);
    }

    public static synchronized String runTurboBoost(Context context) {
        String script = 
            "echo 'Terminating background bloat...'\n" +
            "for p in com.aurora.store com.teslacoilsw.launcher com.finalwire.aida64 com.topjohnwu.magisk io.github.visnkmr.quicklaunch be.ppareit.swiftp_free org.fdroid.fdroid pl.solidexplorer2 com.android.settings; do\n" +
            "  pids=$(pidof $p)\n" +
            "  if [ -n \"$pids\" ]; then\n" +
            "    for pid in $pids; do kill -9 $pid 2>/dev/null; done\n" +
            "    echo \"Stopped $p\"\n" +
            "  fi\n" +
            "done\n" +
            "for p in $(pidof com.shihab.diplay.hudtest); do renice -n -10 -p $p 2>/dev/null; echo \"Prioritized MANZ Play ($p)\"; done\n" +
            "for p in $(pidof media.codec); do renice -n -10 -p $p 2>/dev/null; echo \"Prioritized Codec ($p)\"; done\n" +
            "for p in $(pidof surfaceflinger); do renice -n -8 -p $p 2>/dev/null; done\n" +
            "if [ -f /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load ]; then echo 80 > /sys/devices/system/cpu/cpu0/cpufreq/interactive/go_hispeed_load 2>/dev/null; fi\n" +
            "if [ -f /proc/hps/enabled ]; then echo 0 > /proc/hps/enabled 2>/dev/null; fi\n" +
            "for i in 1 2 3; do echo 1 > /sys/devices/system/cpu/cpu$i/online 2>/dev/null; done\n" +
            "sync; echo 3 > /proc/sys/vm/drop_caches\n" +
            "echo \"Cores: $(cat /sys/devices/system/cpu/online 2>/dev/null) / 4\"\n" +
            "echo \"Temp: $(( $(cat /sys/class/thermal/thermal_zone2/temp 2>/dev/null) / 1000 ))°C\"\n" +
            "FREE_KB=$(grep MemAvailable /proc/meminfo 2>/dev/null | tr -s ' ' | cut -d ' ' -f 2)\n" +
            "if [ -n \"$FREE_KB\" ]; then echo \"Free RAM: $(( FREE_KB / 1024 )) MB\"; fi\n" +
            "echo 'BOOST_DONE'";
        return runRootCommand(context, script);
    }

    public static void launchCarPlay(Context context) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.shihab.diplay.hudtest", "com.shilapi.xcertplay.CarPlayHostActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = context.getPackageManager().getLaunchIntentForPackage("com.shihab.diplay.hudtest");
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(fallback);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void returnHome(Context context) {
        runRootCommand(context, "input keyevent 3");
    }

    public static void sleepTablet(Context context) {
        runRootCommand(context, "input keyevent 26");
    }

    public static boolean isPowerConnected(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void applyPowerConnected(Context context) {
        String cmd = "settings put global stay_on_while_plugged_in 7; " +
                     "settings put system screen_brightness 255; " +
                     "settings put system screen_brightness_mode 0; " +
                     "settings put system screen_off_timeout 2147483647; " +
                     "input keyevent 224; " +
                     "input keyevent 82; " +
                     "if [ -f /proc/hps/enabled ]; then echo 0 > /proc/hps/enabled; fi; " +
                     "for i in 1 2 3; do echo 1 > /sys/devices/system/cpu/cpu$i/online 2>/dev/null; done; " +
                     "echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null; " +
                     "setprop service.adb.tcp.port 5555";
        runRootCommand(context, cmd);
    }

    public static String getDeviceInfo(Context context) {
        StringBuilder sb = new StringBuilder();
        try {
            File f = new File("/sys/devices/system/cpu/online");
            String cores = "4";
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                String l = br.readLine();
                br.close();
                if (l != null) cores = l.trim();
            }
            sb.append("CPU: ").append(cores).append(" Cores | ");

            File ft = new File("/sys/class/thermal/thermal_zone2/temp");
            if (ft.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ft)));
                String l = br.readLine();
                br.close();
                if (l != null) {
                    int mc = Integer.parseInt(l.trim());
                    sb.append(mc / 1000).append("°C | ");
                }
            }

            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent b = context.registerReceiver(null, ifilter);
            if (b != null) {
                int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                sb.append("Batt: ").append(level).append("%");
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    public static String getNetworkStatus() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(ifaces)) {
                if ("ap0".equals(iface.getName()) && iface.isUp()) {
                    return "Hotspot: ACTIVE (MNZCARPLAY)";
                }
            }
            for (NetworkInterface iface : Collections.list(ifaces)) {
                if ("wlan0".equals(iface.getName()) && iface.isUp()) {
                    return "Wi-Fi: Connected";
                }
            }
        } catch (Exception ignored) {}
        return "Offline";
    }
}
