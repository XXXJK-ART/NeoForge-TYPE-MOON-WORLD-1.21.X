package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.VFXTriggerEntity;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXClientRuntime;

public class VFXTriggerRenderer extends EntityRenderer<VFXTriggerEntity> {
   private static final Set<Integer> SPAWNED = new HashSet<>();
   private static final int TARGET_BIND_RETRY_TICKS = 20;

   public VFXTriggerRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(VFXTriggerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (!SPAWNED.contains(entity.getId()) && canSpawn(entity)) {
         SPAWNED.add(entity.getId());
         VFXClientRuntime.spawn(entity.getEffectId(), entity.getX(), entity.getY(), entity.getZ(), entity.getTargetId(), entity.getSeed());
      }
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   private static boolean canSpawn(VFXTriggerEntity entity) {
      int targetId = entity.getTargetId();
      if (targetId < 0) {
         return true;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null && minecraft.level.getEntity(targetId) != null) {
         return true;
      }
      return entity.tickCount >= TARGET_BIND_RETRY_TICKS;
   }

   @Override
   public ResourceLocation getTextureLocation(VFXTriggerEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
