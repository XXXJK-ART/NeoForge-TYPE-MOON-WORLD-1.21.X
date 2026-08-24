package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.AddonRegistrar;
import net.xxxjk.typemoonworld.api.MasterProfileData;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class MatouKariyaMasterIntegration {
    private MatouKariyaMasterIntegration() {
    }

    public static void register() {
        AddonRegistrar addon = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID);
        boolean registered = addon.masters().register(
                new MasterProfileData(
                        MatouKariyaMasterProfile.ID,
                        "item.typemoonworld.master_card_matou_kariya",
                        "kariya",
                        50.0D,
                        1.0D,
                        14
                ),
                MatouKariyaMasterProfile::initialize
        );
        if (!registered) {
            TypeMoonAddon.LOGGER.warn("Matou Kariya master profile registration was rejected or duplicated");
        }
    }
}
