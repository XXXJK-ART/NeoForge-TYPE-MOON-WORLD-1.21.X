package com.example.typemoonaddon.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
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
    private UUID priorityTargetId;
    private int commandMode = MODE_FOLLOW;
    private int attackCooldown;
    private Vec3 wanderCenter;

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
        tickStoredCurse(serverLevel);
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
        boolean canFly = hasSolidBelow(serverLevel, 5);
        setNoGravity(canFly);
        if (!canFly) {
            Vec3 falling = position().add(0.0D, -0.08D, 0.0D);
            if (serverLevel.getWorldBorder().isWithinBounds(falling)) setPos(falling.x, falling.y, falling.z);
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (commandMode == MODE_ATTACK && getTarget() == null) {
            LivingEntity priority = priorityTargetId == null ? null : resolve(priorityTargetId, serverLevel);
            if (priority != null && isHostileTo(priority)) setTarget(priority);
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
            if (wanderCenter == null) wanderCenter = owner.position();
            if (tickCount % 30 == Math.floorMod(getId(), 30)) {
                Vec3 offset = new Vec3(random.nextDouble() * 15.0D - 7.5D, random.nextDouble() * 4.0D - 2.0D,
                        random.nextDouble() * 15.0D - 7.5D);
                moveToward(wanderCenter.add(offset), getMoveSpeed() * 0.8D);
            }
        } else {
            wanderCenter = owner.position();
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
        float manaChance = this instanceof EvilSpiritEntity ? 0.10F : 0.05F;
        double manaDrain = this instanceof EvilSpiritEntity ? 15.0D : 5.0D;
        if (random.nextFloat() < manaChance && target instanceof net.minecraft.server.level.ServerPlayer player) {
            double current = TypeMoonWorldApi.addon("typemoonworld").magics().mana(player).current();
            if (current > 0.0D) {
                TypeMoonWorldApi.addon("typemoonworld").magics().mana(player).tryConsume(Math.min(manaDrain, current));
            }
        }
        if (random.nextFloat() < 0.10F) {
            CompoundTag data = target.getPersistentData();
            data.putInt("TypeMoonSpiritCurseStacks", Math.min(5, data.getInt("TypeMoonSpiritCurseStacks") + 1));
            long until = target.level().getGameTime() + 100L;
            data.putLong("TypeMoonSpiritCurseUntil", until);
            data.putLong("TypeMoonSpiritCurseNextTick", Math.min(data.getLong("TypeMoonSpiritCurseNextTick"), target.level().getGameTime()));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.FALL)) {
            amount *= 0.5F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && this instanceof EvilSpiritEntity && source.getEntity() instanceof LivingEntity attacker
                && attacker != this && !isAlliedTo(attacker)) {
            priorityTargetId = attacker.getUUID();
            setCommandMode(MODE_ATTACK);
            setTarget(attacker);
        }
        return hurt;
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
        Vec3 next = position().add(getDeltaMovement());
        if (level().getWorldBorder().isWithinBounds(next) && !isImpenetrable(next)) {
            setPos(next.x, next.y, next.z);
        }
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwner(LivingEntity owner) {
        ownerId = owner == null ? null : owner.getUUID();
    }

    public void setPriorityTarget(LivingEntity target) {
        priorityTargetId = target == null ? null : target.getUUID();
        if (target != null) {
            setCommandMode(MODE_ATTACK);
            setTarget(target);
        }
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
        if (commandMode == MODE_WANDER && getOwnerId() != null && level() instanceof ServerLevel level) {
            LivingEntity owner = getOwner(level);
            if (owner != null) wanderCenter = owner.position();
        }
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
        if (priorityTargetId != null) tag.putUUID("SummonPriorityTarget", priorityTargetId);
        tag.putInt("SummonCommandMode", commandMode);
        if (wanderCenter != null) {
            tag.putDouble("WanderX", wanderCenter.x);
            tag.putDouble("WanderY", wanderCenter.y);
            tag.putDouble("WanderZ", wanderCenter.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("SummonOwner") ? tag.getUUID("SummonOwner") : null;
        priorityTargetId = tag.hasUUID("SummonPriorityTarget") ? tag.getUUID("SummonPriorityTarget") : null;
        commandMode = Math.max(MODE_FOLLOW, Math.min(MODE_ATTACK, tag.getInt("SummonCommandMode")));
        if (tag.contains("WanderX")) wanderCenter = new Vec3(tag.getDouble("WanderX"), tag.getDouble("WanderY"), tag.getDouble("WanderZ"));
        setNoGravity(true);
    }

    private LivingEntity resolve(UUID uuid, ServerLevel level) {
        Entity entity = level.getEntity(uuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean hasSolidBelow(ServerLevel level, int depth) {
        BlockPos origin = blockPosition();
        for (int i = 1; i <= depth; i++) {
            if (!level.getBlockState(origin.below(i)).isAir()) return true;
        }
        return false;
    }

    private boolean isImpenetrable(Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        var block = level().getBlockState(pos).getBlock();
        return block == Blocks.BARRIER || block == Blocks.BEDROCK;
    }

    private void tickStoredCurse(ServerLevel level) {
        LivingEntity target = getTarget();
        if (target == null) return;
        CompoundTag data = target.getPersistentData();
        int stacks = data.getInt("TypeMoonSpiritCurseStacks");
        long now = level.getGameTime();
        if (stacks <= 0) return;
        if (now >= data.getLong("TypeMoonSpiritCurseUntil")) {
            data.remove("TypeMoonSpiritCurseStacks");
            data.remove("TypeMoonSpiritCurseUntil");
            data.remove("TypeMoonSpiritCurseNextTick");
        } else if (now >= data.getLong("TypeMoonSpiritCurseNextTick")) {
            target.hurt(damageSources().magic(), 2.0F * stacks);
            data.putLong("TypeMoonSpiritCurseNextTick", now + 20L);
        }
    }
}
