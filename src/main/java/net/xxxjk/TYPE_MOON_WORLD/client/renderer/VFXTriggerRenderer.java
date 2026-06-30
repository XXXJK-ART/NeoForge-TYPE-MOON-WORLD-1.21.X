package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.xxxjk.TYPE_MOON_WORLD.entity.VFXTriggerEntity;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXClientRuntime;

public class VFXTriggerRenderer extends EntityRenderer<VFXTriggerEntity> {
   private static final Set<Integer> SPAWNED = new HashSet<>();

   public VFXTriggerRenderer(Context context) {
      super(context);
   }

   @Override
   public void render(VFXTriggerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (SPAWNED.add(entity.getId())) {
         VFXClientRuntime.spawn(entity.getEffectId(), entity.getX(), entity.getY(), entity.getZ(), entity.getTargetId(), entity.getSeed());
      }
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   @Override
   public ResourceLocation getTextureLocation(VFXTriggerEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }
}
