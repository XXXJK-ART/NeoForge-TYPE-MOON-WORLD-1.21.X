package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;

/** Server-owned Enkidu/Gilgamesh duel state. */
public final class GilgameshDuelState {
   private static final String ACTIVE = "GilEnkiduDuelActive";
   private static final String PARTNER = "GilEnkiduDuelPartner";
   private static final String START = "GilEnkiduDuelStart";
   private static final String RELEASED = "GilEnkiduDuelReleased";
   private static final String SYNCHRONIZED_RELEASED = "GilEnkiduDuelSynchronizedReleased";
   private static final String RETREAT_START = "GilEnkiduDuelRetreatStart";
   private static final String RETREAT_END = "GilEnkiduDuelRetreatEnd";
   private static final String RETREAT_GIL_X = "GilEnkiduDuelRetreatGilX";
   private static final String RETREAT_GIL_Y = "GilEnkiduDuelRetreatGilY";
   private static final String RETREAT_GIL_Z = "GilEnkiduDuelRetreatGilZ";
   private static final String RETREAT_ENK_X = "GilEnkiduDuelRetreatEnkX";
   private static final String RETREAT_ENK_Y = "GilEnkiduDuelRetreatEnkY";
   private static final String RETREAT_ENK_Z = "GilEnkiduDuelRetreatEnkZ";
   private static final String FINALE_START = "GilEnkiduDuelFinaleStart";
   private static final String RUSH_STARTED = "GilEnkiduDuelRushStarted";
   private static final String RUSH_TICK = "GilEnkiduDuelRushTick";
   private static final String CENTER_X = "GilEnkiduDuelCenterX";
   private static final String CENTER_Y = "GilEnkiduDuelCenterY";
   private static final String CENTER_Z = "GilEnkiduDuelCenterZ";
   private static final String FINAL_IMPACT = "GilEnkiduDuelFinalImpact";

   private static final int RETREAT_TICKS = 14;
   private static final double RETREAT_DISTANCE = 15.0;
   private static final float FINALE_HEALTH_THRESHOLD = 80.0F;
   private static final int EA_PRELUDE_TICKS = GilgameshCombatHelper.EA_SUMMON_TICKS + GilgameshCombatHelper.EA_DRAW_TICKS;
   private static final int FINALE_RELEASE_TICKS = 140;
   // Leave enough time for EA's BEAM -> IMPACT tick and controller cleanup.
   private static final int FINAL_TICK_AFTER_RELEASE = FINALE_RELEASE_TICKS + GilgameshEaBeamEntity.BEAM_TICKS + 32;

   private GilgameshDuelState() { }

   public static boolean tickGilgamesh(GilgameshEntity gil, ServerLevel level, long now) {
      CompoundTag data = gil.getPersistentData();
      if (!data.getBoolean(ACTIVE)) {
         LivingEntity target = gil.getTarget();
         if (!(target instanceof EnkiduEntity enkidu) || enkidu.getTarget() != gil
            || gil.getHealth() >= FINALE_HEALTH_THRESHOLD
               && enkidu.getHealth() >= FINALE_HEALTH_THRESHOLD) return false;
         begin(gil, enkidu, now);
      }
      Entity partnerEntity = data.hasUUID(PARTNER) ? level.getEntity(data.getUUID(PARTNER)) : null;
      if (!(partnerEntity instanceof EnkiduEntity enkidu) || !enkidu.isAlive()) { clear(gil); return false; }

      boolean synchronizedFinale = data.getBoolean(SYNCHRONIZED_RELEASED);
      if (!data.getBoolean(RELEASED) || !synchronizedFinale) {
         if (!data.getBoolean(RELEASED)) {
            if (!data.contains(RETREAT_START)) beginRetreat(gil, enkidu, now);
            if (now < data.getLong(RETREAT_END)) {
               tickRetreat(gil, enkidu, data, now);
               return true;
            }
            finishRetreat(gil, enkidu, data, now);
            return true;
         }
         long finaleStart = data.getLong(FINALE_START);
         if (!synchronizedFinale && finaleStart > 0L && now >= finaleStart) {
            data.putBoolean(SYNCHRONIZED_RELEASED, true);
            enkidu.getPersistentData().putBoolean(SYNCHRONIZED_RELEASED, true);
            EnkiduCombatHelper.startGilgameshFinale(enkidu, level, gil, now);
            level.playSound(null, gil.blockPosition(), ModSounds.GILGAMESH_VOICE_ENKIDU_DUEL.get(), SoundSource.HOSTILE, 8.0F, 1.0F);
         }
         if (!data.getBoolean(SYNCHRONIZED_RELEASED)) lockPair(gil, enkidu);
         return true;
      }

      // Once the synchronized finale begins, Enkidu is free to perform its rush.
      if (now - data.getLong(FINALE_START) >= FINAL_TICK_AFTER_RELEASE) finish(level, gil, enkidu);
      return true;
   }

