package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.antores.AntoresService;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class AntoresEvents {
    private AntoresEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            AntoresService.tickCurse(living);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        AntoresService.clearCurse(event.getEntity());
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            AntoresService.clearCurse(living);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        AntoresService.clearRuntimeState();
    }
}
