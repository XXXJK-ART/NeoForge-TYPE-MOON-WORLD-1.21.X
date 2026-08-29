package io.github.typemoonaddon.event;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.item.AwakeningCatalystItems;
import io.github.typemoonaddon.compat.TypeMoonWeaponBalance;
import io.github.typemoonaddon.magic.ImaginaryStorageService;
import io.github.typemoonaddon.magic.ImaginaryShadowService;
import io.github.typemoonaddon.magic.BlackMudHuntService;
import io.github.typemoonaddon.magic.BlackMudService;
import io.github.typemoonaddon.magic.BlackShadowNightService;
import io.github.typemoonaddon.magic.HolyGrailService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.magic.ShadowMaterializationService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowBindingService;
import io.github.typemoonaddon.magic.ShadowTransferService;
import io.github.typemoonaddon.magic.SummonBlackMudService;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.magic.ForbiddenMagicService;
import io.github.typemoonaddon.magic.GrailErosionService;
import io.github.typemoonaddon.magic.CursedArmorService;
import io.github.typemoonaddon.magic.MagicOutputService;
import io.github.typemoonaddon.magic.EmiyaShadowRhoAiasService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import io.github.typemoonaddon.magic.SpiritualDamageService;
import io.github.typemoonaddon.magic.MatouSakuraMasterProfile;
import io.github.typemoonaddon.magic.VoidRingRegaliaService;
import io.github.typemoonaddon.entity.ShadowFamiliarEntity;
import io.github.typemoonaddon.network.AddonNetwork;
import io.github.typemoonaddon.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.Mob;
import net.xxxjk.typemoonworld.api.event.CommandSpellEvent;
import net.xxxjk.typemoonworld.api.event.MagicCastEvent;
import net.xxxjk.typemoonworld.api.event.ServantActionEvent;
import net.xxxjk.typemoonworld.api.event.MasterProfileEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void onMasterProfileApplied(MasterProfileEvent.Post event) {
        if (MatouSakuraMasterProfile.FHA_ID.equals(event.profileId())) {
            VoidRingRegaliaService.equip(event.player());
        }
    }

    @SubscribeEvent
    public static void onMasterProfileReleased(MasterProfileEvent.End event) {
        if (MatouSakuraMasterProfile.variant(event.profileId().toString()) != null) {
            MatouSakuraMasterProfile.restoreAddonState(event.player());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        BlackShadowNightService.tick(event.getServer());
        ImaginaryStorageService.tick(event.getServer());
        ImaginaryShadowService.tick(event.getServer());
        ShadowBindingService.tick(event.getServer());
        HolyGrailService.tick(event.getServer());
        ShadowMaterializationService.tick(event.getServer());
        BlackMudHuntService.tick(event.getServer());
        ShadowTransferService.tick(event.getServer());
        ForbiddenMagicService.tick(event.getServer());
        GrailErosionService.tick(event.getServer());
        ShadowArtService.tick(event.getServer());
        MagicOutputService.tick(event.getServer());
        EmiyaShadowRhoAiasService.tick(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            CursedArmorService.tick(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TypeMoonWeaponBalance.tick(player);
        }
        if (event.getEntity() instanceof LivingEntity living) {
            BlackMudService.maintainImmobilization(living);
            SpiritualDamageService.tick(living);
            PollutionService.tickEntity(living);
            PollutionService.beginManaSuppression(living);
        }
        ShadowBindingService.maintainTargetLock(event.getEntity());
        if (ShadowBindingService.interceptGateProjectile(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Projectile projectile
            && ShadowBindingService.interceptProjectile(projectile)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            PollutionService.finishManaSuppression(living);
        }
        if (event.getEntity() instanceof Mob mob) {
            BlackMudService.avoidNearbyMud(mob);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (PollutionService.cannotTargetAlly(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }
        LivingEntity forcedTarget = ShadowBindingService.forcedTarget(event.getEntity());
        if (forcedTarget != null && event.getNewAboutToBeSetTarget() != forcedTarget) {
            event.setNewAboutToBeSetTarget(forcedTarget);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TypeMoonIntegration.verifyRegistration();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BlackShadowNightService.serverStopping(event.getServer());
        ImaginaryStorageService.serverStopping(event.getServer());
        ImaginaryShadowService.serverStopping(event.getServer());
        ShadowBindingService.serverStopping(event.getServer());
        HolyGrailService.serverStopping(event.getServer());
        ShadowMaterializationService.serverStopping(event.getServer());
        BlackMudHuntService.serverStopping(event.getServer());
        ShadowTransferService.serverStopping();
        ShadowArtService.serverStopping();
        MagicOutputService.serverStopping();
        EmiyaShadowRhoAiasService.serverStopping(event.getServer());
        ForbiddenMagicService.serverStopping(event.getServer());
        SummonBlackMudService.serverStopping(event.getServer());
        PollutionService.serverStopping();
        AddonNetwork.serverStopping();
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            BlackShadowNightService.entityLeavingLevel(event.getEntity());
            ImaginaryStorageService.entityLeavingLevel(event.getEntity());
            ImaginaryShadowService.entityLeavingLevel(event.getEntity());
            ShadowBindingService.entityLeavingLevel(event.getEntity());
            ShadowMaterializationService.entityLeavingLevel(event.getEntity());
            PollutionService.clearManaSuppression(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureAttribute(player);
            CursedArmorService.beginFormation(player);
            CursedArmorService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackShadowNightService.playerUnavailable(player);
            BlackMudHuntService.playerChangedDimension(player);
            ShadowTransferService.playerChangedDimension(player);
            ForbiddenMagicService.playerChangedDimension(player);
            ShadowArtService.playerUnavailable(player);
            ensureAttribute(player);
            CursedArmorService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackShadowNightService.playerUnavailable(player);
            ImaginaryStorageService.playerLoggedOut(player);
            ImaginaryShadowService.playerLoggedOut(player);
            BlackMudHuntService.playerLoggedOut(player);
            HolyGrailService.playerLoggedOut(player);
            ShadowMaterializationService.playerLoggedOut(player);
            ShadowTransferService.playerLoggedOut(player);
            ForbiddenMagicService.playerUnavailable(player);
            SummonBlackMudService.playerLoggedOut(player);
            ShadowArtService.playerUnavailable(player);
            AddonNetwork.playerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onAwakeningCatalystPickup(ItemEntityPickupEvent.Post event) {
        if (event.getPlayer() instanceof ServerPlayer player
            && !AwakeningCatalystItems.isAwakened(player)
            && AwakeningCatalystItems.isCatalyst(event.getOriginalStack().getItem())) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.hint"), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerAttemptingSleep(CanPlayerSleepEvent event) {
        if (event.getProblem() == null) {
            BlackShadowNightService.playerAttemptingSleep(event.getEntity(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackShadowNightService.playerWokeUp(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onShadowBindingDamage(LivingIncomingDamageEvent event) {
        if (SpiritualDamageService.isCollapse(event.getSource())) {
            return;
        }
        if (MagicOutputService.isDamage(event.getSource())) {
            return;
        }
        if (HolyGrailService.blocksAction(event.getSource().getEntity())) {
            return;
        }
        if (ShadowBindingService.absorbBoundAttackerDamage(event)) {
            event.setAmount(0.0F);
            event.setInvulnerabilityTicks(0);
            event.setCanceled(true);
            return;
        }
        if (ShadowBindingService.absorbDamage(event)) {
            // Type Moon World rolls servant/card defenses later in this event. Remove the hit before dodge can react.
            event.setAmount(0.0F);
            event.setInvulnerabilityTicks(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (SpiritualDamageService.isCollapse(event.getSource())) {
            return;
        }
        if (PollutionService.shouldBlockDamage(
            event.getEntity(),
            event.getSource().getEntity()
        )) {
            event.setCanceled(true);
            return;
        }
        if (HolyGrailService.blocksAction(event.getSource().getEntity())) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(TypeMoonIntegration.imaginaryIncomingDamage(
            event.getSource(),
            event.getEntity(),
            event.getAmount()
        ));
        if (event.getEntity() instanceof ServerPlayer player
            && HolyGrailService.blocksDamage(player, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setAmount(CursedArmorService.reduceDamage(player, event.getSource(), event.getAmount()));
            ImaginaryShadowService.interruptOnDamage(player);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            float remainingDamage = ImaginaryStorageService.absorbDamage(
                player,
                event.getSource(),
                event.getAmount()
            );
            if (remainingDamage <= 0.0F) {
                event.setCanceled(true);
            } else if (remainingDamage < event.getAmount()) {
                event.setAmount(remainingDamage);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void enforceSpiritualCollapseDamage(LivingIncomingDamageEvent event) {
        if (!SpiritualDamageService.isCollapse(event.getSource())) return;
        event.setCanceled(false);
        event.setInvulnerabilityTicks(0);
        event.setAmount(Float.MAX_VALUE);
    }

    @SubscribeEvent
    public static void onFinalDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0.0F) {
            ShadowTransferService.interruptOnDamage(player);
        }
        if (event.getEntity() instanceof ServerPlayer player
            && HolyGrailService.preventLethalDamage(player, event.getSource(), event.getNewDamage())) {
            event.setNewDamage(0.0F);
        }
    }

    @SubscribeEvent
    public static void onDamageApplied(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F) {
            PollutionService.rememberServantDamage(event.getEntity(), event.getSource());
            ShadowBindingService.rewardBlackShadowManaFromDamage(event.getEntity(), event.getNewDamage());
        }
        if (event.getNewDamage() > 0.0F && event.getSource().getDirectEntity() instanceof ShadowFamiliarEntity familiar) {
            PollutionService.expose(event.getEntity(), familiar, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        if (SpiritualDamageService.isCollapse(event.getSource())) {
            event.setCanceled(false);
            event.getEntity().setHealth(0.0F);
            SpiritualDamageService.completeCollapseDeath(event.getEntity());
        }
        if (event.isCanceled()) {
            return;
        }
        PollutionService.entityDied(event.getEntity(), event.getSource());
        if (event.getEntity() instanceof ServerPlayer player) {
            BlackShadowNightService.playerUnavailable(player);
            SummonBlackMudService.playerDied(player);
            HolyGrailService.playerDied(player);
            ForbiddenMagicService.playerUnavailable(player);
            ShadowArtService.playerUnavailable(player);
        }
    }

    @SubscribeEvent
    public static void onMagicCast(MagicCastEvent.Pre event) {
        Entity caster = event.context().caster();
        Entity target = event.context().target();
        if (HolyGrailService.blocksAction(caster)) {
            event.setCanceled(true);
            return;
        }
        if (target != null && ImaginaryStorageService.nullifiesTargetedMagic(caster, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCommandSpell(CommandSpellEvent.Pre event) {
        if (PollutionService.blocksCommandSpell(event.context().target())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServantAction(ServantActionEvent.Pre event) {
        if (HolyGrailService.blocksAction(event.context().caster())
            || ImaginaryShadowService.preventsServantAction(event.context().caster())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServantAction(ServantActionEvent.Post event) {
        if (event.result().success() && event.result().resourceCost() > 0.0D) {
            SpiritualDamageService.recordMagicCost(event.context().caster(), event.result().resourceCost());
        }
    }

    @SubscribeEvent
    public static void onMagicCast(MagicCastEvent.Post event) {
        if (event.result().success() && event.result().resourceCost() > 0.0D) {
            SpiritualDamageService.recordMagicCost(event.context().caster(), event.result().resourceCost());
        }
    }

    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureAttribute(player);
            GrailErosionService.ensureCrestWormExpelled(player);
            CursedArmorService.beginFormation(player);
            CursedArmorService.sync(player);
        }
    }

    private static void ensureAttribute(ServerPlayer player) {
        if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).learned()) {
            TypeMoonIntegration.ensureImaginaryAttribute(player);
            TypeMoonIntegration.ensureGrailWormPower(player);
            if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).forbiddenMagicUnlocked()) {
                TypeMoonIntegration.ensureForbiddenMagicKnowledge(player);
            }
            if (player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowArtUnlocked()) {
                TypeMoonIntegration.ensureShadowArtKnowledge(player);
            }
        }
    }

    private CommonEvents() {
    }
}
