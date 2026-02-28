package net.xxxjk.TYPE_MOON_WORLD.magic.jewel.gravity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravityEffectHandler;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

import java.util.List;

public final class GemGravityFieldMagic {
    private static final String TAG_IS_GRAVITY_FIELD_GEM = "TypeMoonIsGravityFieldGem";
    private static final String TAG_FIELD_RADIUS = "TypeMoonGravityFieldRadius";
    private static final String TAG_FIELD_DURATION = "TypeMoonGravityFieldDuration";
    private static final String TAG_HEAVY_DURATION = "TypeMoonGravityHeavyDuration";
    private static final String TAG_PARTICLE_COUNT = "TypeMoonGravityParticleCount";

    private static final int PULSE_INTERVAL_TICKS = 10;

    private GemGravityFieldMagic() {
    }

    public static boolean throwGravityFieldProjectile(ServerPlayer player, ItemStack sourceGem) {
        if (player == null || sourceGem.isEmpty()) {
            return false;
        }

        float qualityMultiplier = 1.0f;
        if (sourceGem.getItem() instanceof FullManaCarvedGemItem fullGem) {
            qualityMultiplier = fullGem.getQuality().getEffectMultiplier();
        }

        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        double proficiency = Math.max(vars.proficiency_gravity_magic, vars.proficiency_jewel_magic_release);

        float radius = (float) (2.8D + 2.2D * qualityMultiplier);
        int fieldDuration = 160 + (int) Math.round(proficiency * 2.5D);
        int heavyDuration = 120 + (int) Math.round(proficiency * 3.0D);
        int particleCount = Math.max(72, 72 + (int) Math.round(30 * qualityMultiplier));

        ItemStack projectileStack = sourceGem.copy();
        projectileStack.setCount(1);

        CompoundTag tag = projectileStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TAG_IS_GRAVITY_FIELD_GEM, true);
        tag.putFloat(TAG_FIELD_RADIUS, radius);
        tag.putInt(TAG_FIELD_DURATION, fieldDuration);
        tag.putInt(TAG_HEAVY_DURATION, heavyDuration);
        tag.putInt(TAG_PARTICLE_COUNT, particleCount);
        projectileStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        RubyProjectileEntity projectile = new RubyProjectileEntity(player.level(), player);
        projectile.setGemType(5); // White visual trail by default.
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, MagicConstants.RUBY_THROW_VELOCITY, 0.6F);
        player.level().addFreshEntity(projectile);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.75F);
        return true;
    }

    public static boolean tryHandleProjectileImpact(RubyProjectileEntity projectile, ItemStack stack) {
        if (projectile == null || stack.isEmpty()) {
            return false;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean(TAG_IS_GRAVITY_FIELD_GEM)) {
            return false;
        }
        if (!(projectile.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        float radius = tag.contains(TAG_FIELD_RADIUS) ? tag.getFloat(TAG_FIELD_RADIUS) : 4.6F;
        int fieldDuration = tag.contains(TAG_FIELD_DURATION) ? tag.getInt(TAG_FIELD_DURATION) : 220;
        int heavyDuration = tag.contains(TAG_HEAVY_DURATION) ? tag.getInt(TAG_HEAVY_DURATION) : 180;
        int particleCount = tag.contains(TAG_PARTICLE_COUNT) ? tag.getInt(TAG_PARTICLE_COUNT) : 90;
        Vec3 center = projectile.position();

        spawnGravityField(serverLevel, center, radius, fieldDuration, heavyDuration, particleCount);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y + 0.8D, center.z, particleCount * 3, radius * 0.55D, 0.42D, radius * 0.55D, 0.004D);
        serverLevel.sendParticles(ParticleTypes.PORTAL, center.x, center.y + 0.25D, center.z, particleCount * 2, radius * 0.45D, 0.2D, radius * 0.45D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 0.45D, center.z, particleCount, radius * 0.35D, 0.25D, radius * 0.35D, 0.0D);
        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.85F, 0.55F);
        return true;
    }

    private static void spawnGravityField(ServerLevel level, Vec3 center, float radius, int fieldDuration, int heavyDuration, int particleCount) {
        int pulses = Math.max(1, fieldDuration / PULSE_INTERVAL_TICKS);
        for (int i = 0; i <= pulses; i++) {
            final int pulseIndex = i;
            int delay = i * PULSE_INTERVAL_TICKS;
            TYPE_MOON_WORLD.queueServerWork(delay, () -> {
                if (level.getServer() == null) {
                    return;
                }

                double progress = (double) pulseIndex / (double) Math.max(1, pulses);
                double wave = Math.sin(pulseIndex * 0.72D) * 0.12D;
                double topY = center.y + 1.35D - progress * 0.95D + wave;
                double midY = center.y + 0.72D - progress * 0.45D + wave * 0.45D;
                double lowY = center.y + 0.16D + wave * 0.18D;
                double spread = radius * (0.46D - progress * 0.18D);

                int blackParticles = Math.max(90, (int) (particleCount * 2.6D));
                int enderParticles = Math.max(60, (int) (particleCount * 1.6D));
                int pressureParticles = Math.max(42, (int) (particleCount * 1.1D));

                level.sendParticles(ParticleTypes.SQUID_INK, center.x, topY, center.z, blackParticles, spread, 0.45D, spread, 0.004D);
                level.sendParticles(ParticleTypes.SQUID_INK, center.x, midY, center.z, blackParticles / 2, spread * 0.75D, 0.22D, spread * 0.75D, 0.003D);
                level.sendParticles(ParticleTypes.PORTAL, center.x, lowY, center.z, enderParticles, spread * 0.62D, 0.16D, spread * 0.62D, 0.012D);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, midY, center.z, enderParticles / 2, spread * 0.48D, 0.22D, spread * 0.48D, 0.0D);
                level.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, center.x, topY + 0.2D, center.z, pressureParticles, spread * 0.4D, 0.12D, spread * 0.4D, 0.01D);

                AABB area = new AABB(
                        center.x - radius, center.y - 1.0D, center.z - radius,
                        center.x + radius, center.y + 2.0D, center.z + radius
                );
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);
                long until = level.getGameTime() + heavyDuration;

                for (LivingEntity target : targets) {
                    if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
                        continue;
                    }
                    MagicGravityEffectHandler.applyGravityState(target, MagicGravity.MODE_ULTRA_HEAVY, until);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(100, heavyDuration / 2), 4, false, false, true));
                }
            });
        }
    }
}
