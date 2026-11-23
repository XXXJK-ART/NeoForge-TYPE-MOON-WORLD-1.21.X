package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.phys.Vec3;

/** Public E-A active effect; the accepting executor owns payment and cooldown. */
public final class DisengageService {
   public record Rank(double healing, double retreat, double speed, int mana, int cooldown) { }
   private static final ResourceLocation SPEED = ResourceLocation.fromNamespaceAndPath("typemoonworld","disengage_speed");
   private static final String UNTIL = "TypeMoonDisengageUntil";
   private DisengageService() { }
   public static Rank rank(String rank) {
      return switch (rank) {
         case "E" -> new Rank(.06,2,.10,10,1400);
         case "D" -> new Rank(.08,2.5,.15,14,1200);
         case "C" -> new Rank(.10,3,.20,18,1000);
         case "B" -> new Rank(.12,3.5,.25,22,800);
         case "A" -> new Rank(.15,4,.30,26,600);
         default -> throw new IllegalArgumentException("Unsupported disengage rank: " + rank);
      };
   }
   public static void cleanse(LivingEntity owner) {
      for (var effect : List.copyOf(owner.getActiveEffects())) {
         if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) owner.removeEffect(effect.getEffect());
      }
      owner.clearFire();
   }
   public static void apply(LivingEntity owner, String rank) {
      Rank value = rank(rank);
      cleanse(owner);
      owner.heal((float)(owner.getMaxHealth() * value.healing()));
      Vec3 back = owner.getLookAngle().multiply(1,0,1).normalize().scale(-value.retreat());
      // Entity movement resolves voxel collision along the complete retreat.
      owner.move(MoverType.SELF,back);
      owner.hurtMarked = true;
      var speed = owner.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(SPEED);
         speed.addTransientModifier(new AttributeModifier(SPEED,value.speed(),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      owner.getPersistentData().putLong(UNTIL,owner.level().getGameTime()+60);
   }
   public static void tick(LivingEntity owner) {
      if (owner.getPersistentData().contains(UNTIL) && owner.level().getGameTime() >= owner.getPersistentData().getLong(UNTIL)) clear(owner);
   }
   public static void clear(LivingEntity owner) {
      var speed=owner.getAttribute(Attributes.MOVEMENT_SPEED);
      if(speed!=null) speed.removeModifier(SPEED);
      owner.getPersistentData().remove(UNTIL);
   }
}
