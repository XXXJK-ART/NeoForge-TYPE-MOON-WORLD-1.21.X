package com.captain.servant.lancelot;

import com.captain.servant.compat.superbwarfare.SuperbWarfareLoader;
import com.captain.servant.spiritual.SpiritualizationService;
import com.captain.servant.network.payload.KnightArmoryOpenPayload;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardLancelotBerserkerSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardManaService;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.lancelot.LancelotCombatHelper;

public final class KnightArmoryService {
    public static final String OWNERIZED_TAG = "FateSupportKnightOfOwner";
    public static final String OWNER_UUID_TAG = "FateSupportKnightOwner";
    public static final String SHOTS_TAG = "FateSupportKnightShots";
    public static final String EXPIRES_TAG = "FateSupportKnightItemExpires";
    private static final String UNLOCKS_TAG = "FateSupportKnightArmoryUnlocks";
    private static final String COOLDOWN_PREFIX = "FateSupportKnightArmoryCooldown_";
    private static final int DEFAULT_UNLOCKS = KnightArmoryEntry.IRON_ROD.bit();

    private KnightArmoryService() {
    }

    public static boolean open(ServerPlayer player) {
        if (!player.isAlive() || SpiritualizationService.isRestricted(player)
                || !ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            return false;
        }
        if (ServantCardLancelotBerserkerSkills.isAroundightActive(player)) {
            player.displayClientMessage(Component.translatable("message.fatesupport.knight_armory.sealed"), true);
            return false;
        }
        PacketDistributor.sendToPlayer(player, new KnightArmoryOpenPayload(unlockedMask(player)));
        return true;
    }

