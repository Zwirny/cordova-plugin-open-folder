package cordova.plugin.openfolder;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.core.content.FileProvider;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;

/**
 * Cordova plugin (Android only) that opens a given folder path directly in the
 * device's native file manager app - no folder-picker dialog is shown.
 *
 * Strategy (in order, first one that finds an app to handle it wins):
 *  1. ACTION_VIEW on a FileProvider content:// URI with mime type "resource/folder"
 *     (understood by Google Files and many OEM file managers, e.g. on Zebra devices).
 *  2. ACTION_VIEW on Android's built-in DocumentsUI ("Files") document URI,
 *     for paths located on primary external/shared storage.
 *  3. ACTION_VIEW with a generic "*&#47;*" mime type as a last resort, letting
 *     Android offer any app that can handle the content URI.
 */
public class OpenFolder extends CordovaPlugin {

    private static final String ACTION_OPEN = "open";
    private static final String PROVIDER_SUFFIX = ".cordova.plugin.openfolder.provider";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!ACTION_OPEN.equals(action)) {
            return false;
        }

        final String path = args.isNull(0) ? null : args.getString(0);

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                openFolder(path, callbackContext);
            }
        });

        return true;
    }

    private void openFolder(String rawPath, CallbackContext callbackContext) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            callbackContext.error("No path provided");
            return;
        }

        String cleanPath = toFileSystemPath(rawPath);
        File folder = new File(cleanPath);

        if (!folder.exists()) {
            callbackContext.error("Path does not exist: " + cleanPath);
            return;
        }
        if (!folder.isDirectory()) {
            callbackContext.error("Path is not a directory: " + cleanPath);
            return;
        }

        Uri contentUri;
        try {
            String authority = cordova.getActivity().getPackageName() + PROVIDER_SUFFIX;
            contentUri = FileProvider.getUriForFile(cordova.getActivity(), authority, folder);
        } catch (IllegalArgumentException e) {
            callbackContext.error("Folder is outside the paths configured for FileProvider: " + e.getMessage());
            return;
        }

        if (tryOpen(contentUri, "resource/folder")) {
            callbackContext.success(contentUri.toString());
            return;
        }

        if (tryOpenDocumentsUi(cleanPath)) {
            callbackContext.success(contentUri.toString());
            return;
        }

        if (tryOpen(contentUri, "*/*")) {
            callbackContext.success(contentUri.toString());
            return;
        }

        callbackContext.error("No file manager app found on this device that can open a folder");
    }

    /** Accepts either a plain filesystem path or a file:// URL (as returned by cordova-plugin-file). */
    private String toFileSystemPath(String path) {
        if (path.startsWith("file://")) {
            String decoded = Uri.parse(path).getPath();
            return decoded != null ? decoded : path;
        }
        return path;
    }

    private boolean tryOpen(Uri uri, String mimeType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(cordova.getActivity().getPackageManager()) != null) {
                cordova.getActivity().startActivity(intent);
                return true;
            }
        } catch (ActivityNotFoundException | SecurityException e) {
            // try next strategy
        }
        return false;
    }

    /**
     * Opens Android's built-in Documents UI ("Files") directly at the folder location.
     * Only works for paths located on primary external/shared storage
     * (e.g. /storage/emulated/0/...), which covers cordova.file.externalDataDirectory.
     */
    private boolean tryOpenDocumentsUi(String absolutePath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        String primaryRoot;
        try {
            primaryRoot = Environment.getExternalStorageDirectory().getCanonicalPath();
        } catch (Exception e) {
            primaryRoot = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        String canonicalPath = absolutePath;
        try {
            canonicalPath = new File(absolutePath).getCanonicalPath();
        } catch (Exception e) {
            // keep absolutePath as-is
        }

        if (!canonicalPath.startsWith(primaryRoot)) {
            return false;
        }

        String relative = canonicalPath.substring(primaryRoot.length());
        if (relative.startsWith(File.separator)) {
            relative = relative.substring(1);
        }

        String docId = "primary:" + relative;
        Uri docUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId);

        return tryOpen(docUri, "vnd.android.document/directory");
    }
}
