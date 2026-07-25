package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;

public class KendoMasterRenderer extends HumanoidMobRenderer<KendoMasterEntity, PlayerModel<KendoMasterEntity>> {
   private static final ResourceLocation RONIN = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/ronin.png");
   private static final ResourceLocation SHINSENGUMI = ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/shinsengumi.png");
   public KendoMasterRenderer(EntityRendererProvider.Context context) { super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F); }
   @Override public ResourceLocation getTextureLocation(KendoMasterEntity entity) { return entity.school() == KendoSchool.TENNEN ? SHINSENGUMI : RONIN; }
}
