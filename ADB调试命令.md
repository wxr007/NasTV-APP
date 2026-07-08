# ADB 调试常用命令

## 设备连接

```bash
# 查看已连接的设备
adb devices

# 连接模拟器（如 MuMu、蓝叠等）
adb connect 127.0.0.1:7555        # MuMu 模拟器
adb connect 127.0.0.1:5555        # 蓝叠模拟器
adb connect 127.0.0.1:21503       # 夜神模拟器

# 断开设备
adb disconnect 127.0.0.1:7555

# 指定设备执行命令（多设备时）
adb -s 127.0.0.1:7555 <command>
```

## 安装与卸载

```bash
# 安装 APK
adb install app-debug.apk

# 覆盖安装（保留数据）
adb install -r app-debug.apk

# 卸载应用
adb uninstall com.nastv

# 获取应用包名列表
adb shell pm list packages
adb shell pm list packages | grep nastv
```

## 文件操作

```bash
# 推送文件到设备
adb push local-file.txt /sdcard/Download/

# 从设备拉取文件
adb pull /sdcard/Download/local-file.txt ./

# 进入设备 Shell
adb shell
```

## 日志查看

```bash
# 查看所有日志
adb logcat

# 按包名过滤日志
adb logcat | grep com.nastv

# 按标签过滤日志
adb logcat -s SpeedDebug
adb logcat -s SpeedDebugUI

# 清除日志缓冲区
adb logcat -c

# 保存日志到文件
adb logcat > app.log

# 按优先级过滤（V:Verbose, D:Debug, I:Info, W:Warn, E:Error）
adb logcat *:E                    # 只显示错误日志
adb logcat *:W                    # 显示警告及以上
```

## 屏幕与截屏

```bash
# 截屏
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./

# 录屏
adb shell screenrecord /sdcard/demo.mp4
# Ctrl+C 停止录屏
adb pull /sdcard/demo.mp4 ./
```

## 模拟输入

```bash
# 点击指定坐标
adb shell input tap x y

# 滑动
adb shell input swipe x1 y1 x2 y2

# 输入文本
adb shell input text "hello"

# 按键事件
adb shell input keyevent KEYCODE_BACK        # 返回键 (4)
adb shell input keyevent KEYCODE_HOME        # Home 键 (3)
adb shell input keyevent KEYCODE_MENU        # 菜单键 (82)
adb shell input keyevent KEYCODE_DPAD_UP     # 上 (19)
adb shell input keyevent KEYCODE_DPAD_DOWN   # 下 (20)
adb shell input keyevent KEYCODE_DPAD_LEFT   # 左 (21)
adb shell input keyevent KEYCODE_DPAD_RIGHT  # 右 (22)
adb shell input keyevent KEYCODE_ENTER       # 确认 (66)
adb shell input keyevent KEYCODE_DEL         # 删除 (67)
```

## 应用调试

```bash
# 启动 Activity
adb shell am start -n com.nastv/.MainActivity

# 强制停止应用
adb shell am force-stop com.nastv

# 清除应用数据
adb shell pm clear com.nastv

# 查看 Activity 栈
adb shell dumpsys activity activities | grep com.nastv
```

## 性能与状态

```bash
# 查看设备信息
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release

# 查看 CPU 和内存
adb shell top -n 1 | grep com.nastv

# 查看内存使用
adb shell dumpsys meminfo com.nastv

# 查看电池状态
adb shell dumpsys battery
```

## 无线调试（Android 11+）

```bash
# 1. 设备连接 USB 后开启无线调试端口
adb tcpip 5555

# 2. 断开 USB，通过 IP 连接
adb connect 192.168.1.100:5555
```

## 构建与安装快捷操作

```bash
# 构建并安装（在项目目录下）
cd client-android
./gradlew installDebug

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```
