package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.detection.DetectionService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class DetectionMagic {
    public static final String MAGIC_ID = "detection";

    private DetectionMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null || !MAGIC_ID.equals(context.magicId())) {
            return player == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.rejected("wrong_magic");
        }
        DetectionService.toggle(player);
        return MagicExecutionResult.STARTED;
    }
}
