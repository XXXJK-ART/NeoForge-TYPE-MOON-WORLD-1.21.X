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
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.DiarmuidSpearRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardDiarmuidSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid.DiarmuidCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.DiarmuidUaDuibhneEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DiarmuidSpearItem extends SwordItem implements GeoItem, NoblePhantasmItem {
   public enum SpearType {
      GAE_DEARG("gae_dearg"),
      GAE_BUIDHE("gae_buidhe");

      private final String id;

      SpearType(String id) {
         this.id = id;
      }

      public String id() {
         return this.id;
      }
   }

   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final SpearType spearType;

   public DiarmuidSpearItem(SpearType spearType, Properties properties) {
      super(Tiers.NETHERITE, properties);
      this.spearType = spearType;
   }

   public SpearType spearType() {
      return this.spearType;
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (attacker instanceof DiarmuidUaDuibhneEntity diarmuid) {
         if (this.spearType == SpearType.GAE_DEARG) {
            DiarmuidCombatHelper.applyRedRoseHit(diarmuid, target);
         } else {
            DiarmuidCombatHelper.applyYellowRoseHit(diarmuid, target);
         }
      } else if (attacker instanceof net.minecraft.server.level.ServerPlayer player && ServantCardDiarmuidSkills.isActiveCard(player)) {
         ServantCardDiarmuidSkills.applySpearItemHit(player, target, this.spearType);
      }
      return super.hurtEnemy(stack, target, attacker);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("item.typemoonworld." + this.spearType.id() + ".desc").withStyle(ChatFormatting.GOLD));
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private DiarmuidSpearRenderer renderer;

         @Override
         public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
            if (this.renderer == null) {
               this.renderer = new DiarmuidSpearRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
   }

   private PlayState predicate(AnimationState<DiarmuidSpearItem> event) {
      event.getController().setAnimation(RawAnimation.begin().thenLoop("1"));
      return PlayState.CONTINUE;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
