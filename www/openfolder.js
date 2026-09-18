/*
 * Copyright (c) 2026 Zwirny
 *
 * MIT License
 */

var exec = require("cordova/exec");

/**
 * Opens the Android system folder picker.
 *
 * @param {String|null} path
 *      Optional initial content:// URI.
 *
 * @param {Function} success
 *      Called when a folder was selected.
 *
 * @param {Function} error
 *      Called when the picker was cancelled or failed.
 */
exports.open = function (path, success, error) {

    exec(
        success,
        error,
        "OpenFolder",
        "open",
        [
            path || null
        ]
    );
};