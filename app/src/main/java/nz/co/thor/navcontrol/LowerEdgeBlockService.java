package nz.co.thor.navcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Collections;

public final class LowerEdgeBlockService extends Service {
    private static final String CHANNEL_ID = "lower_edge_block";
    private static final int NOTIFICATION_ID = 41;
    private WindowManager windowManager;
    private View blocker;

    static void start(Context context) {
        context.startForegroundService(new Intent(context, LowerEdgeBlockService.class));
    }

    static void stop(Context context) {
        context.stopService(new Intent(context, LowerEdgeBlockService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Thor lower-edge blocker")
                .setContentText("Bottom-screen navigation swipes are blocked")
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
        installBlocker();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (blocker == null) installBlocker();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (windowManager != null && blocker != null) {
            try {
                windowManager.removeView(blocker);
            } catch (RuntimeException ignored) {
                // Already detached.
            }
        }
        blocker = null;
        getSharedPreferences("nav_control", MODE_PRIVATE)
                .edit().putBoolean("lower_edge_blocker_running", false).apply();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void installBlocker() {
        DisplayManager displayManager = getSystemService(DisplayManager.class);
        Display secondary = null;
        for (Display display : displayManager.getDisplays()) {
            if (display.getDisplayId() != Display.DEFAULT_DISPLAY) {
                secondary = display;
                break;
            }
        }
        if (secondary == null) {
            stopSelf();
            return;
        }

        Context displayContext = createDisplayContext(secondary);
        windowManager = displayContext.getSystemService(WindowManager.class);
        blocker = new View(displayContext);
        blocker.setBackgroundColor(Color.TRANSPARENT);
        blocker.setOnTouchListener((view, event) -> true);
        blocker.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right > left && bottom > top) {
                view.setSystemGestureExclusionRects(Collections.singletonList(
                        new Rect(0, 0, right - left, bottom - top)));
            }
        });

        int height = Math.round(32 * displayContext.getResources()
                .getDisplayMetrics().density);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM;
        params.setTitle("ThorLowerEdgeBlocker");

        try {
            windowManager.addView(blocker, params);
            getSharedPreferences("nav_control", MODE_PRIVATE)
                    .edit().putBoolean("lower_edge_blocker_running", true).apply();
        } catch (RuntimeException e) {
            blocker = null;
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Lower-edge blocker",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the AYN Thor lower-screen navigation gesture blocked");
        manager.createNotificationChannel(channel);
    }
}
