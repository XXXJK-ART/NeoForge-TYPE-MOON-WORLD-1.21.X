# 从者与 AI 制作流程指南

这份文档给后续新从者制作用，目标是先做出一个能生成、能显示、能战斗的最小闭环，再逐步补技能、宝具、AI 特化逻辑和特效。

## 1. 先理解整体结构

一个完整从者通常由这些部分组成：

| 部分 | 作用 | 是否必做 |
| --- | --- | --- |
| 实体类 | 游戏里真正生成的从者实体，继承 `ServantEntity` | 必做 |
| GEO 模型 | GeckoLib 模型、骨骼、动画 | 必做 |
| 渲染器 | 把实体和模型挂起来 | 必做 |
| 实体注册 | 在 `ModEntities` 注册 | 必做 |
| 属性注册 | 在 `ModEventBusEvents` 注册基础属性 | 必做 |
| 客户端注册 | 在 `TypeMoonWorldClientEvents` 注册 renderer | 必做 |
| 定义 JSON | 从者面板、模型路径、AI 入口、刷怪蛋颜色等 | 必做 |
| 刷怪蛋 | 方便测试和创造模式使用 | 强烈建议 |
| 语言文件 | 显示名称、刷怪蛋名 | 强烈建议 |
| 技能 JSON | 常驻或主动技能数据 | 复杂从者建议 |
| AI/Combat Helper | 专属战斗逻辑和阶段逻辑 | 复杂从者建议 |
| 宝具/实体 | 新投射物、召唤物、领域实体等 | 视需求 |

最稳的顺序是：
1. 先做实体和 GEO 模型。
2. 再接注册和刷怪蛋。
3. 再写定义 JSON。
4. 先跑通普通攻击和基础 AI。
5. 最后加小技能、宝具、领域和花活。

## 2. 命名规则

建议保持一套固定命名：

| 用途 | 写法 |
| --- | --- |
| 数据 id | `oda_nobunaga` |
| Java 类 | `OdaNobunagaEntity` |
| 常量 | `ODA_NOBUNAGA` |
| 定义 JSON | `oda_nobunaga.json` |
| 模型文件 | `geo/oda_nobunaga.geo.json` |
| 贴图 | `textures/entity/oda_nobunaga.png` |
| 动画 | `animations/oda_nobunaga.animation.json` |
| 刷怪蛋 | `oda_nobunaga_spawn_egg` |

原则：
- JSON id 必须和实体里的 `SERVANT_KEY` 完全一致。
- namespace 通常就是 `typemoonworld`。
- 资源路径、动画 key、骨骼名都要严格对上，不能凭感觉写。

## 3. GEO 模型优先

这个项目里，大部分从者都是 GEO 模型，不是普通物品模型。

你需要提前确认：
- `geo` 文件已经导出。
- `animation` 文件和骨骼名对得上。
- 站立、行走、攻击、技能、宝具等动作都能在同一个骨架里切换。

尤其要注意：
- `standing` 往往不是“一个普通待机动作”，而是模型基础姿态和缩放修正。
- 这个基础修正要尽量在所有动作里保留，不然会出现某些动作里身体比例不对、部件偏移、手臂穿模。
- 像信长这种角色，头发这类骨骼最好单独做抬头反向补偿，避免穿模。

如果模型分组比较复杂，优先先保证：
1. 身体比例正确。
2. 头、手、武器、头发能正常跟随。
3. 再去做更细的动作分层。

## 4. 最小实体骨架

实体类建议先只做三件事：

```java
public class ExampleServantEntity extends ServantEntity {
   public static final String SERVANT_KEY = "example_servant";

   public ExampleServantEntity(EntityType<ExampleServantEntity> type, Level level) {
      super(type, level, SERVANT_KEY);
   }
}
```

后续再按需要加：
- `customServerAiStep()` 调用专属 CombatHelper
- 死亡、维度切换、消失时清理特效实体
- 对火焰、岩浆、掉落伤害等特殊免疫

对应模型类通常是：

```java
public class ExampleServantModel extends BaseServantModel<ExampleServantEntity> {
   public ExampleServantModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/example_servant.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/example_servant.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/example_servant.animation.json")
      );
   }
}
```

