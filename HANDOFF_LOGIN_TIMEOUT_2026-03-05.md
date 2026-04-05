# ClientFN 项目交接文档（登录超时 / GuiConnecting 崩溃）

生成时间：2026-03-05 09:20（Asia/Shanghai）
编写人：Codex（当前会话）

## 1. 项目概况
- 项目名称：ClientFN（Minecraft 1.7.10 客户端性能/功能增强）
- 目标版本：Minecraft 1.7.10（非 Forge 兼容路线）
- Java：JDK 8
- 启动链：LaunchWrapper + 自定义 `com.clientfn.tweak.ClientFnTweaker`
- 注入框架：SpongePowered Mixin 0.7.11-SNAPSHOT
- 代码目录：`D:\CNE9\project\client_fn`
- 运行目录：`D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN`

## 2. 当前线上（本地运行）状态
- 当前已部署 ClientFN jar：
  - `D:\CNE9\Apps\mc\.minecraft\libraries\com\clientfn\client_fn\0.1.0-SNAPSHOT\client_fn-0.1.0-SNAPSHOT.jar`
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\client_fn-0.1.0-SNAPSHOT.jar`
- 当前部署包哈希（SHA256）：`CA42359799B5E4A6963E32E1C949A181DE8A7337CBED649DE1BF88E12E1B717F`
- 当前启用 mixin（来自运行 jar 中 `mixins.clientfn.json`）：
  - `core.MixinEntityRenderer`
  - `core.MixinMinecraft`
  - `core.MixinGuiConnectingSafety`
  - `core.MixinFontRenderer`
  - `hud.MixinGuiNewChat`
  - `hud.MixinGuiIngameHud`
  - `freelook.MixinEntityTurnInput`
  - `perf.particle.MixinEffectRenderer`
  - `perf.render.MixinRenderGlobal`
  - `perf.render.MixinRenderShadow`

## 3. 当前核心问题

### 问题 A：多人服登录阶段经常卡住，用户侧表现为 `Time out`
- 现象：点击连接后停在 `Connecting to mc31.rhymc.com., 35423`，长时间无进一步状态。
- 日志特征（ClientFN 版本）：
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\logs\latest.log`
  - 仅见 `Connecting to ...`，经常看不到明确断开原因行。

### 问题 B：若禁用 GuiConnecting 防护，会直接崩溃（NPE）
- 崩溃报告：
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\crash-reports\crash-2026-03-05_09.13.48-client.txt`
- 关键堆栈：
  - `java.lang.NullPointerException: Ticking screen`
  - `net.minecraft.client.multiplayer.GuiConnecting.func_73876_c(SourceFile:80)`
- 结论：`GuiConnecting` 的经典 NPE（`getNetHandler()` 为空但仍调用）在该运行环境可稳定出现。

## 4. 已验证对照结果（非常关键）
- 原版 `1.7.10` 在同一机器、同一账号、同一服务器可正常进入。
- 原版日志证据：
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10\logs\latest.log`
  - 01:42:02 开始连接，01:42:05 出现服务器聊天消息（已进服）。
- 这说明：网络环境本身不是唯一问题，ClientFN 启动链/运行时组合存在额外干扰。

## 5. 已做工作与结果（按影响度）
- 已恢复 `GuiConnecting` 安全防护 mixin（`MixinGuiConnectingSafety`）。
  - 作用：避免 `GuiConnecting.func_73876_c` 空指针直接崩溃。
  - 结果：崩溃可避免，但不等价于修复超时根因。
- 已禁用 ClientFN 内部 OptiFine bootstrap（代码层面）。
  - 日志会打印：`OptiFine bootstrap disabled for stability (multiplayer login safeguard).`
- 已尝试移除 `1.7.10-ClientFN.json` 中的 OptiFine library 条目。
  - 文件：`D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\1.7.10-ClientFN.json`
  - 备份：`D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\1.7.10-ClientFN.json.bak_20260305_090752`

## 6. 高置信度异常线索（接手优先看）

### 线索 1：启动命令行里仍然出现 OptiFine jar
- 证据：运行进程命令行（Win32_Process）中 classpath 仍包含：
  - `D:\CNE9\Apps\mc\.minecraft\libraries\optifine\OptiFine\1.7.10_HD_U_E7\OptiFine-1.7.10_HD_U_E7.jar`
- 即便 `1.7.10-ClientFN.json` 已移除该库，仍被注入。
- 推断：Launcher / Wrapper 层存在“额外库注入”机制，未被版本 json 改动覆盖。

### 线索 2：`1.7.10-ClientFN.jar` 与原版 `1.7.10.jar` 哈希不同
- `1.7.10-ClientFN.jar` SHA256：`4B5C3C0C75055F9E56D2573EA582A739A5994354950A9EA2203FC7442A77FA56`
- `1.7.10.jar` SHA256：`A4FC2284657544E0F4BCC964F927C2FDA3E3A205178ED1D5D58883AAF9780CCE`
- 推断：ClientFN 基础游戏 jar 极可能被历史补丁/整合包链路改写过，不是干净原版基底。

## 7. 当前最可能根因（按优先级）
- P0：Launcher/Wrappper 仍在强制注入 OptiFine 或其他外部变换器，导致登录链路行为偏离原版。
- P0：`1.7.10-ClientFN.jar` 基底不是纯净 vanilla 1.7.10 客户端，存在未知补丁，触发 `GuiConnecting` NPE 与连接异常。
- P1：历史诊断 mixin 曾有签名不匹配，说明 1.7.10 运行时签名敏感，错误注入容易造成误导。

## 8. 给下一位接手者的建议执行顺序
- 第一步（必须）：构建“绝对干净对照环境”
  - 新建一个全新版本（不要复用 `1.7.10-ClientFN`），基底直接复制原版 `1.7.10.jar`。
  - 仅加入 `launchwrapper + mixin + client_fn` 三项库。
  - 确认最终 java 命令行 classpath 中 **没有** `OptiFine-*.jar`。
- 第二步：先验证纯登录
  - 临时仅保留 `MixinGuiConnectingSafety`（或甚至零 mixin）验证进服。
- 第三步：逐项恢复功能 mixin
  - 每次只加 1-2 个，复测登录，定位导致超时的具体 mixin/功能模块。
- 第四步：定位 Launcher 注入源
  - 优先检查 PCL/JavaWrapper 的 profile 合成逻辑、全局“附加库/补丁”配置。

## 9. 关键文件索引（交接直接打开）
- 项目源码：
  - `D:\CNE9\project\client_fn\src\main\java\com\clientfn\tweak\ClientFnTweaker.java`
  - `D:\CNE9\project\client_fn\src\main\java\com\clientfn\core\mixin\MixinGuiConnectingSafety.java`
  - `D:\CNE9\project\client_fn\src\main\resources\mixins.clientfn.json`
- 运行配置：
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\1.7.10-ClientFN.json`
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10\1.7.10.json`
- 关键日志与崩溃：
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\logs\latest.log`
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10-ClientFN\crash-reports\crash-2026-03-05_09.13.48-client.txt`
  - `D:\CNE9\Apps\mc\.minecraft\versions\1.7.10\logs\latest.log`

## 10. 现阶段结论
- 当前版本已做到：
  - 避免 `GuiConnecting` NPE 直接崩溃（通过 safety mixin）。
- 但未做到：
  - 根治多人服登录超时。
- 根因大概率不在单一 ClientFN 业务功能，而在“运行时组合链路”（Launcher 注入 + 基底 jar 污染）。
