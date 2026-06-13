package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.NamelessBowRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaArrowOrbProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NamelessBowItem extends net.minecraft.world.item.Item implements GeoItem, NoblePhantasmItem {
   private static final int MIN_CHARGE_TICKS = 4;
   private static final int FULL_CHARGE_TICKS = 20;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public NamelessBowItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResultHolder.pass(stack);
      }

      player.startUsingItem(hand);
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BOW;
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (!(living instanceof Player player) || level.isClientSide) {
         return;
      }

      int chargeTicks = this.getUseDuration(stack, living) - timeLeft;
      if (chargeTicks < MIN_CHARGE_TICKS || !(level instanceof ServerLevel serverLevel)) {
         return;
      }

      ItemStack offhand = player.getOffhandItem();
      boolean hasPayload = !offhand.isEmpty();
      ItemStack payload = hasPayload ? offhand.copy() : new ItemStack(net.minecraft.world.item.Items.ARROW);
      payload.setCount(1);

      float charge = Math.min(1.0F, chargeTicks / (float)FULL_CHARGE_TICKS);
      Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(0.65));
      if (payload.is(ModItems.CRIMSON_HOUND.get())) {
         CrimsonHoundProjectileEntity projectile = new CrimsonHoundProjectileEntity(serverLevel, player);
         projectile.setPos(spawn.x, spawn.y - 0.12, spawn.z);
         projectile.setTrackedTarget(findLookTarget(serverLevel, player, 64.0));
         projectile.setDeltaMovement(player.getLookAngle().normalize().scale(2.2 + charge * 0.8));
         serverLevel.addFreshEntity(projectile);
      } else {
         EmiyaArrowOrbProjectileEntity projectile = new EmiyaArrowOrbProjectileEntity(serverLevel, player, payload);
         projectile.setPos(spawn.x, spawn.y - 0.12, spawn.z);
         projectile.setDirectDamage(10.0F + charge * 10.0F);
         projectile.setBrokenPhantasm(hasPayload);
         projectile.setDeltaMovement(player.getLookAngle().normalize().scale(2.4 + charge * 1.0));
         serverLevel.addFreshEntity(projectile);
      }

      if (hasPayload && !player.getAbilities().instabuild) {
         offhand.shrink(1);
      }
      serverLevel.sendParticles(ParticleTypes.END_ROD, spawn.x, spawn.y, spawn.z, 18, 0.16, 0.16, 0.16, 0.08);
      serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
   }

   private static LivingEntity findLookTarget(ServerLevel level, Player player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB area = player.getBoundingBox().expandTowards(look.scale(range)).inflate(3.0);
      LivingEntity best = null;
      double bestScore = 0.985;
      for (LivingEntity living : level.getEntitiesOfClass(
         LivingEntity.class,
         area,
         target -> target != player && target.isAlive() && !EntityUtils.isImmunePlayerTarget(target)
      )) {
         Vec3 toTarget = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = toTarget.length();
         if (distance <= 0.01 || distance > range) {
            continue;
         }

         double score = look.dot(toTarget.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }

      return best;
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private NamelessBowRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new NamelessBowRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<NamelessBowItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.nameless_bow.desc").withStyle(ChatFormatting.RED));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
