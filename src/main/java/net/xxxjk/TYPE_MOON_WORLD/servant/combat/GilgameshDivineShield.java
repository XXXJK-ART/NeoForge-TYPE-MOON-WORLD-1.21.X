package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

public final class GilgameshDivineShield {
   public static final int DURATION_TICKS = 15 * 20;
   public static final float MAX_HP = 2000.0F;
   public static final float NON_PROJECTILE_ABSORPTION = 0.80F;
   public static final String TAG_UNTIL = "GilgameshDivineShieldUntil";
   public static final String TAG_HP = "GilgameshDivineShieldHp";

   private GilgameshDivineShield() {
   }

   public static void activate(LivingEntity defender) {
      CompoundTag data = defender.getPersistentData();
      data.putLong(TAG_UNTIL, defender.level().getGameTime() + DURATION_TICKS);
      data.putFloat(TAG_HP, MAX_HP);
      playActivationFx(defender);
   }

   public static void tick(LivingEntity defender) {
      CompoundTag data = defender.getPersistentData();
      if (data.getLong(TAG_UNTIL) > defender.level().getGameTime() && data.getFloat(TAG_HP) > 0.0F) {
         return;
      }
      clear(defender);
   }

   public static void clear(LivingEntity defender) {
      defender.getPersistentData().remove(TAG_UNTIL);
      defender.getPersistentData().remove(TAG_HP);
   }

   public static boolean isActive(LivingEntity defender) {
      CompoundTag data = defender.getPersistentData();
      return data.getLong(TAG_UNTIL) > defender.level().getGameTime() && data.getFloat(TAG_HP) > 0.0F;
   }

   public static ShieldHit tryAbsorb(LivingEntity defender, DamageSource source, float incomingDamage) {
      if (defender == null || source == null || incomingDamage <= 0.0F
         || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !isActive(defender)) {
         return null;
      }

      CompoundTag data = defender.getPersistentData();
      ShieldHit hit = absorb(data.getFloat(TAG_HP), incomingDamage, source.getDirectEntity() instanceof Projectile);
      if (hit.broken()) {
         clear(defender);
      } else {
         data.putFloat(TAG_HP, hit.remainingShieldHp());
      }
      playHitFx(defender, hit.broken());
      return hit;
   }

   public static ShieldHit absorb(float shieldHp, float incomingDamage, boolean projectile) {
      float safeShieldHp = Math.max(0.0F, shieldHp);
      float safeDamage = Math.max(0.0F, incomingDamage);
      float absorptionRate = projectile ? 1.0F : NON_PROJECTILE_ABSORPTION;
      float absorbed = Math.min(safeShieldHp, safeDamage * absorptionRate);
      float remainingShieldHp = Math.max(0.0F, safeShieldHp - absorbed);
      float remainingDamage = Math.max(0.0F, safeDamage - absorbed);
      return new ShieldHit(remainingShieldHp, remainingDamage, absorbed, remainingShieldHp <= 0.0F, projectile);
   }

   private static void playActivationFx(LivingEntity defender) {
      if (!(defender.level() instanceof ServerLevel level)) return;
      level.sendParticles(ParticleTypes.END_ROD, defender.getX(), defender.getY() + defender.getBbHeight() * 0.55,
         defender.getZ(), 32, 0.75, 0.8, 0.75, 0.04);
      level.playSound(null, defender.blockPosition(), SoundEvents.SHIELD_BLOCK, soundSource(defender), 1.25F, 0.75F);
   }

   private static void playHitFx(LivingEntity defender, boolean broken) {
      if (!(defender.level() instanceof ServerLevel level)) return;
      level.sendParticles(broken ? ParticleTypes.FLASH : ParticleTypes.ENCHANT,
         defender.getX(), defender.getY() + defender.getBbHeight() * 0.55, defender.getZ(),
         broken ? 1 : 18, 0.55, 0.65, 0.55, 0.04);
      level.playSound(null, defender.blockPosition(), broken ? SoundEvents.SHIELD_BREAK : SoundEvents.SHIELD_BLOCK,
         soundSource(defender), 1.1F, broken ? 0.65F : 1.2F);
   }

   private static SoundSource soundSource(LivingEntity defender) {
      return defender instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
   }

   public record ShieldHit(
      float remainingShieldHp,
      float remainingDamage,
      float absorbedDamage,
      boolean broken,
      boolean projectile
   ) {
   }
}
