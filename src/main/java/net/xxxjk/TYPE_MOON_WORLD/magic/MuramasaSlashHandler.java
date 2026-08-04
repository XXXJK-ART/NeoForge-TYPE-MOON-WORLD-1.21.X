package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class MuramasaSlashHandler {
   private static final List<MuramasaSlashHandler.SlashInstance> ACTIVE_SLASHES = new ArrayList<>();

   public static void initiate(ServerLevel level, ServerPlayer player, int charge, int maxDist, int maxWidth, int maxHeight) {
      initiate(level, (LivingEntity)player, charge, maxDist, maxWidth, maxHeight);
   }

   public static void initiate(ServerLevel level, LivingEntity owner, int charge, int maxDist, int maxWidth, int maxHeight) {
      if (charge > 0 && owner != null) {
         Vec3 look = owner.getLookAngle();
         ACTIVE_SLASHES.add(
            new MuramasaSlashHandler.SlashInstance(
               owner.getUUID(), level.dimension(), owner.position().add(0.0, owner.getEyeHeight() * 0.5, 0.0),
               look, charge, maxDist, maxWidth, maxHeight, false
            )
         );
      }
   }

   public static void initiateTsumukari(ServerLevel level, LivingEntity owner, int charge, int maxDist, int maxWidth, int maxHeight) {
      if (charge > 0 && owner != null) {
         Vec3 look = owner.getLookAngle();
         ACTIVE_SLASHES.add(
            new MuramasaSlashHandler.SlashInstance(
               owner.getUUID(), level.dimension(), owner.position().add(0.0, owner.getEyeHeight() * 0.5, 0.0),
               look, charge, maxDist, maxWidth, maxHeight, true
            )
         );
      }
   }

   /** Fixed-size ground cross used by Gilgamesh's Igalima/Sulsagana manifestation. */
   public static void initiateGilgameshCross(ServerLevel level, LivingEntity owner, Vec3 forward, boolean burnAfter) {
      // Keep the X centered on the caster while using a narrow 25-degree half-angle.
      // This leaves the two arms broad left/right and avoids the old 45-degree diamond.
      final double crossHalfAngle = Math.toRadians(25.0);
      Vec3 flat = new Vec3(forward.x, 0.0, forward.z);
      if (flat.lengthSqr() < 1.0E-6) flat = new Vec3(0, 0, 1);
      flat = flat.normalize();
      Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
      Vec3 diagonal = flat.add(right.scale((burnAfter ? -1.0 : 1.0) * Math.tan(crossHalfAngle))).normalize();
      // The cut is deliberately released 30 blocks below the caster's feet.
      double releaseY = Math.max(level.getMinBuildHeight() + 1.0, owner.getY() - 30.0);
      Vec3 center = new Vec3(owner.getX(), releaseY, owner.getZ());
      Vec3 start = center.subtract(diagonal.scale(200.0));
      if (burnAfter) {
         TYPE_MOON_WORLD.queueServerWork(20, () -> ACTIVE_SLASHES.add(new SlashInstance(owner.getUUID(), level.dimension(), start, diagonal,
            100, 400, 20, 150, true, true, null)));
      }
      ACTIVE_SLASHES.add(new SlashInstance(owner.getUUID(), level.dimension(), start, diagonal,
         100, 400, 20, 150, true, false, null));
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getLevel() instanceof Level level) {
            ResourceKey<Level> dim = level.dimension();
            ACTIVE_SLASHES.removeIf(slash -> slash.dimension.equals(dim));
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(Post event) {
      if (!event.getLevel().isClientSide) {
         ServerLevel level = (ServerLevel)event.getLevel();
         ResourceKey<Level> dim = level.dimension();
         Iterator<MuramasaSlashHandler.SlashInstance> it = ACTIVE_SLASHES.iterator();

         while (it.hasNext()) {
            MuramasaSlashHandler.SlashInstance slash = it.next();
            if (slash.dimension.equals(dim)) {
               double speed = 2.0;
               double prevDist = slash.currentDistance;
               slash.currentDistance += speed;
               if (slash.currentDistance > slash.maxDistance) {
                  slash.currentDistance = slash.maxDistance;
               }

               processSegment(level, slash, prevDist, slash.currentDistance);
               if (slash.currentDistance >= slash.maxDistance) {
                  if (slash.completion != null) slash.completion.run();
                  it.remove();
               }
            }
         }
      }
   }

   private static void processSegment(ServerLevel level, MuramasaSlashHandler.SlashInstance slash, double startDist, double endDist) {
      if (slash.burnPass) {
         processBurnSegment(level, slash, startDist, endDist);
         return;
      }
      for (double d = startDist; d < endDist; d += 0.5) {
         Vec3 center = slash.startPos.add(slash.direction.scale(d));
         double widthFactor = slashWidthFactor(d / slash.maxDistance);

         int currentWidth = Math.max(1, (int)(slash.width * widthFactor));
         double currentWidthDouble = Math.max(0.5, slash.width * widthFactor);

         for (int w = -currentWidth / 2; w <= currentWidth / 2; w++) {
            Vec3 wOffset = slash.right.scale(w);

            for (int h = -1; h < slash.height; h++) {
               Vec3 posVec = center.add(wOffset).add(0.0, h, 0.0);
               BlockPos pos = BlockPos.containing(posVec);
               if (isCasterSafetyColumn(slash, pos)) continue;
               if (!level.hasChunkAt(pos)) continue;
               BlockState state = level.getBlockState(pos);
               boolean isFluid = !level.getFluidState(pos).isEmpty();
               float hardness = state.getDestroySpeed(level, pos);
               boolean isBreakable = hardness >= 0.0F;
               boolean canBreak;
               if (slash.charge > 60) {
                  canBreak = isBreakable && !state.is(Blocks.BEDROCK);
               } else {
                  canBreak = isBreakable && hardness < 50.0F;
               }

               if (!state.isAir() && canBreak || isFluid) {
                  level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                  if (level.random.nextInt(10) == 0) {
                     level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                  }
               }
            }
         }

         AABB box = new AABB(center.x, center.y, center.z, center.x, center.y, center.z)
            .inflate(currentWidthDouble, 0.0, currentWidthDouble)
            .expandTowards(0.0, slash.height, 0.0)
            .expandTowards(0.0, -1.0, 0.0);

         for (Entity e : level.getEntities(null, box)) {
            if (e instanceof LivingEntity living && !e.getUUID().equals(slash.playerUUID) && !EntityUtils.isImmunePlayerTarget(e)) {
                  float damage = slash.causalSeverance
                     ? Float.MAX_VALUE
                     : slash.fixedGeometry ? 1500.0F : 20.0F + slash.charge * 5.0F;
                  Entity attackerEntity = level.getEntity(slash.playerUUID);
                  if (attackerEntity instanceof LivingEntity attacker) {
                  if (slash.causalSeverance) {
                     markCausalSeverance(living);
                     level.sendParticles(ParticleTypes.REVERSE_PORTAL, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(), 30, 0.5, 0.5, 0.5, 0.3);
                     level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1.0, living.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
                  } else if (!slash.fixedGeometry && slash.charge >= 100
                     && living instanceof net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity servantTarget) {
                     servantTarget.getPersistentData().putBoolean("CausalSevered", true);
                     level.sendParticles(ParticleTypes.REVERSE_PORTAL, living.getX(), living.getY() + living.getBbHeight() / 2, living.getZ(), 30, 0.5, 0.5, 0.5, 0.3);
                     level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1.0, living.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
                  }
                  living.invulnerableTime = 0;
                  DamageSource source = slash.causalSeverance
                     ? level.damageSources().source(MuramasaDamageTypes.TSUMUKARI_MURAMASA, attacker, attacker)
                     : level.damageSources().indirectMagic(attacker, attacker);
                  living.hurt(source, damage);
                  living.invulnerableTime = 0;
                  if (slash.causalSeverance && living.isAlive()) {
                     living.setHealth(0.0F);
                     living.die(source);
                     if (living.isAlive()) {
                        living.die(level.damageSources().genericKill());
                     }
                  }
                  if (attacker instanceof Player player) EntityUtils.triggerSwarmAnger(level, player, living);
               } else {
                  living.invulnerableTime = 0;
                  living.hurt(level.damageSources().magic(), damage);
                  living.invulnerableTime = 0;
               }

               living.igniteForSeconds(5.0F);
            }
         }

         double step = 10.0;

         for (double h = 0.0; h < slash.height; h += step) {
            double currentStepHeight = Math.min(step, slash.height - h);
            double chunkY = center.y + h + currentStepHeight / 2.0;
            level.sendParticles(
               ParticleTypes.EXPLOSION_EMITTER, center.x, chunkY, center.z, 1, currentWidthDouble / 2.0, currentStepHeight / 2.0, currentWidthDouble / 2.0, 0.0
            );
            level.sendParticles(
               ParticleTypes.FLAME, center.x, chunkY, center.z, 20, currentWidthDouble / 2.0, currentStepHeight / 2.0, currentWidthDouble / 2.0, 0.1
            );
            level.sendParticles(
               ParticleTypes.LAVA, center.x, chunkY, center.z, 3, currentWidthDouble / 2.0, currentStepHeight / 2.0, currentWidthDouble / 2.0, 0.0
            );
            level.sendParticles(
               ParticleTypes.LARGE_SMOKE, center.x, chunkY, center.z, 2, currentWidthDouble / 2.0, currentStepHeight / 2.0, currentWidthDouble / 2.0, 0.05
            );
         }
      }
   }

   private static void markCausalSeverance(LivingEntity target) {
      CompoundTag data = target.getPersistentData();
      data.putBoolean("CausalSevered", true);
      data.putBoolean("GodHandActive", false);
      data.putInt("GodHandLives", 0);
      data.putBoolean("BattleContinuationActive", false);
      data.remove("BattleContinuationRecoveryActive");
      data.remove("BattleContinuationLastHealTick");
      data.remove("GawainGutsReady");
      data.remove("ServantCardGawainBeltReady");
      data.remove("GodHandReviveLockUntil");
      data.remove("GodHandHighDamageReviveUntil");
      if (target instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars =
            player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         vars.servant_card_death_release = false;
         vars.master_revive_available = false;
         vars.syncPlayerVariables(player);
      }
   }

   private static void processBurnSegment(ServerLevel level, SlashInstance slash, double startDist, double endDist) {
      for (double d = startDist; d < endDist; d += 0.5) {
         Vec3 center = slash.startPos.add(slash.direction.scale(d));
         double widthFactor = slashWidthFactor(d / slash.maxDistance);
         int halfWidth = Math.max(1, (int)Math.ceil(slash.width * widthFactor * 0.5));
         long distanceSeed = mix64(Double.doubleToLongBits(Math.floor(d * 2.0)) ^ slash.variationSeed);
         int shell = 2 + (int)((distanceSeed >>> 8) & 3L);
         double wave = Math.sin(d * 0.075 + noise01(distanceSeed) * Math.PI * 2.0) * 1.5
            + Math.sin(d * 0.021 + noise01(distanceSeed ^ 0x6A09E667F3BCC909L) * Math.PI * 2.0) * 1.0;
         int lower = -1 - shell - Math.max(0, (int)Math.round(wave));
         int upper = slash.height + shell + Math.max(0, (int)Math.round(-wave));
         for (int w = -halfWidth - shell; w <= halfWidth + shell; w++) {
            for (int h = lower; h < upper; h++) {
               boolean insideCut = Math.abs(w) <= halfWidth && h >= -1 && h < slash.height;
               if (insideCut) continue;
               BlockPos pos = BlockPos.containing(center.add(slash.right.scale(w)).add(0.0, h, 0.0));
               if (isCasterSafetyColumn(slash, pos)) continue;
               if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) continue;
               BlockState state = level.getBlockState(pos);
               float hardness = state.getDestroySpeed(level, pos);
               if (state.isAir() || state.is(Blocks.BEDROCK) || hardness < 0.0F) continue;
               long hash = mix64(pos.asLong() ^ slash.variationSeed ^ distanceSeed);
               BlockState replacement = ((hash >>> 16) & 0xFFL) < 30L
                  ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
               level.setBlock(pos, replacement, 3);
               if ((hash & 63L) == 0L) {
                  level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                     1, 0.0, 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   /**
    * Leaves the caster's release point supported while the 150-block cut passes below it.
    * The protected column is intentionally small, so it reads as a central safe footing
    * instead of interrupting either arm of the X farther out.
    */
   private static boolean isCasterSafetyColumn(SlashInstance slash, BlockPos pos) {
      if (!slash.fixedGeometry) return false;
      Vec3 releaseCenter = slash.startPos.add(slash.direction.scale(slash.maxDistance * 0.5));
      double dx = pos.getX() + 0.5 - releaseCenter.x;
      double dz = pos.getZ() + 0.5 - releaseCenter.z;
      if (dx * dx + dz * dz > 2.25) return false;
      return pos.getY() >= Math.floor(releaseCenter.y - 1.0)
         && pos.getY() <= Math.floor(releaseCenter.y + 56.0);
   }

   /** Width tapers at both ends so the slash grows in, holds, then fades out. */
   private static double slashWidthFactor(double progress) {
      double edge = Math.min(Math.max(progress, 0.0), 1.0);
      edge = Math.min(edge, 1.0 - edge);
      double ramp = Math.max(0.0, Math.min(1.0, edge / 0.18));
      ramp = ramp * ramp * (3.0 - 2.0 * ramp);
      return 0.16 + 0.84 * ramp;
   }

   private static long mix64(long value) {
      value ^= value >>> 30;
      value *= 0xBF58476D1CE4E5B9L;
      value ^= value >>> 27;
      value *= 0x94D049BB133111EBL;
      return value ^ (value >>> 31);
   }

   private static double noise01(long value) {
      return (mix64(value) >>> 11) * 0x1.0p-53;
   }

   private static class SlashInstance {
      final UUID playerUUID;
      final ResourceKey<Level> dimension;
      final Vec3 startPos;
      final Vec3 direction;
      final Vec3 right;
      final int charge;
      final int maxDistance;
      final int width;
      final int height;
      final boolean fixedGeometry;
      final boolean burnPass;
      final boolean causalSeverance;
      final Runnable completion;
      final long variationSeed;
      double currentDistance = 0.0;

      SlashInstance(
         UUID playerUUID, ResourceKey<Level> dimension, Vec3 startPos, Vec3 direction,
         int charge, int maxDistLimit, int maxWidthLimit, int maxHeightLimit, boolean causalSeverance
      ) {
         this.playerUUID = playerUUID;
         this.dimension = dimension;
         this.startPos = startPos;
         this.direction = direction.normalize();
         this.right = new Vec3(-direction.z, 0.0, direction.x).normalize();
         this.charge = charge;
         this.maxDistance = Math.max(20, Math.min(maxDistLimit, (int)(charge * (maxDistLimit / 100.0))));
         this.width = Math.min(maxWidthLimit, 1 + charge / 10);
         this.height = Math.min(maxHeightLimit, Math.max(5, charge));
         this.fixedGeometry = false;
         this.burnPass = false;
         this.causalSeverance = causalSeverance;
         this.completion = null;
         this.variationSeed = mix64(Double.doubleToLongBits(startPos.x)
            ^ Double.doubleToLongBits(startPos.y) ^ Double.doubleToLongBits(startPos.z));
      }

      SlashInstance(UUID playerUUID, ResourceKey<Level> dimension, Vec3 startPos, Vec3 direction,
                    int charge, int maxDistance, int width, int height, boolean fixedGeometry,
                    boolean burnPass, Runnable completion) {
         this.playerUUID = playerUUID;
         this.dimension = dimension;
         this.startPos = startPos;
         this.direction = direction.normalize();
         this.right = new Vec3(-direction.z, 0.0, direction.x).normalize();
         this.charge = charge;
         this.maxDistance = maxDistance;
         this.width = width;
         this.height = height;
         this.fixedGeometry = fixedGeometry;
         this.burnPass = burnPass;
         this.causalSeverance = false;
         this.completion = completion;
         this.variationSeed = mix64(Double.doubleToLongBits(startPos.x)
            ^ Double.doubleToLongBits(startPos.y) ^ Double.doubleToLongBits(startPos.z)
            ^ Double.doubleToLongBits(this.direction.x) ^ Double.doubleToLongBits(this.direction.z));
      }
   }
}
