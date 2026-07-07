package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.xxxjk.TYPE_MOON_WORLD.client.CommandSpellVisualClient;

public class CommandSpellMarkLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public CommandSpellMarkLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
      super(renderer);
   }

   @Override
   public void render(
      PoseStack poseStack,
      MultiBufferSource buffer,
      int packedLight,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (CommandSpellVisualClient.shouldRenderMark(player)) {
         CommandSpellMarkRenderer.renderRightHandMark(this.getParentModel(), player, poseStack, buffer);
      }
   }
}
