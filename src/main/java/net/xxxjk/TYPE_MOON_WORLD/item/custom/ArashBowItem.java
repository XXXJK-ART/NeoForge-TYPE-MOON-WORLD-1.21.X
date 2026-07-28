package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
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
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashAimHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashCombatRules;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ArashBowItem extends net.minecraft.world.item.Item implements GeoItem {
   public static final int CHARGED_ARROW_TICKS = 40;
   public static final int HEAVY_CHARGED_ARROW_TICKS = 80;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ArashBowItem(Properties properties) { super(properties); }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
      if (level instanceof ServerLevel serverLevel) fireBasicArrow(serverLevel, player);
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(stack);
   }

   @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
   @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (!(living instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;
      int chargeTicks = getUseDuration(stack, living) - timeLeft;
      if (chargeTicks < CHARGED_ARROW_TICKS) return;
      int slot = chargeTicks >= HEAVY_CHARGED_ARROW_TICKS ? 2 : 1;
      if (player instanceof ServerPlayer serverPlayer && ServantCardArashSkills.isArash(serverPlayer)) {
         ServantCardArashSkills.performBowChargedArrowNoCooldown(serverPlayer, slot == 2);
      } else if (slot == 2) {
         ServantCardArashSkills.performLargeEnergyArrow((ServerPlayer)player);
      } else {
         ServantCardArashSkills.performSmallEnergyArrow((ServerPlayer)player);
      }
   }

   private static void fireBasicArrow(ServerLevel serverLevel, Player player) {
      Vec3 look = player.getLookAngle().normalize();
      Vec3 start = player.getEyePosition().add(look.scale(0.65));
      Vec3 direction = ArashAimHelper.autoAimDirection(player, start, look, 3.4);
      ArashParticleArrowEntity arrow = new ArashParticleArrowEntity(serverLevel, player,
         ArashParticleArrowEntity.NORMAL, ArashCombatRules.NORMAL_ARROW_DAMAGE);
      arrow.setPos(start.x, start.y - 0.12, start.z);
      arrow.setDeltaMovement(direction.scale(3.4));
      serverLevel.addFreshEntity(arrow);
      if (player instanceof ServerPlayer serverPlayer) ServantCardVoiceHelper.tryPlayAttack(serverPlayer);
      serverLevel.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 14, 0.12, 0.12, 0.12, 0.08);
      serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
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

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) { }
   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
