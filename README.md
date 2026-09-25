# G2 Watch

Eine eigene Wear-OS-App für die Even Realities G2: Die Pixel Watch ist der Rechner, die Brille der Bildschirm mit Mauszeiger, das Uhrdisplay das Touchpad. Die App baut auf [Faceclaw](https://github.com/jimrandomh/faceclaw) auf (GPL-3.0). Dessen Kotlin-Kern für Protokoll, Sitzung und Bildübertragung ist unverändert übernommen.

![Desktop auf der Brille](docs/bilder/desktop-start.png)

- **Architektur, Sicherheit, Bauen und Testen:** [docs/ARCHITEKTUR.md](docs/ARCHITEKTUR.md)
- **Recherche zu eigener Firmware, Risiken und Stufenplan:** [RECHERCHE_FIRMWARE.md](RECHERCHE_FIRMWARE.md)

## Kurz

- Die Anzeige auf der Brille braucht **Faceclaws Firmware ab Revision 34**. Mit anderer Firmware liest die App nur die Version, hört auf und erklärt warum.
- Die App **schreibt nie Firmware**. Ob eine eigene Firmware auf die Brille kommt, ist eine eigene, bewusste Entscheidung (siehe Recherche).
- Ohne Brille läuft der Desktop als Vorschau auf der Uhr.
- Auf Hardware ist die App noch nicht getestet.

## Bauen

Voraussetzung ist JDK 25; Gradle lädt es bei Bedarf selbst.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk` und lässt sich mit `adb install` auf die Uhr bringen.
