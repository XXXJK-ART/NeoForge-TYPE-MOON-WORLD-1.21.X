package net.xxxjk.TYPE_MOON_WORLD.passive;

import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.magic.MuramasaDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.ProjectileThreatClassifier;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;

public final class PassiveService {
   public static final String DIVINITY = "divinity";
   public static final String CLAIRVOYANCE = "clairvoyance";
   public static final String MIND_EYE_TRUE = "mind_eye_true";
   public static final String MIND_EYE_FALSE = "mind_eye_false";
   public static final String INSTINCT = "instinct";
   public static final Set<String> IDS = Set.of(DIVINITY, CLAIRVOYANCE, MIND_EYE_TRUE, MIND_EYE_FALSE, INSTINCT);
   private static final ResourceLocation DIVINITY_HEALTH_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "passive_divinity_health");
   private static final ResourceLocation DIVINITY_ATTACK_ID = ResourceLocation.fromNamespaceAndPath("typemoonworld", "passive_divinity_attack");

   private PassiveService() {
   }

   public static boolean isPassive(String id) {
      return id != null && IDS.contains(id);
   }

   public static boolean has(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      return rank(vars, id) != null;
   }

   public static PassiveRank rank(TypeMoonWorldModVariables.PlayerVariables vars, String id) {
      return vars == null || id == null ? null : vars.passive_ranks.get(id);
   }

   public static double dodgeChance(TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank trueEye = rank(vars, MIND_EYE_TRUE);
      PassiveRank falseEye = rank(vars, MIND_EYE_FALSE);
      PassiveRank instinct = rank(vars, INSTINCT);
      return dodgeChance(trueEye, falseEye, instinct);
   }

   public static double dodgeChance(PassiveRank trueEye, PassiveRank falseEye, PassiveRank instinct) {
      double eyeChance = Math.max(trueEye == null ? 0.0 : trueEye.dodgeChance(), falseEye == null ? 0.0 : falseEye.dodgeChance());
      return Math.min(0.80, eyeChance + (instinct == null ? 0.0 : instinct.dodgeChance()));
   }

   public static boolean tryDodge(ServerPlayer player, DamageSource source) {
      if (player == null || source == null || source.getEntity() == null
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(FanaticDamageTypes.GUARANTEED_HITS)
         || source.is(MuramasaDamageTypes.TSUMUKARI_MURAMASA)
         || ProjectileThreatClassifier.classify(source).contains(FactBypass.SURE_HIT)
         || UshiwakamaruCombatHelper.isGuaranteedHit(source, player.level().getGameTime())) return false;
      boolean projectile = source.getDirectEntity() instanceof Projectile;
      boolean melee = source.getDirectEntity() == source.getEntity() && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity;
      if (!projectile && !melee) return false;
      double chance = dodgeChance(player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) return false;
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 10, 0.3, 0.45, 0.3, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.35F);
      }
      return true;
   }

   public static void tick(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (has(vars, CLAIRVOYANCE)) {
         MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
         if (current == null || current.getDuration() < 120) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
         }
      }
      if (player.tickCount % 20 == 0) reconcileAttributes(player, vars);
   }

   public static void reconcileAttributes(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      PassiveRank divinity = rank(vars, DIVINITY);
      updateModifier(player.getAttribute(Attributes.MAX_HEALTH), DIVINITY_HEALTH_ID, divinity == null ? 0.0 : divinity.healthBonus());
      updateModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), DIVINITY_ATTACK_ID, divinity == null ? 0.0 : divinity.attackBonus());
      if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) return;
      AttributeModifier current = attribute.getModifier(id);
      if (current != null && Math.abs(current.amount() - amount) < 1.0E-9) return;
      if (current != null) attribute.removeModifier(id);
      if (amount > 0.0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
   }
}
