package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.client.model.BajiquanPlayerModel;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoApprenticeEntity;

public class KendoApprenticeRenderer extends HumanoidMobRenderer<KendoApprenticeEntity, PlayerModel<KendoApprenticeEntity>> {
   private static final ResourceLocation MALE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/kendo_apprentice_male.png");
   private static final ResourceLocation FEMALE = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/kendo_apprentice_female.png");
   private final PlayerModel<KendoApprenticeEntity> male;
   private final PlayerModel<KendoApprenticeEntity> female;
   public KendoApprenticeRenderer(EntityRendererProvider.Context context) { super(context, new BajiquanPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F); male = getModel(); female = new BajiquanPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true); }
   @Override public void render(KendoApprenticeEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) { model = entity.isFemale() ? female : male; super.render(entity, yaw, partialTick, pose, buffer, light); }
   @Override public ResourceLocation getTextureLocation(KendoApprenticeEntity entity) { return entity.isFemale() ? FEMALE : MALE; }
}
