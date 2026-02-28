package net.xxxjk.TYPE_MOON_WORLD.magic.other;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class MagicGravityEffectHandler {
    private static final String TAG_MODE = "TypeMoonGravityMode";
    private static final String TAG_UNTIL = "TypeMoonGravityUntil";
    private static final double HEAVY_EXTRA_FALL_ACCEL = 0.09D;
    private static final double ULTRA_HEAVY_EXTRA_FALL_ACCEL = 0.16D;

    public static void applyGravityState(LivingEntity target, int mode, long untilGameTime) {
        CompoundTag tag = target.getPersistentData();
        tag.putInt(TAG_MODE, mode);
        tag.putLong(TAG_UNTIL, untilGameTime);
    }

    public static void clearGravityState(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        tag.remove(TAG_MODE);
        tag.remove(TAG_UNTIL);
    }

    public static int getCurrentMode(LivingEntity target) {
        CompoundTag tag = target.getPersistentData();
        if (!tag.contains(TAG_MODE) || !tag.contains(TAG_UNTIL)) {
            return MagicGravity.MODE_NORMAL;
        }
        long now = target.level().getGameTime();
        long until = tag.getLong(TAG_UNTIL);
        if (now >= until) {
            clearGravityState(target);
            return MagicGravity.MODE_NORMAL;
        }

        int mode = tag.getInt(TAG_MODE);
        if (mode < MagicGravity.MODE_ULTRA_LIGHT || mode > MagicGravity.MODE_ULTRA_HEAVY) {
            clearGravityState(target);
            return MagicGravity.MODE_NORMAL;
        }
        return mode;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (living.level().isClientSide()) return;

        int mode = getCurrentMode(living);
        if (mode == MagicGravity.MODE_NORMAL) {
            return;
        }

        if (mode == MagicGravity.MODE_LIGHT || mode == MagicGravity.MODE_ULTRA_LIGHT) {
            // Light gravity tiers: higher jumps + slow falling.
            int jumpAmplifier = mode == MagicGravity.MODE_ULTRA_LIGHT ? 4 : 2;
            living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, jumpAmplifier, false, false, false));
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, false));
            return;
        }

        // Heavy gravity tiers: harder to jump and falls faster.
        living.removeEffect(MobEffects.SLOW_FALLING);
        if (!living.onGround() && !living.isInWaterOrBubble() && !living.isFallFlying()) {
            Vec3 motion = living.getDeltaMovement();
            double extraFallAccel = mode == MagicGravity.MODE_ULTRA_HEAVY ? ULTRA_HEAVY_EXTRA_FALL_ACCEL : HEAVY_EXTRA_FALL_ACCEL;
            double minY = mode == MagicGravity.MODE_ULTRA_HEAVY ? -3.6D : -3.0D;
            double newY = Math.max(minY, motion.y - extraFallAccel);
            living.setDeltaMovement(motion.x, newY, motion.z);
            living.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;

        int mode = getCurrentMode(living);
        if (mode == MagicGravity.MODE_HEAVY || mode == MagicGravity.MODE_ULTRA_HEAVY) {
            Vec3 motion = living.getDeltaMovement();
            double jumpScale = mode == MagicGravity.MODE_ULTRA_HEAVY ? 0.18D : 0.35D;
            living.setDeltaMovement(motion.x, motion.y * jumpScale, motion.z);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide()) return;

        int mode = getCurrentMode(living);
        if (mode == MagicGravity.MODE_LIGHT || mode == MagicGravity.MODE_ULTRA_LIGHT) {
            event.setDistance(0.0F);
            event.setCanceled(true);
        } else if (mode == MagicGravity.MODE_HEAVY || mode == MagicGravity.MODE_ULTRA_HEAVY) {
            float scale = mode == MagicGravity.MODE_ULTRA_HEAVY ? 2.3F : 1.6F;
            event.setDamageMultiplier(event.getDamageMultiplier() * scale);
        }
    }
}
