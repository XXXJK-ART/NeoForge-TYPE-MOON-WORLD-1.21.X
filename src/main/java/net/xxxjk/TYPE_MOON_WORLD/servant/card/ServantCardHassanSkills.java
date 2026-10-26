package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesGodHandHelper;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

public final class ServantCardHassanSkills {
   private static final String SELF_MOD_LEVEL = "CursedArmSelfModificationLevel";
   private static final ResourceLocation SELF_MOD_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cursed_arm_self_mod_health");
   private static final ResourceLocation SELF_MOD_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "cursed_arm_self_mod_attack");

   private ServantCardHassanSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"cursed_arm_hassan".equals(vars.servant_card_id)) {
         return;
      }
      ServantCardConcealmentHelper.tick(player);
      keepRightHandEmpty(player);
       syncSelfModificationAttributes(player);
      if (!vars.servant_card_hassan_cloak_broken && player.getHealth() <= player.getMaxHealth() * (2.0F / 3.0F)) {
         vars.servant_card_hassan_cloak_broken = true;
         vars.syncPlayerVariables(player);
         if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.72, player.getZ(), 24, 0.45, 0.5, 0.45, 0.045);
            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 0.7F, 0.75F);
         }
      }
   }

   public static void giveDirk(ServerPlayer player) {
      keepRightHandEmpty(player);
      ItemStack current = player.getOffhandItem();
      if (!current.isEmpty() && !current.is(ModItems.DIRK_SMALL_KNIFE.get())) {
         storeOrDrop(player, current.copy());
         current = ItemStack.EMPTY;
      }
      if (current.isEmpty()) {
         ItemStack dirk = new ItemStack(ModItems.DIRK_SMALL_KNIFE.get(), 16);
         ServantCardTransformManager.markGeneratedItem(dirk, true, false);
         player.setItemInHand(InteractionHand.OFF_HAND, dirk);
      } else if (current.getCount() < 16) {
         current.grow(16 - current.getCount());
      }
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 16, 0.28, 0.35, 0.28, 0.04);
         level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + player.getBbHeight() * 0.48, player.getZ(), 8, 0.22, 0.25, 0.22, 0.05);
         level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.55F, 1.65F);
      }
   }

   private static void keepRightHandEmpty(ServerPlayer player) {
      ItemStack main = player.getMainHandItem();
      if (main.isEmpty()) {
         return;
      }
      ItemStack off = player.getOffhandItem();
      if (off.isEmpty()) {
         player.setItemInHand(InteractionHand.OFF_HAND, main.copy());
      } else {
         storeOrDrop(player, main.copy());
      }
      player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
   }

   private static void storeOrDrop(ServerPlayer player, ItemStack stack) {
      if (!stack.isEmpty() && !player.getInventory().add(stack)) {
         player.drop(stack, false);
      }
   }

   public static int selfModificationLevel(ServerPlayer player) {
      return Math.max(0, Math.min(3, player.getPersistentData().getInt(SELF_MOD_LEVEL)));
   }

   public static boolean performSelfModification(ServerPlayer player) {
      int level = selfModificationLevel(player);
      if (level >= 3) return false;
      player.getPersistentData().putInt(SELF_MOD_LEVEL, level + 1);
      syncSelfModificationAttributes(player);
      player.heal(50.0F);
      if (player.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + player.getBbHeight() * 0.55, player.getZ(), 24, 0.35, 0.45, 0.35, 0.03);
         serverLevel.playSound(null, player.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 0.8F, 0.75F);
      }
      return true;
   }

   private static void syncSelfModificationAttributes(ServerPlayer player) {
      int level = selfModificationLevel(player);
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) {
         AttributeModifier current = health.getModifier(SELF_MOD_HEALTH_ID);
         double expected = level * 50.0;
         if (level == 0) health.removeModifier(SELF_MOD_HEALTH_ID);
         else if (current == null || current.amount() != expected || current.operation() != AttributeModifier.Operation.ADD_VALUE) {
            health.removeModifier(SELF_MOD_HEALTH_ID);
            health.addTransientModifier(new AttributeModifier(SELF_MOD_HEALTH_ID, expected, AttributeModifier.Operation.ADD_VALUE));
         }
      }
      AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         AttributeModifier current = attack.getModifier(SELF_MOD_ATTACK_ID);
         double expected = level * 5.0;
         if (level == 0) attack.removeModifier(SELF_MOD_ATTACK_ID);
         else if (current == null || current.amount() != expected || current.operation() != AttributeModifier.Operation.ADD_VALUE) {
            attack.removeModifier(SELF_MOD_ATTACK_ID);
            attack.addTransientModifier(new AttributeModifier(SELF_MOD_ATTACK_ID, expected, AttributeModifier.Operation.ADD_VALUE));
         }
      }
   }

   public static void clear(ServerPlayer player) {
      player.getPersistentData().remove(SELF_MOD_LEVEL);
      player.getPersistentData().remove(CursedArmHassanCombatHelper.ZABANIYA_WINDUP_UNTIL);
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      vars.syncPlayerVariables(player);
      AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
      if (health != null) health.removeModifier(SELF_MOD_HEALTH_ID);
      AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) attack.removeModifier(SELF_MOD_ATTACK_ID);
      player.getPersistentData().remove(CursedArmHassanCombatHelper.WIND_DODGE_CHARGES);
      player.getPersistentData().remove(CursedArmHassanCombatHelper.WIND_DODGE_RESTORE_AT);
   }

   public static void performDirkThrow(ServerPlayer player) {
      ItemStack knives = player.getOffhandItem();
      if (!knives.is(ModItems.DIRK_SMALL_KNIFE.get()) || knives.getCount() < 3) return;
      knives.shrink(3);
      for (int i = -1; i <= 1; i++) {
         net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity projectile = new net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity(player.level(), player);
         projectile.setDamage(14.0F);
         projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
          projectile.setMaxDistance(18.0);
         Vec3 look = player.getLookAngle().yRot((float)Math.toRadians(i * 5.0));
         projectile.shoot(look.x, look.y, look.z, 2.2F, 0.0F);
         player.level().addFreshEntity(projectile);
      }
   }

   public static boolean performShadowStep(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 8.0, 1.8);
      if (!isValidHassanTarget(player, target) || !player.hasLineOfSight(target)) return false;
      Vec3 destination = findSafeBackstabPosition(player, target);
      if (destination == null) return false;
      player.teleportTo(destination.x, destination.y, destination.z);
      player.setDeltaMovement(Vec3.ZERO);
      player.hurtMarked = true;
      player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.position());
      dealScaledDamage(player, target, 1.65F);
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      return true;
   }

   private static Vec3 findSafeBackstabPosition(ServerPlayer player, LivingEntity target) {
      if (!(player.level() instanceof ServerLevel level)) return null;
      Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(1.35));
      BlockPos anchor = BlockPos.containing(behind.x, target.getY(), behind.z);
      for (int dy = -2; dy <= 3; dy++) {
         BlockPos feet = anchor.offset(0, dy, 0);
         BlockPos below = feet.below();
         if (!level.getBlockState(below).isSolidRender(level, below)
            || !level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
            || !level.getWorldBorder().isWithinBounds(feet)) continue;
         Vec3 candidate = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
         if (!level.noCollision(player, player.getBoundingBox().move(candidate.subtract(player.position())))) continue;
         Vec3 eye = candidate.add(0.0, player.getBbHeight() * 0.55, 0.0);
         if (level.clip(new ClipContext(player.getEyePosition(), eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType()
            != net.minecraft.world.phys.HitResult.Type.MISS) continue;
         return candidate;
      }
      return null;
   }

   public static boolean performShadowLunge(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 6.5, 1.8);
      if (!isValidHassanTarget(player, target) || !player.hasLineOfSight(target)) return false;
      Vec3 dir = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0).normalize();
      player.setDeltaMovement(dir.scale(1.65).add(0.0, 0.18, 0.0));
      player.hurtMarked = true;
      dealScaledDamage(player, target, 1.05F);
      target.push(dir.x * 0.85, 0.24, dir.z * 0.85);
      target.hurtMarked = true;
      return true;
   }

   public static boolean performKnifeFeint(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 9.0, 1.8);
      if (!isValidHassanTarget(player, target) || player.distanceTo(target) < 2.5 || !player.hasLineOfSight(target)) return false;
      if (!dealScaledDamage(player, target, 0.72F)) return false;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1, false, true, true));
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 35, 0, false, true, true));
      return true;
   }

   public static boolean performElbowStrike(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 2.8, 1.6);
      if (!isValidHassanTarget(player, target) || !player.hasLineOfSight(target)) return false;
      if (dealScaledDamage(player, target, 0.90F)) applyShortInterrupt(target, player.level().getGameTime());
      return true;
   }

   public static boolean performTripSweep(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 2.5, 1.6);
      if (!isValidHassanTarget(player, target) || !player.hasLineOfSight(target)) return false;
      if (!dealScaledDamage(player, target, 0.65F)) return false;
      target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
      return true;
   }

   private static boolean dealScaledDamage(ServerPlayer player, LivingEntity target, float scale) {
      AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
      float damage = (float)((attack == null ? 8.0 : attack.getValue()) * scale);
      return target.hurt(player.damageSources().playerAttack(player), damage);
   }

   public static boolean hasDirks(ServerPlayer player) {
      ItemStack knives = player.getOffhandItem();
      return knives.is(ModItems.DIRK_SMALL_KNIFE.get()) && knives.getCount() >= 3;
   }

   private static boolean isValidHassanTarget(ServerPlayer player, LivingEntity target) {
      return target != null && EntityUtils.isValidCombatTarget(player, target)
         && !net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting.isContractMaster(player, target)
         && !CursedArmHassanCombatHelper.isForbiddenTarget(target);
   }

   private static void applyShortInterrupt(LivingEntity target, long now) {
      if (target instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servant) {
         servant.getPersistentData().putLong(net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem.TAG_STUN_UNTIL, now + 8L);
      } else {
         target.getPersistentData().putLong("ServantCardCombatStunUntil", now + 8L);
      }
   }

   public static boolean performZabaniya(ServerPlayer player) {
      LivingEntity target = findZabaniyaTarget(player);
      if (target == null) {
         return false;
      }
      if (!(player.level() instanceof ServerLevel level)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      player.getPersistentData().putLong(CursedArmHassanCombatHelper.ZABANIYA_WINDUP_UNTIL, player.level().getGameTime() + 20L);
      vars.servant_card_hassan_zabaniya_animation_until = player.tickCount + 40;
      vars.syncPlayerVariables(player);
      Vec3 start = player.position().add(0.0, player.getBbHeight() * 0.7, 0.0);
      Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      for (double t = 0.0; t <= 1.0; t += 0.1) {
         Vec3 pos = start.lerp(end, t);
         level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 2, 0.035, 0.035, 0.035, 0.01);
         if (((int)(t * 10.0)) % 2 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 1, 0.025, 0.025, 0.025, 0.005);
         }
      }
      VFXServerEffects.spawn(level, "servant_cursed_arm_zabaniya_windup", player, 96.0);
      level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 0.75F, 0.62F);

      TYPE_MOON_WORLD.queueServerWork(20, () -> resolveHassanZabaniya(player, target));
      return true;
   }

   public static LivingEntity findZabaniyaTarget(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 7.0, 1.75);
      if (!isValidHassanTarget(player, target) || !player.hasLineOfSight(target)) {
         return null;
      }
      return target;
   }

   private static void resolveHassanZabaniya(ServerPlayer player, LivingEntity target) {
      long windupUntil = player.getPersistentData().getLong(CursedArmHassanCombatHelper.ZABANIYA_WINDUP_UNTIL);
      player.getPersistentData().remove(CursedArmHassanCombatHelper.ZABANIYA_WINDUP_UNTIL);
      if (windupUntil <= 0L || player.level().getGameTime() < windupUntil) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_hassan_zabaniya_animation_until = 0;
      vars.syncPlayerVariables(player);
      if (!player.isAlive() || player.isRemoved() || player.hasDisconnected()
         || !vars.servant_card_transformed || !"cursed_arm_hassan".equals(vars.servant_card_id)
         || target == null
         || !target.isAlive()
         || EntityUtils.isImmunePlayerTarget(target)
          || target.level() != player.level()
          || player.distanceToSqr(target) > 7.0 * 7.0
          || !player.hasLineOfSight(target)
          || CursedArmHassanCombatHelper.isForbiddenTarget(target)) {
         return;
      }
      boolean instantDeathTarget = CursedArmHassanCombatHelper.isHumanoidInstantDeathTarget(target);
      boolean killed = instantDeathTarget && player.getRandom().nextFloat() < 0.6F;
      if (target.getPersistentData().getBoolean("CausalSevered")) {
         killed = false;
      }
      if (killed && ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "zabaniya", net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService.attackToken(player))) {
         spawnHassanZabaniyaImpact(player, target, false);
         return;
      }
       if (net.xxxjk.TYPE_MOON_WORLD.servant.heracles.GodHandService.conceptHit(target,player.damageSources().playerAttack(player),55,"cursed_arm_hassan:zabaniya",killed)) {
         spawnHassanZabaniyaImpact(player, target, true);
         return;
      }

      if (killed) {
         target.hurt(player.damageSources().playerAttack(player), Math.max(target.getMaxHealth() * 2.0F, 500.0F));
         if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(player.damageSources().genericKill());
         }
      } else {
         boolean hit = target.hurt(player.damageSources().playerAttack(player), 55.0F);
         if (hit) applyHassanZabaniyaCurse(target);
      }
      spawnHassanZabaniyaImpact(player, target, killed);
   }

   private static void applyHassanZabaniyaCurse(LivingEntity target) {
      CursedArmHassanCombatHelper.applyZabaniyaCurse(target);
   }

   private static void spawnHassanZabaniyaImpact(ServerPlayer player, LivingEntity target, boolean killed) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      VFXServerEffects.spawn(level, "servant_cursed_arm_zabaniya_impact", target.position(), 96.0);
      level.sendParticles(killed ? ParticleTypes.DRAGON_BREATH : ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), killed ? 38 : 22, 0.38, 0.48, 0.38, 0.045);
      level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.62, target.getZ(), 10, 0.25, 0.28, 0.25, 0.08);
      level.playSound(null, target.blockPosition(), killed ? SoundEvents.WITHER_DEATH : SoundEvents.WITHER_HURT, SoundSource.PLAYERS, killed ? 0.95F : 0.75F, 1.25F);
   }


}
