package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Server-owned Enkidu/Gilgamesh 300-gate duel and deterministic cleanup. */
public final class GilgameshDuelState {
   private static final String ACTIVE = "GilEnkiduDuelActive";
   private static final String PARTNER = "GilEnkiduDuelPartner";
   private static final String START = "GilEnkiduDuelStart";
   private static final String RELEASED = "GilEnkiduDuelReleased";
   private static final int VOLLEY_ROUNDS = 12;
   private static final int ROUND_INTERVAL = 20;
   private static final int WEAPONS_PER_SIDE_PER_ROUND = 150;
   private static final int VOLLEY_TICKS = VOLLEY_ROUNDS * ROUND_INTERVAL;
   private static final int FINAL_TICK = VOLLEY_TICKS + 175;
   private GilgameshDuelState() { }

   public static boolean tickGilgamesh(GilgameshEntity gil, ServerLevel level, long now) {
      CompoundTag data = gil.getPersistentData();
      if (!data.getBoolean(ACTIVE)) {
         LivingEntity target = gil.getTarget();
         if (!(target instanceof EnkiduEntity enkidu) || enkidu.getTarget() != gil
            || ServantCombatSystem.getPhase(gil) == ServantCombatPhase.PROBING
            || gil.getCurrentMp() < gil.getMaxMp() * 0.35 || enkidu.getCurrentMp() < enkidu.getMaxMp() * 0.35) return false;
         begin(gil, enkidu, now);
      }
      Entity partnerEntity = data.hasUUID(PARTNER) ? level.getEntity(data.getUUID(PARTNER)) : null;
      if (!(partnerEntity instanceof EnkiduEntity enkidu) || !enkidu.isAlive()) { clear(gil); return false; }
      long elapsed = now - data.getLong(START);
      lockPair(gil, enkidu);
      if (elapsed < VOLLEY_TICKS && elapsed % ROUND_INTERVAL == 0L) spawnPairedBatch(level, gil, enkidu, (int)(elapsed / ROUND_INTERVAL));
      if (elapsed >= VOLLEY_TICKS && !data.getBoolean(RELEASED)) {
         data.putBoolean(RELEASED, true);
         Vec3 dir = enkidu.position().add(0, enkidu.getBbHeight() * 0.5, 0).subtract(gil.position().add(0, gil.getBbHeight() * 0.65, 0)).normalize();
         level.addFreshEntity(new GilgameshEaBeamEntity(level, gil, dir));
         EnkiduCombatHelper.startGilgameshFinale(enkidu, level, gil, now);
         level.playSound(null, gil.blockPosition(), ModSounds.GILGAMESH_VOICE_ENKIDU_DUEL.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
      }
      if (elapsed >= FINAL_TICK) finish(level, gil, enkidu);
      return true;
   }

   public static boolean tickEnkidu(EnkiduEntity enkidu, ServerLevel level) {
      if (!enkidu.getPersistentData().getBoolean(ACTIVE)) return false;
      Entity partner = enkidu.getPersistentData().hasUUID(PARTNER) ? level.getEntity(enkidu.getPersistentData().getUUID(PARTNER)) : null;
      if (!(partner instanceof GilgameshEntity gil) || !gil.isAlive()) { clear(enkidu); return false; }
      lockPair(gil, enkidu);
      return true;
   }

   public static boolean areDuelPartners(LivingEntity a, LivingEntity b) {
      return a != null && b != null && a.getPersistentData().getBoolean(ACTIVE)
         && a.getPersistentData().hasUUID(PARTNER) && b.getUUID().equals(a.getPersistentData().getUUID(PARTNER));
   }

   private static void begin(GilgameshEntity gil, EnkiduEntity enkidu, long now) {
      mark(gil, enkidu.getUUID(), now); mark(enkidu, gil.getUUID(), now);
      gil.setCurrentMp(Math.max(0, gil.getCurrentMp() - gil.getMaxMp() * 0.35));
      enkidu.setCurrentMp(Math.max(0, enkidu.getCurrentMp() - enkidu.getMaxMp() * 0.35));
      gil.triggerNamedActionAnimation("gate_of_babylon");
      enkidu.triggerNamedActionAnimation("age_of_babylon");
   }

   private static void mark(LivingEntity entity, UUID partner, long now) {
      CompoundTag data = entity.getPersistentData();
      data.putBoolean(ACTIVE, true); data.putUUID(PARTNER, partner); data.putLong(START, now); data.putBoolean(RELEASED, false);
      data.putBoolean("GilEnkiduDuelOldInvulnerable", entity.isInvulnerable());
      entity.setInvulnerable(true);
   }

   private static void lockPair(GilgameshEntity gil, EnkiduEntity enkidu) {
      gil.getNavigation().stop(); enkidu.getNavigation().stop();
      gil.setDeltaMovement(Vec3.ZERO); enkidu.setDeltaMovement(Vec3.ZERO);
      gil.faceToward(enkidu.position().add(0, 0.9, 0)); enkidu.faceToward(gil.position().add(0, 0.9, 0));
   }

   private static void spawnPairedBatch(ServerLevel level, GilgameshEntity gil, EnkiduEntity enkidu, int batch) {
      Vec3 a = gil.position().add(0, 4.0, 0); Vec3 b = enkidu.position().add(0, 4.0, 0);
      Vec3 line = b.subtract(a); if (line.lengthSqr() < 1.0E-4) return;
      Vec3 dir = line.normalize(); Vec3 side = new Vec3(-dir.z, 0, dir.x);
      String[] weapons = {"durandal", "gram", "vajra", "harpe", "fangtian_huaji"};
      for (int i = 0; i < WEAPONS_PER_SIDE_PER_ROUND; i++) {
         int column = i % 25;
         int row = i / 25;
         double lane = (column - 12) * 0.72;
         double height = row * 0.9;
         Vec3 fromGil = a.add(side.scale(lane)).add(0, height, 0);
         Vec3 fromEnkidu = b.add(side.scale(-lane)).add(0, height, 0);
         String token = gil.getUUID() + ":" + batch + ":" + i;
         String weapon = weapons[(batch + i) % weapons.length];
         GilgameshGateWeaponProjectileEntity gp = new GilgameshGateWeaponProjectileEntity(level, gil, fromGil, dir, weapon, 0);
         gp.setLaunchDelay(24 + row * 3); gp.setSourceStyle(0); gp.setDuelToken(token);
         GilgameshGateWeaponProjectileEntity ep = new GilgameshGateWeaponProjectileEntity(level, enkidu, fromEnkidu, dir.scale(-1), weapon, 0);
         ep.setLaunchDelay(24 + row * 3); ep.setSourceStyle(2); ep.setDuelToken(token);
         level.addFreshEntity(gp); level.addFreshEntity(ep);
         if (i % 2 == 0) {
            VFXServerEffects.spawn(level, "servant_enkidu_age_of_babylon_gate", fromEnkidu, 160.0);
         }
      }
      Vec3 middle = a.lerp(b, 0.5);
      level.sendParticles(ParticleTypes.END_ROD, middle.x, middle.y, middle.z, 180, 8, 4, 8, 0.16);
   }

   private static void finish(ServerLevel level, GilgameshEntity gil, EnkiduEntity enkidu) {
      int result = gil.getRandom().nextInt(3);
      restore(gil); restore(enkidu);
      if (result == 0 || result == 2) forceDeath(gil);
      if (result == 1 || result == 2) forceDeath(enkidu);
      level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, (gil.getX()+enkidu.getX())*0.5, (gil.getY()+enkidu.getY())*0.5+1, (gil.getZ()+enkidu.getZ())*0.5, 12, 3, 3, 3, 0);
   }

   private static void forceDeath(LivingEntity entity) {
      entity.setInvulnerable(false); entity.invulnerableTime = 0; entity.setHealth(0.0F); entity.die(entity.damageSources().genericKill());
   }
   private static void restore(LivingEntity entity) { entity.setInvulnerable(entity.getPersistentData().getBoolean("GilEnkiduDuelOldInvulnerable")); clear(entity); }
   private static void clear(LivingEntity entity) {
      CompoundTag data=entity.getPersistentData(); data.remove(ACTIVE); data.remove(PARTNER); data.remove(START); data.remove(RELEASED); data.remove("GilEnkiduDuelOldInvulnerable");
   }
}
