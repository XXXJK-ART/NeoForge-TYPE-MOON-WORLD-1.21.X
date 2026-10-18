# 战斗画面变色与闪屏：移除全屏效果

2026-10-18；Minecraft 1.21.1 / NeoForge 21.1.242 / 64 位 Java 21。

此前只过滤了通用 VFX 的部分颜色效果，仍有多条独立路径可以改变整幅画面：

- `DuelScreenFlashClient` 直接在 HUD 上覆盖白色，对波、村正和 EA 等通过独立网络包触发。
- 泛光从整张主帧缓冲提取亮色，普通加色粒子也会启用它，天空和地面一同参与处理。
- 环境管理器仍会改变雾颜色、雾距离、雨量和雷暴；苍白骑士还有独立的去色后处理。
- 固有时制御直接绘制整屏红色；原版 `FLASH` 粒子会展开为大型亮色面片。
- 技能生成的原版雷电会调用 `Level.setSkyFlashTime`，即使雷电是 `visualOnly` 也会使天空闪白。

本次删除全屏着色器链及其执行器。后处理、环境、去色和对战闪光类仅保留不执行任何操作的兼容入口，不再订阅渲染或雾事件。服务器停止发送对战闪光请求，客户端仍可接收旧包但忽略内容。旧资源包中的 `screen_effects` / `environment` 继续解析，旧 `vfxBloom=true` / `vfxScreenEffects=true` 也不能重新启用这些功能。

97 处 Java 大型闪光粒子请求（56 个文件，含旧 JSON 粒子名称的解析入口）改为小型 `END_ROD` 光点。已有粒子、光束、贴花和材质自身的发光仍在场景几何上绘制。固有时制御保留残影，移除整屏红色。

六处技能雷电生成入口使用已同步的无重力标记。雷电本身不受重力运动影响；客户端 `CombatLightningFlashMixin` 仅跳过带此标记的雷电设置天空闪白的调用，保留雷电形状、寿命、伤害与起火行为。原版缺少单次雷电闪白的专用事件，因此使用这一处限定注入；自然雷电的行为保持原样。

## 验证

- `gradlew.bat test --tests net.xxxjk.TYPE_MOON_WORLD.vfx.* --offline`：12 项通过。
- `gradlew.bat runClient -I tools/client-ui-vfx-smoke.init.gradle --offline`：实际客户端通过。连续 297 帧逐像素采样天空和地面，最大 RGB 通道差为 0；覆盖 11 次重复白闪网络包、14 类屏幕效果、7 类环境效果、技能雷电、旧配置强制开启，以及离屏普通粒子激活的情况。断言测试期间没有菜单遮挡，同时确认普通 VFX 确实有绘制。验证窗口缩放及原有 UI 回归。
- 像素基线排除了存档遗留的夜视药水：原版夜视在即将到期时本来就会闪动，不能把它当作特效染色基线。
- `gradlew.bat test build runGameTestServer -I tools/fatesupport-compat.init.gradle -PgameTestNamespaces=typemoonworld_fatesupport --offline`：508 项单元测试、10 项专用服务器 GameTest 全部通过，构建成功。对应更新了原来强制要求战车生成 `FLASH` 粒子的旧断言。
- 已检查发布 JAR：包含客户端雷电注入及场景材质；不含全屏着色器、旧后处理执行器或烟测模块。

客户端记录：`tmp/combat-screen-client-final.log`；截图：`build/client-ui-vfx-run/qa/screenshots/`。测试模块不会进入发布 JAR。本次覆盖项目默认渲染器及声明的基础依赖，没有使用第三方光影包。
