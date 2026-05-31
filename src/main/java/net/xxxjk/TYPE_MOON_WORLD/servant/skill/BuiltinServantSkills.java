package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;

public final class BuiltinServantSkills {
   private BuiltinServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("god_hand_passive", BuiltinServantSkills::executeGodHandPassive, "typemoonworld_core");
      registry.register("mad_enhancement_b", BuiltinServantSkills::executeMadEnhancement, "typemoonworld_core");
      registry.register("battle_continuation_a", BuiltinServantSkills::executeBattleContinuation, "typemoonworld_core");
      registry.register("valor_a_plus", BuiltinServantSkills::executeValor, "typemoonworld_core");
      registry.register("false_mind_eye_b", BuiltinServantSkills::executeFalseMindEye, "typemoonworld_core");
      registry.register("divinity_a", BuiltinServantSkills::executeDivinity, "typemoonworld_core");
   }

   /**
    * 十二试炼 — 被动防御核心
    * 存储防御参数到 entity 的 persistent data，供 CombatModule 读取
    */
   private static ServantExecutionResult executeGodHandPassive(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      // 存储十二试炼参数（CombatModule 在受伤时读取）
      entity.getPersistentData().putBoolean("GodHandActive", true);
      entity.getPersistentData().putFloat("GodHandThreshold", 3.0F);
      entity.getPersistentData().putInt("GodHandLives", 11);
      entity.getPersistentData().putFloat("GodHandAdaptiveReduction", 0.25F);
      entity.getPersistentData().putFloat("GodHandAdaptiveMax", 0.75F);
      entity.getPersistentData().putInt("GodHandStrongCost", 2);
      entity.getPersistentData().putInt("GodHandExtraStrongCost", 3);

      return ServantExecutionResult.SUCCESS;
   }

   /**
    * 狂化 B — 被动，降低服从率，提升伤害
    * 修改 entity 的 persistent data 供 AI 读取
    */
   private static ServantExecutionResult executeMadEnhancement(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      // 狂化等级存储（供 CombatModule/CommandModule 读取）
      entity.getPersistentData().putBoolean("MadEnhancementActive", true);
      entity.getPersistentData().putFloat("MadEnhancementPenalty", 0.3F);
      entity.getPersistentData().putFloat("MadEnhancementDamageBonus", 0.15F);

      return ServantExecutionResult.SUCCESS;
   }

   /**
    * 战斗续行 A — 被动，致死时保留1HP + 5s无敌
    * 此技能为被动触发器，实际逻辑在 LivingDamageEvent 中执行
    * 此处存储标记供事件处理读取
    */
   private static ServantExecutionResult executeBattleContinuation(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      entity.getPersistentData().putBoolean("BattleContinuationActive", true);
      entity.getPersistentData().putInt("BattleContinuationCooldown", 0);
      entity.getPersistentData().putInt("BattleContinuationMaxCooldown", 6000);

      return ServantExecutionResult.SUCCESS;
   }

   /**
    * 勇猛 A+ — 主动，ATK×2 持续10s + 免疫精神debuff
    */
   private static ServantExecutionResult executeValor(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      // 给予力量 II 效果（等级1 = II）持续 600 tick (30s)
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1, false, false, true));

      return ServantExecutionResult.SUCCESS;
   }

   /**
    * 伪·心眼 B — 主动，40%回避 + 暴击伤害×2 持续3.3s
    */
   private static ServantExecutionResult executeFalseMindEye(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      // 用抗性提升模拟闪避（等级 4 = 80% 减伤，接近 40% 回避的效果）
      entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false, true));

      // 存储暴击伤害加成供 CombatModule 读取
      entity.getPersistentData().putBoolean("CritDamageBoostActive", true);
      entity.getPersistentData().putFloat("CritDamageMultiplier", 2.0F);
      entity.getPersistentData().putInt("CritDamageTicksRemaining", 200);

      return ServantExecutionResult.SUCCESS;
   }

   /**
    * 神性 A — 被动，近战附带25点魔法伤害
    * 存储附加伤害值供 CombatModule 读取
    */
   private static ServantExecutionResult executeDivinity(ServantExecutionContext context) {
      LivingEntity entity = context.caster();
      if (entity == null) return ServantExecutionResult.FAILED;

      entity.getPersistentData().putBoolean("DivinityActive", true);
      entity.getPersistentData().putFloat("DivinityFlatDamage", 25.0F);

      return ServantExecutionResult.SUCCESS;
   }
}
