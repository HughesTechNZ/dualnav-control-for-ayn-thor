package nz.co.thor.navcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        boolean enabled = context.getSharedPreferences("nav_control", Context.MODE_PRIVATE)
                .getBoolean("reapply_on_boot", false);
        if (!enabled || !DeviceSupport.isSupportedThor(context)) return;

        final PendingResult pending = goAsync();
        new Thread(() -> {
            RootShell.Result result = RootShell.run(NavCommands.APPLY);
            if (result.ok) {
                LowerEdgeBlockService.stop(context);
                context.getSharedPreferences("nav_control", Context.MODE_PRIVATE)
                        .edit().putBoolean("lower_edge_blocker_running", false).apply();
                RootShell.run(NavCommands.RESTART_SYSTEM_UI);
            }
            pending.finish();
        }, "ThorNavBoot").start();
    }
}
