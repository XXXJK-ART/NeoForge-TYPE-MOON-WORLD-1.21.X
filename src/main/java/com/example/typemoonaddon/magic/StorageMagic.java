package com.example.typemoonaddon.magic;

import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class StorageMagic {
    public static final String MAGIC_ID = "storage";

    private StorageMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        // Storage input is handled by the addon-owned press/release state machine.
        return context.asServerPlayer() == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.FAILED;
    }
}
