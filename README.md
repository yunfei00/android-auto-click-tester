# android-auto-click-tester

Android UI 自动点击测试工具，仅用于合法的界面压力测试。应用通过无障碍服务的 `dispatchGesture` 按配置间隔和屏幕位置执行点击，支持单点或双点独立配置。

## 功能

- 在手机屏幕上直接点选点击位置，自动换算为屏幕百分比坐标。
- 支持点 1 / 点 2 的独立间隔和坐标配置。
- 运行中可通过“停止点击”按钮停止，也可按音量下键停止。
- 可显示红色点 1、蓝色点 2 的点击标记。
- 主界面压缩为一屏，常用配置无需向下滚动。

## 构建

```powershell
.\gradlew.bat :app:assembleRelease
```

生成文件：

- `app/build/outputs/apk/release/Auto-Click-Tester-v0.5.1-release.apk`
- `app/build/outputs/apk/debug/Auto-Click-Tester-v0.5.1-debug.apk`

## GitHub Actions

- 推送 `main` 会构建 Debug APK，并上传 `Auto-Click-Tester-debug-apk` artifact。
- 推送语义化版本 tag（例如 `0.5.1` 或 `v0.5.1`）会构建 Release APK，并创建 GitHub Release 上传 `Auto-Click-Tester-v0.5.1-release.apk`。

## 使用

1. 打开 App，点击“无障碍”，在系统设置中启用 `Auto Click Tester`。
2. 如需屏幕点选或点击标记，点击“悬浮窗”并授予权限。
3. 在“点 1 / 点 2”行配置间隔、X、Y，或点击“选点”后直接在屏幕上点选位置。
4. 需要双点点击时，勾选“启用点 2”。
5. 点击“开始点击”运行，点击“停止点击”或按音量下键停止。

## 免责声明

本项目不包含反作弊绕过、Hook、Root、内存修改等能力，仅用于合法 UI 压力测试。
