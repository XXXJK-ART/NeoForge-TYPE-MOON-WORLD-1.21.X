package com.captain.servant.mixin;

import com.captain.servant.spiritual.SpiritualizationService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SpiritualizationMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void fatesupport$moveHorizontallyThroughBlocks(
            MoverType moverType,
            Vec3 movement,
            CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof ServerPlayer player)
                || !SpiritualizationService.shouldPassThrough(player)
                || movement.y > 0.0D) {
            return;
        }

        player.setPos(player.getX() + movement.x, player.getY(), player.getZ() + movement.z);
        callbackInfo.cancel();
    }
}
