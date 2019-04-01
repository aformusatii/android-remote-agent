package webview.smarthome.defendor.s4frame;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import static webview.smarthome.defendor.s4frame.Constants.*;

public class HTTPServer {

    private AsyncHttpServer server;
    private Service service;
    private ActivityManager activityManager;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;

    public HTTPServer(Service service,
                      ActivityManager activityManager,
                      PowerManager.WakeLock wakeLock,
                      AudioManager audioManager) {
        this.service = service;
        this.activityManager = activityManager;
        this.wakeLock = wakeLock;
        this.audioManager = audioManager;
        setupHTTPServer();
    }

    private void setupHTTPServer() {
        server = new AsyncHttpServer();
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("UP");
            }
        });

        server.get("/activity", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
                if(!taskInfo.isEmpty()) {
                    Gson gson = new Gson();
                    String json = gson.toJson(taskInfo.get(0));
                    response.send(CONTENT_TYPE_JSON, json);
                } else {
                    response.code(501);
                    response.end();
                }
            }
        });

        server.get("/activities", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(Integer.MAX_VALUE);
                if(!taskInfo.isEmpty()) {
                    Gson gson = new Gson();
                    String json = gson.toJson(taskInfo);
                    response.send(CONTENT_TYPE_JSON, json);
                } else {
                    response.code(501);
                    response.end();
                }
            }
        });

        server.post("/activity/class", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String body = request.getBody().get().toString();
                JSONObject obj = getJSONObject(body);

                try {
                    String packageName = getProperty(obj, PROP_PACKAGE_NAME);
                    String className = getProperty(obj, PROP_CLASS_NAME);
                    Log.d("S4Frame", "HTTPListenerService->activity:packageName:" + packageName);
                    Log.d("S4Frame", "HTTPListenerService->activity:className:" + className);

                    Intent intent = new Intent();
                    intent.setClassName(packageName,className);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    JSONObject extrasObj = getJSONObjectProperty(obj, PROP_EXTRAS_NAME);
                    if (extrasObj != null) {
                        Iterator<String> extrasKeys = extrasObj.keys();
                        while (extrasKeys.hasNext()) {
                            String extrasKey = extrasKeys.next();
                            String extrasValue = getProperty(extrasObj, extrasKey);
                            intent.putExtra(extrasKey, extrasValue);
                        }
                    }

                    service.startActivity(intent);

                    addProperty(obj, PROP_RUN_FLAG, true);
                } catch (Exception e) {
                    addProperty(obj, PROP_RUN_FLAG, false);
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });

        server.post("/activity/actionHome", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject obj = new JSONObject();

                try {
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    service.startActivity(startMain);

                    addProperty(obj, PROP_RUN_FLAG, true);
                } catch (Exception e) {
                    addProperty(obj, PROP_RUN_FLAG, false);
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });

        server.post("/activity/actionView", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String urlActivity = request.getBody().get().toString();
                Log.d("S4Frame", "HTTPListenerService->actionView:" + urlActivity);
                JSONObject obj = new JSONObject();
                try {
                    Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlActivity));
                    appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    service.startActivity(appIntent);

                    addProperty(obj, PROP_RUN_FLAG, true);
                } catch (Exception e) {
                    addProperty(obj, PROP_RUN_FLAG, false);
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });

        server.get("/power", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject obj = new JSONObject();
                addProperty(obj, "wakeLock", wakeLock.isHeld());
                response.send(obj);
            }
        });

        server.post("/power/lock", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                wakeLock.acquire();
                response.send("DONE");
            }
        });

        server.post("/power/unlock", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                wakeLock.release();
                response.send("DONE");
            }
        });

        server.post("/volume", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String volume = request.getBody().get().toString().trim();
                audioManager.adjustVolume(getVolumeFlag(volume), AudioManager.FLAG_PLAY_SOUND);
                response.send("DONE");
            }
        });

        server.get("/stream/volume", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject obj = new JSONObject();
                Integer volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                addProperty(obj, PROP_VOLUME, volume);
                response.send(obj);
            }
        });

        server.post("/stream/volume", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String volume = request.getBody().get().toString().trim();
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.valueOf(volume), 0);
                response.send("DONE");
            }
        });

        server.get("/settings", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject obj = new JSONObject();

                SharedPreferences preferences = service.getSharedPreferences(S4FRAME_PREF, Context.MODE_PRIVATE);
                Map<String, ?> properties = preferences.getAll();

                for (Map.Entry<String, ?> entry : properties.entrySet()) {
                    addProperty(obj, entry.getKey(), entry.getValue());
                }

                response.send(obj);
            }
        });

        server.post("/settings", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String body = request.getBody().get().toString();
                JSONObject obj = getJSONObject(body);

                try {
                    SharedPreferences preferences = service.getSharedPreferences(S4FRAME_PREF, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = preferences.edit();

                    Iterator<String> extrasKeys = obj.keys();
                    while (extrasKeys.hasNext()) {
                        String extrasKey = extrasKeys.next();
                        String extrasValue = getProperty(obj, extrasKey);
                        edit.putString(extrasKey, extrasValue);
                    }

                    edit.commit();

                    addProperty(obj, PROP_RUN_FLAG, true);
                } catch (Exception e) {
                    addProperty(obj, PROP_RUN_FLAG, false);
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });

        server.get("/brightness", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                JSONObject obj = new JSONObject();

                try {
                    Integer brightness = android.provider.Settings.System.getInt(service.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
                    addProperty(obj, PROP_BRIGHTNESS_NAME, brightness);
                } catch (Exception e) {
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });

        server.post("/brightness", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                String body = request.getBody().get().toString();
                JSONObject obj = getJSONObject(body);

                try {
                    String brightnessValue = getProperty(obj, PROP_BRIGHTNESS_NAME);
                    android.provider.Settings.System.putInt(service.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    android.provider.Settings.System.putInt(service.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, Integer.parseInt(brightnessValue));

                    addProperty(obj, PROP_RUN_FLAG, true);
                } catch (Exception e) {
                    addProperty(obj, PROP_RUN_FLAG, false);
                    addProperty(obj, PROP_ERROR_MSG, e.getMessage());
                    e.printStackTrace();
                }

                response.send(obj);
            }
        });
    }

    private void addProperty(JSONObject obj, String name, Object value) {
        try {
            obj.put(name, value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getProperty(JSONObject obj, String name) {
        try {
            return obj.getString(name);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private JSONObject getJSONObjectProperty(JSONObject obj, String name) {
        try {
            if (obj.has(name)) {
                return obj.getJSONObject(name);
            }

            return null;
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private JSONObject getJSONObject(String value) {
        try {
            return new JSONObject(value);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    private Integer getVolumeFlag(String value) {
        switch(value) {
            case "UP":
                return AudioManager.ADJUST_RAISE;
            case "DOWN":
                return AudioManager.ADJUST_LOWER;
            case "MUTE":
                return AudioManager.ADJUST_MUTE;
            case "UNMUTE":
                return AudioManager.ADJUST_UNMUTE;
            default:
                return AudioManager.ADJUST_SAME;
        }
    }

    public void start() {
        server.listen(5000);
    }

    public void stop() {
        server.stop();
    }

}
