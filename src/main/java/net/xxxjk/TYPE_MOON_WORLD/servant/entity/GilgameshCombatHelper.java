package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatPhase;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;

/** Gilgamesh-specific ranged AI and passive state. */
public final class GilgameshCombatHelper {
   private static final String LAST_GATE = "GilgameshLastGate";
   private static final String LAST_CHAIN = "GilgameshLastChain";
   private static final String LAST_EA = "GilgameshLastEa";
   private static final String LAST_CROSS = "GilgameshLastCross";
   private static final String LAST_CHARISMA = "GilgameshLastCharisma";
   private GilgameshCombatHelper() { }

   public static void tick(GilgameshEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean("GilgameshPassivesInitialized")) {
         data.putBoolean("GilgameshPassivesInitialized", true);
         data.putFloat("MagicResistanceDamageReduction", 0.20F);
         data.putFloat("GilgameshCommandObedienceMin", 0.20F);
         data.putFloat("GilgameshCommandObedienceMax", 0.60F);
      }
      updateMonotonicPhase(entity);
      if (entity.tickCount % 20 == 0) {
         entity.setCurrentMp(Math.min(entity.getMaxMp(), entity.getCurrentMp() + Math.max(0.25, entity.getMaxMp() * 0.013)));
      }
      LivingEntity target = entity.getTarget();
      updateFlight(entity, target, now);
      if (target == null || !target.isAlive()) return;
      if (GilgameshDuelState.tickGilgamesh(entity, level, now)) return;
      entity.getLookControl().setLookAt(target, 40.0F, 40.0F);
      double distance = entity.distanceTo(target);
      if (distance < 10.0) {
         Vec3 away = entity.position().subtract(target.position()).multiply(1, 0, 1);
         if (away.lengthSqr() > 1.0E-4) entity.setDeltaMovement(entity.getDeltaMovement().add(away.normalize().scale(0.35)));
      } else if (distance >= 15.0) {
         entity.getNavigation().stop();
      }

