package nz.co.thor.navcontrol;

import android.content.Context;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

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
                            Context context = (Context) XposedHelpers.getObjectField(
                                    param.thisObject, "mContext");
                            forceApplicationImmersive(windowState, context);
                            int displayId = (int) XposedHelpers.callMethod(
                                    windowState, "getDisplayId");
                            String title = String.valueOf(
                                    XposedHelpers.callMethod(windowState, "getLastTitle"));
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

    private static void forceApplicationImmersive(Object windowState, Context context) {
        if (Settings.Global.getInt(context.getContentResolver(),
                "dualnav_auto_immersive", 0) != 1) return;
        try {
            WindowManager.LayoutParams attrs = (WindowManager.LayoutParams)
                    XposedHelpers.callMethod(windowState, "getAttrs");
            if (attrs.type < WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                    || attrs.type > WindowManager.LayoutParams.LAST_APPLICATION_WINDOW) return;

            String packageName = attrs.packageName;
            if (packageName == null
                    || packageName.equals("android")
                    || packageName.equals("com.android.systemui")
                    || packageName.equals("com.android.settings")
                    || packageName.equals("com.android.launcher3")
                    || packageName.equals("nz.co.thor.navcontrol")) return;

            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
            attrs.systemUiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        } catch (Throwable throwable) {
            XposedBridge.log("ThorNavControl auto immersive failed: " + throwable);
        }
    }
}
