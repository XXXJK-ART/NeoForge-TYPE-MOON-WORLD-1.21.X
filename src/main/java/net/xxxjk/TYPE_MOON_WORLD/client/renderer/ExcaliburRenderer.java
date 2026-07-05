package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ExcaliburRenderer extends GeoItemRenderer<ExcaliburItem> {
   public ExcaliburRenderer() {
      super(new ExcaliburModel());
   }

   @Override
   public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
      if (shouldHideWindVeiledExcalibur(stack, displayContext)) {
         return;
      }
      super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
   }

   private static boolean shouldHideWindVeiledExcalibur(ItemStack stack, ItemDisplayContext displayContext) {
      if (!isHandDisplay(displayContext)) {
         return false;
      }
      CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
      if (customData == null || !customData.copyTag().getBoolean("ServantCardArtoriaWindVeiled")) {
         return false;
      }
      if (Minecraft.getInstance().player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = Minecraft.getInstance().player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed
         && "artoria_pendragon".equals(vars.servant_card_id)
         && !ArtoriaPendragonCombatHelper.isInvisibleAirRevealed(Minecraft.getInstance().player);
   }

   private static boolean isHandDisplay(ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }
}
