package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.SakuraBlackShadowEntity;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.example.typemoonaddon.magic.BlackShadowNightService;
import com.example.typemoonaddon.magic.CursedArmorService;
import com.example.typemoonaddon.magic.SakuraBlackMudHuntService;
import com.example.typemoonaddon.magic.SakuraBlackMudService;
import com.example.typemoonaddon.magic.SakuraGrailErosionService;
import com.example.typemoonaddon.magic.SakuraPollutionService;
import com.example.typemoonaddon.magic.SakuraShadowArtService;
import com.example.typemoonaddon.magic.SakuraShadowBindingService;
import com.example.typemoonaddon.magic.SakuraShadowMaterializationService;
import com.example.typemoonaddon.magic.SakuraShadowTransferService;
import com.example.typemoonaddon.magic.SakuraSummonBlackMudService;
import com.example.typemoonaddon.magic.SakuraTypeMoonIntegration;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class SakuraBehaviorEvents {
    private SakuraBehaviorEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SakuraShadowMaterializationService.maintainHeld(player);
            if (player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get()).tickProtection()) {
                com.example.typemoonaddon.registry.AddonAttachments.sync(player, com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        BlackShadowNightService.tick(event.getServer());
        SakuraShadowMaterializationService.tick(event.getServer());
        event.getServer().getAllLevels().forEach(SakuraBlackMudService::tick);
        event.getServer().getAllLevels().forEach(GillesDeRaisCombatHelper::tickPollutionZones);
        SakuraSummonBlackMudService.tick(event.getServer());
        SakuraBlackMudHuntService.tick(event.getServer());
        SakuraShadowBindingService.tick(event.getServer());
        SakuraShadowTransferService.tick(event.getServer());
        SakuraShadowArtService.tick(event.getServer());
        SakuraGrailErosionService.tick(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            CursedArmorService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SakuraShadowTransferService.interruptOnDamage(player);
            var data = player.getData(com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE.get());
            float remaining = data.absorbProtectionDamage(event.getAmount());
            if (remaining <= 0.0F) {
                event.setAmount(0.0F);
                event.setCanceled(true);
                com.example.typemoonaddon.registry.AddonAttachments.sync(player, com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE);
                return;
            }
            if (remaining < event.getAmount()) {
                event.setAmount(remaining);
                com.example.typemoonaddon.registry.AddonAttachments.sync(player, com.example.typemoonaddon.registry.AddonAttachments.IMAGINARY_SPACE);
            }
        }
        if (SakuraShadowBindingService.absorbDamage(event)) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F) {
            SakuraPollutionService.rememberServantDamage(event.getEntity(), event.getSource());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        SakuraPollutionService.entityDied(event.getEntity(), event.getSource());
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof SakuraShadowFamiliarEntity) {
            SakuraShadowMaterializationService.entityLeavingLevel(event.getEntity());
        }
        if (event.getEntity() instanceof SakuraBlackShadowEntity blackShadow) {
            BlackShadowNightService.entityLeavingLevel(blackShadow);
        }
        SakuraShadowBindingService.entityLeavingLevel(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerAttemptingSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == null) {
            BlackShadowNightService.playerAttemptingSleep(event.getEntity(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SakuraShadowMaterializationService.playerUnavailable(player);
            SakuraShadowTransferService.playerUnavailable(player);
            SakuraSummonBlackMudService.playerUnavailable(player);
            SakuraBlackMudHuntService.playerUnavailable(player);
            SakuraShadowArtService.playerUnavailable(player);
            BlackShadowNightService.playerUnavailable(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SakuraShadowMaterializationService.playerUnavailable(player);
            SakuraShadowTransferService.playerUnavailable(player);
            SakuraSummonBlackMudService.playerUnavailable(player);
            SakuraBlackMudHuntService.playerUnavailable(player);
            SakuraShadowArtService.playerUnavailable(player);
            BlackShadowNightService.playerUnavailable(player);
            syncSakuraState(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncSakuraState(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncSakuraState(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BlackShadowNightService.serverStopping(event.getServer());
        SakuraShadowMaterializationService.serverStopping(event.getServer());
        SakuraSummonBlackMudService.serverStopping(event.getServer());
        SakuraBlackMudHuntService.serverStopping(event.getServer());
        SakuraShadowBindingService.serverStopping(event.getServer());
        SakuraShadowTransferService.serverStopping();
        SakuraShadowArtService.serverStopping();
    }

    private static void syncSakuraState(ServerPlayer player) {
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.learned()) {
            SakuraTypeMoonIntegration.ensureImaginaryAttribute(player);
            SakuraTypeMoonIntegration.ensureGrailWormPower(player);
            if (data.forbiddenMagicUnlocked()) {
                SakuraTypeMoonIntegration.ensureForbiddenMagicKnowledge(player);
            }
            if (data.shadowArtUnlocked()) {
                SakuraTypeMoonIntegration.ensureShadowArtKnowledge(player);
            }
        }
        SakuraGrailErosionService.ensureCrestWormExpelled(player);
        CursedArmorService.beginFormation(player);
        CursedArmorService.sync(player);
    }
}
