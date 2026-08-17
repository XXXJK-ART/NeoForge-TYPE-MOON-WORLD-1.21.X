package com.example.typemoonaddon.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Temporarily hides Archer's held equipment during the client render pass. */
public final class EmiyaShadowWeaponOcclusion {
    private static final Map<UUID, HiddenEquipment> HIDDEN = new HashMap<>();

    public static void apply(LivingEntity entity, float partialTick) {
        if (!EmiyaShadowPiercingAnimation.isPerformanceActive(entity, partialTick)
            || HIDDEN.containsKey(entity.getUUID())) {
            return;
        }
        HIDDEN.put(
            entity.getUUID(),
            new HiddenEquipment(entity, entity.getMainHandItem().copy(), entity.getOffhandItem().copy())
        );
        entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        entity.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    public static void restore(LivingEntity entity) {
        HiddenEquipment hidden = HIDDEN.remove(entity.getUUID());
        if (hidden != null) {
            restore(hidden);
        }
    }

    public static void restoreOutstanding() {
        for (HiddenEquipment hidden : HIDDEN.values()) {
            restore(hidden);
        }
        HIDDEN.clear();
    }

    private static void restore(HiddenEquipment hidden) {
        hidden.entity().setItemSlot(EquipmentSlot.MAINHAND, hidden.mainHand());
        hidden.entity().setItemSlot(EquipmentSlot.OFFHAND, hidden.offHand());
    }

    private record HiddenEquipment(LivingEntity entity, ItemStack mainHand, ItemStack offHand) {
    }

    private EmiyaShadowWeaponOcclusion() {
    }
}
