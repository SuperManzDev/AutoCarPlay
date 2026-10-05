package com.car.autocarplay;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class PowerMonitorService extends Service {
    private PowerBroadcastReceiver receiver;
    private Handler handler;
    private AutoLaunchRunnable autoLaunchRunnable;
    private SleepRunnable sleepRunnable;

    static class PowerBroadcastReceiver extends BroadcastReceiver {
        private final PowerMonitorService service;

        PowerBroadcastReceiver(PowerMonitorService service) {
            this.service = service;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || service == null) return;
            String action = intent.getAction();
            service.handlePowerEvent(action);
        }
    }

    static class AutoLaunchRunnable implements Runnable {
        private final Context ctx;

        AutoLaunchRunnable(Context context) {
            this.ctx = context;
        }

        @Override
        public void run() {
            CarPlayHelper.applyPowerConnected(ctx);
            CarPlayHelper.enableHotspot(ctx);
            CarPlayHelper.runTurboBoost(ctx);
            CarPlayHelper.launchCarPlay(ctx);
        }
    }

    static class SleepRunnable implements Runnable {
        private final Context ctx;

        SleepRunnable(Context context) {
            this.ctx = context;
        }

        @Override
        public void run() {
            CarPlayHelper.sleepTablet(ctx);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        autoLaunchRunnable = new AutoLaunchRunnable(getApplicationContext());
        sleepRunnable = new SleepRunnable(getApplicationContext());
        receiver = new PowerBroadcastReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(receiver, filter);
    }

    public void handlePowerEvent(String action) {
        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            handler.removeCallbacks(sleepRunnable);
            handler.postDelayed(autoLaunchRunnable, 1000);
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            handler.removeCallbacks(autoLaunchRunnable);
            handler.postDelayed(sleepRunnable, 3000);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
