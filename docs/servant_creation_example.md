# 从零制作一个英灵：完整示范流程

这份文档参考 `MagicAddonTemplateExample.java` 的写法：先给一个能照着改名的最小示例，再解释每个文件为什么需要、放在哪里、怎么验证。

示例英灵使用虚构角色 `Example Saber`，文件 id 使用 `example_saber`。你做自己的英灵时，把所有 `example_saber`、`ExampleSaber`、`EXAMPLE_SABER` 换成你的角色名即可。

## 0. 先理解本模组的英灵结构

一个英灵不是只靠一个 Java 类完成的。通常由这几部分组成：

| 类型 | 作用 | 必需 |
| --- | --- | --- |
| 实体类 | 让游戏里有这个实体，继承 `ServantEntity` | 必需 |
| 模型类 | 告诉 GeckoLib 使用哪个 geo、贴图、动画 | 必需 |
| 渲染器 | 把模型类挂到实体上 | 必需 |
| 实体注册 | 在 `ModEntities` 注册实体类型 | 必需 |
| 属性注册 | 在 `ModEventBusEvents` 注册基础属性 | 必需 |
| 客户端渲染注册 | 在 `TypeMoonWorldClientEvents` 注册 renderer | 必需 |
| 刷怪蛋注册 | 在 `ModItems` 注册 spawn egg | 推荐 |
| 创造栏注册 | 在 `ModCreativeModeTabs` 显示 spawn egg | 推荐 |
| 语言文件 | 显示刷怪蛋名字 | 推荐 |
| 定义 JSON | 面板、职阶、模型路径、AI 动作、技能 | 必需 |
| AI JSON | 通用行为参数，通常复用默认职阶 AI | 可复用 |
| 技能 JSON | 被定义 JSON 引用的被动/主动技能说明 | 可选 |
| 宝具 JSON | 宝具数据说明 | 可选 |
| assets 资源 | geo 模型、png 贴图、animation 动画 | 必需 |
| CombatHelper | 专属 AI 小技能/宝具逻辑 | 复杂英灵需要 |

最小路线是：先做一个像佐佐木小次郎那样很薄的实体，复用通用战斗系统；等它能生成、能动、能打，再加专属 Helper 和宝具。

## 1. 命名规则

以 `example_saber` 为例：

| 用途 | 写法 |
| --- | --- |
| 数据 id | `example_saber` |
| Java 类名前缀 | `ExampleSaber` |
| 注册常量 | `EXAMPLE_SABER` |
| 定义 JSON | `example_saber.json` |
| 模型资源 | `geo/example_saber.geo.json` |
| 贴图资源 | `textures/entity/example_saber.png` |
| 动画资源 | `animations/example_saber.animation.json` |
| 刷怪蛋物品 | `example_saber_spawn_egg` |

注意：JSON id、资源路径、注册名建议全部小写加下划线。Java 类名用大驼峰。

## 2. 准备资源文件

先准备三个客户端资源：

```text
src/main/resources/assets/typemoonworld/geo/example_saber.geo.json
src/main/resources/assets/typemoonworld/textures/entity/example_saber.png
src/main/resources/assets/typemoonworld/animations/example_saber.animation.json
```

如果暂时没有新模型，开发阶段可以先复制一个体型相近的从者资源，例如佐佐木：

```text
geo/sasaki_kojiro.geo.json -> geo/example_saber.geo.json
textures/entity/sasaki_kojiro.png -> textures/entity/example_saber.png
animations/sasaki_kojiro.animation.json -> animations/example_saber.animation.json
```

复制后至少把动画文件里的动画名改成 `animation.example_saber.xxx`。定义 JSON 里的 `model.animations` 必须和动画文件里的名字对上。

常用动画键：

```json
{
  "idle": "animation.example_saber.idle",
  "walk": "animation.example_saber.walk",
  "sweep": "animation.example_saber.horizontal_swing",
  "slash": "animation.example_saber.slash",
  "uppercut": "animation.example_saber.uppercut",
  "horizontal_swing": "animation.example_saber.horizontal_swing"
}
```

