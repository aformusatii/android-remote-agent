package webview.smarthome.defendor.s4frame;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Defendor on 01/15/2017.
 */
public class DeviceAdminSample extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "test";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
    }

}
