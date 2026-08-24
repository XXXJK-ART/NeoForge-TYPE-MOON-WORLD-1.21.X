package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.BlackKeyMiracleService;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BlackKeyProjectileEntity extends ThrowableItemProjectile implements GeoEntity {
   private static final int MAX_LIFE_TICKS = 60;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
   private boolean recoverable;
   private int brokenBlocks;

   public BlackKeyProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.noCulling = true;
      setNoGravity(true);
   }

   public BlackKeyProjectileEntity(Level level, LivingEntity owner, ItemStack stack, boolean recoverable) {
      super(ModEntities.BLACK_KEY_PROJECTILE.get(), owner, level);
      this.noCulling = true;
      setNoGravity(true);
      ItemStack carried = stack.copyWithCount(1);
      setItem(carried);
      this.recoverable = recoverable;
   }

   @Override protected Item getDefaultItem() { return ModItems.BLACK_KEY.get(); }

   @Override public boolean isNoGravity() { return true; }

   @Override
   public void tick() {
      setNoGravity(true);
      super.tick();
      if (!level().isClientSide && tickCount > MAX_LIFE_TICKS && !isRemoved()) {
         finish(getItem().copyWithCount(1));
      }
   }

   @Override
   protected boolean canHitEntity(Entity entity) {
      if (entity == getOwner()) return false;
      return !(getOwner() instanceof LivingEntity owner && entity instanceof LivingEntity living && owner.isAlliedTo(living))
         && super.canHitEntity(entity);
   }

   @Override
   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
         ItemStack carried = getItem().copyWithCount(1);
         float base = isUndead(target) ? 22.0F : 11.0F;
         target.invulnerableTime = 0;
         target.hurt(damageSources().thrown(this, getOwner()), base);
         BlackKeyMiracleService.onProjectileHit(this, target, carried);
         BlackKeyItem.consumeSharedDurability(carried, 1);
         finish(carried);
      }
   }

   @Override
   protected void onHitBlock(BlockHitResult result) {
      super.onHitBlock(result);
      if (level().isClientSide || isRemoved()) return;

      boolean enhanced = BlackKeyMiracleService.tryTriggerIronArmorAction(getOwner());
      int radius = enhanced ? BlackKeyMiracleService.blockBreakRadius(getOwner()) : 0;
      int brokenThisHit = breakImpactBlocks(result.getBlockPos(), radius);
      if (brokenThisHit <= 0) {
         finish(getItem().copyWithCount(1));
         return;
      }

      brokenBlocks += brokenThisHit;
      int maxBroken = enhanced ? BlackKeyMiracleService.maxBrokenBlocks(getOwner()) : ChurchDeadApostleRules.BLACK_KEY_MAX_BROKEN_BLOCKS;
      if (brokenBlocks >= maxBroken) {
         finish(getItem().copyWithCount(1));
         return;
      }

      Vec3 motion = getDeltaMovement();
      if (motion.lengthSqr() > 1.0E-6) {
         Vec3 throughBlock = result.getLocation().add(motion.normalize().scale(0.2));
         setPos(throughBlock.x, throughBlock.y, throughBlock.z);
      }
   }

   private int breakImpactBlocks(BlockPos center, int radius) {
      int maxBroken = radius > 0 ? BlackKeyMiracleService.maxBrokenBlocks(getOwner()) : ChurchDeadApostleRules.BLACK_KEY_MAX_BROKEN_BLOCKS;
      int remaining = Math.max(0, maxBroken - brokenBlocks);
      int broken = 0;
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
         if (broken >= remaining) break;
         BlockPos immutable = pos.immutable();
         BlockState state = level().getBlockState(immutable);
         if (radius <= 0) {
            float hardness = state.getDestroySpeed(level(), immutable);
            boolean protectedBlock = state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME);
            if (state.isAir() || !ChurchDeadApostleRules.blackKeyCanBreakBlock(hardness, state.hasBlockEntity(), protectedBlock)) continue;
         } else if (!BlackKeyMiracleService.canBreakBlock(level(), immutable, state, getOwner())) {
            continue;
         }
         if (level().destroyBlock(immutable, true, getOwner())) broken++;
      }
      return broken;
   }

   private void finish(ItemStack stack) {
      if (recoverable && getOwner() instanceof Player && !stack.isEmpty()) {
         ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), stack);
         item.setDefaultPickUpDelay();
         level().addFreshEntity(item);
      }
      discard();
   }

   public static boolean isUndead(LivingEntity target) {
      return DeadApostleEntity.isDeadApostle(target)
         || target.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "black_key_projectile_state", 0, state -> {
         String animation = BlackKeyItem.isExpanded(getItem()) ? "count_1_expanded" : "count_1_folded";
         state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
         return PlayState.CONTINUE;
      }));
   }

   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("Recoverable", recoverable);
      tag.putInt("BrokenBlocks", brokenBlocks);
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      recoverable = tag.getBoolean("Recoverable");
      brokenBlocks = Math.clamp(tag.getInt("BrokenBlocks"), 0, ChurchDeadApostleRules.BLACK_KEY_MAX_BROKEN_BLOCKS);
      setNoGravity(true);
   }
}
