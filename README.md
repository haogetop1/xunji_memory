# 循记 0.1.0 源码恢复

循记是一个 Android 闪卡应用。恢复以 `com.xunji.flashcards.debug` 的 0.1.0 调试 APK 为基准。

- `jadx/` 有 205 个 Java 反编译文件、Manifest 和解码后的资源。部分 Compose 和协程代码有反编译占位，可结合 Smali 复核。
- `smali/` 按原 DEX 分组，包含 822 个应用类，可重新汇编。其余依赖类和已编译资源取自原 APK。
- `kotlin-core/` 有可独立编译、验证的 Kotlin 学习逻辑，包括学习队列、限额、计时、输入校验和七天统计。
- `evidence/` 保存原始类索引、数据库建表 SQL、依赖版本和核验结果。

完整的反编译和 Smali 文件位于 [xunji-0.1.0-source-recovery.zip](xunji-0.1.0-source-recovery.zip)。解压后进入压缩包顶层目录运行构建命令。

## 重新生成调试 APK

需要原始 APK、Python 3.11+、JDK 21、JADX headless MCP 0.7.1 JAR 和 Android SDK Build Tools 36.0.0。构建时使用自己的签名文件。环境变量 `JAVA_HOME`、`REA_JADX_MCP_JAR`、`XUNJI_KEYSTORE_PASSWORD`、`XUNJI_KEY_PASSWORD` 由构建者提供。

```sh
python3 tools/build_apk.py base.apk xunji-0.1.0-reassembled-debug.apk \
  --build-tools "$ANDROID_HOME/build-tools/36.0.0" \
  --keystore /path/to/debug.keystore \
  --key-alias androiddebugkey
```

恢复包尚未包含完整的 Kotlin Android 工程。调试 APK 的重编译从 Smali 开始。重编译 APK 若使用不同签名，无法覆盖安装在同包名原版之上。需要保留原版数据时，应先通过原版的备份功能导出数据。
