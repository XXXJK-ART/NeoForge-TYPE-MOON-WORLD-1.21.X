package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/**
 * Keeps the legacy self-knowledge storage compatible with the namespaced
 * ResourceLocation values written by the newer magic API.
 */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class MagicKnowledgeCompatibilityEvents {
    private MagicKnowledgeCompatibilityEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            normalizeAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            normalizeAndSync(player);
        }
    }

    private static void normalizeAndSync(ServerPlayer player) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        vars.ensureMagicSystemInitialized();

        boolean changed = normalizeLearnedMagics(vars);
        changed |= normalizeProficiencies(vars);
        changed |= normalizeLearningProgress(vars);
        changed |= normalizeWheelEntries(vars);
        changed |= normalizeCrestEntries(vars);

        if (changed) {
            vars.rebuildSelectedMagicsFromActiveWheel();
            vars.syncPlayerVariables(player);
            vars.syncRuntimeSelection(player);
            vars.syncModeState(player);
            TypeMoonAddon.LOGGER.debug("Normalized magic knowledge IDs for {}", player.getGameProfile().getName());
        }
    }

    private static boolean normalizeLearnedMagics(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.learned_magics == null || vars.learned_magics.isEmpty()) {
            return false;
        }

        boolean changed = false;
        Set<String> canonical = new LinkedHashSet<>();
        List<String> original = new ArrayList<>(vars.learned_magics);
        for (String raw : original) {
            String normalized = canonicalSelfMagicId(raw);
            if (normalized == null || normalized.isEmpty()) {
                continue;
            }
            changed |= !normalized.equals(raw);
            canonical.add(normalized);
        }
        if (canonical.size() != vars.learned_magics.size() || !canonical.equals(new LinkedHashSet<>(vars.learned_magics))) {
            vars.learned_magics.clear();
            vars.learned_magics.addAll(canonical);
            changed = true;
        }
        return changed;
    }

    private static boolean normalizeProficiencies(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.magic_proficiencies == null || vars.magic_proficiencies.isEmpty()) {
            return false;
        }

        boolean changed = false;
        Map<String, Double> normalized = new HashMap<>();
        for (Map.Entry<String, Double> entry : vars.magic_proficiencies.entrySet()) {
            String key = canonicalSelfMagicId(entry.getKey());
            if (key == null || key.isEmpty()) {
                changed = true;
                continue;
            }
            Double value = entry.getValue();
            double safeValue = value == null || !Double.isFinite(value) ? 0.0D : Math.max(0.0D, Math.min(100.0D, value));
            Double previous = normalized.put(key, Math.max(normalized.getOrDefault(key, 0.0D), safeValue));
            changed |= !key.equals(entry.getKey()) || previous != null || safeValue != (value == null ? 0.0D : value);
        }
        if (!normalized.equals(vars.magic_proficiencies)) {
            vars.magic_proficiencies.clear();
            vars.magic_proficiencies.putAll(normalized);
            changed = true;
        }
        return changed;
    }

    private static boolean normalizeWheelEntries(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.magic_wheels == null || vars.magic_wheels.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry : vars.magic_wheels) {
            if (entry == null || entry.magicId == null || entry.magicId.isEmpty()) {
                continue;
            }
            String normalized = canonicalSelfMagicId(entry.magicId);
            if (normalized != null && !normalized.equals(entry.magicId)) {
                entry.magicId = normalized;
                changed = true;
            }
        }
        return changed;
    }

    private static boolean normalizeLearningProgress(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.magic_learning_progress == null || vars.magic_learning_progress.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Map<String, Double> progress = new HashMap<>();
        Map<String, Long> lastGain = new HashMap<>();
        for (Map.Entry<String, Double> entry : vars.magic_learning_progress.entrySet()) {
            String key = canonicalSelfMagicId(entry.getKey());
            if (key == null || key.isEmpty()) {
                changed = true;
                continue;
            }
            progress.merge(key, entry.getValue() == null ? 0.0D : Math.max(0.0D, entry.getValue()), Math::max);
            if (!key.equals(entry.getKey())) changed = true;
            if (vars.magic_learning_progress_last_gain_tick.containsKey(entry.getKey())) {
                lastGain.merge(key, vars.magic_learning_progress_last_gain_tick.get(entry.getKey()), Math::max);
            }
        }
        changed |= !progress.equals(vars.magic_learning_progress);
        if (changed) {
            vars.magic_learning_progress.clear();
            vars.magic_learning_progress.putAll(progress);
            vars.magic_learning_progress_last_gain_tick.clear();
            vars.magic_learning_progress_last_gain_tick.putAll(lastGain);
        }
        return changed;
    }

    private static boolean normalizeCrestEntries(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.crest_entries == null || vars.crest_entries.isEmpty()) {
            return false;
        }

        boolean changed = false;
        for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry : vars.crest_entries) {
            if (entry == null || entry.magicId == null || entry.magicId.isEmpty()) {
                continue;
            }
            String normalized = canonicalSelfMagicId(entry.magicId);
            if (normalized != null && !normalized.equals(entry.magicId)) {
                entry.magicId = normalized;
                changed = true;
            }
        }
        return changed;
    }

    private static String canonicalSelfMagicId(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty() || value.indexOf(':') < 0) {
            return value;
        }

        ResourceLocation id = ResourceLocation.tryParse(value.toLowerCase(Locale.ROOT));
        if (id == null || (!TYPE_MOON_WORLD.MOD_ID.equals(id.getNamespace())
                && !TypeMoonAddon.MOD_ID.equals(id.getNamespace()))) {
            return value;
        }
        return id.getPath();
    }
}
