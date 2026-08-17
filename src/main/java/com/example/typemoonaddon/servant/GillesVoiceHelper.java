package com.example.typemoonaddon.servant;

import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.registry.AddonSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public final class GillesVoiceHelper {
    private static final String GLOBAL_VOICE_TICK_TAG = "TypeMoonVoiceGlobalTick";
    private static final String VOICE_LOCK_UNTIL_TAG = "TypeMoonVoiceLockUntil";
    private static final String CATEGORY_VOICE_TICK_PREFIX = "TypeMoonVoice.";
    private static final int GLOBAL_VOICE_COOLDOWN = 40;
    private static final int ATTACK_VOICE_COOLDOWN = 90;
    private static final int SPECIAL_VOICE_COOLDOWN = 200;
    private static final int VICTORY_VOICE_COOLDOWN = 320;
    private static final int FAIL_VOICE_COOLDOWN = 80;

    private GillesVoiceHelper() {
    }

    public static void tryPlayAttack(GillesDeRaisEntity servant) {
        if (servant == null || servant.getRandom().nextFloat() > 0.50F) {
            return;
        }
        playVoice(servant, "attack", ATTACK_VOICE_COOLDOWN, 1.05F, 0.92F, AddonSounds.GILLES_VOICE_ATTACK.get());
    }

    public static void tryPlayVictory(GillesDeRaisEntity servant, LivingEntity defeated) {
        if (servant == null || defeated == null || defeated == servant || defeated.isAlliedTo(servant)) {
            return;
        }
        playVoice(servant, "victory", VICTORY_VOICE_COOLDOWN, 1.1F, 0.92F, AddonSounds.GILLES_VOICE_VICTORY.get());
    }

    public static void tryPlayFail(GillesDeRaisEntity servant) {
        playVoice(servant, "fail", FAIL_VOICE_COOLDOWN, 1.05F, 0.88F, AddonSounds.GILLES_VOICE_FAIL.get());
    }

    public static void tryPlaySummon(GillesDeRaisEntity servant) {
        playVoice(servant, "gilles_summon", SPECIAL_VOICE_COOLDOWN, 1.0F, 0.9F, AddonSounds.GILLES_VOICE_SUMMON.get());
    }

    public static void tryPlayGaze(GillesDeRaisEntity servant) {
        playVoice(servant, "gilles_gaze", SPECIAL_VOICE_COOLDOWN, 1.0F, 0.95F, AddonSounds.GILLES_VOICE_GAZE.get());
    }

    public static void tryPlayNp(GillesDeRaisEntity servant) {
        playVoiceForced(servant, "gilles_np", 1.45F, 0.9F, AddonSounds.GILLES_VOICE_NP.get());
    }

    private static void playVoice(GillesDeRaisEntity servant, String category, int cooldownTicks, float volume, float pitch, SoundEvent sound) {
        if (servant == null || !(servant.level() instanceof ServerLevel serverLevel) || sound == null) {
            return;
        }

        CompoundTag data = servant.getPersistentData();
        long now = serverLevel.getGameTime();
        if (now < data.getLong(VOICE_LOCK_UNTIL_TAG)) {
            return;
        }
        if (now - data.getLong(GLOBAL_VOICE_TICK_TAG) < GLOBAL_VOICE_COOLDOWN) {
            return;
        }

        String categoryTag = CATEGORY_VOICE_TICK_PREFIX + category;
        if (now - data.getLong(categoryTag) < cooldownTicks) {
            return;
        }

        data.putLong(GLOBAL_VOICE_TICK_TAG, now);
        data.putLong(categoryTag, now);
        float finalPitch = pitch + (servant.getRandom().nextFloat() - 0.5F) * 0.08F;
        serverLevel.playSound(null, servant.getX(), servant.getY(), servant.getZ(), sound, SoundSource.HOSTILE, volume, finalPitch);
    }

    private static void playVoiceForced(GillesDeRaisEntity servant, String category, float volume, float pitch, SoundEvent sound) {
        if (servant == null || !(servant.level() instanceof ServerLevel serverLevel) || sound == null) {
            return;
        }

        CompoundTag data = servant.getPersistentData();
        long now = serverLevel.getGameTime();
        if (now < data.getLong(VOICE_LOCK_UNTIL_TAG)) {
            return;
        }
        data.putLong(GLOBAL_VOICE_TICK_TAG, now);
        data.putLong(CATEGORY_VOICE_TICK_PREFIX + category, now);
        float finalPitch = pitch + (servant.getRandom().nextFloat() - 0.5F) * 0.08F;
        serverLevel.playSound(null, servant.getX(), servant.getY(), servant.getZ(), sound, SoundSource.HOSTILE, volume, finalPitch);
    }
}
