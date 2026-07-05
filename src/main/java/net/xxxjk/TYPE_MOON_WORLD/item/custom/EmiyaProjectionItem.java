package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.EmiyaProjectionItemRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EmiyaProjectionItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private static final int PSEUDO_SPIRAL_COOLDOWN = 10;
   private static final int PARACELSUS_SWORD_COOLDOWN = 1200;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final String projectionId;

   public EmiyaProjectionItem(Properties properties, String projectionId) {
      super(Tiers.NETHERITE, properties);
      this.projectionId = projectionId;
   }

   public String projectionId() {
      return this.projectionId;
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private EmiyaProjectionItemRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new EmiyaProjectionItemRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<EmiyaProjectionItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld." + this.projectionId + ".desc").withStyle(ChatFormatting.RED));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if ("excalibur_gallatin".equals(this.projectionId)) {
         if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
         }
         if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerNoblePhantasmHelper.startGallatinCharge(serverPlayer);
         }
         player.startUsingItem(hand);
         return InteractionResultHolder.consume(stack);
      }
      if ("pseudo_spiral_sword".equals(this.projectionId)) {
         if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
         }
         if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerNoblePhantasmHelper.usePseudoSpiralDash(serverPlayer);
            player.getCooldowns().addCooldown(this, PSEUDO_SPIRAL_COOLDOWN);
         }
         return InteractionResultHolder.consume(stack);
      }
      if ("paracelsus_sword".equals(this.projectionId)) {
         if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
         }
         if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && PlayerNoblePhantasmHelper.consumeStrict(serverPlayer, 80.0)) {
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1, false, true, true));
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true, true));
            player.getCooldowns().addCooldown(this, PARACELSUS_SWORD_COOLDOWN);
         }
         return InteractionResultHolder.consume(stack);
      }
      if (PlayerNoblePhantasmHelper.isUbwProjection(stack) && isKanshouBakuya()) {
         if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerNoblePhantasmHelper.useKanshouBakuya(serverPlayer, hand, this.projectionId);
         }
         return InteractionResultHolder.consume(stack);
      }
      return super.use(level, player, hand);
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return "excalibur_gallatin".equals(this.projectionId) ? 72000 : super.getUseDuration(stack, entity);
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return "excalibur_gallatin".equals(this.projectionId) ? UseAnim.BOW : super.getUseAnimation(stack);
   }

   @Override
   public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
      if ("excalibur_gallatin".equals(this.projectionId)) {
         int useTicks = this.getUseDuration(stack, living) - remainingUseDuration;
         PlayerNoblePhantasmHelper.tickGallatinCharge(level, living, useTicks);
      }
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if ("excalibur_gallatin".equals(this.projectionId) && !level.isClientSide() && living instanceof ServerPlayer player) {
         PlayerNoblePhantasmHelper.releaseGallatin(player);
      }
   }

   private boolean isKanshouBakuya() {
      return "gan_jiang".equals(this.projectionId)
         || "mo_ye".equals(this.projectionId)
         || "gan_jiang_overedge".equals(this.projectionId)
         || "mo_ye_overedge".equals(this.projectionId);
   }
}