对应 renderer 通常是：

```java
public class ExampleServantRenderer extends BaseServantRenderer<ExampleServantEntity> {
   public ExampleServantRenderer(Context renderManager) {
      super(renderManager, new ExampleServantModel(), 0.9F);
   }
}
```

`0.9F` 是阴影大小，不是模型缩放。模型大小优先在 GEO/动画基础姿态里修，只有特殊情况再在 model 里做骨骼补偿。

## 5. 注册顺序

推荐固定顺序：
1. `ModEntities` 注册实体类型。
2. `ModEventBusEvents` 注册属性。
3. `TypeMoonWorldClientEvents` 注册 renderer。
4. `ModItems` 注册刷怪蛋。
5. `ModCreativeModeTabs` 把刷怪蛋放进创造页。
6. `lang` 添加显示名。

这一步最容易漏的是：
- 只注册了实体，没注册 renderer，结果实体生成了但看不见。
- 只注册了实体，没注册属性，结果世界生成或刷出时报错。
- 刷怪蛋注册了，但没放进创造页，或者语言 key 写错，显示成黑紫方块。

## 6. 定义 JSON

定义 JSON 是从者管线的核心数据入口。

一般至少包含：
- `id`
- `display_name`
- `display_name_zh`
- `class_type`
- `traits`
- `parameters`
- `model`
- `specialization`
- `skills`
- `noble_phantasm`
- `ai_config`
- `spawn_egg`

建议最先填这些内容：
- 面板参数
- 模型路径
- 基础尺寸
- 预设武器
- `combat_actions`

如果先做 AI，建议先复用默认 AI 配置，再逐步替换成专属 AI。

## 7. AI 制作顺序

### 7.0 战术配置与连续交战

`servant/ai` 的 `tactical` 段负责战斗方式，而不负责凭空授予技能。`style` 决定基础换位语法，
距离带决定常规站位，`pursuit_aggression` 和 `intercept_bias` 分别控制追击积极度与提前封路程度。
`maximum_terrain_impact` 是普通碰撞破坏上限，宝具自身的地形效果仍由宝具实现负责。

需要让某个动作进入统一击飞/追击链时，在 `servant/actions` 中增加 `launcher`、`pursuit`、
`intercept` 等标签并声明 `maneuver`。没有 `maneuver` 的旧动作继续由原 Combat Helper 执行，
因此旧数据包不需要迁移。追击动作必须有前摇、冷却和 `pursuit_window`，不能用传送修复寻路失败。

共享动作按服务器 tick 执行 `APPROACH -> WINDUP -> ACTIVE -> RECOVERY`。`timing.windup`、
`timing.active`、`timing.recovery` 分别控制三个战斗阶段；接近失败不会扣除 MP、耐力或提交冷却。
`maneuver.approach_range` 是允许开始寻路的最远距离，缺省等于 `maximum_range`；
`damage_scale` 控制共享命中的攻击倍率；`interrupt_resistance` 控制前摇抗打断等级，缺省继承
`interrupt_level`。普通战线的 `approach_range` 不能超过 48 格。

`counter` 只在敌方有效前摇中候选，`anti_air` 只针对浮空或高差目标，`finisher` 只针对撞墙或
落地硬直，`terrain_break` 只在真实命中后触发。不要把这些标签当作单纯的权重提示。

推荐把 AI 分成三层：

### 7.1 通用层

从通用动作标签开始，例如：
- `slash`
- `sweep`
- `uppercut`
- `horizontal_swing`
- `launcher`
- `pursuit`
- `charge`
- `interrupt`

这层先保证：
- 能靠近
- 能攻击
- 能转向
- 能基本躲避

### 7.2 专属层

当通用层稳定后，再加角色专属逻辑：
- 远程射击
- 小技能循环
- 特殊状态切换
- 武器浮游、召唤物、强化

像信长这种从者，专属层通常包括：
- 火枪点射
- 火枪浮游炮
- 近战小技能
- 暴击爆发
- 宝具前摇和收招

专属 helper 建议长这样：

