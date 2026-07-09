package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ThompsonContenderRenderer;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ThompsonContenderItem extends Item implements GeoItem {
   private static final String LOADED_BULLET_TAG = "LoadedBullet";
   private static final int EMPTY = 0;
   private static final int NORMAL = 1;
   private static final int ORIGIN = 2;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ThompsonContenderItem(Properties properties) {
      super(properties);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ThompsonContenderRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new ThompsonContenderRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<ThompsonContenderItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
         int loaded = getLoadedBullet(stack);
         if (loaded == EMPTY) {
            int loadedKind = tryLoad(serverPlayer, hand);
            if (loadedKind != EMPTY) {
               setLoadedBullet(stack, loadedKind);
               level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.7F, 1.25F);
               serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.thompson_contender.loaded", bulletName(loadedKind)), true);
            } else {
               serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.thompson_contender.no_ammo"), true);
               return InteractionResultHolder.fail(stack);
            }
         } else {
            fire(serverPlayer, loaded == ORIGIN);
            setLoadedBullet(stack, EMPTY);
            serverPlayer.getCooldowns().addCooldown(this, 18);
         }
      }
      return InteractionResultHolder.consume(stack);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      int loaded = getLoadedBullet(stack);
      tooltip.add(Component.translatable("item.typemoonworld.thompson_contender.desc").withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("item.typemoonworld.thompson_contender.loaded", bulletName(loaded)).withStyle(loaded == ORIGIN ? ChatFormatting.RED : ChatFormatting.GOLD));
   }

   private static Component bulletName(int kind) {
      return switch (kind) {
         case NORMAL -> Component.translatable("item.typemoonworld.bullet");
         case ORIGIN -> Component.translatable("item.typemoonworld.origin_bullet");
         default -> Component.translatable("item.typemoonworld.thompson_contender.empty");
      };
   }

   private static void fire(ServerPlayer player, boolean origin) {
      if (!(player.level() instanceof ServerLevel level)) {
         return;
      }
      ContenderBulletEntity bullet = new ContenderBulletEntity(level, player, origin);
      Vec3 spawn = player.getEyePosition().add(player.getLookAngle().scale(0.55));
      bullet.setPos(spawn.x, spawn.y - 0.06, spawn.z);
      Vec3 look = player.getLookAngle();
      bullet.shoot(look.x, look.y, look.z, 4.6F, 0.01F);
      level.addFreshEntity(bullet);
      level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, origin ? 1.15F : 0.95F, origin ? 1.55F : 1.85F);
   }

   private static int tryLoad(ServerPlayer player, InteractionHand gunHand) {
      InteractionHand otherHand = gunHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      ItemStack offhand = player.getItemInHand(otherHand);
      int offhandKind = kindOf(offhand);
      if (offhandKind != EMPTY) {
         offhand.shrink(1);
         return offhandKind;
      }
      Inventory inventory = player.getInventory();
      int normalSlot = findAmmoSlot(inventory, NORMAL);
      if (normalSlot >= 0) {
         inventory.getItem(normalSlot).shrink(1);
         return NORMAL;
      }
      int originSlot = findAmmoSlot(inventory, ORIGIN);
      if (originSlot >= 0) {
         inventory.getItem(originSlot).shrink(1);
         return ORIGIN;
      }
      return EMPTY;
   }

   private static int findAmmoSlot(Inventory inventory, int kind) {
      for (int i = 0; i < inventory.items.size(); i++) {
         if (kindOf(inventory.items.get(i)) == kind) {
            return i;
         }
      }
      return -1;
   }

   private static int kindOf(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return EMPTY;
      }
      if (stack.is(ModItems.ORIGIN_BULLET.get())) {
         return ORIGIN;
      }
      if (stack.is(ModItems.BULLET.get())) {
         return NORMAL;
      }
      return EMPTY;
   }

   private static int getLoadedBullet(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      if (data == null) {
         return EMPTY;
      }
      return Math.max(EMPTY, Math.min(ORIGIN, data.copyTag().getInt(LOADED_BULLET_TAG)));
   }

   private static void setLoadedBullet(ItemStack stack, int kind) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      if (kind == EMPTY) {
         tag.remove(LOADED_BULLET_TAG);
      } else {
         tag.putInt(LOADED_BULLET_TAG, kind);
      }
      if (tag.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
   }
}
