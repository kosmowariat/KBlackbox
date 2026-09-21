package top.niunaijun.blackbox.core;

import top.niunaijun.blackbox.BlackBoxCore;


public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        new CrashHandler();
    }

    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Thread.UncaughtExceptionHandler exceptionHandler = BlackBoxCore.get().getExceptionHandler();
        if (exceptionHandler != null) {
            exceptionHandler.uncaughtException(t, e);
        }
        if (mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            Runtime.getRuntime().exit(10);
        }
    }
}
