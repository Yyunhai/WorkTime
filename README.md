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
- **支出记录**：一天可记多笔支出（金额、分类、备注），支出页提供当月合计、日均、分类汇总与明细；分类内置餐饮 / 交通 / 购物 / 居住 / 娱乐 / 医疗 / 其他。月历不显示支出，保持工时视图干净。
- **主题切换**：跟随系统 / 浅色 / 深色。
- **备份与恢复**：导出为 `worktime-yyyyMM.json`，导入时按日期合并（同名日期覆盖，设置项保留本机值），导入前有变更预览。

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose（Material 3）、Compose BOM 2026.02.01 |
| 架构 | 单 Activity + `ViewModel` + `StateFlow`，手写仓库层 |
| 并发与存储 | Coroutines + `Mutex` 串行化读写，`fsync` + 原子改名落盘 |
| 序列化 | kotlinx.serialization（JSON） |
| 构建 | Gradle 9.5.0（Wrapper）、AGP 9.3.2、版本目录 `libs.versions.toml` |
| SDK | minSdk 31 / targetSdk 36 / compileSdk 37 |
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

# 运行单元测试（工时/支出计算、文件读写、备份合并）
./gradlew test

# 运行仪器测试（需连接设备）
./gradlew connectedAndroidTest
```

构建产物位于 `app/build/outputs/apk/`。

### 版本号与签名

版本号从 Git 标签推导，`TAG_NAME` 形如 `v1.2.3`：

- `versionName` 取标签去掉 `v` 后的部分；
- `versionCode = major*1_000_000 + minor*1_000 + patch`，保证任意版本都递增（例如 `v1.100 < v2.0`）；
- 需要手动指定时，用环境变量 `VERSION_CODE` 覆盖。

```bash
# macOS / Linux
TAG_NAME=v1.2.0 ./gradlew assembleRelease

# Windows PowerShell
$env:TAG_NAME="v1.2.0"; .\gradlew.bat assembleRelease
```

Release 签名依次从环境变量 `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` 和根目录的 `keystore.properties`（不入库）读取；两处都缺时产出未签名的 `app-release-unsigned.apk`。推送 `v*` 标签会触发 `.github/workflows/release.yml` 自动签名打包并创建 Release。

## 项目结构

```
app/src/main/java/com/worktime/app/
├── WorkTimeApp.kt          # Application，启动时加载仓库
├── MainActivity.kt         # 唯一 Activity，setContent { AppRoot() }
├── AppViewModel.kt         # 状态与业务编排（记录/支出/导入导出/主题）
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

## 测试

`./gradlew test` 覆盖不依赖 Android 的纯 JVM 逻辑：

- `WorkMathTest`：工时计算、记录校验、月度汇总；
- `ExpenseMathTest`：金额解析与格式化、分类汇总、导入清洗；
- `StoreFileTest`：原子写入、覆盖写入、损坏归档；
- `BackupTest`：导入预览计数与合并策略。

## 数据与备份

- 全部记录与支出保存在应用私有目录的 `files/worktime/data.json`，**无任何权限依赖**。
- 落盘采用「临时文件 → `fsync` → 原子改名」；写入失败会回滚内存态，磁盘上的旧文件保持不动。
- 启动时如果发现文件损坏，会把它改名为 `data.corrupt-yyyyMMdd-HHmmss-SSS.json` 留档，以空数据启动并提示；文件读不了（权限 / IO 异常）时会暂时禁止保存，避免覆盖。
- **卸载应用或清除数据会一并丢失记录**，重装前请先在「设置」中导出备份。
- 备份文件同时包含工时记录与支出；导入按日期覆盖本机同日期内容，应用设置（如主题）保留本机当前值。
- 导入时会跳过无法参与统计的坏记录与非法支出（如净工时为 0 的上班日、金额非正的支出），并提示忽略数量。

### 工时计算规则

- 每天的净工时 = 各段时间区间之和 − 午休分钟数（结果不小于 0）。
- 跨夜区间（结束时间不晚于开始时间）视为非法，不参与计算。
- 只有状态为「上班」且净工时大于 0 的天数计入出勤与总工时。

### 支出规则

- 金额以「分」为单位存整数，避免浮点误差；输入支持 `12`、`12.3`、`12.34`，0、负数、超过两位小数会被拒绝。
- 单笔上限 1000 万元。
- 支出与工时记录相互独立：删除当天工作记录不会删除支出；「清空全部数据」会把两者一起清空。
- 支出页可按月查看合计、日均、分类汇总与逐笔明细，点明细可编辑或删除。

## 隐私

应用不声明任何权限、不进行任何网络请求。数据保存在应用私有目录，系统自动备份（如 Google 云备份）是否包含它取决于设备的备份设置；不需要时可在系统设置中关闭备份。

## 许可证

本项目基于 [MIT License](LICENSE) 开源，Copyright (c) 2026 Ysalu。
