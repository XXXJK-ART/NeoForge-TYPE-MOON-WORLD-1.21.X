package com.example.typemoonaddon.magic;

import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class OriasMagic {
    public static final String MAGIC_ID = "orias";

    private OriasMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        // Press/release charging is handled by the addon-owned server state machine.
        return context.asServerPlayer() == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.FAILED;
    }
}
