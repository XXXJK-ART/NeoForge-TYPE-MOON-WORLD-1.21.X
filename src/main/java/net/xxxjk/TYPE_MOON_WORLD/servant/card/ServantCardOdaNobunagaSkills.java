package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.UBWInstanceManager;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

import static net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSkillUtils.*;

public final class ServantCardOdaNobunagaSkills {
   private static final String PRIMARY_ATTACK_COOLDOWN = "ServantCardOdaPrimaryAttackCooldown";
   private static final String SECONDARY_ATTACK_COOLDOWN = "ServantCardOdaSecondaryAttackCooldown";
   private static final String CHARGED_ACTIVE = "ServantCardOdaChargedMatchlockActive";
   private static final String CHARGED_START = "ServantCardOdaChargedMatchlockStart";
   private static final String THREE_THOUSAND_ACTIVE = "ServantCardOdaThreeThousandActive";
   private static final String THREE_THOUSAND_START = "ServantCardOdaThreeThousandStart";
   private static final String THREE_THOUSAND_FOLLOW = "ServantCardOdaThreeThousandFollow";
   private static final String THREE_THOUSAND_RELEASED = "ServantCardOdaThreeThousandReleased";
   private static final String HAJUN_ACTIVE = "ServantCardOdaHajunCardActive";
   private static final String HAJUN_START = "ServantCardOdaHajunCardStart";
   private static final String HAJUN_PROGRESS = "ServantCardOdaHajunCardProgress";
   private static final String HAJUN_TIMER = "ServantCardOdaHajunCardTimer";
   private static final String HAJUN_FIELD_ACTIVE_UNTIL = "ServantCardOdaHajunFieldActiveUntil";
   private static final String HAJUN_FIELD_RETURN_DIM = "ServantCardOdaHajunFieldReturnDim";
   private static final String HAJUN_FIELD_RETURN_X = "ServantCardOdaHajunFieldReturnX";
   private static final String HAJUN_FIELD_RETURN_Y = "ServantCardOdaHajunFieldReturnY";
   private static final String HAJUN_FIELD_RETURN_Z = "ServantCardOdaHajunFieldReturnZ";
   private static final String HAJUN_FIELD_OWNER = "ServantCardOdaHajunFieldOwner";
   private static final String HAJUN_FIELD_TARGET_RETURN_DIM = "ServantCardOdaHajunTargetReturnDim";
   private static final String HAJUN_FIELD_TARGET_RETURN_X = "ServantCardOdaHajunTargetReturnX";
   private static final String HAJUN_FIELD_TARGET_RETURN_Y = "ServantCardOdaHajunTargetReturnY";
   private static final String HAJUN_FIELD_TARGET_RETURN_Z = "ServantCardOdaHajunTargetReturnZ";
   private static final double FLIGHT_MP_PER_TICK = 0.28;
   private static final int FLIGHT_MAX_TICKS = 30 * 20;
   private static final int FLIGHT_EXHAUSTED_COOLDOWN = 20 * 20;
   private static final int FLIGHT_RECHARGE_INTERVAL = 30;
   private static final int HAJUN_CHANT_LINE_TICKS = 22;
   private static final int HAJUN_CHANT_LINES = 5;
   private static final int HAJUN_CARD_DURATION = 15 * 20;
   private static final double HAJUN_CARD_RADIUS = 25.0;
   private static final int HAJUN_TERRAIN_RADIUS = 52;
   private static final int HAJUN_CHANT_SURFACE_SPREAD_DELAY = 18;
   private static final int HAJUN_CHANT_SURFACE_RADIUS = 18;
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_CARD_HAJUN_BLOCKS = new HashMap<>();
   private static final Map<UUID, Map<BlockPos, BlockBackup>> ODA_CARD_HAJUN_CHANT_BLOCKS = new HashMap<>();

   private ServantCardOdaNobunagaSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"oda_nobunaga".equals(vars.servant_card_id)) {
         return;
      }
      tickOdaHajunChant(player, vars);
      tickOdaHajunField(player);
   }

   public static void clear(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(PRIMARY_ATTACK_COOLDOWN);
      data.remove(SECONDARY_ATTACK_COOLDOWN);
      data.remove(CHARGED_ACTIVE);
      data.remove(CHARGED_START);
      data.remove(THREE_THOUSAND_ACTIVE);
      data.remove(THREE_THOUSAND_START);
      data.remove(THREE_THOUSAND_FOLLOW);
      data.remove(THREE_THOUSAND_RELEASED);
      if (player.level() instanceof ServerLevel level) {
         if (data.getLong(HAJUN_FIELD_ACTIVE_UNTIL) > 0L) {
            returnFromOdaHajunField(player, level);
            return;
         }
         restoreOdaHajunTerrain(player, level, Integer.MAX_VALUE);
         restoreOdaHajunChantTerrain(player, level, Integer.MAX_VALUE);
      }
      data.remove(HAJUN_ACTIVE);
      data.remove(HAJUN_START);
      data.remove(HAJUN_PROGRESS);
      data.remove(HAJUN_TIMER);
      data.remove(HAJUN_FIELD_ACTIVE_UNTIL);
      data.remove(HAJUN_FIELD_RETURN_DIM);
      data.remove(HAJUN_FIELD_RETURN_X);
      data.remove(HAJUN_FIELD_RETURN_Y);
      data.remove(HAJUN_FIELD_RETURN_Z);
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(player, "oda_nobunaga", ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get(), null);
   }

   public static boolean isHoldingHeshikiriClient(Player player) {
      return player != null && player.getMainHandItem().is(ModItems.HESHIKIRI_HASEBE.get());
   }

   public static void handleBasicAttackPacket(ServerPlayer player, boolean secondary) {
      if (secondary) {
         handleHeshikiriRightClick(player);
      } else if (!player.isCrouching()) {
         fireHeshikiriPrimary(player);
      }
   }

   public static void fireHeshikiriPrimary(ServerPlayer player) {
      if (!canUseHeshikiriGun(player) || !(player.level() instanceof ServerLevel level) || isOnCooldown(player, PRIMARY_ATTACK_COOLDOWN)) {
         return;
      }
      setCooldown(player, PRIMARY_ATTACK_COOLDOWN, 10);
      shootMatchlockBullet(level, player, player.getEyePosition().add(player.getLookAngle().scale(0.85)), player.getLookAngle(), 13.0F, 3.15F, 0);
      muzzleFx(level, player, player.getLookAngle(), 1);
   }

   public static boolean handleHeshikiriRightClick(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!canUseHeshikiriGun(player) || !(player.level() instanceof ServerLevel level) || isOnCooldown(player, SECONDARY_ATTACK_COOLDOWN)) {
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, 3.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      setCooldown(player, SECONDARY_ATTACK_COOLDOWN, 16);
      Vec3 look = player.getLookAngle();
      shootMatchlockBullet(level, player, player.getEyePosition().add(look.scale(0.9)), look, 18.0F, 3.3F, 3);
      muzzleFx(level, player, look, 2);
      return true;
   }

   public static void performOdaHasebeShortThrust(ServerPlayer player) {
      dashSlash(player, 4.6, 22.0F, 0.55, true);
   }

   public static void performOdaHasebeBreakthrough(ServerPlayer player) {
      dashSlash(player, 6.5, 26.0F, 0.85, false);
      if (player.level() instanceof ServerLevel level) {
         Vec3 look = player.getLookAngle();
         for (int i = 0; i < 3; i++) {
            Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale((i - 1) * 0.18);
            shootMatchlockBullet(level, player, player.getEyePosition().add(look.scale(0.8)).add(side), look.add(side.scale(0.04)).normalize(), 14.0F, 3.0F, 3);
         }
         muzzleFx(level, player, look, 3);
      }
   }

   public static void performOdaFloatingRefill(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      int existing = level.getEntitiesOfClass(OdaMatchlockGunEntity.class, player.getBoundingBox().inflate(48.0), gun -> gun.isFloatingFor(player.getUUID())).size();
      int target = 5;
      for (int i = existing; i < target; i++) {
         level.addFreshEntity(OdaMatchlockGunEntity.floating(level, player, i, target, 20 * 30, i * 5));
      }
      level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.65F, 1.45F);
   }

   public static void performOdaStrategy(ServerPlayer player) {
      buffNearby(player, 12.0, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 260, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 260, 0, false, true, true));
      if (player.level() instanceof ServerLevel level) {
         for (ServerPlayer other : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(12.0), p -> p != player && player.isAlliedTo(p))) {
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, false, true, true));
         }
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0, player.getZ(), 24, 1.0, 0.5, 1.0, 0.03);
         level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.65F, 1.25F);
      }
   }

   public static void performOdaMaou(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 220, 0, false, true, true));
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_oda_maou", player, 160.0);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0, 3.0, 8.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(120);
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, hasTrait(living, ServantTraitTag.DIVINE) ? 2 : 0, false, true, true));
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 24.0F : 10.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 48, 2.1, 0.7, 2.1, 0.08);
      level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.45, player.getZ(), 28, 1.8, 0.3, 1.8, 0.04);
      level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.55F, 1.45F);
   }

   public static void performOdaMatchlock(ServerPlayer player, int count, float damage) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 baseLook = player.getLookAngle().normalize();
      Vec3 right = new Vec3(-baseLook.z, 0.0, baseLook.x).normalize();
      for (int i = 0; i < count; i++) {
         double offset = count <= 1 ? 0.0 : (i - (count - 1) * 0.5) * 0.38;
         OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, player, null, damage);
         Vec3 spawn = player.getEyePosition().add(right.scale(offset)).add(baseLook.scale(0.8));
         Vec3 dir = baseLook.add(right.scale((player.getRandom().nextDouble() - 0.5) * 0.04 * Math.max(1, count))).normalize();
         bullet.setPos(spawn.x, spawn.y, spawn.z);
         bullet.setDeltaMovement(dir.scale(3.1));
         level.addFreshEntity(bullet);
      }
      level.sendParticles(ParticleTypes.SMOKE, player.getX() + baseLook.x, player.getY() + 1.3, player.getZ() + baseLook.z, 12, 0.25, 0.12, 0.25, 0.04);
      level.sendParticles(ParticleTypes.FLAME, player.getX() + baseLook.x, player.getY() + 1.3, player.getZ() + baseLook.z, 6, 0.12, 0.08, 0.12, 0.03);
      level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.65F);
   }

   public static void performOdaFireBarrage(ServerPlayer player) {
      for (int wave = 0; wave < 3; wave++) {
         int delay = wave * 5;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive()) {
               performOdaMatchlock(player, 7, 9.0F);
            }
         });
      }
   }

   public static void performOdaThreeLine(ServerPlayer player) {
      for (int wave = 0; wave < 3; wave++) {
         int delay = wave * 7;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive() && player.level() instanceof ServerLevel level) {
               LivingEntity target = findLookTarget(player, 28.0, 1.8);
               Vec3 look = target == null
                  ? player.getLookAngle()
                  : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(player.getEyePosition()).normalize();
               for (int i = 0; i < 5; i++) {
                  Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale((i - 2) * 0.32);
                  shootMatchlockBullet(level, player, player.getEyePosition().add(look.scale(0.9)).add(side), look.add(side.scale(0.03)).normalize(), 12.0F, 3.2F, 3);
               }
               muzzleFx(level, player, look, 3);
            }
         });
      }
   }

   public static boolean performOdaEncirclingMatchlocks(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 30.0, 1.8);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
         return false;
      }
      for (int i = 0; i < 3; i++) {
         double angle = i * Math.PI * 2.0 / 3.0;
         Vec3 pos = target.position().add(Math.cos(angle) * 2.6, target.getBbHeight() * 0.65 + 0.4, Math.sin(angle) * 2.6);
         level.addFreshEntity(OdaMatchlockGunEntity.oneShotTracking(level, player, target, pos, i * 8));
      }
      level.playSound(null, target.blockPosition(), SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), SoundSource.PLAYERS, 0.8F, 1.35F);
      return true;
   }

   public static void startOdaChargedMatchlock(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.putBoolean(CHARGED_ACTIVE, true);
      data.putLong(CHARGED_START, level.getGameTime());
      level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
      level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.4, 0.28, 0.4, 0.04);
   }

   public static void releaseOdaChargedMatchlock(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(CHARGED_ACTIVE) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      int held = (int)Math.max(0L, level.getGameTime() - data.getLong(CHARGED_START));
      data.remove(CHARGED_ACTIVE);
      data.remove(CHARGED_START);
      double charge = Mth.clamp((held - 10) / 40.0, 0.0, 1.0);
      float damage = (float)Mth.lerp(charge, 35.0, 95.0);
      Vec3 look = player.getLookAngle();
      shootMatchlockBullet(level, player, player.getEyePosition().add(look.scale(1.0)), look, damage, (float)Mth.lerp(charge, 3.2, 4.2), 2);
      level.sendParticles(ParticleTypes.FLASH, player.getX() + look.x, player.getY() + 1.2 + look.y, player.getZ() + look.z, 2, 0.0, 0.0, 0.0, 0.0);
      muzzleFx(level, player, look, 5);
   }

   public static boolean performOdaCrossfireNet(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      LivingEntity target = findLookTarget(player, 30.0, 1.8);
      Vec3 center = target == null ? player.getEyePosition().add(player.getLookAngle().scale(14.0)) : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      for (int i = 0; i < 6; i++) {
         double angle = i * Math.PI / 3.0;
         Vec3 pos = center.add(Math.cos(angle) * 4.0, 1.2, Math.sin(angle) * 4.0);
         OdaMatchlockGunEntity gun = OdaMatchlockGunEntity.oneShotTracking(level, player, target, pos, i * 5);
         level.addFreshEntity(gun);
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(5.0, 2.5, 5.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, true, true));
         living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true, true));
      }
      level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 36, 3.0, 0.5, 3.0, 0.04);
      return true;
   }

   public static void performOdaScorchedEarth(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      LivingEntity target = findLookTarget(player, 28.0, 1.8);
      Vec3 center = target == null ? player.position().add(PlayerNoblePhantasmHelper.horizontalLook(player).scale(8.0)) : target.position();
      for (int wave = 0; wave < 5; wave++) {
         int delay = wave * 20;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> scorchEarthPulse(player, center));
      }
   }

   private static void scorchEarthPulse(ServerPlayer player, Vec3 center) {
      if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(7.0, 2.5, 7.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(80);
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 16.0F : 9.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.15, center.z, 56, 4.0, 0.12, 4.0, 0.05);
      level.sendParticles(ParticleTypes.ASH, center.x, center.y + 0.55, center.z, 64, 4.5, 0.25, 4.5, 0.035);
      level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.55F, 0.85F);
   }

   public static void performOdaAtsumori(ServerPlayer player) {
      if (player.level() instanceof ServerLevel level) {
         Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
         Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(player.isCrouching() ? -5.0 : 5.0);
         level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.35, player.getZ(), 20, 0.35, 0.12, 0.35, 0.05);
         player.teleportTo(player.getX() + side.x, player.getY() + 0.1, player.getZ() + side.z);
         player.hurtMarked = true;
         level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.6, player.getZ(), 20, 0.45, 0.18, 0.45, 0.04);
      }
   }

   public static void performOdaAntiMystery(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 26.0, 1.8);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      float damage = hasTrait(target, ServantTraitTag.DIVINE) ? 42.0F : 24.0F;
      OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, target);
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), damage);
      target.invulnerableTime = 0;
      spawnLineParticles(level, player.getEyePosition(), target.position().add(0.0, target.getBbHeight() * 0.55, 0.0), ParticleTypes.SOUL_FIRE_FLAME);
      level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
   }

   public static void performOdaAshField(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      double radius = 7.0;
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2.5, radius), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(100);
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 15.0F : 8.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.12, player.getZ(), 48, 3.6, 0.12, 3.6, 0.035);
      level.sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 0.4, player.getZ(), 52, 4.0, 0.2, 4.0, 0.025);
   }

   public static void performOdaHasebeRepel(ServerPlayer player) {
      LivingEntity target = findLookTarget(player, 7.0, 1.8);
      if (target == null || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = target.position().subtract(player.position()).normalize();
      target.invulnerableTime = 0;
      target.hurt(player.damageSources().playerAttack(player), hasTrait(target, ServantTraitTag.DIVINE) ? 26.0F : 16.0F);
      target.invulnerableTime = 0;
      target.push(dir.x * 1.25, 0.22, dir.z * 1.25);
      target.hurtMarked = true;
      OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, target);
      level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 3, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 12, 0.35, 0.18, 0.35, 0.05);
   }

   public static void performOdaThreeThousand(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_oda_three_thousand", player, 160.0);
      for (int wave = 0; wave < 6; wave++) {
         int delay = wave * 4;
         TYPE_MOON_WORLD.queueServerWork(delay, () -> {
            if (player.isAlive()) {
               performOdaMatchlock(player, 12, 12.0F);
            }
         });
      }
   }

   public static void startOdaThreeThousand(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(THREE_THOUSAND_ACTIVE)) {
         return;
      }
      data.putBoolean(THREE_THOUSAND_ACTIVE, true);
      data.putBoolean(THREE_THOUSAND_FOLLOW, false);
      data.putBoolean(THREE_THOUSAND_RELEASED, false);
      data.putLong(THREE_THOUSAND_START, level.getGameTime());
      level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODA_NOBUNAGA_VOICE_NP.get(), SoundSource.VOICE, 1.0F, 1.0F);
      VFXServerEffects.spawn(level, "servant_oda_three_thousand", player, 160.0);
      TYPE_MOON_WORLD.queueServerWork(70, () -> releaseOdaThreeThousand(player));
   }

   public static void markOdaThreeThousandRelease(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(THREE_THOUSAND_ACTIVE)) {
         return;
      }
      int held = (int)Math.max(0L, level.getGameTime() - data.getLong(THREE_THOUSAND_START));
      if (!data.getBoolean(THREE_THOUSAND_RELEASED)) {
         data.putBoolean(THREE_THOUSAND_FOLLOW, held >= 12);
         data.putBoolean(THREE_THOUSAND_RELEASED, true);
      }
   }

   private static void releaseOdaThreeThousand(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!player.isAlive() || !data.getBoolean(THREE_THOUSAND_ACTIVE) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      boolean follow = data.getBoolean(THREE_THOUSAND_FOLLOW);
      data.remove(THREE_THOUSAND_ACTIVE);
      data.remove(THREE_THOUSAND_START);
      data.remove(THREE_THOUSAND_FOLLOW);
      data.remove(THREE_THOUSAND_RELEASED);
      LivingEntity target = findLookTarget(player, 34.0, 2.0);
      Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.72, 0.0);
      for (int i = 0; i < 36; i++) {
         int index = i;
         TYPE_MOON_WORLD.queueServerWork((i / 6) * 6, () -> {
            if (!player.isAlive() || !(player.level() instanceof ServerLevel delayedLevel)) {
               return;
            }
            if (follow) {
               delayedLevel.addFreshEntity(OdaMatchlockGunEntity.threeThousandWorldsFollow(delayedLevel, player, target, index, 36, (index % 6) * 4));
            } else {
               double angle = index * Math.PI * 2.0 / 36.0;
               double ring = 3.0 + (index % 3) * 1.1;
               Vec3 pos = origin.add(Math.cos(angle) * ring, 0.2 + (index % 4) * 0.35, Math.sin(angle) * ring);
               Vec3 facing = target == null ? player.getLookAngle() : target.position().add(0.0, target.getBbHeight() * 0.55, 0.0).subtract(pos);
               delayedLevel.addFreshEntity(OdaMatchlockGunEntity.threeThousandWorlds(delayedLevel, player, target, pos, facing, (index % 6) * 4));
            }
         });
      }
      level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.1F, 0.75F);
   }

   public static void performOdaHajun(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get(), SoundSource.VOICE, 1.0F, 1.0F);
      VFXServerEffects.spawn(level, "servant_oda_hajun", player, 192.0);
      RedSkeletonHajunEntity skeleton = new RedSkeletonHajunEntity(level, player, 260);
      skeleton.setPos(player.getX(), player.getY(), player.getZ());
      level.addFreshEntity(skeleton);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(14.0, 5.0, 14.0), e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.addEffect(new MobEffectInstance(MobEffects.WITHER, 140, 1, false, true, true));
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? 60.0F : 34.0F);
         living.invulnerableTime = 0;
      }
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 72, 4.0, 0.18, 4.0, 0.04);
   }

   public static boolean performOdaHajunAction(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, ServantCardSkillAction action) {
      CompoundTag data = player.getPersistentData();
      if (data.getLong(HAJUN_FIELD_ACTIVE_UNTIL) > 0L) {
         if (returnFromOdaHajunField(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.ubw_released"), true);
            return true;
         }
         return false;
      }
      if (data.getBoolean(HAJUN_ACTIVE)) {
         int progress = data.getInt(HAJUN_PROGRESS);
         if (progress < 3) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.ubw_need_third_line"), true);
            return false;
         }
         if (!ServantCardManaService.consume(player, vars, 95.0)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
            return false;
         }
         if (!(player.level() instanceof ServerLevel level)) {
            return false;
         }
         finishOdaHajunChant(player, true);
         activateOdaHajunField(player, level);
         vars.servant_card_np_cooldown = action.cooldownTicks();
         vars.syncPlayerVariables(player);
         return true;
      }
      if (vars.servant_card_np_cooldown > 0) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.cooldown", String.format(java.util.Locale.ROOT, "%.1f", vars.servant_card_np_cooldown / 20.0F)), true);
         return false;
      }
      if (!ServantCardManaService.consume(player, vars, 35.0)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      startOdaHajun(player);
      vars.syncPlayerVariables(player);
      return true;
   }

   public static void startOdaHajun(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      CompoundTag data = player.getPersistentData();
      if (data.getBoolean(HAJUN_ACTIVE)) {
         return;
      }
      data.putBoolean(HAJUN_ACTIVE, true);
      data.putLong(HAJUN_START, level.getGameTime());
      data.putInt(HAJUN_PROGRESS, 1);
      data.putInt(HAJUN_TIMER, 0);
      PlayerNoblePhantasmHelper.startServantCardVoiceSession(player, "oda_nobunaga", ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get());
      VFXServerEffects.spawn(level, "servant_oda_hajun", player, 192.0);
      level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 0.72F);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 48, 3.2, 0.18, 3.2, 0.025);
   }

   public static void releaseOdaHajun(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ServantCardSkillAction action = ServantCardSkillLayout.actionFor("oda_nobunaga", 9, false);
      if (action != null) {
         performOdaHajunAction(player, vars, action);
      }
   }

   public static boolean toggleMountFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.servant_card_transformed || !"oda_nobunaga".equals(vars.servant_card_id) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      OdaMatchlockGunEntity mount = findMount(level, player);
      if (vars.servant_card_flying || mount != null || player.getVehicle() instanceof OdaMatchlockGunEntity) {
         stopMountFlight(player, vars, true);
         return true;
      }
      long now = level.getGameTime();
      if (now < vars.servant_card_oda_flight_cooldown_until) {
         player.displayClientMessage(Component.translatable(
            "message.typemoonworld.servant_card.oda_flight_cooldown",
            String.format(java.util.Locale.ROOT, "%.1f", (vars.servant_card_oda_flight_cooldown_until - now) / 20.0)
         ), true);
         return false;
      }
      if (vars.servant_card_oda_flight_ticks <= 0) {
         return false;
      }
      mount = OdaMatchlockGunEntity.flightMount(level, player);
      mount.setPos(player.getX(), player.getY() + 0.08, player.getZ());
      level.addFreshEntity(mount);
      player.startRiding(mount, true);
      vars.servant_card_flying = true;
      vars.servant_card_flight_mode = 1;
      vars.servant_card_flight_toggle_cooldown = 8;
      vars.syncPlayerVariables(player);
      level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.9F, 0.7F);
      return true;
   }

   public static boolean tickMountFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"oda_nobunaga".equals(vars.servant_card_id)) {
         return false;
      }
      if (!vars.servant_card_flying) {
         return true;
      }
      if (!(player.level() instanceof ServerLevel level) || !(player.getVehicle() instanceof OdaMatchlockGunEntity mount) || !mount.isMountFor(player.getUUID())) {
         stopMountFlight(player, vars, true);
         return true;
      }
      if (!ServantCardManaService.consumeSilently(player, vars, FLIGHT_MP_PER_TICK)) {
         stopMountFlight(player, vars, true);
         return true;
      }
      vars.servant_card_oda_flight_ticks = Math.max(0, vars.servant_card_oda_flight_ticks - 1);
      if (vars.servant_card_oda_flight_ticks <= 0) {
         vars.servant_card_oda_flight_cooldown_until = level.getGameTime() + FLIGHT_EXHAUSTED_COOLDOWN;
         vars.servant_card_oda_flight_recharge_at = vars.servant_card_oda_flight_cooldown_until + FLIGHT_RECHARGE_INTERVAL;
         stopMountFlight(player, vars, true);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.oda_flight_exhausted"), true);
         return true;
      }
      if (player.tickCount % 10 == 0) {
         vars.syncMana(player);
      }
      mount.setMountInput(vars.servant_card_flight_forward, vars.servant_card_flight_strafe, vars.servant_card_flight_vertical);
      player.fallDistance = 0.0F;
      return true;
   }

   public static void stopMountFlight(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, boolean sync) {
      if (player.getVehicle() instanceof OdaMatchlockGunEntity mount && mount.isMountFor(player.getUUID())) {
         player.stopRiding();
         if (mount.level() instanceof ServerLevel level) {
            mount.discard();
            level.sendParticles(ParticleTypes.ENCHANT, mount.getX(), mount.getY(), mount.getZ(), 12, 0.25, 0.18, 0.25, 0.06);
         }
      } else if (player.level() instanceof ServerLevel level) {
         OdaMatchlockGunEntity mount = findMount(level, player);
         if (mount != null) {
            mount.discard();
         }
      }
      vars.servant_card_flying = false;
      vars.servant_card_flight_mode = 0;
      vars.servant_card_flight_forward = 0.0;
      vars.servant_card_flight_strafe = 0.0;
      vars.servant_card_flight_vertical = 0.0;
      if (player.level() instanceof ServerLevel level && vars.servant_card_oda_flight_ticks < FLIGHT_MAX_TICKS && vars.servant_card_oda_flight_recharge_at <= 0L) {
         vars.servant_card_oda_flight_recharge_at = level.getGameTime() + FLIGHT_RECHARGE_INTERVAL;
      }
      if (sync) {
         vars.syncPlayerVariables(player);
      }
   }

   public static void tickMountFlightRecharge(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!(player.level() instanceof ServerLevel level) || vars.servant_card_flying) {
         return;
      }
      long now = level.getGameTime();
      if (now < vars.servant_card_oda_flight_cooldown_until || vars.servant_card_oda_flight_ticks >= FLIGHT_MAX_TICKS) {
         return;
      }
      if (vars.servant_card_oda_flight_recharge_at <= 0L) {
         vars.servant_card_oda_flight_recharge_at = now + FLIGHT_RECHARGE_INTERVAL;
         return;
      }
      if (now >= vars.servant_card_oda_flight_recharge_at) {
         vars.servant_card_oda_flight_ticks = Math.min(FLIGHT_MAX_TICKS, vars.servant_card_oda_flight_ticks + 20);
         vars.servant_card_oda_flight_recharge_at = vars.servant_card_oda_flight_ticks >= FLIGHT_MAX_TICKS ? 0L : now + FLIGHT_RECHARGE_INTERVAL;
         vars.syncPlayerVariables(player);
      }
   }

   private static void activateOdaHajunField(ServerPlayer player, ServerLevel source) {
      if (ModDimensions.isHajunDimension(source.dimension().location())) {
         restoreOdaHajunChantTerrain(player, source, Integer.MAX_VALUE);
         startOdaHajunLocalField(player, source);
         return;
      }
      ServerLevel hajunLevel = source.getServer().getLevel(ModDimensions.HAJUN_KEY);
      if (hajunLevel == null) {
         restoreOdaHajunChantTerrain(player, source, Integer.MAX_VALUE);
         startOdaHajunLocalField(player, source);
         return;
      }
      CompoundTag data = player.getPersistentData();
      data.putString(HAJUN_FIELD_RETURN_DIM, source.dimension().location().toString());
      data.putDouble(HAJUN_FIELD_RETURN_X, player.getX());
      data.putDouble(HAJUN_FIELD_RETURN_Y, player.getY());
      data.putDouble(HAJUN_FIELD_RETURN_Z, player.getZ());
      stopMountFlight(player, player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES), true);
      List<LivingEntity> targets = collectOdaHajunTargets(player, source);
      Vec3 randomEntry = UBWInstanceManager.randomEntryPosition(player.getRandom());
      Vec3 entry = new Vec3(randomEntry.x, findHajunSafeSpawnY(hajunLevel, Mth.floor(randomEntry.x), Mth.floor(randomEntry.z)), randomEntry.z);
      source.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.55F);
      source.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 72, 4.0, 0.18, 4.0, 0.04);
      restoreOdaHajunChantTerrain(player, source, Integer.MAX_VALUE);
      hajunLevel.getChunk(Mth.floor(entry.x) >> 4, Mth.floor(entry.z) >> 4);
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 4, false, false, false));
      player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 6, false, false, false));
      TYPE_MOON_WORLD.queueServerWork(40, () -> {
         if (!player.isAlive() || player.level() != source) return;
         moveOdaHajunTargets(player, source, hajunLevel, targets, entry);
         Entity moved = player.changeDimension(new DimensionTransition(hajunLevel, entry, Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof ServerPlayer movedPlayer) {
            CompoundTag movedData = movedPlayer.getPersistentData();
            movedData.putString(HAJUN_FIELD_RETURN_DIM, data.getString(HAJUN_FIELD_RETURN_DIM));
            movedData.putDouble(HAJUN_FIELD_RETURN_X, data.getDouble(HAJUN_FIELD_RETURN_X));
            movedData.putDouble(HAJUN_FIELD_RETURN_Y, data.getDouble(HAJUN_FIELD_RETURN_Y));
            movedData.putDouble(HAJUN_FIELD_RETURN_Z, data.getDouble(HAJUN_FIELD_RETURN_Z));
            startOdaHajunLocalField(movedPlayer, hajunLevel);
         }
      });
   }

   private static void startOdaHajunLocalField(ServerPlayer player, ServerLevel level) {
      CompoundTag data = player.getPersistentData();
      long now = level.getGameTime();
      data.putLong(HAJUN_FIELD_ACTIVE_UNTIL, now + HAJUN_CARD_DURATION);
      RedSkeletonHajunEntity skeleton = new RedSkeletonHajunEntity(level, player, HAJUN_CARD_DURATION);
      skeleton.setPos(player.getX(), player.getY(), player.getZ());
      level.addFreshEntity(skeleton);
      VFXServerEffects.spawn(level, "servant_oda_hajun", player, 192.0);
      level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2F, 0.55F);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 90, 4.0, 0.18, 4.0, 0.04);
      spreadOdaHajunTerrain(player, level, 220);
      applyOdaHajunPulse(player, level, true);
   }

   private static void tickOdaHajunChant(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(HAJUN_ACTIVE) || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      int timer = data.getInt(HAJUN_TIMER) + 1;
      data.putInt(HAJUN_TIMER, timer);
      long now = level.getGameTime();
      if (now % 5L == 0L) {
         double radius = 2.5 + Math.min(8.0, timer * 0.07);
         level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.2, player.getZ(), 20, radius, 0.16, radius, 0.025);
         level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY() + 0.4, player.getZ(), 8, radius * 0.8, 0.18, radius * 0.8, 0.01);
         stainOdaHajunChantSurfaceFromCaster(player, level, now, 16);
      }
      int progress = Math.min(HAJUN_CHANT_LINES, 1 + timer / HAJUN_CHANT_LINE_TICKS);
      if (progress > data.getInt(HAJUN_PROGRESS)) {
         data.putInt(HAJUN_PROGRESS, progress);
         VFXServerEffects.spawn(level, "servant_oda_hajun", player, 160.0);
         level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.65F, 0.65F + progress * 0.06F);
      }
      if (progress >= HAJUN_CHANT_LINES && timer >= HAJUN_CHANT_LINE_TICKS * HAJUN_CHANT_LINES) {
         if (!ServantCardManaService.consume(player, vars, 95.0)) {
            finishOdaHajunChant(player, false);
            player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
            return;
         }
         finishOdaHajunChant(player, false);
         activateOdaHajunField(player, level);
         vars.servant_card_np_cooldown = 3600;
         vars.syncPlayerVariables(player);
      }
   }

   private static void finishOdaHajunChant(ServerPlayer player, boolean playShort) {
      CompoundTag data = player.getPersistentData();
      data.remove(HAJUN_ACTIVE);
      data.remove(HAJUN_START);
      data.remove(HAJUN_PROGRESS);
      data.remove(HAJUN_TIMER);
      if (playShort) {
         PlayerNoblePhantasmHelper.finishServantCardVoiceSession(player, "oda_nobunaga", ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get(), ModSounds.ODA_NOBUNAGA_VOICE_HAJUN_SHORT.get());
      } else {
         PlayerNoblePhantasmHelper.finishServantCardVoiceSession(player, "oda_nobunaga", ModSounds.ODA_NOBUNAGA_VOICE_HAJUN.get(), null);
      }
   }

   private static void tickOdaHajunField(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      long activeUntil = data.getLong(HAJUN_FIELD_ACTIVE_UNTIL);
      if (activeUntil <= 0L || !(player.level() instanceof ServerLevel level)) {
         return;
      }
      long now = level.getGameTime();
      if (!player.isAlive() || now >= activeUntil) {
         returnFromOdaHajunField(player, level);
         return;
      }
      if (now % 20L == 0L) {
         applyOdaHajunPulse(player, level, false);
      }
      if (now % 6L == 0L) {
         emitOdaHajunAmbientFx(player, level);
      }
      if (now % 5L == 0L) {
         spreadOdaHajunTerrain(player, level, 30);
      }
   }

   public static boolean returnFromOdaHajunField(ServerPlayer player) {
      if (!(player.level() instanceof ServerLevel level) || player.getPersistentData().getLong(HAJUN_FIELD_ACTIVE_UNTIL) <= 0L) {
         return false;
      }
      returnFromOdaHajunField(player, level);
      return true;
   }

   private static List<LivingEntity> collectOdaHajunTargets(ServerPlayer player, ServerLevel source) {
      List<LivingEntity> targets = new ArrayList<>();
      LivingEntity lookTarget = findLookTarget(player, HAJUN_CARD_RADIUS, 2.4);
      if (isOdaHajunTarget(player, source, lookTarget)) {
         targets.add(lookTarget);
      }
      for (LivingEntity living : source.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HAJUN_CARD_RADIUS), living -> living != lookTarget && isOdaHajunTarget(player, source, living))) {
         targets.add(living);
      }
      return targets;
   }

   private static boolean isOdaHajunTarget(ServerPlayer player, ServerLevel source, LivingEntity living) {
      return living != null
         && living.isAlive()
         && living != player
         && living.level() == source
         && !player.isAlliedTo(living)
         && !living.isAlliedTo(player)
         && !EntityUtils.isImmunePlayerTarget(living)
         && living.distanceToSqr(player) <= HAJUN_CARD_RADIUS * HAJUN_CARD_RADIUS;
   }

   private static void moveOdaHajunTargets(ServerPlayer player, ServerLevel source, ServerLevel hajunLevel, List<LivingEntity> targets, Vec3 entry) {
      for (LivingEntity living : targets) {
         if (!isOdaHajunTarget(player, source, living)) {
            continue;
         }
         CompoundTag data = living.getPersistentData();
         data.putUUID(HAJUN_FIELD_OWNER, player.getUUID());
         data.putString(HAJUN_FIELD_TARGET_RETURN_DIM, source.dimension().location().toString());
         data.putDouble(HAJUN_FIELD_TARGET_RETURN_X, living.getX());
         data.putDouble(HAJUN_FIELD_TARGET_RETURN_Y, living.getY());
         data.putDouble(HAJUN_FIELD_TARGET_RETURN_Z, living.getZ());
         double relX = Mth.clamp(living.getX() - player.getX(), -16.0, 16.0);
         double relZ = Mth.clamp(living.getZ() - player.getZ(), -16.0, 16.0);
         double targetX = entry.x + relX;
         double targetZ = entry.z + relZ;
         double targetY = findHajunSafeSpawnY(hajunLevel, Mth.floor(targetX), Mth.floor(targetZ));
         Entity moved = living.changeDimension(new DimensionTransition(hajunLevel, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof LivingEntity movedLiving) {
            CompoundTag movedData = movedLiving.getPersistentData();
            movedData.putUUID(HAJUN_FIELD_OWNER, player.getUUID());
            movedData.putString(HAJUN_FIELD_TARGET_RETURN_DIM, source.dimension().location().toString());
            movedData.putDouble(HAJUN_FIELD_TARGET_RETURN_X, data.getDouble(HAJUN_FIELD_TARGET_RETURN_X));
            movedData.putDouble(HAJUN_FIELD_TARGET_RETURN_Y, data.getDouble(HAJUN_FIELD_TARGET_RETURN_Y));
            movedData.putDouble(HAJUN_FIELD_TARGET_RETURN_Z, data.getDouble(HAJUN_FIELD_TARGET_RETURN_Z));
         }
      }
   }

   private static void applyOdaHajunPulse(ServerPlayer player, ServerLevel level, boolean initial) {
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HAJUN_CARD_RADIUS), e -> e.isAlive() && e != player && !player.isAlliedTo(e) && !EntityUtils.isImmunePlayerTarget(e))) {
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
         living.setRemainingFireTicks(Math.max(living.getRemainingFireTicks(), initial ? 160 : 80));
         living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 45, hasTrait(living, ServantTraitTag.DIVINE) ? 2 : 0, false, true, true));
         if (hasTrait(living, ServantTraitTag.DIVINE)) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 1, false, true, true));
         }
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().magic(), hasTrait(living, ServantTraitTag.DIVINE) ? initial ? 120.0F : 40.0F : initial ? 52.0F : 12.0F);
         living.invulnerableTime = 0;
      }
   }

   private static void returnFromOdaHajunField(ServerPlayer player, ServerLevel level) {
      CompoundTag data = player.getPersistentData();
      ServerLevel returnLevel = resolveDimension(level, data.getString(HAJUN_FIELD_RETURN_DIM));
      Vec3 returnPos = new Vec3(data.getDouble(HAJUN_FIELD_RETURN_X), data.getDouble(HAJUN_FIELD_RETURN_Y), data.getDouble(HAJUN_FIELD_RETURN_Z));
      returnOdaHajunTargets(player.getUUID(), level, returnLevel);
      restoreOdaHajunTerrain(player, level, Integer.MAX_VALUE);
      restoreOdaHajunChantTerrain(player, level, Integer.MAX_VALUE);
      data.remove(HAJUN_ACTIVE);
      data.remove(HAJUN_START);
      data.remove(HAJUN_PROGRESS);
      data.remove(HAJUN_TIMER);
      data.remove(HAJUN_FIELD_ACTIVE_UNTIL);
      data.remove(HAJUN_FIELD_RETURN_DIM);
      data.remove(HAJUN_FIELD_RETURN_X);
      data.remove(HAJUN_FIELD_RETURN_Y);
      data.remove(HAJUN_FIELD_RETURN_Z);
      for (RedSkeletonHajunEntity skeleton : level.getEntitiesOfClass(RedSkeletonHajunEntity.class, player.getBoundingBox().inflate(96.0))) {
         skeleton.discard();
      }
      if (player.isAlive() && ModDimensions.isHajunDimension(level.dimension().location())) {
         player.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
      }
   }

   private static void returnOdaHajunTargets(UUID ownerId, ServerLevel sourceLevel, ServerLevel fallbackLevel) {
      List<LivingEntity> toReturn = new ArrayList<>();
      for (Entity candidate : sourceLevel.getEntities().getAll()) {
         if (candidate instanceof LivingEntity living && living.getPersistentData().hasUUID(HAJUN_FIELD_OWNER) && ownerId.equals(living.getPersistentData().getUUID(HAJUN_FIELD_OWNER))) {
            toReturn.add(living);
         }
      }
      for (LivingEntity living : toReturn) {
         CompoundTag data = living.getPersistentData();
         ServerLevel returnLevel = resolveDimensionOrFallback(sourceLevel, data.getString(HAJUN_FIELD_TARGET_RETURN_DIM), fallbackLevel);
         Vec3 returnPos = new Vec3(data.getDouble(HAJUN_FIELD_TARGET_RETURN_X), data.getDouble(HAJUN_FIELD_TARGET_RETURN_Y), data.getDouble(HAJUN_FIELD_TARGET_RETURN_Z));
         clearOdaHajunTarget(living);
         Entity moved = living.changeDimension(new DimensionTransition(returnLevel, returnPos, Vec3.ZERO, living.getYRot(), living.getXRot(), DimensionTransition.DO_NOTHING));
         if (moved instanceof LivingEntity movedLiving) {
            clearOdaHajunTarget(movedLiving);
         }
      }
   }

   private static void clearOdaHajunTarget(LivingEntity living) {
      CompoundTag data = living.getPersistentData();
      data.remove(HAJUN_FIELD_OWNER);
      data.remove(HAJUN_FIELD_TARGET_RETURN_DIM);
      data.remove(HAJUN_FIELD_TARGET_RETURN_X);
      data.remove(HAJUN_FIELD_TARGET_RETURN_Y);
      data.remove(HAJUN_FIELD_TARGET_RETURN_Z);
   }

   private static ServerLevel resolveDimension(ServerLevel level, String id) {
      ServerLevel resolved = resolveDimensionOrFallback(level, id, level.getServer().overworld());
      return resolved == null ? level.getServer().overworld() : resolved;
   }

   private static ServerLevel resolveDimensionOrFallback(ServerLevel current, String id, ServerLevel fallback) {
      ResourceLocation location = ResourceLocation.tryParse(id == null ? "" : id);
      if (location == null) {
         return fallback;
      }
      ServerLevel resolved = current.getServer().getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location));
      return resolved == null ? fallback : resolved;
   }

   private static int findHajunSafeSpawnY(ServerLevel level, int x, int z) {
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMaxBuildHeight() - 1, z);
      for (int y = level.getMaxBuildHeight() - 1; y > level.getMinBuildHeight(); y--) {
         pos.setY(y);
         BlockState state = level.getBlockState(pos);
         if (!state.isAir() && state.isFaceSturdy(level, pos, Direction.UP)) {
            return y + 1;
         }
      }
      return 72;
   }

   private static void stainOdaHajunSurface(ServerLevel level, BlockPos center, int attempts) {
      for (int i = 0; i < attempts; i++) {
         BlockPos sample = center.offset(level.random.nextInt(51) - 25, level.random.nextInt(7) - 3, level.random.nextInt(51) - 25);
         BlockPos surface = findNearbyHajunSurface(level, sample);
         if (surface != null && canReplaceOdaHajunSurface(level.getBlockState(surface))) {
            level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
            if (level.random.nextInt(3) == 0) {
               level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
            }
         }
      }
   }

   private static void stainOdaHajunChantSurfaceFromCaster(ServerPlayer player, ServerLevel level, long now, int budget) {
      long chantAge = Math.max(0L, now - player.getPersistentData().getLong(HAJUN_START));
      if (chantAge < HAJUN_CHANT_SURFACE_SPREAD_DELAY) {
         return;
      }
      int radius = Mth.clamp(2 + (int)((chantAge - HAJUN_CHANT_SURFACE_SPREAD_DELAY) / 4L), 2, HAJUN_CHANT_SURFACE_RADIUS);
      BlockPos center = player.blockPosition();
      Map<BlockPos, BlockBackup> backups = ODA_CARD_HAJUN_CHANT_BLOCKS.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
      int changed = 0;
      int attempts = budget * 4;
      for (int i = 0; i < attempts && changed < budget; i++) {
         double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(player.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findNearbyHajunSurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceOdaHajunSurface(current)) {
            continue;
         }
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
         changed++;
         if (player.getRandom().nextInt(2) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static void emitOdaHajunAmbientFx(ServerPlayer player, ServerLevel level) {
      double x = player.getX();
      double y = player.getY() + player.getBbHeight() * 0.45;
      double z = player.getZ();
      level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 2.5, 0.35, 2.5, 0.01);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 1, 2.0, 0.25, 2.0, 0.005);
      level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.1, z, 2, 2.2, 0.2, 2.2, 0.01);
      if (level.random.nextInt(3) == 0) {
         level.sendParticles(ParticleTypes.FLAME, x, y + 0.15, z, 2, 1.8, 0.18, 1.8, 0.01);
      }
   }

   private static void spreadOdaHajunTerrain(ServerPlayer player, ServerLevel level, int budget) {
      BlockPos center = player.blockPosition();
      Map<BlockPos, BlockBackup> backups = ODA_CARD_HAJUN_BLOCKS.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
      long activeUntil = player.getPersistentData().getLong(HAJUN_FIELD_ACTIVE_UNTIL);
      long age = Math.max(0L, HAJUN_CARD_DURATION - Math.max(0L, activeUntil - level.getGameTime()));
      int radius = Mth.clamp(6 + (int)(age / 3L), 6, HAJUN_TERRAIN_RADIUS);
      for (int i = 0; i < budget; i++) {
         double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
         double distance = Math.sqrt(player.getRandom().nextDouble()) * radius;
         BlockPos sample = center.offset(Mth.floor(Math.cos(angle) * distance), 0, Mth.floor(Math.sin(angle) * distance));
         BlockPos surface = findNearbyHajunSurface(level, sample);
         if (surface == null || backups.containsKey(surface)) {
            continue;
         }
         BlockState current = level.getBlockState(surface);
         if (!canReplaceOdaHajunSurface(current)) {
            continue;
         }
         backups.put(surface.immutable(), new BlockBackup(current, saveBlockEntity(level, surface)));
         level.setBlock(surface, level.random.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState(), 3);
         if (player.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.FLAME, surface.getX() + 0.5, surface.getY() + 1.05, surface.getZ() + 0.5, 1, 0.1, 0.03, 0.1, 0.01);
         }
      }
   }

   private static void restoreOdaHajunTerrain(ServerPlayer player, ServerLevel level, int budget) {
      restoreOdaHajunBlocks(ODA_CARD_HAJUN_BLOCKS, player.getUUID(), level, budget);
   }

   private static void restoreOdaHajunChantTerrain(ServerPlayer player, ServerLevel level, int budget) {
      restoreOdaHajunBlocks(ODA_CARD_HAJUN_CHANT_BLOCKS, player.getUUID(), level, budget);
   }

   private static void restoreOdaHajunBlocks(Map<UUID, Map<BlockPos, BlockBackup>> store, UUID ownerId, ServerLevel level, int budget) {
      Map<BlockPos, BlockBackup> backups = store.get(ownerId);
      if (backups == null || backups.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<BlockPos, BlockBackup>> iterator = backups.entrySet().iterator();
      int restored = 0;
      while (iterator.hasNext() && restored++ < budget) {
         Map.Entry<BlockPos, BlockBackup> entry = iterator.next();
         BlockPos pos = entry.getKey();
         BlockBackup backup = entry.getValue();
         level.setBlock(pos, backup.state(), 3);
         if (backup.blockEntityNbt() != null && level.getBlockEntity(pos) instanceof BlockEntity blockEntity) {
            blockEntity.loadWithComponents(backup.blockEntityNbt(), level.registryAccess());
            blockEntity.setChanged();
         }
         iterator.remove();
      }
      if (backups.isEmpty()) {
         store.remove(ownerId);
      }
   }

   private static CompoundTag saveBlockEntity(ServerLevel level, BlockPos pos) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      return blockEntity == null ? null : blockEntity.saveWithFullMetadata(level.registryAccess());
   }

   private static BlockPos findNearbyHajunSurface(ServerLevel level, BlockPos sample) {
      int startY = Mth.clamp(sample.getY() + 4, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2);
      int minY = Math.max(level.getMinBuildHeight() + 1, sample.getY() - 8);
      for (int y = startY; y >= minY; y--) {
         BlockPos pos = new BlockPos(sample.getX(), y, sample.getZ());
         if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
            return pos;
         }
      }
      return null;
   }

   private static boolean canReplaceOdaHajunSurface(BlockState state) {
      return !state.isAir() && !state.hasBlockEntity() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.MAGMA_BLOCK) && !state.is(Blocks.NETHERRACK);
   }

   private record BlockBackup(BlockState state, CompoundTag blockEntityNbt) {
   }

   private static boolean canUseHeshikiriGun(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed
         && "oda_nobunaga".equals(vars.servant_card_id)
         && player.getMainHandItem().is(ModItems.HESHIKIRI_HASEBE.get());
   }

   private static boolean isOnCooldown(ServerPlayer player, String tag) {
      return player.getPersistentData().getInt(tag) > player.tickCount;
   }

   private static void setCooldown(ServerPlayer player, String tag, int ticks) {
      player.getPersistentData().putInt(tag, player.tickCount + Math.max(1, ticks));
   }

   private static void shootMatchlockBullet(ServerLevel level, LivingEntity owner, Vec3 pos, Vec3 direction, float damage, float speed, int kind) {
      Vec3 dir = direction.lengthSqr() < 1.0E-4 ? owner.getLookAngle() : direction.normalize();
      OdaMatchlockBulletEntity bullet = new OdaMatchlockBulletEntity(level, owner, null, damage).setBulletKind(kind);
      bullet.setPos(pos.x, pos.y, pos.z);
      bullet.setDeltaMovement(dir.scale(speed));
      level.addFreshEntity(bullet);
   }

   private static void muzzleFx(ServerLevel level, ServerPlayer player, Vec3 look, int intensity) {
      Vec3 pos = player.getEyePosition().add(look.normalize().scale(0.9));
      level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 8 + intensity * 4, 0.12 * intensity, 0.08 * intensity, 0.12 * intensity, 0.04);
      level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 3 + intensity * 2, 0.08 * intensity, 0.05 * intensity, 0.08 * intensity, 0.03);
      level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, Math.min(1.2F, 0.55F + intensity * 0.12F), 1.55F);
   }

   private static void dashSlash(ServerPlayer player, double distance, float damage, double knockback, boolean shortDash) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 dir = PlayerNoblePhantasmHelper.horizontalLook(player);
      Vec3 start = player.position();
      Vec3 end = start.add(dir.scale(distance));
      player.setDeltaMovement(dir.scale(shortDash ? 1.05 : 1.45).add(0.0, 0.08, 0.0));
      player.hurtMarked = true;
      player.fallDistance = 0.0F;
      AABB path = new AABB(start, end).inflate(1.15, 1.0, 1.15);
      for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, path, e -> e.isAlive() && e != player && !EntityUtils.isImmunePlayerTarget(e))) {
         living.invulnerableTime = 0;
         living.hurt(player.damageSources().playerAttack(player), hasTrait(living, ServantTraitTag.DIVINE) ? damage + 6.0F : damage);
         living.invulnerableTime = 0;
         living.push(dir.x * knockback, 0.16, dir.z * knockback);
         living.hurtMarked = true;
         OdaNobunagaCombatHelper.applyDivineDefenseBreak(player, living);
      }
      for (int i = 0; i < 8; i++) {
         Vec3 pos = start.lerp(end, i / 7.0);
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y + 1.0, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
         level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.55, pos.z, 3, 0.18, 0.12, 0.18, 0.03);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 0.85F);
   }

   private static OdaMatchlockGunEntity findMount(ServerLevel level, ServerPlayer player) {
      return level.getEntitiesOfClass(OdaMatchlockGunEntity.class, player.getBoundingBox().inflate(64.0), gun -> gun.isMountFor(player.getUUID()))
         .stream()
         .findFirst()
         .orElse(null);
   }

}