   /** Stops both beams at Enkidu's impact point and resolves the duel immediately. */
   public static void completeDuelRush(EnkiduEntity enkidu, ServerLevel level, GilgameshEntity gil, Vec3 impact) {
      if (enkidu == null || gil == null || level == null || impact == null || !areDuelPartners(gil, enkidu)) return;
      CompoundTag data = gil.getPersistentData();
      if (data.getBoolean(FINAL_IMPACT)) return;
      data.putDouble(CENTER_X, impact.x);
      data.putDouble(CENTER_Y, impact.y);
      data.putDouble(CENTER_Z, impact.z);
      for (GilgameshEaBeamEntity beam : level.getEntitiesOfClass(GilgameshEaBeamEntity.class,
         gil.getBoundingBox().inflate(10.0), candidate -> candidate.isAlive() && candidate.isOwnedBy(gil))) {
         beam.stopForDuelImpact();
      }
      EnkiduCombatHelper.cancelGilgameshDuelFinale(enkidu);
      finish(level, gil, enkidu);
   }

   public static boolean tickEnkidu(EnkiduEntity enkidu, ServerLevel level) {
      CompoundTag data = enkidu.getPersistentData();
      if (!data.getBoolean(ACTIVE)) return false;
      Entity partner = data.hasUUID(PARTNER) ? level.getEntity(data.getUUID(PARTNER)) : null;
      if (!(partner instanceof GilgameshEntity gil) || !gil.isAlive()) { clear(enkidu); return false; }
      if (!data.getBoolean(SYNCHRONIZED_RELEASED)) lockPair(gil, enkidu);
      return true;
   }

   public static boolean areDuelPartners(LivingEntity a, LivingEntity b) {
      return a != null && b != null && a.getPersistentData().getBoolean(ACTIVE)
         && a.getPersistentData().hasUUID(PARTNER) && b.getUUID().equals(a.getPersistentData().getUUID(PARTNER));
   }

   /** Called by Enkidu on the exact tick its final rush starts. */
   public static void markEnkiduRushStarted(EnkiduEntity enkidu, ServerLevel level, long now) {
      CompoundTag data = enkidu.getPersistentData();
      if (!data.getBoolean(ACTIVE) || !data.getBoolean(SYNCHRONIZED_RELEASED) || data.getBoolean(RUSH_STARTED)) return;
      data.putBoolean(RUSH_STARTED, true);
      data.putLong(RUSH_TICK, now);
      Entity partner = data.hasUUID(PARTNER) ? level.getEntity(data.getUUID(PARTNER)) : null;
      if (partner instanceof GilgameshEntity gil) {
         gil.getPersistentData().putBoolean(RUSH_STARTED, true);
         gil.getPersistentData().putLong(RUSH_TICK, now);
         for (GilgameshEaBeamEntity beam : level.getEntitiesOfClass(GilgameshEaBeamEntity.class,
            gil.getBoundingBox().inflate(8.0), candidate -> candidate.isAlive() && candidate.isOwnedBy(gil))) {
            beam.synchronizeDuelRelease(level);
         }
      }
   }

   public static boolean isEnkiduRushStarted(LivingEntity owner, LivingEntity target) {
      return owner != null && target instanceof EnkiduEntity
         && areDuelPartners(owner, target) && target.getPersistentData().getBoolean(RUSH_STARTED);
   }

