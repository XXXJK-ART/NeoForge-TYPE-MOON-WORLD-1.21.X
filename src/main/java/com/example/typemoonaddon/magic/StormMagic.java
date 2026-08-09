package com.example.typemoonaddon.magic;

import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

public final class StormMagic {
    public static final String MAGIC_ID = "storm";
    private static volatile boolean registered;

    private StormMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        // The addon-owned press/release state machine performs this sustained cast.
        return context.asServerPlayer() == null ? MagicExecutionResult.NOT_HANDLED : MagicExecutionResult.FAILED;
    }

    public static boolean isRegistered() {
        return registered;
    }

    static void setRegistered(boolean value) {
        registered = value;
    }
}
