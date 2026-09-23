package cordova.plugin.openfolder;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
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

    private static final String TAG = "OpenFolderPlugin";

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) {
        if (!ACTION_OPEN.equals(action)) {
            return false;
        }

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                // Catch literally everything (Exception AND Error) so a problem here
                // can never bring down the whole app - it always reports back to JS instead.
                try {
                    String path = args.isNull(0) ? null : args.getString(0);
                    openFolder(path, callbackContext);
                } catch (Throwable t) {
                    reportCrashSafely(t, callbackContext);
                }
            }
        });

        return true;
    }

    private void reportCrashSafely(Throwable t, CallbackContext callbackContext) {
        android.util.Log.e(TAG, "OpenFolder plugin failed", t);
        String msg = t.getClass().getName() + ": " + t.getMessage();
        try {
            callbackContext.error(msg);
        } catch (Throwable ignored) {
            // never let error reporting itself crash the app
        }
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
        } catch (Throwable t) {
            reportCrashSafely(t, callbackContext);
            return;
        }

        try {
            if (tryOpenDocumentsUi(cleanPath)) {
                callbackContext.success(contentUri.toString());
                return;
            }

            if (tryOpenWithRawFileUri(folder)) {
                callbackContext.success(contentUri.toString());
                return;
            }

            if (tryOpen(contentUri, "resource/folder")) {
                callbackContext.success(contentUri.toString());
                return;
            }

            if (tryOpen(contentUri, "*/*")) {
                callbackContext.success(contentUri.toString());
                return;
            }
        } catch (Throwable t) {
            reportCrashSafely(t, callbackContext);
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

    /**
     * Many simpler/legacy file manager apps (e.g. ones built on top of a raw java.io.File
     * based browser, which is what most on-device "Files" apps with full storage access do)
     * only know how to navigate a folder when given a plain file:// URI - a content:// URI
     * from our own FileProvider isn't recognized by them and just causes the app to fall
     * back to its default/home screen instead of actually navigating.
     *
     * Since Android 7 (API 24) throws a FileUriExposedException when a file:// URI is
     * exposed via an Intent, we temporarily relax that StrictMode check just for this call.
     */
    private boolean tryOpenWithRawFileUri(File folder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // FileUriExposedException does not exist before API 24; file:// just works.
            Uri fileUri = Uri.fromFile(folder);
            return tryOpen(fileUri, "resource/folder") || tryOpen(fileUri, "*/*");
        }

        StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
        try {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
            Uri fileUri = Uri.fromFile(folder);
            return tryOpen(fileUri, "resource/folder") || tryOpen(fileUri, "*/*");
        } catch (Throwable t) {
            android.util.Log.w(TAG, "tryOpenWithRawFileUri failed", t);
            return false;
        } finally {
            try {
                StrictMode.setVmPolicy(oldPolicy);
            } catch (Throwable ignored) {
                // never let restoring the policy crash the app
            }
        }
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
        } catch (Throwable t) {
            android.util.Log.w(TAG, "tryOpen(" + mimeType + ") failed", t);
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
        Uri docUri;
        try {
            docUri = DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", docId);
        } catch (Throwable t) {
            android.util.Log.w(TAG, "buildDocumentUri failed", t);
            return false;
        }

        return tryOpen(docUri, "vnd.android.document/directory");
    }
}
