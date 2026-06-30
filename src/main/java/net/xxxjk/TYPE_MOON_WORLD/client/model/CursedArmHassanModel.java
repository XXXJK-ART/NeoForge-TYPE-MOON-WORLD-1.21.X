package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class CursedArmHassanModel extends BaseServantModel<CursedArmHassanEntity> {
   public CursedArmHassanModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/cursed_arm_hassan.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/cursed_arm_hassan.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/cursed_arm_hassan.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(CursedArmHassanEntity entity, long instanceId, AnimationState<CursedArmHassanEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      setBoneVisible("cape", !entity.hasNoCape());
      setBoneVisible("cloak", !entity.hasNoCape());
      setBoneVisible("mantle", !entity.hasNoCape());
      setBoneVisible("bandage", !entity.hasNoBandages());
      setBoneVisible("bandages", !entity.hasNoBandages());
      setBoneVisible("right_bandage", !entity.hasNoBandages());
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         float scale = entity.getVisualScale();
         root.setScaleX(scale);
         root.setScaleY(scale);
         root.setScaleZ(scale);
      }
      stretchRightArmTowardZabaniyaTarget(entity);
   }

   private void stretchRightArmTowardZabaniyaTarget(CursedArmHassanEntity entity) {
      GeoBone rightArm = this.getAnimationProcessor().getBone("right arm");
      if (rightArm == null) {
         return;
      }
      int targetId = entity.getZabaniyaTargetId();
      if (targetId <= 0) {
         rightArm.setScaleX(1.0F);
         rightArm.setScaleY(1.0F);
         rightArm.setScaleZ(1.0F);
         return;
      }
      Entity target = entity.level().getEntity(targetId);
      if (target == null) {
         rightArm.setScaleX(1.0F);
         rightArm.setScaleY(1.0F);
         rightArm.setScaleZ(1.0F);
         return;
      }

      Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.62, 0.0);
      Vec3 targetCenter = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      Vec3 delta = targetCenter.subtract(origin);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float distance = (float)delta.length();
      float stretch = Mth.clamp(1.0F + distance * 0.45F, 1.0F, 4.75F);
      float targetYaw = (float)(-Mth.atan2(delta.x, delta.z) * Mth.RAD_TO_DEG);
      float relativeYaw = Mth.wrapDegrees(targetYaw - entity.getYRot() + 180.0F) * Mth.DEG_TO_RAD;
      float pitch = (float)(-Mth.atan2(delta.y, horizontal));

      rightArm.setScaleX(0.85F);
      rightArm.setScaleY(stretch);
      rightArm.setScaleZ(0.85F);
      rightArm.setRotX(-1.15F + pitch * 0.55F);
      rightArm.setRotY(relativeYaw);
   }

   private void setBoneVisible(String name, boolean visible) {
      GeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         float scale = visible ? 1.0F : 0.0F;
         bone.setScaleX(scale);
         bone.setScaleY(scale);
         bone.setScaleZ(scale);
      }
   }
}
