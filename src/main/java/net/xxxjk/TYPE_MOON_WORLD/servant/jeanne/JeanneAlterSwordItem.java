package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class JeanneAlterSwordItem extends SwordItem implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public JeanneAlterSwordItem(Properties properties) {
      super(Tiers.DIAMOND, properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private JeanneAlterSwordRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new JeanneAlterSwordRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
