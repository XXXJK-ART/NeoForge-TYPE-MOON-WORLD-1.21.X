package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import javax.annotation.Nullable;
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
   private ServantCombatSpectacleService() { }

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
