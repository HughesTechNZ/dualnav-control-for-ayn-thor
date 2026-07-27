package nz.co.thor.navcontrol;

import android.content.Context;
import android.provider.Settings;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ThorSwipeHook implements IXposedHookLoadPackage {
    private static final int THOR_LOWER_DISPLAY_ID = 4;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!"android".equals(loadPackageParam.packageName)) {
            return;
        }

        try {
            Class<?> callback = XposedHelpers.findClass(
                    "com.android.server.wm.DisplayPolicy$1",
                    loadPackageParam.classLoader);
            XposedBridge.hookAllMethods(callback, "onSwipeFromBottom",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object displayPolicy =
                                    XposedHelpers.getObjectField(param.thisObject, "this$0");
                            Object displayContent =
                                    XposedHelpers.getObjectField(displayPolicy, "mDisplayContent");
                            int displayId = (int) XposedHelpers.callMethod(
                                    displayContent, "getDisplayId");
                            if (displayId == THOR_LOWER_DISPLAY_ID) {
                                param.setResult(null);
                            }
                        }
                    });
            XposedBridge.log(
                    "ThorNavControl: display-4 bottom swipe hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log("ThorNavControl swipe hook failed: " + throwable);
        }

        try {
            Class<?> displayPolicy = XposedHelpers.findClass(
                    "com.android.server.wm.DisplayPolicy",
                    loadPackageParam.classLoader);
            XposedBridge.hookAllMethods(displayPolicy, "getNavigationBarHeight",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length == 0 || param.args[0] == null) {
                                return;
                            }

                            int displayId = (int) XposedHelpers.callMethod(
                                    param.args[0], "getDisplayId");
                            Context context = (Context) param.args[0];
                            if (displayId == THOR_LOWER_DISPLAY_ID
                                    && isWorkaroundEnabled(context)) {
                                param.setResult(0);
                            }
                        }
                    });
            XposedBridge.hookAllMethods(displayPolicy, "layoutWindowLw",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args.length == 0 || param.args[0] == null) {
                                return;
                            }

                            Object windowState = param.args[0];
                            int displayId = (int) XposedHelpers.callMethod(
                                    windowState, "getDisplayId");
                            String title = String.valueOf(
                                    XposedHelpers.callMethod(windowState, "getLastTitle"));
                            Context context = (Context) XposedHelpers.getObjectField(
                                    param.thisObject, "mContext");
                            if (displayId == THOR_LOWER_DISPLAY_ID
                                    && "NavigationBar4".equals(title)
                                    && isWorkaroundEnabled(context)) {
                                XposedHelpers.setIntField(
                                        windowState, "mRequestedHeight", 0);
                                XposedHelpers.setIntField(
                                        windowState, "mViewVisibility", 8);
                            }
                        }
                    });
            XposedBridge.log(
                    "ThorNavControl: display-4 navigation layout hooks installed");
        } catch (Throwable throwable) {
            XposedBridge.log("ThorNavControl height hook failed: " + throwable);
        }
    }

    private static boolean isWorkaroundEnabled(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(), "hide_nav_bar", 0) == 1;
    }
}
