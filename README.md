# 工作记录 (WorkTime)

一个**完全离线**的 Android 工时记录应用：在月历上标注每天的上班 / 休息 / 请假，记录多段工作时间与午休，自动汇总成月度工时统计。

- **无网络**：应用不申请任何权限，不联网，不含统计与广告 SDK。
- **无需存储权限**：数据保存在应用私有目录，备份通过系统文件选择器（SAF）导入导出。
- **纯 Kotlin + Jetpack Compose**：单 Activity，Material 3，深色 / 浅色 / 跟随系统。

## 功能特性

- **月历视图**：每月一屏，按状态着色标记每天。
- **每日记录**：
  - 状态：`上班` / `休息` / `请假`
  - 上班可记录多段时间区间（支持上下午分段），自动扣除设定的午休分钟数
  - 可为每天写备注
- **统计视图**：本月出勤天数、休息 / 请假天数、总工时、日均工时，以及近若干个月的工时趋势。
- **支出记录**：一天可记多笔支出（金额、分类、备注），提供当月合计、分类汇总与明细；分类内置餐饮 / 交通 / 购物 / 居住 / 娱乐 / 医疗 / 其他。
- **主题切换**：跟随系统 / 浅色 / 深色。
- **备份与恢复**：导出为 `worktime-yyyyMM.json`，导入时按日期合并（同名日期覆盖，设置项保留本机值），导入前有变更预览。

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose（Material 3）、Compose BOM 2026.02.01 |
| 架构 | 单 Activity + `ViewModel` + `StateFlow`，手写仓库层 |
| 序列化 | kotlinx.serialization（JSON） |
| 构建 | Gradle 9.5.0（Wrapper）、AGP 9.3.2、版本目录 `libs.versions.toml` |
| SDK | minSdk 31 / targetSdk 36 / compileSdk 37 |
| Java | 源码与目标兼容级别 11 |

## 环境要求

- **JDK 17 或更高**（运行 Gradle 所需；应用字节码兼容级别为 Java 11）
- **Android Studio**（最新稳定版）或已配置的 Android SDK / 命令行工具
- Android SDK Platform 37 与对应 Build Tools

## 构建与运行

首次构建前，确保 `local.properties` 中已配置 SDK 路径（Android Studio 会自动生成）：

```properties
sdk.dir=/path/to/Android/Sdk
```

常用命令（Windows 下把 `./gradlew` 换成 `gradlew.bat`）：

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 安装到已连接的设备 / 模拟器
./gradlew installDebug

# 运行单元测试（工时计算逻辑）
./gradlew test

# 运行仪器测试（需连接设备）
./gradlew connectedAndroidTest
```

构建产物位于 `app/build/outputs/apk/`。

## 项目结构

```
app/src/main/java/com/worktime/app/
├── WorkTimeApp.kt          # Application，启动时加载仓库
├── MainActivity.kt         # 唯一 Activity，setContent { AppRoot() }
├── AppViewModel.kt         # 状态与业务编排（保存/删除/导入导出/主题）
├── data/
│   ├── Models.kt           # 数据模型：Store / DayRecord / Segment / Expense / Settings
│   ├── StoreFile.kt        # 单文件原子读写、损坏留档（纯 JVM，可单测）
│   ├── StoreRepository.kt  # 内存态唯一来源，串行化落盘为单个 JSON 文件
│   └── Backup.kt           # 通过 SAF 导入导出、合并策略
├── domain/
│   ├── WorkMath.kt         # 纯 Kotlin 工时计算（不依赖 Android API，便于单测）
│   └── ExpenseMath.kt      # 支出金额解析、月度汇总与导入清洗
└── ui/
    ├── AppRoot.kt          # 根 Composable：顶栏、翻页、底部面板编排
    ├── CalendarScreen.kt   # 月历视图
    ├── StatsScreen.kt      # 统计视图
    ├── DayEditSheet.kt     # 每日编辑面板（工时 + 当日支出）
    ├── SettingsSheet.kt    # 设置面板（主题 / 备份 / 清空）
    ├── ExpenseScreen.kt    # 支出页（月度合计 / 分类汇总 / 明细）
    ├── ExpenseEditDialog.kt # 新增、编辑、删除单笔支出
    ├── ClockPickerDialog.kt
    ├── LaunchArt.kt        # 启动插画
    ├── Components.kt
    └── theme/              # 颜色、字体、主题
```

## 数据与备份

- 全部记录与支出保存在应用私有目录下的单个 JSON 文件，**无任何权限依赖**。
- **卸载应用或清除数据会一并丢失记录**，重装前请先在「设置」中导出备份。
- 导入采用按日期合并策略：备份中的日期覆盖本机同日期记录与支出，应用设置（如主题）保留本机当前值。
- 导入时会跳过无法参与统计的坏记录与非法支出（如净工时为 0 的上班日、金额非正的支出），并提示忽略数量。

### 工时计算规则

- 每天的净工时 = 各段时间区间之和 − 午休分钟数（结果不小于 0）。
- 跨夜区间（结束时间不晚于开始时间）视为非法，不参与计算。
- 只有状态为「上班」且净工时大于 0 的天数计入出勤与总工时。

## 隐私

应用不声明任何权限、不进行任何网络请求，所有数据仅存在于本机。

## 许可证

本项目基于 [MIT License](LICENSE) 开源，Copyright (c) 2026 Ysalu。