没有对应动作时，可以先让多个键共用同一个动画，例如 `uppercut` 先共用 `slash`。

## 3. 新建实体类

路径：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ExampleSaberEntity.java
```

最小实体类：

```java
package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ExampleSaberEntity extends ServantEntity {
   public static final String SERVANT_KEY = "example_saber";

   public ExampleSaberEntity(EntityType<ExampleSaberEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }
}
```

`SERVANT_KEY` 必须和定义 JSON 的 `"id"` 完全一致。通用从者系统会用这个 key 去读取：

```text
src/main/resources/data/typemoonworld/servant/definitions/example_saber.json
```

## 4. 新建模型类

路径：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/model/ExampleSaberModel.java
```

最小模型类：

```java
package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ExampleSaberEntity;

public class ExampleSaberModel extends BaseServantModel<ExampleSaberEntity> {
   public ExampleSaberModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/example_saber.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/example_saber.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/example_saber.animation.json")
      );
   }
}
```

如果模型比例不对，再参考 `SasakiKojiroModel` 的 `applySpawnPoseFallback`，对骨骼做临时缩放。但新手第一版建议先不写骨骼调整，确保能显示。

## 5. 新建渲染器

路径：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/ExampleSaberRenderer.java
```

代码：

```java
package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ExampleSaberModel;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ExampleSaberEntity;

public class ExampleSaberRenderer extends BaseServantRenderer<ExampleSaberEntity> {
   public ExampleSaberRenderer(Context renderManager) {
      super(renderManager, new ExampleSaberModel(), 0.95F);
   }
}
```

最后一个数字是阴影大小，普通人形一般 `0.9F` 到 `1.0F`。

## 6. 注册实体

文件：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java
```

先添加 import：

```java
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ExampleSaberEntity;
```

在现有从者注册附近添加：

```java
public static final DeferredHolder<EntityType<?>, EntityType<ExampleSaberEntity>> EXAMPLE_SABER = ENTITY_TYPES.register(
   "example_saber", () -> Builder.of(ExampleSaberEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("example_saber")
);
```

`sized(0.6F, 1.8F)` 是碰撞箱宽高。普通人形用这个即可；赫拉克勒斯那种大体型可以用 `1.0F, 3.0F`。

## 7. 注册属性

文件：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEventBusEvents.java
```

找到类似下面的区域：

```java
event.put(ModEntities.SASAKI_KOJIRO.get(), ServantEntity.createAttributes().build());
```

添加：

```java
event.put(ModEntities.EXAMPLE_SABER.get(), ServantEntity.createAttributes().build());
```

如果忘了这一步，常见结果是实体生成时报属性相关错误。

## 8. 注册客户端渲染器

文件：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java
```

添加 import：

```java
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ExampleSaberRenderer;
```

在 `registerRenderers` 方法里添加：

```java
event.registerEntityRenderer(ModEntities.EXAMPLE_SABER.get(), ExampleSaberRenderer::new);
```

如果忘了这一步，服务端可能能生成实体，但客户端看不到或渲染报错。

## 9. 注册刷怪蛋

文件通常是：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModItems.java
```

参考现有从者 spawn egg，添加类似代码：

```java
public static final DeferredHolder<Item, Item> EXAMPLE_SABER_SPAWN_EGG = ITEMS.register(
   "example_saber_spawn_egg",
   () -> new SpawnEggItem(ModEntities.EXAMPLE_SABER.get(), 0x304C89, 0xE6D7A8, new Item.Properties())
);
```

两个颜色是刷怪蛋主色和副色，十六进制 RGB。

然后把它加入创造栏：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModCreativeModeTabs.java
```

在从者刷怪蛋区域添加：

```java
output.accept(ModItems.EXAMPLE_SABER_SPAWN_EGG);
```

## 10. 添加语言文件

中文：

