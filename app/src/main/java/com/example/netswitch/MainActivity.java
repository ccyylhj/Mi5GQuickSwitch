package com.example.netswitch;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

/**
 * 自用 5G 优先开关：只读显示 + 跳转，不做自动点、不写 radio。
 *
 * 状态来源（已在 2410DPN6CC 上验证）：
 *  4G优先: preferred_network_mode1=20, allowed types 无 NR
 *  5G优先: preferred_network_mode1=32, allowed types 带 NR
 */
public class MainActivity extends Activity {

    private static final int REQ_READ_PHONE_STATE = 1001;

    private Switch switch5g;
    private TextView tvStatus;
    private TextView tvDetail;
    private boolean lastKnown5G = false;
    private boolean suppressToggle = false;
    private int lastSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switch5g = findViewById(R.id.switch_5g);
        tvStatus = findViewById(R.id.tv_status);
        tvDetail = findViewById(R.id.tv_detail);
        Button btnOpen = findViewById(R.id.btn_open);
        Button btnRefresh = findViewById(R.id.btn_refresh);
        Button btnAddTile = findViewById(R.id.btn_add_tile);
        Button btnNoFreeze = findViewById(R.id.btn_nofreeze);

        // 开关本身不直接改值：点一下就还原显示并跳转系统页
        switch5g.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressToggle) return;
            suppressToggle = true;
            switch5g.setChecked(lastKnown5G);
            suppressToggle = false;
            openNetworkTypePage();
        });
        btnOpen.setOnClickListener(v -> openNetworkTypePage());
        btnRefresh.setOnClickListener(v -> refreshStatus());
        btnAddTile.setOnClickListener(v -> TileHelper.requestAddTile(this));
        btnNoFreeze.setOnClickListener(v -> TileHelper.requestNoFreeze(this));

        ensurePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void ensurePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQ_READ_PHONE_STATE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQ_READ_PHONE_STATE) {
            refreshStatus();
        }
    }

    private void refreshStatus() {
        StringBuilder detail = new StringBuilder();
        try {
            lastSubId = NetMode.getDataSubId(this);
            detail.append("dataSubId=").append(lastSubId).append("; ");
            lastKnown5G = NetMode.is5GFirst(this, lastSubId, detail);
        } catch (Exception e) {
            detail.append("读取失败:").append(e.getMessage());
            lastKnown5G = false;
        }
        suppressToggle = true;
        switch5g.setChecked(lastKnown5G);
        suppressToggle = false;
        tvStatus.setText(lastKnown5G ? "当前：5G 优先" : "当前：4G 优先");
        tvDetail.setText(detail.toString());
    }

    /** 直达当前上网卡的网络类型选择页。 */
    private void openNetworkTypePage() {
        int subId = lastSubId;
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            subId = NetMode.getDataSubId(this);
        }
        Log.i("NetTile", "app openNetworkTypePage subId=" + subId);
        try {
            startActivity(NetMode.networkTypeIntent(subId));
            Log.i("NetTile", "app explicit launched");
        } catch (Exception e) {
            Log.i("NetTile", "app explicit failed: " + e, e);
            try {
                startActivity(NetMode.fallbackIntent(subId));
                Log.i("NetTile", "app fallback launched");
            } catch (Exception e2) {
                Log.i("NetTile", "app fallback failed: " + e2, e2);
            }
        }
    }
}
