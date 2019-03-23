package webview.smarthome.defendor.s4frame;

import android.app.Application;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Timer;
import java.util.TimerTask;

public class MQTTClientService {

    private static final String MQTT_URI = "tcp://192.168.1.116:1883";

    private static final String MQTT_DISPLAY_TOPIC = "CONTROL/DISPLAY2";

    private MqttAndroidClient client;

    private Timer timer;
    private DevicePolicyManager deviceManger;
    private Window currentWindow;
    private AppCompatActivity mainActivity;
    private PowerManager.WakeLock wakeLock;
    private boolean mqTTSubscribed = false;

    public void setupMQTTClient(final AppCompatActivity mainActivity) {
        this.timer = new Timer();
        this.deviceManger = (DevicePolicyManager) mainActivity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.currentWindow = mainActivity.getWindow();
        this.mainActivity = mainActivity;

        PowerManager powerManager = ((PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE));
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "s4frame:TAG");

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(mainActivity.getApplicationContext(), MQTT_URI, clientId);

        try {

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(false);

            client.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("test", "success");

                    if (mqTTSubscribed) {
                        return;
                    }

                    try {
                        client.subscribe(MQTT_DISPLAY_TOPIC, 0, new MQTTMessageListener());
                        mqTTSubscribed = true;
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("test", "onFailure");
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private class MQTTMessageListener implements IMqttMessageListener {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String body = new String(message.getPayload());

            Log.d(MQTT_DISPLAY_TOPIC, "Received: " + body);

            if (DisplayCommands.SWITCH_ON.name().equalsIgnoreCase(body)) {

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //wake();
                        wakeLock.acquire();
                    }
                });

            } else if (DisplayCommands.SWITCH_OFF.name().equalsIgnoreCase(body)) {
                /* deviceManger.lockNow(); */

                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("test", "Run lock");
                        // lock();
                        try {
                            wakeLock.release();
                        } catch(RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                });

            } else {
                Log.w(MQTT_DISPLAY_TOPIC, "Received unexpected body: " + body);
            }
        }
    }

    public void wake() {
        Log.d("test", "Run wake 1");
        //PowerManager powerManager = ((PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE));
        //PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "s4frame:TAG");
        if (wakeLock == null) {
            PowerManager powerManager = ((PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE));
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "s4frame:TAG");
        }

        Log.d("test", "Run wake 2");

        try {
            wakeLock.acquire();

            Log.d("test", "Run wake 3 " + wakeLock.isHeld());

            mainActivity.getWindow().addFlags(
                              WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            );

            Log.d("test", "Run wake 4");
        } finally {
            // wakeLock.release();
        }
    }

    public void lock() {
        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public void close() {
        try {
            client.disconnect();
            client.close();
        } catch(Exception e) {
            Log.w("MQTT-CLOSE", e.getMessage());
            // e.printStackTrace();
        }
    }

    public enum DisplayCommands {
        SWITCH_OFF,
        SWITCH_ON
    }

}
