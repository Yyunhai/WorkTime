# 工作记录 (WorkTime)

一个**完全离线**的 Android 工时与支出记录应用：在月历上标注每天的上班 / 休息 / 请假，记录多段工作时间与午休，自动汇总成月度工时统计；同时可以按天记录多笔支出，查看月度合计与分类汇总。

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
- **支出记录**：一天可记多笔支出（金额、分类、备注），支出页提供当月合计、日均、分类汇总与明细；默认分类为餐饮 / 交通 / 购物 / 其他，历史数据和自定义分类会保留。月历不显示支出，保持工时视图干净。
- **主题切换**：跟随系统 / 浅色 / 深色 / 昔涟；昔涟主题带专属背景与开屏图。
- **备份与恢复**：导出为 `worktime-yyyyMM.json`，导入时记录按日期覆盖、支出按 id 去重追加，设置项保留本机值，导入前有变更预览。

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose（Material 3）、Compose BOM 2026.02.01 |
| 架构 | 单 Activity + `ViewModel` + `StateFlow`，手写仓库层 |
| 本地存储 | Room 2.8.5（KSP）+ DataStore Preferences |
| 数据访问 | DAO 返回 `Flow`，按日期范围 `BETWEEN` 查询；统计在 SQL 里完成 |
| 序列化 | kotlinx.serialization（仅备份 JSON，基于 org.json 做旧数据迁移） |
| 构建 | Gradle 9.5.0（Wrapper）、AGP 9.3.2、版本目录 `libs.versions.toml` |
| SDK | minSdk 23（Android 6）/ targetSdk 36 / compileSdk 37 |
| Java | 源码与目标兼容级别 11 |

## 环境要求

- **JDK 17 或更高**（运行 Gradle 所需；构建工具链为 JDK 21，缺省时由 Gradle 自动解析下载；应用字节码兼容级别为 Java 11）
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

# 运行单元测试（工时/支出计算、备份 JSON、分类映射）
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
├── AppViewModel.kt         # 状态与业务编排（记录/支出/导入导出/主题）
├── data/
│   ├── Models.kt           # 数据模型：DayRecord / Segment / Expense / ExpenseCategory / Settings
│   ├── WorkRepository.kt   # Room + DataStore 的统一入口，组装月度数据
│   ├── SettingsStore.kt    # 主题 / 开屏图开关（DataStore）
│   ├── Backup.kt           # 备份 JSON 的结构与 SAF 读写
│   ├── LegacyJsonImporter.kt # 旧版 data.json 一次性迁移（事务、失败回滚）
│   ├── CategoryMapping.kt  # 旧分类标识与分类名的映射
│   └── db/
│       ├── Entities.kt     # day_record / segment / expense_category / expense
│       ├── Daos.kt         # 日期范围 Flow、统计查询、事务保存
│       └── AppDatabase.kt  # Room 数据库、默认分类、schema 导出
├── domain/
│   ├── WorkMath.kt         # 纯 Kotlin 工时计算（不依赖 Android API，便于单测）
│   ├── ExpenseMath.kt      # 支出金额解析与格式化
│   └── MonthData.kt        # 当前月份的数据快照（记录 / 统计 / 支出）
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

- 工时与支出保存在应用私有目录的 Room 数据库 `worktime.db`，设置保存在 DataStore；不再使用单个 JSON 文件。
- 从旧版本升级时，启动会自动把 `files/worktime/data.json` 迁入 Room；成功后改名为 `data.json.bak`，失败则整体回滚、保留原文件并在下次启动重试。
- 支出金额仍以「分」存整数；旧数据里的 `food` / `shopping` 等分类标识会映射到分类表，无法识别的分类会按原名新建，不会丢数据。
- 导出 / 导入仍是 JSON（`worktime-yyyyMM.json`），结构保持 `settings` / `records` / `expenses`，方便查看和跨版本备份；支出用 `externalId` 去重。
- Room 的 schema 导出在 `app/schemas/`，后续修改表结构时用于编写 Migration。

## 隐私

应用不声明任何权限、不进行任何网络请求，数据仅保存在本机应用私有目录。

## 许可证

本项目基于 [MIT License](LICENSE) 开源，Copyright (c) 2026 Ysalu。
