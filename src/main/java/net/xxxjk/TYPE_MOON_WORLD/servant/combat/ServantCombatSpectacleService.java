package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;

/** Converts raw combat stats into more visible daily-battle impact shapes. */
public final class ServantCombatSpectacleService {
   private static final Map<ServerLevel, FxBudget> EXCHANGE_BUDGET = new WeakHashMap<>();
   private static final class FxBudget { long tick; int used; }
   private ServantCombatSpectacleService() { }

   /** Small local effects use the existing adaptive client particle budget. */
   public static void exchangeFx(ServantEntity actor, LivingEntity target, boolean dash, boolean guard) {
      if (!(actor.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      String cooldown = "TypeMoonExchangeFx" + (dash ? "Dash" : guard ? "Guard" : "Hit");
      if (now < actor.getPersistentData().getLong(cooldown)) return;
      Vec3 origin = dash ? actor.position().add(0, 0.35, 0) : guard ? target.position().add(0, 1, 0)
         : actor.position().add(target.position()).scale(0.5).add(0, 1, 0);
      if (level.players().stream().noneMatch(p -> p.distanceToSqr(origin) <= 48 * 48)) return;
      FxBudget budget = EXCHANGE_BUDGET.computeIfAbsent(level, ignored -> new FxBudget());
      if (budget.tick != now) { budget.tick = now; budget.used = 0; }
      if (budget.used >= 24) return;
      budget.used++;
      actor.getPersistentData().putLong(cooldown, now + (dash ? 10 : 6));
      VFXServerEffects.spawnOrientedScaled(level, dash ? "servant_exchange_dash" : "servant_exchange_clash", origin,
         dash ? actor.getDeltaMovement() : target.position().subtract(actor.position()), 48,
         (float)Mth.clamp(Config.combatSpectacleIntensity * (guard ? 0.85 : 0.65), 0.35, 1.4));
      if (dash) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
            origin.x, origin.y, origin.z, 8, 0.28, 0.12, 0.28, 0.06);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
            origin.x, origin.y + 0.25, origin.z, 4, 0.2, 0.2, 0.2, 0.04);
      } else {
         // A clash gets a crisp flash even when the custom VFX budget is full.
         level.sendParticles(guard ? net.minecraft.core.particles.ParticleTypes.ENCHANT
               : net.minecraft.core.particles.ParticleTypes.CRIT,
            origin.x, origin.y, origin.z, guard ? 7 : 10, 0.35, 0.28, 0.35, 0.08);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
            origin.x, origin.y - 0.65, origin.z, 6, 0.35, 0.08, 0.35, 0.04);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
            origin.x, origin.y, origin.z, 3, 0.22, 0.22, 0.22, 0.05);
         if (!actor.getMainHandItem().isEmpty() || !target.getMainHandItem().isEmpty()) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
               origin.x, origin.y, origin.z, guard ? 16 : 12, 0.42, 0.36, 0.42, 0.12);
         }
      }
      level.playSound(null, origin.x, origin.y, origin.z,
         guard ? SoundEvents.SHIELD_BLOCK : dash ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.ANVIL_HIT,
         SoundSource.HOSTILE, dash ? 0.35F : 0.45F, guard ? 1.6F : dash ? 1.7F : 1.9F);
   }

   public static double strengthScale(@Nullable ServantEntity attacker) {
      ServantParams params = attacker == null || attacker.getDefinition() == null ? null : attacker.getDefinition().parameters();
      int strength = ServantCombatFormulas.strengthStep(params);
      return strengthScaleForStep(strength, attacker instanceof HeraclesEntity,
         attacker instanceof GawainEntity gawain && GawainCombatHelper.hasSunBlessing(gawain),
         Config.combatSpectacleIntensity);
   }

   public static double enduranceResistanceScale(@Nullable LivingEntity target) {
      ServantParams params = target instanceof ServantEntity servant && servant.getDefinition() != null
         ? servant.getDefinition().parameters() : null;
      int endurance = ServantCombatFormulas.enduranceStep(params);
      double knockbackResistance = target == null ? 0.0
         : target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
      return enduranceResistanceScaleForStep(endurance, knockbackResistance);
   }

   static double strengthScaleForStep(int strengthStep, boolean heraclesStyle, boolean sunBlessing, double intensity) {
      double scale = 0.88 + Mth.clamp(strengthStep, 0, 5) * 0.12;
      if (heraclesStyle) scale += 0.18;
      if (sunBlessing) scale += 0.14;
      return scale * Math.max(0.5, intensity);
   }

   static double enduranceResistanceScaleForStep(int enduranceStep, double knockbackResistance) {
      double scale = 1.0 + Mth.clamp(enduranceStep, 0, 5) * 0.055;
      scale += Mth.clamp(knockbackResistance, 0.0, 0.9) * 0.75;
      return Math.max(0.72, Math.min(1.85, scale));
   }

   public static double impactScale(@Nullable ServantEntity attacker, @Nullable LivingEntity target) {
      return Mth.clamp(strengthScale(attacker) / Math.max(0.72, enduranceResistanceScale(target)), 0.65, 2.2);
   }

   public static boolean shouldProtectFooting() {
      return Config.protectCombatFooting;
   }

   public static boolean isForcedBreaker(@Nullable ServantEntity attacker) {
      if (attacker instanceof HeraclesEntity) {
         return true;
      }
      return attacker instanceof GawainEntity gawain && GawainCombatHelper.hasSunBlessing(gawain);
   }

   public static int chainImpactLimit(@Nullable ServantEntity attacker) {
      if (isForcedBreaker(attacker)) {
         return Math.max(2, Config.maxCinematicChainImpacts);
      }
      ServantParams params = attacker == null || attacker.getDefinition() == null ? null : attacker.getDefinition().parameters();
      int endurance = ServantCombatFormulas.enduranceStep(params);
      int strength = ServantCombatFormulas.strengthStep(params);
      return Mth.clamp(1 + (strength + endurance) / 4, 1, Config.maxCinematicChainImpacts);
   }

   public static TerrainImpactProfile routineGroundProfile(@Nullable ServantEntity attacker, @Nullable LivingEntity target,
                                                           double verticalPower, boolean heavyFx) {
      double scale = impactScale(attacker, target);
      TerrainImpactProfile.Tier tier = heavyFx || scale >= 1.6
         ? TerrainImpactProfile.Tier.HEAVY
         : scale >= 1.25 ? TerrainImpactProfile.Tier.MEDIUM : TerrainImpactProfile.Tier.SMALL;
      TerrainImpactProfile base = TerrainImpactProfile.of(tier);
      double radius = Mth.clamp(base.radius() * (0.8 + scale * 0.28), 1.8, heavyFx ? 6.5 : 4.75);
      int debris = Math.max(base.debrisCount(), (int)Math.round((base.debrisCount() + 8) * Math.max(1.0, scale)));
      int dust = Math.max(base.dustCount(), (int)Math.round((base.dustCount() + 18) * Math.max(1.0, scale)));
      return new TerrainImpactProfile(tier, radius, base.maximumHardness(), debris, dust);
   }

   public static TerrainImpactProfile wallTunnelProfile(@Nullable ServantEntity attacker, @Nullable LivingEntity target,
                                                        boolean heavyFx) {
      double scale = impactScale(attacker, target);
      TerrainImpactProfile.Tier tier = heavyFx || scale >= 1.55
         ? TerrainImpactProfile.Tier.HEAVY
         : scale >= 1.18 ? TerrainImpactProfile.Tier.MEDIUM : TerrainImpactProfile.Tier.SMALL;
      TerrainImpactProfile base = TerrainImpactProfile.of(tier);
      double radius = Mth.clamp(base.radius() * (0.45 + scale * 0.22), 1.35, heavyFx ? 4.25 : 3.25);
      int debris = Math.max(base.debrisCount(), (int)Math.round((base.debrisCount() + 10) * Math.max(1.0, scale)));
      int dust = Math.max(base.dustCount(), (int)Math.round((base.dustCount() + 24) * Math.max(1.0, scale)));
      return new TerrainImpactProfile(tier, radius, base.maximumHardness(), debris, dust);
   }

   public static TerrainImpactProfile breakthroughProfile(@Nullable ServantEntity attacker, @Nullable LivingEntity target,
                                                          boolean heavyFx) {
      double scale = impactScale(attacker, target);
      TerrainImpactProfile.Tier tier = heavyFx || scale >= 1.45
         ? TerrainImpactProfile.Tier.HEAVY
         : scale >= 1.1 ? TerrainImpactProfile.Tier.MEDIUM : TerrainImpactProfile.Tier.SMALL;
      TerrainImpactProfile base = TerrainImpactProfile.of(tier);
      double radius = Mth.clamp(base.radius() * (0.4 + scale * 0.18), 1.2, heavyFx ? 3.8 : 3.0);
      int debris = Math.max(base.debrisCount(), (int)Math.round((base.debrisCount() + 6) * Math.max(1.0, scale)));
      int dust = Math.max(base.dustCount(), (int)Math.round((base.dustCount() + 12) * Math.max(1.0, scale)));
      return new TerrainImpactProfile(tier, radius, base.maximumHardness(), debris, dust);
   }

   public static double horizontalPower(@Nullable ServantEntity attacker, @Nullable LivingEntity target, double base) {
      return Mth.clamp(base * impactScale(attacker, target), 0.0, 4.5);
   }

   public static double verticalPower(@Nullable ServantEntity attacker, @Nullable LivingEntity target, double base) {
      double scale = impactScale(attacker, target);
      return Mth.clamp(base * (0.72 + scale * 0.18), 0.0, 2.2);
   }

   public static double wallTunnelLength(@Nullable ServantEntity attacker, @Nullable LivingEntity target, double horizontalPower) {
      double scale = impactScale(attacker, target);
      return Mth.clamp(1.5 + horizontalPower * (1.1 + scale * 0.5), 2.0, isForcedBreaker(attacker) ? 10.0 : 7.0);
   }

   public static int wallTunnelWidth(@Nullable ServantEntity attacker, @Nullable LivingEntity target, double horizontalPower) {
      double scale = impactScale(attacker, target);
      return Mth.clamp((int)Math.ceil(1.0 + horizontalPower * 0.35 + scale * 0.75), 2, isForcedBreaker(attacker) ? 5 : 4);
   }

   public static int wallTunnelHeight(@Nullable ServantEntity attacker, @Nullable LivingEntity target, double verticalPower) {
      double scale = impactScale(attacker, target);
      return Mth.clamp((int)Math.ceil(2.0 + verticalPower * 1.15 + scale * 0.35), 3, isForcedBreaker(attacker) ? 6 : 5);
   }
}
