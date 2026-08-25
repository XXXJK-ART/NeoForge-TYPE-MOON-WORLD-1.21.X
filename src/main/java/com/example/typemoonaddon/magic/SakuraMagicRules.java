package com.example.typemoonaddon.magic;

import java.util.List;
import java.util.Set;

public final class SakuraMagicRules {
    public static final List<String> SAKURA_MAGIC_IDS = List.of(
            SakuraTypeMoonIntegration.IMAGINARY_STORAGE.toString(),
            SakuraTypeMoonIntegration.IMAGINARY_ABSORPTION.toString(),
            SakuraTypeMoonIntegration.SHADOW_MATERIALIZATION.toString(),
            SakuraTypeMoonIntegration.BLACK_MUD_CONTROL.toString(),
            SakuraTypeMoonIntegration.SUMMON_BLACK_MUD.toString(),
            SakuraTypeMoonIntegration.SHADOW_BINDING.toString(),
            SakuraTypeMoonIntegration.SHADOW_TRANSFER.toString(),
            SakuraTypeMoonIntegration.HEROIC_SPIRIT_DEVOURER.toString(),
            SakuraTypeMoonIntegration.FORBIDDEN_MAGIC.toString(),
            SakuraTypeMoonIntegration.SHADOW_ART.toString()
    );

    public static final Set<String> CREST_FORBIDDEN_MAGIC_IDS = Set.copyOf(SAKURA_MAGIC_IDS);

    private SakuraMagicRules() {
    }

    public static boolean isSakuraMagic(String magicId) {
        if (magicId == null || magicId.isBlank()) {
            return false;
        }
        return CREST_FORBIDDEN_MAGIC_IDS.contains(magicId) || CREST_FORBIDDEN_MAGIC_IDS.contains(shortId(magicId));
    }

    public static String shortId(String magicId) {
        if (magicId == null) {
            return "";
        }
        int split = magicId.indexOf(':');
        return split >= 0 ? magicId.substring(split + 1) : magicId;
    }
}
