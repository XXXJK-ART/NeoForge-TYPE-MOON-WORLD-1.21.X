package com.captain.servant.mixin.client;

import com.captain.servant.network.SpiritualizationClientState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class ClientSpiritualizationMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void fatesupport$moveHorizontallyThroughBlocks(
            MoverType moverType,
            Vec3 movement,
            CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof LocalPlayer player)
                || !SpiritualizationClientState.isRestricted(player.getUUID())
                || !player.isShiftKeyDown()
                || !player.onGround()
                || movement.y > 0.0D
                || player.isPassenger()) {
            return;
        }

        player.setPos(player.getX() + movement.x, player.getY(), player.getZ() + movement.z);
        callbackInfo.cancel();
    }
}
