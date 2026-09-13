package net.xxxjk.TYPE_MOON_WORLD.client.model;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.entity.SeaBeastEntity;
import software.bernie.geckolib.model.GeoModel;
public class SeaBeastModel extends GeoModel<SeaBeastEntity> {
   @Override public ResourceLocation getModelResource(SeaBeastEntity e){return id("geo/sea_beast.geo.json");}
   @Override public ResourceLocation getTextureResource(SeaBeastEntity e){return id("textures/entity/sea_beast.png");}
   @Override public ResourceLocation getAnimationResource(SeaBeastEntity e){return id("animations/sea_beast.animation.json");}
   private ResourceLocation id(String p){return ResourceLocation.fromNamespaceAndPath("typemoonworld",p);}
}
