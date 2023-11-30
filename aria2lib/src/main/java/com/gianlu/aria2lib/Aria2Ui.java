package com.gianlu.aria2lib;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.gianlu.aria2lib.commonutils.Md5CheckSum;
import com.gianlu.aria2lib.commonutils.Prefs;
import com.gianlu.aria2lib.internal.Aria2;
import com.gianlu.aria2lib.internal.Aria2Service;
import com.gianlu.aria2lib.internal.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Aria2Ui {
    public static final int MAX_LOG_LINES = 100;
    private static final String TAG = Aria2Ui.class.getSimpleName();
    private final Aria2 aria2;
    private final Context context;
    private final Listener listener;
    private final LocalBroadcastManager broadcastManager;
    private final List<LogMessage> messages = new ArrayList<>(MAX_LOG_LINES);
    private final ServiceBroadcastReceiver receiver;
    private Messenger messenger;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);

            askForStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
        }
    };

    public Aria2Ui(@NonNull Context context, @Nullable Listener listener) {
        this.context = context;
        this.listener = listener;
        this.aria2 = Aria2.get();
        this.broadcastManager = LocalBroadcastManager.getInstance(context);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Aria2Service.BROADCAST_MESSAGE);
        filter.addAction(Aria2Service.BROADCAST_STATUS);
        broadcastManager.registerReceiver(receiver = new ServiceBroadcastReceiver(), filter);
    }

    public static void provider(@NonNull Class<? extends BareConfigProvider> providerClass) {
        Prefs.putString(Aria2PK.BARE_CONFIG_PROVIDER, providerClass.getCanonicalName());
    }


    public void bind() {
        if (messenger != null) return;

        context.bindService(new Intent(context, Aria2Service.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void finalize() {
        if (receiver != null) broadcastManager.unregisterReceiver(receiver);
    }

    public void unbind() {
        if (messenger == null) return;

        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void askForStatus() {
        try {
            if (messenger != null)
                messenger.send(android.os.Message.obtain(null, Aria2Service.MESSAGE_STATUS));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed sending message (ask for status).", ex);
        }
    }

    public void loadEnv(@NonNull Context context) throws BadEnvironmentException, IOException, NoSuchAlgorithmException {
        File parent = context.getFilesDir();
        if (!checkAria2ExecFileExist() || !isAssetMd5MatchesLastCopiedFileMd5(context.getAssets().open("aria2c"))) {
            installExecFromAssets();
        }
        aria2.loadEnv(parent, new File(parent, "aria2cExec/aria2c"), new File(parent, "session"));
    }

    private boolean isAssetMd5MatchesLastCopiedFileMd5(InputStream aria2c) throws NoSuchAlgorithmException, IOException {
        return Prefs.getString(Aria2PK.ARIA2_LAST_MD5).equalsIgnoreCase(Md5CheckSum.checkMd5Sum(aria2c));
    }

    private void installExecFromAssets() throws NoSuchAlgorithmException {
        try {
            File parent = context.getFilesDir();
            File execDir = new File(parent, "aria2cExec");
            if (!execDir.exists()) {
                execDir.mkdirs();
            }
            File execFile = new File(execDir, "aria2c");
            if (!execFile.exists()) {
                execFile.createNewFile();
            }
            InputStream assertFileInputStream = context.getAssets().open("aria2c");
            OutputStream fos = new FileOutputStream(execFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = assertFileInputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            fos.close();
            assertFileInputStream.close();
            Prefs.putString(Aria2PK.ARIA2_LAST_MD5, Md5CheckSum.checkMd5Sum(execFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Boolean checkAria2ExecFileExist() {
        File file = new File(context.getFilesDir(), "aria2cExec/aria2c");
        return file.exists();
    }

    @NonNull
    public String version() throws IOException, BadEnvironmentException {
        return aria2.version();
    }

    public void startService() {
        bind();

        try {
            Aria2Service.startService(context);
        } catch (SecurityException ex) {
            if (listener != null) {
                publishMessage(new LogMessage(Message.Type.PROCESS_ERROR, 0, ex.getMessage()));
                listener.updateUi(false);
            }

            Log.e(TAG, "Failed starting service.", ex);
        }
    }

    public void startServiceFromReceiver() {
        try {
            Aria2Service.startService(context);
        } catch (SecurityException ex) {
            if (listener != null) {
                publishMessage(new LogMessage(Message.Type.PROCESS_ERROR, 0, ex.getMessage()));
                listener.updateUi(false);
            }

            Log.e(TAG, "Failed starting server (from receiver).", ex);
        }
    }

    public void stopService() {
        if (messenger != null) {
            try {
                messenger.send(android.os.Message.obtain(null, Aria2Service.MESSAGE_STOP));
                return;
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed stopping service.", ex);
            }
        }

        Aria2Service.stopService(context);
    }

    public boolean delete() {
        return aria2.delete();
    }

    public boolean hasEnv() {
        return aria2.hasEnv();
    }

    @UiThread
    public void updateLogs(@NonNull Listener listener) {
        listener.onUpdateLogs(Collections.unmodifiableList(messages));
    }

    private void publishMessage(@NonNull LogMessage msg) {
        if (msg.type != Message.Type.MONITOR_UPDATE) {
            if (messages.size() >= MAX_LOG_LINES)
                messages.remove(0);

            messages.add(msg);
        }

        if (listener != null) listener.onMessage(msg);
    }

    @UiThread
    public interface Listener {
        void onUpdateLogs(@NonNull List<LogMessage> msg);

        void onMessage(@NonNull LogMessage msg);

        void updateUi(boolean on);
    }

    public static class LogMessage {
        public final Message.Type type;
        public final int i;
        public final Serializable o;

        private LogMessage(@NonNull Message.Type type, int i, @Nullable Serializable o) {
            this.type = type;
            this.i = i;
            this.o = o;
        }

        @Override
        public String toString() {
            return "LogMessage{" +
                    "type=" + type +
                    ", i=" + i +
                    ", o=" + o +
                    '}';
        }
    }

    private class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Aria2Service.BROADCAST_MESSAGE)) {
                Message.Type type = (Message.Type) intent.getSerializableExtra("type");
                if (type == null) return;

                int i = intent.getIntExtra("i", 0);
                Serializable o = intent.getSerializableExtra("o");
                publishMessage(new LogMessage(type, i, o));
            } else if (Objects.equals(intent.getAction(), Aria2Service.BROADCAST_STATUS)) {
                if (listener != null) listener.updateUi(intent.getBooleanExtra("on", false));
            }
        }
    }
}
