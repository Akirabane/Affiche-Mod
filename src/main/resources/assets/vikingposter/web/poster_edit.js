'use strict';

let nonce = 0;

function send(msg) {
    console.log('POSTER::' + msg + '|' + (++nonce));
}

function updateSizeLabel() {
    const w = parseFloat(document.getElementById('img-width').value).toFixed(1);
    const h = parseFloat(document.getElementById('img-height').value).toFixed(1);
    document.getElementById('img-width-label').textContent  = w;
    document.getElementById('img-height-label').textContent = h;
}

function updateOffsetLabel() {
    const ox = parseFloat(document.getElementById('img-offset-x').value).toFixed(1);
    const oy = parseFloat(document.getElementById('img-offset-y').value).toFixed(1);
    document.getElementById('img-offset-x-label').textContent = ox;
    document.getElementById('img-offset-y-label').textContent = oy;
}

function applyRatio(rw, rh) {
    const wSlider = document.getElementById('img-width');
    const hSlider = document.getElementById('img-height');
    const w = parseFloat(wSlider.value);
    let h = w * rh / rw;
    h = Math.max(0.5, Math.min(16, Math.round(h * 2) / 2));
    hSlider.value = h;
    updateSizeLabel();
}

document.getElementById('image-url').addEventListener('input', function() {
    const url  = this.value.trim();
    const img  = document.getElementById('img-preview');
    const hint = document.getElementById('img-preview-hint');
    if (url.startsWith('http://') || url.startsWith('https://')) {
        img.onload  = () => { img.style.display = 'block'; hint.style.display = 'none'; };
        img.onerror = () => { img.style.display = 'none';  hint.style.display = ''; hint.textContent = 'Impossible de charger l\'image'; };
        img.src = url;
    } else {
        img.style.display = 'none';
        hint.style.display = '';
        hint.textContent = 'Aperçu (si URL valide)';
    }
});

function loadData(imageUrl, imgW, imgH, offsetX, offsetY) {
    document.getElementById('image-url').value    = imageUrl || '';
    document.getElementById('img-width').value    = imgW;
    document.getElementById('img-height').value   = imgH;
    document.getElementById('img-offset-x').value = offsetX || 0;
    document.getElementById('img-offset-y').value = offsetY || 0;
    updateSizeLabel();
    updateOffsetLabel();

    if (imageUrl) {
        document.getElementById('image-url').dispatchEvent(new Event('input'));
    }
}

function submit() {
    const url = document.getElementById('image-url').value.trim();
    const w   = document.getElementById('img-width').value;
    const h   = document.getElementById('img-height').value;
    const ox  = document.getElementById('img-offset-x').value;
    const oy  = document.getElementById('img-offset-y').value;
    // submit|imageUrl|w|h|offsetX|offsetY
    send('submit|' + url + '|' + w + '|' + h + '|' + ox + '|' + oy);
}

function cancel() {
    send('close');
}

send('ready');
