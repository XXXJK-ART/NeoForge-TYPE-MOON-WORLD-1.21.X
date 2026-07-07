package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HeshikiriHasebeRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardOdaNobunagaSkills;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HeshikiriHasebeItem extends SwordItem implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public HeshikiriHasebeItem(Properties properties) {
      super(Tiers.NETHERITE, properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && ServantCardOdaNobunagaSkills.handleHeshikiriRightClick(serverPlayer)) {
         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
      }
      return super.use(level, player, hand);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private HeshikiriHasebeRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new HeshikiriHasebeRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
         return PlayState.CONTINUE;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return true;
   }
}