```java
public final class ExampleServantCombatHelper {
   private static final String LAST_SPECIAL_TICK = "ExampleServantLastSpecialTick";
   private static final int SPECIAL_COOLDOWN = 80;

   private ExampleServantCombatHelper() {
   }

   public static void tick(ExampleServantEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) {
         return;
      }
      if (entity.isPerformingAction() || ServantCombatSystem.cannotAct(entity)) {
         return;
      }

      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         return;
      }

      long now = level.getGameTime();
      if (now - entity.getPersistentData().getLong(LAST_SPECIAL_TICK) < SPECIAL_COOLDOWN) {
         return;
      }

      double distance = entity.distanceTo(target);
      if (distance > 4.0D) {
         return;
      }

      entity.getPersistentData().putLong(LAST_SPECIAL_TICK, now);
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();
      target.hurt(entity.damageSources().mobAttack(entity), 8.0F);
   }
}
```

helper 的底线：
- 不要每 tick 都无条件放技能。
- 要检查目标、距离、冷却、MP、不可行动状态。
- 粒子和音效尽量只在服务端统一触发。
- 用 persistent data 存冷却和阶段状态，避免退出重进后逻辑乱跳。

### 7.3 大招层

最后再加：
- 宝具
- 领域
- 领域内地形铺开
- 特殊维度和返回逻辑
- 多阶段血量触发

## 8. 参考信长这种从者时要注意的坑

信长是很好的例子，因为她把很多分支都踩了一遍。

### 8.1 基础动作要统一

如果模型里有 `standing` 作为基础缩放修正，那就要确保：
- 所有动作都继承这个修正
- 不要只在站立里改，其他动作漏掉

否则会出现：
- 某些动作体型不对
- 武器位置不对
- 头发、手、刀在不同动作里偏移

### 8.2 GEO 骨骼的转向要真的生效

火枪、刀、头发这类骨骼，最好单独测试：
- 俯仰
- 偏航
- 反向旋转
- 180 度翻转

很多问题不是逻辑错，而是骨骼轴向和模型导出方向反了。

### 8.3 武器大小不要先过度适配

像压切长谷部这种近身武器，第一版先按“玩家手持武器大小”处理就够了。

别一开始就做太大的模型适配，不然很容易：
- 手上看着像贴图失真
- 手和刀的比例崩掉
- 动作一换就穿模

### 8.4 火枪是实体，不是手持物品

信长的火绳枪如果是实体，就要按实体做：
- 独立生成
- 独立转向
- 独立射击
- 死亡或消失时直接淡出/魔力散失

不要把它当成手持物品模型来做。

### 8.5 领域和维度逻辑要分清

领域展开通常要分成：
- 咏唱阶段
- 转场阶段
- 维度内战斗阶段
- 返回阶段

不要把“视觉特效”和“真正换维度”混成一个 tick 里全部完成，不然很容易卡死、丢目标、退出后卡在异常状态。

### 8.6 先别把所有技能一次性塞满

最稳方式是：
1. 先跑通普通攻击。
2. 再加 1 到 3 个基础技能。
3. 再做宝具。
4. 最后做领域和阶段 AI。

一次性塞太多，坏了很难定位。

## 9. 推荐技能设计节奏

建议新从者先按这个节奏做：

1. 1 个被动
2. 2 到 3 个小技能
3. 1 个普通远程手段
4. 1 个近战补强
5. 1 个宝具
6. 1 个特殊阶段动作

如果是 Archer、Caster 这种远程从者，建议额外补：
- 悬浮或站姿变化
- 发射前摇
- 射击烟雾和粒子
- 子弹实体
- 命中反馈

复杂技能建议分三类：

| 类型 | 例子 | 推荐实现 |
| --- | --- | --- |
| 纯数值技能 | 暴击、魔力恢复、抗性 | 技能 JSON + helper 加 buff |
| 动作技能 | 突刺、横扫、跳斩 | helper 里做目标判定、动画、伤害 |
| 实体技能 | 火枪、飞剑、魔弹 | 新实体 + renderer + 生命周期清理 |

只要有“独立飞行、独立索敌、独立消失”的东西，就优先当实体处理，不要硬塞成手持物或粒子。

## 10. 宝具和阶段 AI

