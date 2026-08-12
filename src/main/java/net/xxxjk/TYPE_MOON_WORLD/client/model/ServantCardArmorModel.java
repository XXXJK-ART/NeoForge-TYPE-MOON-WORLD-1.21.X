package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ServantCardArmorModel extends GeoModel<ServantCardArmorItem> {
   private static final ResourceLocation EMIYA_MODEL = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/servant_card_emiya_archer.geo.json");
   private static final ResourceLocation EMIYA_TEXTURE = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/models/armor/servant_card_emiya_archer.png");
   private static final ResourceLocation EMIYA_ANIMATION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/servant_card_emiya_archer.animation.json");
   private static final ResourceLocation MEDUSA_ANIMATION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/servant_card_medusa.animation.json");
   private static final ResourceLocation EMPTY_ANIMATION = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/empty.animation.json");

   @Override
   public ResourceLocation getModelResource(ServantCardArmorItem animatable) {
      String servantId = animatable == null ? "" : animatable.servantId();
      if (isHeadSlot(animatable) && usesFullHeadwearModel(servantId)) {
         return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "geo/servant_card_" + servantId + ".geo.json");
      }
      if (isHeadSlot(animatable) && hasDedicatedHeadModel(servantId)) {
         if ("artoria_pendragon".equals(servantId)) {
            return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
               "geo/servant_hair_artoria_pendragon.geo.json");
         }
         return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "geo/servant_card_" + servantId + "_head.geo.json");
      }
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "geo/servant_card_" + servantId + ".geo.json")
         : EMIYA_MODEL;
   }

   @Override
   public ResourceLocation getTextureResource(ServantCardArmorItem animatable) {
      String servantId = animatable == null ? "" : animatable.servantId();
      if (isHeadSlot(animatable) && usesFullHeadwearModel(servantId)) {
         return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "textures/models/armor/servant_card_" + servantId + ".png");
      }
      if (isHeadSlot(animatable) && hasDedicatedHeadModel(servantId)) {
         if ("artoria_pendragon".equals(servantId)) {
            return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
               "textures/entity/servant_hair_artoria_pendragon.png");
         }
         return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "textures/models/armor/servant_card_" + servantId + "_head.png");
      }
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "textures/models/armor/servant_card_" + servantId + ".png")
         : EMIYA_TEXTURE;
   }

   @Override
   public ResourceLocation getAnimationResource(ServantCardArmorItem animatable) {
      String servantId = animatable == null ? "" : animatable.servantId();
      if (isHeadSlot(animatable) && "medusa".equals(servantId)) {
         return MEDUSA_ANIMATION;
      }
      if (isHeadSlot(animatable)) {
         return EMPTY_ANIMATION;
      }
      return hasDedicatedArmor(servantId)
         ? ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "animations/servant_card_" + servantId + ".animation.json")
         : EMIYA_ANIMATION;
   }

   private static boolean hasDedicatedArmor(String servantId) {
      return switch (servantId) {
         case "emiya_archer", "enkidu", "cu_chulainn", "medea",
            "artoria_pendragon", "sasaki_kojiro", "medusa", "cursed_arm_hassan", "shadow_hassan", "heracles",
            "gilgamesh", "gilgamesh_caster", "gawain", "paracelsus", "li_shuwen", "oda_nobunaga", "ushiwakamaru_rider" -> true;
         case "fanatic_assassin", "arash", "nightingale", "zhao_yun_rider", "senko_muramasa" -> true;
         case "hundred_faces_hassan", "diarmuid_ua_duibhne", "lancelot_berserker" -> true;
         default -> false;
      };
   }

   private static boolean hasDedicatedHeadModel(String servantId) {
      return switch (servantId) {
         case "artoria_pendragon", "enkidu", "gilgamesh_caster", "li_shuwen",
            "medusa", "paracelsus", "sasaki_kojiro",
            "ushiwakamaru_rider", "zhao_yun_rider" -> true;
         default -> false;
      };
   }

   private static boolean usesFullHeadwearModel(String servantId) {
      return switch (servantId) {
         case "enkidu", "medusa", "oda_nobunaga", "paracelsus", "sasaki_kojiro",
            "ushiwakamaru_rider", "zhao_yun_rider", "gilgamesh_caster" -> true;
         default -> false;
      };
   }

   private static boolean isHeadSlot(ServantCardArmorItem animatable) {
      return animatable != null && animatable.armorSlot() == net.minecraft.world.entity.EquipmentSlot.HEAD;
   }

   @Override
   public void setCustomAnimations(ServantCardArmorItem animatable, long instanceId,
                                   AnimationState<ServantCardArmorItem> state) {
      if (animatable == null) {
         return;
      }
      EntityModelData entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
      if (entityData == null) {
         return;
      }
      if ("diarmuid_ua_duibhne".equals(animatable.servantId())) {
         applyDiarmuidArmorFit();
      }
      GeoBone head = this.getAnimationProcessor().getBone("armorHead");
      if (head != null) {
         float yawRad = Mth.clamp(entityData.netHeadYaw(), -40.0F, 40.0F) * (float)(Math.PI / 180.0);
         float pitchRad = Mth.clamp(entityData.headPitch(), -40.0F, 40.0F) * (float)(Math.PI / 180.0);
         head.setRotY(yawRad);
         head.setRotX(pitchRad);
      }
      if (!usesLongHairCounterRotation(animatable.servantId())) {
         return;
      }
      float pitchRad = Mth.clamp(entityData.headPitch(), -40.0F, 40.0F) * (float)(Math.PI / 180.0);
      // The hat/headpiece is a child of armorHead and follows the head. Long
      // hair must cancel that vertical pitch locally, otherwise it clips
      // through the face or hat when the servant looks up/down. Child bones
      // inherit the correction from their nearest hair root.
      counterRotateHair("hair", pitchRad, 1.25F);
      counterRotateHair("hair1", pitchRad, 1.35F);
      counterRotateHair("hair2", pitchRad, 1.35F);
   }

   private void applyDiarmuidArmorFit() {
      setScale("armorRightArm", 1.5F, 1.5F, 1.5F);
      setScale("armorLeftArm", 1.5F, 1.5F, 1.5F);
      setScale("armorRightLeg", 1.5F, 1.5F, 1.5F);
      setScale("armorLeftLeg", 1.5F, 1.5F, 1.5F);
      setScale("armorRightBoot", 1.5F, 1.5F, 1.5F);
      setScale("armorLeftBoot", 1.5F, 1.5F, 1.5F);
   }

   private void setScale(String boneName, float x, float y, float z) {
      GeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setScaleX(x);
         bone.setScaleY(y);
         bone.setScaleZ(z);
      }
   }

   private void counterRotateHair(String boneName, float pitchRad, float strength) {
      GeoBone bone = this.getAnimationProcessor().getBone(boneName);
      if (bone != null) {
         bone.setRotX(-pitchRad * strength);
      }
   }

   private static boolean usesLongHairCounterRotation(String servantId) {
      return switch (servantId) {
         case "enkidu", "medusa", "oda_nobunaga", "paracelsus" -> true;
         default -> false;
      };
   }
}
