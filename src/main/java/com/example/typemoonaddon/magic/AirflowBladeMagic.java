package com.example.typemoonaddon.magic;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

/** Input-driven magic; press/release state is owned by AirflowBladeService. */
public final class AirflowBladeMagic {
    public static final String MAGIC_ID = "airflow_blade";

    private AirflowBladeMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        ServerPlayer player = context.asServerPlayer();
        if (player == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        return MAGIC_ID.equals(context.magicId())
                ? MagicExecutionResult.FAILED
                : MagicExecutionResult.NOT_HANDLED;
    }
}
