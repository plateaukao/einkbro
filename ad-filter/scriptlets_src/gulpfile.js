const gulp = require('gulp');
const fs = require('fs');

// uglify-es
const uglifyjs = require('uglify-es');
const composer = require('gulp-uglify/composer');
const pump = require('pump');
const uglifyES = composer(uglifyjs, console);

const file = 'scriptlets.js';
const dist = '../src/main/js/';
const minFile = dist + 'scriptlets.min.js'

gulp.task('uglify', function (cb) {
    var options = {
        toplevel: true,
        compress: {
            inline: false,
            keep_fargs: true,
            keep_classnames: true,
            keep_fnames: true
        },
        mangle: false
    };

    pump([
        gulp.src(file),
        uglifyES(options),
        gulp.dest(dist)
    ],
        cb
    );
});

gulp.task('rename', function (done) {
    const distFile = dist + file;
    fs.rename(distFile, minFile, done);
});

gulp.task('default', gulp.series('uglify', 'rename'));