血量阶段建议不要只做“优先级”，而是做“技能锁定”：

| 阶段 | 示例设计 |
| --- | --- |
| `>80%` | 只能试探、普通攻击、支援技能 |
| `60%-80%` | 解锁压制技能、强化技能 |
| `<=60%` | 解锁宝具、领域、认真模式 |

这样不会出现满血就乱开最终宝具的问题。

宝具有前摇时，建议拆成：
1. 写入开始 tick。
2. 播放语音和吟唱动作。
3. 前摇期间允许移动或限制移动，按角色设定决定。
4. 到点后真正生成实体、造成伤害或换维度。
5. 结束后清理状态。

领域类宝具要额外考虑：
- 没有拉到玩家或目标时的胜负判定。
- 创造模式、旁观者模式是否要纳入。
- 退出游戏再进入是否要返回原世界。
- 维度内死亡、卸载、实体消失时的清理。
- 领域优先级，避免两个领域互相覆盖导致死循环。

## 11. 常见坑点

### 11.1 JSON 读不到
- id 和 `SERVANT_KEY` 不一致
- 路径错了
- namespace 错了
- JSON 末尾逗号或引号坏了

### 11.2 实体能出但看不见
- renderer 没注册
- model 路径错了
- texture 路径错了
- geo 文件没放对目录

### 11.3 动画不播
- `model.animations` 的 key 和动画 JSON 名字对不上
- 动作名写错
- 骨骼名和动画目标名不一致

### 11.4 刷怪蛋显示成黑紫色块
- 没有对应的 `models/item/*_spawn_egg.json`
- 语言 key 没写
- 资源没放到 `assets/typemoonworld/models/item/`

### 11.5 属性/生成崩溃
- 忘了在 `ModEventBusEvents` 注册 `createAttributes()`
- 实体尺寸和定义 JSON `dimensions` 差太多

### 11.6 AI 一直发呆或乱放技能
- `combat_actions` 为空
- `ai_config` 没对上
- helper 里没检查冷却、MP、目标距离、不可行动状态

### 11.7 模型动作穿模
- 只修了 standing，没同步到其他动作
- 头发、武器、手臂没做反向修正
- 武器骨骼轴向反了

### 11.8 新实体导致世界卡崩
- 每 tick 生成太多实体
- 实体没有寿命上限
- 消失时没清理 owner UUID、目标 UUID 或维度状态
- 大范围铺方块没有 budget 限制
- 客户端和服务端都在生成实体

大型召唤物、枪阵、领域地形一定要分批处理。

## 12. 从者卡制作流程

从者卡不是 NPC 从者本体，也不是玩家原魔术技能，而是“玩家变身为从者”的第三套入口。制作时要始终区分四层逻辑：
- NPC 从者 AI 和 CombatHelper。
- 从者卡玩家技能。
- 玩家原本魔术/投影技能。
- 武器自带技能和宝具。

### 12.1 资源导入

中文文件名只作为源文件名，进入 `assets` 后必须改成英文小写资源 ID，避免 `ResourceLocation` 路径问题。

推荐路径：

```text
textures/item/servant_cards/<servant_id>_card.png
textures/item/servant_card_armor/<servant_id>_chest.png
textures/item/servant_card_armor/<servant_id>_legs.png
geo/servant_card_<servant_id>.geo.json
textures/models/armor/servant_card_<servant_id>.png
animations/servant_card_<servant_id>.animation.json
```

如果从者卡盔甲有自己的 GEO、贴图、动画，就必须三件套一起使用，不要混用 NPC 本体的模型或动画。比如红 A 从者卡盔甲应使用从者卡目录中的 `Emiya.geo.json`、`Emiya.png`、`Emiya.animation.json`，导入后改名为 `servant_card_emiya_archer.*`。

物品栏/创造页图标也要单独做。盔甲图标不应该继续用从者卡卡面贴图；胸甲、护腿分别使用 `textures/item/servant_card_armor/*_chest.png` 和 `*_legs.png`。

### 12.2 物品、创造页与注册

