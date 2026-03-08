package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MagicGandrMachineGun {
    private static final int MODE_RAPID_BURST = 0;
    private static final int MODE_GATE_BARRAGE = 1;
    private static final int BURST_COUNT = 3;
    private static final int CHANT_TICKS = 20;
    private static final int BURST_INTERVAL_TICKS = 2;
    private static final int BARRAGE_INTERVAL_TICKS = 5;
    private static final int BARRAGE_INTERVAL_JITTER_TICKS = 2;
    private static final double MANA_COST_PER_SHOT = 20.0D;
    private static final float PROJECTILE_SCALE_MIN = 0.207F;
    private static final float PROJECTILE_SCALE_MAX = 0.370F;
    private static final double RIGHT_HAND_FORWARD_OFFSET = 0.08D;
    private static final double RANDOM_SIDE_JITTER = 0.06D;
    private static final double RANDOM_UP_JITTER = 0.05D;
    private static final double RANDOM_FORWARD_JITTER = 0.03D;
    private static final float RANDOM_YAW_JITTER = 0.45F;
    private static final float RANDOM_PITCH_JITTER = 0.35F;
    private static final int BARRAGE_MIN_SHOTS = 12;
    private static final int BARRAGE_MAX_SHOTS = 24;
    private static final int BARRAGE_COLUMNS = 8;
    private static final double BARRAGE_BACK_OFFSET = 2.2D;
    private static final double BARRAGE_UP_OFFSET = 0.72D;
    private static final double BARRAGE_ROW_SPACING = 0.44D;
    private static final double BARRAGE_COL_SPACING = 0.52D;
    private static final double BARRAGE_SIDE_JITTER = 0.20D;
    private static final double BARRAGE_HEIGHT_JITTER = 0.24D;
    private static final double BARRAGE_DEPTH_JITTER = 0.58D;
    private static final double BARRAGE_DEPTH_LAYER_SPREAD = 0.42D;
    private static final double BARRAGE_AIM_DISTANCE = 36.0D;
    private static final double BARRAGE_AIM_JITTER = 0.85D;
    private static final float BARRAGE_PROJECTILE_SPEED_MIN = 3.4F;
    private static final float BARRAGE_PROJECTILE_SPEED_MAX = 4.2F;
    private static final float BARRAGE_PROJECTILE_INACCURACY_MIN = 0.05F;
    private static final float BARRAGE_PROJECTILE_INACCURACY_MAX = 0.17F;
    private static final double CHARGE_ANCHOR_FORWARD_FROM_HAND = 0.54D;
    private static final double CHARGE_ANCHOR_UP_FROM_HAND = 0.10D;
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);

    private static final String TAG_ACTIVE = "TypeMoonGandrMachineGunActive";
    private static final String TAG_CHANTING = "TypeMoonGandrMachineGunChanting";
    private static final String TAG_CHANT_END_TICK = "TypeMoonGandrMachineGunChantEndTick";
    private static final String TAG_NEXT_BURST_TICK = "TypeMoonGandrMachineGunNextBurstTick";
    private static final String TAG_MODE = "TypeMoonGandrMachineGunMode";
    private static final String TAG_CHARGE_PREVIEW_UUIDS = "TypeMoonGandrMachineGunChargePreviewUUIDs";
    private static final String TAG_CHARGE_PREVIEW_UUID = "TypeMoonGandrMachineGunChargePreviewUUID";

    private static final float[] SHOT_YAW_OFFSETS = {0.0F, -1.1F, 1.1F};
    private static final float[] SHOT_PITCH_OFFSETS = {0.0F, -0.7F, 0.7F};
    private static final double[] SHOT_SIDE_OFFSETS = {0.0D, -0.12D, 0.12D};
    private static final double[] SHOT_UP_OFFSETS = {0.0D, 0.06D, -0.06D};

    private MagicGandrMachineGun() {
    }

    public static boolean execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.learned_magics.contains("gandr_machine_gun")) {
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
        var tag = player.getPersistentData();
        if (tag.getBoolean(TAG_ACTIVE) || tag.getBoolean(TAG_CHANTING)) {
            clearState(player);
            return true;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
            return false;
        }

        int mode = clampMode(vars.gandr_machine_gun_mode);
        if (!hasEnoughManaForMode(vars, mode)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }

        long now = player.level().getGameTime();
        tag.putBoolean(TAG_CHANTING, true);
        tag.putBoolean(TAG_ACTIVE, false);
        tag.putLong(TAG_CHANT_END_TICK, now + CHANT_TICKS);
        tag.putLong(TAG_NEXT_BURST_TICK, now + CHANT_TICKS);
        tag.putInt(TAG_MODE, mode);
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gandr_machine_gun.start"), true);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }

        var tag = player.getPersistentData();
        boolean chanting = tag.getBoolean(TAG_CHANTING);
        boolean active = tag.getBoolean(TAG_ACTIVE);
        if (!chanting && !active) {
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!canMaintain(player, vars)) {
            clearState(player);
            return;
        }
        int mode = clampMode(tag.getInt(TAG_MODE));

        long now = player.level().getGameTime();
        if (chanting) {
            long chantEndTick = tag.getLong(TAG_CHANT_END_TICK);
            if (now < chantEndTick) {
                double progress = 1.0D - ((chantEndTick - now) / (double) CHANT_TICKS);
                discardChargingPreview(player);
                spawnChantParticles(player, progress, mode, vars.proficiency_gander);
                return;
            }
            discardChargingPreview(player);
            tag.putBoolean(TAG_CHANTING, false);
            tag.putBoolean(TAG_ACTIVE, true);
            tag.putLong(TAG_NEXT_BURST_TICK, now);
        }

        if (!tag.getBoolean(TAG_ACTIVE)) {
            return;
        }

        long nextBurstTick = tag.getLong(TAG_NEXT_BURST_TICK);
        if (now < nextBurstTick) {
            return;
        }

        boolean fired;
        if (mode == MODE_GATE_BARRAGE) {
            fired = fireGateBarrage(player, vars);
            if (!fired) {
                clearState(player);
                return;
            }
            vars.magic_cooldown = BARRAGE_INTERVAL_TICKS;
            vars.proficiency_gander = Math.min(100.0D, vars.proficiency_gander + getBarrageShotCount(vars.proficiency_gander) * 0.02D);
            vars.syncMana(player);
            vars.syncProficiency(player);
            int jitter = player.getRandom().nextInt((BARRAGE_INTERVAL_JITTER_TICKS * 2) + 1) - BARRAGE_INTERVAL_JITTER_TICKS;
            int nextInterval = Math.max(2, BARRAGE_INTERVAL_TICKS + jitter);
            tag.putLong(TAG_NEXT_BURST_TICK, now + nextInterval);
            return;
        }

        fired = fireBurst(player, vars);
        if (!fired) {
            clearState(player);
            return;
        }

        vars.magic_cooldown = BURST_INTERVAL_TICKS;
        vars.proficiency_gander = Math.min(100.0D, vars.proficiency_gander + 0.05D);
        vars.syncMana(player);
        vars.syncProficiency(player);
        tag.putLong(TAG_NEXT_BURST_TICK, now + BURST_INTERVAL_TICKS);
    }

    private static boolean canMaintain(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
        }
        if (!vars.learned_magics.contains("gandr_machine_gun")) {
            return false;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            return false;
        }
        if (vars.selected_magics.isEmpty()) {
            return false;
        }
        int index = vars.current_magic_index;
        if (index < 0 || index >= vars.selected_magics.size()) {
            return false;
        }
        return "gandr_machine_gun".equals(vars.selected_magics.get(index));
    }

    private static boolean fireBurst(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        if (!hasEnoughManaForRapidBurst(vars)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }

        int chargeSeconds = getEquivalentChargeSeconds(vars.proficiency_gander);
        vars.player_mana = Math.max(0.0D, vars.player_mana - getRapidBurstManaCost());
        Level level = player.level();
        for (int i = 0; i < BURST_COUNT; i++) {
            shootRapidBullet(level, player, i, chargeSeconds);
        }
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                net.minecraft.sounds.SoundEvents.BLAZE_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5F,
                1.15F + (level.random.nextFloat() * 0.2F)
        );
        return true;
    }

    private static void shootRapidBullet(Level level, ServerPlayer player, int shotIndex, int chargeSeconds) {
        int pattern = Math.floorMod(shotIndex, BURST_COUNT);
        GanderProjectileEntity projectile = new GanderProjectileEntity(level, player);
        projectile.setNoGravity(true);
        projectile.setChargeSeconds(chargeSeconds);
        projectile.setVisualScale(getProjectileScaleForCharge(chargeSeconds));
        projectile.setItem(new ItemStack(ModItems.GANDER.get()));

        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);

        double randomSide = (level.random.nextDouble() * 2.0D - 1.0D) * RANDOM_SIDE_JITTER;
        double randomUp = (level.random.nextDouble() * 2.0D - 1.0D) * RANDOM_UP_JITTER;
        double randomForward = level.random.nextDouble() * RANDOM_FORWARD_JITTER;
        Vec3 spawnPos = handAnchor
                .add(forward.scale(RIGHT_HAND_FORWARD_OFFSET + randomForward))
                .add(right.scale(SHOT_SIDE_OFFSETS[pattern]))
                .add(up.scale(SHOT_UP_OFFSETS[pattern]))
                .add(right.scale(randomSide))
                .add(up.scale(randomUp));
        projectile.setPos(spawnPos);

        float yawOffset = SHOT_YAW_OFFSETS[pattern] + ((float) level.random.nextGaussian() * RANDOM_YAW_JITTER);
        float pitchOffset = SHOT_PITCH_OFFSETS[pattern] + ((float) level.random.nextGaussian() * RANDOM_PITCH_JITTER);
        Vec3 shotDir = forward;
        projectile.shootFromRotation(
                player,
                player.getXRot() + pitchOffset,
                player.getYRot() + yawOffset,
                0.0F,
                3.8F,
                0.10F
        );
        level.addFreshEntity(projectile);
        spawnShotParticles(level, spawnPos, shotDir);
    }

    private static boolean fireGateBarrage(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
        int shotCount = getBarrageShotCount(vars.proficiency_gander);
        double manaCost = shotCount * MANA_COST_PER_SHOT;
        if (vars.player_mana < manaCost) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.not_enough_mana"), true);
            return false;
        }

        vars.player_mana = Math.max(0.0D, vars.player_mana - manaCost);
        Level level = player.level();
        int chargeSeconds = getEquivalentChargeSeconds(vars.proficiency_gander);

        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        Vec3 center = player.getEyePosition()
                .add(forward.scale(-BARRAGE_BACK_OFFSET))
                .add(up.scale(BARRAGE_UP_OFFSET));
        Vec3 targetCenter = player.getEyePosition().add(forward.scale(BARRAGE_AIM_DISTANCE));

        int columns = Math.min(BARRAGE_COLUMNS + level.random.nextInt(3), Math.max(1, shotCount));
        columns = Math.max(5, columns);
        int rows = (int) Math.ceil(shotCount / (double) columns);
        int[] order = buildShuffledOrder(shotCount, level);

        for (int i = 0; i < shotCount; i++) {
            int slot = order[i];
            int row = slot / columns;
            int col = slot % columns;
            double x = (col - ((columns - 1) * 0.5D)) * BARRAGE_COL_SPACING
                    + ((level.random.nextDouble() - 0.5D) * BARRAGE_SIDE_JITTER);
            double y = (((rows - 1) * 0.5D) - row) * BARRAGE_ROW_SPACING
                    + ((level.random.nextDouble() - 0.5D) * BARRAGE_HEIGHT_JITTER);
            double rowDepthOffset = (((rows - 1) * 0.5D) - row) * BARRAGE_DEPTH_LAYER_SPREAD;
            Vec3 spawnPos = center
                    .add(right.scale(x))
                    .add(up.scale(y))
                    .add(forward.scale(rowDepthOffset + ((level.random.nextDouble() - 0.5D) * BARRAGE_DEPTH_JITTER)));

            Vec3 jitter = right.scale((level.random.nextDouble() - 0.5D) * BARRAGE_AIM_JITTER)
                    .add(up.scale((level.random.nextDouble() - 0.5D) * (BARRAGE_AIM_JITTER * 0.65D)));
            Vec3 direction = targetCenter.add(jitter).subtract(spawnPos).normalize();
            float speed = BARRAGE_PROJECTILE_SPEED_MIN + (level.random.nextFloat() * (BARRAGE_PROJECTILE_SPEED_MAX - BARRAGE_PROJECTILE_SPEED_MIN));
            float inaccuracy = BARRAGE_PROJECTILE_INACCURACY_MIN
                    + (level.random.nextFloat() * (BARRAGE_PROJECTILE_INACCURACY_MAX - BARRAGE_PROJECTILE_INACCURACY_MIN));
            shootBarrageBullet(level, player, spawnPos, direction, chargeSeconds, speed, inaccuracy);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                net.minecraft.sounds.SoundEvents.ILLUSIONER_PREPARE_MIRROR,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.65F,
                1.25F
        );
        return true;
    }

    private static void shootBarrageBullet(Level level, ServerPlayer player, Vec3 spawnPos, Vec3 direction, int chargeSeconds, float speed, float inaccuracy) {
        GanderProjectileEntity projectile = new GanderProjectileEntity(level, player);
        projectile.setNoGravity(true);
        projectile.setChargeSeconds(chargeSeconds);
        projectile.setVisualScale(getProjectileScaleForCharge(chargeSeconds));
        projectile.setItem(new ItemStack(ModItems.GANDER.get()));
        projectile.setPos(spawnPos);
        projectile.shoot(direction.x, direction.y, direction.z, speed, inaccuracy);
        level.addFreshEntity(projectile);
        spawnShotParticles(level, spawnPos, direction);
    }

    private static int[] buildShuffledOrder(int size, Level level) {
        int[] order = new int[size];
        for (int i = 0; i < size; i++) {
            order[i] = i;
        }
        for (int i = size - 1; i > 0; i--) {
            int j = level.random.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }
        return order;
    }

    private static void spawnShotParticles(Level level, Vec3 spawnPos, Vec3 direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 dir = direction.normalize();
        Vec3 right = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(dir).normalize();
        Vec3 mouth = spawnPos.add(dir.scale(0.04D));

        serverLevel.sendParticles(BLACK_DUST, mouth.x, mouth.y, mouth.z, 5, 0.05D, 0.05D, 0.05D, 0.02D);
        serverLevel.sendParticles(RED_DUST, mouth.x, mouth.y, mouth.z, 4, 0.04D, 0.04D, 0.04D, 0.02D);

        for (int i = 0; i < 2; i++) {
            double side = (serverLevel.random.nextDouble() - 0.5D) * 0.08D;
            double vertical = (serverLevel.random.nextDouble() - 0.5D) * 0.08D;
            Vec3 start = mouth.add(right.scale(side)).add(up.scale(vertical));
            Vec3 velBlack = dir.scale(0.16D + (serverLevel.random.nextDouble() * 0.06D));
            Vec3 velRed = dir.scale(0.20D + (serverLevel.random.nextDouble() * 0.06D));
            serverLevel.sendParticles(BLACK_DUST, start.x, start.y, start.z, 0, velBlack.x, velBlack.y, velBlack.z, 1.0D);
            serverLevel.sendParticles(RED_DUST, start.x, start.y, start.z, 0, velRed.x, velRed.y, velRed.z, 1.0D);
        }
    }

    private static boolean hasEnoughManaForMode(TypeMoonWorldModVariables.PlayerVariables vars, int mode) {
        if (mode == MODE_GATE_BARRAGE) {
            return vars.player_mana >= getBarrageManaCost(vars.proficiency_gander);
        }
        return hasEnoughManaForRapidBurst(vars);
    }

    private static boolean hasEnoughManaForRapidBurst(TypeMoonWorldModVariables.PlayerVariables vars) {
        return vars.player_mana >= getRapidBurstManaCost();
    }

    private static double getRapidBurstManaCost() {
        return MANA_COST_PER_SHOT * BURST_COUNT;
    }

    private static double getBarrageManaCost(double proficiency) {
        return getBarrageShotCount(proficiency) * MANA_COST_PER_SHOT;
    }

    private static int getBarrageShotCount(double proficiency) {
        double t = Math.max(0.0D, Math.min(100.0D, proficiency)) / 100.0D;
        return (int) Math.round(BARRAGE_MIN_SHOTS + ((BARRAGE_MAX_SHOTS - BARRAGE_MIN_SHOTS) * t));
    }

    private static int getEquivalentChargeSeconds(double proficiency) {
        double p = Math.max(0.0D, Math.min(100.0D, proficiency));
        return 1 + (int) Math.floor(p / 25.0D);
    }

    private static float getProjectileScaleForCharge(int chargeSeconds) {
        int clamped = Math.max(1, Math.min(5, chargeSeconds));
        double ratio = (clamped - 1) / 4.0D;
        return (float) (PROJECTILE_SCALE_MIN + ((PROJECTILE_SCALE_MAX - PROJECTILE_SCALE_MIN) * ratio));
    }

    private static int clampMode(int mode) {
        return mode == MODE_GATE_BARRAGE ? MODE_GATE_BARRAGE : MODE_RAPID_BURST;
    }

    private static void clearState(ServerPlayer player) {
        discardChargingPreview(player);
        var tag = player.getPersistentData();
        tag.putBoolean(TAG_ACTIVE, false);
        tag.putBoolean(TAG_CHANTING, false);
        tag.putLong(TAG_CHANT_END_TICK, 0L);
        tag.putLong(TAG_NEXT_BURST_TICK, 0L);
        tag.putInt(TAG_MODE, MODE_RAPID_BURST);
    }

    public static void forceCleanup(ServerPlayer player) {
        clearState(player);
    }

    private static void updateChargingPreview(ServerPlayer player, double progressRatio, double proficiency, int mode) {
        List<Vec3> anchors = getChargeAnchors(player, proficiency, mode);
        if (anchors.isEmpty()) {
            discardChargingPreview(player);
            return;
        }
        List<GanderProjectileEntity> previews = getOrCreateChargingPreviews(player, anchors.size());
        if (previews.size() != anchors.size()) {
            return;
        }

        int chargeSeconds = getEquivalentChargeSeconds(proficiency);
        float targetScale = getProjectileScaleForCharge(chargeSeconds);
        double t = Math.max(0.0D, Math.min(1.0D, progressRatio));
        float scale = (float) (PROJECTILE_SCALE_MIN + ((targetScale - PROJECTILE_SCALE_MIN) * t));

        for (int i = 0; i < previews.size(); i++) {
            GanderProjectileEntity preview = previews.get(i);
            Vec3 anchor = anchors.get(i);
            preview.setNoGravity(true);
            preview.setChargingPreview(true);
            preview.setChargeSeconds(chargeSeconds);
            preview.setVisualScale(scale);
            preview.setPos(anchor);
            preview.setDeltaMovement(Vec3.ZERO);
            preview.setYRot(player.getYRot());
            preview.setXRot(player.getXRot());
            preview.setYHeadRot(player.getYHeadRot());
            preview.hasImpulse = true;
        }
    }

    private static List<Vec3> getChargeAnchors(ServerPlayer player, double proficiency, int mode) {
        if (mode == MODE_GATE_BARRAGE) {
            int shotCount = getBarrageShotCount(proficiency);
            return getBarrageChargeAnchors(player, shotCount);
        }
        return List.of(getRapidChargeAnchor(player));
    }

    private static Vec3 getRapidChargeAnchor(ServerPlayer player) {
        Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
        Vec3 forward = player.getLookAngle().normalize();
        return handAnchor
                .add(forward.scale(CHARGE_ANCHOR_FORWARD_FROM_HAND))
                .add(new Vec3(0.0D, CHARGE_ANCHOR_UP_FROM_HAND, 0.0D));
    }

    private static Vec3 getBarrageFormationCenter(ServerPlayer player) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        return player.getEyePosition()
                .add(forward.scale(-BARRAGE_BACK_OFFSET))
                .add(up.scale(BARRAGE_UP_OFFSET));
    }

    private static List<Vec3> getBarrageChargeAnchors(ServerPlayer player, int shotCount) {
        List<Vec3> anchors = new ArrayList<>();
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = right.cross(forward).normalize();
        Vec3 center = getBarrageFormationCenter(player);

        int columns = Math.min(BARRAGE_COLUMNS, Math.max(1, shotCount));
        int rows = (int) Math.ceil(shotCount / (double) columns);
        for (int i = 0; i < shotCount; i++) {
            int row = i / columns;
            int col = i % columns;
            double x = (col - ((columns - 1) * 0.5D)) * BARRAGE_COL_SPACING
                    + (signedSlotNoise(i, 1) * BARRAGE_SIDE_JITTER);
            double y = (((rows - 1) * 0.5D) - row) * BARRAGE_ROW_SPACING
                    + (signedSlotNoise(i, 2) * BARRAGE_HEIGHT_JITTER);
            double rowDepthOffset = (((rows - 1) * 0.5D) - row) * BARRAGE_DEPTH_LAYER_SPREAD;
            double z = rowDepthOffset + (signedSlotNoise(i, 3) * BARRAGE_DEPTH_JITTER);
            anchors.add(center.add(right.scale(x)).add(up.scale(y)).add(forward.scale(z)));
        }
        return anchors;
    }

    private static double signedSlotNoise(int slot, int salt) {
        long n = (slot * 1103515245L) ^ (salt * 214013L + 2531011L);
        n = (n << 13) ^ n;
        double unit = 1.0D - (((n * (n * n * 15731L + 789221L) + 1376312589L) & 0x7fffffffL) / 1073741824.0D);
        return Math.max(-1.0D, Math.min(1.0D, unit));
    }

    private static List<GanderProjectileEntity> getOrCreateChargingPreviews(ServerPlayer player, int count) {
        List<GanderProjectileEntity> previews = getChargingPreviews(player);
        if (previews.size() == count) {
            return previews;
        }
        discardChargingPreview(player);
        if (!(player.level() instanceof ServerLevel)) {
            return List.of();
        }

        previews = new ArrayList<>(count);
        Vec3 fallbackPos = player.position();
        for (int i = 0; i < count; i++) {
            GanderProjectileEntity preview = new GanderProjectileEntity(player.level(), player);
            preview.setNoGravity(true);
            preview.setChargingPreview(true);
            preview.setVisualScale(PROJECTILE_SCALE_MIN);
            preview.setItem(new ItemStack(ModItems.GANDER.get()));
            preview.setPos(fallbackPos);
            player.level().addFreshEntity(preview);
            previews.add(preview);
        }
        saveChargingPreviewIds(player, previews);
        return previews;
    }

    private static void saveChargingPreviewIds(ServerPlayer player, List<GanderProjectileEntity> previews) {
        var tag = player.getPersistentData();
        ListTag list = new ListTag();
        for (GanderProjectileEntity preview : previews) {
            list.add(StringTag.valueOf(preview.getUUID().toString()));
        }
        tag.put(TAG_CHARGE_PREVIEW_UUIDS, list);
        if (previews.size() == 1) {
            tag.putUUID(TAG_CHARGE_PREVIEW_UUID, previews.get(0).getUUID());
        } else {
            tag.remove(TAG_CHARGE_PREVIEW_UUID);
        }
    }

    private static List<GanderProjectileEntity> getChargingPreviews(ServerPlayer player) {
        List<GanderProjectileEntity> previews = new ArrayList<>();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return previews;
        }
        var tag = player.getPersistentData();
        if (tag.contains(TAG_CHARGE_PREVIEW_UUIDS, 9)) {
            ListTag list = tag.getList(TAG_CHARGE_PREVIEW_UUIDS, 8);
            for (int i = 0; i < list.size(); i++) {
                String raw = list.getString(i);
                try {
                    UUID uuid = UUID.fromString(raw);
                    Entity entity = serverLevel.getEntity(uuid);
                    if (entity instanceof GanderProjectileEntity preview && preview.isChargingPreview()) {
                        previews.add(preview);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!previews.isEmpty()) {
                return previews;
            }
        }

        if (tag.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            UUID legacy = tag.getUUID(TAG_CHARGE_PREVIEW_UUID);
            Entity entity = serverLevel.getEntity(legacy);
            if (entity instanceof GanderProjectileEntity preview && preview.isChargingPreview()) {
                previews.add(preview);
            }
        }
        return previews;
    }

    private static void discardChargingPreview(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var tag = player.getPersistentData();
        List<GanderProjectileEntity> previews = getChargingPreviews(player);
        for (GanderProjectileEntity preview : previews) {
            preview.discard();
        }
        if (tag.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            UUID legacy = tag.getUUID(TAG_CHARGE_PREVIEW_UUID);
            Entity entity = serverLevel.getEntity(legacy);
            if (entity instanceof GanderProjectileEntity preview) {
                preview.discard();
            }
        }
        tag.remove(TAG_CHARGE_PREVIEW_UUIDS);
        tag.remove(TAG_CHARGE_PREVIEW_UUID);
    }

    private static void spawnChantParticles(ServerPlayer player, double progressRatio, int mode, double proficiency) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double t = Math.max(0.0D, Math.min(1.0D, progressRatio));
        Vec3 handCenter = getRapidChargeAnchor(player);
        Vec3 backCenter = getBarrageFormationCenter(player);

        if (mode == MODE_GATE_BARRAGE) {
            List<Vec3> anchors = getBarrageChargeAnchors(player, getBarrageShotCount(proficiency));
            for (Vec3 anchor : anchors) {
                // Every barrage ball slot gets its own charge spark so the wall feels "alive".
                spawnChargeGatheringAtCenter(serverLevel, anchor, t, 1, 1, 0.46D);
            }
            spawnChargeGatheringAtCenter(serverLevel, backCenter, t, 3, 2, 0.84D);
            spawnChargeGatheringAtCenter(serverLevel, handCenter, t, 2, 1, 0.72D);
        } else {
            spawnChargeGatheringAtCenter(serverLevel, handCenter, t, 3, 2, 0.92D);
            spawnChargeGatheringAtCenter(serverLevel, backCenter, t, 2, 1, 0.72D);
        }
    }

    private static void spawnChargeGatheringAtCenter(
            ServerLevel serverLevel,
            Vec3 center,
            double progress,
            int blackCount,
            int redCount,
            double radiusScale
    ) {
        double blackRadius = (0.22D - (0.10D * progress)) * radiusScale;
        double redRadius = (0.16D - (0.08D * progress)) * radiusScale;
        for (int i = 0; i < blackCount; i++) {
            double yaw = serverLevel.random.nextDouble() * (Math.PI * 2.0D);
            double pitch = (serverLevel.random.nextDouble() - 0.5D) * 0.8D;
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            Vec3 from = center.add(dir.scale(blackRadius));
            Vec3 vel = center.subtract(from).scale(0.28D);
            serverLevel.sendParticles(BLACK_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0D);
        }
        for (int i = 0; i < redCount; i++) {
            double yaw = serverLevel.random.nextDouble() * (Math.PI * 2.0D);
            double pitch = (serverLevel.random.nextDouble() - 0.5D) * 0.7D;
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            Vec3 from = center.add(dir.scale(redRadius));
            Vec3 vel = center.subtract(from).scale(0.34D);
            serverLevel.sendParticles(RED_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0D);
        }
    }
}
