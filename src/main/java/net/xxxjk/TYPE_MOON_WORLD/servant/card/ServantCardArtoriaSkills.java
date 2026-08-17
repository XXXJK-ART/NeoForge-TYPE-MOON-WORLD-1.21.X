package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.ManaBurstService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardArtoriaSkills {
   private static final double ARTORIA_MANA_BURST_DRAIN_PER_SECOND = 6.0;
   private static final int ARTORIA_MANA_BURST_DURATION = 1200;
   private static final String ARTORIA_CARD_MANA_BURST_DRAIN_TICK = "ServantCardArtoriaManaBurstDrainTick";
   public static final int WIND_HAMMER_SLOT = 1;
   public static final int WIND_RELEASE_SLOT = 3;

   private ServantCardArtoriaSkills() {
   }

   public static boolean isWindAction(ServantCardSkillAction action) {
      return action != null && ("invisible_air_hammer".equals(action.effectId()) || "invisible_air_release".equals(action.effectId()));
   }

   public static boolean isWindLockedByExcalibur(ServerPlayer player) {
      return PlayerNoblePhantasmHelper.isArtoriaExcaliburWindLocked(player);
   }

   public static void performManaBurst(ServerPlayer player) {
      long until = player.level().getGameTime() + ARTORIA_MANA_BURST_DURATION;
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ARTORIA_MANA_BURST_DURATION, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ARTORIA_MANA_BURST_DURATION, 1, false, true, true));
      player.getPersistentData().putLong(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL, until);
      player.getPersistentData().putInt(ARTORIA_CARD_MANA_BURST_DRAIN_TICK, player.tickCount + 20);
      ManaBurstService.primeExternalJetMovement(player, 5);
      spawnManaBurstActivationFx(player);
   }

   public static void performCharisma(ServerPlayer player) {
      ServantCardSkillUtils.buffNearby(player, 10.0, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 220, 0, false, true, true));
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON);
      data.remove(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL);
      data.remove(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE);
      data.remove(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE + "Until");
      data.remove(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_RELEASED);
      data.remove(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL);
      data.remove(ARTORIA_CARD_MANA_BURST_DRAIN_TICK);
      ManaBurstService.clearExternalJetMovement(player);
      PlayerNoblePhantasmHelper.clearArtoriaExcaliburWindLock(player);
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"artoria_pendragon".equals(vars.servant_card_id)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      data.remove(ArtoriaPendragonCombatHelper.TAG_HAS_AVALON);
      ArtoriaPendragonCombatHelper.tickLakeProtection(player);
      long manaBurstUntil = data.getLong(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL);
      if (manaBurstUntil <= now) {
         data.remove(ARTORIA_CARD_MANA_BURST_DRAIN_TICK);
         ManaBurstService.clearExternalJetMovement(player);
         return;
      }
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, false, true, true));
      ManaBurstService.tickExternalJetMovement(player, 5);
      if (player.tickCount >= data.getInt(ARTORIA_CARD_MANA_BURST_DRAIN_TICK)) {
         data.putInt(ARTORIA_CARD_MANA_BURST_DRAIN_TICK, player.tickCount + 20);
         if (!ServantCardManaService.consume(player, vars, ARTORIA_MANA_BURST_DRAIN_PER_SECOND)) {
            data.remove(ArtoriaPendragonCombatHelper.TAG_MANA_BURST_UNTIL);
            data.remove(ARTORIA_CARD_MANA_BURST_DRAIN_TICK);
            ManaBurstService.clearExternalJetMovement(player);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
            return;
         }
      }
      if (player.tickCount % 6 == 0 && player.level() instanceof ServerLevel level) {
         Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
         Vec3 front = player.position().add(look.scale(0.8)).add(0.0, player.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, front.x, front.y + 0.1, front.z, 5, 0.16, 0.22, 0.16, 0.018);
         level.sendParticles(ParticleTypes.ENCHANT, front.x, front.y, front.z, 3, 0.18, 0.2, 0.18, 0.025);
      }
   }

   public static void performInvisibleAirHammer(ServerPlayer player, int revealTicks) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      data.putBoolean(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE, true);
      data.putBoolean(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_RELEASED, true);
      data.putLong(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE + "Until", now + Math.max(80, revealTicks));
      data.putLong(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL, now + Math.max(80, revealTicks));
      PlayerNoblePhantasmHelper.revealArtoriaWindVeiledExcalibur(player, Math.max(80, revealTicks));
      VFXServerEffects.spawn(level, "artoria_strike_air", player, 128.0);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.55, 0.0);
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      boolean burst = ArtoriaPendragonCombatHelper.isManaBurstActive(player);
      float damage = burst ? 125.0F : 90.0F;
      double maxRange = burst ? 12.5 : 10.5;
      double widthScale = burst ? 0.58 : 0.48;
      java.util.HashSet<Integer> hit = new java.util.HashSet<>();
      for (double dist = 1.2; dist <= maxRange; dist += 0.75) {
         double halfWidth = dist * widthScale;
         double halfHeight = 0.7 + dist * 0.28;
         for (double side = -halfWidth; side <= halfWidth; side += 0.8) {
            Vec3 pos = origin.add(look.scale(dist)).add(right.scale(side)).add(0.0, halfHeight * 0.18, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.012);
            if (((int)(dist * 10 + side * 7)) % 3 == 0) {
               level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 1, 0.04, 0.04, 0.04, 0.01);
            }
            AABB box = new AABB(pos, pos).inflate(0.9, 0.85 + dist * 0.04, 0.9);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
               if (hit.add(target.getId())) {
                  target.invulnerableTime = 0;
                  target.hurt(player.damageSources().playerAttack(player), damage);
                  target.invulnerableTime = 0;
                  target.push(look.x * 1.05, 0.34, look.z * 1.05);
                  target.hurtMarked = true;
               }
            }
         }
      }
      level.sendParticles(ParticleTypes.FLASH, origin.x + look.x * 2.4, origin.y, origin.z + look.z * 2.4, 2, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.35F, 1.2F);
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.75F);
   }

   public static void performInvisibleAirRelease(ServerPlayer player, int revealTicks) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      data.putBoolean(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE, true);
      data.putBoolean(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_RELEASED, true);
      data.putLong(ArtoriaPendragonCombatHelper.TAG_INVISIBLE_AIR_ACTIVE + "Until", now + Math.max(60, revealTicks));
      data.putLong(ArtoriaPendragonCombatHelper.TAG_WIND_REGATHER_UNTIL, now + Math.max(60, revealTicks));
      PlayerNoblePhantasmHelper.revealArtoriaWindVeiledExcalibur(player, Math.max(60, revealTicks));
      Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.48, 0.0);
      double radius = 6.0;
      float damage = ArtoriaPendragonCombatHelper.isManaBurstActive(player) ? 32.0F : 22.0F;
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.2, radius), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         Vec3 away = target.position().subtract(player.position());
         Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
         double distance = horizontal.length();
         if (distance > radius || distance < 0.05) {
            continue;
         }
         Vec3 dir = horizontal.normalize();
         float scaledDamage = (float)(damage * Mth.clamp(1.0 - distance / (radius * 1.35), 0.35, 1.0));
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().playerAttack(player), scaledDamage);
         target.invulnerableTime = 0;
         target.push(dir.x * 1.1, 0.28, dir.z * 1.1);
         target.hurtMarked = true;
      }
      for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius + 1.5), p -> p.isAlive() && p.getOwner() != player)) {
         Vec3 away = projectile.position().subtract(center);
         if (away.lengthSqr() > (radius + 1.5) * (radius + 1.5)) {
            continue;
         }
         Vec3 dir = away.lengthSqr() < 0.01 ? player.getLookAngle().scale(-1.0) : away.normalize();
         projectile.setDeltaMovement(projectile.getDeltaMovement().add(dir.scale(1.9)).add(0.0, 0.18, 0.0));
         projectile.hurtMarked = true;
      }
      for (double r = 1.2; r <= radius; r += 1.0) {
         int points = Math.max(12, Mth.floor(r * 8.0));
         for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0) * i / points;
            double x = player.getX() + Math.cos(angle) * r;
            double z = player.getZ() + Math.sin(angle) * r;
            double y = player.getY() + 0.35 + (i % 3) * 0.18;
            if (i % 2 == 0) {
               level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            } else {
               level.sendParticles(ParticleTypes.END_ROD, x, y + 0.08, z, 1, 0.02, 0.02, 0.02, 0.006);
            }
         }
      }
      level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + player.getBbHeight() * 0.56, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.05F, 1.45F);
      level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.65F);
   }

   public static void performSmallCombo(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 16.0, 1.5);
      Vec3 look = PlayerNoblePhantasmHelper.horizontalLook(player);
      if (target != null && player.distanceTo(target) > 5.0F) {
         Vec3 toTarget = target.position().subtract(player.position());
         Vec3 dash = new Vec3(toTarget.x, 0.0, toTarget.z).normalize();
         player.setDeltaMovement(player.getDeltaMovement().add(dash.x * 1.65, 0.24, dash.z * 1.65));
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.55, player.getZ(), 18, 0.24, 0.18, 0.24, 0.04);
         TYPE_MOON_WORLD.queueServerWork(5, () -> {
            if (player.isAlive() && target.isAlive() && player.distanceToSqr(target) <= 49.0 && player.level() instanceof ServerLevel delayedLevel) {
               target.invulnerableTime = 0;
               target.hurt(player.damageSources().playerAttack(player), 32.0F);
               target.invulnerableTime = 0;
               target.push(dash.x * 0.55, 0.18, dash.z * 0.55);
               target.hurtMarked = true;
               delayedLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
            }
         });
      } else {
         ServantCardSkillUtils.hitForwardArc(player, look, 4.8, 28.0F);
         TYPE_MOON_WORLD.queueServerWork(4, () -> {
            if (player.isAlive()) {
               ServantCardSkillUtils.hitForwardArc(player, PlayerNoblePhantasmHelper.horizontalLook(player), 5.5, 24.0F);
            }
         });
      }
      for (double t = 0.8; t <= 4.8; t += 0.55) {
         Vec3 pos = player.position().add(look.scale(t)).add(0.0, 0.7 + t * 0.12, 0.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 2, 0.06, 0.06, 0.06, 0.01);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.95F, 1.3F);
   }

   public static void performInstinct(ServerPlayer player) {
      player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      player.removeEffect(MobEffects.DIG_SLOWDOWN);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, true, true));
      player.getPersistentData().putLong("ServantCardArtoriaInstinctUntil", player.level().getGameTime() + 40L);
   }

   public static void performRiding(ServerPlayer player) {
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      player.setDeltaMovement(player.getDeltaMovement().add(dir.x * 2.2, 0.18, dir.z * 2.2));
      player.hurtMarked = true;
      ServantCardSkillUtils.hitForwardArc(player, dir, 6.0, 26.0F);
   }

   public static void performManaBurstBeam(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) return;
      Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.8));
      Vec3 direction = player.getLookAngle().normalize();
      if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
      Vec3 maximumEnd = start.add(direction.scale(32.0));
      HitResult blockHit = level.clip(new ClipContext(start, maximumEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
      Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maximumEnd : blockHit.getLocation();
      double length = start.distanceTo(end);
      AABB corridor = new AABB(start, end).inflate(2.0);
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, corridor,
         entity -> entity != player && entity.isAlive() && !player.isAlliedTo(entity)
            && !entity.isAlliedTo(player) && !EntityUtils.isImmunePlayerTarget(entity))) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         double projected = Mth.clamp(center.subtract(start).dot(direction), 0.0, length);
         Vec3 nearest = start.add(direction.scale(projected));
         double hitRadius = 1.6 + target.getBbWidth() * 0.5;
         if (center.distanceToSqr(nearest) > hitRadius * hitRadius) continue;
         target.invulnerableTime = 0;
         target.hurt(player.damageSources().magic(), 200.0F);
         target.invulnerableTime = 0;
         target.push(direction.x * 0.7, Math.max(0.08, direction.y * 0.35), direction.z * 0.7);
         target.hurtMarked = true;
      }
      for (double distance = 0.0; distance <= length; distance += 0.65) {
         Vec3 point = start.add(direction.scale(distance));
         level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 2, 0.12, 0.12, 0.12, 0.01);
         if (((int)(distance * 10.0)) % 13 == 0) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 3, 0.18, 0.18, 0.18, 0.025);
         }
      }
      level.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 28, 0.65, 0.65, 0.65, 0.06);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.25F, 1.55F);
      level.playSound(null, net.minecraft.core.BlockPos.containing(end), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.65F);
   }

   private static void spawnManaBurstActivationFx(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + player.getBbHeight() * 0.58, player.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + player.getBbHeight() * 0.62, player.getZ(), 42, 0.42, 0.55, 0.42, 0.055);
         level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.52, player.getZ(), 28, 0.38, 0.44, 0.38, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.25F, 1.28F);
         level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.35F);
      }
   }


}
