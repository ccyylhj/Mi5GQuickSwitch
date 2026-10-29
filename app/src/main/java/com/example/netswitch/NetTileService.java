package com.example.netswitch;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.telephony.SubscriptionManager;
import android.util.Log;

/**
 * 下拉快捷磁贴：亮=5G优先，灭=4G优先。点一下直达当前上网卡的网络类型选择页。
 */
public class NetTileService extends TileService {

    private static final String TAG = "NetTile";
    private static long lastClickMs = 0;

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        StringBuilder detail = new StringBuilder();
        int subId = NetMode.getDataSubId(this);
        boolean is5g;
        try {
            is5g = NetMode.is5GFirst(this, subId, detail);
        } catch (Exception e) {
            is5g = false;
        }
        tile.setState(is5g ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(is5g ? "5G优先" : "4G优先");
        }
        tile.setContentDescription(
                (is5g ? "5G优先" : "4G优先") + "，点击打开网络类型选择");
        tile.updateTile();
    }

    @Override
    public void onClick() {
        long now = System.currentTimeMillis();
        if (now - lastClickMs < 1500) {
            Log.i(TAG, "onClick debounced");
            return;
        }
        lastClickMs = now;
        Log.i(TAG, "onClick enter, locked=" + isLocked());
        int subId = NetMode.getDataSubId(this);
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            subId = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        final int target = subId;
        Log.i(TAG, "onClick target subId=" + target);
        Runnable open = () -> {
            try {
                Log.i(TAG, "try PendingIntent explicit");
                startActivityAndCollapse(toPendingIntent(
                        NetMode.networkTypeIntent(target), 1));
                Log.i(TAG, "explicit launched");
            } catch (Exception e) {
                Log.i(TAG, "explicit failed: " + e, e);
                try {
                    Log.i(TAG, "try PendingIntent fallback");
                    startActivityAndCollapse(toPendingIntent(
                            NetMode.fallbackIntent(target), 2));
                    Log.i(TAG, "fallback launched");
                } catch (Exception e2) {
                    Log.i(TAG, "fallback failed: " + e2, e2);
                }
            }
        };
        if (isLocked()) {
            unlockAndRun(open);
        } else {
            open.run();
        }
    }

    private PendingIntent toPendingIntent(Intent i, int code) {
        // 每次用不同 requestCode，避免连点时互相顶掉
        int unique = (int) (System.currentTimeMillis() & 0xfffffff) + code;
        return PendingIntent.getActivity(this, unique, i,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }
}
