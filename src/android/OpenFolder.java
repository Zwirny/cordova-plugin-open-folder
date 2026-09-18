/*
 * Copyright (c) 2026 Zwirny
 *
 * MIT License
 */

package com.zwirny.openfolder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Cordova plugin for opening the Android folder picker.
 *
 * Android's Storage Access Framework is used through:
 *
 *     Intent.ACTION_OPEN_DOCUMENT_TREE
 *
 * The result is a content:// URI representing the selected folder.
 */
public class OpenFolder extends CordovaPlugin {

    /**
     * Request code used to identify our folder picker.
     */
    private static final int REQUEST_OPEN_FOLDER = 1001;

    /**
     * JavaScript callback waiting for the picker result.
     */
    private CallbackContext callbackContext;


    /**
     * Entry point called by Cordova JavaScript.
     */
    @Override
    public boolean execute(
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) throws JSONException {

        /*
         * We currently expose only one action:
         *
         *     open
         */
        if ("open".equals(action)) {

            /*
             * Store the callback because the Android activity
             * returns asynchronously.
             */
            this.callbackContext = callbackContext;

            openFolder(args);

            return true;
        }

        /*
         * Unknown action.
         */
        return false;
    }


    /**
     * Opens the Android system folder picker.
     */
    private void openFolder(JSONArray args) {

        /*
         * Android UI operations must run on the UI thread.
         */
        cordova.getActivity().runOnUiThread(() -> {

            try {

                /*
                 * ACTION_OPEN_DOCUMENT_TREE is available from
                 * Android 5.0 / API 21.
                 */
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

                    callbackContext.error(
                            "Android 5.0 (API 21) or newer is required."
                    );

                    callbackContext = null;

                    return;
                }


                /*
                 * Create the native Android folder picker.
                 */
                Intent intent = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT_TREE
                );


                /*
                 * Request read/write access to the selected
                 * directory.
                 *
                 * Persistable permission allows the application
                 * to keep using the selected URI later.
                 */
                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                );


                /*
                 * Android 8.0 / API 26 introduced EXTRA_INITIAL_URI.
                 *
                 * Only a content:// URI can safely be passed here.
                 *
                 * A file:// URI, such as the URI returned by
                 * cordova-plugin-file, cannot simply be converted
                 * into a SAF URI.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    String path = null;

                    if (args != null
                            && args.length() > 0
                            && !args.isNull(0)) {

                        path = args.getString(0);
                    }


                    if (path != null
                            && path.startsWith("content://")) {

                        Uri initialUri = Uri.parse(path);

                        intent.putExtra(
                                "android.provider.extra.INITIAL_URI",
                                initialUri
                        );
                    }
                }


                /*
                 * Start the Android activity.
                 *
                 * Cordova will call onActivityResult() when
                 * the picker is closed.
                 */
                cordova.startActivityForResult(
                        this,
                        intent,
                        REQUEST_OPEN_FOLDER
                );

            } catch (Exception e) {

                /*
                 * Something went wrong while starting the picker.
                 */
                if (callbackContext != null) {

                    callbackContext.error(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Could not open folder picker."
                    );

                    callbackContext = null;
                }
            }
        });
    }


    /**
     * Receives the result from Android's folder picker.
     */
    @Override
    public void onActivityResult(
            int requestCode,
            int resultCode,
            Intent intent
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                intent
        );


        /*
         * Ignore results belonging to other activities.
         */
        if (requestCode != REQUEST_OPEN_FOLDER) {
            return;
        }


        /*
         * No JavaScript callback is waiting.
         */
        if (callbackContext == null) {
            return;
        }


        /*
         * User selected a folder.
         */
        if (resultCode == Activity.RESULT_OK
                && intent != null) {

            Uri uri = intent.getData();


            if (uri != null) {

                /*
                 * Ask Android to keep the granted permission.
                 *
                 * Some document providers may not support this.
                 * In that case the selected URI is still returned.
                 */
                try {

                    final int takeFlags =
                            intent.getFlags()
                                    & (
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    );

                    cordova.getActivity()
                            .getContentResolver()
                            .takePersistableUriPermission(
                                    uri,
                                    takeFlags
                            );

                } catch (Exception ignored) {

                    /*
                     * The document provider does not support
                     * persistable permissions.
                     *
                     * This is not fatal.
                     */
                }


                /*
                 * Return the selected content:// URI to JavaScript.
                 */
                callbackContext.success(
                        uri.toString()
                );

            } else {

                callbackContext.error(
                        "No folder URI was returned by Android."
                );
            }

        } else {

            /*
             * User pressed Back or otherwise cancelled the picker.
             */
            callbackContext.error(
                    "Folder selection cancelled."
            );
        }


        /*
         * The callback has now been handled.
         */
        callbackContext = null;
    }
}