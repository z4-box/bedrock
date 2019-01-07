/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/*
For 'standard' /new variations, this file will append all query params
(most importantly the 'xv' param) to download button links
so the correct template is rendered on /download/thanks/.
*/

(function() {
    'use strict';

    var downloadLinks = document.getElementsByClassName('download-link');
    var href;
    var newQs;
    var params = new window._SearchParams().params;
    var prefix;

    // we need to propogate 'v' or 'xv' query params to ensure the correct template is loaded on /download/thanks/
    // only one of these keys should be present

    // 'xv' denotes a 'variant' page for a specific marketing campaign
    if (params.hasOwnProperty('xv')) {
        newQs = 'xv=' + params['xv'];
    // 'v' denotes an experiment variation
    } else if (params.hasOwnProperty('v')) {
        newQs = 'v=' + params['v'];
    }

    // merge v/xv params from the current URL with any query params existing on in-page links pointing to /download/thanks/
    for (var i = 0; i < downloadLinks.length; i++) {
        href = downloadLinks[i].href;

        // only alter links going to /firefox/download/thanks/
        if (href.indexOf('download/thanks/') > 0) {
            // account for an existing querystring on the link
            prefix = (href.indexOf('?') > 0) ? '&': '?';
            downloadLinks[i].href += prefix + newQs;
        }
    }
})();
