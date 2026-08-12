package net.xxxjk.TYPE_MOON_WORLD.servant.lancelot;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LancelotWeaponItem;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterProtection;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LancelotBerserkerEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;

public final class LancelotCombatHelper {
   public static final String KNIGHT_OF_OWNER_TAG = "KnightOfOwner";
   public static final String KNIGHT_OF_OWNER_OWNER_TAG = "KnightOfOwnerOwner";
   public static final String AROUNDIGHT_MODE_TAG = "LancelotAroundightMode";
   public static final String AROUNDIGHT_DRAIN_TICK_TAG = "LancelotAroundightDrainTick";
   public static final String AROUNDIGHT_COOLDOWN_UNTIL_TAG = "LancelotAroundightCooldownUntil";
   public static final String UNABLE_UNTIL_TAG = "LancelotUnableUntil";
   public static final String BATTLE_KILLS_TAG = "LancelotBattleKills";
   public static final String BATTLE_KILL_BUFF_UNTIL_TAG = "LancelotBattleKillBuffUntil";
   public static final String LAST_PROJECTILE_INTERCEPT_TAG = "LancelotLastProjectileIntercept";
   public static final String LAST_THROW_TAG = "LancelotLastThrow";
   public static final double KNIGHT_OF_OWNER_MP_COST = 2.0;
   public static final double AROUNDIGHT_DRAW_MP_COST = 50.0;
   public static final double AROUNDIGHT_DRAIN_MP = 5.0;
   public static final int AROUNDIGHT_DRAIN_INTERVAL = 20;
   public static final int UNABLE_DURATION_TICKS = 45;
   public static final int PROJECTILE_INTERCEPT_COOLDOWN = 8;
   public static final int THROW_COOLDOWN = 70;
   private static final ResourceLocation AROUNDIGHT_DAMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_damage");
   private static final ResourceLocation AROUNDIGHT_ARMOR_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_armor");
   private static final ResourceLocation AROUNDIGHT_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_speed");
   private static final ResourceLocation AROUNDIGHT_ATTACK_SPEED_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_aroundight_attack_speed");
   private static final ResourceLocation KILL_BUFF_DAMAGE_ID =
      ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "lancelot_battle_kill_damage");
   private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.03F, 0.02F, 0.04F), 1.25F);
   private static final DustParticleOptions PURPLE_DUST = new DustParticleOptions(new Vector3f(0.28F, 0.08F, 0.48F), 1.0F);

   private LancelotCombatHelper() {
   }

   public static void tick(LancelotBerserkerEntity entity, ServerLevel level) {
      long now = level.getGameTime();
      if (isUnable(entity, now)) {
         entity.getNavigation().stop();
         if ((entity.tickCount & 7) == 0) {
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(), 8, 0.35, 0.45, 0.35, 0.04);
         }
         return;
      }
      tickAroundight(entity, level, now);
      ensureCombatLoadout(entity);
      tickBlessingBuff(entity, now);
      maybeInterceptProjectile(entity, level, now);
      spawnModeParticles(entity, level);
   }

   public static boolean isUnable(LivingEntity entity, long now) {
      return entity != null && now < entity.getPersistentData().getLong(UNABLE_UNTIL_TAG);
   }

   public static boolean isAroundightMode(LivingEntity entity) {
      return entity != null && entity.getPersistentData().getBoolean(AROUNDIGHT_MODE_TAG);
   }

   public static boolean tryDrawAroundight(LancelotBerserkerEntity entity, ServerLevel level, LivingEntity target) {
      long now = level.getGameTime();
      CompoundTag data = entity.getPersistentData();
      if (isAroundightMode(entity) || isUnable(entity, now) || now < data.getLong(AROUNDIGHT_COOLDOWN_UNTIL_TAG)
         || entity.getCurrentMp() < AROUNDIGHT_DRAW_MP_COST) {
         return false;
      }
      entity.setCurrentMp(entity.getCurrentMp() - AROUNDIGHT_DRAW_MP_COST);
      data.putBoolean(AROUNDIGHT_MODE_TAG, true);
      data.putLong(AROUNDIGHT_DRAIN_TICK_TAG, now + AROUNDIGHT_DRAIN_INTERVAL);
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.AROUNDIGHT.get()), entity));
      entity.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      applyAroundightModifiers(entity);
      level.playSound(null, entity.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_NP.get(), SoundSource.HOSTILE, 1.35F, 0.95F);
      level.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
      level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 40, 0.75, 0.8, 0.75, 0.2);
      return true;
   }

   public static void stopAroundight(LancelotBerserkerEntity entity, boolean unable) {
      CompoundTag data = entity.getPersistentData();
      if (!data.getBoolean(AROUNDIGHT_MODE_TAG)) {
         return;
      }
      data.remove(AROUNDIGHT_MODE_TAG);
      data.remove(AROUNDIGHT_DRAIN_TICK_TAG);
      data.putLong(AROUNDIGHT_COOLDOWN_UNTIL_TAG, entity.level().getGameTime() + 220L);
      removeAroundightModifiers(entity);
      if (unable) {
         data.putLong(UNABLE_UNTIL_TAG, entity.level().getGameTime() + UNABLE_DURATION_TICKS);
         if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, entity.blockPosition(), ModSounds.LANCELOT_BERSERKER_VOICE_FAIL.get(), SoundSource.HOSTILE, 1.0F, 0.82F);
         }
      }
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
   }

   public static void ensureCombatLoadout(LancelotBerserkerEntity entity) {
      if (isAroundightMode(entity)) {
         if (!(entity.getMainHandItem().getItem() instanceof LancelotWeaponItem weapon)
            || weapon.weaponType() != LancelotWeaponItem.WeaponType.AROUNDIGHT) {
            entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.AROUNDIGHT.get()), entity));
         }
         return;
      }
      ItemStack main = entity.getMainHandItem();
      if (main.isEmpty()) {
         entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
         return;
      }
      if (!isKnightOfOwner(main) && canOwnerize(main) && entity.getCurrentMp() >= KNIGHT_OF_OWNER_MP_COST) {
         entity.setCurrentMp(Math.max(0.0, entity.getCurrentMp() - KNIGHT_OF_OWNER_MP_COST));
         entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(main, entity));
      }
   }

   public static ItemStack knightOfOwnerStack(ItemStack stack, LivingEntity owner) {
      if (stack.isEmpty()) return stack;
      CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      tag.putBoolean(KNIGHT_OF_OWNER_TAG, true);
      if (owner != null) tag.putUUID(KNIGHT_OF_OWNER_OWNER_TAG, owner.getUUID());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   public static boolean isKnightOfOwner(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return false;
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data != null && data.copyTag().getBoolean(KNIGHT_OF_OWNER_TAG);
   }

   public static boolean canOwnerize(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() != Items.AIR;
   }

   public static void applyWeaponHit(LivingEntity owner, LivingEntity target, ItemStack stack) {
      if (owner == null || target == null || target == owner) return;
      if (isAroundightMode(owner) && stack.getItem() instanceof LancelotWeaponItem weapon
         && weapon.weaponType() == LancelotWeaponItem.WeaponType.AROUNDIGHT) {
         if (ServantIdentityHelper.hasTrait(target, ServantTraitTag.DRAGON)) {
            target.invulnerableTime = 0;
            target.hurt(owner.damageSources().mobAttack(owner), (float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5));
            target.invulnerableTime = 0;
         }
         spawnAroundightHit(owner, target);
      } else if (isKnightOfOwner(stack)) {
         target.invulnerableTime = 0;
         target.hurt(owner.damageSources().mobAttack(owner), (float)(owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5));
         target.invulnerableTime = 0;
         spawnKnightHit(owner, target);
      }
   }

   public static void recordKill(LancelotBerserkerEntity entity) {
      CompoundTag data = entity.getPersistentData();
      int kills = Math.min(5, data.getInt(BATTLE_KILLS_TAG) + 1);
      data.putInt(BATTLE_KILLS_TAG, kills);
      data.putLong(BATTLE_KILL_BUFF_UNTIL_TAG, entity.level().getGameTime() + 200L);
      updateKillBuff(entity, kills);
   }

   public static boolean throwHeldWeapon(LancelotBerserkerEntity entity, ServerLevel level, LivingEntity target) {
      if (isAroundightMode(entity) || target == null || !target.isAlive()) return false;
      CompoundTag data = entity.getPersistentData();
      long now = level.getGameTime();
      if (now - data.getLong(LAST_THROW_TAG) < THROW_COOLDOWN) return false;
      ItemStack held = entity.getMainHandItem();
      if (held.isEmpty()) return false;
      ItemStack thrown = held.copy();
      thrown.setCount(1);
      knightOfOwnerStack(thrown, entity);
      Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(0.6));
      Vec3 dir = target.getEyePosition().subtract(start);
      if (dir.lengthSqr() < 1.0E-4) return false;
      EmiyaThrownWeaponEntity projectile = new EmiyaThrownWeaponEntity(level, entity, thrown);
      projectile.setPos(start);
      projectile.setFixedDamage((float)Math.max(18.0, entity.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5));
      projectile.shoot(dir.x, dir.y, dir.z, 1.8F, 2.5F);
      level.addFreshEntity(projectile);
      data.putLong(LAST_THROW_TAG, now);
      entity.setItemInHand(InteractionHand.MAIN_HAND, knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), entity));
      ServantVoiceHelper.tryPlayAttack(entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 0.9F, 0.72F);
      return true;
   }

   private static void tickAroundight(LancelotBerserkerEntity entity, ServerLevel level, long now) {
      if (!isAroundightMode(entity)) {
         removeAroundightModifiers(entity);
         return;
      }
      applyAroundightModifiers(entity);
      if (now < entity.getPersistentData().getLong(AROUNDIGHT_DRAIN_TICK_TAG)) {
         return;
      }
      entity.getPersistentData().putLong(AROUNDIGHT_DRAIN_TICK_TAG, now + AROUNDIGHT_DRAIN_INTERVAL);
      if (entity.getCurrentMp() < AROUNDIGHT_DRAIN_MP) {
         stopAroundight(entity, true);
         return;
      }
      entity.setCurrentMp(entity.getCurrentMp() - AROUNDIGHT_DRAIN_MP);
      level.sendParticles(PURPLE_DUST, entity.getX(), entity.getY() + 1.05, entity.getZ(), 12, 0.55, 0.65, 0.55, 0.04);
   }

   private static void maybeInterceptProjectile(LancelotBerserkerEntity entity, ServerLevel level, long now) {
      if (isAroundightMode(entity) || now - entity.getPersistentData().getLong(LAST_PROJECTILE_INTERCEPT_TAG) < PROJECTILE_INTERCEPT_COOLDOWN) {
         return;
      }
      Entity projectile = level.getEntitiesOfClass(Entity.class, entity.getBoundingBox().inflate(4.0),
         candidate -> isInterceptableProjectile(entity, candidate)).stream()
         .min(Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
      if (projectile == null) return;
      entity.getPersistentData().putLong(LAST_PROJECTILE_INTERCEPT_TAG, now);
      LivingEntity target = entity.getTarget();
      if (target == null || !target.isAlive()) {
         target = scanNearestEnemy(entity, level);
      }
      ItemStack stack = stackForProjectile(projectile, entity);
      projectile.discard();
      if (target != null) {
         Vec3 start = entity.getEyePosition().add(entity.getLookAngle().scale(0.5));
         Vec3 dir = target.getEyePosition().subtract(start);
         EmiyaThrownWeaponEntity counter = new EmiyaThrownWeaponEntity(level, entity, stack);
         counter.setPos(start);
         counter.setFixedDamage(28.0F);
         counter.shoot(dir.x, dir.y, dir.z, 2.0F, 1.4F);
         level.addFreshEntity(counter);
      }
      level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.1, entity.getZ(), 22, 0.55, 0.65, 0.55, 0.08);
      level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.55F);
   }

   private static boolean isInterceptableProjectile(LancelotBerserkerEntity entity, Entity candidate) {
      if (candidate == entity || !candidate.isAlive()) return false;
      if (candidate instanceof GilgameshGateWeaponProjectileEntity gate) {
         LivingEntity owner = gate.getOwnerEntity();
         return owner != null && owner != entity && !owner.isAlliedTo(entity) && !ServantMasterProtection.isProtectedMaster(entity, owner);
      }
      if (candidate instanceof Projectile projectile) {
         Entity owner = projectile.getOwner();
         return owner instanceof LivingEntity living && living != entity && !living.isAlliedTo(entity)
            && !ServantMasterProtection.isProtectedMaster(entity, living);
      }
      return candidate instanceof ThrowableItemProjectile;
   }

   private static ItemStack stackForProjectile(Entity projectile, LancelotBerserkerEntity owner) {
      if (projectile instanceof ThrowableItemProjectile thrown) {
         ItemStack stack = thrown.getItem().copy();
         if (!stack.isEmpty()) return knightOfOwnerStack(stack, owner);
      }
      if (projectile instanceof GilgameshGateWeaponProjectileEntity gate && "gae_bulg".equals(gate.getWeaponId())) {
         return knightOfOwnerStack(new ItemStack(ModItems.GILGAMESH_GAE_BULG.get()), owner);
      }
      return knightOfOwnerStack(new ItemStack(ModItems.LANCELOT_IRON_ROD.get()), owner);
   }

   private static LivingEntity scanNearestEnemy(LancelotBerserkerEntity entity, ServerLevel level) {
      AABB area = entity.getBoundingBox().inflate(24.0);
      return level.getEntitiesOfClass(LivingEntity.class, area,
         candidate -> candidate != entity && candidate.isAlive() && !candidate.isAlliedTo(entity)
            && !EntityUtils.isImmunePlayerTarget(candidate) && !ServantMasterProtection.isProtectedMaster(entity, candidate)
            && EntityUtils.isValidCombatTarget(entity, candidate)).stream()
         .min(Comparator.comparingDouble(entity::distanceToSqr))
         .orElse(null);
   }

   private static void tickBlessingBuff(LancelotBerserkerEntity entity, long now) {
      CompoundTag data = entity.getPersistentData();
      if (now > data.getLong(BATTLE_KILL_BUFF_UNTIL_TAG)) {
         data.remove(BATTLE_KILLS_TAG);
         removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), KILL_BUFF_DAMAGE_ID);
         return;
      }
      updateKillBuff(entity, data.getInt(BATTLE_KILLS_TAG));
   }

   private static void updateKillBuff(LancelotBerserkerEntity entity, int kills) {
      if (kills <= 0) return;
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), KILL_BUFF_DAMAGE_ID, 0.10 * Math.min(5, kills), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void applyAroundightModifiers(LivingEntity entity) {
      updateModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID, 0.55, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      updateModifier(entity.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID, 0.75, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static void removeAroundightModifiers(LivingEntity entity) {
      removeModifier(entity.getAttribute(Attributes.ATTACK_DAMAGE), AROUNDIGHT_DAMAGE_ID);
      removeModifier(entity.getAttribute(Attributes.ARMOR), AROUNDIGHT_ARMOR_ID);
      removeModifier(entity.getAttribute(Attributes.MOVEMENT_SPEED), AROUNDIGHT_SPEED_ID);
      removeModifier(entity.getAttribute(Attributes.ATTACK_SPEED), AROUNDIGHT_ATTACK_SPEED_ID);
   }

   private static void updateModifier(AttributeInstance attribute, ResourceLocation id, double value, AttributeModifier.Operation operation) {
      if (attribute == null) return;
      attribute.removeModifier(id);
      attribute.addTransientModifier(new AttributeModifier(id, value, operation));
   }

   private static void removeModifier(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) attribute.removeModifier(id);
   }

   private static void spawnModeParticles(LancelotBerserkerEntity entity, ServerLevel level) {
      if ((entity.tickCount & 3) != 0) return;
      if (isAroundightMode(entity)) {
         level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 4, 0.45, 0.65, 0.45, 0.03);
      } else {
         level.sendParticles(DARK_DUST, entity.getX(), entity.getY() + 0.9, entity.getZ(), 5, 0.45, 0.65, 0.45, 0.035);
         if ((entity.tickCount & 15) == 0) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + 0.85, entity.getZ(), 2, 0.32, 0.45, 0.32, 0.02);
         }
      }
   }

   private static void spawnKnightHit(LivingEntity owner, LivingEntity target) {
      if (owner.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(DARK_DUST, center.x, center.y, center.z, 18, 0.38, 0.35, 0.38, 0.08);
         level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 8, 0.32, 0.25, 0.32, 0.08);
      }
   }

   private static void spawnAroundightHit(LivingEntity owner, LivingEntity target) {
      if (owner.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
         level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 22, 0.45, 0.4, 0.45, 0.1);
         level.sendParticles(PURPLE_DUST, center.x, center.y, center.z, 10, 0.35, 0.32, 0.35, 0.06);
      }
   }
}
