package net.xxxjk.TYPE_MOON_WORLD.mixin;

import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {
   @ModifyConstant(method = "explode", constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 4.0F))
   private float typemoonworld$sanctifiedTntPower(float radius) {
      PrimedTnt tnt = (PrimedTnt)(Object)this;
      if (tnt.getPersistentData().getBoolean("TypeMoonSanctifiedTnt")
         || (tnt.level() instanceof ServerLevel level
         && net.xxxjk.TYPE_MOON_WORLD.magic.sanctification.SanctificationService.isSanctifiedBlock(level, tnt.blockPosition()))) {
         return radius * 1.5F;
      }
      return radius;
   }
}
