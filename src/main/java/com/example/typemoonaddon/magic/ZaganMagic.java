package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.zagan.ZaganService;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class ZaganMagic {
    public static final String MAGIC_ID = "zagan";

    private ZaganMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        if (!MAGIC_ID.equals(context.magicId())) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        return ZaganService.cast(player)
                ? MagicExecutionResult.SUCCESS
                : MagicExecutionResult.FAILED;
    }
}
