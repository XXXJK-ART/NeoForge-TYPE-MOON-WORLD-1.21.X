package io.github.typemoonaddon.registry;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.effect.BanishmentEffect;
import io.github.typemoonaddon.effect.BlackMudCorruptionEffect;
import io.github.typemoonaddon.shadowlogic.effect.ShadowBindingSlownessEffect;
import io.github.typemoonaddon.effect.SpiritualDamageEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<MobEffect, BanishmentEffect> BANISHMENT = EFFECTS.register(
        "banishment",
        BanishmentEffect::new
    );
    public static final DeferredHolder<MobEffect, ShadowBindingSlownessEffect> SHADOW_BINDING_SLOWNESS = EFFECTS.register(
        "shadow_binding_slowness",
        ShadowBindingSlownessEffect::new
    );
    public static final DeferredHolder<MobEffect, BlackMudCorruptionEffect> BLACK_MUD_CORRUPTION = EFFECTS.register(
        "black_mud_corruption",
        BlackMudCorruptionEffect::new
    );
    public static final DeferredHolder<MobEffect, SpiritualDamageEffect> SPIRITUAL_DAMAGE = EFFECTS.register(
        "spiritual_damage", SpiritualDamageEffect::new
    );

    private ModEffects() {
    }
}
