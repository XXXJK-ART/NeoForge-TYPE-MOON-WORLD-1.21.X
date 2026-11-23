# 从者横扫与隐含特攻修复记录

日期：2026-10-24。环境：Minecraft 1.21.1、NeoForge 21.1.242、Java 21。

## 原因与复现

这次问题包含伤害计算错误，不只是 NPC 攻速过快。

| 对照项目 | 原始代码 | 修复后 |
| --- | --- | --- |
| 库丘林一次 NPC 普攻打原生迪尔姆德 | 300 → 0 生命 | 300 → 296.1206 生命 |
| 上述攻击的共享公式计算值，尚未进入 NPC 决斗等后续减伤 | 2513.3253 | 29.0566 |
| 玩家对原版坚守者在 16 ticks 内输入 4 次充分蓄力的横扫 | 累计 152 伤害 | 累计 76 伤害 |
| 玩家对人工设为 20 护甲的坚守者，普通攻击／横扫对照 | 31.9200／31.6667，横扫不消耗蓄力 | 31.9200／31.9200，横扫消耗蓄力 |
| 取消一次 NPC 横扫后，同 tick 重复调用攻击入口 | 3 次伤害尝试 | 1 次伤害尝试 |

第一项使用原生生命、护甲和装备。只关闭被击者的反应技能，排除闪避和招架造成的随机性；没有使用宝具或即死。

坚守者原版没有护甲。单次横扫并不在所有场景下都比普通攻击高：该场景的明显差异来自横扫清空受伤无敌帧，允许短时间内每次输入都造成完整伤害。护甲对照单独增加 20 护甲来检查两种攻击是否走同一防御流程。

## 全局排查与修复

1. `ServantCombatService` 的 NPC、玩家两个重载均曾把攻击者的身份标签作为默认特攻条件；共有标签每项再乘 1.5。现在未指定特攻时使用空列表。除横扫外，使用该公式的通用挑飞以及扩展调用也修复了默认行为。
2. `ServantCardTraitService` 还存在按角色 ID 自动追加特攻的旧表，涉及恩奇都、美杜莎、阿尔托莉雅、高文、咒腕哈桑、李书文、佐佐木小次郎。移除此通用附加层；特攻由实际技能、武器或宝具的来源处理。
3. `ServantTraitAttackEvaluator` 对显式条件去重，同一条件重复出现只计算一次。不同的显式条件继续沿用原有独立乘算约定。
4. 宝具 `special_attack_conditions` 原先通过容错的身份解析读取，未知字符串也会变成 `SERVANT`。现在只读取已知特性并去重，技能状态／施放前提等字符串不会凭空产生对从者特攻。

已检查身份读取、特攻计算器的全部调用点，以及独立的目标特性判断。保留武器／技能有来源的判定，例如 Gram 对龙、Harpe 对不死、天下布武、人间理解、龙之魔女和天之锁；它们不根据双方身份交集自动增伤。

## 横扫规则

- 横扫使用普通攻击伤害和正常护甲结算，每个范围内目标只尝试命中一次。
- 保留正常受伤无敌帧，不再在命中前后清空。
- 玩家横扫按普攻蓄力计算伤害，并消耗蓄力；冷却中重复输入不会延后下一次可攻击时间。
- NPC 横扫被挡、取消或处于冷却时，不再回退成另一次普通攻击；公共攻击入口只调用一次。
- 库丘林的专属动作进行期间，公共近战不再额外叠加横扫。
- 按用户后续说明，保留现有推飞与赫拉克勒斯的地形表现；只有有效命中才触发。

未额外修改基础攻速、角色生命、技能／宝具的既有固定伤害或 NPC 决斗减伤系数。

## 验证

- 全部 31 位内置从者逐个检查 NPC 共享公式、玩家共享公式和玩家普通攻击修正：只有身份时不产生特攻，显式条件仍生效。
- 新增 3 项单元测试，覆盖无特攻、重复条件、未知宝具条件。
- 新增 10 项 GameTest 全部通过，含普攻伤害、护甲、蓄力、无敌帧、取消／重复输入、全从者检查及实战。
- 3 组固定随机种子、每组 600 ticks 的原生库丘林对迪尔姆德实战：未发现普攻倍率异常。一次记录到迪尔姆德被 `gae_bulg_projectile` 的爆炸击杀，伤害分类明确为宝具；这与原始代码已经复现的普通横扫秒杀是两条路径。固定种子不保证所有服务器异步调度完全一致，因此不据此宣称胜率或长期平衡已确定。
- 扩大到库丘林、迪尔姆德、招架与本次新增用例，共 101 项 GameTest，97 项通过。以下 4 项失败已用 HEAD 原始实现独立复现，属于已有问题：
  - `servantCardItemAppliesPassivesAndCanTransformAgain`：减益抗性期望为 0.1，实际 0。
  - `stanceStartsDisabledAndTogglesWithoutManaOrWeaponRepair`：模拟玩家未允许接收 `diarmuid_action` 数据包。
  - `singleHandDanceHasThreeHitsAndOnlyDeflectsWeakProjectilesInWindow`：手部参数为 null。
  - `independentHandCooldownsAllowSimultaneousAndOverlappingAttacks`：红枪冷却断言失败。
- 构建和单元测试通过。专用服务器烟测通过；客户端招架烟测通过。库丘林完整客户端旧脚本在与本次横扫无关的飞行宝具启动断言处退出，未将该脚本记为通过。

复跑命令：

```powershell
.\gradlew.bat test build
.\gradlew.bat -I tools/cu-validation.init.gradle runGameTestServer '-PgameTestNamespaces=typemoonworld_basic_balance,typemoonworld_basic_duels'
.\gradlew.bat -I tools/cu-validation.init.gradle runGameTestServer '-PgameTestNamespaces=typemoonworld_basic_balance,typemoonworld_basic_duels,typemoonworld_cu,typemoonworld_diarmuid,typemoonworld_guard'
.\gradlew.bat -I tools/cu-smoke.init.gradle runServer
.\gradlew.bat -I tools/servant-guard-smoke.init.gradle runClient
```

原始对照和修复后的日志保存在 `build/basic-balance-*.log`；对照用旧实现单独解压在 `build/` 下，不覆盖工作区源码。
