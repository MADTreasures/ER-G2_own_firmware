# faceclaw-android – Herkunft

Faceclaws Android-Anbindung an Bluetooth LE (GPL-3.0), unverändert übernommen. Die Klassen sind reines Android ohne NativeScript und laufen deshalb auch auf Wear OS.

| | |
|---|---|
| Quelle | https://github.com/jimrandomh/faceclaw, `App_Resources/Android/src/main/java/com/faceclaw/app/` |
| Commit | `a6291cf9370f51652a4675b035269e9fa24d9f40` (25.09.2026), derselbe Stand wie `faceclaw-core` |
| Übernommen | `FaceclawBleManager.kt` (GATT), `AndroidSessionLink.kt` (`SessionLink` für die Sitzung), `AndroidStockLink.kt` (`StockLink` für die Prüf- und Kopplungsabläufe), `FaceclawDeviceInfoProbe.kt` (liest Firmware-Versionen, koppelt dabei) |
| Lokale Änderungen | keine |

`FaceclawBleManager` nutzt die `writeCharacteristic`-Variante ab API 33, daher braucht die App mindestens Wear OS 4.

Beim Aktualisieren von `faceclaw-core` diese vier Dateien vom selben Faceclaw-Commit mitkopieren.
