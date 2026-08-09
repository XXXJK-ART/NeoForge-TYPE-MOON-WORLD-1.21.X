package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.ParticleTypes;
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
   private static final ResourceLocation HASSAN_ZABANIYA_CURSE_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_hassan_zabaniya_curse_attack");
   private static final ResourceLocation HASSAN_ZABANIYA_CURSE_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_hassan_zabaniya_curse_armor");

   private ServantCardHassanSkills() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!"cursed_arm_hassan".equals(vars.servant_card_id)) {
         return;
      }
      ServantCardConcealmentHelper.tick(player);
      keepRightHandEmpty(player);
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
      }
      ItemStack dirk = new ItemStack(ModItems.DIRK_SMALL_KNIFE.get(), 16);
      ServantCardTransformManager.markGeneratedItem(dirk, true, false);
      player.setItemInHand(InteractionHand.OFF_HAND, dirk);
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

   public static void performSelfModification(ServerPlayer player) {
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true, true));
   }

   public static void performDirkThrow(ServerPlayer player) {
      for (int i = -1; i <= 1; i++) {
         net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity projectile = new net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity(player.level(), player);
         projectile.setDamage(14.0F);
         projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
         Vec3 look = player.getLookAngle().yRot((float)Math.toRadians(i * 5.0));
         projectile.shoot(look.x, look.y, look.z, 2.2F, 0.0F);
         player.level().addFreshEntity(projectile);
      }
   }

   public static void performShadowStep(ServerPlayer player) {
      Vec3 target = player.position().add(player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(6.0));
      if (ServantCardSkillUtils.trySafeHorizontalTeleport(player, target.add(0.0, 0.1, 0.0))) {
         ServantCardConcealmentHelper.apply(player, 40);
      }
   }

   public static void performShadowLunge(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 14.0, 1.8);
      Vec3 dir = target == null ? player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : target.position().subtract(player.position()).normalize();
      player.setDeltaMovement(dir.scale(2.4).add(0.0, 0.18, 0.0));
      player.hurtMarked = true;
      ServantCardSkillUtils.hitForwardArc(player, dir, 5.5, 30.0F);
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
      vars.servant_card_hassan_zabaniya_animation_until = player.tickCount + 32;
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

      TYPE_MOON_WORLD.queueServerWork(12, () -> resolveHassanZabaniya(player, target));
      return true;
   }

   public static LivingEntity findZabaniyaTarget(ServerPlayer player) {
      LivingEntity target = ServantCardSkillUtils.findLookTarget(player, 7.0, 1.75);
      if (target == null || !target.isAlive() || CursedArmHassanCombatHelper.isProtectedPigKind(target)) {
         return null;
      }
      return target;
   }

   private static void resolveHassanZabaniya(ServerPlayer player, LivingEntity target) {
      if (!player.isAlive()
         || target == null
         || !target.isAlive()
         || EntityUtils.isImmunePlayerTarget(target)
         || player.distanceToSqr(target) > 8.5 * 8.5
         || CursedArmHassanCombatHelper.isProtectedPigKind(target)) {
         return;
      }
      boolean instantDeathTarget = CursedArmHassanCombatHelper.isHumanoidInstantDeathTarget(target);
      boolean killed = instantDeathTarget && player.getRandom().nextFloat() < 0.6F;
      if (target.getPersistentData().getBoolean("CausalSevered")) {
         killed = false;
      }
      if (killed && ArtoriaPendragonCombatHelper.tryNegateCertainHitOrDeath(target, "zabaniya")) {
         spawnHassanZabaniyaImpact(player, target, false);
         return;
      }
      if (killed && HeraclesGodHandHelper.consumeLifeForZabaniya(target)) {
         spawnHassanZabaniyaImpact(player, target, true);
         return;
      }

      target.invulnerableTime = 0;
      if (killed) {
         target.hurt(player.damageSources().playerAttack(player), Math.max(target.getMaxHealth() * 2.0F, 500.0F));
         target.invulnerableTime = 0;
         if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(player.damageSources().genericKill());
         }
      } else {
         target.hurt(player.damageSources().playerAttack(player), 55.0F);
         target.invulnerableTime = 0;
         applyHassanZabaniyaCurse(target);
      }
      spawnHassanZabaniyaImpact(player, target, killed);
   }

   private static void applyHassanZabaniyaCurse(LivingEntity target) {
      int duration = 160;
      AttributeInstance attack = target.getAttribute(Attributes.ATTACK_DAMAGE);
      if (attack != null) {
         attack.removeModifier(HASSAN_ZABANIYA_CURSE_ATTACK_ID);
         attack.addTransientModifier(new AttributeModifier(HASSAN_ZABANIYA_CURSE_ATTACK_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
      if (armor != null) {
         armor.removeModifier(HASSAN_ZABANIYA_CURSE_ARMOR_ID);
         armor.addTransientModifier(new AttributeModifier(HASSAN_ZABANIYA_CURSE_ARMOR_ID, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, true, true));
      TYPE_MOON_WORLD.queueServerWork(duration, () -> {
         if (!target.isAlive()) {
            return;
         }
         AttributeInstance delayedAttack = target.getAttribute(Attributes.ATTACK_DAMAGE);
         if (delayedAttack != null) {
            delayedAttack.removeModifier(HASSAN_ZABANIYA_CURSE_ATTACK_ID);
         }
         AttributeInstance delayedArmor = target.getAttribute(Attributes.ARMOR);
         if (delayedArmor != null) {
            delayedArmor.removeModifier(HASSAN_ZABANIYA_CURSE_ARMOR_ID);
         }
      });
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
