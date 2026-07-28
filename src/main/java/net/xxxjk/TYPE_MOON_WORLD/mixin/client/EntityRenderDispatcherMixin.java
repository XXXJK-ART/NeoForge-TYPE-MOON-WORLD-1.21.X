package net.xxxjk.TYPE_MOON_WORLD.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.client.ServantCardConcealmentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
   @ModifyExpressionValue(
      method = "render",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;displayFireAnimation()Z")
   )
   private boolean typemoonworld$hideConcealedFire(boolean original, Entity entity) {
      return original && !ServantCardConcealmentClient.isPerfectlyConcealed(entity);
   }
}
