package com.car.autocarplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    static class Runner implements Runnable {
        private final Context ctx;
        private final String action;

        Runner(Context context, String action) {
            this.ctx = context;
            this.action = action;
        }

        @Override
        public void run() {
            try {
                if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                    "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action) ||
                    "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
                    Thread.sleep(5000);
                }

                Intent serviceIntent = new Intent(ctx, PowerMonitorService.class);
                ctx.startService(serviceIntent);

                if (CarPlayHelper.isPowerConnected(ctx)) {
                    CarPlayHelper.applyPowerConnected(ctx);
                    CarPlayHelper.enableHotspot(ctx);
                    CarPlayHelper.runTurboBoost(ctx);
                    CarPlayHelper.launchCarPlay(ctx);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        new Thread(new Runner(context, action)).start();
    }
}
