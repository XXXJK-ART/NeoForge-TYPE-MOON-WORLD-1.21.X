# 从者附属功能整合

目标版本保持 Minecraft 1.21.1、NeoForge 21.1.242、Java 21。选定功能由本体的 `AddonEntrypoint` 初始化，不需要独立安装原来的 Fate Support。

## 本次范围

- 传话石、传话魔术、御主与从者之间的心灵通信，以及 Simple Voice Chat 语音路由。
- 从者卡玩家的灵体化、状态同步、HUD、蹲伏穿墙与战斗限制。
- 兰斯洛特装备库、枪械与载具同化，以及玩家/NPC 的 A-10 兼容功能。
- 人工灵脉在从者卡玩家失去御主后的兜底；单独行动倒计时先运行，之后由有效灵脉接管，持续消耗 5 MP/秒，魔力耗尽后每秒损失 5 HP。失去有效灵脉后恢复本体的失主生存规则。
- Emiya/UBW 的复制限制、轮换装备、无铭弓与幻想崩坏扩展。Ea、王之财宝钥匙不能复制；旧存档中已有的这两类复制记录也不会进入新装备轮换。Excalibur 复制/取用需要 600 MP。
- 红石方块粒子与命中音效的受伤反馈，保留原附属对傀儡、石人、苍白骑士的排除规则。

Nemo、Master Card 奖励、哈桑与狂信子额外调整、XaeroLib 补丁不属于此次迁移。

保留 `fatesupport:message_stone`、`fatesupport:message_magic`、原持久化字段与语音插件 ID，避免无谓更换已有物品和魔术的标识。附属原来依赖本体私有方法的改动继续通过独立 Mixin 配置接入；Superb Warfare 的 Mixin 在目标模组存在时才应用。

## 按键

| 操作 | 默认键 |
| --- | --- |
| 灵体化 | 左 Ctrl |
| 从者闪避 | 左 Alt |
| 魔术模式切换 | 左 Alt，沿用本体原键位，按从者卡状态区分 |
| 魔术轮盘切换 | 右 Alt |
| 心灵通信 | Y，避开原版聊天键 |

这些是默认值，原有 `options.txt` 中玩家保存的自定义键位继续生效。需要时在控制设置中重置相应按键。

## 可选兼容

两个兼容模组均通过 `compileOnly` 编译，不包含在本体发布 JAR 内，也不作为必需运行依赖。

| 安装情况 | 传话语音 | 装备库铁棍 | Superb Warfare 枪械/载具/A-10 |
| --- | --- | --- | --- |
| 都不安装 | 不启用，使用时提示缺少 Voice Chat | 可用 | 不启用 |
| 仅 Voice Chat | 启用 | 可用 | 不启用 |
| 仅 Superb Warfare | 不启用 | 可用 | 启用 |
| 两个都安装 | 启用 | 可用 | 启用 |

验证版本为 Simple Voice Chat `1.21.1-2.6.21`（API `2.6.20`）、Superb Warfare `0.8.9.1-final`、Kotlin for Forge `5.12.0`。Superb Warfare 自身需要的 Kotlin for Forge 和其他依赖仍由它负责。本体的兼容层不代替这些依赖。

请移除独立的 `fatesupport` JAR；同时安装会重复注册相同内容，因此元数据明确声明不兼容。原始附属工程未修改。

## 复现验证

兼容测试使用独立的 `build/fatesupport-runs/<模式>/` 目录，不读取日常开发客户端的 `mods` 和存档。`tools/fatesupport-compat.init.gradle` 的模式支持 `none`、`voice`、`warfare`、`both`。

```powershell
.\gradlew.bat test build
.\gradlew.bat runGameTestServer -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=none -PgameTestNamespaces=typemoonworld_fatesupport
.\gradlew.bat runGameTestServer -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=voice -PgameTestNamespaces=typemoonworld_fatesupport
.\gradlew.bat runGameTestServer -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=warfare -PgameTestNamespaces=typemoonworld_fatesupport
.\gradlew.bat runGameTestServer -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=both -PgameTestNamespaces=typemoonworld_fatesupport
.\gradlew.bat runClient -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=none -PfateSupportClientSmoke=true
.\gradlew.bat runClient -I tools/fatesupport-compat.init.gradle -PfateSupportCompat=both -PfateSupportClientSmoke=true
```

服务器包含 10 项 GameTest，验证可选桥接加载、传话石配对、契约语音路由失效、装备库输入校验、灵体化限制/冷却/隐身恢复、灵脉兜底与单独行动、Emiya 复制装备，以及 A-10 的召唤、归属和冷却。

客户端测试单独编译测试模组，检查实际按键登记和默认值，并打开装备库渲染后自动退出。通过标志为 `FATESUPPORT_CLIENT_SMOKE_PASS`；测试模组不打入本体发布 JAR。

本次验证结果：`test build` 成功，123 个测试套件共 503 项单元测试通过；四种依赖组合各 10 项服务器 GameTest 通过；`none` 与 `both` 两种客户端启动/按键/装备库检查通过。发布 JAR 已检查，不含 Voice Chat、Superb Warfare 的依赖类、Nemo 内容、临时服务文件或客户端测试模组。

自动测试不覆盖两个真实客户端的麦克风收音与听感，也不等同于对旧存档全部内容的实机回归。现有资源中 `durandal_survival` 配方、`bypasses_cooldown` 标签的错误不属于本次整合；Superb Warfare 0.8.9.1 的 `ClientLevelMixin` 在独立服务器上也会输出目标侧别警告。
