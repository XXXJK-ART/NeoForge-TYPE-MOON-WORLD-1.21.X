# 客户端 UI、从者按键与天空渲染修复

验证日期：2026-10-18。环境为 Minecraft 1.21.1、NeoForge 21.1.242、64 位 Java 21，使用项目原有依赖版本。

## 天空异常的原因与修复

在隔离客户端中，使用同一白色粒子特效对照泛光开关，成功复现开启泛光时天空出现大片黑色区域、关闭后恢复的情况。

原来的三个后处理链最终都使用原版 `blit`，其混合方式是 `SRC_ALPHA / ONE_MINUS_SRC_ALPHA`。世界帧缓冲的 Alpha 不表示画面像素是否有效，天空像素可以具有零 Alpha；把完整画面按透明图层拷回已经清空的主目标会丢失这些像素的颜色。

泛光、通用屏幕特效和苍白骑士去色现在统一使用 `typemoonworld:vfx_copy`，以 `ONE / ZERO` 完整替换目标 RGB 并写入不透明 Alpha。处理前明确设置替换混合状态，避免 `EffectInstance` 的混合缓存受前一帧绘制状态影响。处理期间禁止深度写入，保护原画面的深度数据；结束时恢复混合、深度和活动纹理单元，并重新绑定主目标。

泛光中间目标随主画面一起调整尺寸，通过 `PostChain.resize` 更新整个链的投影。去色链重新创建时重置尺寸缓存，保证首次处理前已经设置投影。

## UI 和输入

- 普通玩家与从者的左下角魔力条使用更明亮的青蓝渐变；低魔力与超额魔力保留各自的警示颜色。
- 普通轮盘页默认左 Alt、魔术模式保持原来的左 Alt；从者技能选择独立为 Z + 滚轮，释放选中技能独立为 R。
- 从者防御默认鼠标中键、闪避左 Alt、切换飞行 G、独立技能配置 O；另提供默认不绑定的蓄力跳跃／飞行冲刺单键。原来的组合操作保留，移动、跳跃、潜行改为读取原版可配置映射。
- 灵体化与心灵通信保持原有按键标识和自定义配置，分类统一为 `Type Moon World`。补全闪避等新增条目的简体中文、繁体中文与英文翻译。
- 普通形态和从者形态使用独立输入上下文，避免共享默认键位积压另一种形态的点击事件。
- O 界面改为自适应窗口和可滚动技能表，显示技能名、点按／按住和独立键位。支持长名称提示、等待按键状态、冲突反馈、Esc 取消、Delete 清除以及恢复继承全局键位。基础操作冲突按实际改键结果判断，不再写死 R、O 或 Alt。

已有 `options.txt` 中的配置继续生效。若已有配置仍使用旧的默认左右 Alt，可在「选项 → 控制 → 按键绑定」里重置对应条目。

## 验证结果

1. `gradlew.bat test build --offline`：通过，123 个测试套件、503 项测试，零失败。
2. `gradlew.bat runClient -I tools/client-ui-vfx-smoke.init.gradle --offline`：通过，日志标志 `CLIENT_UI_VFX_SMOKE_PASS`。检查了泛光、活动特效期间调整窗口、通用屏幕效果、去色、Ea 和从者交锋材质的实际天空像素；检查了两种 GUI 缩放、列表滚动、组件边界、保留按键提示、全局键位继承、重复键处理、Z 技能选择以及 R 在不同形态的分流。
3. `gradlew.bat runClient -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=none -PfateSupportClientSmoke=true --offline`：通过，日志标志 `FATESUPPORT_CLIENT_SMOKE_PASS`；检查新增默认键、注册和合并后的分类。
4. 使用全新的 `build/client-ui-vfx-server` 目录运行 `runGameTestServer -PgameTestNamespaces=typemoonworld_fatesupport`：10 项必需测试全部通过，无客户端类被专用服务器加载的问题。
5. `tools/validate_lang_keys.ps1`：简中和英文键集合一致。发布 JAR 已检查包含新增着色器与处理类，未包含任何客户端测试模块或临时 QA 类。

截图位于 `build/client-ui-vfx-run/qa/screenshots/`；运行记录位于 `tmp/ui-vfx-regression-final.log`、`tmp/ui-vfx-server.log`、`tmp/ui-vfx-scoped-build.log` 和 `tmp/ui-vfx-final-verification.log`。两个客户端测试模块均使用独立 source set，不进入发布 JAR。

验证覆盖原版渲染器和项目声明的基本依赖。本次未运行第三方光影包或其他替换渲染器的组合。
