# 李书文（Assassin）版本适配

本轮按用户已确认方案适配现有版本，不重做招式。保留八极拳动作、圈境、阴阳交错及无二打的既有消耗、冷却和释放机制。

## 身份与参数

- 职阶 Assassin；副属性人；中立·恶。
- 筋力 C、耐久 D、敏捷 A+、魔力 E、幸运 E；宝具参数为无等级。
- 人物特性：男性、人、人型、人科从者、从者、七骑士从者、中立、恶、被 EA 特攻。
- 使用现有 `human`、`human_kind`、`servant`、`seven_knights`、`neutral_alignment`、`evil`、`ea_special` 等键；不把副属性“人”当成活人身份。

## 技能适配

| 技能 | 等级 | 本地形式 |
| --- | --- | --- |
| 老练 | A+ | 常驻被动，零 MP、无冷却 |
| 中国武术（八极拳） | A+++ | 保留现有武术被动及动作来源 |
| 圈境（极） | A- | 保留现有操作技能 |
| 阴阳交错 | B | 保留现有操作技能 |

老练采用用户确认的三类效果和方案默认幅度：击退抗性增加 0.20，移速最终乘 1.05，所受伤害独立乘 0.95。复用固定 ID 的临时属性修饰，重复 tick 不叠加；玩家解除变身或切换从者时移除。NPC 与玩家卡共用数值与结算实现，不修改玩家鼠标转向。

击退抗性遵守 Minecraft 的 0～1 上限。现有从者 NPC 基础值已为 1，因此继续保持满抗性；当其他效果降低其基础抗性时，老练的 0.20 加成仍可生效。

减伤只结算一次，与原有防御独立相乘。例如 100 点符合条件的伤害经老练后为 95；若同时触发现有 NPC 近战减伤，则为 `100 × 0.5 × 0.95 = 47.5`。穿透抗性、无敌或技能防御的伤害不受新增减伤影响。保留原有精神稳定：自身清除反胃、失明、黑暗；每秒清理 20 格内友方从者的同类效果。玩家卡也使用这一实现。

“无二打”继续作为原有宝具操作存在，资源及参数等级留空以显示无等级，不删除宝具，也不添加新的主动槽位。既有气息遮断·圈境登记保留为兼容入口，不追加新技能。

## 卡面

来源：`H:/Minecraft/JAVAmod/资源临时储存/新从者模型/李书文.png`。按现有卡面生成约定适配正面，保留 Assassin 背面、3D 卡片边缘及头部眼镜叠层。

## 验证范围

资源测试覆盖身份、有效特性键、参数、技能等级及无等级宝具；独立 `typemoonworld_li_shuwen` GameTest 覆盖 NPC／玩家卡被动、重复应用与退出清理。实际运行结果以本轮交付说明和构建日志为准。

2026-11-14 实测：李书文资源测试 2 项、独立服务端 GameTest 2 项通过；`build` 成功，JAR 中的卡面与最终源文件 SHA-256 一致，正反面、边缘和头部贴图齐全。适配时发现旧 `BuiltinServantSkills` 门面未被当前初始化流程使用，已在实际 `ServantSkillRegistry.ensureInitialized` 中恢复李书文技能登记，验证旧存档加载后会重新激活老练。

复现命令：

```powershell
.\gradlew.bat test --tests '*LiShuwenResourcesTest' build runGameTestServer -I tools/li-shuwen-validation.init.gradle --project-cache-dir build/li-shuwen-gradle-cache -PgameTestNamespaces=typemoonworld_li_shuwen
```

本轮还执行了全量测试：674 项中 673 项通过，1 项失败来自并行重置中的 `JeanneAlterResetResourcesTest` 宝具等级断言（预期 A+、读取到 A），未据此修改其他角色。验证输出使用独立目录，构建产物位于 `build/li-shuwen-validation/libs/`。

客户端卡片检查也已通过，实际以正常物品渲染器显示新版卡面，截图位于 `build/li-shuwen-card-smoke-run/qa/screenshots/li-shuwen-card.png`。由于随后并行中的黑贞德源码改动暂时无法编译，这次客户端使用上述已成功构建 JAR 的解包快照；校验了烘焙模型、正面 292×500 贴图、背面、卡边与头部叠层，测试模组未进入发布 JAR。可选检查入口为 `tools/li-shuwen-card-smoke.init.gradle`。
