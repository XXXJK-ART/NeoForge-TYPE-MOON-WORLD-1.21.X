package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.UBWWeaponBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.NoblePhantasmCastService;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;

/** Shared NPC/player forging presentation; only complete() grants the weapon. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class MuramasaNoblePhantasmPresentation {
   private static final String FIELD_ACTIVE = "MuramasaFieldActive";
   private static final String FIELD_RESTORE_TICK = "MuramasaFieldRestoreTick";
   private static final String FIELD_TERRAIN = "MuramasaFieldTerrain";
   private static final String FIELD_CENTER_X = "MuramasaFieldCenterX";
   private static final String FIELD_CENTER_Z = "MuramasaFieldCenterZ";
   private static final String FIELD_COLLAPSING = "MuramasaFieldCollapsing";

   private static final String FIELD_EXPAND_TICKS = "MuramasaFieldExpandTicks";
   private static final String FIELD_SEQUENCE = "MuramasaFieldSequence";
   private static final String FIELD_DIMENSION = "MuramasaFieldDimension";
   private static final int NP_COLLAPSE_TICKS = 80;

   private MuramasaNoblePhantasmPresentation() { }

   @SubscribeEvent
   public static void tick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof LivingEntity entity) || entity.level().isClientSide()) return;
      if (entity instanceof SenkoMuramasaEntity servant && MuramasaCombatHelper.isSummonPending(servant)
         && !NoblePhantasmCastService.chanting(entity)) {
         MuramasaCombatHelper.cancelNoblePhantasmSummon(servant);
         return;
      }
      if (!entity.getPersistentData().getBoolean(FIELD_ACTIVE)) return;
      // Queued work and cast tokens do not survive a server restart.
      if (!NoblePhantasmCastService.chanting(entity)
         || entity.level().getGameTime() >= entity.getPersistentData().getLong(FIELD_RESTORE_TICK)) {
         cancel(entity);
         if (entity instanceof SenkoMuramasaEntity servant) MuramasaCombatHelper.cancelNoblePhantasmSummon(servant);
         else if (entity instanceof ServerPlayer player)
            net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardSenkoMuramasaSkills.cancelNoblePhantasm(player);
      }
   }

   public static void begin(LivingEntity entity, int voiceTicks) {
      cancel(entity);
      if (!(entity.level() instanceof ServerLevel level)) return;
      CompoundTag data = entity.getPersistentData();
      data.putLong(FIELD_SEQUENCE, data.getLong(FIELD_SEQUENCE) + 1L);
      data.putInt(FIELD_EXPAND_TICKS, Math.max(1, voiceTicks - NP_COLLAPSE_TICKS));
      data.putString(FIELD_DIMENSION, level.dimension().location().toString());
      spawnMuramasaFieldEffects(level, entity);
      level.playSound(null, entity.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 0.8F, 0.7F);
      schedule(level, entity, data.getInt(FIELD_EXPAND_TICKS), () -> {
         data.putBoolean(FIELD_COLLAPSING, true);
         shatterMuramasaFieldSwords(level, entity);
         scheduleMuramasaCollapse(level, entity, 0);
         scheduleTsumukariHandEffects(level, entity, 0);
      });
   }

   public static void complete(LivingEntity entity) {
      cancel(entity);
      entity.setItemInHand(InteractionHand.MAIN_HAND, MuramasaCombatHelper.projectedStack(ModItems.TSUMUKARI_MURAMASA.get()));
      if (entity.level() instanceof ServerLevel level)
         level.playSound(null, entity.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 0.75F, 0.55F);
   }

   public static void cancel(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      var dimension = net.minecraft.resources.ResourceLocation.tryParse(entity.getPersistentData().getString(FIELD_DIMENSION));
      ServerLevel originalLevel = dimension == null ? level : level.getServer().getLevel(
         net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
      if (originalLevel != null) restoreFieldTerrain(originalLevel, entity);
      entity.getPersistentData().putBoolean(FIELD_ACTIVE, false);
   }

   private static boolean valid(ServerLevel level, LivingEntity entity) {
      if (!entity.isAlive() || entity.isRemoved() || entity.level() != level
         || NoblePhantasmCastService.cancelled(entity)) return false;
      if (entity instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return !player.hasDisconnected() && vars.servant_card_transformed && "senko_muramasa".equals(vars.servant_card_id);
      }
      return true;
   }

   private static void schedule(ServerLevel level, LivingEntity entity, int delay, Runnable effect) {
      long sequence = entity.getPersistentData().getLong(FIELD_SEQUENCE);
      TYPE_MOON_WORLD.queueServerWork(delay, () -> {
         if (!entity.getPersistentData().getBoolean(FIELD_ACTIVE)
            || entity.getPersistentData().getLong(FIELD_SEQUENCE) != sequence) return;
         if (!valid(level, entity)) { cancel(entity); return; }
         effect.run();
      });
   }
   private static void spawnMuramasaFieldEffects(ServerLevel level, LivingEntity entity) {
      Vec3 origin = entity.position().add(0.0, 0.15, 0.0);

      level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
         origin.x, origin.y + 0.8, origin.z, 2, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         origin.x, origin.y + 0.85, origin.z, 90, 0.9, 0.75, 0.9, 0.08);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         origin.x, origin.y + 0.9, origin.z, 36, 0.75, 0.65, 0.75, 0.05);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
         origin.x, origin.y + 1.0, origin.z, 24, 0.8, 0.9, 0.8, 0.12);

      placeMuramasaField(level, entity);
      scheduleMuramasaBarrier(level, entity, 0);
      VFXServerEffects.screenFlash(level, origin, 48.0, 5, 0.24F);
   }

   private static void placeMuramasaField(ServerLevel level, LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      if (data.getBoolean(FIELD_ACTIVE)) {
         return;
      }

      ListTag terrain = new ListTag();
      data.put(FIELD_TERRAIN, terrain);
      data.putBoolean(FIELD_ACTIVE, true);
      data.putBoolean(FIELD_COLLAPSING, false);
      data.putInt(FIELD_CENTER_X, entity.blockPosition().getX());
      data.putInt(FIELD_CENTER_Z, entity.blockPosition().getZ());
      int restoreDelay = data.getInt(FIELD_EXPAND_TICKS) + NP_COLLAPSE_TICKS + 2;
      data.putLong(FIELD_RESTORE_TICK, level.getGameTime() + restoreDelay);
      schedule(level, entity, 1, () -> expandMuramasaTerrain(level, entity, 0));
   }

   private static void expandMuramasaTerrain(ServerLevel level, LivingEntity entity, int radius) {
      if (!entity.isAlive() || entity.level() != level
         || !entity.getPersistentData().getBoolean(FIELD_ACTIVE)
         || entity.getPersistentData().getBoolean(FIELD_COLLAPSING)) {
         return;
      }
      CompoundTag data = entity.getPersistentData();
      ListTag terrain = data.getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      int centerX = data.getInt(FIELD_CENTER_X);
      int centerZ = data.getInt(FIELD_CENTER_Z);
      int outerSquared = radius * radius;
      int innerSquared = Math.max(0, radius - 1) * Math.max(0, radius - 1);

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            int distanceSquared = dx * dx + dz * dz;
            if (distanceSquared > outerSquared || (radius > 0 && distanceSquared <= innerSquared)) {
               continue;
            }
            int x = centerX + dx;
            int z = centerZ + dz;
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos groundPos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(groundPos) || level.getBlockEntity(groundPos) != null) {
               continue;
            }
            BlockState original = level.getBlockState(groundPos);
            if (!original.is(Blocks.BEDROCK) && original.getDestroySpeed(level, groundPos) >= 0.0F) {
               BlockState replacement = Blocks.RED_SANDSTONE.defaultBlockState();
               if (!original.equals(replacement) && level.setBlock(groundPos, replacement, 3)) {
                  rememberFieldBlock(terrain, groundPos, original, replacement);
               }
            }

            long seed = mix64(groundPos.asLong() ^ entity.getUUID().getMostSignificantBits());
            if ((seed & 15L) != 0L) {
               continue;
            }
            BlockPos swordPos = groundPos.above();
            if (!level.hasChunkAt(swordPos) || level.getBlockEntity(swordPos) != null
               || !level.getBlockState(swordPos).canBeReplaced()) {
               continue;
            }
            Direction facing = Direction.fromYRot((seed & 0xFFFFL) * 360.0 / 65536.0);
            BlockState swordState = ((UBWWeaponBlock)ModBlocks.UBW_WEAPON_BLOCK.get()).defaultBlockState()
               .setValue(UBWWeaponBlock.FACING, facing.getAxis().isHorizontal() ? facing : Direction.NORTH)
               .setValue(UBWWeaponBlock.ROTATION_A, (seed & 1L) != 0L)
               .setValue(UBWWeaponBlock.ROTATION_B, (seed & 2L) != 0L)
               .setValue(UBWWeaponBlock.ROTATION_C, (seed & 4L) != 0L);
            BlockState swordOriginal = level.getBlockState(swordPos);
            if (!level.setBlock(swordPos, swordState, 3)) {
               continue;
            }
            rememberFieldBlock(terrain, swordPos, swordOriginal, swordState);
            if (level.getBlockEntity(swordPos) instanceof UBWWeaponBlockEntity tile) {
               ItemStack sword = MuramasaCombatHelper.projectedStack(Items.IRON_SWORD);
               tile.setStoredItem(sword);
            }
         }
      }

      if (radius < 25) {
         schedule(level, entity, Math.max(1, data.getInt(FIELD_EXPAND_TICKS) / 25), () -> expandMuramasaTerrain(level, entity, radius + 1));
      }
   }

   private static void rememberFieldBlock(ListTag terrain, BlockPos pos, BlockState original, BlockState replacement) {
      CompoundTag entry = new CompoundTag();
      entry.putLong("pos", pos.asLong());
      entry.putInt("original", Block.getId(original));
      entry.putInt("replacement", Block.getId(replacement));
      terrain.add(entry);
   }

   private static void restoreFieldTerrain(ServerLevel level, LivingEntity entity) {
      CompoundTag data = entity.getPersistentData();
      ListTag terrain = data.getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      for (int i = terrain.size() - 1; i >= 0; i--) {
         CompoundTag entry = terrain.getCompound(i);
         BlockPos pos = BlockPos.of(entry.getLong("pos"));
         if (!level.hasChunkAt(pos)) {
            level.getChunkAt(pos);
         }
         BlockState current = level.getBlockState(pos);
         BlockState replacement = Block.stateById(entry.getInt("replacement"));
         if (current.equals(replacement)) {
            level.setBlock(pos, Block.stateById(entry.getInt("original")), 3);
         }
      }
      data.remove(FIELD_TERRAIN);
      data.putBoolean(FIELD_ACTIVE, false);
      data.remove(FIELD_RESTORE_TICK);
      data.remove(FIELD_CENTER_X);
      data.remove(FIELD_CENTER_Z);
      data.remove(FIELD_COLLAPSING);
      data.remove(FIELD_DIMENSION);
      data.remove(FIELD_EXPAND_TICKS);
   }

   private static void scheduleMuramasaBarrier(ServerLevel level, LivingEntity entity, int step) {
      if (step > entity.getPersistentData().getInt(FIELD_EXPAND_TICKS)) {
         return;
      }
      schedule(level, entity, step == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         double radius = 0.5 + 24.5 * step / entity.getPersistentData().getInt(FIELD_EXPAND_TICKS);
         spawnMuramasaBarrierRing(level, entity, radius);
         scheduleMuramasaBarrier(level, entity, step + 4);
      });
   }

   private static void scheduleMuramasaCollapse(ServerLevel level, LivingEntity entity, int tick) {
      if (tick > NP_COLLAPSE_TICKS) {
         return;
      }
      schedule(level, entity, tick == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         double progress = Math.min(1.0, tick / (double)NP_COLLAPSE_TICKS);
         double radius = 25.0 * (1.0 - progress);
         if (radius > 0.25) {
            spawnMuramasaBarrierRing(level, entity, radius);
         }
         scheduleMuramasaCollapse(level, entity, tick + 4);
      });
   }

   private static void shatterMuramasaFieldSwords(ServerLevel level, LivingEntity entity) {
      ListTag terrain = entity.getPersistentData().getList(FIELD_TERRAIN, Tag.TAG_COMPOUND);
      int shattered = 0;
      for (int i = 0; i < terrain.size(); i++) {
         CompoundTag entry = terrain.getCompound(i);
         if (!Block.stateById(entry.getInt("replacement")).is(ModBlocks.UBW_WEAPON_BLOCK.get())) {
            continue;
         }
         BlockPos pos = BlockPos.of(entry.getLong("pos"));
         if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModBlocks.UBW_WEAPON_BLOCK.get())) {
            continue;
         }
         level.setBlock(pos, Block.stateById(entry.getInt("original")), 3);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 16, 0.18, 0.4, 0.18, 0.08);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.32, 0.42, 0.32, 0.1);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 8, 0.22, 0.35, 0.22, 0.12);
         shattered++;
      }
      if (shattered > 0) {
         level.playSound(null, entity.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.55F);
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
            entity.getX(), entity.getY() + 1.0, entity.getZ(), shattered * 3, 8.0, 3.0, 8.0, 0.12);
      }
   }

   private static void scheduleTsumukariHandEffects(ServerLevel level, LivingEntity entity, int tick) {
      if (tick > NP_COLLAPSE_TICKS) {
         return;
      }
      schedule(level, entity, tick == 0 ? 1 : 4, () -> {
         if (!entity.isAlive() || entity.level() != level) {
            return;
         }
         spawnTsumukariHandEffects(level, entity, tick);
         scheduleTsumukariHandEffects(level, entity, tick + 4);
      });
   }

   private static void spawnTsumukariHandEffects(ServerLevel level, LivingEntity entity, int tick) {
      Vec3 hand = entity.position().add(entity.getLookAngle().scale(0.35)).add(0.0, entity.getBbHeight() * 0.68, 0.0);
      int count = 12 + Math.min(24, tick / 3);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
         hand.x, hand.y, hand.z, count, 0.28, 0.3, 0.28, 0.08);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
         hand.x, hand.y, hand.z, count / 2, 0.22, 0.24, 0.22, 0.05);
      level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
         hand.x, hand.y, hand.z, count / 2, 0.2, 0.25, 0.2, 0.1);
      if ((tick & 7) == 0) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
            hand.x, hand.y, hand.z, 32, 0.5, 0.45, 0.5, 0.15);
      }
   }

   private static void spawnMuramasaBarrierRing(ServerLevel level, LivingEntity entity, double radius) {
      double y = entity.getY() + 0.08;
      int points = Math.max(24, Math.min(72, (int)(radius * 3.0)));
      for (int i = 0; i < points; i++) {
         double angle = Math.PI * 2.0 * i / points;
         double x = entity.getX() + Math.cos(angle) * radius;
         double z = entity.getZ() + Math.sin(angle) * radius;
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
            x, y, z, 1, 0.0, 0.12, 0.0, 0.025);
         if ((i & 3) == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
               x, y + 0.12, z, 1, 0.0, 0.08, 0.0, 0.018);
         }
         if ((i & 7) == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
               x, y + 0.45, z, 1, 0.0, 0.1, 0.0, 0.04);
         }
      }
      spawnMuramasaHemisphereShell(level, entity, radius);
   }

   private static void spawnMuramasaHemisphereShell(ServerLevel level, LivingEntity entity, double radius) {
      if (radius < 1.0) {
         return;
      }
      int latitudeBands = Math.max(3, Math.min(6, (int)(radius / 4.0)));
      int longitudePoints = Math.max(24, Math.min(72, (int)(radius * 2.5)));
      for (int latitude = 1; latitude <= latitudeBands; latitude++) {
         double polar = (Math.PI * 0.5) * latitude / latitudeBands;
         double horizontalRadius = Math.sin(polar) * radius;
         double height = Math.cos(polar) * radius;
         for (int longitude = 0; longitude < longitudePoints; longitude++) {
            double angle = Math.PI * 2.0 * longitude / longitudePoints;
            double x = entity.getX() + Math.cos(angle) * horizontalRadius;
            double z = entity.getZ() + Math.sin(angle) * horizontalRadius;
            double y = entity.getY() + 0.1 + height;
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
               x, y, z, 1, 0.0, 0.03, 0.0, 0.015);
            if ((longitude + latitude) % 3 == 0) {
               level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                  x, y, z, 1, 0.0, 0.04, 0.0, 0.025);
            }
         }
      }
   }

   private static long mix64(long value) {
      value ^= value >>> 30;
      value *= 0xBF58476D1CE4E5B9L;
      value ^= value >>> 27;
      value *= 0x94D049BB133111EBL;
      return value ^ (value >>> 31);
   }

}
