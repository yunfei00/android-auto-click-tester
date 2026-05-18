# android-auto-click-tester

Android UI 自动点击测试工具（仅用于界面压力测试）。项目通过 `AccessibilityService#dispatchGesture` 按配置的时间间隔和屏幕百分比位置执行点击。

## GitHub Actions 状态

![Android CI](https://github.com/<YOUR_ORG>/<YOUR_REPO>/actions/workflows/android.yml/badge.svg)

## 下载 APK

- Debug APK（每次 push main 后）：
  - 进入 GitHub 仓库 **Actions** -> 对应 workflow run -> **Artifacts** 下载 `app-debug-apk`。
- Release APK（打 tag `v*` 后）：
  - 进入仓库 **Releases** 页面下载 `app-release.apk`。

## 如何开启无障碍权限

1. 打开 App。
2. 点击“启动无障碍服务”。
3. 在系统无障碍设置中找到 `Auto Click Tester` 并启用。

## 如何使用

1. 配置“点击间隔 (ms)”、“X 位置 (%)”、“Y 位置 (%)”并保存。
2. 点击“悬浮窗权限检测”并授予权限（如系统要求）。
3. 点击“开始点击”，按保存的配置开始点击。
4. 点击“停止点击”结束任务。

## 免责声明

本项目不包含反作弊绕过、Hook、Root、内存修改等能力，仅用于合法 UI 压力测试。
