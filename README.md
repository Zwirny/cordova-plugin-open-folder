# cordova-plugin-open-folder
Opens folders on android devices
# cordova-plugin-open-folder

A small Cordova plugin for Android that opens the Android system folder picker using `ACTION_OPEN_DOCUMENT_TREE`.

This plugin is useful when a Cordova application needs to let the user select a directory instead of opening a regular file.

## Installation

Install the plugin directly from GitHub:

```bash
cordova plugin add https://github.com/Zwirny/cordova-plugin-open-folder.git
```

Or install it from a local directory:

```bash
cordova plugin add ./cordova-plugin-open-folder
```

## Requirements

* Cordova
* Android
* Android 5.0 (API 21) or newer

No additional Android permissions are required.

## Usage

After installing the plugin, the API is available through:

```javascript
cordova.plugins.openFolder
```

### Open the folder picker

```javascript
cordova.plugins.openFolder.open(
    null,
    function (uri) {
        console.log("Folder selected:", uri);
    },
    function (error) {
        console.error("Could not open folder:", error);
    }
);
```

The success callback receives the URI of the selected directory.

Example:

```text
content://com.android.externalstorage.documents/tree/primary%3ADocuments
```

### Using a folder path

You can also pass a path to the plugin:

```javascript
const folder = cordova.file.externalDataDirectory;

cordova.plugins.openFolder.open(
    folder,
    function (uri) {
        console.log("Folder selected:", uri);
    },
    function (error) {
        console.error("Could not open folder:", error);
    }
);
```

For example, with `cordova-plugin-file`:

```javascript
const folder = cordova.file.externalDataDirectory;

cordova.plugins.openFolder.open(
    folder,
    function (uri) {
        console.log("Selected:", uri);
    },
    function (error) {
        console.error("Error:", error);
    }
);
```

## API

### `open(path, success, error)`

Opens the Android directory picker.

| Parameter | Type             | Description                                     |
| --------- | ---------------- | ----------------------------------------------- |
| `path`    | `String \| null` | Optional path used as the initial folder        |
| `success` | `Function`       | Called after the user selects a folder          |
| `error`   | `Function`       | Called when the operation fails or is cancelled |

### Success callback

```javascript
function (uri) {
    console.log("Selected folder:", uri);
}
```

The returned value is an Android `content://` URI.

### Error callback

```javascript
function (error) {
    console.error("Error:", error);
}
```

The callback is also invoked when the user cancels the folder selection.

## Example

A complete example using `cordova-plugin-file`:

```javascript
function selectFolder() {

    const path = cordova.file.externalDataDirectory;

    cordova.plugins.openFolder.open(
        path,
        function (uri) {
            console.log("Folder selected successfully:");
            console.log(uri);
        },
        function (error) {
            console.error("Folder selection failed:");
            console.error(error);
        }
    );
}
```

## Android Storage Access Framework

On Android 5.0 and newer, the plugin uses Android's Storage Access Framework:

```text
Intent.ACTION_OPEN_DOCUMENT_TREE
```

The user selects a directory through the Android system UI.

The returned `content://` URI can be used with Android APIs that support Storage Access Framework URIs.

## Important

This plugin opens the **Android folder selection UI**.

It does **not** open an arbitrary directory in a third-party file manager application.

In particular, Android does not guarantee that a given physical filesystem path can be opened directly in the user's preferred file manager.

The returned URI is a Storage Access Framework URI and may look like:

```text
content://com.android.externalstorage.documents/tree/primary%3ADocuments
```

## Permissions

The plugin does not require additional permissions in `AndroidManifest.xml`.

Access to the selected directory is granted by Android through the Storage Access Framework.

## Cancellation

If the user presses the Back button or otherwise cancels the folder selection, the error callback is called:

```javascript
cordova.plugins.openFolder.open(
    null,
    function (uri) {
        console.log("Selected:", uri);
    },
    function (error) {
        console.log("Selection cancelled or failed:", error);
    }
);
```

## License

MIT

## Author

Zwirny

## Repository

https://github.com/Zwirny/cordova-plugin-open-folder
