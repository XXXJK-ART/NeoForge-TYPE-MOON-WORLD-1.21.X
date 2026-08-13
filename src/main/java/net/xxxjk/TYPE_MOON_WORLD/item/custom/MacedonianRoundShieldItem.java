package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.MacedonianRoundShieldRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class MacedonianRoundShieldItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public MacedonianRoundShieldItem(Properties properties) {
      super(properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private MacedonianRoundShieldRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) {
               renderer = new MacedonianRoundShieldRenderer();
            }
            return renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
