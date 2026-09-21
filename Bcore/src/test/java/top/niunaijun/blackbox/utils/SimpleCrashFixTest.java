package top.niunaijun.blackbox.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SimpleCrashFixTest {

    private Thread.UncaughtExceptionHandler handlerBeforeTest;

    @Before
    public void resetInstallState() throws Exception {
        handlerBeforeTest = Thread.getDefaultUncaughtExceptionHandler();
        Field installed = SimpleCrashFix.class.getDeclaredField("sIsInstalled");
        installed.setAccessible(true);
        installed.setBoolean(null, false);
    }

    @After
    public void restoreHandler() {
        Thread.setDefaultUncaughtExceptionHandler(handlerBeforeTest);
    }

    private List<Throwable> installAndCapture() {
        List<Throwable> delivered = new ArrayList<>();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> delivered.add(e));
        SimpleCrashFix.installSimpleFix();
        return delivered;
    }

    private static StackTraceElement[] appFrames() {
        return new StackTraceElement[]{
                new StackTraceElement("com.example.app.SyncEngine", "flush", "SyncEngine.java", 91)};
    }

    @Test
    public void unrelatedCrashStillReachesThePreviousHandler() {
        List<Throwable> delivered = installAndCapture();

        ArrayIndexOutOfBoundsException boom = new ArrayIndexOutOfBoundsException("index is out of bounds");
        boom.setStackTrace(appFrames());
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(new Thread(), boom);

        assertEquals(1, delivered.size());
        assertSame(boom, delivered.get(0));
    }

    @Test
    public void crashMentioningContextIsNoLongerSwallowed() {
        List<Throwable> delivered = installAndCapture();

        IllegalStateException boom = new IllegalStateException(
                "Cannot read the view model from a detached context");
        boom.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("android.app.ContextImpl", "getSystemService", "ContextImpl.java", 1408),
                new StackTraceElement("android.app.ContextImpl$ThemeImplOverlay", "applyStyle", "ContextImpl.java", 160)});
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(new Thread(), boom);

        assertEquals("the word context in a message or class name is not a null-Context crash",
                1, delivered.size());
    }

    @Test
    public void nullContextCrashIsSwallowed() {
        List<Throwable> delivered = installAndCapture();

        NullPointerException boom = new NullPointerException(
                "Attempt to invoke interface method 'android.content.res.Resources "
                        + "android.content.Context.getResources()' on a null object reference");
        boom.setStackTrace(appFrames());
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(new Thread(), boom);

        assertEquals(0, delivered.size());
    }

    @Test
    public void googlePlayServicesCrashIsStillSwallowed() {
        List<Throwable> delivered = installAndCapture();

        IllegalStateException boom = new IllegalStateException(
                "A fatal error occurred while checking com.google.android.gms availability");
        boom.setStackTrace(appFrames());
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(new Thread(), boom);

        assertEquals(0, delivered.size());
    }
}