```text
src/main/resources/assets/typemoonworld/lang/zh_cn.json
```

```json
"item.typemoonworld.example_saber_spawn_egg": "示例剑士(Saber) 刷怪蛋"
```

英文：

```text
src/main/resources/assets/typemoonworld/lang/en_us.json
```

```json
"item.typemoonworld.example_saber_spawn_egg": "Example Saber Spawn Egg"
```

注意 JSON 逗号：不是最后一行时，上一行末尾要有逗号；最后一行不能多逗号。

## 11. 新建从者定义 JSON

路径：

```text
src/main/resources/data/typemoonworld/servant/definitions/example_saber.json
```

完整示例：

```json
{
  "id": "example_saber",
  "display_name": "Example Saber",
  "display_name_zh": "示例剑士",
  "class_type": "saber",
  "faction": "human",
  "traits": ["servant", "humanoid", "living_human", "saber", "female", "lawful", "good"],
  "parameters": {
    "endurance": "B",
    "endurance_plus": false,
    "strength": "B",
    "strength_plus": false,
    "agility": "A",
    "agility_plus": false,
    "magic": "C",
    "magic_plus": false,
    "luck": "C",
    "luck_plus": false
  },
  "model": {
    "geometry": "typemoonworld:geo/example_saber.geo.json",
    "texture": "typemoonworld:textures/entity/example_saber.png",
    "animation": "typemoonworld:animations/example_saber.animation.json",
    "animations": {
      "idle": "animation.example_saber.idle",
      "walk": "animation.example_saber.walk",
      "sweep": "animation.example_saber.horizontal_swing",
      "slash": "animation.example_saber.slash",
      "uppercut": "animation.example_saber.uppercut",
      "horizontal_swing": "animation.example_saber.horizontal_swing"
    }
  },
  "specialization": {
    "dimensions": {
      "width": 0.6,
      "height": 1.8,
      "eye_height": 1.62
    },
    "hand_item_offset": {
      "x": 0.1,
      "y": -0.8,
      "z": -0.2
    },
    "default_weapon": "minecraft:iron_sword",
    "immune_to_stone_axe_debuff": false,
    "combat_actions": [
      "sweep",
      "slash",
      "uppercut",
      "horizontal_swing",
      "launcher",
      "pursuit"
    ]
  },
  "skills": ["example_battle_instinct_b"],
  "noble_phantasm": "",
  "personality": {
    "obedience": "cooperative",
    "principle": "orderly",
    "morality": "good",
    "social": "normal",
    "combat": "cautious",
    "starting_favor": 50.0
  },
  "ai_config": "default_saber",
  "spawn_egg": {
    "primary": 3165321,
    "secondary": 15128488
  }
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `id` | 必须等于实体类里的 `SERVANT_KEY` |
| `class_type` | 职阶，可用现有值如 `saber`、`lancer`、`rider`、`caster`、`assassin`、`berserker` |
| `parameters` | 面板等级，支持 `E`、`D`、`C`、`B`、`A`，加号用 `xxx_plus` |
| `model` | 模型、贴图、动画资源路径 |
| `model.animations` | 通用动作名到 GeckoLib 动画名的映射 |
| `dimensions` | 和实体注册的 `sized` 保持接近 |
| `hand_item_offset` | 手持武器位置微调 |
| `default_weapon` | 默认武器，可填本模组物品或原版物品 |
| `combat_actions` | AI 可用动作标签，通用战斗系统会按这些标签尝试技能 |
| `skills` | 引用 `servant/skills` 里的 JSON id |
| `noble_phantasm` | 宝具 id；没有就先留空 |
| `ai_config` | 引用 `servant/ai` 下的 AI 配置 |

## 12. 新建技能 JSON

如果定义 JSON 写了：

```json
"skills": ["example_battle_instinct_b"]
```

就需要创建：

```text
src/main/resources/data/typemoonworld/servant/skills/example_battle_instinct_b.json
```

示例：

```json
{
  "id": "example_battle_instinct_b",
  "display_name": "Battle Instinct B",
  "display_name_zh": "战斗直感 B",
  "type": "passive",
  "mp_cost": 0,
  "cooldown_ticks": 0,
  "duration_ticks": 0,
  "effects": [
    {
      "effect_type": "dodge_window_bonus",
      "params": {
        "perfect_dodge_window_bonus": 0.03,
        "stamina_recovery_bonus": 2.0
      }
    }
  ]
}
```

如果暂时不想做技能，可以把定义 JSON 的 `"skills"` 改成空数组：

```json
"skills": []
```

## 13. AI 配置：先复用默认职阶

示例里用：

```json
"ai_config": "default_saber"
```

对应文件：

```text
src/main/resources/data/typemoonworld/servant/ai/default_saber.json
```

新手建议先复用默认 AI。等角色能正常战斗后，再复制一份：

```text
default_saber.json -> example_saber_ai.json
```

然后把定义 JSON 改为：

```json
"ai_config": "example_saber_ai"
```

AI JSON 主要调：

| 字段 | 作用 |
| --- | --- |
| `preferred_attack_distance` | 喜欢保持的攻击距离 |
| `retreat_health_ratio` | 低于多少血量更倾向后退 |
| `melee_preference` | 近战倾向 |
| `berserk_health_ratio` | 低血量狂暴阈值 |
| `berserk_damage_multiplier` | 狂暴伤害倍率 |

## 14. 小技能：先用通用动作标签

当前通用战斗系统能识别一些常用标签。定义 JSON 的 `combat_actions` 可以先写：

```json
"combat_actions": [
  "sweep",
  "slash",
  "uppercut",
  "horizontal_swing",
  "launcher",
  "pursuit"
]
```

常见标签含义：

| 标签 | 大概用途 |
| --- | --- |
| `slash` | 普通斩击 |
| `sweep` | 横扫 |
| `uppercut` | 上挑，可作为击飞候选 |
| `horizontal_swing` | 横斩 |
| `launcher` | 明确告诉连招系统：这是击飞动作 |
| `pursuit` | 明确告诉连招系统：这是追击动作 |
| `teleport_behind` | 瞬移背刺类追击 |
| `charge` | 冲刺类追击 |

如果只是想让英灵能打，不需要立刻写专属 Java 技能。先用这些标签让它接入普攻、击飞、追击和通用 AI。

## 15. 专属小技能：需要 Helper 时怎么做

如果角色有特殊技能，比如“冲刺三连斩”“火焰剑气”“格挡反击”，建议新建 Helper：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ExampleSaberCombatHelper.java
```

