package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ServantCardArmorRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardRegistry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ServantCardArmorItem extends ArmorItem implements GeoItem {
   private final String servantId;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ServantCardArmorItem(Properties properties, String servantId, EquipmentSlot slot) {
      super(ArmorMaterials.LEATHER, typeFor(slot), properties);
      this.servantId = servantId;
   }

   public String servantId() {
      return this.servantId;
   }

   public boolean hasRealArmorModel() {
      ServantCardRegistry.Entry entry = ServantCardRegistry.byId(this.servantId);
      return entry != null && entry.hasRealArmor();
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ServantCardArmorRenderer renderer;

         @Override
         @Nullable
         public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
            if (!ServantCardArmorItem.this.hasRealArmorModel()) {
               return null;
            }
            if (this.renderer == null) {
               this.renderer = new ServantCardArmorRenderer();
            }
            return this.renderer;
         }
      });
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      if (this.hasRealArmorModel()) {
         controllers.add(new AnimationController<>(this, "servant_card_armor", 0, this::predicate));
      }
   }

   private PlayState predicate(AnimationState<ServantCardArmorItem> state) {
      String animation = "1";
      if ("medea".equals(this.servantId)) {
         animation = isMedeaFlying(state) ? "fly" : "standing";
      } else if ("enkidu".equals(this.servantId) || "cu_chulainn".equals(this.servantId)) {
         animation = "animation";
      }
      state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
      return PlayState.CONTINUE;
   }

   private boolean isMedeaFlying(AnimationState<ServantCardArmorItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      if (!(entity instanceof LivingEntity living)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = living.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "medea".equals(vars.servant_card_id) && vars.servant_card_flying;
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private static ArmorItem.Type typeFor(EquipmentSlot slot) {
      return switch (slot) {
         case HEAD -> ArmorItem.Type.HELMET;
         case CHEST -> ArmorItem.Type.CHESTPLATE;
         case LEGS -> ArmorItem.Type.LEGGINGS;
         case FEET -> ArmorItem.Type.BOOTS;
         default -> ArmorItem.Type.CHESTPLATE;
      };
   }
}
