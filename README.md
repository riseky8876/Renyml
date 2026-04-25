# Genshin Performance Manager

Android (Java) app oleh **xueyin with Furina** untuk mengoptimalkan performa Genshin Impact.

## Cara Build

1. Buka Android Studio (Hedgehog atau lebih baru).
2. Pilih **Open** dan arahkan ke folder `GenshinPerformanceManager`.
3. Tunggu Gradle sync selesai. Klik **Run** (▶) untuk install ke device.
4. Pastikan device sudah **rooted** (Magisk/KernelSU) supaya tombol bekerja.

## Spesifikasi

- minSdk 26 (Android 8.0)
- targetSdk 33 (Android 13)
- Bahasa: Java
- Build system: Gradle (AGP 8.1)

## Struktur

```
app/src/main/
├── AndroidManifest.xml
├── java/com/xueyin/genshinperf/
│   ├── SplashActivity.java
│   ├── MainActivity.java
│   ├── SettingsActivity.java
│   ├── AboutActivity.java
│   ├── CrashLogActivity.java
│   ├── CrashHandler.java
│   ├── LogManager.java
│   └── RootExecutor.java
└── res/
    ├── layout/
    ├── drawable/
    ├── anim/
    ├── menu/
    └── values/
```

## Script

Letakkan script di `/sdcard/GenshinPerf/main.sh`. Tombol akan
menjalankan: `su -c "sh /sdcard/GenshinPerf/main.sh <1|2|3|4>"`.