最小结构：

```java
package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;

public final class ExampleSaberCombatHelper {
   private static final String LAST_QUICK_SLASH_TICK = "ExampleSaberLastQuickSlashTick";
   private static final int QUICK_SLASH_COOLDOWN = 70;

   private ExampleSaberCombatHelper() {
   }

   public static void tick(ExampleSaberEntity entity, ServantAiContext context) {
      LivingEntity target = context.target();
      if (target == null || !target.isAlive()) {
         return;
      }

      long now = context.gameTick();
      double distance = entity.distanceTo(target);
      if (tryQuickSlash(entity, target, distance, now)) {
         return;
      }
   }

   private static boolean tryQuickSlash(ExampleSaberEntity entity, LivingEntity target, double distance, long now) {
      if (distance > 4.0 || entity.isPerformingAction()) {
         return false;
      }
      if (now - entity.getPersistentData().getLong(LAST_QUICK_SLASH_TICK) < QUICK_SLASH_COOLDOWN) {
         return false;
      }

      entity.getPersistentData().putLong(LAST_QUICK_SLASH_TICK, now);
      entity.faceToward(target.position());
      entity.triggerSlashAnimation();

      float damage = (float)(entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.15);
      target.invulnerableTime = 0;
      target.hurt(entity.damageSources().mobAttack(entity), damage);

      Vec3 push = target.position().subtract(entity.position()).multiply(1.0, 0.0, 1.0);
      if (push.lengthSqr() > 1.0E-4) {
         push = push.normalize();
         target.push(push.x * 0.6, 0.25, push.z * 0.6);
         target.hurtMarked = true;
      }

      if (entity.level() instanceof ServerLevel level) {
         level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.2F);
      }
      return true;
   }
}
```

