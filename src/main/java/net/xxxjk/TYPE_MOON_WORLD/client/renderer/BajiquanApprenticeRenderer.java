package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BajiquanPlayerModel;

public class BajiquanApprenticeRenderer extends HumanoidMobRenderer<BajiquanApprenticeEntity, PlayerModel<BajiquanApprenticeEntity>> {
   private static final ResourceLocation MALE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/bajiquan_apprentice_male.png");
   private static final ResourceLocation FEMALE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/bajiquan_apprentice_female.png");
   private final PlayerModel<BajiquanApprenticeEntity> male;
   private final PlayerModel<BajiquanApprenticeEntity> female;
   public BajiquanApprenticeRenderer(EntityRendererProvider.Context context) {
      super(context, new BajiquanPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
      this.male = this.getModel();
      this.female = new BajiquanPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
   }
   @Override public void render(BajiquanApprenticeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
      this.model = entity.isFemale() ? this.female : this.male;
      super.render(entity, yaw, partialTick, pose, buffer, light);
   }
   @Override public ResourceLocation getTextureLocation(BajiquanApprenticeEntity entity) { return entity.isFemale() ? FEMALE : MALE; }
}
