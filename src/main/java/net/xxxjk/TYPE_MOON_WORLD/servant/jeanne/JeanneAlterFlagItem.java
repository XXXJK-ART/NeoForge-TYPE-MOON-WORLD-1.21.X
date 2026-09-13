package net.xxxjk.TYPE_MOON_WORLD.servant.jeanne;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class JeanneAlterFlagItem extends Item implements GeoItem {
   private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public JeanneAlterFlagItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
      if (!(player instanceof ServerPlayer server) || !JeanneAlterSkills.isJeanneAlter(player)) {
         return InteractionResultHolder.fail(stack);
      }
      if (player.isShiftKeyDown()) {
         return InteractionResultHolder.sidedSuccess(stack, JeanneAlterSkills.consumeGrudgeBurst(player));
      }
      if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
      if (!JeanneAlterSkills.startFlagExplosion(server)) return InteractionResultHolder.fail(stack);
      player.getCooldowns().addCooldown(this, 12 * 20);
      return InteractionResultHolder.sidedSuccess(stack, true);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private JeanneAlterFlagRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new JeanneAlterFlagRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "flag", 0, state -> {
         state.getController().setAnimation(IDLE);
         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
