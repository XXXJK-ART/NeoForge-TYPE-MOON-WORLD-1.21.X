package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;
import org.joml.Vector3f;
import java.util.UUID;

public final class MagicGander {
    private static final double MANA_PER_SECOND = 5.0D;
    private static final int BASE_MAX_CHARGE_SECONDS = 4;
    private static final int ADVANCED_MAX_CHARGE_SECONDS = 5;
    private static final double ADVANCED_CHARGE_PROFICIENCY_THRESHOLD = 50.0D;
    private static final int BASE_CHARGE_STEP_TICKS = 20; // 1.0s per stage at 0 proficiency
    private static final int MIN_CHARGE_STEP_TICKS = 2;   // 0.1s per stage at high proficiency
    private static final double RELEASE_FORWARD_FROM_ANCHOR = 0.08D;
    private static final double CHARGE_ANCHOR_FORWARD = 1.32D;
    private static final double CHARGE_ANCHOR_DOWN = -0.12D;
    private static final double CHARGE_ANCHOR_RIGHT = 0.3D;
    private static final double CHARGE_ANCHOR_FORWARD_FROM_HAND = 0.54D;
    private static final double CHARGE_ANCHOR_UP_FROM_HAND = 0.10D;
    // Adapted for the new Gander OBJ size (~6.4% larger radius than previous mesh).
    private static final float CHARGE_PREVIEW_SCALE_MIN = 0.207F;
    private static final float CHARGE_PREVIEW_SCALE_MAX = 0.370F;
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.0F);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.08F, 0.12F), 1.1F);
    private static final String TAG_CHARGING = "TypeMoonGanderCharging";
    private static final String TAG_CHARGE_START_TICK = "TypeMoonGanderChargeStartTick";
    private static final String TAG_CHARGE_SECONDS = "TypeMoonGanderChargeSeconds";
    private static final String TAG_CHARGE_PREVIEW_UUID = "TypeMoonGanderChargePreviewUUID";

    private MagicGander() {
    }

    public static boolean execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        // Fallback behavior for legacy single-click calls: toggle start/release.
        if (isCharging(player)) {
            return releaseCharge(player);
        }
        beginCharge(player);
        return false;
    }

    public static boolean beginCharge(ServerPlayer player) {
        if (!canUse(player)) {
            return false;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
            return false;
        }
        if (isCharging(player)) {
            return true;
        }
        var tag = player.getPersistentData();
        long now = player.level().getGameTime();
        tag.putBoolean(TAG_CHARGING, true);
        tag.putLong(TAG_CHARGE_START_TICK, now);
        tag.putInt(TAG_CHARGE_SECONDS, 0);
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int maxChargeSeconds = getMaxChargeSeconds(vars.proficiency_gander);
        updateChargingPreview(player, now, now, BASE_CHARGE_STEP_TICKS, 0, maxChargeSeconds);
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.start"), true);
        return true;
    }

    public static boolean releaseCharge(ServerPlayer player) {
        if (!isCharging(player)) {
            return false;
        }
        if (!EntityUtils.hasAnyEmptyHand(player)) {
            clearCharge(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.need_empty_hand"), true);
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int maxChargeSeconds = getMaxChargeSeconds(vars.proficiency_gander);
        int chargeSeconds = Math.max(0, Math.min(maxChargeSeconds, player.getPersistentData().getInt(TAG_CHARGE_SECONDS)));
        clearCharge(player);

        if (chargeSeconds <= 0) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.too_low"), true);
            return false;
        }

        GanderProjectileEntity projectile = new GanderProjectileEntity(player.level(), player);
        projectile.setNoGravity(true);
        projectile.setChargeSeconds(chargeSeconds);
        projectile.setItem(new ItemStack(ModItems.GANDER.get()));

        Vec3 direction = player.getLookAngle().normalize();
        HitResult hit = EntityUtils.getRayTraceTarget(player, 48.0D);
        if (hit instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            if (!(EntityUtils.isImmunePlayerTarget(target))) {
                Vec3 targetPos = target.getEyePosition().subtract(player.getEyePosition());
                if (targetPos.lengthSqr() > 1.0E-6D) {
                    direction = targetPos.normalize();
                }
            }
        }

        // Spawn from the same center anchor used by the charging visual (between two raised hands).
        Vec3 spawnPos = getChargeAnchor(player).add(direction.scale(RELEASE_FORWARD_FROM_ANCHOR));
        projectile.setPos(spawnPos);
        projectile.setVisualScale(getChargeVisualScaleBySeconds(chargeSeconds, maxChargeSeconds));
        projectile.shoot(direction.x, direction.y, direction.z, 3.8F, 0.0F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.7F,
                1.1F + (chargeSeconds * 0.05F)
        );
        vars.proficiency_gander = Math.min(100.0, vars.proficiency_gander + (0.2 * chargeSeconds));
        vars.syncProficiency(player);
        player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.cast", chargeSeconds, (int) (chargeSeconds * MANA_PER_SECOND)), true);
        return true;
    }

    public static void tick(ServerPlayer player) {
        if (!isCharging(player)) {
            return;
        }
        if (!canUse(player)) {
            clearCharge(player);
            return;
        }

        var tag = player.getPersistentData();
        long now = player.level().getGameTime();
        long startTick = tag.getLong(TAG_CHARGE_START_TICK);
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        int maxChargeSeconds = getMaxChargeSeconds(vars.proficiency_gander);
        int chargedSeconds = Math.max(0, Math.min(maxChargeSeconds, tag.getInt(TAG_CHARGE_SECONDS)));
        int chargeStepTicks = getChargeStepTicks(vars.proficiency_gander);
        int shouldReachSeconds = Math.max(0, Math.min(maxChargeSeconds, (int) ((now - startTick) / (long) chargeStepTicks)));
        updateChargingPreview(player, now, startTick, chargeStepTicks, chargedSeconds, maxChargeSeconds);

        if (chargedSeconds >= shouldReachSeconds) {
            return;
        }

        while (chargedSeconds < shouldReachSeconds) {
            if (!ManaHelper.consumeManaOrHealth(player, MANA_PER_SECOND)) {
                // Not enough resource to continue charging; release current charge automatically.
                releaseCharge(player);
                return;
            }
            chargedSeconds++;
            tag.putInt(TAG_CHARGE_SECONDS, chargedSeconds);
            vars.syncMana(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.progress", chargedSeconds, maxChargeSeconds), true);
            applySelfChargeFeedback(player, chargedSeconds);
            if (chargedSeconds >= maxChargeSeconds) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.magic.gander.charge.max"), true);
                break;
            }
        }
    }

    private static int getChargeStepTicks(double proficiency) {
        double clamped = Math.max(0.0, Math.min(100.0, proficiency));
        double t = clamped / 100.0;
        int stepTicks = (int) Math.round(BASE_CHARGE_STEP_TICKS - ((BASE_CHARGE_STEP_TICKS - MIN_CHARGE_STEP_TICKS) * t));
        return Math.max(MIN_CHARGE_STEP_TICKS, Math.min(BASE_CHARGE_STEP_TICKS, stepTicks));
    }

    private static int getMaxChargeSeconds(double proficiency) {
        return proficiency >= ADVANCED_CHARGE_PROFICIENCY_THRESHOLD ? ADVANCED_MAX_CHARGE_SECONDS : BASE_MAX_CHARGE_SECONDS;
    }

    private static float getChargeVisualScaleBySeconds(int chargeSeconds, int maxChargeSeconds) {
        int clampedMax = Math.max(1, maxChargeSeconds);
        int clamped = Math.max(1, Math.min(clampedMax, chargeSeconds));
        double ratio = clamped / (double) clampedMax;
        return getChargeVisualScaleByRatio(ratio);
    }

    private static float getChargeVisualScaleByRatio(double ratio) {
        double progressRatio = Math.max(0.0D, Math.min(1.0D, ratio));
        return (float) (CHARGE_PREVIEW_SCALE_MIN + ((CHARGE_PREVIEW_SCALE_MAX - CHARGE_PREVIEW_SCALE_MIN) * progressRatio));
    }

    private static void updateChargingPreview(ServerPlayer player, long now, long startTick, int chargeStepTicks, int chargedSeconds, int maxChargeSeconds) {
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }
        int clampedMaxCharge = Math.max(1, maxChargeSeconds);

        double rawProgress = Math.max(0.0D, (now - startTick) / (double) chargeStepTicks);
        double chargeProgress = Math.max(chargedSeconds, Math.min(clampedMaxCharge, rawProgress));
        double progressRatio = Math.max(0.0D, Math.min(1.0D, chargeProgress / clampedMaxCharge));
        float scale = getChargeVisualScaleByRatio(progressRatio);

        GanderProjectileEntity preview = getOrCreateChargingPreview(player);
        if (preview == null) {
            return;
        }

        Vec3 anchor = getChargeAnchor(player);
        preview.setNoGravity(true);
        preview.setChargingPreview(true);
        preview.setVisualScale(scale);
        preview.setPos(anchor);
        preview.setDeltaMovement(Vec3.ZERO);
        preview.setYRot(player.getYRot());
        preview.setXRot(player.getXRot());
        preview.setYHeadRot(player.getYHeadRot());
        preview.hasImpulse = true;

        // Only show particles while charge is still gathering; stop when fully charged.
        if (progressRatio < 0.999D) {
            spawnGatheringParticles((ServerLevel) player.level(), anchor, progressRatio);
        }
    }

    private static void applySelfChargeFeedback(ServerPlayer player, int chargedSeconds) {
        int amplifier = Math.max(0, chargedSeconds - 1);
        // Charging feedback: each second increases burden level by 1.
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, amplifier, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 25, amplifier, false, false, true));
    }

    private static boolean canUse(ServerPlayer player) {
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!vars.is_magus || !vars.is_magic_circuit_open) {
            return false;
        }
        if (vars.selected_magics.isEmpty()) {
            return false;
        }
        int index = vars.current_magic_index;
        if (index < 0 || index >= vars.selected_magics.size()) {
            return false;
        }
        if (!"gander".equals(vars.selected_magics.get(index)) || !vars.learned_magics.contains("gander")) {
            return false;
        }
        return EntityUtils.hasAnyEmptyHand(player);
    }

    private static boolean isCharging(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_CHARGING);
    }

    private static void clearCharge(ServerPlayer player) {
        discardChargingPreview(player);
        var tag = player.getPersistentData();
        tag.putBoolean(TAG_CHARGING, false);
        tag.putLong(TAG_CHARGE_START_TICK, 0L);
        tag.putInt(TAG_CHARGE_SECONDS, 0);
    }

    public static void forceCleanup(ServerPlayer player) {
        clearCharge(player);
    }

    public static Vec3 getChargeAnchor(LivingEntity caster) {
        // Use a rigid local basis so offset always follows player rotation consistently.
        float yawRad = caster.getYRot() * ((float)Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0D, Math.cos(yawRad));
        Vec3 worldUp = new Vec3(0.0D, 1.0D, 0.0D);
        if (caster instanceof net.minecraft.world.entity.player.Player player) {
            Vec3 handAnchor = EntityUtils.getCurrentEmptyHandCastAnchor(player);
            return handAnchor
                    .add(forward.scale(CHARGE_ANCHOR_FORWARD_FROM_HAND))
                    .add(worldUp.scale(CHARGE_ANCHOR_UP_FROM_HAND));
        }
        Vec3 sideBase = forward.cross(worldUp);
        if (sideBase.lengthSqr() < 1.0E-6D) {
            sideBase = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            sideBase = sideBase.normalize();
        }

        return caster.getEyePosition()
                .add(forward.scale(CHARGE_ANCHOR_FORWARD))
                .add(sideBase.scale(CHARGE_ANCHOR_RIGHT))
                .add(worldUp.scale(CHARGE_ANCHOR_DOWN));
    }

    private static GanderProjectileEntity getOrCreateChargingPreview(ServerPlayer player) {
        GanderProjectileEntity preview = getChargingPreview(player);
        if (preview != null && preview.isAlive()) {
            return preview;
        }
        if (!(player.level() instanceof ServerLevel)) {
            return null;
        }
        preview = new GanderProjectileEntity(player.level(), player);
        preview.setNoGravity(true);
        preview.setChargingPreview(true);
        preview.setVisualScale(CHARGE_PREVIEW_SCALE_MIN);
        preview.setItem(new ItemStack(ModItems.GANDER.get()));
        preview.setPos(getChargeAnchor(player));
        player.level().addFreshEntity(preview);
        player.getPersistentData().putUUID(TAG_CHARGE_PREVIEW_UUID, preview.getUUID());
        return preview;
    }

    private static GanderProjectileEntity getChargingPreview(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        var tag = player.getPersistentData();
        if (!tag.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            return null;
        }
        UUID uuid = tag.getUUID(TAG_CHARGE_PREVIEW_UUID);
        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof GanderProjectileEntity preview && preview.isChargingPreview()) {
            return preview;
        }
        return null;
    }

    private static void discardChargingPreview(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var tag = player.getPersistentData();
        if (!tag.hasUUID(TAG_CHARGE_PREVIEW_UUID)) {
            return;
        }
        UUID uuid = tag.getUUID(TAG_CHARGE_PREVIEW_UUID);
        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof GanderProjectileEntity preview) {
            preview.discard();
        }
        tag.remove(TAG_CHARGE_PREVIEW_UUID);
    }

    private static void spawnGatheringParticles(ServerLevel level, Vec3 center, double progressRatio) {
        double t = Math.max(0.0D, Math.min(1.0D, progressRatio));
        double outerRadius = 0.34D - (0.20D * t);
        double innerRadius = 0.22D - (0.14D * t);
        for (int i = 0; i < 4; i++) {
            double yaw = level.random.nextDouble() * (Math.PI * 2.0D);
            double pitch = (level.random.nextDouble() - 0.5D) * 0.9D;
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            Vec3 from = center.add(dir.scale(outerRadius));
            Vec3 vel = center.subtract(from).scale(0.30D);
            level.sendParticles(BLACK_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0D);
        }
        for (int i = 0; i < 3; i++) {
            double yaw = level.random.nextDouble() * (Math.PI * 2.0D);
            double pitch = (level.random.nextDouble() - 0.5D) * 0.7D;
            Vec3 dir = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
            Vec3 from = center.add(dir.scale(innerRadius));
            Vec3 vel = center.subtract(from).scale(0.36D);
            level.sendParticles(RED_DUST, from.x, from.y, from.z, 0, vel.x, vel.y, vel.z, 1.0D);
        }
    }
}
