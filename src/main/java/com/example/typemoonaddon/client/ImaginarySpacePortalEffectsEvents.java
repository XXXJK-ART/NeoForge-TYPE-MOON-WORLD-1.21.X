package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/** Registers the custom dimension effect on the client mod event bus. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ImaginarySpacePortalEffectsEvents {
    private ImaginarySpacePortalEffectsEvents() {
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space_portal"),
                new ImaginarySpacePortalEffects());
    }
}
