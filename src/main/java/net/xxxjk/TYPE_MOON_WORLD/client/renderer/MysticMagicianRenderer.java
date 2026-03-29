package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

public class MysticMagicianRenderer extends HumanoidMobRenderer<MysticMagicianEntity, PlayerModel<MysticMagicianEntity>> {
   private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_0.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_1.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_2.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_3.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_4.png"),
      ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/mystic_magician_5.png")
   };
   private final PlayerModel<MysticMagicianEntity> steveModel;
   private final PlayerModel<MysticMagicianEntity> alexModel;

   public MysticMagicianRenderer(Context context) {
      super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.steveModel = this.getModel();
      this.alexModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
      this.addLayer(new MysticMagicianReinforcementLayer(this, context));
   }

   public void render(MysticMagicianEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      int index = Mth.clamp(entity.getSkinVariant(), 0, TEXTURES.length - 1);
      this.model = MysticMagicianEntity.isFemaleVariant(index) ? this.alexModel : this.steveModel;
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }

   public ResourceLocation getTextureLocation(MysticMagicianEntity entity) {
      int index = Mth.clamp(entity.getSkinVariant(), 0, TEXTURES.length - 1);
      return TEXTURES[index];
   }
}