从者卡系统至少包含：
- `ServantCardItem`：右键触发变身。
- `ServantCardArmorItem`：从者卡盔甲外观。
- `ServantMasterContractItem`：绑定御主。
- `ServantCardReleaseItem`：主动解除变身。
- 独立创造页：放所有从者卡、从者卡盔甲、契约道具、解除道具。

从者卡卡面贴图可以批量导入所有 `*卡.png`，但资源名要统一映射成英文 ID，例如 `卫宫卡.png` -> `emiya_archer_card.png`。

### 12.3 变身状态

变身管理器需要统一处理：
- 保存原盔甲、主手、副手。
- 穿上对应从者卡盔甲。
- 发放该从者武器。
- 应用 `ServantDefinition.parameters()` 的生命、攻击、速度、护甲、暴击、魔力上限、魔力恢复。
- 锁定玩家原魔术回路，避免按键误触。
- 解除变身时恢复原装备、武器、属性和魔术状态。
- 死亡、登出、维度切换时清理飞行、投射物、领域状态和临时 NBT。

不要把从者卡状态写成零散的 item use 逻辑。所有入口都应该汇入统一 manager，便于恢复和排错。

### 12.4 魔力系统

从者卡 MP 独立于玩家原本 `player_mana`：
- 最大值来自从者定义的 `manaPool()`。
- 恢复速度按从者魔力等级或定义参数派生。
- 技能、宝具、吟唱、飞行先扣自身 servant MP。
- servant MP 不足时，再扣绑定御主的 `player_mana`。
- 未绑定御主也能战斗，只是没有备用魔力池。

从者卡 HUD 显示 servant MP；变身状态下原魔术师左下角魔力 UI 可以隐藏，避免两个魔力系统混在一起。

### 12.5 HUD、按键与 F1

所有从者卡 GUI 必须遵守原版 GUI 隐藏：

```java
if (Minecraft.getInstance().options.hideGui) {
   return;
}
```

HUD 建议显示：
- HP。
- servant MP。
- 御主 MP。
- 0-9 技能/宝具 CD。
- 跳跃次数。
- 飞行状态。

快捷键建议：
- NUMPAD 0-8：主动技能。
- NUMPAD 9：非武器自带宝具。
- 蹲下左键：节约技能槽的小技能。
- 蹲下空格：高跳。
- 双击跳跃：飞行从者切换飞行。

武器自带宝具不要重复放进 NUMPAD 9，直接用武器右键/蓄力释放，并给武器自身加长 CD。

### 12.6 移动、跳跃与飞行

从者卡玩家应按敏捷提高基础跳跃能力。蹲下空格是高跳：
- 没有方向键时垂直高跳。
- 带 W/A/S/D 时向对应方向高跳。
- 高跳消耗次数并逐步恢复。
- 高跳要有基础粒子和音效反馈。
- 从者卡玩家不吃摔落伤害。

飞行能力不占技能槽。美狄亚、织田信长、恩奇都这类从者由统一飞行控制器处理：
- 双击跳跃开关。
- 跳跃上升，潜行下降，WASD 水平移动。
- 持续消耗 servant MP。
- MP 不足或解除变身时自动落地。

### 12.7 NPC 技能适配到玩家动作

适配玩家动作前，必须先读对应 NPC CombatHelper，理解真实技能结构，再决定槽位：
- 只保留适合玩家主动释放的技能。
- 小技能优先并入普攻、蹲下左键或武器右键。
- 武器自带宝具不占 9。
- 杀阶气息遮断做主动隐身。
- 被动技能继续被动触发，不要硬塞进槽位。
- 10 个槽不必填满，但不要超出太多。

红 A 这类角色尤其要分清：
- 玩家投影技能。
- 从者卡红 A 技能。
- NPC 红 A CombatHelper。
- 无名弓、干将莫邪、螺旋剑等武器物品逻辑。

例如红 A 从者卡：
- 七重圆环应生成真正 `RhoAiasEntity`。
- 无限剑制应走红 A 版本的节奏，进入后有剑雨和自动防御投射物。
- 进入无限剑制后按 9 可直接退出。
- 连续投影层写是红 A NPC 的连续射出逻辑，不是玩家技能里的连续投影层写。

### 12.8 性能与清理

