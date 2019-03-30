package webview.smarthome.defendor.s4frame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootAutostart extends BroadcastReceiver {

    public void onReceive(Context context, Intent arg1) {
        Intent intent = new Intent(context, HTTPListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        Log.i("S4Frame", "Autostart-Started");
    }

}