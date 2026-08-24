package com.example.typemoonaddon.entity;

import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;

public final class SeaMonsterSpitEntity extends ThrowableItemProjectile {
    private float impactDamage = 8.0F;
    private double maxRange = 28.0;
    private double pollutionRadius = 4.0;
    private int pollutionLifetimeTicks = 160;
    private float pollutionDamagePerSecond = 3.0F;
    private int maxLifeTicks = 60;
    private Vec3 originPos = Vec3.ZERO;

    public SeaMonsterSpitEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public SeaMonsterSpitEntity(Level level, LivingEntity owner) {
        super(com.example.typemoonaddon.registry.AddonEntities.GILLES_SEA_MONSTER_SPIT.get(), owner, level);
        this.setItem(new ItemStack(Items.SLIME_BALL));
        this.setNoGravity(true);
        this.originPos = this.position();
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SLIME_BALL;
    }

    public void configure(float impactDamage, double maxRange, double pollutionRadius, int pollutionLifetimeTicks,
                          float pollutionDamagePerSecond) {
        this.impactDamage = Math.max(0.0F, impactDamage);
        this.maxRange = Math.max(4.0, maxRange);
        this.pollutionRadius = Math.max(0.5, pollutionRadius);
        this.pollutionLifetimeTicks = Math.max(20, pollutionLifetimeTicks);
        this.pollutionDamagePerSecond = Math.max(0.0F, pollutionDamagePerSecond);
        this.maxLifeTicks = Math.max(40, Mth.ceil(this.maxRange * 1.5));
        this.originPos = this.position();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        Entity owner = this.getOwner();
        if (entity == owner || EntityUtils.isImmunePlayerTarget(entity)) {
            return false;
        }
        if (owner instanceof LivingEntity ownerLiving && (ownerLiving.isAlliedTo(living) || living.isAlliedTo(ownerLiving))) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-4) {
            this.setYRot((float)(Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
            this.setXRot((float)(Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        }
        if (this.level().isClientSide()) {
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.SQUID_INK, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                this.level().addParticle(ParticleTypes.BUBBLE, this.getX(), this.getY(), this.getZ(), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (this.tickCount > this.maxLifeTicks || this.position().distanceToSqr(this.originPos) > this.maxRange * this.maxRange) {
            this.discard();
            return;
        }
        if (this.level() instanceof ServerLevel level && this.tickCount % 2 == 0) {
            Vec3 back = motion.lengthSqr() > 1.0E-4 ? motion.normalize().scale(-0.12) : Vec3.ZERO;
            Vec3 pos = this.position().add(back);
            level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y, pos.z, 2, 0.06, 0.06, 0.06, 0.01);
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(this.level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        this.applyImpact(level, result.getLocation(), target);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.applyImpact(level, result.getLocation(), null);
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
    }

    private void applyImpact(ServerLevel level, Vec3 impactPos, LivingEntity target) {
        Entity owner = this.getOwner();
        DamageSource source = owner instanceof LivingEntity livingOwner
                ? this.damageSources().mobProjectile(this, livingOwner)
                : this.damageSources().magic();
        if (target != null && !EntityUtils.isImmunePlayerTarget(target)) {
            target.invulnerableTime = 0;
            target.hurt(source, this.impactDamage);
            target.invulnerableTime = 0;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,
                    this.pollutionRadius >= 12.0 ? 180 : this.pollutionRadius >= 6.0 ? 120 : 80, 0, false, true, true));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    this.pollutionRadius >= 12.0 ? 160 : this.pollutionRadius >= 6.0 ? 100 : 60,
                    this.pollutionRadius >= 12.0 ? 2 : 1, false, true, true));
            Vec3 push = target.position().subtract(impactPos);
            if (push.horizontalDistanceSqr() > 1.0E-4) {
                push = push.normalize();
                target.push(push.x * 0.45, 0.12, push.z * 0.45);
                target.hurtMarked = true;
            }
        }
        GillesDeRaisCombatHelper.addPollutionZone(
                level.dimension(), impactPos, this.pollutionRadius, this.pollutionLifetimeTicks,
                this.pollutionDamagePerSecond, resolveSourceUuid(), resolveMasterUuid());
        level.playSound(null, BlockPos.containing(impactPos), SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE,
                this.pollutionRadius >= 12.0 ? 1.9F : this.pollutionRadius >= 6.0 ? 1.4F : 1.0F,
                this.pollutionRadius >= 12.0 ? 0.55F : 0.75F);
        level.sendParticles(ParticleTypes.SQUID_INK, impactPos.x, impactPos.y + 0.25, impactPos.z,
                this.pollutionRadius >= 12.0 ? 120 : this.pollutionRadius >= 6.0 ? 48 : 24,
                this.pollutionRadius * 0.25, this.pollutionRadius * 0.12, this.pollutionRadius * 0.25, 0.03);
        level.sendParticles(ParticleTypes.BUBBLE, impactPos.x, impactPos.y + 0.15, impactPos.z,
                this.pollutionRadius >= 12.0 ? 80 : this.pollutionRadius >= 6.0 ? 28 : 12,
                this.pollutionRadius * 0.18, this.pollutionRadius * 0.08, this.pollutionRadius * 0.18, 0.04);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, impactPos.x, impactPos.y + 0.35, impactPos.z,
                this.pollutionRadius >= 12.0 ? 40 : this.pollutionRadius >= 6.0 ? 16 : 8,
                this.pollutionRadius * 0.2, this.pollutionRadius * 0.08, this.pollutionRadius * 0.2, 0.02);
    }

    @Nullable
    private UUID resolveSourceUuid() {
        Entity owner = this.getOwner();
        if (owner instanceof SeaMonsterEntity seaMonster) {
            return seaMonster.getPollutionSourceUuid();
        }
        if (owner instanceof HugeSeaMonsterEntity hugeSeaMonster) {
            return hugeSeaMonster.getSourceUuid();
        }
        return owner instanceof GillesDeRaisEntity gilles ? gilles.getUUID() : null;
    }

    @Nullable
    private UUID resolveMasterUuid() {
        Entity owner = this.getOwner();
        if (owner instanceof SeaMonsterEntity seaMonster) {
            return seaMonster.getPollutionMasterUuid();
        }
        if (owner instanceof HugeSeaMonsterEntity hugeSeaMonster) {
            return hugeSeaMonster.getMasterUuid();
        }
        if (owner instanceof GillesDeRaisEntity gilles && gilles.getEntityMaster() != null) {
            return gilles.getEntityMaster().getUUID();
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("GillesSeaMonsterSpitImpactDamage", this.impactDamage);
        tag.putDouble("GillesSeaMonsterSpitMaxRange", this.maxRange);
        tag.putDouble("GillesSeaMonsterSpitPollutionRadius", this.pollutionRadius);
        tag.putInt("GillesSeaMonsterSpitPollutionLifetime", this.pollutionLifetimeTicks);
        tag.putFloat("GillesSeaMonsterSpitPollutionDamage", this.pollutionDamagePerSecond);
        tag.putInt("GillesSeaMonsterSpitMaxLifeTicks", this.maxLifeTicks);
        tag.putDouble("GillesSeaMonsterSpitOriginX", this.originPos.x);
        tag.putDouble("GillesSeaMonsterSpitOriginY", this.originPos.y);
        tag.putDouble("GillesSeaMonsterSpitOriginZ", this.originPos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.impactDamage = tag.getFloat("GillesSeaMonsterSpitImpactDamage");
        this.maxRange = tag.getDouble("GillesSeaMonsterSpitMaxRange");
        this.pollutionRadius = tag.getDouble("GillesSeaMonsterSpitPollutionRadius");
        this.pollutionLifetimeTicks = tag.getInt("GillesSeaMonsterSpitPollutionLifetime");
        this.pollutionDamagePerSecond = tag.getFloat("GillesSeaMonsterSpitPollutionDamage");
        this.maxLifeTicks = tag.getInt("GillesSeaMonsterSpitMaxLifeTicks");
        this.originPos = new Vec3(
                tag.getDouble("GillesSeaMonsterSpitOriginX"),
                tag.getDouble("GillesSeaMonsterSpitOriginY"),
                tag.getDouble("GillesSeaMonsterSpitOriginZ"));
        this.setNoGravity(true);
    }
}
