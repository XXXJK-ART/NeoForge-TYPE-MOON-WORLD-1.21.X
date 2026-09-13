package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BrokenExcaliburRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Broken Excalibur item rendered with the supplied Blockbench model. */
public final class BrokenExcaliburItem extends Item implements GeoItem {
   private static final RawAnimation DISPLAY_ANIMATION = RawAnimation.begin().thenLoop("animation.saber.new");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public BrokenExcaliburItem(Properties properties) {
      super(properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private BrokenExcaliburRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new BrokenExcaliburRenderer();
            return renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "display", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<BrokenExcaliburItem> state) {
      state.getController().setAnimation(DISPLAY_ANIMATION);
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return cache;
   }
}