   private static void begin(GilgameshEntity gil, EnkiduEntity enkidu, long now) {
      mark(gil, enkidu.getUUID(), now); mark(enkidu, gil.getUUID(), now);
      gil.setCurrentMp(Math.max(0, gil.getCurrentMp() - gil.getMaxMp() * 0.35));
      enkidu.setCurrentMp(Math.max(0, enkidu.getCurrentMp() - enkidu.getMaxMp() * 0.35));
   }

   private static void mark(LivingEntity entity, UUID partner, long now) {
      CompoundTag data = entity.getPersistentData();
      data.putBoolean(ACTIVE, true); data.putUUID(PARTNER, partner); data.putLong(START, now);
      data.putBoolean(RELEASED, false); data.putBoolean(SYNCHRONIZED_RELEASED, false);
      data.putBoolean(RUSH_STARTED, false); data.putBoolean(FINAL_IMPACT, false);
      data.putBoolean("GilEnkiduDuelOldInvulnerable", entity.isInvulnerable());
      entity.setInvulnerable(true);
   }

   private static void lockPair(GilgameshEntity gil, EnkiduEntity enkidu) {
      gil.getNavigation().stop(); enkidu.getNavigation().stop();
      gil.setDeltaMovement(Vec3.ZERO); enkidu.setDeltaMovement(Vec3.ZERO);
      gil.faceToward(enkidu.position().add(0, 0.9, 0)); enkidu.faceToward(gil.position().add(0, 0.9, 0));
   }

   private static void beginRetreat(GilgameshEntity gil, EnkiduEntity enkidu, long now) {
      Vec3 line = enkidu.position().subtract(gil.position()).multiply(1.0, 0.0, 1.0);
      if (line.lengthSqr() < 1.0E-4) line = new Vec3(1.0, 0.0, 0.0);
      Vec3 awayGil = line.normalize().scale(-RETREAT_DISTANCE);
      Vec3 awayEnk = line.normalize().scale(RETREAT_DISTANCE);
      Vec3 gilEnd = gil.position().add(awayGil);
      Vec3 enkEnd = enkidu.position().add(awayEnk);
      CompoundTag data = gil.getPersistentData();
      data.putLong(RETREAT_START, now); data.putLong(RETREAT_END, now + RETREAT_TICKS);
      putPos(data, RETREAT_GIL_X, RETREAT_GIL_Y, RETREAT_GIL_Z, gilEnd);
      putPos(data, RETREAT_ENK_X, RETREAT_ENK_Y, RETREAT_ENK_Z, enkEnd);
      copyRetreatData(data, enkidu.getPersistentData());
   }

   private static void copyRetreatData(CompoundTag from, CompoundTag to) {
      to.putLong(RETREAT_START, from.getLong(RETREAT_START)); to.putLong(RETREAT_END, from.getLong(RETREAT_END));
      to.putDouble(RETREAT_GIL_X, from.getDouble(RETREAT_GIL_X)); to.putDouble(RETREAT_GIL_Y, from.getDouble(RETREAT_GIL_Y)); to.putDouble(RETREAT_GIL_Z, from.getDouble(RETREAT_GIL_Z));
      to.putDouble(RETREAT_ENK_X, from.getDouble(RETREAT_ENK_X)); to.putDouble(RETREAT_ENK_Y, from.getDouble(RETREAT_ENK_Y)); to.putDouble(RETREAT_ENK_Z, from.getDouble(RETREAT_ENK_Z));
   }

   private static void putPos(CompoundTag data, String x, String y, String z, Vec3 pos) {
      data.putDouble(x, pos.x); data.putDouble(y, pos.y); data.putDouble(z, pos.z);
   }

   private static void tickRetreat(GilgameshEntity gil, EnkiduEntity enkidu, CompoundTag data, long now) {
      double progress = Math.max(0.0, Math.min(1.0, (now - data.getLong(RETREAT_START)) / (double)RETREAT_TICKS));
      Vec3 gilEnd = readPos(data, RETREAT_GIL_X, RETREAT_GIL_Y, RETREAT_GIL_Z);
      Vec3 enkEnd = readPos(data, RETREAT_ENK_X, RETREAT_ENK_Y, RETREAT_ENK_Z);
      gil.setPos(gil.position().lerp(gilEnd, progress)); enkidu.setPos(enkidu.position().lerp(enkEnd, progress));
      gil.setDeltaMovement(Vec3.ZERO); enkidu.setDeltaMovement(Vec3.ZERO);
      gil.faceToward(enkidu.position().add(0, 0.9, 0)); enkidu.faceToward(gil.position().add(0, 0.9, 0));
   }

