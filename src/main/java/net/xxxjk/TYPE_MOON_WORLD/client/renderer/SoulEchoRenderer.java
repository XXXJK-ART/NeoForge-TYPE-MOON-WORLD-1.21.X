package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public final class SoulEchoRenderer extends EntityRenderer<SoulEchoEntity> {
   private final Map<Integer, Entity> proxies = new HashMap<>();

   public SoulEchoRenderer(EntityRendererProvider.Context context) {
      super(context);
      this.shadowRadius = 0.45F;
   }

   @Override
   public void render(SoulEchoEntity echo, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
      Entity proxy = this.proxies.computeIfAbsent(echo.getId(), ignored -> createProxy(echo));
      if (proxy == null) {
         return;
      }
      proxy.setYRot(echo.getYRot());
      proxy.setXRot(echo.getXRot());
      proxy.yRotO = echo.yRotO;
      proxy.xRotO = echo.xRotO;
      if (proxy instanceof net.minecraft.world.entity.LivingEntity living) {
         living.yBodyRot = echo.yBodyRot;
         living.yBodyRotO = echo.yBodyRotO;
         living.yHeadRot = echo.yHeadRot;
         living.yHeadRotO = echo.yHeadRotO;
         living.walkAnimation.setSpeed(echo.walkAnimation.speed());
      }
      RenderSystem.setShaderColor(0.015F, 0.015F, 0.018F, 1.0F);
      Minecraft.getInstance().getEntityRenderDispatcher().render(proxy, 0.0, 0.0, 0.0, yaw, partialTick, poseStack, buffers, light);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static Entity createProxy(SoulEchoEntity echo) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         return null;
      }
      ResourceLocation id = ResourceLocation.tryParse(echo.getSourceType());
      if (echo.getSoulKind() != net.xxxjk.TYPE_MOON_WORLD.servant.palerider.SoulSnapshot.SoulKind.CREATURE
         && minecraft.level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
         String name = echo.getPlayerProfile().isBlank() ? (echo.getCustomName() == null ? "Pale Soul" : echo.getCustomName().getString()) : echo.getPlayerProfile();
         UUID profileId;
         try {
            profileId = UUID.fromString(echo.getPlayerUuid());
         } catch (IllegalArgumentException ignored) {
            profileId = UUID.nameUUIDFromBytes(("pale_rider:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
         }
         return new net.minecraft.client.player.RemotePlayer(clientLevel, new GameProfile(profileId, name));
      }
      EntityType<?> type = id == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
      Entity proxy = type == null || type == echo.getType() ? null : type.create(minecraft.level);
      if (proxy == null) {
         proxy = EntityType.ZOMBIE.create(minecraft.level);
      }
      return proxy;
   }

   @Override
   public ResourceLocation getTextureLocation(SoulEchoEntity entity) {
      return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
   }
}