      if (tryChains(entity, level, target, now, data)) return;
      if (tryCrossSlash(entity, level, target, now, data)) return;
      if (tryEa(entity, level, target, now, data)) return;
      int gateCooldown = isProjectionCounterTarget(target) ? 320 : 90;
      if (now - data.getLong(LAST_GATE) >= gateCooldown && entity.getCurrentMp() >= 14.4) {
         ServantCombatPhase phase = ServantCombatSystem.getPhase(entity);
         fireGateVolley(entity, level, target, phase == ServantCombatPhase.DECISIVE ? 120 : 48,
            phase == ServantCombatPhase.DECISIVE ? 24.0F : 18.0F);
         data.putLong(LAST_GATE, now);
         entity.setCurrentMp(entity.getCurrentMp() - 14.4);
      }
      if (now - data.getLong(LAST_CHARISMA) >= 700 && entity.getCurrentMp() >= 20.0 && entity.getRandom().nextFloat() < 0.08F) {
         useCharisma(entity, level, data, now);
      }
   }

   private static void updateMonotonicPhase(GilgameshEntity entity) {
      double ratio = entity.getHealth() / Math.max(1.0, entity.getMaxHealth());
      if (ratio <= 0.40) ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.DECISIVE);
      else if (ratio <= 0.60) ServantCombatSystem.forcePhaseAtLeast(entity, ServantCombatPhase.NORMAL);
   }

   public static boolean isFlying(GilgameshEntity entity) {
      return entity.getPersistentData().getBoolean("GilgameshFlying");
   }

   private static void updateFlight(GilgameshEntity entity, LivingEntity target, long now) {
      CompoundTag data = entity.getPersistentData();
      if (target == null || !target.isAlive() || ServantCombatSystem.cannotAct(entity)) {
         data.putBoolean("GilgameshFlying", false); entity.setNoGravity(false); return;
      }
      double distance = entity.distanceTo(target);
      boolean fly = !entity.onGround() || !target.onGround() || target.getY() > entity.getY() + 2.0 || distance > 10.0;
      data.putBoolean("GilgameshFlying", fly);
      if (!fly) { entity.setNoGravity(false); return; }
      entity.getNavigation().stop(); entity.setNoGravity(true); entity.fallDistance = 0.0F;
      double desiredY = Math.max(target.getY() + 5.0, entity.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, entity.blockPosition()).getY() + 6.0);
      Vec3 away = entity.position().subtract(target.position()).multiply(1, 0, 1);
      if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
      away = away.normalize();
      double radial = distance < 13.0 ? 0.18 : distance > 18.0 ? -0.12 : 0.0;
      Vec3 orbit = new Vec3(-away.z, 0, away.x).scale(0.10);
      double vertical = net.minecraft.util.Mth.clamp((desiredY - entity.getY()) * 0.08, -0.22, 0.22);
      entity.setDeltaMovement(entity.getDeltaMovement().scale(0.58).add(away.scale(radial)).add(orbit).add(0, vertical, 0));
   }

   private static boolean tryChains(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      boolean divine = ServantIdentityHelper.hasTrait(target, ServantTraitTag.DIVINE)
         || ServantIdentityHelper.hasTrait(target, ServantTraitTag.CELESTIAL);
      if (entity.distanceTo(target) > 30 || now - data.getLong(LAST_CHAIN) < 300 || entity.getCurrentMp() < 24.0) return false;
      if (!divine && entity.distanceTo(target) > 5.0) return false;
      int duration = divine ? 200 : 100;
      target.setDeltaMovement(Vec3.ZERO);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, divine ? 20 : 8, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, divine ? 20 : 8, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, divine ? 10 : 4, false, true, true));
      target.getPersistentData().putLong("ChainsOfHeavenBoundUntil", now + duration);
      target.getPersistentData().putBoolean("ChainsOfHeavenBlocksTeleport", true);
      level.addFreshEntity(new ChainsOfHeavenBindingEntity(level, entity, target, duration, divine));
      TYPE_MOON_WORLD.queueServerWork(duration, () -> target.getPersistentData().remove("ChainsOfHeavenBlocksTeleport"));
      entity.triggerNamedActionAnimation("chain_of_heaven");
      entity.setCurrentMp(entity.getCurrentMp() - 24.0);
      data.putLong(LAST_CHAIN, now);
      return true;
   }

   private static boolean tryEa(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (ServantCombatSystem.getPhase(entity) == ServantCombatPhase.PROBING || now - data.getLong(LAST_EA) < 1200 || entity.getCurrentMp() < 200.0) return false;
      float chance = ServantCombatSystem.getPhase(entity) == ServantCombatPhase.DECISIVE ? 0.035F : 0.012F;
      if (entity.getRandom().nextFloat() > chance) return false;
      if (UBWInstanceManager.isUbwDimension(level)
         && (target instanceof EmiyaArcherEntity || target instanceof net.minecraft.server.level.ServerPlayer player
            && "emiya_archer".equals(player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_id))) {
         entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10, false, true, true));
         entity.getPersistentData().putLong("GilgameshEaInterruptedUntil", now + 40);
         data.putLong(LAST_EA, now);
         return true;
      }
      Vec3 look = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(entity.position().add(0, entity.getBbHeight() * 0.65, 0)).normalize();
      entity.triggerNamedActionAnimation("ea_unlock");
      level.addFreshEntity(new GilgameshEaBeamEntity(level, entity, look));
      data.putLong(LAST_EA, now);
      return true;
   }

   private static boolean tryCrossSlash(GilgameshEntity entity, ServerLevel level, LivingEntity target, long now, CompoundTag data) {
      if (ServantCombatSystem.getPhase(entity) != ServantCombatPhase.DECISIVE || now - data.getLong(LAST_CROSS) < 1800 || entity.tickCount % 40 != 0) return false;
      if (entity.getRandom().nextFloat() >= 0.05F) return false;
      Vec3 direction = target.position().subtract(entity.position()).multiply(1, 0, 1).normalize();
      data.putLong("GilgameshCrossSlashCopyUntil", now + 32L);
      data.putDouble("GilgameshCrossSlashDirX", direction.x); data.putDouble("GilgameshCrossSlashDirY", direction.y); data.putDouble("GilgameshCrossSlashDirZ", direction.z);
      entity.triggerNamedActionAnimation("igalima_slash");
      GilgameshCrossSlashEntity green = new GilgameshCrossSlashEntity(level, entity, GilgameshCrossSlashEntity.SlashType.IGALIMA, direction);
      if (target instanceof EmiyaArcherEntity emiya) green.addImmuneEntity(emiya);
      level.addFreshEntity(green);
      TYPE_MOON_WORLD.queueServerWork(8, () -> {
         if (entity.isAlive() && entity.level() == level) {
            entity.triggerNamedActionAnimation("sulsagana_slash");
            GilgameshCrossSlashEntity white = new GilgameshCrossSlashEntity(level, entity, GilgameshCrossSlashEntity.SlashType.SULSAGANA, direction);
            if (target instanceof EmiyaArcherEntity emiya) white.addImmuneEntity(emiya);
            level.addFreshEntity(white);
         }
      });
      if (target instanceof EmiyaArcherEntity emiya) {
         Vec3 counterDirection = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(emiya.position().add(0, emiya.getBbHeight() * 0.5, 0)).normalize();
         GilgameshCrossSlashEntity counterGreen = new GilgameshCrossSlashEntity(level, emiya, GilgameshCrossSlashEntity.SlashType.IGALIMA, counterDirection);
         counterGreen.addImmuneEntity(entity); counterGreen.addImmuneEntity(emiya); level.addFreshEntity(counterGreen);
         TYPE_MOON_WORLD.queueServerWork(8, () -> {
            if (entity.isAlive() && emiya.isAlive() && emiya.level() == level) {
               GilgameshCrossSlashEntity counterWhite = new GilgameshCrossSlashEntity(level, emiya, GilgameshCrossSlashEntity.SlashType.SULSAGANA, counterDirection);
               counterWhite.addImmuneEntity(entity); counterWhite.addImmuneEntity(emiya); level.addFreshEntity(counterWhite);
            }
         });
         VFXServerEffects.spawn(level, "servant_emiya_projection", emiya, 256.0);
      }
      VFXServerEffects.spawn(level, "gilgamesh_cross_slash", entity.position(), 256.0);
      data.putLong(LAST_CROSS, now);
      return true;
   }

   public static void fireGateVolley(GilgameshEntity entity, ServerLevel level, LivingEntity target, int count, float damage) {
      boolean projectionCounter = isProjectionCounterTarget(target);
      boolean ubw = projectionCounter && UBWInstanceManager.isUbwDimension(level);
      if (projectionCounter) {
         count = 150 * 12;
      }
      int waveSize = projectionCounter ? 150 : count >= 100 ? 20 : 16;
      int waves = projectionCounter ? 12 : (count + waveSize - 1) / waveSize;
      final int totalCount = count;
      String weapon = selectWeapon(target);
      for (int wave = 0; wave < waves; wave++) {
         int waveIndex = wave;
         TYPE_MOON_WORLD.queueServerWork(wave * (ubw ? 15 : 10), () -> {
            if (!entity.isAlive() || !target.isAlive() || entity.level() != level) return;
            int amount = Math.min(waveSize, totalCount - waveIndex * waveSize);
            Vec3 forward = target.position().add(0, target.getBbHeight() * 0.55, 0)
               .subtract(entity.position().add(0, entity.getBbHeight() * 0.55, 0)).normalize();
            int columns = gateColumns(amount);
            for (int i = 0; i < amount; i++) {
               Vec3 gate = arrangedGatePosition(entity.position().add(0, 2.5, 0), forward, i, amount);
               Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(gate).normalize();
               GilgameshGateWeaponProjectileEntity projectile = new GilgameshGateWeaponProjectileEntity(level, entity, gate, aim, weapon, damage);
               projectile.setLaunchDelay(24 + Math.min(16, (i / columns) * 3));
               if (projectionCounter) { projectile.setSourceStyle(0); projectile.setDuelToken(entity.getUUID() + ":projection:" + waveIndex + ":" + i); }
               level.addFreshEntity(projectile);
               level.sendParticles(ParticleTypes.END_ROD, gate.x, gate.y, gate.z, 14, 0.35, 0.35, 0.35, 0.02);
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.HOSTILE, 1.2F, 1.45F);
         });
         if (projectionCounter) {
            TYPE_MOON_WORLD.queueServerWork(wave * (ubw ? 5 : 10), () -> spawnProjectionCounterWave(level, entity, target, totalCount, waveSize, waveIndex, weapon));
         }
      }
   }

   private static boolean isProjectionCounterTarget(LivingEntity target) {
      if (target instanceof EmiyaArcherEntity) return true;
      if (target instanceof net.minecraft.server.level.ServerPlayer player) {
         var vars = player.getData(net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "emiya_archer".equals(vars.servant_card_id);
      }
      return false;
   }

   private static void spawnProjectionCounterWave(ServerLevel level, GilgameshEntity gil, LivingEntity emiya, int count, int waveSize, int waveIndex, String weapon) {
      if (!gil.isAlive() || !emiya.isAlive() || gil.level() != level) return;
      int amount = Math.min(waveSize, count - waveIndex * waveSize);
      Vec3 center = emiya.position().add(0, 3.0, 0);
      Vec3 forward = gil.position().add(0, gil.getBbHeight() * 0.55, 0).subtract(center).normalize();
      int columns = gateColumns(amount);
      for (int i=0;i<amount;i++) {
         Vec3 gate=arrangedGatePosition(center,forward,i,amount);
         Vec3 aim=gil.position().add(0,gil.getBbHeight()*0.55,0).subtract(gate).normalize();
         GilgameshGateWeaponProjectileEntity counter=new GilgameshGateWeaponProjectileEntity(level,emiya,gate,aim,weapon,0);
         counter.setLaunchDelay(24 + Math.min(16, (i / columns) * 3));
         counter.setSourceStyle(1); counter.setDuelToken(gil.getUUID()+":projection:"+waveIndex+":"+i); level.addFreshEntity(counter);
         VFXServerEffects.spawn(level,"servant_emiya_projection",gate,160.0);
      }
   }

   private static int gateColumns(int amount) {
      if (amount >= 100) return 25;
      if (amount >= 18) return 9;
      return Math.max(4, Math.min(8, amount));
   }

   private static Vec3 arrangedGatePosition(Vec3 center, Vec3 forward, int index, int amount) {
      if (forward.lengthSqr() < 1.0E-6) forward = new Vec3(0, 0, 1);
      forward = forward.normalize();
      Vec3 right = forward.cross(new Vec3(0, 1, 0));
      if (right.lengthSqr() < 1.0E-6) right = new Vec3(1, 0, 0);
      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      int columns = gateColumns(amount);
      int row = index / columns;
      int column = index % columns;
      int rowCount = Math.min(columns, amount - row * columns);
      double horizontal = (column - (rowCount - 1) * 0.5) * (amount >= 100 ? 0.78 : 1.35);
      double vertical = row * (amount >= 100 ? 0.9 : 1.25);
      double depth = 1.8 + row * 0.22 + Math.abs(column - (rowCount - 1) * 0.5) * 0.035;
      return center.add(right.scale(horizontal)).add(up.scale(vertical)).subtract(forward.scale(depth));
   }

   private static String selectWeapon(LivingEntity target) {
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)) return "gram";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.UNDEAD)) return "harpe";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.MECHANICAL) || !target.onGround()) return "vajra";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.GIANT)) return "fangtian_huaji";
      if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.SABER)) return "durandal";
      String[] fallback = {"durandal", "gram", "vajra", "harpe", "fangtian_huaji"};
      return fallback[target.getRandom().nextInt(fallback.length)];
   }

   private static void useCharisma(GilgameshEntity entity, ServerLevel level, CompoundTag data, long now) {
      AABB area = entity.getBoundingBox().inflate(20.0);
      for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, area, e -> e == entity || e.isAlliedTo(entity))) {
         ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0, false, true, true));
      }
      entity.setCurrentMp(entity.getCurrentMp() - 20.0);
      data.putLong(LAST_CHARISMA, now);
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 30, 2.5, 1.2, 2.5, 0.05);
   }
}