   private static Vec3 readPos(CompoundTag data, String x, String y, String z) {
      return new Vec3(data.getDouble(x), data.getDouble(y), data.getDouble(z));
   }

   private static void finishRetreat(GilgameshEntity gil, EnkiduEntity enkidu, CompoundTag data, long now) {
      gil.setPos(readPos(data, RETREAT_GIL_X, RETREAT_GIL_Y, RETREAT_GIL_Z));
      enkidu.setPos(readPos(data, RETREAT_ENK_X, RETREAT_ENK_Y, RETREAT_ENK_Z));
      Vec3 center = gil.position().lerp(enkidu.position(), 0.5);
      putPos(data, CENTER_X, CENTER_Y, CENTER_Z, center);
      copyRetreatData(data, enkidu.getPersistentData());
      data.putBoolean(RELEASED, true);
      data.putLong(FINALE_START, now + EA_PRELUDE_TICKS);
      gil.getPersistentData().putLong(FINALE_START, now + EA_PRELUDE_TICKS);
      enkidu.getPersistentData().putLong(FINALE_START, now + EA_PRELUDE_TICKS);
      GilgameshCombatHelper.beginNpcEaSummon(gil, (ServerLevel)gil.level(), enkidu, now);
   }

   private static void finish(ServerLevel level, GilgameshEntity gil, EnkiduEntity enkidu) {
      CompoundTag data = gil.getPersistentData();
      if (!data.getBoolean(FINAL_IMPACT)) {
         data.putBoolean(FINAL_IMPACT, true);
         Vec3 center = readPos(data, CENTER_X, CENTER_Y, CENTER_Z);
         GilgameshEaBeamEntity.spawnDuelFinalImpact(level, center, 65.0);
      }
      for (GilgameshEaBeamEntity beam : level.getEntitiesOfClass(GilgameshEaBeamEntity.class,
         gil.getBoundingBox().inflate(10.0), candidate -> candidate.isAlive() && candidate.isOwnedBy(gil))) {
         beam.stopForDuelImpact();
      }
      EnkiduCombatHelper.cancelGilgameshDuelFinale(enkidu);
      GilgameshCombatHelper.clearEaEquipment(gil);
      int result = gil.getRandom().nextInt(3);
      restore(gil); restore(enkidu);
      if (result == 0 || result == 2) forceDeath(gil);
      if (result == 1 || result == 2) forceDeath(enkidu);
   }

   private static void forceDeath(LivingEntity entity) {
      entity.setInvulnerable(false); entity.invulnerableTime = 0; entity.setHealth(0.0F); entity.die(entity.damageSources().genericKill());
   }

   private static void restore(LivingEntity entity) {
      entity.setInvulnerable(entity.getPersistentData().getBoolean("GilEnkiduDuelOldInvulnerable"));
      clear(entity);
   }

   private static void clear(LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      data.remove(ACTIVE); data.remove(PARTNER); data.remove(START); data.remove(RELEASED); data.remove(SYNCHRONIZED_RELEASED);
      data.remove(RETREAT_START); data.remove(RETREAT_END); data.remove(RETREAT_GIL_X); data.remove(RETREAT_GIL_Y); data.remove(RETREAT_GIL_Z);
      data.remove(RETREAT_ENK_X); data.remove(RETREAT_ENK_Y); data.remove(RETREAT_ENK_Z); data.remove(FINALE_START);
      data.remove(RUSH_STARTED); data.remove(RUSH_TICK); data.remove(CENTER_X); data.remove(CENTER_Y); data.remove(CENTER_Z);
      data.remove(FINAL_IMPACT); data.remove("GilEnkiduDuelOldInvulnerable");
   }
}