然后在 `ExampleSaberEntity` 里接入：

```java
@Override
public void servantAiTick(ServantAiContext context) {
   super.servantAiTick(context);
   ExampleSaberCombatHelper.tick(this, context);
}
```

如果实际父类没有这个精确方法名，就参考现有 `CuChulainnEntity`、`MedeaEntity`、`MedusaEntity` 的专属 Helper 接入方式。不同英灵可能挂在不同 tick 钩子里，以现有代码为准。

专属技能的基本原则：

1. 先判断目标、距离、MP、冷却、视线。
2. 扣 MP 和写入冷却。
3. 调 `faceToward` 或 `faceVector`，避免技能方向错。
4. 触发动画。
5. 造成伤害、击退、粒子、音效。
6. 服务端逻辑放在 `ServerLevel` 判断里，客户端只负责渲染。

## 16. 宝具：先建数据，再接 Java 入口

宝具数据放在：

```text
src/main/resources/data/typemoonworld/servant/noble_phantasms/example_sword_np.json
```

示例：

```json
{
  "id": "example_sword_np",
  "display_name": "Example Sword",
  "display_name_zh": "示例圣剑",
  "type": "anti_unit",
  "rank": "B",
  "mp_cost": 40,
  "cooldown_ticks": 600,
  "windup_ticks": 30,
  "effects": [
    {
      "effect_type": "beam_slash",
      "params": {
        "damage": 30,
        "range": 18,
        "width": 2.0
      }
    }
  ]
}
```

然后把从者定义 JSON 改成：

```json
"noble_phantasm": "example_sword_np"
```

注意：宝具 JSON 只是数据。真正释放效果一般还需要在实体或 Helper 里写 Java 逻辑，参考：

| 角色 | 可参考文件 |
| --- | --- |
| 库丘林 Gae Bolg | `CuChulainnCombatHelper.java`、`GaeBulgProjectileEntity.java` |
| 美杜莎 Bellerophon | `MedusaCombatHelper.java`、`MedusaPegasusEntity.java` |
| 美狄亚 Rule Breaker/魔术 | `MedeaCombatHelper.java`、`MedeaMagicBoltEntity.java` |
| 赫拉克勒斯 God Hand | `HeraclesGodHandHelper.java` |

新手建议第一版先不做宝具，等普通技能稳定后再加。

## 17. 如果需要新投射物

