package webview.smarthome.defendor.s4frame;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import static webview.smarthome.defendor.s4frame.Constants.*;

public class MainActivity extends AppCompatActivity {

    private MQTTClientService mqTTService;

    private Intent httpListenerServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        Log.d("S4Frame", "onCreate!!!");

        if (!isMyServiceRunning(HTTPListenerService.class)) {
            httpListenerServiceIntent = new Intent(this, HTTPListenerService.class);
            Log.d("S4Frame", "MainActivity->!!!startService!!!");
            startService(httpListenerServiceIntent);
        } else {
            Log.d("S4Frame", "MainActivity->service already running!!!");
        }

        setupWebView();
    }

    private WebView setupWebView() {
        final WebView wv = (WebView) findViewById(R.id.MainWebView);
        // wv.getSettings().setBuiltInZoomControls(true);
        wv.getSettings().setUseWideViewPort(true);
        wv.setInitialScale(1);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                SharedPreferences preferences = getSharedPreferences(S4FRAME_PREF, MODE_PRIVATE);
                String username = preferences.getString(PROP_USERNAME_NAME, "");
                String password = preferences.getString(PROP_PASSWORD_NAME, "");
                handler.proceed(username, password);
            }
        });

        wv.clearCache(true);
        wv.clearHistory();

        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        wv.addJavascriptInterface(new WebViewJsObject(), "AndroidObj");

        wv.loadUrl(getUrlOrDefault());

        wv.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int vis) {
                wv.loadUrl("javascript:{ScreenSaver.documentAction()};");
            }
        });

        return wv;
    }

    public void onStart() {
        super.onStart();
        Log.d("S4Frame", "onStart!!!");
    }

    public void onStop() {
        super.onStop();
        Log.d("S4Frame", "onStop!!!");
    }

    public void onResume() {
        super.onResume();
        Log.d("S4Frame", "onResume!!!");
    }

    public void onPause() {
        super.onPause();
        Log.d("S4Frame", "onPause!!!");
    }

    public void onRestart() {
        super.onRestart();
        Log.d("S4Frame", "onRestart!!!");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("S4Frame", "onDestroy!!!");
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private String getUrlOrDefault() {
        Bundle data = getIntent().getExtras();
        if (data != null) {
            String webUrl = data.getString("webUrl");
            if (webUrl != null) {
                return webUrl;
            }
        }

        SharedPreferences preferences = getSharedPreferences(S4FRAME_PREF, MODE_PRIVATE);
        return preferences.getString("webUrl", "http://google.com");
    }
}
