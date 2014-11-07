package com.infthink.myflingoffice;

public class LogUtils {
    private static final boolean debug = true;

    public static void d(String tag, String string) {
        if (debug) {
            android.util.Log.d(tag, string);
        }
    }

    public static void v(String tag, String string) {
        if (debug) {
            android.util.Log.v(tag, string);
        }
    }

    public static void e(String tag, String string) {
        if (debug) {
            android.util.Log.e(tag, string);
        }
    }

    public static void e(String tag, String string, Throwable exception) {
        if (debug) {
            android.util.Log.e(tag, string, exception);
        }
    }

}
