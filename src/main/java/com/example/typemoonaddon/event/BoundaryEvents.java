package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class BoundaryEvents {
    private BoundaryEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (BoundaryMagicIntegration.absorbDamage(event)) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }
}
