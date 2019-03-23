package webview.smarthome.defendor.s4frame;

import android.webkit.JavascriptInterface;

public class WebViewJsObject {

    @JavascriptInterface
    public Boolean turnOnScreenAlwaysOn() {
        return Boolean.TRUE;
    }

    @JavascriptInterface
    public Boolean turnOffScreenAlwaysOn() {
        return Boolean.TRUE;
    }

    @JavascriptInterface
    public Boolean hideBottomToolbar() {
        return Boolean.TRUE;
    }

}
