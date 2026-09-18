# cordova-plugin-open-folder

Cordova-Plugin (nur Android), das einen übergebenen Ordnerpfad **direkt** in der
auf dem Gerät installierten Datei-Explorer-App öffnet – **ohne** Auswahldialog.

Gedacht z.B. für den SAP Neptune Mobile Client auf Zebra-Scannern, um dem Nutzer
per Knopfdruck den Inhalt eines bestimmten Ordners (z. B. `cordova.file.externalDataDirectory`)
im normalen Dateimanager anzuzeigen.

## Installation

```bash
cordova plugin add https://github.com/Zwirny/cordova-plugin-open-folder.git
```

oder lokal:

```bash
cordova plugin add ./cordova-plugin-open-folder
```

Das Plugin benötigt `cordova-android >= 8.0.0` und bindet automatisch einen
`FileProvider` ins `AndroidManifest.xml` ein (kein zusätzliches Setup nötig).

## Verwendung

```js
var pfad = cordova.file.externalDataDirectory; // z.B. mit cordova-plugin-file

cordova.plugins.openFolder.open(
    pfad,
    function (uri) {
        console.log('Ordner geöffnet:', uri);
    },
    function (error) {
        console.error('Ordner konnte nicht geöffnet werden:', error);
    }
);
```

### API: `open(path, success, error)`

| Parameter | Typ        | Beschreibung                                                        |
| --------- | ---------- | -------------------------------------------------------------------- |
| `path`    | `String`   | Absoluter Dateisystempfad **oder** `file://`-URL (z. B. aus `cordova-plugin-file`) |
| `success` | `Function` | Wird aufgerufen, sobald eine App zum Anzeigen des Ordners gestartet wurde |
| `error`   | `Function` | Wird aufgerufen, wenn der Pfad ungültig ist oder keine App gefunden wurde |

Der `success`-Callback erhält die `content://`-URI des Ordners.

## Funktionsweise / Fallback-Strategie

Android bietet keine garantierte API, um "irgendeinen Ordner im Dateimanager öffnen"
zu erzwingen. Das Plugin probiert deshalb nacheinander:

1. **`ACTION_VIEW` mit MIME-Type `resource/folder`** auf einer `FileProvider`-URI –
   wird von vielen Datei-Manager-Apps unterstützt (u. a. Google Files, viele
   OEM-/Zebra-Dateimanager).
2. **Android's eingebautes "Files"/DocumentsUI** über eine `DocumentsContract`-URI –
   funktioniert für Pfade auf dem primären (internen/externen) Speicher, z. B.
   `cordova.file.externalDataDirectory`.
3. **Generischer `ACTION_VIEW` mit `*/*`** als letzter Fallback, damit Android
   selbst eine passende App vorschlägt.

Schlägt alles fehl (z. B. weil auf dem Gerät gar keine Datei-Manager-App
installiert ist), wird der `error`-Callback aufgerufen.

## Unterstützte Android-Versionen

- Minimum: Android 7.1.2 (API 25)
- Verwendet `androidx.core.content.FileProvider`, daher keine
  `FileUriExposedException` auf Android 7+.

## Berechtigungen

Keine zusätzlichen Runtime-Permissions nötig, solange der übergebene Pfad
innerhalb einer der im `FileProvider` konfigurierten Verzeichnisse liegt
(App-eigene Verzeichnisse, interner/externer Cache, externer Speicher).
Diese Konfiguration liegt in `src/android/res/xml/file_paths.xml` und kann bei
Bedarf erweitert werden.

## Lizenz

MIT

## Autor

Zwirny
