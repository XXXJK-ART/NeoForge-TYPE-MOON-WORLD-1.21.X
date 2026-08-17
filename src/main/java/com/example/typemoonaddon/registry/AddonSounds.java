package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AddonSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, TypeMoonAddon.MOD_ID);

    /** 5.638-second lock-and-attack cue used by Demon God Gaze. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DEMON_GOD_GAZE = SOUNDS.register(
            "demon_god_gaze",
            key -> SoundEvent.createFixedRangeEvent(key, 96.0F));

    /** Local copies of the storage-box cues used by creature transport. */
    public static final DeferredHolder<SoundEvent, SoundEvent> IMAGINARY_STORAGE_START = SOUNDS.register(
            "imaginary_storage_start",
            key -> SoundEvent.createFixedRangeEvent(key, 48.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> IMAGINARY_STORAGE_END = SOUNDS.register(
            "imaginary_storage_end",
            key -> SoundEvent.createFixedRangeEvent(key, 48.0F));

    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_ATTACK = SOUNDS.register(
            "gilles_de_rais_caster_voice_attack",
            key -> SoundEvent.createFixedRangeEvent(key, 64.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_FAIL = SOUNDS.register(
            "gilles_de_rais_caster_voice_fail",
            key -> SoundEvent.createFixedRangeEvent(key, 64.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_VICTORY = SOUNDS.register(
            "gilles_de_rais_caster_voice_victory",
            key -> SoundEvent.createFixedRangeEvent(key, 64.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_NP = SOUNDS.register(
            "gilles_de_rais_caster_voice_np",
            key -> SoundEvent.createFixedRangeEvent(key, 96.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_SUMMON = SOUNDS.register(
            "gilles_de_rais_caster_voice_summon",
            key -> SoundEvent.createFixedRangeEvent(key, 64.0F));
    public static final DeferredHolder<SoundEvent, SoundEvent> GILLES_VOICE_GAZE = SOUNDS.register(
            "gilles_de_rais_caster_voice_gaze",
            key -> SoundEvent.createFixedRangeEvent(key, 64.0F));

    private AddonSounds() {
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
