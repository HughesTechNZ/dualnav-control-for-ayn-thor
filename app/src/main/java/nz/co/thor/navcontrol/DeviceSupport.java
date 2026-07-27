package nz.co.thor.navcontrol;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;

final class DeviceSupport {
    private DeviceSupport() {}

    static boolean isSupportedThor(Context context) {
        String model = Build.MODEL == null ? "" : Build.MODEL.toLowerCase();
        boolean thorModel = model.contains("ayn") && model.contains("thor");
        DisplayManager manager = context.getSystemService(DisplayManager.class);
        boolean secondaryDisplay = false;
        for (Display display : manager.getDisplays()) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                secondaryDisplay = true;
                break;
            }
        }
        return thorModel && secondaryDisplay;
    }

    static String description(Context context) {
        DisplayManager manager = context.getSystemService(DisplayManager.class);
        return Build.MODEL + " · " + manager.getDisplays().length + " display(s)";
    }
}
