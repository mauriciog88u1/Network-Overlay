package csx55.overlay.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DEBUG {

    public static boolean DEBUG = false;

    public enum DebugLevel {
        INFO,
        WARN,
        ERROR
    }

    public static void debug_print(String message) {
        if (DEBUG) {
            StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
            System.out.println(formatMessage("DEBUG", caller, message));
        }
    }

    private static String formatMessage(String level, StackTraceElement caller, String message) {
        String timestamp = getTimestamp();
        String className = caller.getClassName();
        int lineNumber = caller.getLineNumber();
        return timestamp + " [" + level + "] " + className + ":" + lineNumber + " - " + message;
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
