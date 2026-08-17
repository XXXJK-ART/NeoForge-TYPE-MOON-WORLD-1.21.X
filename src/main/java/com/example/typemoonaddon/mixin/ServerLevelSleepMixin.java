package com.example.typemoonaddon.mixin;

import com.example.typemoonaddon.magic.BlackShadowNightService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Defers vanilla's all-players-asleep transition while a night hunt owns that level's sleep cycle. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSleepMixin {
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"
        )
    )
    private boolean typemoonaddon$deferSleepSkip(SleepStatus sleepStatus, int requiredPercentage) {
        ServerLevel level = (ServerLevel)(Object)this;
        return !BlackShadowNightService.blocksSleepSkip(level)
            && sleepStatus.areEnoughSleeping(requiredPercentage);
    }
}
