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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.AvalonRenderer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AvalonItem extends Item implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private static final long TICKS_FOR_SWORD_ATTRIBUTE = 24000L;
   private static final String AVALON_ACTIVE_TAG = "TypeMoonAvalonActive";
   private static final String LEGACY_PLAYER_AVALON_UNTIL_TAG = "TypeMoonPlayerAvalonUntil";

   public AvalonItem(Properties properties) {
      super(properties);
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private AvalonRenderer renderer;

         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new AvalonRenderer();
            }

            return this.renderer;
         }
      });
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (!level.isClientSide && entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.player_magic_attributes_sword) {
            CustomData customData = (CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = customData.copyTag();
            long time = tag.getLong("binding_time");
            tag.putLong("binding_time", ++time);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (time >= 24000L) {
               vars.player_magic_attributes_sword = true;
               vars.syncPlayerVariables(player);
               level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
         }

         if (isAvalonActivated(stack)) {
            applyPlayerAvalonEffects(player);
         } else if (player.getPersistentData().getLong(LEGACY_PLAYER_AVALON_UNTIL_TAG) > player.level().getGameTime()) {
            activateAvalonStack(stack);
            player.getPersistentData().remove(LEGACY_PLAYER_AVALON_UNTIL_TAG);
            applyPlayerAvalonEffects(player);
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      if (!player.isCrouching()) {
         return InteractionResultHolder.pass(stack);
      }
      if (!level.isClientSide) {
         activateAvalonStack(stack);
         applyPlayerAvalonEffects(player);
         if (player.level() instanceof ServerLevel serverLevel) {
            ArtoriaPendragonCombatHelper.spawnAvalonFx(player);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.35F);
         }
         player.getCooldowns().addCooldown(this, 200);
      }
      return InteractionResultHolder.success(stack);
   }

   private static boolean isAvalonActivated(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean(AVALON_ACTIVE_TAG);
   }

   private static void activateAvalonStack(ItemStack stack) {
      updateCustomData(stack, tag -> tag.putBoolean(AVALON_ACTIVE_TAG, true));
      stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

   private static void applyPlayerAvalonEffects(Player player) {
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 4, false, false, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false, true));
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<AvalonItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.model.new"));
      return PlayState.CONTINUE;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.avalon.desc").withStyle(ChatFormatting.GOLD));
      if (isAvalonActivated(stack)) {
         tooltip.add(Component.translatable("item.typemoonworld.avalon.active").withStyle(ChatFormatting.AQUA));
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private static CompoundTag customTag(ItemStack stack) {
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? null : data.copyTag();
   }

   private static void updateCustomData(ItemStack stack, TagUpdater updater) {
      CompoundTag tag = customTag(stack);
      if (tag == null) {
         tag = new CompoundTag();
      }
      updater.update(tag);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   @FunctionalInterface
   private interface TagUpdater {
      void update(CompoundTag tag);
   }
}
