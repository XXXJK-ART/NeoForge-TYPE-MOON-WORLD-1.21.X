package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class MagicMagicBullet {
   private MagicMagicBullet() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = vars.isCurrentSelectionFromCrest("magic_bullet") ? 100.0 : vars.proficiency_magic_bullet;
      double cost = 8.0 + proficiency * 0.06;
      if (!ManaHelper.consumeOneTimeMagicCost(player, cost)) {
         return false;
      }

      int element = chooseElement(vars, proficiency, player.getRandom());
      float damage = damage(proficiency);
      if (element != MagicBulletProjectileEntity.ELEMENT_NONE) {
         damage += 10.0F + player.getRandom().nextFloat() * 5.0F;
      }
      fire(player, player.getLookAngle(), damage, slowPercent(proficiency), range(proficiency), speed(proficiency), element, 0.08F, proficiency);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.magic_bullet.cast"), true);
      if (!vars.isCurrentSelectionFromCrest("magic_bullet")) {
         vars.proficiency_magic_bullet = Math.min(100.0, vars.proficiency_magic_bullet + 0.18);
      }
      return true;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency) {
      if (caster == null || target == null || !target.isAlive() || vars == null) {
         return false;
      }
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      Vec3 direction = target.getEyePosition().subtract(EntityUtils.getRightHandCastAnchor(caster)).normalize();
      int element = chooseElement(vars, p, caster.getRandom());
      float damage = damage(p);
      if (element != MagicBulletProjectileEntity.ELEMENT_NONE) {
         damage += 10.0F + caster.getRandom().nextFloat() * 5.0F;
      }
      fire(caster, direction, damage, slowPercent(p), range(p), speed(p), element, 0.06F, p);
      return true;
   }

   public static float damage(double proficiency) {
      return (float)(3.0 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 32.0);
   }

   public static float slowPercent(double proficiency) {
      return (float)(BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 0.4);
   }

   public static double range(double proficiency) {
      return 15.0 + BasicMagecraftHelper.clampProficiency(proficiency) / 100.0 * 15.0;
   }

   public static float speed(double proficiency) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      if (p >= 75.0) {
         return 5.0F;
      } else if (p >= 50.0) {
         return 3.2F;
      } else if (p >= 25.0) {
         return 2.5F;
      }
      return 1.5F;
   }

   private static void fire(
      LivingEntity caster, Vec3 direction, float damage, float slowPercent, double range, float speed, int element, float inaccuracy, double proficiency
   ) {
      if (caster == null || caster.level().isClientSide || direction.lengthSqr() < 1.0E-6) {
         return;
      }
      Vec3 normalized = direction.normalize();
      MagicBulletProjectileEntity projectile = new MagicBulletProjectileEntity(caster.level(), caster);
      projectile.setNoGravity(true);
      projectile.setItem(new ItemStack(ModItems.MAGIC_FRAGMENTS.get()));
      projectile.configure(damage, slowPercent, range, element, element == MagicBulletProjectileEntity.ELEMENT_NONE ? 0.55F : 0.75F);
      projectile.setMagicSource("magic_bullet", proficiency);
      projectile.setPos(EntityUtils.getRightHandCastAnchor(caster).add(normalized.scale(0.12)));
      projectile.shoot(normalized.x, normalized.y, normalized.z, speed, inaccuracy);
      caster.level().addFreshEntity(projectile);
      caster.level().playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.55F);
   }

   private static int chooseElement(TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, net.minecraft.util.RandomSource random) {
      if (vars == null || proficiency < 60.0) {
         return MagicBulletProjectileEntity.ELEMENT_NONE;
      }
      List<Integer> elements = new ArrayList<>();
      if (vars.player_magic_attributes_fire) {
         elements.add(MagicBulletProjectileEntity.ELEMENT_FIRE);
      }
      if (vars.player_magic_attributes_water) {
         elements.add(MagicBulletProjectileEntity.ELEMENT_WATER);
      }
      if (vars.player_magic_attributes_earth) {
         elements.add(MagicBulletProjectileEntity.ELEMENT_EARTH);
      }
      if (vars.player_magic_attributes_wind) {
         elements.add(MagicBulletProjectileEntity.ELEMENT_WIND);
      }
      return elements.isEmpty() ? MagicBulletProjectileEntity.ELEMENT_NONE : elements.get(random.nextInt(elements.size()));
   }
}
