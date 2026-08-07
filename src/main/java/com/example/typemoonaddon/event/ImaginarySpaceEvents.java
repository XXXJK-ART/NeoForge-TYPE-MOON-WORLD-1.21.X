package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class ImaginarySpaceEvents {
    private ImaginarySpaceEvents() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ImaginarySpaceService.tickServer(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ImaginarySpaceService.tickServer(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.tickPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.onChangedDimension(player, event.getFrom(), event.getTo());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.onDeath(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            ImaginarySpaceService.onDeath(original);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ImaginarySpaceAttachments.PLAYER_STATE).clearSession();
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImaginarySpaceService.onLogin(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ImaginarySpaceService.DIMENSION.equals(event.getEntity().level().dimension())
                && ImaginarySpaceService.isBoundaryDamage(event.getSource())) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (ImaginarySpaceService.DIMENSION.equals(player.serverLevel().dimension())
                && ImaginarySpaceService.hasImaginaryDiveImmunity(player)
                && source.is(ImaginarySpaceService.WORLD_CORRECTION_DAMAGE)) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
        if (!ImaginarySpaceService.isEntryInvulnerable(player)) {
            return;
        }
        if (source.is(ImaginarySpaceService.WORLD_CORRECTION_DAMAGE)
                || source.is(DamageTypes.GENERIC_KILL)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setAmount(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && ImaginarySpaceService.isEntryInvulnerable(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !ImaginarySpaceService.isEntryInvulnerable(player)
                || ImaginarySpaceService.allowOuterGodEffect(player)) {
            return;
        }
        if (event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (ImaginarySpaceService.isTransientVictim(event.getEntity())) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ImaginarySpaceService.stopServer(event.getServer());
    }
}
