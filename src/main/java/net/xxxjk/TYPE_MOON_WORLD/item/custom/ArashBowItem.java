package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ArashBowRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardArashSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardVoiceHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.joml.Vector3f;

public final class ArashBowItem extends net.minecraft.world.item.Item implements GeoItem {
   public static final int CHARGED_ARROW_TICKS = ArashCombatRules.ARROW_MAX_CHARGE_TICKS;
   public static final String INTERRUPTED_TAG = "ArashBowChargeInterrupted";
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ArashBowItem(Properties properties) { super(properties); }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
      player.getPersistentData().remove(INTERRUPTED_TAG);
      if (level instanceof ServerLevel serverLevel) GeoItem.getOrAssignId(stack, serverLevel);
      if (player instanceof ServerPlayer serverPlayer && ServantCardArashSkills.isArash(serverPlayer)
         && !ServantCardArashSkills.hasArrows(serverPlayer, ArashCombatRules.NORMAL_ARROW_COST)) {
         return InteractionResultHolder.fail(stack);
      }
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(stack);
   }

   @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
   @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

   @Override
   public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
      if (living.getTicksUsingItem() == 1) {
         if (level instanceof ServerLevel serverLevel) GeoItem.getOrAssignId(stack, serverLevel);
         if (level.isClientSide) this.triggerAnim(living, GeoItem.getId(stack), "bow_controller", "draw");
      }
      if (level.isClientSide && (living instanceof ArashEntity || living instanceof Player chargePlayer
         && ServantCardArashSkills.isArash(chargePlayer)) && living.tickCount % 2 == 0) {
         int chargeTicks = Math.min(ArashCombatRules.ARROW_MAX_CHARGE_TICKS, living.getTicksUsingItem());
         double progress = chargeTicks / (double)ArashCombatRules.ARROW_MAX_CHARGE_TICKS;
         // Keep the dense charge particles away from the reticle at eye distance.
         var color = new DustParticleOptions(progress < 0.34 ? new Vector3f(1.0F, 1.0F, 1.0F)
            : progress < 0.67 ? new Vector3f(0.15F, 1.0F, 0.35F) : new Vector3f(1.0F, 0.05F, 0.015F),
            (float)(0.45 + progress * 0.3));
         Vec3 look = living.getLookAngle().normalize();
         Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
         if (side.lengthSqr() < 1.0E-6) side = new Vec3(1.0, 0.0, 0.0);
         side = side.normalize();
         Vec3 up = side.cross(look).normalize();
         double handOffset = living.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.32 : -0.32;
         Vec3 center = living.getEyePosition().add(look.scale(1.15)).add(side.scale(handOffset)).add(up.scale(-0.2));
         double radius = 0.07 + progress * 0.13;
         for (int i = 0; i < 8; i++) {
            double angle = (living.tickCount * 0.32) + Math.PI * 2.0 * i / 8.0;
            Vec3 point = center.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            level.addParticle(color, true, point.x, point.y, point.z, 0.0, 0.0, 0.0);
         }
         if (living.tickCount % 8 == 0) level.addParticle(ParticleTypes.END_ROD, true,
            center.x, center.y, center.z, 0.0, 0.0, 0.0);
         for (int i = 0; i < 2 + (int)(progress * 5); i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, true, center.x, center.y, center.z,
               (level.random.nextDouble() - 0.5) * 0.08, (level.random.nextDouble() - 0.5) * 0.08,
               (level.random.nextDouble() - 0.5) * 0.08);
         }
      }
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (level.isClientSide) {
         this.triggerAnim(living, GeoItem.getId(stack), "bow_controller", "release");
         return;
      }
      if (!(living instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;
      if (player.getPersistentData().getBoolean(INTERRUPTED_TAG)) {
         player.getPersistentData().remove(INTERRUPTED_TAG);
         return;
      }
      int chargeTicks = Math.min(ArashCombatRules.ARROW_MAX_CHARGE_TICKS, getUseDuration(stack, living) - timeLeft);
      boolean arash = player instanceof ServerPlayer serverPlayer && ServantCardArashSkills.isArash(serverPlayer);
      if (arash && !ServantCardArashSkills.hasArrows((ServerPlayer)player, ArashCombatRules.NORMAL_ARROW_COST)) return;
      if (fireBasicArrow(serverLevel, player, chargeTicks, arash) && arash) {
         ServantCardArashSkills.consumeBasicArrow((ServerPlayer)player);
      }
   }

   private static boolean fireBasicArrow(ServerLevel serverLevel, Player player, int chargeTicks, boolean arash) {
      Vec3 look = player.getLookAngle().normalize();
      Vec3 start = player.getEyePosition().add(look.scale(0.65));
      Vec3 direction = look;
      ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(serverLevel, player,
         ArashParticleArrowEntity.NORMAL, arash ? ArashCombatRules.ARROW_MIN_DAMAGE : ArashCombatRules.NORMAL_ARROW_DAMAGE,
         arash ? chargeTicks : 0, arash);
      arrow.setPos(start.x, start.y - 0.12, start.z);
      arrow.setDeltaMovement(direction.scale(3.4));
      if (!serverLevel.addFreshEntity(arrow)) return false;
      if (player instanceof ServerPlayer serverPlayer && arash) ServantCardVoiceHelper.tryPlayAttack(serverPlayer);
      serverLevel.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 14, 0.12, 0.12, 0.12, 0.08);
      serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
      return true;
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ArashBowRenderer renderer;
         @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new ArashBowRenderer();
            return renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      AnimationController<ArashBowItem> bow = new AnimationController<>(this, "bow_controller", 0,
         state -> PlayState.STOP);
      bow.triggerableAnim("draw", RawAnimation.begin().thenLoop("animation.arash_bow.draw"));
      bow.triggerableAnim("release", RawAnimation.begin().thenPlay("animation.arash_bow.release"));
      controllers.add(bow);
   }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
