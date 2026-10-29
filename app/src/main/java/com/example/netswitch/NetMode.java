package com.example.netswitch;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

/** 5G/4G 偏好读写小助手：只读状态 + 跳转，不做写 radio。 */
public final class NetMode {

    private NetMode() {}

    /** 当前上网卡 subId，拿不到就返回默认数据卡。 */
    public static int getDataSubId(Context ctx) {
        try {
            int subId = SubscriptionManager.getActiveDataSubscriptionId();
            if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                return subId;
            }
        } catch (SecurityException ignored) {
            // 无 READ_PHONE_STATE 时走 fallback
        }
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }

    /**
     * 是否 5G 优先。优先用 allowed types 的 NR 位（需 READ_PHONE_STATE），
     * 降级看 preferred_network_mode1（RIL 里 23~33 为 NR 段；本机 20=4G / 32=5G）。
     */
    public static boolean is5GFirst(Context ctx, int subId,
                                    StringBuilder detail) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                TelephonyManager tm = ((TelephonyManager)
                        ctx.getSystemService(Context.TELEPHONY_SERVICE))
                        .createForSubscriptionId(subId);
                long allowed = tm.getAllowedNetworkTypesForReason(
                        TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
                boolean hasNr =
                        (allowed & TelephonyManager.NETWORK_TYPE_BITMASK_NR)
                                != 0;
                detail.append("allowedTypes含NR=").append(hasNr);
                return hasNr;
            } catch (SecurityException e) {
                detail.append("allowedTypes无权限，降级读settings; ");
            }
        }
        int mode = Settings.Global.getInt(
                ctx.getContentResolver(), "preferred_network_mode1", -1);
        detail.append("preferred_network_mode1=").append(mode);
        return mode >= 23;
    }

    /** 直达某张卡的网络类型选择页（5G/4G/3G）。 */
    public static Intent networkTypeIntent(int subId) {
        Intent i = new Intent();
        i.setClassName("com.android.phone",
                "com.android.phone.settings.PreferredNetworkTypeListPreference");
        i.putExtra(Settings.EXTRA_SUB_ID, subId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /** 通用移动网络页兜底（个别 ROM 不认上面的类名/extra 时用）。 */
    public static Intent fallbackIntent(int subId) {
        Intent i = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        i.putExtra(Settings.EXTRA_SUB_ID, subId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
