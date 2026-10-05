# MANZ CarPlay

Launcher and background service that enables wireless Apple CarPlay on Android 7.0+ tablets without an external USB adapter.

![CarPlay Dashboard](docs/screenshots/carplay_dashboard_redacted.png)

## Screenshots

| Dashboard | App grid |
| :---: | :---: |
| ![Dashboard](docs/screenshots/carplay_dashboard_redacted.png) | ![Apps](docs/screenshots/carplay_appgrid.png) |

| Controller interface |
| :---: |
| ![Launcher](docs/screenshots/launcher_ui.png) |

## What it does

- Wakes the tablet, sets brightness to maximum, and starts the CarPlay receiver when USB or 12V charger power is detected.
- Starts a local Wi-Fi hotspot (`MNZCARPLAY`) on Android 7 using reflection on `IWifiManager.setWifiApEnabled`.
- Adds Linux kernel policy routing rules so non-root apps can communicate over the `ap0` interface via IPv6 link-local and TCP port 7000.
- Runs a bundled `dnsmasq` instance on `ap0` to serve DHCP leases to connected phones.
- Drops background apps and pins all 4 CPU cores online on MediaTek MT8167B hardware to reduce frame drops during H.264 video decoding.
- Keeps ADB open on TCP port 5555 for remote management.

## How it works

Older versions of Android isolate network routing between UIDs. On Android 7, apps without system privileges are blocked by default from resolving local routes on tethered interfaces. MANZ CarPlay uses temp root permissions (`mtk-su`) during initialization to insert routing priorities:

```sh
ip rule add from all lookup main pref 99
ip -6 rule add from all lookup main pref 99
ip -6 rule add from all lookup 1014 pref 98
ip -6 route add fe80::/64 dev ap0 table main
ip route add default dev ap0 table main
```

This lets CarPlay receivers such as DiPlay or xcertplay discover the iPhone over Bonjour and accept the AirPlay stream without requiring system image modifications.

## Building from source

Requirements:
- JDK 8 or higher
- Android SDK build tools (AAPT, D8, zipalign, apksigner)
- Android API 24 platform (`android-24`)

Run the build script:

```powershell
.\build.ps1
```

The compiled package is saved as `MANZCarPlay.apk`.

Install it via ADB:

```powershell
adb install -r MANZCarPlay.apk
adb shell "appops set com.car.autocarplay WRITE_SETTINGS allow"
```

## License

GPL-3.0. See [LICENSE](LICENSE) for details.
CarPlay is a trademark of Apple Inc.
