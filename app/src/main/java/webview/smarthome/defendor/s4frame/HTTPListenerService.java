package webview.smarthome.defendor.s4frame;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

public class HTTPListenerService extends Service {

    private PowerManager.WakeLock wakeLock;
    private ActivityManager activityManager;
    private AudioManager audioManager;
    private HTTPServer httpServer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("S4Frame", "HTTPListenerService->onCreate");

        PowerManager powerManager = ((PowerManager) getSystemService(Context.POWER_SERVICE));
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "S4FrameService:TAG");
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        httpServer = new HTTPServer(this, activityManager, wakeLock, audioManager);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("S4Frame", "HTTPListenerService->onStartCommand");
        httpServer.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("S4Frame", "HTTPListenerService->onDestroy");
        httpServer.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
