package com.captain.servant.lancelot;

/** Shared player/NPC summon lifecycle, in server ticks. */
public final class KnightArmoryRules {
    public static final int WEAPON_DURATION_TICKS = 600;
    public static final int A10_DURATION_TICKS = 300;
    public static final int A10_LAUNCH_GRACE_TICKS = 20;
    public static final int NPC_HEAVY_WEAPON_LOCK_TICKS = 1200;

    private KnightArmoryRules() { }

    public static boolean shouldExplode(long now, long expires, int age,
                                        boolean horizontalCollision, boolean verticalCollision) {
        return now >= expires || age >= A10_LAUNCH_GRACE_TICKS
                && (horizontalCollision || verticalCollision);
    }

    public static boolean npcCanUseAtHealth(KnightArmoryEntry entry, float health, float maxHealth) {
        return entry != KnightArmoryEntry.MINIGUN && entry != KnightArmoryEntry.A10
                || maxHealth > 0.0F && health <= maxHealth * 0.5F;
    }

    /**
     * Chooses one NPC armory mode for a decision window. The iron rod remains
     * the normal state; aircraft and minigun are mutually exclusive bursts.
     */
    public static KnightArmoryEntry chooseNpcArmoryEntry(boolean lowHealth,
                                                          boolean a10Available,
                                                          boolean minigunAvailable,
                                                          float roll) {
        if (!lowHealth || (!a10Available && !minigunAvailable)) {
            return KnightArmoryEntry.IRON_ROD;
        }
        float normalized = Math.max(0.0F, Math.min(0.999999F, roll));
        if (a10Available && minigunAvailable) {
            if (normalized < 0.18F) return KnightArmoryEntry.A10;
            if (normalized < 0.48F) return KnightArmoryEntry.MINIGUN;
            return KnightArmoryEntry.IRON_ROD;
        }
        if (a10Available) {
            return normalized < 0.25F ? KnightArmoryEntry.A10 : KnightArmoryEntry.IRON_ROD;
        }
        return normalized < 0.40F ? KnightArmoryEntry.MINIGUN : KnightArmoryEntry.IRON_ROD;
    }

    public static boolean npcCanChooseHeavyWeapon(boolean minigunHeld,
                                                   boolean activeA10,
                                                   long now,
                                                   long heavyWeaponLockUntil) {
        return !minigunHeld && !activeA10 && now >= heavyWeaponLockUntil;
    }

}
