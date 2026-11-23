# 循记 0.1.0 Kotlin 核心逻辑恢复

从调试 APK 的 Smali 手工重建了五组纯 Kotlin/JVM 逻辑。应用 ID 为 `com.xunji.flashcards.debug`。取证 APK 的 SHA-256 为 `5747b7c062123a005d8c649a5d7941ecb9eb75f095b6178bf64476d4c78516c6`。

| 已重建逻辑 | 原 APK 证据 |
| --- | --- |
| 到期优先、学习阶段排序、新卡顺序和等待状态 | `classes3.dex` 的 `QueuePlanner.next` 及五个内联比较器 |
| 首次复习计入当天新卡限额，按本地日界计算 | `classes3.dex` 的 `NewCardQuota.remaining`，`classes5.dex` 的 `LocalDay.bounds` |
| 学习计时、暂停、恢复及 300 秒上限 | `classes3.dex` 的 `SessionTimer` |
| 卡组和卡片输入校验，按 Unicode 码点计数 | `classes3.dex` 的 `InputValidator` |
| 最近七天统计、撤销过滤、评分次数、去重卡数和时长 | `classes7.dex` 的 `StudyStatistics.summarize` |

纯 JVM 验证已通过。覆盖了到期时间边界、剩余限额为零、夏令时的 23 小时日期、计时状态转换、Unicode 代理对、撤销的评分过滤及空统计。

已有 Kotlin 2.x 命令行编译器和 Java 17 或更高版本时，可从本目录执行：

```sh
find src -name '*.kt' -print0 | xargs -0 kotlinc -include-runtime -d xunji-recovery-check.jar
java -jar xunji-recovery-check.jar
```

这份代码保留了已重建逻辑的包名与数据字段。Android 界面、Room、FSRS 参数计算、Anki 导入、备份以及 Kotlin 序列化生成代码均待整合。原 APK 的安装行为和完整工程构建尚未对照验证。
