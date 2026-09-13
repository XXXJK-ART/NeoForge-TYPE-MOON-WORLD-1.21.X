package net.xxxjk.TYPE_MOON_WORLD.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class SwordInStoneBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final RawAnimation DISPLAY_ANIMATION = RawAnimation.begin().thenLoop("animation.saber.new");
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public SwordInStoneBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType<?>) ModBlockEntities.SWORD_IN_STONE_BLOCK_ENTITY.get(), pos, state);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "display", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<SwordInStoneBlockEntity> state) {
      state.getController().setAnimation(DISPLAY_ANIMATION);
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return cache;
   }
}
