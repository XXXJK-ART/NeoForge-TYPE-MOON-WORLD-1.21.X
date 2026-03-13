package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

         if (this.hasSaberMana(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20, 5, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20, 4, false, false));
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      if (!level.isClientSide) {
         if (this.hasSaberMana(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            player.getCooldowns().addCooldown(this, 200);
            return InteractionResultHolder.success(player.getItemInHand(usedHand));
         } else {
            player.displayClientMessage(Component.translatable("message.typemoonworld.avalon.no_saber_mana"), true);
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
         }
      } else {
         return InteractionResultHolder.pass(player.getItemInHand(usedHand));
      }
   }

   private boolean hasSaberMana(Player player) {
      return false;
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
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
