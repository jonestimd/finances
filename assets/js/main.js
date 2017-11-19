(function(global) {
    global.fixScale = function(doc) {
        var addEvent = 'addEventListener',
            type = 'gesturestart',
            qsa = 'querySelectorAll',
            scales = [1, 1],
            meta = qsa in doc ? doc[qsa]('meta[name=viewport]') : [];

        function fix() {
            meta.content = 'width=device-width,minimum-scale=' + scales[0] + ',maximum-scale=' + scales[1];
            doc.removeEventListener(type, fix, true);
        }

        if ((meta = meta[meta.length - 1]) && addEvent in doc) {
            fix();
            scales = [.25, 1.6];
            doc[addEvent](type, fix, true);
        }
    };

    global.scaleImages = function(doc, baseurl) {
        baseurl = baseurl || '';
        const overlay = doc.getElementById('image-overlay');
        overlay.onclick = () => overlay.classList.add('hidden');
        for (let i=0; i<doc.images.length; i++) {
            const image = doc.images[i];
            // image.onload = (event) => image.width = image.clientWidth / 2;
            image.onclick = (event) => {
                overlay.innerHTML = '<img class="close-icon" src="' + baseurl + '/assets/images/ic_close_white_24px.svg"/>'
                                  + '<img class="center" src="' + event.target.src + '"/>';
                overlay.classList.remove('hidden');
            };
        }
        doc.onkeyup = (e) => (e.key === 'Escape') && overlay.classList.add('hidden');
    };
})(window);