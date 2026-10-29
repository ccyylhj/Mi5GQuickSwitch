package com.example.netswitch;

import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** 下拉磁贴：一键请求添加到快捷开关。 */
public final class TileHelper {

    private TileHelper() {}

    public static void requestAddTile(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            StatusBarManager sbm =
                    ctx.getSystemService(StatusBarManager.class);
            if (sbm == null) {
                Toast.makeText(ctx, "拿不到 StatusBarManager",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            sbm.requestAddTileService(
                    new ComponentName(ctx, NetTileService.class),
                    ctx.getString(R.string.tile_label),
                    Icon.createWithResource(ctx, R.drawable.ic_5g),
                    ctx.getMainExecutor(),
                    result -> {
                        if (result
                                == StatusBarManager
                                        .TILE_ADD_REQUEST_ERROR_APP_NOT_IN_FOREGROUND) {
                            Toast.makeText(ctx, "请回到 App 内再点一次",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // 其他结果（已添加/用户取消）系统自己有提示，无需再弹
                    });
        } else {
            Toast.makeText(ctx,
                    "请下拉通知栏两次 → 点编辑 → 把「5G开关」拖进来",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** 请求忽略电池优化（防 MIUI 冻结磁贴进程，点一次就行）。 */
    public static void requestNoFreeze(android.app.Activity act) {
        try {
            PowerManager pm =
                    act.getSystemService(PowerManager.class);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(
                    act.getPackageName())) {
                Intent i = new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + act.getPackageName()));
                act.startActivity(i);
                return;
            }
        } catch (Exception ignored) {
        }
        // 已经加白或跳不过去时，直接进应用详情让用户手动设省电策略
        try {
            Intent i = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + act.getPackageName()));
            act.startActivity(i);
        } catch (Exception ignored) {
        }
    }
}
