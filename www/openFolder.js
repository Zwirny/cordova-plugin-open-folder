var exec = require('cordova/exec');

var PLUGIN_NAME = 'OpenFolder';

var OpenFolder = {
    /**
     * Opens the given folder path directly in the device's native file manager app.
     *
     * @param {string} path            Absolute filesystem path or file:// URL,
     *                                  e.g. cordova.file.externalDataDirectory
     * @param {function} successCallback  Called with the content:// URI of the folder once an app was launched.
     * @param {function} errorCallback    Called with an error message if no app could open the folder.
     */
    open: function (path, successCallback, errorCallback) {
        exec(successCallback, errorCallback, PLUGIN_NAME, 'open', [path]);
    }
};

module.exports = OpenFolder;
