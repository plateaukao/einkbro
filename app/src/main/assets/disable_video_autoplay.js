(function() {
    var originalPlay = HTMLMediaElement.prototype.play;
    var userGesture = false;
    document.addEventListener('click', function() {
        userGesture = true;
        setTimeout(function() { userGesture = false; }, 1000);
    }, true);
    document.addEventListener('touchend', function() {
        userGesture = true;
        setTimeout(function() { userGesture = false; }, 1000);
    }, true);
    HTMLMediaElement.prototype.play = function() {
        if (userGesture) {
            return originalPlay.call(this);
        }
        this.pause();
        return Promise.resolve();
    };
})();
