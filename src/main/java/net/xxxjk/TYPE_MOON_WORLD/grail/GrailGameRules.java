package net.xxxjk.TYPE_MOON_WORLD.grail;

import net.minecraft.world.level.GameRules;

/** The public server rule controlling whether a full grail may accept wishes. */
public final class GrailGameRules {
    public static final GameRules.Key<GameRules.BooleanValue> ALLOW_GRAIL_WISH =
            GameRules.register("allowGrailWish", GameRules.Category.MISC, GameRules.BooleanValue.create(true));

    private GrailGameRules() {}
}
