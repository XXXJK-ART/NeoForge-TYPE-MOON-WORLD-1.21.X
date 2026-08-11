package net.xxxjk.TYPE_MOON_WORLD.item.custom;

import java.util.function.Consumer;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.ServantCardArmorRenderer;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
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
   private final EquipmentSlot slot;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public ServantCardArmorItem(Properties properties, String servantId, EquipmentSlot slot) {
      super(ArmorMaterials.LEATHER, typeFor(slot), properties);
      this.servantId = servantId;
      this.slot = slot;
   }

   public String servantId() {
      return this.servantId;
   }

   public EquipmentSlot armorSlot() {
      return this.slot;
   }

   public String servantId(ItemStack stack) {
      if (this.servantId != null && !this.servantId.isBlank()) return this.servantId;
      CustomData data = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? "" : data.copyTag().getString("tmw_servant_id");
   }

   public static ItemStack create(ItemStack stack, String servantId) {
      CompoundTag tag = new CompoundTag();
      tag.putString("tmw_servant_id", servantId == null ? "" : servantId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return stack;
   }

   public boolean hasRealArmorModel() {
      ServantCardRegistry.Entry entry = ServantCardRegistry.byId(this.servantId);
      return (entry != null && entry.hasRealArmor()) || switch (this.servantId) {
         case "artoria_pendragon", "sasaki_kojiro", "medusa", "cursed_arm_hassan", "shadow_hassan", "heracles",
            "enkidu",
            "gilgamesh", "gilgamesh_caster", "gawain", "paracelsus", "li_shuwen", "oda_nobunaga", "ushiwakamaru_rider" -> true;
         case "fanatic_assassin", "arash", "nightingale", "zhao_yun_rider", "senko_muramasa" -> true;
         case "hundred_faces_hassan" -> true;
         default -> false;
      };
   }

   @Override
   public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
      consumer.accept(new GeoRenderProvider() {
         private ServantCardArmorRenderer renderer;
         private final Map<LivingEntity, EnumMap<EquipmentSlot, ServantCardArmorRenderer>> entityRenderers = new WeakHashMap<>();

         @Override
         @Nullable
         public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
            if (!ServantCardArmorItem.this.hasRealArmorModel()) {
               return null;
            }
            if (livingEntity == null) {
               if (this.renderer == null) {
                  this.renderer = new ServantCardArmorRenderer();
               }
               return this.renderer;
            }
            EquipmentSlot slot = equipmentSlot == null ? ServantCardArmorItem.this.slot : equipmentSlot;
            EnumMap<EquipmentSlot, ServantCardArmorRenderer> renderers =
               this.entityRenderers.computeIfAbsent(livingEntity, ignored -> new EnumMap<>(EquipmentSlot.class));
            return renderers.computeIfAbsent(slot, ignored -> new ServantCardArmorRenderer());
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
      } else if ("medusa".equals(this.servantId)) {
         animation = isMedusaMysticEyesActive(state) ? "eyes_open" : "animation";
      } else if ("cursed_arm_hassan".equals(this.servantId)) {
         animation = hassanArmorAnimation(state);
      } else if ("li_shuwen".equals(this.servantId)) {
         animation = liShuwenArmorAnimation(state);
      } else if ("gilgamesh_caster".equals(this.servantId)) {
         animation = casterGilgameshArmorAnimation(state);
      } else if ("senko_muramasa".equals(this.servantId)) {
         animation = "animation";
      } else if ("hundred_faces_hassan".equals(this.servantId)) {
         animation = "1";
      } else if ("enkidu".equals(this.servantId) || "cu_chulainn".equals(this.servantId)
         || "artoria_pendragon".equals(this.servantId) || "sasaki_kojiro".equals(this.servantId)
         || "heracles".equals(this.servantId) || "gilgamesh".equals(this.servantId) || "gawain".equals(this.servantId)
         || "paracelsus".equals(this.servantId) || "oda_nobunaga".equals(this.servantId)) {
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

   private boolean isMedusaMysticEyesActive(AnimationState<ServantCardArmorItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      if (!(entity instanceof LivingEntity living)) {
         return false;
      }
      // NPC Medusa has no player-variable capability state.  Its synced entity
      // state is authoritative for the head armor animation on clients.
      if (living instanceof MedusaEntity medusa) {
         return medusa.isEyesReleased();
      }
      TypeMoonWorldModVariables.PlayerVariables vars = living.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "medusa".equals(vars.servant_card_id) && vars.servant_card_medusa_mystic_eyes_active;
   }

   private String hassanArmorAnimation(AnimationState<ServantCardArmorItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      if (!(entity instanceof LivingEntity living)) {
         return "cloak";
      }
      TypeMoonWorldModVariables.PlayerVariables vars = living.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed || !"cursed_arm_hassan".equals(vars.servant_card_id)) {
         return "cloak";
      }
      if (living.tickCount <= vars.servant_card_hassan_zabaniya_animation_until) {
         return vars.servant_card_hassan_cloak_broken ? "Cursed Arm No Cloak" : "Cursed Arm";
      }
      return vars.servant_card_hassan_cloak_broken ? "cloak out" : "cloak";
   }

   private String liShuwenArmorAnimation(AnimationState<ServantCardArmorItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      if (!(entity instanceof LivingEntity living)) {
         return "animation";
      }
      float ratio = living.getHealth() / Math.max(1.0F, living.getMaxHealth());
      if (ratio <= 0.333F) {
         return "Jacket";
      }
      return ratio <= 0.666F ? "sunglasses" : "animation";
   }

   private String casterGilgameshArmorAnimation(AnimationState<ServantCardArmorItem> state) {
      Entity entity = state.getData(DataTickets.ENTITY);
      if (!(entity instanceof LivingEntity living)) {
         return "standing";
      }
      TypeMoonWorldModVariables.PlayerVariables vars = living.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed && "gilgamesh_caster".equals(vars.servant_card_id) && vars.servant_card_flying) {
         return living.getDeltaMovement().horizontalDistanceSqr() > 0.01 ? "fly" : "float_idle";
      }
      return living.getDeltaMovement().horizontalDistanceSqr() > 0.012 ? "walk" : "standing";
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
