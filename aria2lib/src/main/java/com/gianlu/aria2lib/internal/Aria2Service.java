package com.gianlu.aria2lib.internal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gianlu.aria2lib.BadEnvironmentException;
import com.gianlu.aria2lib.commonutils.Prefs;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public final class Aria2Service extends Service implements Aria2.MessageListener {
    public static final String ACTION_START_SERVICE = Aria2Service.class.getCanonicalName() + ".START";
    public static final String ACTION_STOP_SERVICE = Aria2Service.class.getCanonicalName() + ".STOP";
    public static final String BROADCAST_MESSAGE = Aria2Service.class.getCanonicalName() + ".BROADCAST_MESSAGE";
    public static final String BROADCAST_STATUS = Aria2Service.class.getCanonicalName() + ".BROADCAST_STATUS";
    public static final int MESSAGE_STATUS = 2;
    public static final int MESSAGE_STOP = 3;
    private static final int MESSAGE_START = 4;
    private static final String CHANNEL_ID = "aria2service";
    private static final String SERVICE_NAME = "Service for aria2";
    private static final int NOTIFICATION_ID = 69;
    private static final String TAG = Aria2Service.class.getSimpleName();
    private final HandlerThread serviceThread = new HandlerThread("aria2-service");
    private Messenger messenger;
    private LocalBroadcastManager broadcastManager;
    private Aria2 aria2;

    public static void startService(@NonNull Context context) {
        context.startService(new Intent(context,Aria2Service.class).setAction(ACTION_START_SERVICE));
    }

    public static void stopService(@NonNull Context context) {
        context.startService(new Intent(context, Aria2Service.class)
                .setAction(ACTION_STOP_SERVICE));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Prefs.init(this);

        aria2 = Aria2.get();
        aria2.addListener(this);
        serviceThread.start();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            Log.d(TAG, aria2.version());
        } catch (BadEnvironmentException | IOException ex) {
            Log.e(TAG, "Failed getting aria2 version.", ex);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (messenger == null) messenger = new Messenger(new LocalHandler(this));
        return messenger.getBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aria2 != null) aria2.removeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Objects.equals(intent.getAction(), ACTION_START_SERVICE)) {

                try {
                    if (messenger == null) messenger = new Messenger(new LocalHandler(this));
                    messenger.send(Message.obtain(null, MESSAGE_START));
                    return flags == 1 ? START_STICKY : START_REDELIVER_INTENT;
                } catch (RemoteException ex) {
                    Log.e(TAG, "Failed starting executable on service thread!", ex);

                    try {
                        start();
                        return flags == 1 ? START_STICKY : START_REDELIVER_INTENT;
                    } catch (IOException | BadEnvironmentException exx) {
                        Log.e(TAG, "Still failed to start service.", exx);
                    }
                }
            } else if (Objects.equals(intent.getAction(), ACTION_STOP_SERVICE)) {
                stop();
            }
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    private void stop() {
        try {
            aria2.stop();
            stopForeground(true);
            dispatchStatus();
        } catch (RuntimeException ignored) {
        }
    }

    private void start() throws IOException, BadEnvironmentException {
        aria2.start();
        dispatchStatus();

    }

    @Override
    public void onMessage(@NonNull com.gianlu.aria2lib.internal.Message msg) {
        dispatch(msg);
    }

    private void dispatch(@NonNull com.gianlu.aria2lib.internal.Message msg) {
        Intent intent = new Intent(BROADCAST_MESSAGE);
        intent.putExtra("type", msg.type());
        intent.putExtra("i", msg.integer());
        if (msg.object() instanceof Serializable) intent.putExtra("o", (Serializable) msg.object());
        broadcastManager.sendBroadcast(intent);
    }

    private void dispatchStatus() {
        if (broadcastManager == null) return;

        Intent intent = new Intent(BROADCAST_STATUS);
        intent.putExtra("on", aria2.isRunning());
        broadcastManager.sendBroadcast(intent);
    }

    private static class LocalHandler extends Handler {
        private final Aria2Service service;

        LocalHandler(@NonNull Aria2Service service) {
            super(service.serviceThread.getLooper());
            this.service = service;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATUS:
                    service.dispatchStatus();
                    break;
                case MESSAGE_STOP:
                    service.stop();
                    service.stopSelf();
                    break;
                case MESSAGE_START:
                    try {
                        service.start();
                    } catch (IOException | BadEnvironmentException ex) {
                        Log.e(TAG, "Failed starting service.", ex);
                        service.stop();
                        service.stopSelf();
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
