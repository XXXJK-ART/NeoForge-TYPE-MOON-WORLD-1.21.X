package net.xxxjk.TYPE_MOON_WORLD.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.Explosion;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterDamageRouter;
import org.spongepowered.asm.mixin.Mixin;

/** Explosion damage must be collected before either member's defenses or death cleanup run. */
@Mixin(Explosion.class)
public abstract class ExplosionCarryDamageMixin {
   @WrapMethod(method = "explode")
   private void typemoonworld$mergeCarryHits(Operation<Void> original) {
      try (var segment = ServantMasterDamageRouter.attackSegment()) { original.call(); }
   }
}
