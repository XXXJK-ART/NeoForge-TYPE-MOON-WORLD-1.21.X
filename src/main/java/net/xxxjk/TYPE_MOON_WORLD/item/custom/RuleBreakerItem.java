package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.RuleBreakerRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaCombatHelper;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RuleBreakerItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public RuleBreakerItem(Properties properties) {
      super(Tiers.IRON, properties);
   }

   @Override
   public @NotNull ItemStack getDefaultInstance() {
      ItemStack stack = super.getDefaultInstance();
      if (!stack.isEnchanted()) {
         EnchantmentHelper.updateEnchantments(
            stack,
            mutable -> mutable.set(BuiltInRegistries.ENCHANTMENT.getHolderOrThrow(Enchantments.SHARPNESS), 3)
         );
      }
      return stack;
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      MedeaCombatHelper.applyRuleBreakerHit(target, attacker);
      return super.hurtEnemy(stack, target, attacker);
   }

   @Override
   public boolean isFoil(@NotNull ItemStack stack) {
      return true;
   }

   @Override
   public boolean isEnchantable(@NotNull ItemStack stack) {
      return true;
   }

   @Override
   public int getEnchantmentValue() {
      return 22;
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private RuleBreakerRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new RuleBreakerRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld.rule_breaker.desc").withStyle(ChatFormatting.DARK_PURPLE));
   }

   private PlayState predicate(AnimationState<RuleBreakerItem> state) {
      state.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }
}
