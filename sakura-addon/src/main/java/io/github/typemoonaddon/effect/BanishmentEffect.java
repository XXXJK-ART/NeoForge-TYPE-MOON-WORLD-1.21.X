package io.github.typemoonaddon.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Marker effect refreshed by a living-target absorption channel. */
public final class BanishmentEffect extends MobEffect {
    public BanishmentEffect() {
        super(MobEffectCategory.HARMFUL, 0x1B1028);
    }
}
