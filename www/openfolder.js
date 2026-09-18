var exec = require('cordova/exec');

module.exports = {
    open: function (path, success, error) {
        exec(
            success,
            error,
            'OpenFolder',
            'open',
            [path || null]
        );
    }
};