package net.xxxjk.TYPE_MOON_WORLD.combat.ai;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition.FactBypass;
import net.xxxjk.TYPE_MOON_WORLD.combat.OriginBulletHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;

/** Classifies only bypasses represented by real damage tags or explicit projectile markers. */
public final class ProjectileThreatClassifier {
   public static final String EXPLOSIVE_TAG = "TypeMoonProjectileExplosive";
   public static final String PIERCING_TAG = "TypeMoonProjectilePiercing";
   public static final String SURE_HIT_TAG = "TypeMoonProjectileSureHit";
   public static final String RULE_BREAKER_TAG = "TypeMoonProjectileRuleBreaker";

   private ProjectileThreatClassifier() { }

   public static Set<FactBypass> classify(Projectile projectile) {
      if (projectile == null) return Set.of();
      EnumSet<FactBypass> result = EnumSet.noneOf(FactBypass.class);
      var data = projectile.getPersistentData();
      if (data.getBoolean(EXPLOSIVE_TAG)) result.add(FactBypass.EXPLOSION);
      if (data.getBoolean(PIERCING_TAG)) result.add(FactBypass.PIERCING);
      if (data.getBoolean(SURE_HIT_TAG)) result.add(FactBypass.SURE_HIT);
      if (data.getBoolean(RULE_BREAKER_TAG)) result.add(FactBypass.RULE_BREAKER);
      if (projectile instanceof GaeBulgProjectileEntity || projectile instanceof GaeBulgArmyProjectileEntity) {
         result.add(FactBypass.SURE_HIT);
      }
      if (projectile instanceof BrokenPhantasmProjectileEntity
         || projectile instanceof CrimsonHoundProjectileEntity
         || projectile instanceof PseudoSpiralSwordProjectileEntity
         || projectile instanceof SwordBarrelProjectileEntity sword && sword.isBrokenPhantasm()) {
         result.add(FactBypass.EXPLOSION);
      }
      return Set.copyOf(result);
   }

   public static Set<FactBypass> classify(DamageSource source) {
      if (source == null) return Set.of();
      EnumSet<FactBypass> result = EnumSet.noneOf(FactBypass.class);
      if (source.is(DamageTypeTags.IS_EXPLOSION)) result.add(FactBypass.EXPLOSION);
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) result.add(FactBypass.PIERCING);
      if (source.is(FanaticDamageTypes.GUARANTEED_HITS)) result.add(FactBypass.SURE_HIT);
      if (OriginBulletHelper.isOriginBulletDamage(source)) result.add(FactBypass.RULE_BREAKER);
      Entity direct = source.getDirectEntity();
      if (direct instanceof Projectile projectile) result.addAll(classify(projectile));
      return Set.copyOf(result);
   }

   public static boolean bypassesProjectileNegation(Set<FactBypass> bypasses) {
      return bypasses != null && (bypasses.contains(FactBypass.EXPLOSION)
         || bypasses.contains(FactBypass.PIERCING)
         || bypasses.contains(FactBypass.SURE_HIT)
         || bypasses.contains(FactBypass.RULE_BREAKER));
   }
}
