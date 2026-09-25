# faceclaw-core – Herkunft

Dieses Modul ist der gemeinsame Kotlin-Kern von **Faceclaw** (Jim Babcock, GPL-3.0), unverändert übernommen.

| | |
|---|---|
| Quelle | https://github.com/jimrandomh/faceclaw |
| Commit | `a6291cf9370f51652a4675b035269e9fa24d9f40` (25.09.2026) |
| Übernommen | `native/kotlin/shared/src/commonMain` → `src/commonMain`, `native/kotlin/shared/src/androidMain` → `src/androidMain` |
| Tests | `tests/kotlin/src/commonTest` → `src/commonTest`, `tests/kotlin/src/androidHostTest` → `src/androidHostTest` |
| Weggelassen | `native/kotlin/shared/src/iosMain` (nur für iOS), `FontTest.kt` (braucht eine 6 MB große Schrift aus Faceclaws `app/`) |
| Lokale Änderungen | keine |
| Lizenz | GPL-3.0 (siehe `LICENSE` im Wurzelverzeichnis) |

## Firmware-Voraussetzung

Dieser Stand spricht das private Protokoll der Custom-Firmware in **Revision 34** („Faceclaw/34“), aufgebaut auf der Stock-Firmware G2 **2.3.0.24**. Dieselbe Revision erzeugt g2flash (`patches/settings_ext.c`, Stand 25.09.2026).

Ältere Revisionen passen nicht. Neuere akzeptiert Faceclaws eigene App, weil Revisionen den Vertrag laut `app/g2/firmware-compat.ts` nur erweitern; die Uhr-App hält es genauso (`FirmwareRequirement`). g2flash rät Fremdprojekten allerdings, eine Firmware nur bei exakt bekannter Kennung als kompatibel anzusehen (README, Abschnitt zur Firmware-Kennung).

## Aktualisieren

Den Kern nie von Hand ändern, sondern auf einen neuen Faceclaw-Stand heben:

```sh
scripts/sync-faceclaw-core.sh /pfad/zu/faceclaw   # Checkout auf dem gewünschten Commit
./gradlew :faceclaw-core:testAndroidHostTest
```

Stand 25.09.2026: 180 Tests in 37 Klassen, alle grün (JDK 25, AGP 9.4.1).

Danach in dieser Datei Commit und Firmware-Revision nachtragen und in der App `FirmwareRequirement.REQUIRED_REVISION` auf denselben Wert setzen. Die benötigte Revision steht in Faceclaw unter `app/g2/firmware-compat.ts` (`REQUIRED_FACECLAW_FIRMWARE_VERSION`).
