package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.Config;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TerrainDebrisMessage;

/** Visual-only material shards. They never become entities and can never place blocks. */
@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT)
public final class TerrainDebrisClient {
   private static final int MAX_ACTIVE = 512;
   private static final List<Shard> SHARDS = new ArrayList<>();

   private TerrainDebrisClient() { }

   public static void spawn(TerrainDebrisMessage message) {
      int qualityCap = switch (Mth.clamp(Config.terrainDebrisQuality, 0, 2)) { case 0 -> 8; case 2 -> 48; default -> 24; };
      int count = Math.min(qualityCap, message.samples().size());
      RandomSource random = RandomSource.create(message.seed());
      for (int i = 0; i < count; i++) {
         TerrainDebrisMessage.Sample sample = message.samples().get(i);
         BlockState state = Block.stateById(sample.stateId());
         Vec3 pos = Vec3.atCenterOf(sample.pos());
         Vec3 outward = pos.subtract(message.center());
         outward = outward.lengthSqr() < 1.0E-4 ? new Vec3(random.nextGaussian(), 0.2, random.nextGaussian()) : outward.normalize();
         Vec3 velocity = outward.scale(0.08 + random.nextDouble() * 0.17)
            .add((random.nextDouble() - 0.5) * 0.12, 0.20 + random.nextDouble() * 0.28, (random.nextDouble() - 0.5) * 0.12);
         SHARDS.add(new Shard(state, pos, velocity, 28 + random.nextInt(18), random.nextFloat() * 360.0F,
            8.0F + random.nextFloat() * 18.0F, 11.0F + random.nextFloat() * 22.0F));
      }
      if (SHARDS.size() > MAX_ACTIVE) SHARDS.subList(0, SHARDS.size() - MAX_ACTIVE).clear();
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || minecraft.isPaused()) return;
      Iterator<Shard> iterator = SHARDS.iterator();
      while (iterator.hasNext()) {
         Shard shard = iterator.next();
         if (--shard.life <= 0) { iterator.remove(); continue; }
         Vec3 next = shard.pos.add(shard.velocity);
         var landingPos = net.minecraft.core.BlockPos.containing(next.x, next.y - 0.12, next.z);
         boolean collision = shard.velocity.y < 0.0
            && !minecraft.level.getBlockState(landingPos).getCollisionShape(minecraft.level, landingPos).isEmpty();
         if (collision) {
            if (!shard.bounced) {
               shard.bounced = true;
               shard.velocity = new Vec3(shard.velocity.x * 0.62, -shard.velocity.y * 0.34, shard.velocity.z * 0.62);
            } else {
               shard.velocity = new Vec3(shard.velocity.x * 0.7, 0.0, shard.velocity.z * 0.7);
            }
         } else {
            shard.pos = next;
            shard.velocity = shard.velocity.add(0.0, -0.045, 0.0).scale(0.985);
         }
         shard.rotationX += shard.spinX;
         shard.rotationY += shard.spinY;
      }
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || SHARDS.isEmpty()) return;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) return;
      Vec3 camera = event.getCamera().getPosition();
      PoseStack poseStack = event.getPoseStack();
      BufferSource buffers = minecraft.renderBuffers().bufferSource();
      poseStack.pushPose();
      poseStack.translate(-camera.x, -camera.y, -camera.z);
      for (Shard shard : SHARDS) {
         float fade = Mth.clamp(shard.life / 10.0F, 0.0F, 1.0F);
         float scale = 0.18F * fade;
         poseStack.pushPose();
         poseStack.translate(shard.pos.x, shard.pos.y, shard.pos.z);
         poseStack.mulPose(Axis.XP.rotationDegrees(shard.rotationX));
         poseStack.mulPose(Axis.YP.rotationDegrees(shard.rotationY));
         poseStack.scale(scale, scale, scale);
         poseStack.translate(-0.5F, -0.5F, -0.5F);
         minecraft.getBlockRenderer().renderSingleBlock(shard.state, poseStack, buffers,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
         poseStack.popPose();
      }
      poseStack.popPose();
      buffers.endBatch();
   }

   private static final class Shard {
      final BlockState state;
      Vec3 pos, velocity;
      int life;
      float rotationX, rotationY;
      final float spinX, spinY;
      boolean bounced;

      Shard(BlockState state, Vec3 pos, Vec3 velocity, int life, float rotation, float spinX, float spinY) {
         this.state = state;
         this.pos = pos;
         this.velocity = velocity;
         this.life = life;
         this.rotationX = rotation;
         this.rotationY = rotation * 0.63F;
         this.spinX = spinX;
         this.spinY = spinY;
      }
   }
}
