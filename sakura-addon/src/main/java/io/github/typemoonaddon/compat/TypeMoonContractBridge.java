package io.github.typemoonaddon.compat;

import io.github.typemoonaddon.TypeMoonAddon;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Contract access limited to operations exposed by the public API. */
public final class TypeMoonContractBridge {
    public static void transferTo(ServerPlayer controller, LivingEntity servant) {
        if (controller == null || servant == null || controller == servant) {
            return;
        }
        var master = TypeMoonWorldApi.master(controller);
        if (!master.active() && !master.activate()) {
            return;
        }
        if (servant instanceof ServerPlayer cardServant && !master.bind(cardServant)) {
            TypeMoonAddon.LOGGER.warn("Public API rejected servant-card contract transfer to {}", cardServant.getUUID());
        }
    }

    @Nullable
    public static LivingEntity boundServant(ServerPlayer masterPlayer) {
        if (masterPlayer == null) {
            return null;
        }
        UUID bound = TypeMoonWorldApi.master(masterPlayer).boundServant();
        return bound == null ? null : masterPlayer.server.getPlayerList().getPlayer(bound);
    }

    public static boolean isContractedTo(ServerPlayer masterPlayer, LivingEntity servant) {
        if (masterPlayer == null || servant == null || masterPlayer == servant) {
            return false;
        }
        UUID bound = TypeMoonWorldApi.master(masterPlayer).boundServant();
        return bound != null && bound.equals(servant.getUUID());
    }

    private TypeMoonContractBridge() {
    }
}
