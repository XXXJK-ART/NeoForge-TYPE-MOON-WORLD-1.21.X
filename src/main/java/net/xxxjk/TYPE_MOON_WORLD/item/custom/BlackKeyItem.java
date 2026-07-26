package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BlackKeyRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BlackKeyItem extends Item implements GeoItem {
   private static final String EXPANDED = "BlackKeyExpanded";
   private static final String DURABILITY = "BlackKeyDurability";
   private static final String FIRE_ENGRAVED = "BlackKeyFireEngraved";
   public static final int MAX_DURABILITY = 100;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public BlackKeyItem(Properties properties) {
      super(properties.stacksTo(3));
   }

   public static boolean isExpanded(ItemStack stack) {
      return data(stack).getBoolean(EXPANDED);
   }

   public static int getSharedDurability(ItemStack stack) {
      CompoundTag tag = data(stack);
      int value = tag.contains(DURABILITY) ? tag.getInt(DURABILITY) : MAX_DURABILITY;
      return Math.clamp(value, 0, MAX_DURABILITY);
   }

   public static boolean isFireEngraved(ItemStack stack) {
      return data(stack).getBoolean(FIRE_ENGRAVED);
   }

   public static void setExpanded(ItemStack stack, boolean expanded) {
      mutate(stack, tag -> {
         tag.putBoolean(EXPANDED, expanded);
         if (!tag.contains(DURABILITY)) tag.putInt(DURABILITY, MAX_DURABILITY);
      });
   }

   public static void setFireEngraved(ItemStack stack, boolean fireEngraved) {
      mutate(stack, tag -> tag.putBoolean(FIRE_ENGRAVED, fireEngraved));
   }

   public static void consumeSharedDurability(ItemStack stack, int amount) {
      if (!isExpanded(stack) || amount <= 0) return;
      int remaining = getSharedDurability(stack) - amount;
      if (remaining <= 0) {
         mutate(stack, tag -> {
            tag.putBoolean(EXPANDED, false);
            tag.putInt(DURABILITY, MAX_DURABILITY);
         });
      } else {
         mutate(stack, tag -> tag.putInt(DURABILITY, remaining));
      }
   }

   public static float meleeDamage(ItemStack stack) {
      return net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.blackKeyMeleeDamage(isExpanded(stack), stack.getCount());
   }

   public static boolean engraveWithFire(ServerPlayer player, ItemStack stack) {
      if (!(stack.getItem() instanceof BlackKeyItem) || isFireEngraved(stack)) return false;
      if (!ManaHelper.consumeManaStrict(player, 20.0, false)) return false;
      setFireEngraved(stack, true);
      player.displayClientMessage(Component.translatable("message.typemoonworld.black_key.fire_engraved"), true);
      return true;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) return InteractionResultHolder.consume(stack);
      if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);

      if (player.isShiftKeyDown()) {
         boolean expand = !isExpanded(stack);
         if (expand && !ManaHelper.consumeManaStrict(serverPlayer, 10.0, false)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.black_key.no_mana"), true);
            return InteractionResultHolder.fail(stack);
         }
         setExpanded(stack, expand);
         level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.PLAYERS, 0.7F, expand ? 1.3F : 0.8F);
         return InteractionResultHolder.success(stack);
      }

      if (!isExpanded(stack)) {
         if (!ManaHelper.consumeManaStrict(serverPlayer, 10.0, false)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.black_key.no_mana"), true);
            return InteractionResultHolder.fail(stack);
         }
         setExpanded(stack, true);
      }
      int count = stack.getCount();
      ItemStack template = stack.copyWithCount(1);
      for (int i = 0; i < count; i++) {
         BlackKeyProjectileEntity projectile = new BlackKeyProjectileEntity(level, player, template.copy(), true);
         projectile.shootFromRotation(player, player.getXRot(), player.getYRot() + throwYawOffset(count, i), 0.0F, 3.0F, 0.25F);
         level.addFreshEntity(projectile);
      }
      stack.shrink(count);
      level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8F, 1.25F);
      return InteractionResultHolder.success(stack);
   }

   public static float throwYawOffset(int count, int index) {
      return net.xxxjk.TYPE_MOON_WORLD.combat.ChurchDeadApostleRules.blackKeyThrowYawOffset(count, index);
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!attacker.level().isClientSide && isExpanded(stack)) {
         consumeSharedDurability(stack, Math.clamp(stack.getCount(), 1, 3));
         if (isFireEngraved(stack) && target.isAlive()) {
            target.igniteForSeconds(4.0F);
            target.invulnerableTime = 0;
            target.hurt(target.damageSources().onFire(), BlackKeyProjectileEntity.isUndead(target) ? 10.0F : 5.0F);
         }
      }
      return true;
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return isFireEngraved(stack);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private BlackKeyRenderer renderer;
         @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (renderer == null) renderer = new BlackKeyRenderer();
            return renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "black_key_state", 0, state -> {
         ItemStack stack = state.getData(DataTickets.ITEMSTACK);
         int count = stack == null ? 1 : Math.clamp(stack.getCount(), 1, 3);
         String animation = "count_" + count + (stack != null && isExpanded(stack) ? "_expanded" : "_folded");
         state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
         return PlayState.CONTINUE;
      }));
   }

   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

   private static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static void mutate(ItemStack stack, Consumer<CompoundTag> mutation) {
      CompoundTag tag = data(stack);
      mutation.accept(tag);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }
}