从者卡通常会引入大量投射物、领域、剑雨、爆炸和清方块逻辑，必须注意：
- 大范围清方块要分批执行，有 tick budget。
- 越卡时清理越慢，但最终一定要清理完成。
- 客户端只做输入、HUD、渲染；实体生成、伤害、扣 MP 在服务端。
- 持续投射物必须有寿命、owner UUID 和解除时清理逻辑。
- 解除变身、死亡、登出、维度切换都要清理飞行和领域状态。

### 12.9 从者卡测试顺序

推荐按这个顺序验证：
1. `.\gradlew.bat compileJava`
2. `.\gradlew.bat processResources`
3. 创造页能看到所有从者卡和盔甲。
4. 卡面、盔甲物品图标显示正确。
5. 使用从者卡后能变身、穿盔甲、发武器。
6. 属性、MP 上限、MP 恢复与定义 JSON 一致。
7. F1 能隐藏从者卡 HUD 和原魔力 HUD。
8. NUMPAD 0-9、蹲下左键、高跳、飞行按键工作。
9. MP 消耗先扣 servant MP，不足再扣御主 MP。
10. 解除变身后原装备、原武器、属性、魔术回路恢复。
11. 死亡规则、登出、维度切换不残留状态。

## 13. 测试顺序

推荐按这个顺序验证：
1. `./gradlew compileJava`
2. JSON 解析检查
3. 创造栏能看到刷怪蛋
4. 刷怪蛋能生成实体
5. 模型、贴图、动画正常
6. 普通攻击能打
7. 基础小技能能放
8. 宝具前摇、结束、清理都正常
9. 退出重进不残留异常状态
10. 复杂领域/维度逻辑不把世界弄崩

## 14. 最小落地清单

做一个可用从者，至少要有：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/XXXEntity.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/model/XXXModel.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/XXXRenderer.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEventBusEvents.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java
src/main/resources/data/typemoonworld/servant/definitions/xxx.json
src/main/resources/assets/typemoonworld/geo/xxx.geo.json
src/main/resources/assets/typemoonworld/textures/entity/xxx.png
src/main/resources/assets/typemoonworld/animations/xxx.animation.json
```

强烈建议再加：
- `ModItems` 里的刷怪蛋
- `ModCreativeModeTabs` 的创造页入口
- `lang/zh_cn.json`
- `lang/en_us.json`
- `CombatHelper`
- 技能 JSON
- 宝具 JSON

## 15. 给后续 AI 的执行建议

后续让 AI 做新从者时，建议把需求拆成这些批次：

1. 先导入 GEO、贴图、动画，确认模型能显示。
2. 再加实体、renderer、属性、刷怪蛋、语言。
3. 再写定义 JSON 和基础 `combat_actions`。
4. 编译并进游戏测试生成。
5. 再加 2 到 3 个小技能。
6. 再加宝具和专属实体。
7. 最后做阶段 AI、领域、维度和大规模特效。

不要让 AI 一次性改二十多个文件还不编译。每一个批次后都至少跑一次 `compileJava` 或 JSON 检查。

## 16. 结论

这套工程里，最重要的不是一口气把功能堆满，而是先把从者的“基础闭环”做稳：
- 能生成
- 能显示
- 能战斗
- 能读配置
- 能进创造页
- 能复用 GEO 模型和动画

后面再加 AI 专属逻辑、领域、特攻和华丽特效，就会顺很多。

## 17. 能力事实与战斗认知

技能定义可以声明可选的 `ai.facts`。能力事实只描述已经由效果或 Helper 实现的能力，不能单独赋予伤害、免疫、位移或控制效果。

```json
"ai": {
  "facts": [{
    "type": "projectile_negation",
    "strength": 1.0,
    "requires": ["mobile"],
    "bypassed_by": ["immobilize", "explosion", "piercing"]
  }]
}
```

自身 AI 立即读取完整技能事实；敌方隐藏被动只在技能前摇、免疫触发或实际战斗结果中被确认。旧技能省略 `ai` 时保持兼容，`magic_resistance`、`revive`、`adaptive_defense` 等通用效果会自动推导。独特被动应显式配置事实与真实失效条件。
