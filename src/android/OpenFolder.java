package com.example.openfolder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;

public class OpenFolder extends CordovaPlugin {

    private static final int REQUEST_OPEN_FOLDER = 1001;

    private CallbackContext callbackContext;

    @Override
    public boolean execute(
            String action,
            JSONArray args,
            CallbackContext callbackContext
    ) throws JSONException {

        if ("open".equals(action)) {
            this.callbackContext = callbackContext;

            openFolder(args);

            return true;
        }

        return false;
    }

    private void openFolder(JSONArray args) {

        cordova.getActivity().runOnUiThread(() -> {

            try {

                Intent intent;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                    intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                    );

                    String path = null;

                    if (args != null && args.length() > 0 && !args.isNull(0)) {
                        path = args.getString(0);
                    }

                    /*
                     * Versuche, den gewünschten Ordner als Startpunkt
                     * zu verwenden.
                     */
                    if (path != null && path.length() > 0) {

                        Uri uri = Uri.parse(path);

                        if ("file".equalsIgnoreCase(uri.getScheme())) {

                            String documentId =
                                uri.getPath();

                            if (documentId != null) {
                                documentId =
                                    documentId.replaceFirst("^/", "");

                                Uri initialUri =
                                    DocumentsContract.buildRootUri(
                                        uri.getAuthority(),
                                        documentId
                                    );

                                intent.putExtra(
                                    "android.provider.extra.INITIAL_URI",
                                    initialUri
                                );
                            }
                        }
                    }

                } else {

                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                cordova.startActivityForResult(
                    this,
                    intent,
                    REQUEST_OPEN_FOLDER
                );

            } catch (Exception e) {

                if (callbackContext != null) {
                    callbackContext.error(
                        e.getMessage() != null
                            ? e.getMessage()
                            : "Could not open folder"
                    );
                }
            }
        });
    }

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

        if (requestCode != REQUEST_OPEN_FOLDER) {
            return;
        }

        if (callbackContext == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && intent != null) {

            Uri uri = intent.getData();

            if (uri != null) {

                callbackContext.success(
                    uri.toString()
                );

            } else {

                callbackContext.error(
                    "No folder selected"
                );
            }

        } else {

            callbackContext.error(
                "Folder selection cancelled"
            );
        }

        callbackContext = null;
    }
}