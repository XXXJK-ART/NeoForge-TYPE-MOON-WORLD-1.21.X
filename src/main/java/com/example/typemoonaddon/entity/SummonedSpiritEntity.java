package com.example.typemoonaddon.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import org.jetbrains.annotations.Nullable;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Shared server-side behavior for the two spirit-summoning families. */
public abstract class SummonedSpiritEntity extends Monster {
    public static final int MODE_FOLLOW = 0;
    public static final int MODE_WANDER = 1;
    public static final int MODE_ATTACK = 2;

    @Nullable
    private UUID ownerId;
    private int commandMode = MODE_FOLLOW;
    private int attackCooldown;

    protected SummonedSpiritEntity(EntityType<? extends SummonedSpiritEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = getOwner(serverLevel);
        if (owner == null || !owner.isAlive()) {
            if (requiresOwner()) {
                discard();
            } else {
                setTarget(null);
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (commandMode == MODE_ATTACK && getTarget() == null) {
            LivingEntity nearest = serverLevel.getEntitiesOfClass(
                    LivingEntity.class,
                    getBoundingBox().inflate(getAttackSearchRadius()),
                    candidate -> candidate != this && candidate != owner && isHostileTo(candidate)
            ).stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            setTarget(nearest);
        }
        LivingEntity target = getTarget();
        if (commandMode == MODE_ATTACK && target != null && target.isAlive() && isHostileTo(target)) {
            moveToward(target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D), getMoveSpeed());
            if (distanceToSqr(target) <= getAttackRange() * getAttackRange() && attackCooldown <= 0) {
                performSpiritAttack(target);
                attackCooldown = 20;
            }
            return;
        }
        setTarget(null);
        if (commandMode == MODE_WANDER) {
            if (tickCount % 30 == Math.floorMod(getId(), 30)) {
                Vec3 offset = new Vec3(random.nextDouble() * 12.0D - 6.0D, random.nextDouble() * 5.0D - 2.5D,
                        random.nextDouble() * 12.0D - 6.0D);
                moveToward(owner.position().add(offset), getMoveSpeed() * 0.8D);
            }
        } else {
            double followDistance = getFollowDistance();
            if (distanceToSqr(owner) > followDistance * followDistance) {
                moveToward(owner.position().add(0.0D, owner.getBbHeight() * 0.5D, 0.0D), getMoveSpeed());
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.82D));
            }
        }
    }

    protected abstract double getAttackSearchRadius();
    protected abstract double getAttackRange();
    protected abstract double getMoveSpeed();
    protected abstract double getFollowDistance();
    protected abstract float getSpiritDamage();

    protected boolean requiresOwner() {
        return true;
    }

    protected void performSpiritAttack(LivingEntity target) {
        DamageSource source = damageSources().mobAttack(this);
        if (!target.hurt(source, getSpiritDamage())) {
            return;
        }
        if (random.nextFloat() < 0.35F) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true, true), this);
        }
        if (random.nextFloat() < 0.45F && target instanceof net.minecraft.server.level.ServerPlayer player) {
            double current = TypeMoonWorldApi.addon("typemoonworld").magics().mana(player).current();
            if (current > 0.0D) {
                TypeMoonWorldApi.addon("typemoonworld").magics().mana(player).tryConsume(Math.min(4.0D, current));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.FALL)) {
            amount *= 0.5F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other) || other == this) {
            return true;
        }
        if (ownerId != null && other.getUUID().equals(ownerId)) {
            return true;
        }
        return other instanceof SummonedSpiritEntity spirit
                && ownerId != null && ownerId.equals(spirit.ownerId);
    }

    protected boolean isHostileTo(LivingEntity target) {
        return target != this && target.isAlive() && !isAlliedTo(target) && !target.isAlliedTo(this);
    }

    protected void moveToward(Vec3 destination, double speed) {
        Vec3 delta = destination.subtract(position());
        if (delta.lengthSqr() < 0.01D) {
            return;
        }
        setYRot((float)(Math.atan2(delta.z, delta.x) * 180.0D / Math.PI) - 90.0F);
        setDeltaMovement(delta.normalize().scale(speed));
        move(MoverType.SELF, getDeltaMovement());
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwner(LivingEntity owner) {
        ownerId = owner == null ? null : owner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner(ServerLevel level) {
        if (ownerId == null) {
            return null;
        }
        Entity entity = level.getEntity(ownerId);
        return entity instanceof LivingEntity living ? living : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    public int getCommandMode() {
        return commandMode;
    }

    public void setCommandMode(int mode) {
        commandMode = Math.max(MODE_FOLLOW, Math.min(MODE_ATTACK, mode));
        if (commandMode != MODE_ATTACK) {
            setTarget(null);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("SummonOwner", ownerId);
        }
        tag.putInt("SummonCommandMode", commandMode);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("SummonOwner") ? tag.getUUID("SummonOwner") : null;
        commandMode = Math.max(MODE_FOLLOW, Math.min(MODE_ATTACK, tag.getInt("SummonCommandMode")));
        setNoGravity(true);
    }
}
