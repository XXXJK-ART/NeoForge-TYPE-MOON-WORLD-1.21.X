# 从者运动战与交锋节奏验证

更新日期：2026-10-17。环境：Minecraft 1.21.1、NeoForge 21.1.242、64 位 Java 21。本文记录本次续作后的实现和实测结果。

普通从者对战继续使用接近、交锋、换位、追击和恢复五种状态。高层决策与动作评分间隔为 12 tick，已经选定的前摇、冲刺、防御和击飞物理逐 tick 执行。

- 普攻采用三段节奏：4 tick 前摇；前两击结算后恢复 6 tick，下一击最快相隔 10 tick；第三击后恢复 16 tick，保留收招节拍。伤害仍经原有入口结算一次，沿用现有伤害缩放。
- 近距离走位会保存完整运动方向。后撤执行 8 tick，其他步法通常保持 12 tick；离开近战距离不会立即把后撤翻转为前进。碰墙时仍重新检查安全路线。
- 六 tick 的普通突进按照两 tick 一段执行正斜向、反斜向、正斜向，形成 Z 字路线。每段检查碰撞、地面支撑、液体、区块和世界边界；重复 AI 入口不会重复施加位移。
- 普通对战击飞后等待 8 tick 再开放追击，给击退留出可见距离。低频近战跳劈复用已有动作，每 12 次普通出手才有一次触发机会。
- 格挡带来后滑，连续三次普通格挡压力可触发强击退；完美格挡反推攻击者。这些运动反馈不追加伤害，撞墙和落地继续使用已有破坏、受身和烟尘流程。
- 白刃碰撞增加脚边烟尘和短促爆发；持武器格挡也会产生火花。保留 48 格观察距离、单从者冷却和单维度特效数量限制。
- 飞行安全控制在完整实体 tick 之后执行，避免被角色后续移动覆盖；击飞、撞墙硬直和受身期间由运动状态控制重力与速度。地面高度采样按 12 tick 缓存，地形改变后会重新采样。
- Gilgamesh–Enkidu、固有结界决战、乘坐等特殊阶段保留原控制器。

最终完整工作区的场景结果如下。双方使用原生属性，观察上限为 120 秒。

| 对局 | 种子 | 时长/秒 | 突进 | 击飞/强击退 | 追击 | 最大可见空档/tick |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 阿尔托莉雅 / 库丘林 | 42 | 120 | 175 | 47 | 65 | 0 |
| 阿尔托莉雅 / 库丘林 | 2026 | 120 | 165 | 49 | 63 | 0 |

两局近战均记录到 40 次间隔不超过 12 tick 的普通攻击接续，实际计划动作或原生宝具起手分别为 9 / 7 次。此起手计数不涵盖各角色 helper 内部的全部施法。

接触后最大拉开距离为 15.44 / 14.17 格，最大连续重叠为 3 tick。独立击飞测试改用真实库丘林实体，12 tick 内横向移动 10.67 格；各项距离会受耐力、抗击退、地形和连续控制递减影响。

验证已完成：

- 完整工作区 test check build 通过，503 项单元测试无失败。
- typemoonworld_combat 与 typemoonworld_duels 合并运行，26 项必需 GameTest 全部通过，其中包含 19 项战斗回归与 7 项对局观察。
- 新增回归覆盖 Z 字两次换向、同 tick 重入、跨距离阈值后保持后撤、飞行不覆盖击飞、8 tick 追击间隔，以及连续格挡击退不产生额外伤害。
- 修正物理测试夹具，并区分巨型海魔乘坐/离座后的落地过程与普通击飞限高。
- 客户端运行 560 tick 并检查实战截图，特效预算为 NORMAL，观察到的最大水平速度为 2.22 格/tick，最大击飞轨迹为 11.43 格。此次客户端检查使用主模组；最终服务端回归和构建包含工作区附属模块。
- 已检查发行 JAR 包含交锋服务与两项交锋特效，且不包含临时客户端 QA 类。

为避免与同工作区其他构建干扰，本次将构建输出、Gradle 项目缓存和 GameTest 运行目录切到忽略提交的 tmp/combat-continuation-*。最终完整工作区验证命令为：

    .\gradlew.bat --project-cache-dir tmp/combat-continuation-cache -I tmp/combat-continuation.init.gradle -PincludeWorkspaceAddons test check build --offline
    .\gradlew.bat --project-cache-dir tmp/combat-continuation-cache -I tmp/combat-continuation.init.gradle -PincludeWorkspaceAddons runGameTestServer "-PgameTestNamespaces=typemoonworld_combat,typemoonworld_duels" --offline

初始化脚本仅供本地隔离验证，未修改仓库正式构建配置。日志见 tmp/combat-continuation-build.log、tmp/combat-continuation-final-gametest.log 和 tmp/combat-continuation-client.log；截图见 tmp/combat-continuation-client-qa/screenshots/。项目原有配方、标签、可选兼容类与着色器 uniform 警告仍存在。
