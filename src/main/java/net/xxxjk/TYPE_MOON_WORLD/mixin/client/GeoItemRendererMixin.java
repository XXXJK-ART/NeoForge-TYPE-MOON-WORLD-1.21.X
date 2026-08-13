package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ReinforcementRenderType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * GeckoLib item renderers do not use the vanilla foil RenderType for their
 * model pass. Render the custom magic texture as a second model pass so
 * third-person observers see the same texture as the holder.
 */
@Mixin(value = GeoItemRenderer.class, remap = false)
public abstract class GeoItemRendererMixin {
   @Shadow @Nullable protected ItemStack currentItemStack;
   @Shadow @Nullable protected ItemDisplayContext renderPerspective;

   @ModifyArg(
      method = "renderByItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
         remap = true
      ),
      index = 3,
      remap = false
   )
   private boolean typemoonworld$disableVanillaGlintForThirdPerson(boolean withGlint) {
      return isThirdPerson() && hasMagicTexture(this.currentItemStack) ? false : withGlint;
   }

   @Inject(method = "actuallyRender", at = @At("RETURN"), remap = false)
   private void typemoonworld$renderMagicGlint(
      PoseStack poseStack,
      Item animatable,
      BakedGeoModel model,
      @Nullable RenderType renderType,
      MultiBufferSource bufferSource,
      @Nullable VertexConsumer buffer,
      boolean isReRender,
      float partialTick,
      int packedLight,
      int packedOverlay,
      int colour,
      CallbackInfo ci
   ) {
      if (isReRender || !isThirdPerson() || !hasMagicTexture(this.currentItemStack)) {
         return;
      }

      RenderType glintType = getMagicGlintType(this.currentItemStack);
      @SuppressWarnings("unchecked")
      GeoRenderer<GeoAnimatable> renderer = (GeoRenderer<GeoAnimatable>)(Object)this;
      GeoAnimatable geoAnimatable = (GeoAnimatable)(Object)animatable;
      renderer.reRender(model, poseStack, bufferSource, geoAnimatable, glintType,
         bufferSource.getBuffer(glintType), partialTick, packedLight, packedOverlay, colour);
   }

   private boolean isThirdPerson() {
      return this.renderPerspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
         || this.renderPerspective == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }

   private static boolean hasMagicTexture(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return false;
      }
      var tag = data.copyTag();
      return tag.getBoolean("KnightOfOwner") || tag.getBoolean("is_projected")
         || tag.getBoolean("is_infinite_projection") || tag.getBoolean("Reinforced")
         || tag.contains("ReinforcedLevel") || tag.contains("ReinforcedEnchantment")
         || tag.getBoolean("ReinforcementTemporary");
   }

   private static RenderType getMagicGlintType(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      boolean knightOfOwner = data != null && data.copyTag().getBoolean("KnightOfOwner");
      return knightOfOwner
         ? ReinforcementRenderType.knightOfOwnerEntityGlintDirect3d()
         : ReinforcementRenderType.entityGlintDirect3d();
   }
}
