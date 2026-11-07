<p align="center"><img src="../android/images/logo.svg" alt="NS2Native logo" width="128"></p>

# NS2Native

Play your own copies of **Donkey Kong Bananza**, **Pokémon Pokopia**, and **Mario Kart World** natively on Android.

**NS2Native** adalah fork dari [BananaNative](https://github.com/moceure/BananaNative) (port Android dari [nxnative](https://github.com/seedds/nxnative) oleh seedds) yang difokuskan dan dioptimalkan secara khusus untuk perangkat berspesifikasi **Snapdragon 870 dengan RAM 8 GB** (arsitektur CPU ARMv8.2-A, GPU Adreno 650).

Proyek ini bukan emulator: kode biner 64-bit ARM dari game berjalan langsung secara native di CPU perangkat, dengan grafis Switch 2 di-rebuild menggunakan Vulkan. Tidak ada file game, key, atau firmware yang disertakan dalam repositori ini maupun aplikasi.

> [!NOTE]
> **Fokus & Optimasi untuk Snapdragon 870 / 8GB RAM:**
> - **ARMv8.2-A PAC Instruction Stripping (`scripts/fix_armv82_pac.py`)**: CPU ARMv8.2 (seperti Snapdragon 870, 865, 855 dengan Cortex-A77/A76) memicu `SIGILL` (Illegal Instruction crash) ketika mengeksekusi instruksi ARMv8.3 Pointer Authentication (`retab`, `retaa`, `pacia`, `xpaci`). Fork ini secara otomatis mem-patch biner hasil rewrite agar aman dijalankan pada prosesor ARMv8.2.
> - **Opsi Resolusi 540p & 360p**: Menambahkan opsi resolusi lebih ringan (Phone 540p / 0.5x dan Phone 360p / 0.33x) di Settings dan In-game panel untuk meringankan beban GPU Adreno 650 agar game tetap lancar dan playable.
> - **Efisiensi Penggunaan RAM (`shader_split.py`)**: Pemisahan shader dioptimalkan dengan mekanisme disk streaming agar tidak memicu Out-Of-Memory (OOM) pada perangkat dengan RAM 8 GB.
> - **Linux & Cloud Packaging**: Wrapper `dmsh_xlate` berbasis .NET runtime dan integrasi GitHub Actions workflow untuk membangun APK langsung dari cloud tanpa membebani perangkat lokal.

## Games

| Game | Title ID | Version | Status |
|---|---|---|---|
| Donkey Kong Bananza | `040074A01BF12000` | update 458752 | Works |
| Pokémon Pokopia | `0400A3A01EE4C000` | update 655360 | Works (~60 fps) |
| Mario Kart World | `0400C3F00006E000` | 1.8.0 | In progress (experimental) |

Versi game lain yang tidak sesuai akan ditolak dengan pesan yang menampilkan versi yang dibutuhkan.

## What you need

- **Perangkat**: Android 13 atau lebih baru, 64-bit ARM. Dioptimalkan untuk **Snapdragon 870 dengan 8 GB RAM** (Adreno 650), atau SoC Snapdragon serupa (SD 865 / 865+ / 888 / 8 Gen 1+).
- **Custom GPU Driver**: Disarankan menggunakan driver Turnip (Mesa Turnip Adreno) untuk performa dan stabilitas Vulkan terbaik pada Adreno 650.
- **Game Dump**: Dump game milik Anda sendiri dalam bentuk folder (`base/`, `updates/`, `dlc/`). File NSP/XCI tidak bekerja langsung untuk game Switch 2.
  ```
  <game>/
    base/                  exefs/ and romfs/
    updates/<version>/     the update, if the version above is one
    dlc/<title id>/<n>/    extra content, if you have it
  ```
- Ruang penyimpanan kosong sekitar 1-2 GB di HP untuk pack game yang telah disiapkan.
- Opsional: `Lossless.dll` dari Lossless Scaling untuk fitur frame generation (tidak disertakan).

## Setup & How to start

Setiap game disiapkan (prepare) satu kali: kodenya di-rewrite untuk Android (termasuk penyesuaian instruksi ARMv8.2) dan shadernya diterjemahkan untuk Vulkan.

### Melalui Perangkat (On-device Prepare)
1. Salin folder game dump ke penyimpanan HP (misal: `Switch2/DonkeyKongBananza`).
2. Install APK **NS2Native** dari tab [Actions](https://github.com/hidayat077/NS2Native/actions) / [Releases](https://github.com/hidayat077/NS2Native/releases).
3. Buka **NS2Native**, berikan izin **All files access**.
4. Ketuk **Add game**, pilih folder game, lalu ketuk **Prepare**. Proses berjalan di background service dengan notifikasi status.
5. Ketuk **Play** setelah proses selesai.

### Menggunakan PC / Mac (Lebih Cepat)
1. Siapkan Python dan dependensi (`pip install "capstone>=5,<6" lz4`).
2. Jalankan packer:
   ```sh
   python3 scripts/pack_android.py /path/to/<game>
   ```
   *(Skrip akan otomatis menjalankan `fix_armv82_pac.py` untuk membersihkan instruksi ARMv8.3 PAC).*
3. Salin folder game dan folder pack hasil kompilasi (`<title_id>-work/android-pack`) ke penyimpanan HP.
4. Buka aplikasi **NS2Native**, pilih folder game, dan mainkan.

## Features

- **Optimalisasi ARMv8.2 & Adreno 650**: PAC stripper bawaan dan pilihan resolusi dinamis hingga 540p / 360p.
- **Fold-aware layout**: Layout terpisah untuk layar luar dan layar dalam pada ponsel lipat.
- **Frame generation (LSFG)**: Dukungan frame generation terintegrasi (2x, 3x, 4x, atau target 60/90/120 fps) menggunakan `Lossless.dll`.
- **FSR 1 Upscaling**: AMD FidelityFX Super Resolution 1 dengan pengaturan ketajaman (sharpness) untuk resolusi rendah.
- **Touch Controls & Gamepad**: Kontrol layar transparan yang dapat disesuaikan posisinya serta dukungan penuh kontroller/gamepad eksternal.
- **In-Game Panel & Performance Overlay**: Pantau fps, frame time, GPU time, suhu baterai, clock CPU, dan penggunaan RAM secara realtime.

## Credits and licence

- Fork dari **[BananaNative](https://github.com/moceure/BananaNative)** oleh moceure.
- Dibangun di atas **[nxnative](https://github.com/seedds/nxnative)** oleh seedds (MIT).
- Biner shader translator diturunkan dari Ryujinx, switch2mac (azmasm), dan Eglantine (djihads80).
- Frame generation di-porting dari Eden dan lsfg-vk.

Aplikasi Android ini berlisensi **GPL-3.0-or-later** ([android/NOTICE.md](../android/NOTICE.md)).
Gunakan game yang Anda miliki secara sah. Dilarang meminta atau membagikan file game, key, atau dump.
