package com.captain.servant.lancelot;

import net.minecraft.resources.ResourceLocation;

public enum KnightArmoryEntry {
    IRON_ROD(0, "iron_rod", "typemoonworld:lancelot_iron_rod", null, 5, 10.0D),
    MP5(1, "mp5", "superbwarfare:mp_5", "superbwarfare:handgun_ammo", 60, 50.0D),
    MINIGUN(2, "minigun", "superbwarfare:minigun", "superbwarfare:heavy_ammo", 20, 80.0D),
    AWM(3, "awm", "superbwarfare:awm", "superbwarfare:sniper_ammo", 60, 50.0D),
    BARRETT(4, "barrett", "superbwarfare:m_98b", "superbwarfare:sniper_ammo", 60, 50.0D);

    private final int networkId;
    private final String key;
    private final ResourceLocation itemId;
    private final ResourceLocation ammoId;
    private final int cooldownSeconds;
    private final double manaCost;

    KnightArmoryEntry(
            int networkId,
            String key,
            String itemId,
            String ammoId,
            int cooldownSeconds,
            double manaCost
    ) {
        this.networkId = networkId;
        this.key = key;
        this.itemId = itemId == null ? null : ResourceLocation.parse(itemId);
        this.ammoId = ammoId == null ? null : ResourceLocation.parse(ammoId);
        this.cooldownSeconds = cooldownSeconds;
        this.manaCost = manaCost;
    }

    public int networkId() {
        return networkId;
    }

    public String key() {
        return key;
    }

    public ResourceLocation itemId() {
        return itemId;
    }

    public ResourceLocation ammoId() {
        return ammoId;
    }

    public int cooldownTicks() {
        return cooldownSeconds * 20;
    }

    public double manaCost() {
        return manaCost;
    }

    public int bit() {
        return 1 << networkId;
    }

    public static KnightArmoryEntry byNetworkId(int id) {
        for (KnightArmoryEntry entry : values()) {
            if (entry.networkId == id) {
                return entry;
            }
        }
        return null;
    }
}
