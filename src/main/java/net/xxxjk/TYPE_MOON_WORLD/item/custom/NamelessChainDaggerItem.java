package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.model.NamelessChainDaggerModel;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NamelessChainDaggerItem extends SwordItem implements GeoItem {
   private static final double CHAIN_RANGE = 18.0;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public NamelessChainDaggerItem(Properties properties) {
      super(Tiers.IRON, properties);
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.nameless_chain_dagger.desc").withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         if (player.isCrouching()) {
            LivingEntity target = findLookTarget(serverPlayer);
            if (target == null) {
               serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
            } else {
               pullTargetToPlayer(serverPlayer, target);
            }
         } else {
            Vec3 hookPoint = findHookPoint(serverPlayer);
            if (hookPoint == null) {
               serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.no_target"), true);
            } else {
               pullPlayerToPoint(serverPlayer, hookPoint);
            }
         }
      }
      return InteractionResultHolder.consume(stack);
   }

   private PlayState predicate(AnimationState<NamelessChainDaggerItem> state) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   private static LivingEntity findLookTarget(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 end = eye.add(look.scale(CHAIN_RANGE));
      AABB box = player.getBoundingBox().expandTowards(look.scale(CHAIN_RANGE)).inflate(1.2);
      EntityHitResult hit = ProjectileUtil.getEntityHitResult(
         player,
         eye,
         end,
         box,
         entity -> entity instanceof LivingEntity living && living.isAlive() && entity != player && !EntityUtils.isImmunePlayerTarget(entity),
         CHAIN_RANGE * CHAIN_RANGE
      );
      return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
   }

   private static Vec3 findHookPoint(ServerPlayer player) {
      LivingEntity entityTarget = findLookTarget(player);
      if (entityTarget != null) {
         return entityTarget.position().add(0.0, entityTarget.getBbHeight() * 0.45, 0.0);
      }
      HitResult blockHit = player.pick(CHAIN_RANGE, 1.0F, false);
      if (blockHit instanceof BlockHitResult && blockHit.getType() == HitResult.Type.BLOCK) {
         return blockHit.getLocation();
      }
      return null;
   }

   private static void pullPlayerToPoint(ServerPlayer player, Vec3 hookPoint) {
      Vec3 toTarget = hookPoint.subtract(player.position());
      if (toTarget.lengthSqr() > 1.0E-4) {
         Vec3 motion = toTarget.normalize().scale(Math.min(2.2, Math.max(0.9, toTarget.length() * 0.22)));
         player.setDeltaMovement(motion.x, Math.max(0.18, motion.y + 0.18), motion.z);
         player.hurtMarked = true;
      }
      spawnChainFx(player, hookPoint);
   }

   private static void pullTargetToPlayer(ServerPlayer player, LivingEntity target) {
      Vec3 toPlayer = player.position().add(0.0, player.getBbHeight() * 0.35, 0.0).subtract(target.position());
      if (toPlayer.lengthSqr() > 1.0E-4) {
         Vec3 motion = toPlayer.normalize().scale(Math.min(2.0, Math.max(0.8, toPlayer.length() * 0.22)));
         target.setDeltaMovement(motion.x, Math.max(0.16, motion.y + 0.1), motion.z);
         target.hurtMarked = true;
      }
      spawnChainFx(player, target.position().add(0.0, target.getBbHeight() * 0.5, 0.0));
   }

   private static void spawnChainFx(ServerPlayer player, Vec3 to) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      Vec3 from = player.getEyePosition().subtract(0.0, 0.15, 0.0);
      for (double t = 0.0; t <= 1.0; t += 0.08) {
         Vec3 pos = from.lerp(to, t);
         level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
      }
      level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.9F, 1.35F);
      level.playSound(null, to.x, to.y, to.z, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.55F, 0.7F);
   }
}