例如英灵要发射剑气，需要额外文件：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/entity/ExampleSwordWaveEntity.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/ExampleSwordWaveRenderer.java
```

还要注册：

| 文件 | 要做什么 |
| --- | --- |
| `ModEntities.java` | 注册 projectile entity |
| `TypeMoonWorldClientEvents.java` | 注册 projectile renderer |
| 可能的 item/model/texture | 如果投射物使用物品贴图 |

简单技能能用粒子和 AABB 伤害解决时，先不要新增投射物。少一个实体，少很多注册点。

## 18. 常见错误排查

| 现象 | 优先检查 |
| --- | --- |
| 刷怪蛋没有名字 | `zh_cn.json`、`en_us.json` key 是否是 `item.typemoonworld.xxx_spawn_egg` |
| 刷怪蛋不在创造栏 | `ModCreativeModeTabs` 有没有 `output.accept` |
| 实体生成崩溃 | `ModEventBusEvents` 有没有注册属性 |
| 实体隐形 | `TypeMoonWorldClientEvents` 有没有注册 renderer |
| 模型紫黑/贴图丢失 | `ExampleSaberModel` 和定义 JSON 的 texture 路径 |
| 动画不播放 | 定义 JSON 的 `model.animations` 名字是否存在于 animation JSON |
| 手里武器位置怪 | 调 `hand_item_offset` |
| 碰撞箱不对 | `ModEntities.sized` 和定义 JSON `dimensions` |
| AI 不打架 | `combat_actions` 是否为空、`ai_config` 是否存在 |
| 技能方向怪 | 技能释放前调用 `faceToward(target.position())` 或 `faceVector(dir)` |
| JSON 读取失败 | 逗号、引号、UTF-8 编码 |

## 19. 附属 mod 如何添加英灵

如果你是在附属 mod 里添加英灵，不需要改主 mod 源码。推荐做法是：

1. 在附属 mod 注册自己的 `EntityType`，实体类继承 `ServantEntity`。
2. 把从者定义 JSON 放到附属 mod 的数据目录。
3. 把模型、贴图、动画放到附属 mod 的 assets 目录。
4. 用 `IServantAddonEntrypoint` 注册专属战斗动作、宝具和 AI tick 钩子。

附属 mod 的从者定义路径示例：

```text
src/main/resources/data/exampleaddon/servant/definitions/example_saber.json
```

注意这里 namespace 是 `exampleaddon`，但 JSON 里的资源路径也应该指向你的 namespace：

```json
{
  "id": "example_saber",
  "model": {
    "geometry": "exampleaddon:geo/example_saber.geo.json",
    "texture": "exampleaddon:textures/entity/example_saber.png",
    "animation": "exampleaddon:animations/example_saber.animation.json"
  }
}
```

附属 mod 的资源路径：

```text
src/main/resources/assets/exampleaddon/geo/example_saber.geo.json
src/main/resources/assets/exampleaddon/textures/entity/example_saber.png
src/main/resources/assets/exampleaddon/animations/example_saber.animation.json
```

### 19.1 ServiceLoader 入口

创建文件：

```text
src/main/resources/META-INF/services/net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint
```

文件内容只写你的入口类全名：

```text
com.example.exampleaddon.ExampleServantEntrypoint
```

入口类：

```java
package com.example.exampleaddon;

import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;

public final class ExampleServantEntrypoint implements IServantAddonEntrypoint {
   @Override
   public String providerId() {
      return "exampleaddon";
   }

   @Override
   public void registerServants(IServantAddonRegistry registry) {
      registry.registerCombatAction("example_flash_cut", ExampleServantActions::flashCut, this.providerId());
      registry.registerNoblePhantasm("example_sword_np", ExampleServantActions::exampleSwordNp, this.providerId());
      registry.registerLifecycleHandler("example_saber_tick", ExampleServantActions::tick, this.providerId());
   }
}
```

可以参考主 mod 内的模板：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/ServantAddonTemplateExample.java
```

### 19.2 战斗动作如何被 AI 使用

注册了动作还不够，定义 JSON 里必须写入同名 `combat_actions`：

```json
"combat_actions": [
  "slash",
  "uppercut",
  "launcher",
  "pursuit",
  "example_flash_cut"
]
```

主 mod 的 `CombatModule` 会遍历从者自己的 `combat_actions`。当它发现 `example_flash_cut` 有附属执行器时，就会把当前从者、目标、距离、视线、AI context 传给你的 executor。

如果 executor 返回：

| 返回值 | 含义 |
| --- | --- |
| `ServantExecutionResult.NOT_HANDLED` | 这次条件不合适，继续尝试其他动作 |
| `ServantExecutionResult.FAILED` | 动作处理了但失败，停止本轮动作 |
| `ServantExecutionResult.SUCCESS` | 动作成功，停止本轮动作 |

### 19.3 宝具如何接入

从者定义 JSON：

```json
"noble_phantasm": "example_sword_np"
```

宝具数据 JSON：

```text
src/main/resources/data/exampleaddon/servant/noble_phantasms/example_sword_np.json
```

附属入口注册：

```java
registry.registerNoblePhantasm("example_sword_np", ExampleServantActions::exampleSwordNp, "exampleaddon");
```

