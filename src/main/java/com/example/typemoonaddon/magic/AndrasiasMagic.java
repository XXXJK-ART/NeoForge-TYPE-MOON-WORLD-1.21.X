package com.example.typemoonaddon.magic;

import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.api.MagicExecutionResult;

/** Press/release input for the two-stage ground-rift spell is handled by the addon state machine. */
public final class AndrasiasMagic {
    public static final String MAGIC_ID = "andrasias";

    private AndrasiasMagic() {
    }

    public static MagicExecutionResult execute(MagicExecutionContext context) {
        if (context.asServerPlayer() == null) {
            return MagicExecutionResult.NOT_HANDLED;
        }
        // The normal one-shot CastMagicMessage is intentionally ignored. The
        // client-side press/release payload drives the server charge lifecycle.
        return MagicExecutionResult.FAILED;
    }
}
