package top.niunaijun.blackbox.utils;

public class StackTraceFilter {
    private static boolean sInstalled = false;

    public static synchronized void install() {
        if (sInstalled) {
            return;
        }
        try {
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                e.setStackTrace(filterStackTrace(e.getStackTrace()));
                if (previous != null) {
                    previous.uncaughtException(t, e);
                }
            });
            sInstalled = true;
        } catch (Throwable ignored) {}
    }

    private static StackTraceElement[] filterStackTrace(StackTraceElement[] stack) {
        return java.util.Arrays.stream(stack)
            .filter(element -> !isSuspicious(element.getClassName()))
            .toArray(StackTraceElement[]::new);
    }

    private static boolean isSuspicious(String className) {
        return className.toLowerCase().contains("xposed") ||
               className.toLowerCase().contains("epic") ||
               className.toLowerCase().contains("virtual") ||
               className.toLowerCase().contains("blackbox") ||
               className.toLowerCase().contains("hook");
    }
}
