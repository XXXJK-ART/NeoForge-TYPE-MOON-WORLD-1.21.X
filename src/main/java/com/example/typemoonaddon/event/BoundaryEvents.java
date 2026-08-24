package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.BoundaryMagicIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

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

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide()) {
            BoundaryMagicIntegration.enforceDefenseMovement(living);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && BoundaryMagicIntegration.blocksDefenseInteraction(player, Vec3.atCenterOf(event.getPos()))) {
            cancelInteraction(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && BoundaryMagicIntegration.blocksDefenseInteraction(player, Vec3.atCenterOf(event.getPos()))) {
            cancelInteraction(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player
                && BoundaryMagicIntegration.blocksDefenseInteraction(player, event.getTarget().getBoundingBox().getCenter())) {
            cancelInteraction(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player
                && BoundaryMagicIntegration.blocksDefenseInteraction(player, event.getTarget().getBoundingBox().getCenter())) {
            cancelInteraction(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && BoundaryMagicIntegration.blocksDefenseInteraction(player, event.getTarget().getBoundingBox().getCenter())) {
            event.setCanceled(true);
        }
    }

    private static void cancelInteraction(PlayerInteractEvent event) {
        ((ICancellableEvent) event).setCanceled(true);
    }
}
