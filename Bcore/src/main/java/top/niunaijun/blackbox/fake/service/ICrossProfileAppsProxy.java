package top.niunaijun.blackbox.fake.service;

import java.lang.reflect.Method;

import black.android.app.BRICrossProfileAppsStub;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.utils.MethodParameterUtils;

public class ICrossProfileAppsProxy extends BinderInvocationStub {
    private static final String TAG = "ICrossProfileAppsProxy";

    public ICrossProfileAppsProxy() {
        super(BRServiceManager.get().getService("crossprofileapps"));
    }

    @Override
    protected Object getWho() {
        return BRICrossProfileAppsStub.get().asInterface(BRServiceManager.get().getService("crossprofileapps"));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("crossprofileapps");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodParameterUtils.replaceFirstAppPkg(args);
        return super.invoke(proxy, method, args);
    }
}