    public static void select(ServerPlayer player, int networkId) {
        KnightArmoryEntry entry = KnightArmoryEntry.byNetworkId(networkId);
        if (entry == null || !player.isAlive() || SpiritualizationService.isRestricted(player)
                || !ServantCardLancelotBerserkerSkills.isActiveCard(player)
                || ServantCardLancelotBerserkerSkills.isAroundightActive(player)
                || !isUnlocked(player, entry)) {
            return;
        }

        long now = player.level().getGameTime();
        if (entry != KnightArmoryEntry.IRON_ROD && !SuperbWarfareLoader.isAvailable()) {
            player.displayClientMessage(Component.translatable("message.fatesupport.knight_armory.integration_missing"), true);
            return;
        }
        long cooldownUntil = player.getPersistentData().getLong(COOLDOWN_PREFIX + entry.key());
        if (cooldownUntil > now) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_armory.cooldown",
                    formatSeconds(cooldownUntil - now)
            ), true);
            return;
        }

        if (entry != KnightArmoryEntry.IRON_ROD && hasOwnerizedGun(player, entry)) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_armory.already_owned",
                    Component.translatable("screen.fatesupport.knight_armory.entry." + entry.key())
            ), true);
            return;
        }
        if (LancelotCombatHelper.remainingKnightOfOwnerCapacity(player) <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_vehicle.capacity"), true);
            return;
        }

        double manaCost = entry.manaCost();
        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        if (!ServantCardManaService.consume(player, variables, manaCost)) {
            return;
        }

        ItemStack result = ownerizedWeapon(player, entry);
        if (result.isEmpty()) {
            variables.servant_card_mana = Math.min(variables.servant_card_max_mana,
                    variables.servant_card_mana + manaCost);
            variables.syncServantCardRuntime(player);
            player.displayClientMessage(Component.translatable("message.fatesupport.knight_armory.integration_missing"), true);
            return;
        }

        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        player.getPersistentData().putLong(COOLDOWN_PREFIX + entry.key(), now + entry.cooldownTicks());
        player.displayClientMessage(Component.translatable(
                "message.fatesupport.knight_armory.weapon_received",
                Component.translatable("screen.fatesupport.knight_armory.entry." + entry.key())
        ), true);
        if (entry == KnightArmoryEntry.AWM || entry == KnightArmoryEntry.BARRETT) {
            ItemStack ammo = ownerizedAmmo(player, entry);
            if (!player.getInventory().add(ammo)) {
                player.drop(ammo, false);
            }
        }
    }

    public static boolean summonA10Skill(ServerPlayer player) {
        if (!SuperbWarfareLoader.isAvailable()
                || !ServantCardLancelotBerserkerSkills.isActiveCard(player)) {
            if (!SuperbWarfareLoader.isAvailable()) {
                player.displayClientMessage(Component.translatable(
                        "message.fatesupport.knight_armory.integration_missing"), true);
            }
            return false;
        }
        if (ServantCardLancelotBerserkerSkills.isAroundightActive(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_armory.sealed"), true);
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables variables = variables(player);
        int cooldown = ServantCardTransformManager.getSkillCooldown(variables, 7);
        if (cooldown > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_armory.cooldown",
                    formatSeconds(cooldown)
            ), true);
            return false;
        }
        if (SuperbWarfareLoader.hasActiveSummonedA10(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.fatesupport.knight_armory.a10_exists"), true);
            return false;
        }
        if (!ServantCardManaService.consume(player, variables, 450.0D)) {
            return false;
        }
        if (!SuperbWarfareLoader.summonA10(player, false)) {
            variables.servant_card_mana = Math.min(variables.servant_card_max_mana,
                    variables.servant_card_mana + 450.0D);
            variables.syncServantCardRuntime(player);
            return false;
        }
        ServantCardTransformManager.setSkillCooldownUntil(
                player,
                variables,
                7,
                player.level().getGameTime() + 3_600L
        );
        return true;
    }

    public static boolean ownerizeHeldSuperbWarfareGun(ServerPlayer player) {
        if (!SuperbWarfareLoader.isAvailable() || ServantCardLancelotBerserkerSkills.isAroundightActive(player)) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!SuperbWarfareLoader.isGun(held)) {
            return false;
        }
        if (isOwnerized(held)) {
            if (customData(held).getLong(EXPIRES_TAG) <= 0L) {
                markOwnerizedGun(held, player);
            }
            unlockFromOwnerizedItem(player, held);
            SuperbWarfareLoader.initializeOwnerizedGun(held);
            return true;
        }
        ItemStack ownerized = markOwnerizedGun(LancelotCombatHelper.knightOfOwnerStack(held.copy(), player), player);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ownerized);
        KnightArmoryEntry entry = entryFor(ownerized);
        if (entry != null) {
            unlock(player, entry);
        }
        SuperbWarfareLoader.initializeOwnerizedGun(ownerized);
        return true;
    }

    public static void unlockFromOwnerizedItem(ServerPlayer player, ItemStack stack) {
        KnightArmoryEntry entry = entryFor(stack);
        if (entry != null) {
            unlock(player, entry);
        }
    }

    public static int unlockedMask(ServerPlayer player) {
        int mask = player.getPersistentData().getInt(UNLOCKS_TAG) | DEFAULT_UNLOCKS;
        if (!SuperbWarfareLoader.isAvailable()) {
            return DEFAULT_UNLOCKS;
        }
        return mask;
    }

    public static void setAllUnlocked(ServerPlayer player, boolean unlocked) {
        int mask = DEFAULT_UNLOCKS;
        if (unlocked) {
            for (KnightArmoryEntry entry : KnightArmoryEntry.values()) {
                mask |= entry.bit();
            }
        }
        player.getPersistentData().putInt(UNLOCKS_TAG, mask);
        player.displayClientMessage(Component.translatable(
                unlocked ? "message.fatesupport.knight_armory.unlocked_all"
                        : "message.fatesupport.knight_armory.cleared"
        ), false);
    }

    public static boolean isOwnerized(ItemStack stack) {
        CompoundTag tag = customData(stack);
        return tag.getBoolean(OWNERIZED_TAG) || LancelotCombatHelper.isKnightOfOwner(stack);
    }

    public static ItemStack markOwnerized(ItemStack stack, net.minecraft.world.entity.LivingEntity owner) {
        CompoundTag tag = customData(stack);
        tag.putBoolean(OWNERIZED_TAG, true);
        tag.putUUID(OWNER_UUID_TAG, owner.getUUID());
        tag.putInt(SHOTS_TAG, 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ItemStack markOwnerizedGun(ItemStack stack, ServerPlayer owner) {
        markOwnerized(stack, owner);
        CompoundTag tag = customData(stack);
        tag.putLong(EXPIRES_TAG, owner.level().getGameTime() + 600L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static ItemStack markOwnerizedAmmo(ItemStack stack, ServerPlayer owner) {
        CompoundTag tag = customData(stack);
        tag.putBoolean(OWNERIZED_TAG, true);
        tag.putUUID(OWNER_UUID_TAG, owner.getUUID());
        tag.putLong(EXPIRES_TAG, owner.level().getGameTime() + 600L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean isExpired(ItemStack stack, long gameTime) {
        return !stack.isEmpty() && customData(stack).getLong(EXPIRES_TAG) > 0L
                && gameTime >= customData(stack).getLong(EXPIRES_TAG);
    }

    public static int incrementShots(ItemStack stack) {
        CompoundTag tag = customData(stack);
        int shots = tag.getInt(SHOTS_TAG) + 1;
        tag.putInt(SHOTS_TAG, shots);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return shots;
    }

    public static KnightArmoryEntry entryFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (KnightArmoryEntry entry : KnightArmoryEntry.values()) {
            if (entry.itemId() != null && entry.itemId().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isUnlocked(ServerPlayer player, KnightArmoryEntry entry) {
        return (unlockedMask(player) & entry.bit()) != 0;
    }

    private static void unlock(ServerPlayer player, KnightArmoryEntry entry) {
        player.getPersistentData().putInt(UNLOCKS_TAG, unlockedMask(player) | entry.bit());
    }

    private static boolean hasOwnerizedGun(ServerPlayer player, KnightArmoryEntry wanted) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            KnightArmoryEntry entry = entryFor(stack);
            if (entry == wanted && isOwnerized(stack)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack ownerizedWeapon(ServerPlayer player, KnightArmoryEntry entry) {
        Item item = entry.itemId() == null ? null : BuiltInRegistries.ITEM.get(entry.itemId());
        if (item == null || !BuiltInRegistries.ITEM.containsKey(entry.itemId())) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = markOwnerizedGun(
                LancelotCombatHelper.knightOfOwnerStack(new ItemStack(item), player),
                player
        );
        stack = ServantCardTransformManager.markGeneratedItem(stack, true, false);
        markOwnerizedGun(stack, player);
        SuperbWarfareLoader.initializeOwnerizedGun(stack);
        return stack;
    }

    private static ItemStack ownerizedAmmo(ServerPlayer player, KnightArmoryEntry entry) {
        ItemStack ammo = ammunition(entry);
        if (ammo.isEmpty()) {
            return ammo;
        }
        return markOwnerizedAmmo(LancelotCombatHelper.knightOfOwnerStack(ammo, player), player);
    }

    private static ItemStack ammunition(KnightArmoryEntry entry) {
        if (entry.ammoId() == null || !BuiltInRegistries.ITEM.containsKey(entry.ammoId())) {
            return ItemStack.EMPTY;
        }
        int count = switch (entry) {
            case MP5 -> 30;
            case AWM, BARRETT -> 3;
            case MINIGUN -> 60;
            default -> 1;
        };
        return new ItemStack(BuiltInRegistries.ITEM.get(entry.ammoId()), count);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static TypeMoonWorldModVariables.PlayerVariables variables(ServerPlayer player) {
        return player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
    }

    private static String formatSeconds(long ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }
}
