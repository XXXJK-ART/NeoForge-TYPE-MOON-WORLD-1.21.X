package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public final class ElementalMagicHelper {
   private ElementalMagicHelper() {
   }

   public static double proficiency(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null) {
         return 0.0;
      }
      if (vars.isCurrentSelectionFromCrest(magicId)) {
         return 100.0;
      }
      return switch (magicId) {
         case "fire_magic" -> vars.proficiency_fire_magic;
         case "water_magic" -> vars.proficiency_water_magic;
         case "wind_magic" -> vars.proficiency_wind_magic;
         case "earth_magic" -> vars.proficiency_earth_magic;
         default -> 0.0;
      };
   }

   public static void addPractice(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double amount) {
      if (vars == null || vars.isCurrentSelectionFromCrest(magicId)) {
         return;
      }
      amount *= matchingAttribute(vars, magicId) ? 1.5 : 1.0;
      switch (magicId) {
         case "fire_magic" -> vars.proficiency_fire_magic = Math.min(100.0, vars.proficiency_fire_magic + amount);
         case "water_magic" -> vars.proficiency_water_magic = Math.min(100.0, vars.proficiency_water_magic + amount);
         case "wind_magic" -> vars.proficiency_wind_magic = Math.min(100.0, vars.proficiency_wind_magic + amount);
         case "earth_magic" -> vars.proficiency_earth_magic = Math.min(100.0, vars.proficiency_earth_magic + amount);
      }
   }

   public static int tier(double proficiency) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      if (p >= 75.0) {
         return 3;
      } else if (p >= 50.0) {
         return 2;
      } else if (p >= 25.0) {
         return 1;
      }
      return 0;
   }

   public static double range(double proficiency) {
      return switch (tier(proficiency)) {
         case 3 -> 40.0;
         case 2 -> 30.0;
         case 1 -> 20.0;
         default -> 10.0;
      };
   }

   public static float lerpDamage(double proficiency, float low, float high) {
      double p = BasicMagecraftHelper.clampProficiency(proficiency);
      double tierStart = tier(proficiency) * 25.0;
      double local = Mth.clamp((p - tierStart) / 25.0, 0.0, 1.0);
      return (float)(low + (high - low) * local);
   }

   public static boolean hasUtilityUnlocked(String magicId, double proficiency) {
      int tier = tier(proficiency);
      return switch (magicId) {
         case "fire_magic" -> tier >= 2;
         case "water_magic", "wind_magic", "earth_magic" -> tier >= 1;
         default -> false;
      };
   }

   public static boolean matchingAttribute(TypeMoonWorldModVariables.PlayerVariables vars, String magicId) {
      if (vars == null) {
         return false;
      }
      return switch (magicId) {
         case "fire_magic" -> vars.player_magic_attributes_fire;
         case "water_magic" -> vars.player_magic_attributes_water;
         case "wind_magic" -> vars.player_magic_attributes_wind;
         case "earth_magic" -> vars.player_magic_attributes_earth;
         default -> false;
      };
   }

   public static double applyManaAffinity(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, double cost) {
      return cost * (matchingAttribute(vars, magicId) ? 0.85 : 1.0);
   }

   public static float applyPowerAffinity(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, float value) {
      return value * (matchingAttribute(vars, magicId) ? 1.15F : 1.0F);
   }

   public static float applyVisualAffinity(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, float value) {
      return value * (matchingAttribute(vars, magicId) ? 1.25F : 1.0F);
   }

   public static Vec3 aimDirection(LivingEntity caster, LivingEntity target, double speed) {
      if (target != null && target.isAlive()) {
         return target.getEyePosition().subtract(EntityUtils.getRightHandCastAnchor(caster)).normalize();
      }
      return caster.getLookAngle().normalize();
   }

   public static BlockPos targetBlock(LivingEntity caster, double range) {
      Vec3 start = caster.getEyePosition();
      Vec3 end = start.add(caster.getLookAngle().scale(range));
      HitResult hit = caster.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
      if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
         return blockHit.getBlockPos().relative(blockHit.getDirection());
      }
      Vec3 pos = caster.position().add(caster.getLookAngle().normalize().scale(Math.min(range, 8.0)));
      return BlockPos.containing(pos);
   }

   public static ElementalMagicProjectileEntity spawnProjectile(LivingEntity caster, Vec3 direction, int element, int form, float damage, float radius, float knockback, int igniteSeconds, int slowTicks, float slowPercent, double range, float speed, boolean pierceArmor, boolean cutBlocks, float visualScale) {
      if (caster == null || caster.level().isClientSide || direction.lengthSqr() < 1.0E-6) {
         return null;
      }
      Vec3 normalized = direction.normalize();
      ElementalMagicProjectileEntity projectile = new ElementalMagicProjectileEntity(caster.level(), caster);
      projectile.setNoGravity(true);
      projectile.setItem(new net.minecraft.world.item.ItemStack(ModItems.MAGIC_FRAGMENTS.get()));
      projectile.configure(element, form, damage, radius, knockback, igniteSeconds, slowTicks, slowPercent, range, pierceArmor, cutBlocks, visualScale);
      projectile.setMagicSource(magicIdForElement(element), 0.0);
      projectile.setPos(EntityUtils.getRightHandCastAnchor(caster).add(normalized.scale(0.12)));
      projectile.shoot(normalized.x, normalized.y, normalized.z, speed, 0.08F);
      caster.level().addFreshEntity(projectile);
      caster.level().playSound(null, caster.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.55F, 1.2F);
      return projectile;
   }

   public static ElementalMagicProjectileEntity spawnProjectile(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, LivingEntity caster, Vec3 direction, int element, int form, float damage, float radius, float knockback, int igniteSeconds, int slowTicks, float slowPercent, double range, float speed, boolean pierceArmor, boolean cutBlocks, float visualScale) {
      ElementalMagicProjectileEntity projectile = spawnProjectile(
         caster,
         direction,
         element,
         form,
         applyPowerAffinity(vars, magicId, damage),
         applyPowerAffinity(vars, magicId, radius),
         knockback,
         igniteSeconds,
         slowTicks,
         slowPercent,
         range,
         speed,
         pierceArmor,
         cutBlocks,
         applyVisualAffinity(vars, magicId, visualScale)
      );
      if (projectile != null) {
         projectile.setMagicSource(magicId, proficiency(vars, magicId));
      }
      return projectile;
   }

   public static ElementalMagicFieldEntity spawnField(LivingEntity caster, BlockPos pos, int element, int form, float radius, float width, int duration, float damagePerSecond) {
      if (caster == null || caster.level().isClientSide) {
         return null;
      }
      Level level = caster.level();
      ElementalMagicFieldEntity field = new ElementalMagicFieldEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, element, form, radius, width, duration, caster);
      field.setDamagePerSecond(damagePerSecond);
      level.addFreshEntity(field);
      level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.0F);
      return field;
   }

   public static ElementalMagicFieldEntity spawnField(TypeMoonWorldModVariables.PlayerVariables vars, String magicId, LivingEntity caster, BlockPos pos, int element, int form, float radius, float width, int duration, float damagePerSecond) {
      float visualScale = matchingAttribute(vars, magicId) ? 1.15F : 1.0F;
      return spawnField(
         caster,
         pos,
         element,
         form,
         radius * visualScale,
         width * visualScale,
         duration,
         applyPowerAffinity(vars, magicId, damagePerSecond)
      );
   }

   public static void changeGroundToMudOrFarmland(LivingEntity caster, double range) {
      if (!(caster.level() instanceof ServerLevel level)) {
         return;
      }
      BlockPos center = targetBlock(caster, range).below();
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            BlockPos pos = center.offset(x, 0, z);
            if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
               || level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DIRT)) {
               level.setBlock(pos, net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState(), 3);
            }
         }
      }
   }

   private static String magicIdForElement(int element) {
      return switch (element) {
         case ElementalMagicProjectileEntity.ELEMENT_WATER -> "water_magic";
         case ElementalMagicProjectileEntity.ELEMENT_WIND -> "wind_magic";
         case ElementalMagicProjectileEntity.ELEMENT_EARTH -> "earth_magic";
         default -> "fire_magic";
      };
   }
}
