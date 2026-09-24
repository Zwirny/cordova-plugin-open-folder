# cordova-plugin-open-folder
[![npm - Latest](https://img.shields.io/npm/v/cordova-plugin-open-folder/latest?label=Latest%20Release%20(npm)&color=red)](https://npmjs.com/package/cordova-plugin-open-folder)
[![GitHub](https://img.shields.io/github/package-json/v/Zwirny/cordova-plugin-open-folder?label=Development%20(Git)&color=blue)](https://github.com/Zwirny/cordova-plugin-open-folder)

## AI Usage

This project was created fully by AI. I have not read any of the code, not even this readme.

## Overview

A Cordova plugin (Android only) that opens a specified folder path **directly** in the file explorer app installed on the device – **without** displaying a folder selection dialog.

It is intended, for example, for the SAP Neptune Mobile Client on Zebra scanners, allowing users to open the contents of a specific folder (such as `cordova.file.externalDataDirectory`) in the regular file manager with a single button press.

## Installation

```bash
cordova plugin add https://github.com/Zwirny/cordova-plugin-open-folder.git
```

Or locally:

```bash
cordova plugin add ./cordova-plugin-open-folder
```

The plugin requires `cordova-android >= 8.0.0` and automatically registers a `FileProvider` in the `AndroidManifest.xml` (no additional setup required).

## Usage

```js
var path = cordova.file.externalDataDirectory; // e.g. using cordova-plugin-file

cordova.plugins.openFolder.open(
    path,
    function (uri) {
        console.log('Folder opened:', uri);
    },
    function (error) {
        console.error('Could not open folder:', error);
    }
);
```

### API: `open(path, success, error)`

| Parameter | Type       | Description                                                                     |
| --------- | ---------- | --------------------------------------------------------------------------------- |
| `path`    | `String`   | Absolute filesystem path **or** `file://` URL (e.g. from `cordova-plugin-file`) |
| `success` | `Function` | Called once an app capable of displaying the folder has been launched           |
| `error`   | `Function` | Called if the path is invalid or no suitable app can be found                   |

The `success` callback receives the `content://` URI of the folder.

## How It Works / Fallback Strategy

Android does not provide a guaranteed API for forcing "any folder" to open in a file manager. Therefore, the plugin tries the following approaches in sequence:

1. **Android's built-in "Files"/DocumentsUI** using a `DocumentsContract` URI.
   This works for paths located on the primary internal/external storage, such as `cordova.file.externalDataDirectory`.

2. **`ACTION_VIEW` with a `file://` URI** using the MIME types `resource/folder` and `*/*`.
   This is included as a compatibility fallback for some older or simpler file manager apps that do not properly handle `content://` URIs.

3. **`ACTION_VIEW` with the MIME type `resource/folder`** using a `FileProvider` `content://` URI.
   This is supported by many file manager apps, including Google Files and various OEM/Zebra file managers.

4. **Generic `ACTION_VIEW` with `*/*`** using the `FileProvider` `content://` URI as the final fallback, allowing Android to find an application capable of handling the content.

If all approaches fail (for example, if no suitable file manager app is installed on the device), the `error` callback is invoked.


## Supported Android Versions

* Minimum: Android 7.1.2 (API 25)
* Uses `androidx.core.content.FileProvider`, preventing `FileUriExposedException` on Android 7+.

## Permissions

No additional runtime permissions are required as long as the provided path is located within one of the directories configured in the `FileProvider`.

The current configuration is located in:

```text
src/android/res/xml/file_paths.xml
```

It can be extended if additional directories need to be supported.

## License

MIT

## Author

Claude
