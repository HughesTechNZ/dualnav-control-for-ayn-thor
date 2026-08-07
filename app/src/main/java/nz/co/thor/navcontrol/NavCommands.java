package nz.co.thor.navcontrol;

final class NavCommands {
    private NavCommands() {}

    static final String APPLY =
            "cmd overlay enable-exclusive --user 0 --category " +
            "com.android.internal.systemui.navbar.gestural; " +
            "settings put secure navigation_mode 2; " +
            "settings put global hide_nav_bar 1; " +
            "settings put global second_disable_back_gesture 1; " +
            "settings put global policy_control 'immersive.navigation=*'; " +
            "setprop persist.wm.debug.hide_navbar_window false; " +
            "appops set nz.co.thor.navcontrol SYSTEM_ALERT_WINDOW allow";

    static final String RESTORE =
            "settings delete global policy_control; " +
            "settings put global hide_nav_bar 0; " +
            "settings put global second_disable_back_gesture 0; " +
            "setprop persist.wm.debug.hide_navbar_window false; " +
            "cmd overlay enable-exclusive --user 0 --category " +
            "com.android.internal.systemui.navbar.gestural; " +
            "settings put secure navigation_mode 2";

    static final String THREE_BUTTON =
            "settings delete global policy_control; " +
            "settings put global hide_nav_bar 0; " +
            "settings put global second_disable_back_gesture 0; " +
            "setprop persist.wm.debug.hide_navbar_window false; " +
            "cmd overlay enable-exclusive --user 0 --category " +
            "com.android.internal.systemui.navbar.threebutton; " +
            "settings put secure navigation_mode 0";

    static final String STATUS =
            "printf 'root='; id -u; " +
            "printf 'navigation_mode='; settings get secure navigation_mode; " +
            "printf 'hide_nav_bar='; settings get global hide_nav_bar; " +
            "printf 'secondary_disabled='; settings get global second_disable_back_gesture; " +
            "printf 'policy='; settings get global policy_control; " +
            "printf 'auto_immersive='; settings get global dualnav_auto_immersive; " +
            "printf 'hide_navbar_window='; getprop persist.wm.debug.hide_navbar_window";

    static final String AUTO_IMMERSIVE_ON =
            "settings put global dualnav_auto_immersive 1";

    static final String AUTO_IMMERSIVE_OFF =
            "settings put global dualnav_auto_immersive 0";

    static final String RESTART_SYSTEM_UI = "killall com.android.systemui";
}
