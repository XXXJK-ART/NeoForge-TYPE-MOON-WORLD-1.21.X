package net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

public class MagicSwordBarrelFullOpen {
   private static final int COOLDOWN = 10;
   private static final double MANA_COST = 30.0;

   public static void execute(Entity entity) {
      if (entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (vars.has_unlimited_blade_works) {
            if (!(vars.proficiency_unlimited_blade_works < 1.0)) {
               int mode = vars.sword_barrel_mode;
               if (mode == 4) {
                  if (!vars.is_sword_barrel_active) {
                     vars.is_sword_barrel_active = true;
                     vars.syncPlayerVariables(player);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.trace_off"), true);
                     player.getPersistentData().putDouble("SwordBarrelClearRadius", 40.0);
                  } else {
                     vars.is_sword_barrel_active = false;
                     vars.syncPlayerVariables(player);
                     player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.cancelled"), true);
                  }
               } else if (mode == 3) {
                  boolean isBP = player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled");
                  isBP = !isBP;
                  player.getPersistentData().putBoolean("UBWBrokenPhantasmEnabled", isBP);
                  vars.ubw_broken_phantasm_enabled = isBP;
                  vars.syncPlayerVariables(player);
                  if (isBP) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.ubw.broken_phantasm.on"), true);
                  } else {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.ubw.broken_phantasm.off"), true);
                  }
               } else {
                  vars.is_sword_barrel_active = !vars.is_sword_barrel_active;
                  vars.syncPlayerVariables(player);
                  if (vars.is_sword_barrel_active) {
                     if (mode != 4) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.trace_on"), true);
                     }
                  } else if (mode != 2) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.trace_off"), true);
                  }
               }
            }
         }
      }
   }

   public static void tick(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.is_sword_barrel_active) {
         boolean interrupt = false;
         if (!vars.is_magic_circuit_open) {
            interrupt = true;
         } else if (!vars.has_unlimited_blade_works) {
            interrupt = true;
         } else if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
            String currentMagic = vars.selected_magics.get(vars.current_magic_index);
            if (!"sword_barrel_full_open".equals(currentMagic)) {
               interrupt = true;
            }
         } else {
            interrupt = true;
         }

         if (interrupt) {
            vars.is_sword_barrel_active = false;
            vars.syncPlayerVariables(player);
         } else {
            int mode = vars.sword_barrel_mode;
            if (mode == 4) {
               executeMode4(player, vars);
            } else if (mode == 3) {
               vars.is_sword_barrel_active = false;
               vars.syncPlayerVariables(player);
            } else {
               boolean noCooldown = player.getPersistentData().getBoolean("TypeMoonNoCooldown");
               if (noCooldown || player.tickCount % 5 == 0) {
                  double cost = 30.0;
                  if (player.level().dimension() == ModDimensions.UBW_KEY) {
                     cost = 15.0;
                  }

                  if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
                     cost *= 2.0;
                  }

                  if (!ManaHelper.consumeManaOrHealth(player, cost)) {
                     player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
                     vars.is_sword_barrel_active = false;
                     vars.syncPlayerVariables(player);
                  } else {
                     if (mode == 0) {
                        executeMode0(player, vars);
                     } else if (mode == 1) {
                        executeMode1(player, vars);
                     } else if (mode == 2) {
                        executeMode2(player, vars);
                     }
                  }
               }
            }
         }
      }
   }

   private static void executeMode4(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      double currentR = player.getPersistentData().getDouble("SwordBarrelClearRadius");
      if (currentR <= 0.0) {
         vars.is_sword_barrel_active = false;
         vars.syncPlayerVariables(player);
      } else {
         double speed = 1.0;
         double nextR = currentR - speed;
         if (nextR < 0.0) {
            nextR = 0.0;
         }

         BlockPos center = player.blockPosition();
         Level level = player.level();
         int minX = (int)Math.floor(-currentR);
         int maxX = (int)Math.ceil(currentR);
         int minY = (int)Math.floor(-currentR);
         int maxY = (int)Math.ceil(currentR);
         int minZ = (int)Math.floor(-currentR);
         int maxZ = (int)Math.ceil(currentR);
         double currentRSqr = currentR * currentR;
         double nextRSqr = nextR * nextR;

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  double distSqr = x * x + y * y + z * z;
                  if (distSqr <= currentRSqr && distSqr > nextRSqr) {
                     BlockPos pos = center.offset(x, y, z);
                     if (level.getBlockState(pos).getBlock() instanceof SwordBarrelBlock) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        if (level instanceof ServerLevel serverLevel) {
                           serverLevel.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.1, 0.1, 0.1, 0.05);
                        }
                     }
                  }
               }
            }
         }

         player.getPersistentData().putDouble("SwordBarrelClearRadius", nextR);
      }
   }

   private static void executeMode2(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      List<ItemStack> weapons = new ArrayList<>();

      for (ItemStack stack : vars.analyzed_items) {
         if (!(stack.getItem() instanceof NoblePhantasmItem)
            && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem)) {
            weapons.add(stack);
         }
      }

      if (weapons.isEmpty()) {
         vars.is_sword_barrel_active = false;
         vars.syncPlayerVariables(player);
      } else {
         double range = 40.0;
         Vec3 eyePos = player.getEyePosition();
         Vec3 lookVec = player.getLookAngle();
         Vec3 endPos = eyePos.add(lookVec.scale(range));
         Level level = player.level();
         Entity targetEntity = null;
         Vec3 targetPos = endPos;
         AABB rayBox = new AABB(eyePos, endPos).inflate(2.0);
         List<Entity> entities = level.getEntities(
            player, rayBox, e -> e instanceof LivingEntity && !e.isSpectator() && !(e instanceof Player p && p.isCreative())
         );
         double closestDist = range * range;

         for (Entity e : entities) {
            AABB entityBox = e.getBoundingBox().inflate(0.5);
            Optional<Vec3> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
               double dist = eyePos.distanceToSqr(hit.get());
               if (dist < closestDist) {
                  closestDist = dist;
                  targetEntity = e;
                  targetPos = hit.get();
               }
            }
         }

         RandomSource random = player.getRandom();
         int baseCount = getSwordCount(vars.proficiency_unlimited_blade_works, 75);
         boolean isInsideUBW = player.level().dimension().location().equals(ModDimensions.UBW_KEY.location());
         if (!isInsideUBW) {
            baseCount = (int)(baseCount * 0.66);
            if (baseCount < 5) {
               baseCount = 5;
            }
         }

         int swordCount = baseCount;
         Vec3 horizontalLook = new Vec3(lookVec.x, 0.0, lookVec.z).normalize();
         Vec3 rightVec = horizontalLook.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
         double centerOffsetH = -1.5;
         double centerOffsetV = 2.5;
         Vec3 centerPos = player.position().add(0.0, player.getEyeHeight() * 0.8, 0.0).add(horizontalLook.scale(centerOffsetH)).add(0.0, centerOffsetV, 0.0);
         double lineWidth = 20.0;
         double spacing = lineWidth / (baseCount - 1);
         double startOffset = -lineWidth / 2.0;

         for (int i = 0; i < swordCount; i++) {
            double offset = startOffset + i * spacing;
            offset += (random.nextDouble() - 0.5) * 0.5;
            double vJitter = (random.nextDouble() - 0.5) * 4.0;
            Vec3 spawnPos = centerPos.add(rightVec.scale(offset)).add(0.0, vJitter, 0.0);
            BlockPos blockPos = BlockPos.containing(spawnPos);
            if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
               spawnPos = spawnPos.add(0.0, -1.0, 0.0);
            }

            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(level, player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
               projectile.setBrokenPhantasm(true);
            }

            if (targetEntity != null) {
               projectile.setTargetEntity(targetEntity.getId());
            }

            Vec3 aimTarget = targetEntity != null ? targetEntity.position().add(0.0, targetEntity.getBbHeight() * 0.5, 0.0) : targetPos;
            projectile.setHover(20, aimTarget);
            projectile.setOwner(player);
            projectile.setMode2Tracking(true);
            Vec3 dir = aimTarget.subtract(spawnPos).normalize();
            projectile.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
            projectile.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, dir.z)));
            level.addFreshEntity(projectile);
            if (level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
         }

         level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.5F, 1.5F);
         vars.is_sword_barrel_active = false;
         vars.syncPlayerVariables(player);
      }
   }

   private static void executeMode0(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      List<ItemStack> weapons = new ArrayList<>();

      for (ItemStack stack : vars.analyzed_items) {
         if (!(stack.getItem() instanceof NoblePhantasmItem)
            && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem)) {
            weapons.add(stack);
         }
      }

      if (!weapons.isEmpty()) {
         RandomSource random = player.getRandom();
         int swordCount = getSwordCount(vars.proficiency_unlimited_blade_works, 20);
         Vec3 lookVec = player.getLookAngle();
         Vec3 horizontalLook = new Vec3(lookVec.x, 0.0, lookVec.z).normalize();
         double centerOffsetH = -2.0;
         double centerOffsetV = 2.0;
         Vec3 centerPos = player.position().add(0.0, player.getEyeHeight() * 0.8, 0.0).add(horizontalLook.scale(centerOffsetH)).add(0.0, centerOffsetV, 0.0);
         Vec3 rightVec = horizontalLook.cross(new Vec3(0.0, 1.0, 0.0)).normalize();

         for (int i = 0; i < swordCount; i++) {
            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            double width = 18.0;
            double spread = (random.nextDouble() - 0.5) * width;
            double vNoise = (random.nextDouble() - 0.5) * 4.0;
            Vec3 spawnPos = centerPos.add(rightVec.scale(spread)).add(0.0, vNoise, 0.0);
            if (!player.level().getBlockState(BlockPos.containing(spawnPos)).isAir()) {
               spawnPos = spawnPos.add(0.0, -1.0, 0.0);
            }

            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(player.level(), player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
               projectile.setBrokenPhantasm(true);
            }

            Vec3 targetPoint = player.getEyePosition().add(lookVec.scale(20.0));
            Vec3 dir = targetPoint.subtract(spawnPos).normalize();
            dir = dir.add((random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15, (random.nextDouble() - 0.5) * 0.15).normalize();
            double speed = 1.8 + random.nextDouble() * 0.6;
            projectile.setDeltaMovement(dir.scale(speed));
            projectile.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
            projectile.setYRot((float)Math.toDegrees(Math.atan2(-dir.x, dir.z)));
            player.level().addFreshEntity(projectile);
            if (player.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
         }

         player.level().playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.5F, 1.5F);
      }
   }

   private static void executeMode1(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      List<ItemStack> weapons = new ArrayList<>();

      for (ItemStack stack : vars.analyzed_items) {
         if (!(stack.getItem() instanceof NoblePhantasmItem)
            && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem)) {
            weapons.add(stack);
         }
      }

      if (!weapons.isEmpty()) {
         double range = 64.0;
         Vec3 eyePos = player.getEyePosition();
         Vec3 lookVec = player.getLookAngle();
         Vec3 endPos = eyePos.add(lookVec.scale(range));
         Level level = player.level();
         BlockHitResult blockHit = level.clip(new ClipContext(eyePos, endPos, Block.COLLIDER, Fluid.NONE, player));
         Vec3 targetPos = endPos;
         Entity targetEntity = null;
         if (blockHit.getType() != Type.MISS) {
            targetPos = blockHit.getLocation();
         }

         AABB rayBox = new AABB(eyePos, targetPos).inflate(1.0);
         List<Entity> entities = level.getEntities(
            player, rayBox, e -> e instanceof LivingEntity && !e.isSpectator() && !(e instanceof Player p && p.isCreative())
         );
         double closestDist = range * range;

         for (Entity e : entities) {
            AABB entityBox = e.getBoundingBox().inflate(0.5);
            Optional<Vec3> hit = entityBox.clip(eyePos, endPos);
            if (hit.isPresent()) {
               double dist = eyePos.distanceToSqr(hit.get());
               if (dist < closestDist) {
                  closestDist = dist;
                  targetEntity = e;
                  targetPos = hit.get();
               }
            }
         }

         Vec3 center = targetPos;
         double radius = 4.0;
         if (targetEntity != null) {
            center = targetEntity.position().add(0.0, targetEntity.getBbHeight() * 0.5, 0.0);
            radius = Math.max(3.0, targetEntity.getBbWidth() * 2.0 + 2.0);
         }

         RandomSource random = player.getRandom();
         int swordCount = getSwordCount(vars.proficiency_unlimited_blade_works, 12);

         for (int i = 0; i < swordCount; i++) {
            double phi = random.nextDouble() * 2.0 * Math.PI;
            double u = random.nextDouble() * 2.0 - 1.0;
            double theta = Math.acos(u);
            double x = radius * Math.sin(theta) * Math.cos(phi);
            double y = radius * Math.sin(theta) * Math.sin(phi);
            double z = radius * Math.cos(theta);
            Vec3 spawnOffset = new Vec3(x, y, z);
            Vec3 spawnPos = center.add(spawnOffset);
            BlockPos blockPos = BlockPos.containing(spawnPos);
            if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
               boolean found = false;

               for (int tryCount = 0; tryCount < 3; tryCount++) {
                  phi = random.nextDouble() * 2.0 * Math.PI;
                  u = random.nextDouble() * 2.0 - 1.0;
                  theta = Math.acos(u);
                  x = radius * Math.sin(theta) * Math.cos(phi);
                  y = radius * Math.sin(theta) * Math.sin(phi);
                  z = radius * Math.cos(theta);
                  spawnPos = center.add(x, y, z);
                  blockPos = BlockPos.containing(spawnPos);
                  if (level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
                     found = true;
                     break;
                  }
               }

               if (!found) {
                  continue;
               }
            }

            ItemStack weapon = weapons.get(random.nextInt(weapons.size())).copy();
            SwordBarrelProjectileEntity projectile = new SwordBarrelProjectileEntity(level, player, weapon);
            projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            if (player.getPersistentData().getBoolean("UBWBrokenPhantasmEnabled")) {
               projectile.setBrokenPhantasm(true);
            }

            if (targetEntity != null) {
               projectile.setTargetEntity(targetEntity.getId());
            }

            projectile.setHover(20, center);
            projectile.setOwner(player);
            projectile.setMode1Tracking(true);
            Vec3 dir = center.subtract(spawnPos).normalize();
            projectile.setXRot((float)Math.toDegrees(Math.asin(-dir.y)));
            projectile.setXRot((float)Math.toDegrees(Math.asin(dir.y)));
            projectile.setYRot((float)Math.toDegrees(Math.atan2(dir.x, dir.z)));
            level.addFreshEntity(projectile);
            if (level instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y, spawnPos.z, 3, 0.1, 0.1, 0.1, 0.05);
            }
         }

         level.playSound(null, center.x, center.y, center.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 0.5F, 1.5F);
      }
   }

   private static int getSwordCount(double proficiency, int maxCount) {
      double ratio = Math.min(Math.max(proficiency, 0.0), 100.0) / 100.0;
      return 5 + (int)((maxCount - 5) * ratio);
   }
}
