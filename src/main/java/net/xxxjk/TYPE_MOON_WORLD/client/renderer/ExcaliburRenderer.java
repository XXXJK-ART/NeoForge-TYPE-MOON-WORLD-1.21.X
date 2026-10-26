package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.client.model.ExcaliburModel;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;

/**
 * Renders the same Excalibur model in every item perspective.
 *
 * <p>The blade state selects a dedicated texture while reusing the authored
 * GeckoLib model, keeping first-person, third-person, GUI, and planted sword
 * proportions in sync.</p>
 */
public final class ExcaliburRenderer extends GeoItemRenderer<ExcaliburItem> {
   private static final ResourceLocation MANA_BURST_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "typemoonworld", "textures/item/excalibur_mana_burst.png");
   /** Invisible Air leaves only a faint cyan outline over the authored model. */
   private static final Color WIND_VEIL_COLOR = Color.ofRGBA(82, 190, 236, 13);
   /** The dedicated Mana Burst texture already contains the gold; keep its highlights white-hot. */
   private static final Color MANA_BURST_COLOR = Color.ofRGBA(255, 255, 255, 255);

   public ExcaliburRenderer() {
      super(new ExcaliburModel());
   }

   @Override
   public RenderType getRenderType(ExcaliburItem animatable, ResourceLocation texture,
                                  @Nullable MultiBufferSource bufferSource, float partialTick) {
      return RenderType.entityTranslucentEmissive(texture);
   }

   @Override
   public ResourceLocation getTextureLocation(ExcaliburItem animatable) {
      ItemStack stack = getCurrentItemStack();
      return isWindVeiledBlade(stack) ? super.getTextureLocation(animatable)
         : isManaBurstBlade(stack) ? MANA_BURST_TEXTURE : super.getTextureLocation(animatable);
   }

   @Override
   public Color getRenderColor(ExcaliburItem animatable, float partialTick, int packedLight) {
      ItemStack stack = getCurrentItemStack();
      if (isWindVeiledBlade(stack)) {
         return WIND_VEIL_COLOR;
      }
      if (isManaBurstBlade(stack)) {
         return MANA_BURST_COLOR;
      }
      return Color.WHITE;
   }

   @Override
   public void renderByItem(ItemStack stack, ItemDisplayContext displayContext,
                            PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
      // The held model disappears while the defensive sword is planted in the ground.
      if (shouldHidePlantedWindDefenseSword(stack, displayContext)) {
         return;
      }
      super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
   }

   private static boolean isManaBurstBlade(ItemStack stack) {
      return CompoundData.of(stack).booleanValue("ArtoriaManaBurstActive");
   }

   private static boolean isWindVeiledBlade(ItemStack stack) {
      return CompoundData.of(stack).booleanValue("ServantCardArtoriaWindVeiled");
   }

   private static boolean shouldHidePlantedWindDefenseSword(ItemStack stack, ItemDisplayContext displayContext) {
      if (!isHandDisplay(displayContext)) {
         return false;
      }
      return CompoundData.of(stack).booleanValue("ArtoriaWindDefenseActive");
   }

   private static boolean isHandDisplay(ItemDisplayContext displayContext) {
      return displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
         || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
   }

   /** Small adapter keeps NBT checks null-safe and avoids copying the tag repeatedly. */
   private record CompoundData(net.minecraft.nbt.CompoundTag tag) {
      static CompoundData of(ItemStack stack) {
         CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
         return new CompoundData(customData == null ? new net.minecraft.nbt.CompoundTag() : customData.copyTag());
      }

      boolean booleanValue(String key) {
         return tag.getBoolean(key);
      }
   }
}
