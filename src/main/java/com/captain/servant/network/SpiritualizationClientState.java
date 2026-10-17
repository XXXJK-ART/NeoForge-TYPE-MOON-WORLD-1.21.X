package com.captain.servant.network;

import com.captain.servant.network.payload.SpiritualizationStatePayload;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpiritualizationClientState {
    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private SpiritualizationClientState() {
    }

    public static void apply(SpiritualizationStatePayload payload) {
        STATES.put(
                payload.playerId(),
                new State(payload.phase(), payload.invisible(), payload.restricted(), payload.cooldownTicks())
        );
    }

    public static boolean isInvisible(UUID playerId) {
        State state = STATES.get(playerId);
        return state != null && state.invisible();
    }

    public static boolean isRestricted(UUID playerId) {
        State state = STATES.get(playerId);
        return state != null && state.restricted();
    }

    public static int cooldown(UUID playerId) {
        State state = STATES.get(playerId);
        return state == null ? 0 : state.cooldownTicks();
    }

    public static int phase(UUID playerId) {
        State state = STATES.get(playerId);
        return state == null ? 0 : state.phase();
    }

    public static void clientTick() {
        STATES.replaceAll((playerId, state) -> state.cooldownTicks() <= 0
                ? state
                : new State(state.phase(), state.invisible(), state.restricted(), state.cooldownTicks() - 1));
    }

    public static void clear() {
        STATES.clear();
    }

    private record State(int phase, boolean invisible, boolean restricted, int cooldownTicks) {
    }
}
