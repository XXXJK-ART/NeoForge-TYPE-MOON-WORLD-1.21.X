package net.xxxjk.TYPE_MOON_WORLD.client.model;

import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public final class FanaticAssassinModel extends BaseServantModel<FanaticAssassinEntity> {
   private static final String ROBE = "\u888d\u5b50";
   private static final String RIGHT_CLOAK = "\u6597\u7bf72";
   private static final String LEFT_CLOAK = "\u6597\u7bf73";

   public FanaticAssassinModel() {
      super(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "geo/fanatic_assassin.geo.json"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "textures/entity/fanatic_assassin.png"),
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "animations/fanatic_assassin.animation.json")
      );
   }

   @Override
   public void setCustomAnimations(FanaticAssassinEntity entity, long instanceId,
                                   AnimationState<FanaticAssassinEntity> state) {
      super.setCustomAnimations(entity, instanceId, state);
      applyBaseProportions();
      resetScale("right arm");
      resetScale("bone4");
      resetScale("bone3");
      int technique = entity.getActiveTechnique();
      if (technique == FanaticAssassinEntity.TECHNIQUE_COMPUTER) {
         scale("right arm", 1.2F, 1.45F, 1.2F);
      } else if (technique == FanaticAssassinEntity.TECHNIQUE_HAIR) {
         scale("bone4", 1.08F, 1.35F, 1.08F);
         scale("bone3", 1.08F, 1.28F, 1.08F);
      } else if (technique == FanaticAssassinEntity.TECHNIQUE_TEMPERATURE) {
         scale("bone", 0.65F * 1.035F, 0.65F * 1.035F, 0.65F * 1.035F);
      }
   }

   private void applyBaseProportions() {
      GeoBone root = this.getAnimationProcessor().getBone("bone");
      if (root != null) {
         root.setPosY(-9.0F);
         root.setScaleX(0.65F);
         root.setScaleY(0.65F);
         root.setScaleZ(0.65F);
      }

      GeoBone head = this.getAnimationProcessor().getBone("head");
      if (head != null) {
         head.setHidden(false);
         head.setPosZ(0.5F);
      }

      scale(ROBE, 1.0F, 1.0F, 0.9F);
      scale("bone10", 1.0F, 1.0F, 1.1F);
      scale("bone16", 1.0F, 1.0F, 0.9F);
      scale("bone14", 1.0F, 1.0F, 1.0F);
      scale(RIGHT_CLOAK, 0.9F, 0.9F, 0.9F);
      scale(LEFT_CLOAK, 0.9F, 0.9F, 0.9F);
   }

   private void resetScale(String name) {
      scale(name, 1.0F, 1.0F, 1.0F);
   }

   private void scale(String name, float x, float y, float z) {
      GeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setScaleX(x);
         bone.setScaleY(y);
         bone.setScaleZ(z);
      }
   }
}