当核心宝具执行入口收到 `example_sword_np` 时，会优先调用附属 mod 注册的宝具 executor。executor 可以自己播放动画、扣 MP、生成投射物、造成伤害。建议返回 `SUCCESS.withMpCost(cost)`，让核心统一扣除 MP。

### 19.4 附属 mod 能做到什么程度

目前接口支持：

| 能力 | 支持情况 |
| --- | --- |
| 自定义从者定义 JSON | 支持，任意 namespace 的 `servant/definitions` |
| 自定义模型/贴图/动画 | 支持，由附属 mod 自己的 renderer/model 指向资源 |
| 自定义实体类 | 支持，继承 `ServantEntity` |
| 自定义刷怪蛋 | 支持，由附属 mod 自己注册 |
| 自定义小技能 AI | 支持，`registerCombatAction` |
| 自定义宝具执行 | 支持，`registerNoblePhantasm` |
| 自定义每 tick 行为 | 支持，`registerLifecycleHandler` |
| 自定义技能 executor | 支持，旧接口 `registerSkills` 仍可用 |

仍然建议附属 mod 自己注册实体和 renderer，因为 Minecraft/NeoForge 的实体注册必须在各 mod 自己的注册阶段完成，主 mod 不能可靠地替附属 mod 动态注册完整实体类型。

## 20. 最小文件清单

做一个能生成、能显示、能普通战斗的英灵，至少需要：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/entity/ExampleSaberEntity.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/model/ExampleSaberModel.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/renderer/ExampleSaberRenderer.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEventBusEvents.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/client/TypeMoonWorldClientEvents.java
src/main/resources/data/typemoonworld/servant/definitions/example_saber.json
src/main/resources/assets/typemoonworld/geo/example_saber.geo.json
src/main/resources/assets/typemoonworld/textures/entity/example_saber.png
src/main/resources/assets/typemoonworld/animations/example_saber.animation.json
```

推荐再加：

```text
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModItems.java
src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModCreativeModeTabs.java
src/main/resources/assets/typemoonworld/lang/zh_cn.json
src/main/resources/assets/typemoonworld/lang/en_us.json
src/main/resources/data/typemoonworld/servant/skills/example_battle_instinct_b.json
```

## 21. 最后验证

能联网下载依赖、环境正常时，运行：

```powershell
./gradlew compileJava
```

如果只想先检查 JSON，可以用 PowerShell：

```powershell
Get-Content -Raw -Encoding UTF8 src/main/resources/data/typemoonworld/servant/definitions/example_saber.json | ConvertFrom-Json | Out-Null
Get-Content -Raw -Encoding UTF8 src/main/resources/data/typemoonworld/servant/skills/example_battle_instinct_b.json | ConvertFrom-Json | Out-Null
```

游戏内验证顺序：

1. 创造栏能看到 `Example Saber Spawn Egg`。
2. 右键刷怪蛋能生成实体。
3. 实体模型、贴图、大小正常。
4. 静止和走路动画正常。
5. 攻击目标时能靠近、转向、普攻。
6. `uppercut`、`launcher`、`pursuit` 这类动作不会报错。
7. 如果加了专属 Helper，确认冷却、MP、伤害、粒子、音效都符合预期。

## 22. 推荐制作顺序

不要一开始就把模型、宝具、复杂 AI 全塞进去。最稳的路线是：

1. 复制佐佐木这类薄实体，改出 `ExampleSaberEntity`。
2. 注册实体、属性、渲染器。
3. 复制资源文件，确认模型能显示。
4. 写定义 JSON，先只放通用 `combat_actions`。
5. 加刷怪蛋和语言文件。
6. 进游戏确认能生成和普通战斗。
7. 再加 1-2 个专属小技能。
8. 最后加宝具、投射物、特殊被动、语音和更细动画。

这样每一步坏了都很好定位，不会变成“二十个文件一起坏，不知道哪里错”。
