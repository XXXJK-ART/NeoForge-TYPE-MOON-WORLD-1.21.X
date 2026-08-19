package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RubyStaffRenderer;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.special.ElementalArrayService;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RubyStaffItem extends SwordItem implements GeoItem {
   public static final int CHARGE_TICKS = 100;
   public static final int COOLDOWN_TICKS = 100;
   public static final float SHIELD_HP = 500.0F;
   private static final String TAG_MODE = "RubyStaffUseMode";
   private static final String TAG_SHIELD_ACTIVE = "RubyStaffShieldActive";
   private static final String TAG_SHIELD_HP = "RubyStaffShieldHp";
   private static final int MODE_FIRE = 0;
   private static final int MODE_SHIELD = 1;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public RubyStaffItem(Properties properties) {
      super(Tiers.DIAMOND, properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private RubyStaffRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new RubyStaffRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<RubyStaffItem> state) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.ruby_staff.desc").withStyle(ChatFormatting.RED));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.getCooldowns().isOnCooldown(this)) {
         return InteractionResultHolder.fail(stack);
      }
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         beginUse(serverPlayer, player.isCrouching() ? MODE_SHIELD : MODE_FIRE);
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
   public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
         return;
      }
      int useTicks = this.getUseDuration(stack, living) - remainingUseDuration;
      if (useTicks % 5 == 0 || useTicks == CHARGE_TICKS) {
         ElementalArrayService.spawnChargeArray(player, ElementalArrayService.Kind.FIRE, Math.min(1.0F, useTicks / (float)CHARGE_TICKS), mode(player) == MODE_SHIELD);
      }
      if (mode(player) == MODE_SHIELD && useTicks >= CHARGE_TICKS) {
         activateShield(player, serverLevel);
      }
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
      if (!(living instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
         return;
      }
      int useTicks = this.getUseDuration(stack, living) - timeLeft;
      int mode = mode(player);
      boolean charged = useTicks >= CHARGE_TICKS;
      if (mode == MODE_FIRE && charged) {
         ElementalArrayService.releaseAttack(player, ElementalArrayService.Kind.FIRE);
         player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
      } else if (mode == MODE_SHIELD && charged) {
         player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
      }
      clearUseState(player);
   }

   public static boolean tryAbsorbShield(ServerPlayer player, LivingIncomingDamageEvent event) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE) || event.getAmount() <= 0.0F || !isHoldingActiveShield(player)) {
         return false;
      }
      if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      }
      float hp = data.getFloat(TAG_SHIELD_HP);
      if (hp <= 0.0F) {
         clearUseState(player);
         return false;
      }
      float absorbed = Math.min(hp, event.getAmount());
      float remainingDamage = Math.max(0.0F, event.getAmount() - absorbed);
      float remainingShield = hp - absorbed;
      event.setAmount(remainingDamage);
      data.putFloat(TAG_SHIELD_HP, remainingShield);
      ElementalArrayService.spawnShieldHit(player, ElementalArrayService.Kind.FIRE, remainingShield);
      if (remainingShield <= 0.0F) {
         clearUseState(player);
         player.stopUsingItem();
         player.getCooldowns().addCooldown(ModItems.RUBY_STAFF.get(), COOLDOWN_TICKS);
      }
      if (remainingDamage <= 0.0F) {
         event.setCanceled(true);
         event.setAmount(0.0F);
         return true;
      }
      return false;
   }

   public static void tickActiveShield(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE)) {
         return;
      }
      if (!player.isAlive() || !isHoldingActiveShield(player) || player.hasEffect(ModMobEffects.PETRIFIED)) {
         clearUseState(player);
         return;
      }
      if (player.level() instanceof ServerLevel level && player.tickCount % 4 == 0) {
         ElementalArrayService.spawnShieldArray(player, ElementalArrayService.Kind.FIRE, data.getFloat(TAG_SHIELD_HP) / SHIELD_HP);
      }
   }

   public static boolean isRubyStaff(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.is(ModItems.RUBY_STAFF.get());
   }

   private static void beginUse(ServerPlayer player, int mode) {
      CompoundTag data = player.getPersistentData();
      clearUseState(player);
      data.putInt(TAG_MODE, mode);
      if (player.level() instanceof ServerLevel level) {
         level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.65F, 0.95F);
      }
   }

   private static int mode(ServerPlayer player) {
      return player.getPersistentData().getInt(TAG_MODE);
   }

   private static void activateShield(ServerPlayer player, ServerLevel level) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(TAG_SHIELD_ACTIVE)) {
         data.putBoolean(TAG_SHIELD_ACTIVE, true);
         data.putFloat(TAG_SHIELD_HP, SHIELD_HP);
         level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.35F);
         ElementalArrayService.spawnShieldArray(player, ElementalArrayService.Kind.FIRE, 1.0F);
      }
   }

   private static void clearUseState(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(TAG_MODE);
      data.remove(TAG_SHIELD_ACTIVE);
      data.remove(TAG_SHIELD_HP);
   }

   private static boolean isHoldingActiveShield(ServerPlayer player) {
      return player.isUsingItem() && isRubyStaff(player.getUseItem());
   }
}
