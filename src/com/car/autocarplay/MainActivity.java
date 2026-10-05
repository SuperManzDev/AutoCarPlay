package com.car.autocarplay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity implements View.OnClickListener {
    private TextView statusHeader;
    private TextView logView;
    private Handler mainHandler;
    private ScrollView logScrollView;
    private StatusTicker tickerRunnable;

    static class StatusTicker implements Runnable {
        private final MainActivity act;

        StatusTicker(MainActivity act) {
            this.act = act;
        }

        @Override
        public void run() {
            act.refreshStatus();
            act.mainHandler.postDelayed(this, 3000);
        }
    }

    static class ActionRunner implements Runnable {
        private final MainActivity act;
        private final int action;

        ActionRunner(MainActivity act, int action) {
            this.act = act;
            this.action = action;
        }

        @Override
        public void run() {
            switch (action) {
                case 1:
                    act.postLog("Step 1: Enabling Car Hotspot & IPv6 routing...");
                    String hsLog = CarPlayHelper.enableHotspot(act);
                    act.postLog(hsLog);

                    act.postLog("Step 2: Optimizing MT8167B CPU & RAM...");
                    String boostLog = CarPlayHelper.runTurboBoost(act);
                    act.postLog(boostLog);

                    act.postLog("Step 3: Launching MANZ Play CarPlay...");
                    CarPlayHelper.launchCarPlay(act);
                    act.refreshStatus();
                    break;
                case 2:
                    act.postLog("Activating Car Hotspot (SoftAP)...");
                    String res2 = CarPlayHelper.enableHotspot(act);
                    act.postLog(res2);
                    act.refreshStatus();
                    break;
                case 3:
                    act.postLog("Deactivating Car Hotspot...");
                    String res3 = CarPlayHelper.disableHotspot(act);
                    act.postLog(res3);
                    act.refreshStatus();
                    break;
                case 4:
                    act.postLog("Executing Turbo Boost...");
                    String res4 = CarPlayHelper.runTurboBoost(act);
                    act.postLog(res4);
                    act.refreshStatus();
                    break;
                case 5:
                    act.postLog("Re-enabling Wireless ADB port 5555...");
                    String res5 = CarPlayHelper.enableWirelessAdb(act);
                    act.postLog(res5);
                    act.refreshStatus();
                    break;
            }
        }
    }

    static class LogPost implements Runnable {
        private final MainActivity act;
        private final String msg;

        LogPost(MainActivity act, String msg) {
            this.act = act;
            this.msg = msg;
        }

        @Override
        public void run() {
            if (act.logView != null) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                act.logView.append("[" + time + "] " + msg + "\n");
                act.mainHandler.post(new ScrollPost(act));
            }
        }
    }

    static class ScrollPost implements Runnable {
        private final MainActivity act;

        ScrollPost(MainActivity act) {
            this.act = act;
        }

        @Override
        public void run() {
            if (act.logScrollView != null) {
                act.logScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }
    }

    static class StatusPost implements Runnable {
        private final MainActivity act;
        private final String text;

        StatusPost(MainActivity act, String text) {
            this.act = act;
            this.text = text;
        }

        @Override
        public void run() {
            if (act.statusHeader != null) {
                act.statusHeader.setText(text);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());
        tickerRunnable = new StatusTicker(this);

        Intent serviceIntent = new Intent(this, PowerMonitorService.class);
        startService(serviceIntent);

        buildUi();
        postLog("MANZ CarPlay Control Center initialized.");
        postLog("Tablet hardware: MediaTek MT8167B · Android 7.0");
        refreshStatus();
        mainHandler.postDelayed(tickerRunnable, 3000);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0C0F14"));
        root.setPadding(16, 10, 16, 10);

        // Header Bar
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 6);

        TextView title = new TextView(this);
        title.setText("⚡ MANZ PLAY · CARPLAY CONTROL CENTER");
        title.setTextColor(Color.parseColor("#00E5FF")); // Electric Cyan
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        header.addView(spacer, spacerLp);

        statusHeader = new TextView(this);
        statusHeader.setTextColor(Color.parseColor("#00E5FF"));
        statusHeader.setTextSize(10);
        statusHeader.setTypeface(Typeface.MONOSPACE);
        statusHeader.setText("Initializing...");
        header.addView(statusHeader);

        root.addView(header);

        // Main Body (2 Columns)
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        body.setLayoutParams(bodyLp);

        // LEFT COLUMN: Live Console
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f);
        leftCol.setLayoutParams(leftLp);
        leftCol.setBackgroundColor(Color.parseColor("#141922"));
        leftCol.setPadding(10, 8, 10, 8);

        TextView logTitle = new TextView(this);
        logTitle.setText("SYSTEM LOG CONSOLE");
        logTitle.setTextColor(Color.parseColor("#78909C"));
        logTitle.setTextSize(10);
        logTitle.setTypeface(Typeface.DEFAULT_BOLD);
        logTitle.setPadding(0, 0, 0, 4);
        leftCol.addView(logTitle);

        logScrollView = new ScrollView(this);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        logScrollView.setLayoutParams(svLp);

        logView = new TextView(this);
        logView.setTextColor(Color.parseColor("#00E676"));
        logView.setTextSize(10);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setText("Ready.\n");
        logScrollView.addView(logView);
        leftCol.addView(logScrollView);

        body.addView(leftCol);

        // RIGHT COLUMN: Quick Action Buttons
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.35f);
        rightLp.setMargins(10, 0, 0, 0);
        rightCol.setLayoutParams(rightLp);

        // Button 1: Big Launch CarPlay
        Button btnLaunch = createButton("⚡  LAUNCH WIRELESS CARPLAY", "#00E5FF", "#000000", 1, 1.3f, 13, true);
        rightCol.addView(btnLaunch);

        // Row: Hotspot Start / Stop
        LinearLayout hsRow = new LinearLayout(this);
        hsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams hsRowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.95f);
        hsRowParams.setMargins(0, 2, 0, 2);
        hsRow.setLayoutParams(hsRowParams);

        Button btnStartHs = createHalfButton("📶 START HOTSPOT", "#00C853", "#FFFFFF", 2, 11);
        Button btnStopHs = createHalfButton("🛑 STOP HOTSPOT", "#C62828", "#FFFFFF", 3, 11);
        hsRow.addView(btnStartHs);
        hsRow.addView(btnStopHs);
        rightCol.addView(hsRow);

        // Button 4: Turbo Boost
        Button btnBoost = createButton("🚀  TURBO BOOST (OPTIMIZE TABLET)", "#7B1FA2", "#FFFFFF", 4, 0.95f, 11, true);
        rightCol.addView(btnBoost);

        // Button 5: Wireless ADB
        Button btnAdb = createButton("🌐  RE-ENABLE PORT 5555 (WIRELESS ADB)", "#1565C0", "#FFFFFF", 5, 0.95f, 11, true);
        rightCol.addView(btnAdb);

        // Row: Home & Sleep
        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams navRowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.95f);
        navRowParams.setMargins(0, 2, 0, 0);
        navRow.setLayoutParams(navRowParams);

        Button btnHome = createHalfButton("🏠 HOME SCREEN", "#E65100", "#FFFFFF", 6, 11);
        Button btnSleep = createHalfButton("🌙 SLEEP TABLET", "#37474F", "#FFFFFF", 7, 11);
        navRow.addView(btnHome);
        navRow.addView(btnSleep);
        rightCol.addView(navRow);

        body.addView(rightCol);
        root.addView(body);

        setContentView(root);
    }

    private Button createButton(String text, String bgColor, String textColor, final int tag, float weight, int textSize, boolean bold) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.parseColor(bgColor));
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(textSize);
        btn.setPadding(4, 2, 4, 2);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        if (bold) btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTag(tag);
        btn.setOnClickListener(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, weight);
        lp.setMargins(0, 0, 0, 4);
        btn.setLayoutParams(lp);
        return btn;
    }

    private Button createHalfButton(String text, String bgColor, String textColor, final int tag, int textSize) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.parseColor(bgColor));
        btn.setTextColor(Color.parseColor(textColor));
        btn.setTextSize(textSize);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(4, 2, 4, 2);
        btn.setMinHeight(0);
        btn.setMinimumHeight(0);
        btn.setTag(tag);
        btn.setOnClickListener(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        lp.setMargins(2, 0, 2, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    @Override
    public void onClick(View v) {
        int tag = (Integer) v.getTag();
        if (tag >= 1 && tag <= 5) {
            new Thread(new ActionRunner(this, tag)).start();
        } else if (tag == 6) {
            CarPlayHelper.returnHome(this);
        } else if (tag == 7) {
            CarPlayHelper.sleepTablet(this);
        }
    }

    public void refreshStatus() {
        String info = CarPlayHelper.getDeviceInfo(this);
        String net = CarPlayHelper.getNetworkStatus();
        mainHandler.post(new StatusPost(this, info + " | " + net));
    }

    public void postLog(String msg) {
        mainHandler.post(new LogPost(this, msg));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacks(tickerRunnable);
    }
}
