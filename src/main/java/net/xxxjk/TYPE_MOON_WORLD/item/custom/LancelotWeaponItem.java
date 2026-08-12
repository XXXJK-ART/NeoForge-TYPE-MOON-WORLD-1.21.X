package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.LancelotWeaponRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class LancelotWeaponItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   public enum WeaponType {
      AROUNDIGHT("aroundight"),
      IRON_ROD("lancelot_iron_rod");

      private final String id;

      WeaponType(String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }
   }

   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final WeaponType weaponType;

   public LancelotWeaponItem(WeaponType weaponType, Properties properties) {
      super(Tiers.NETHERITE, properties);
      this.weaponType = weaponType;
   }

   public WeaponType weaponType() {
      return this.weaponType;
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      LancelotCombatHelper.applyWeaponHit(attacker, target, stack);
      return super.hurtEnemy(stack, target, attacker);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld." + this.weaponType.id() + ".desc").withStyle(ChatFormatting.DARK_PURPLE));
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return LancelotCombatHelper.isKnightOfOwner(stack) || super.isFoil(stack);
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private LancelotWeaponRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new LancelotWeaponRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<LancelotWeaponItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
