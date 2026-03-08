package net.xxxjk.TYPE_MOON_WORLD.magic.jewel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

import java.util.ArrayList;
import java.util.List;

public class MagicJewelMachineGun {
    private static final int MIN_GEMS_REQUIRED = 3;
    private static final int BURST_COUNT = 3;
    private static final int CHANT_TICKS = 20;
    private static final int BURST_INTERVAL_TICKS = 2;
    private static final double EMPTY_GEM_MANA_COST = 50.0;
    private static final String TAG_ACTIVE = "TypeMoonMachineGunActive";
    private static final String TAG_CHANTING = "TypeMoonMachineGunChanting";
    private static final String TAG_CHANT_END_TICK = "TypeMoonMachineGunChantEndTick";
    private static final String TAG_NEXT_BURST_TICK = "TypeMoonMachineGunNextBurstTick";
    private static final float[] SHOT_YAW_OFFSETS = {0.0F, -1.2F, 1.2F};
    private static final float[] SHOT_PITCH_OFFSETS = {0.0F, -0.8F, 0.8F};
    private static final double[] SHOT_SIDE_OFFSETS = {0.0D, -0.12D, 0.12D};
    private static final double[] SHOT_UP_OFFSETS = {0.0D, 0.07D, -0.07D};

    public static boolean execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains("jewel_machine_gun")) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
            return false;
        }
        if (!vars.learned_magics.contains("jewel_magic_shoot")) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_learned"), true);
            return false;
        }
        if (!vars.learned_magics.contains("gander")) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.typemoonworld.scroll.requirement_not_met",
                            Component.translatable("magic.typemoonworld.gander.name")
                    ),
                    true
            );
            return false;
        }
        CompoundTag state = player.getPersistentData();
        if (state.getBoolean(TAG_ACTIVE) || state.getBoolean(TAG_CHANTING)) {
            clearState(player);
            return true;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
            return false;
        }

        if (!hasEnoughResourceForBurst(player, vars)) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        state.putBoolean(TAG_CHANTING, true);
        state.putBoolean(TAG_ACTIVE, false);
        state.putLong(TAG_CHANT_END_TICK, gameTime + CHANT_TICKS);
        state.putLong(TAG_NEXT_BURST_TICK, gameTime + CHANT_TICKS);
        player.displayClientMessage(getMachineGunChantComponent(), true);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }

        CompoundTag state = player.getPersistentData();
        boolean chanting = state.getBoolean(TAG_CHANTING);
        boolean active = state.getBoolean(TAG_ACTIVE);
        if (!chanting && !active) {
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!canMaintainMachineGun(player, vars)) {
            clearState(player);
            return;
        }

        long now = player.level().getGameTime();
        if (chanting) {
            long chantEndTick = state.getLong(TAG_CHANT_END_TICK);
            if (now < chantEndTick) {
                return;
            }
            state.putBoolean(TAG_CHANTING, false);
            state.putBoolean(TAG_ACTIVE, true);
            state.putLong(TAG_NEXT_BURST_TICK, now);
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.CYQ_GEM_SHOOT_STAR.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }

        if (!state.getBoolean(TAG_ACTIVE)) {
            return;
        }

        long nextBurstTick = state.getLong(TAG_NEXT_BURST_TICK);
        if (now < nextBurstTick) {
            return;
        }

        boolean fired = fireBurst(player, vars);
        if (!fired) {
            clearState(player);
            return;
        }

        vars.magic_cooldown = BURST_INTERVAL_TICKS;
        vars.proficiency_jewel_magic_release = Math.min(100.0, vars.proficiency_jewel_magic_release + 0.1);
        vars.syncProficiency(player);
        vars.syncMana(player);
        state.putLong(TAG_NEXT_BURST_TICK, now + BURST_INTERVAL_TICKS);
    }

    private static boolean canMaintainMachineGun(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
        }
        if (vars.selected_magics.isEmpty()) {
            return false;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            return false;
        }
        int index = vars.current_magic_index;
        if (index < 0 || index >= vars.selected_magics.size()) {
            return false;
        }
        return "jewel_machine_gun".equals(vars.selected_magics.get(index));
    }

    private static void clearState(ServerPlayer player) {
        CompoundTag state = player.getPersistentData();
        state.putBoolean(TAG_ACTIVE, false);
        state.putBoolean(TAG_CHANTING, false);
        state.putLong(TAG_CHANT_END_TICK, 0L);
        state.putLong(TAG_NEXT_BURST_TICK, 0L);
    }

    private static boolean hasEnoughResourceForBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        int availableGems = countAvailableUnengravedGems(player);
        if (availableGems < MIN_GEMS_REQUIRED) {
            player.displayClientMessage(Component.translatable(
                    "message.typemoonworld.magic.jewel_machine_gun.not_enough_gems",
                    MIN_GEMS_REQUIRED
            ), true);
            return false;
        }

        List<ShotPlan> shotPlans = planShots(player, vars.player_mana, BURST_COUNT);
        if (shotPlans.size() < BURST_COUNT) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.not_enough_mana"), true);
            return false;
        }
        return true;
    }

    private static boolean fireBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!hasEnoughResourceForBurst(player, vars)) {
            return false;
        }

        List<ShotPlan> shotPlans = planShots(player, vars.player_mana, BURST_COUNT);
        List<ItemStack> consumed = consumePlannedGems(player, shotPlans);
        if (consumed.size() < BURST_COUNT) {
            return false;
        }

        int emptyUsed = 0;
        for (ShotPlan plan : shotPlans) {
            if (!plan.full) {
                emptyUsed++;
            }
        }
        if (emptyUsed > 0) {
            vars.player_mana = Math.max(0.0, vars.player_mana - (emptyUsed * EMPTY_GEM_MANA_COST));
        }

        for (int i = 0; i < consumed.size(); i++) {
            shootBullet(player.level(), player, consumed.get(i), i);
        }
        return true;
    }

    private static int countAvailableUnengravedGems(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (isEligibleFullGem(stack) || isEligibleEmptyGem(stack)) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (isEligibleFullGem(offhand) || isEligibleEmptyGem(offhand)) {
            count += offhand.getCount();
        }
        return count;
    }

    private static List<ShotPlan> planShots(ServerPlayer player, double startMana, int count) {
        int fullPoor = countGemsByBucket(player, true, GemQuality.POOR);
        int fullNormal = countGemsByBucket(player, true, GemQuality.NORMAL);
        int fullHigh = countGemsByBucket(player, true, GemQuality.HIGH);
        int emptyPoor = countGemsByBucket(player, false, GemQuality.POOR);
        int emptyNormal = countGemsByBucket(player, false, GemQuality.NORMAL);
        int emptyHigh = countGemsByBucket(player, false, GemQuality.HIGH);

        double manaLeft = startMana;
        List<ShotPlan> plans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (fullPoor > 0) {
                fullPoor--;
                plans.add(new ShotPlan(true, GemQuality.POOR));
                continue;
            }
            if (fullNormal > 0) {
                fullNormal--;
                plans.add(new ShotPlan(true, GemQuality.NORMAL));
                continue;
            }
            if (fullHigh > 0) {
                fullHigh--;
                plans.add(new ShotPlan(true, GemQuality.HIGH));
                continue;
            }
            if (emptyPoor > 0 && manaLeft >= EMPTY_GEM_MANA_COST) {
                emptyPoor--;
                manaLeft -= EMPTY_GEM_MANA_COST;
                plans.add(new ShotPlan(false, GemQuality.POOR));
                continue;
            }
            if (emptyNormal > 0 && manaLeft >= EMPTY_GEM_MANA_COST) {
                emptyNormal--;
                manaLeft -= EMPTY_GEM_MANA_COST;
                plans.add(new ShotPlan(false, GemQuality.NORMAL));
                continue;
            }
            if (emptyHigh > 0 && manaLeft >= EMPTY_GEM_MANA_COST) {
                emptyHigh--;
                manaLeft -= EMPTY_GEM_MANA_COST;
                plans.add(new ShotPlan(false, GemQuality.HIGH));
                continue;
            }
            break;
        }
        return plans;
    }

    private static int countGemsByBucket(ServerPlayer player, boolean full, GemQuality quality) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matchesPlan(stack, full, quality)) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (matchesPlan(offhand, full, quality)) {
            count += offhand.getCount();
        }
        return count;
    }

    private static List<ItemStack> consumePlannedGems(ServerPlayer player, List<ShotPlan> plans) {
        List<ItemStack> consumed = new ArrayList<>();
        for (ShotPlan plan : plans) {
            ItemStack one = consumeOneMatchingGem(player, plan.full, plan.quality);
            if (one.isEmpty()) {
                return consumed;
            }
            consumed.add(one);
        }
        return consumed;
    }

    private static ItemStack consumeOneMatchingGem(ServerPlayer player, boolean full, GemQuality quality) {
        int totalMatched = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matchesPlan(stack, full, quality)) {
                totalMatched += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (matchesPlan(offhand, full, quality)) {
            totalMatched += offhand.getCount();
        }
        if (totalMatched <= 0) {
            return ItemStack.EMPTY;
        }

        int pick = player.getRandom().nextInt(totalMatched);
        for (ItemStack stack : player.getInventory().items) {
            if (!matchesPlan(stack, full, quality)) {
                continue;
            }
            if (pick < stack.getCount()) {
                return consumeOneFromStack(stack);
            }
            pick -= stack.getCount();
        }
        if (matchesPlan(offhand, full, quality)) {
            if (pick < offhand.getCount()) {
                return consumeOneFromStack(offhand);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack consumeOneFromStack(ItemStack stack) {
        ItemStack consumed = stack.copy();
        consumed.setCount(1);
        stack.shrink(1);
        return consumed;
    }

    private static boolean matchesPlan(ItemStack stack, boolean full, GemQuality quality) {
        if (full) {
            return isEligibleFullGem(stack)
                    && stack.getItem() instanceof FullManaCarvedGemItem fullGem
                    && fullGem.getQuality() == quality;
        }
        return isEligibleEmptyGem(stack)
                && stack.getItem() instanceof CarvedGemItem carvedGem
                && carvedGem.getQuality() == quality;
    }

    private static boolean isEligibleFullGem(ItemStack stack) {
        if (!(stack.getItem() instanceof FullManaCarvedGemItem)) {
            return false;
        }
        return GemEngravingService.getEngravedMagicId(stack) == null;
    }

    private static boolean isEligibleEmptyGem(ItemStack stack) {
        if (!(stack.getItem() instanceof CarvedGemItem)) {
            return false;
        }
        return GemEngravingService.getEngravedMagicId(stack) == null;
    }

    private record ShotPlan(boolean full, GemQuality quality) {
    }

    private static void shootBullet(Level level, ServerPlayer player, ItemStack gemStack, int shotIndex) {
        if (gemStack.isEmpty()) return;

        // Create new projectile
        RubyProjectileEntity projectile = new RubyProjectileEntity(level, player);

        CompoundTag tag = new CompoundTag();
        CustomData existingData = gemStack.get(DataComponents.CUSTOM_DATA);
        if (existingData != null) {
            tag = existingData.copyTag();
        }
        tag.putBoolean("IsMachineGunMode", true);

        ItemStack projectileStack = gemStack.copy();
        projectileStack.setCount(1);
        projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        projectile.setItem(projectileStack);

        int typeId = 0;
        GemType gemType = null;
        if (projectileStack.getItem() instanceof FullManaCarvedGemItem gemItem) {
            gemType = gemItem.getType();
        } else if (projectileStack.getItem() instanceof CarvedGemItem gemItem) {
            gemType = gemItem.getType();
        }

        if (gemType != null) {
            if (gemType == GemType.RUBY) typeId = 0;
            else if (gemType == GemType.SAPPHIRE) typeId = 1;
            else if (gemType == GemType.EMERALD) typeId = 2;
            else if (gemType == GemType.TOPAZ) typeId = 3;
            else if (gemType == GemType.CYAN) typeId = 4;
            else if (gemType == GemType.WHITE_GEMSTONE) typeId = 5;
            else if (gemType == GemType.BLACK_SHARD) typeId = 6;
        }

        int pattern = Math.floorMod(shotIndex, BURST_COUNT);
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
        Vec3 spawnPos = handAnchor
                .add(forward.scale(0.08D))
                .add(right.scale(SHOT_SIDE_OFFSETS[pattern]))
                .add(up.scale(SHOT_UP_OFFSETS[pattern]));
        projectile.setPos(spawnPos);

        projectile.setGemType(typeId);
        // Simultaneous triple-shot with slight up/down + left/right divergence.
        projectile.shootFromRotation(
                player,
                player.getXRot() + SHOT_PITCH_OFFSETS[pattern],
                player.getYRot() + SHOT_YAW_OFFSETS[pattern],
                0.0F,
                2.8F,
                0.06F
        );
        level.addFreshEntity(projectile);

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.CYM_GEM_BIUBIUBIU.get(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5F,
                1.2F + (level.random.nextFloat() * 0.2f)
        );
    }

    private static Component getMachineGunChantComponent() {
        return Component.literal("Call ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("blue").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("red").withStyle(ChatFormatting.RED))
                .append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("green").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(", for your ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("queen").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }
}
