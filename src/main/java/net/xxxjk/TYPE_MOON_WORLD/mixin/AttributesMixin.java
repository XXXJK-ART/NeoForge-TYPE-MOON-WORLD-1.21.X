package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Attributes.class)
public class AttributesMixin {
   private static final double TYPEMOONWORLD_MAX_HEALTH_CAP = 1000000.0;
   private static final double TYPEMOONWORLD_ARMOR_CAP = 10000.0;
   private static final double TYPEMOONWORLD_ARMOR_TOUGHNESS_CAP = 10000.0;

   @ModifyArg(
      method = "<clinit>",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/ai/attributes/RangedAttribute;<init>(Ljava/lang/String;DDD)V",
         ordinal = 0
      ),
      slice = @Slice(
         from = @At(value = "CONSTANT", args = "stringValue=generic.armor")
      ),
      index = 3
   )
   private static double typemoonworld$raiseArmorCap(double original) {
      return Math.max(original, TYPEMOONWORLD_ARMOR_CAP);
   }

   @ModifyArg(
      method = "<clinit>",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/ai/attributes/RangedAttribute;<init>(Ljava/lang/String;DDD)V",
         ordinal = 0
      ),
      slice = @Slice(
         from = @At(value = "CONSTANT", args = "stringValue=generic.armor_toughness")
      ),
      index = 3
   )
   private static double typemoonworld$raiseArmorToughnessCap(double original) {
      return Math.max(original, TYPEMOONWORLD_ARMOR_TOUGHNESS_CAP);
   }

   @ModifyArg(
      method = "<clinit>",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/ai/attributes/RangedAttribute;<init>(Ljava/lang/String;DDD)V",
         ordinal = 0
      ),
      slice = @Slice(
         from = @At(value = "CONSTANT", args = "stringValue=generic.max_health")
      ),
      index = 3
   )
   private static double typemoonworld$raiseMaxHealthCap(double original) {
      return Math.max(original, TYPEMOONWORLD_MAX_HEALTH_CAP);
   }
}
