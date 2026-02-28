package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;

public final class GemComboTracker {
    private static final String KEY_LAST_TYPE = "TypeMoonGemComboLastType";
    private static final String KEY_LAST_TICK = "TypeMoonGemComboLastTick";
    private static final String KEY_CHAIN_COUNT = "TypeMoonGemComboChainCount";
    private static final String KEY_CHAIN_MULTIPLIER_SUM = "TypeMoonGemComboMultiplierSum";
    private static final int WINDOW_TICKS = 40; // 2 seconds

    private GemComboTracker() {
    }

    public static ComboState recordUse(ServerPlayer player, GemType gemType, double qualityMultiplier, boolean upwardRelease) {
        CompoundTag tag = player.getPersistentData();
        String lastType = tag.getString(KEY_LAST_TYPE);
        long lastTick = tag.getLong(KEY_LAST_TICK);
        int chain = tag.getInt(KEY_CHAIN_COUNT);
        double sumMultiplier = tag.contains(KEY_CHAIN_MULTIPLIER_SUM) ? tag.getDouble(KEY_CHAIN_MULTIPLIER_SUM) : 0.0D;
        long now = player.level().getGameTime();
        boolean requireUpward = requiresUpwardRelease(gemType);

        if (requireUpward && !upwardRelease) {
            reset(player);
            return new ComboState(0, getThreshold(gemType), false, 0.0D);
        }

        if (gemType.name().equals(lastType) && now - lastTick <= WINDOW_TICKS) {
            chain += 1;
            sumMultiplier += qualityMultiplier;
        } else {
            chain = 1;
            sumMultiplier = qualityMultiplier;
        }

        int threshold = getThreshold(gemType);
        boolean triggered = chain >= threshold;

        if (triggered) {
            double avgMultiplier = chain <= 0 ? 1.0D : (sumMultiplier / chain);
            reset(player);
            return new ComboState(0, threshold, true, avgMultiplier);
        }

        tag.putString(KEY_LAST_TYPE, gemType.name());
        tag.putLong(KEY_LAST_TICK, now);
        tag.putInt(KEY_CHAIN_COUNT, chain);
        tag.putDouble(KEY_CHAIN_MULTIPLIER_SUM, sumMultiplier);
        return new ComboState(chain, threshold, false, 0.0D);
    }

    public static void reset(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        tag.remove(KEY_LAST_TYPE);
        tag.remove(KEY_LAST_TICK);
        tag.remove(KEY_CHAIN_COUNT);
        tag.remove(KEY_CHAIN_MULTIPLIER_SUM);
    }

    private static boolean requiresUpwardRelease(GemType gemType) {
        return gemType == GemType.EMERALD
                || gemType == GemType.SAPPHIRE
                || gemType == GemType.TOPAZ;
    }

    public static int getThreshold(GemType gemType) {
        return switch (gemType) {
            case EMERALD -> 2;
            case RUBY, SAPPHIRE, TOPAZ, WHITE_GEMSTONE, CYAN, BLACK_SHARD -> 3;
        };
    }

    public record ComboState(int chainCount, int threshold, boolean triggered, double averageMultiplier) {
    }
}
