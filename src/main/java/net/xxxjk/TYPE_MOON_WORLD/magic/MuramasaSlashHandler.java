package net.xxxjk.TYPE_MOON_WORLD.magic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class MuramasaSlashHandler {
   private static final List<MuramasaSlashHandler.SlashInstance> ACTIVE_SLASHES = new ArrayList<>();

   public static void initiate(ServerLevel level, ServerPlayer player, int charge, int maxDist, int maxWidth, int maxHeight) {
      if (charge > 0) {
         Vec3 look = player.getLookAngle();
         ACTIVE_SLASHES.add(
            new MuramasaSlashHandler.SlashInstance(
               player.getUUID(), level.dimension(), player.position().add(0.0, player.getEyeHeight() * 0.5, 0.0), look, charge, maxDist, maxWidth, maxHeight
            )
         );
      }
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
                  it.remove();
               }
            }
         }
      }
   }

   private static void processSegment(ServerLevel level, MuramasaSlashHandler.SlashInstance slash, double startDist, double endDist) {
      for (double d = startDist; d < endDist; d += 0.5) {
         Vec3 center = slash.startPos.add(slash.direction.scale(d));
         double progress = d / slash.maxDistance;
         double widthFactor = 1.0;
         if (progress > 0.8) {
            widthFactor = Math.max(0.1, 1.0 - (progress - 0.8) * 4.5);
         }

         int currentWidth = Math.max(1, (int)(slash.width * widthFactor));
         double currentWidthDouble = Math.max(0.5, slash.width * widthFactor);

         for (int w = -currentWidth / 2; w <= currentWidth / 2; w++) {
            Vec3 wOffset = slash.right.scale(w);

            for (int h = -1; h < slash.height; h++) {
               Vec3 posVec = center.add(wOffset).add(0.0, h, 0.0);
               BlockPos pos = BlockPos.containing(posVec);
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
               float damage = 20.0F + slash.charge * 5.0F;
               Player player = level.getPlayerByUUID(slash.playerUUID);
               if (player != null) {
                  living.invulnerableTime = 0;
                  living.hurt(level.damageSources().indirectMagic(player, player), damage);
                  living.invulnerableTime = 0;
                  EntityUtils.triggerSwarmAnger(level, player, living);
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
      double currentDistance = 0.0;

      SlashInstance(
         UUID playerUUID, ResourceKey<Level> dimension, Vec3 startPos, Vec3 direction, int charge, int maxDistLimit, int maxWidthLimit, int maxHeightLimit
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
      }
   }
}
