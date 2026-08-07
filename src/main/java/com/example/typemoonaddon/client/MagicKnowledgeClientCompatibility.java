package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class MagicKnowledgeClientCompatibility {
    private static boolean loggedKnowledgeState;

    private MagicKnowledgeClientCompatibility() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.tickCount % 10 != 0) {
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(
                TypeMoonWorldModVariables.PLAYER_VARIABLES);
        normalizeLearnedMagics(vars);
        logKnowledgeStateOnce(vars);
    }

    private static void logKnowledgeStateOnce(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (loggedKnowledgeState) {
            return;
        }
        loggedKnowledgeState = true;

        Set<String> addonIds = com.example.typemoonaddon.magic.AddonMagicRegistration.registeredMagicIds();
        List<String> learnedAddonIds = vars.learned_magics.stream()
                .filter(addonIds::contains)
                .sorted()
                .toList();
        List<String> missingDefinitions = addonIds.stream()
                .filter(id -> !MagicDefinitionRegistry.contains(id))
                .sorted()
                .toList();
        String visibleDefinitions = MagicDefinitionRegistry.all().keySet().stream()
                .filter(id -> addonIds.stream().anyMatch(addonId -> id.equals(addonId) || id.endsWith(":" + addonId)))
                .sorted()
                .collect(Collectors.joining(", "));
        TypeMoonAddon.LOGGER.info(
                "Client magic-knowledge state: registeredAddonIds={}, learnedAddonIds={}, missingDefinitions={}, visibleDefinitions=[{}]",
                addonIds.size(), learnedAddonIds, missingDefinitions, visibleDefinitions);
    }

    private static void normalizeLearnedMagics(TypeMoonWorldModVariables.PlayerVariables vars) {
        if (vars.learned_magics == null || vars.learned_magics.isEmpty()) {
            return;
        }

        Set<String> normalized = new LinkedHashSet<>();
        List<String> original = new ArrayList<>(vars.learned_magics);
        for (String magicId : original) {
            String canonical = canonicalSelfMagicId(magicId);
            if (canonical != null && !canonical.isEmpty()) {
                normalized.add(canonical);
            }
        }
        if (normalized.size() != vars.learned_magics.size()
                || !normalized.equals(new LinkedHashSet<>(vars.learned_magics))) {
            vars.learned_magics.clear();
            vars.learned_magics.addAll(normalized);
        }
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
