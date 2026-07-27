package nz.co.thor.navcontrol;

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
            XposedBridge.log("ThorNavControl hook failed: " + throwable);
        }
    }
}
