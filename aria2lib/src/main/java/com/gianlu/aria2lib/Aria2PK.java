package com.gianlu.aria2lib;

import android.os.Environment;

import com.gianlu.aria2lib.commonutils.Prefs;

public abstract class Aria2PK {
    private static final boolean DEBUG= true;
    public static final Prefs.KeyWithDefault<Integer> RPC_PORT = new Prefs.KeyWithDefault<>("rpcPort", 6800);
    public static final Prefs.KeyWithDefault<String> RPC_TOKEN = new Prefs.KeyWithDefault<>("rpcToken", "station");
    public static final Prefs.KeyWithDefault<Boolean> RPC_ALLOW_ORIGIN_ALL = new Prefs.KeyWithDefault<>("allowOriginAll", DEBUG);
    public static final Prefs.KeyWithDefault<Boolean> RPC_LISTEN_ALL = new Prefs.KeyWithDefault<>("listenAll", DEBUG);
    public static final Prefs.KeyWithDefault<Boolean> CHECK_CERTIFICATE = new Prefs.KeyWithDefault<>("checkCertificate", false);
    public static final Prefs.KeyWithDefault<String> OUTPUT_DIRECTORY = new Prefs.KeyWithDefault<>("outputPath", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    public static final Prefs.Key CUSTOM_OPTIONS = new Prefs.Key("customOptions");
    public static final Prefs.KeyWithDefault<Boolean> SAVE_SESSION = new Prefs.KeyWithDefault<>("saveSession", true);
    public static final Prefs.Key BARE_CONFIG_PROVIDER = new Prefs.Key("bareConfigProvider");
    public static final Prefs.KeyWithDefault ARIA2_LAST_MD5=new Prefs.KeyWithDefault<>("aria2LastMd5","");
}
