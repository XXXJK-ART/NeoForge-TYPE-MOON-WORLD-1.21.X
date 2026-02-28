package net.xxxjk.TYPE_MOON_WORLD.utils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.WeakHashMap;

public final class MerlinWorldEventLimiter {
    public static final int DAILY_LIMIT = 5;
    private static final Map<MinecraftServer, BudgetState> BUDGETS = new WeakHashMap<>();

    private MerlinWorldEventLimiter() {
    }

    public static boolean tryConsume(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null || server.overworld() == null) {
            return false;
        }

        BudgetState state = BUDGETS.computeIfAbsent(server, s -> new BudgetState());
        long day = server.overworld().getGameTime() / 24000L;
        if (state.day != day) {
            state.day = day;
            state.used = 0;
        }

        if (state.used >= DAILY_LIMIT) {
            return false;
        }

        state.used++;
        return true;
    }

    private static final class BudgetState {
        long day = Long.MIN_VALUE;
        int used = 0;
    }
}
