package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BizenNagamitsuRenderer;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BizenNagamitsuItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public BizenNagamitsuItem(Properties properties) {
      super(Tiers.NETHERITE, properties);
   }

   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private BizenNagamitsuRenderer renderer;

         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new BizenNagamitsuRenderer();
            }
            return this.renderer;
         }
      });
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<BizenNagamitsuItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("animation"));
      return PlayState.CONTINUE;
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.bizen_nagamitsu.desc").withStyle(ChatFormatting.GRAY));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!attacker.level().isClientSide() && attacker instanceof ServerPlayer player) {
         PlayerNoblePhantasmHelper.triggerTsubameOnHit(player, stack, target);
      }
      return super.hurtEnemy(stack, target, attacker);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
   }
}
