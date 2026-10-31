# 对魔力重置

以下规则来自用户已确认的重置方案（2026-10-31），用于玩家从者卡与 NPC 的公共结算。

## 生效范围与等级

对魔力只抵抗明确标记的魔术伤害与魔术负面效果。Minecraft 的 `magic`、`indirect_magic` 等伤害类型本身不提供魔术身份；物理命中、神性追加伤害、普通药水、毒、环境火焰和未标记为魔术的宝具不触发对魔力。

| 对魔力 | 完全无效化的最高魔术复杂度 | 超过该复杂度的伤害减免 |
| --- | --- | --- |
| E | 简单动作 `simple_action` | 6% |
| D | 一道咏唱 `one_verse` | 12% |
| C | 二道咏唱 `two_verse` | 22% |
| B | 三道咏唱 `three_verse` | 35% |
| A | 高等魔术 `high_thaumaturgy` | 45% |
| EX | 全部已识别魔术 | 85% 的原默认值保留；当前五档全部免疫 |

先判定复杂度免疫，再对超过免疫范围的伤害乘以 `1 - 减免率`。不再按伤害数额设置“小伤害全免”或“小伤害减半”。例如 B 对高等魔术的 10 点命中承受 6.5 点；同档 1 点命中承受 0.65 点。仍保留已有等级来源、熟练度对抗和明确角色专属覆盖。

魔术定义的 `resistance_complexity` 为伤害等级来源，未填写时沿用 `MagicDefinitionData.resolvedResistanceComplexity()` 的推导。绑定的范围模式最低为既有二道咏唱分类，定义配置更高复杂度时采用配置。美狄亚普通魔弹、元素使技能／精灵、贤王普通石板魔弹默认使用二道咏唱，美狄亚光束默认使用高等魔术。不能通过 Caster 职阶或通用伤害类型推测魔术身份。

## 命中接入

公共附属 API `net.xxxjk.typemoonworld.api.MagecraftDamageSource` 包装原伤害来源，保留原伤害类型及施法归属。普通魔术传入定义 ID：

```java
target.hurt(MagecraftDamageSource.of(baseSource, "addon:spell_id", caster), damage);
```

没有魔术定义的从者技能须明确指定复杂度：

```java
target.hurt(MagecraftDamageSource.of(baseSource, "servant_spell",
    MagicComplexity.TWO_VERSE, caster), damage);
```

需要保留释放时熟练度时使用 `of(baseSource, magicId, caster, proficiency)`。投射物与领域保留其来源 ID；红宝石与符文的魔术爆炸也包装爆炸来源，普通爆炸保持原行为。

对魔力伤害由 `CommonEvents` 的公共伤害事件统一结算。调用 `hurt` 前不要再手动预减伤。`WrappedDamageSource` 使魔术标记、战斗来源和宝具分类能够叠加并被查回。

## 宝具与控制效果

宝具默认不属于魔术。`ServantNoblePhantasmDefinition` 新增可选字段：

```json
{
  "magic_damage": true,
  "magic_complexity": "high_thaumaturgy"
}
```

宝具命中通过 `NoblePhantasmDamageClassifier.withNoblePhantasm(baseSource, npId)` 关联定义，只有 `magic_damage=true` 才附加魔术来源；`magic_complexity` 默认高等魔术。普通宝具覆盖内层普通魔术标记。旧的无来源 `applyNoblePhantasmMagicResistance(entity, amount)` 为兼容入口，直接返回原伤害，不预先结算。

贤王共用弹丸的普通石板射击标记为魔术；爆炸王之号炮关联 `royal_cannon` 宝具，默认绕过对魔力。Rule Breaker、元素使宝具等继续按宝具独立分类。

不再在 `MobEffectEvent.Added` 中全局缩短有害效果。魔术施加的负面状态通过 `MagecraftEffects.add` 或明确的绑定／暗示／重力入口处理：达到免疫阈值时拒绝施加，否则按既有减益抗性与时长上限缩短一次。普通药水直接使用 `addEffect`。

`MagecraftEffects.add` 保留有益效果；用高等级跳跃提升实现的禁跳等负面控制使用 `addHarmful`。冻结、魔术点燃、推拉等非药水附带状态也检查该魔术的复杂度免疫；场景中的实体方块与普通火焰沿用原规则。

## 验证入口

- 单测：`gradlew test`，包括免疫矩阵、默认档位、E 档注册、EX 卡片解析及宝具元数据往返编解码。
- 服务端：`gradlew runGameTestServer -I tools/magecraft-validation.init.gradle -PgameTestNamespaces=typemoonworld_magecraft`。
- 客户端与集成服务器：`gradlew runClient -I tools/magecraft-smoke.init.gradle`。验证辅助模组独立编译，不进入发布 JAR。
- 构建与附属 API：`gradlew build apiJar`。
