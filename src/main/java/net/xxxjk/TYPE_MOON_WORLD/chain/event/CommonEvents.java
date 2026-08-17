package net.xxxjk.TYPE_MOON_WORLD.chain.event;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.ChainControlService;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.EnumaChainService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            EnumaChainService.tick(level);
            BindingService.tick(level);
            ChainControlService.tickPlayerPresses(level);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        LivingEntity attacker;
        if (attackerEntity instanceof LivingEntity sourceAttacker) {
            attacker = sourceAttacker;
        } else {
            Entity directEntity = event.getSource().getDirectEntity();
            if (!(directEntity instanceof LivingEntity directAttacker)) {
                return;
            }
            attacker = directAttacker;
        }
        if (BindingService.isBound(attacker.getUUID())) {
            event.setAmount(BindingService.scaleDamageFromBoundTarget(attacker, event.getSource(), event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onEntityMount(EntityMountEvent event) {
        if (!event.getLevel().isClientSide() && event.isMounting()
            && BindingService.isBound(event.getEntityMounting().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && EnumaChainService.isCasting(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && EnumaChainService.isCasting(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (blockCombatItemUse(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (blockCombatItemUse(event.getEntity(), event.getItemStack())) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (blockCombatItemUse(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blockCombatItemUse(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player
            && EnumaChainService.isCasting(player)
            && isCombatOrSkillItem(event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnumaChainService.cleanupOwner(player);
            ChainControlService.cleanupOwner(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnumaChainService.cleanupOwner(player);
            ChainControlService.cleanupOwner(player);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            EnumaChainService.clearLevel(level);
            BindingService.clearLevel(level);
        }
    }

    private static boolean blockCombatItemUse(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        return player instanceof ServerPlayer serverPlayer
            && EnumaChainService.isCasting(serverPlayer)
            && isCombatOrSkillItem(stack);
    }

    private static boolean isCombatOrSkillItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof ProjectileItem) {
            return true;
        }
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return "typemoonworld".equals(namespace) || TYPE_MOON_WORLD.MOD_ID.equals(namespace);
    }

    private CommonEvents() {
    }
}

