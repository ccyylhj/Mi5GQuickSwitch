# 5G开关（NetSwitch）

小米手机经常要在 4G / 5G 优先之间切换，从设置里一步步点进去很繁琐。
这个 App 加一个开关，一键直达当前上网卡的「网络类型选择」页，顺手还能在 App 和下拉磁贴上看到当前是 5G 优先还是 4G 优先，省了很多步骤。

> 自用项目，无需 root、无需 Shizuku。在小米 2410DPN6CC（HyperOS）上实测通过。

## 功能

- App 内开关：显示当前偏好（5G 优先 / 4G 优先），点一下直达当前上网卡的网络类型选择页（5G/4G/3G），改完回来自动刷新
- 下拉快捷磁贴：亮 = 5G 优先，灭 = 4G 优先，点一下同样直达（需在 App 里点「添加到下拉快捷开关」或手动去编辑页拖入）
- 状态来源：`allowed types` 的 NR 位（`TelephonyManager.getAllowedNetworkTypesForReason(REASON_USER)`），低版本降级读 `preferred_network_mode1`（本机实测 20=4G / 32=5G）
- 防冻结加白按钮：MIUI 会冻结后台应用导致磁贴点不动，点一次申请忽略电池优化（另需手动把省电策略设为「无限制」）

## 能做什么 / 不能做什么

- 能：只读显示 + 跳转系统页。普通 App 没有 `MODIFY_PHONE_STATE` 权限，做不到静默切换，必须由用户在系统页点一下
- 不能：不写 radio、不碰 `preferred_network_mode`，不断网风险

## 安装

到 [Releases](../../releases) 下载 `5G开关-vX.Y.apk`，用文件管理器点开安装即可。
首次打开允许「电话」权限（用来读上网卡和偏好状态）。

## 从源码构建

需要 JDK 17 + Android SDK（compileSdk `android-34-ext12`，build-tools 34.0.0）：

```bat
set JAVA_HOME=<你的JDK17>
set ANDROID_SDK_ROOT=<你的SDK>
gradle assembleDebug
```

产物在 `app/build/outputs/apk/debug/app-debug.apk`。

## 实现要点

- 定向跳转：`Settings.ACTION_NETWORK_OPERATOR_SETTINGS` + `Settings.EXTRA_SUB_ID`；网络类型页用显式 Component `com.android.phone/.settings.PreferredNetworkTypeListPreference`，失败回退到通用移动网络页
- 磁贴点击走 `TileService.startActivityAndCollapse(PendingIntent)`（HyperOS 禁了 `Intent` 重载，会抛 `UnsupportedOperationException`）
- 磁贴防连击：1.5s 内重复点击丢弃（MIUI 会一次点发多次 `onClick`）

## License

MIT
