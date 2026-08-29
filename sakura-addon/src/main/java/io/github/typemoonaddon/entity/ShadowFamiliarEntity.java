package io.github.typemoonaddon.entity;

import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.magic.GrailParticleService;
import io.github.typemoonaddon.magic.BlackMudHuntService;
import io.github.typemoonaddon.magic.ImaginaryShadowService;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowBindingService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEntities;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An owner-bound ground combat familiar created by Shadow Materialization. */
public final class ShadowFamiliarEntity extends Monster {
    public static final float MIN_SUMMON_SIZE = 1.0F;
    public static final float MAX_SUMMON_SIZE = 6.0F;
    public static final float MIN_SPLIT_SIZE = MIN_SUMMON_SIZE * 0.25F;

    public static final byte ATTACK_PHASE_IDLE = 0;
    public static final byte ATTACK_PHASE_SHADOW_BINDING = 1;
    public static final byte ATTACK_PHASE_VOID_ABSORPTION = 4;
    public static final int SHADOW_BINDING_CAST_TICKS = 20;
    public static final int VOID_ABSORPTION_CAST_TICKS = 30;

    private static final EntityDataAccessor<Boolean> DATA_FORMING = SynchedEntityData.defineId(
        ShadowFamiliarEntity.class,
        EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Byte> DATA_ATTACK_PHASE = SynchedEntityData.defineId(
        ShadowFamiliarEntity.class,
        EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Integer> DATA_ATTACK_PHASE_START = SynchedEntityData.defineId(
        ShadowFamiliarEntity.class,
        EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> DATA_VOID_ABSORPTION_CONNECTED = SynchedEntityData.defineId(
        ShadowFamiliarEntity.class,
        EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Boolean> DATA_SHADOW_BINDING_CONNECTED = SynchedEntityData.defineId(
        ShadowFamiliarEntity.class,
        EntityDataSerializers.BOOLEAN
    );

    private static final double FOLLOW_RANGE = 24.0D;
    private static final double OWNER_LEASH_RANGE = 32.0D;
    private static final double TELEPORT_RANGE = 28.0D;
    private static final int TARGET_SCAN_INTERVAL = 10;
    private static final int REJECTED_TARGET_TICKS = 60;
    private static final double VOID_ABSORPTION_RADIUS = 30.0D;
    private static final float VOID_ABSORPTION_MIN_DAMAGE = 30.0F;
    private static final float VOID_ABSORPTION_MAX_DAMAGE = 90.0F;
    private static final float VOID_ABSORPTION_SERVANT_DAMAGE = 150.0F;
    private static final double VOID_ABSORPTION_MANA_PER_DAMAGE = 1.0D;
    private static final int VOID_ABSORPTION_DAMAGE_INTERVAL_TICKS = 20;
    private static final int VOID_ABSORPTION_RETRY_TICKS = 40;
    private static final double SHADOW_BINDING_RANGE = 50.0D;
    private static final int SHADOW_BINDING_RETRY_TICKS = 100;
    private static final double SPREAD_RADIUS = 50.0D;
    private static final double SPREAD_MIN_RADIUS = 10.0D;
    private static final int GROUND_SEARCH_UP = 6;
    private static final int GROUND_SEARCH_DOWN = 16;
    private static final float SPLIT_HEALTH_RATIO = 0.3F;
    private static final float PASSIVE_SIZE_STEP = 0.05F;
    private static final double MANA_PER_SIZE_STEP = 1.0D;
    private static final String PALE_RIDER_INFECTION_LEVEL_TAG = "PaleRiderInfectionLevel";
    private static final String PALE_RIDER_INFECTION_IMMUNITY_TAG = "PaleRiderInfectionImmuneUntil";
    private static final String PALE_RIDER_CONTROLLED_TAG = "PaleRiderControlled";
    private static final String PALE_RIDER_PREVIOUS_NO_AI_TAG = "PaleRiderPreviousNoAi";
    private static final ResourceKey<MobEffect> PALE_RIDER_INFECTION = ResourceKey.create(
        Registries.MOB_EFFECT,
        ResourceLocation.fromNamespaceAndPath("typemoonworld", "pale_rider_infection")
    );
    private static final List<String> PALE_RIDER_INFECTION_STATE_TAGS = List.of(
        PALE_RIDER_INFECTION_LEVEL_TAG,
        "PaleRiderInfectionOwner",
        "PaleRiderInfectionUntil",
        "PaleRiderInfectionLastServiceTick",
        "PaleRiderInfectionWasInCalamity",
        "PaleRiderInfectionCalamityExitCleanse",
        PALE_RIDER_CONTROLLED_TAG,
        PALE_RIDER_PREVIOUS_NO_AI_TAG,
        "PaleRiderControlledNextAttack",
        "PaleRiderControlledNextTargetScan",
        "PaleRiderControlledCommandTarget",
        "PaleRiderControlledNextNavigation",
        "PaleRiderLethalNextTick"
    );
    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID temporarilyRejectedTarget;
    private long rejectedTargetUntil;
    private int voidAbsorptionCooldown;
    private int shadowBindingCooldown;
    private int ownerDismissalGeneration;
    private double storedGrowthMana;
    private boolean retaliationOnly;
    private int temporarySummonTicks;
    private ShadowCommandMode lastObservedCommandMode = ShadowCommandMode.FREE;

    public ShadowFamiliarEntity(EntityType<? extends ShadowFamiliarEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new MoveControl(this);
        this.setNoGravity(false);
        this.setPersistenceRequired();
        this.xpReward = 0;
        this.ensurePaleRiderInfectionImmunity();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, healthForSize(MIN_SUMMON_SIZE))
            .add(Attributes.ATTACK_DAMAGE, damageForSize(MIN_SUMMON_SIZE))
            .add(Attributes.ATTACK_KNOCKBACK, knockbackForSize(MIN_SUMMON_SIZE))
            .add(Attributes.MOVEMENT_SPEED, speedForSize(MIN_SUMMON_SIZE))
            .add(Attributes.KNOCKBACK_RESISTANCE, resistanceForSize(MIN_SUMMON_SIZE))
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
            .add(Attributes.SCALE, MIN_SUMMON_SIZE);
    }

    private static double healthForSize(float size) {
        return 20.0D + 10.0D * size;
    }

    private static double damageForSize(float size) {
        return 4.0D + 2.0D * size;
    }

    private static double speedForSize(float size) {
        return 0.32D - 0.02D * (size - MIN_SUMMON_SIZE);
    }

    private static double knockbackForSize(float size) {
        return 0.2D + 0.12D * (size - MIN_SUMMON_SIZE);
    }

    private static double resistanceForSize(float size) {
        return 0.2D + 0.12D * (size - MIN_SUMMON_SIZE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FORMING, false);
        builder.define(DATA_ATTACK_PHASE, ATTACK_PHASE_IDLE);
        builder.define(DATA_ATTACK_PHASE_START, 0);
        builder.define(DATA_VOID_ABSORPTION_CONNECTED, false);
        builder.define(DATA_SHADOW_BINDING_CONNECTED, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(false);
        return navigation;
    }

    public float getSummonSize() {
        return this.getScale();
    }

    public void setSummonSize(float requestedSize) {
        float size = Mth.clamp(requestedSize, MIN_SPLIT_SIZE, MAX_SUMMON_SIZE);
        float oldMaximum = this.getMaxHealth();
        float healthRatio = oldMaximum > 0.0F ? Mth.clamp(this.getHealth() / oldMaximum, 0.0F, 1.0F) : 1.0F;

        this.setAttributeBaseValue(Attributes.SCALE, size);
        this.setAttributeBaseValue(Attributes.MAX_HEALTH, healthForSize(size));
        this.setAttributeBaseValue(Attributes.ATTACK_DAMAGE, damageForSize(size));
        this.setAttributeBaseValue(Attributes.ATTACK_KNOCKBACK, knockbackForSize(size));
        this.setAttributeBaseValue(Attributes.MOVEMENT_SPEED, speedForSize(size));
        this.setAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE, resistanceForSize(size));
        this.refreshDimensions();

        if (this.isAlive()) {
            this.setHealth(Math.max(1.0F, this.getMaxHealth() * healthRatio));
        }
    }

    private void setAttributeBaseValue(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerDismissalGeneration(int generation) {
        this.ownerDismissalGeneration = generation;
    }

    public void configureTemporaryRetaliator(int lifetimeTicks) {
        this.retaliationOnly = true;
        this.temporarySummonTicks = Math.max(1, lifetimeTicks);
        this.setSummonSize(MAX_SUMMON_SIZE);
        this.stopCombatAndMovement();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.ownerId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.ownerId);
            return entity instanceof LivingEntity livingEntity ? livingEntity : null;
        }
        return this.level().getPlayerByUUID(this.ownerId);
    }

    public boolean isForming() {
        return this.entityData.get(DATA_FORMING);
    }

    public void setForming(boolean forming) {
        this.entityData.set(DATA_FORMING, forming);
        this.setNoGravity(false);
        if (forming) {
            this.stopCombatAndMovement();
        }
    }

    public byte getAttackPhase() {
        return this.entityData.get(DATA_ATTACK_PHASE);
    }

    public float getAttackPhaseProgress(float ageInTicks) {
        int duration = switch (this.getAttackPhase()) {
            case ATTACK_PHASE_SHADOW_BINDING -> SHADOW_BINDING_CAST_TICKS;
            case ATTACK_PHASE_VOID_ABSORPTION -> VOID_ABSORPTION_CAST_TICKS;
            default -> 1;
        };
        return Mth.clamp((ageInTicks - this.entityData.get(DATA_ATTACK_PHASE_START)) / duration, 0.0F, 1.0F);
    }

    public boolean isVoidAbsorptionConnected() {
        return this.entityData.get(DATA_VOID_ABSORPTION_CONNECTED);
    }

    private void setVoidAbsorptionConnected(boolean connected) {
        this.entityData.set(DATA_VOID_ABSORPTION_CONNECTED, connected);
    }

    public boolean isShadowBindingConnected() {
        return this.entityData.get(DATA_SHADOW_BINDING_CONNECTED);
    }

    private void setShadowBindingConnected(boolean connected) {
        this.entityData.set(DATA_SHADOW_BINDING_CONNECTED, connected);
    }

    private void setAttackPhase(byte phase) {
        if (this.entityData.get(DATA_ATTACK_PHASE) == phase) {
            return;
        }
        this.entityData.set(DATA_ATTACK_PHASE, phase);
        this.entityData.set(DATA_ATTACK_PHASE_START, this.tickCount);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == this) {
            return true;
        }
        if (PollutionService.areShadowFactionAllies(this, other)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        if (owner != null && (other == owner || owner.isAlliedTo(other) || other.isAlliedTo(owner))) {
            return true;
        }
        if (other instanceof ShadowFamiliarEntity familiar
            && this.ownerId != null
            && this.ownerId.equals(familiar.ownerId)) {
            return true;
        }
        return super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return this.isValidCombatTarget(target);
    }

    private boolean isValidCombatTarget(@Nullable LivingEntity target) {
        if (BlackMudHuntService.isHuntTarget(this, target)) {
            return this.isDamageableEnemy(target);
        }
        if (this.ownerId != null
            && this.getShadowCommandMode() == ShadowCommandMode.GATHER
            && !this.isAttackAroundEnabled()) {
            return false;
        }
        if (!this.isDamageableEnemy(target)) {
            return false;
        }
        if (this.ownerId != null && !this.hasUnlimitedOwnerRange()) {
            LivingEntity owner = this.getOwner();
            if (owner == null) {
                return false;
            }
            if (owner.distanceToSqr(target) > OWNER_LEASH_RANGE * OWNER_LEASH_RANGE
                || this.distanceToSqr(owner) > OWNER_LEASH_RANGE * OWNER_LEASH_RANGE) {
                return false;
            }
        }
        return true;
    }

    private boolean isDamageableEnemy(@Nullable LivingEntity target) {
        if (target == null
            || target == this
            || !target.isAlive()
            || target.isRemoved()
            || !target.isAttackable()
            || target.isInvulnerable()
            || this.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (this.ownerId != null) {
            LivingEntity owner = this.getOwner();
            if (owner == null || !owner.isAlive()) {
                return false;
            }
        }
        return true;
    }

    private boolean canSelectTarget(@Nullable LivingEntity target) {
        return this.isValidCombatTarget(target)
            && (this.temporarilyRejectedTarget == null
                || !this.temporarilyRejectedTarget.equals(target.getUUID())
                || this.level().getGameTime() >= this.rejectedTargetUntil);
    }

    private void rejectTarget(LivingEntity target) {
        this.temporarilyRejectedTarget = target.getUUID();
        this.rejectedTargetUntil = this.level().getGameTime() + REJECTED_TARGET_TICKS;
        if (this.getTarget() == target) {
            this.setTarget(null);
        }
    }

    private double combatDistanceToSqr(LivingEntity target) {
        return target.getBoundingBox().getCenter().distanceToSqr(this.getBoundingBox().getCenter());
    }

    private boolean shouldUseVoidAbsorption(@Nullable LivingEntity target) {
        return this.isVoidAbsorptionTarget(target)
            && this.isValidCombatTarget(target)
            && !PollutionService.shouldPrioritizeCorruption(target)
            && this.getAttackPhase() == ATTACK_PHASE_IDLE
            && this.isWithinVoidAbsorptionRange(target)
            && ShadowBindingService.isAtSourceCapacity(target);
    }

    private boolean isVoidAbsorptionTarget(@Nullable LivingEntity target) {
        return (target instanceof Mob || target instanceof Player) && this.isDamageableEnemy(target);
    }

    private boolean isWithinVoidAbsorptionRange(LivingEntity target) {
        return this.combatDistanceToSqr(target) <= VOID_ABSORPTION_RADIUS * VOID_ABSORPTION_RADIUS;
    }

    public boolean isWithinShadowBindingRange(LivingEntity target) {
        return this.combatDistanceToSqr(target) <= SHADOW_BINDING_RANGE * SHADOW_BINDING_RANGE;
    }

    @Nullable
    private ServerPlayer getServerPlayerOwner() {
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private boolean pulseVoidAbsorption(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        if (PollutionService.shouldPrioritizeCorruption(target)) {
            return false;
        }
        float damage = PollutionService.limitDamageForCorruptionPriority(
            target,
            this.voidAbsorptionDamage(target)
        );
        if (damage <= 0.0F) {
            return false;
        }
        double maximumManaDrain = Math.min(damage, target.getHealth()) * VOID_ABSORPTION_MANA_PER_DAMAGE;
        boolean hit = ShadowBindingService.absorbDirectDamage(target, owner, damage)
            || target.hurt(this.damageSources().indirectMagic(this, owner), damage);
        if (!hit) {
            return false;
        }

        double drainedMana = TypeMoonIntegration.drainMana(target, maximumManaDrain);
        if (drainedMana > 0.0D && !BlackMudHuntService.depositMana(this, drainedMana)) {
            this.addStoredGrowthMana(drainedMana);
        }
        this.spawnVoidAbsorptionHit(level, owner, target);
        PollutionService.expose(target, this, true);
        return true;
    }

    private void spawnVoidAbsorptionHit(ServerLevel level, ServerPlayer owner, LivingEntity target) {
        GrailParticleService.send(
            level,
            owner,
            ParticleTypes.SQUID_INK,
            target.getX(),
            target.getY() + target.getBbHeight() * 0.5D,
            target.getZ(),
            7,
            target.getBbWidth() * 0.35D,
            target.getBbHeight() * 0.25D,
            target.getBbWidth() * 0.35D,
            0.01D
        );
        level.playSound(
            null,
            target.blockPosition(),
            SoundEvents.WARDEN_ATTACK_IMPACT,
            SoundSource.HOSTILE,
            0.75F,
            0.7F
        );
    }

    private float voidAbsorptionDamage(LivingEntity target) {
        if (TypeMoonIntegration.isServantLike(target)) {
            return VOID_ABSORPTION_SERVANT_DAMAGE;
        }
        float sizeProgress = (this.getSummonSize() - MIN_SUMMON_SIZE) / (MAX_SUMMON_SIZE - MIN_SUMMON_SIZE);
        return Mth.lerp(
            Mth.clamp(sizeProgress, 0.0F, 1.0F),
            VOID_ABSORPTION_MIN_DAMAGE,
            VOID_ABSORPTION_MAX_DAMAGE
        );
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel serverLevel) {
            this.trySplit(serverLevel);
        }
        return hurt;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new ShadowBindingGoal());
        this.goalSelector.addGoal(1, new VoidAbsorptionGoal());
        this.goalSelector.addGoal(2, new SpreadOwnerGoal());
        this.goalSelector.addGoal(3, new FollowOwnerGoal());
        this.goalSelector.addGoal(4, new IdleDriftGoal());
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new FamiliarTargetGoal());
    }

    @Override
    public void tick() {
        this.ensurePaleRiderInfectionImmunity();
        super.tick();
        this.setNoGravity(false);
        if (!this.level().isClientSide && this.voidAbsorptionCooldown > 0) {
            this.voidAbsorptionCooldown--;
        }
        if (!this.level().isClientSide && this.shadowBindingCooldown > 0) {
            this.shadowBindingCooldown--;
        }
        if (!this.level().isClientSide && this.retaliationOnly && --this.temporarySummonTicks <= 0) {
            this.discard();
            return;
        }
        LivingEntity owner = this.ownerId == null ? null : this.getOwner();
        if (!this.level().isClientSide && owner != null) {
            if (owner instanceof ServerPlayer player
                && this.ownerDismissalGeneration != player.getData(
                    ModAttachments.IMAGINARY_SPACE.get()
                ).shadowDismissalGeneration()) {
                this.discard();
                return;
            }
            ShadowCommandMode commandMode = this.getShadowCommandMode();
            if (commandMode != this.lastObservedCommandMode) {
                this.lastObservedCommandMode = commandMode;
                this.stopCombatAndMovement();
            }
        }
        if (!this.level().isClientSide && (this.isForming() || this.ownerId != null && (owner == null || !owner.isAlive()))) {
            this.stopCombatAndMovement();
        }
        if (!this.level().isClientSide
            && !this.isForming()
            && this.isAlive()
            && !this.retaliationOnly
            && !BlackMudHuntService.isHunting(this)) {
            this.tickPassiveSize(owner);
        }
        if (!this.retaliationOnly && this.level() instanceof ServerLevel serverLevel) {
            this.trySplit(serverLevel);
        }
        if (!this.level().isClientSide) {
            BlackMudHuntService.HuntOrder huntOrder = BlackMudHuntService.orderFor(this);
            if (huntOrder != null) {
                this.tickHuntMovement(huntOrder);
            } else if (this.getShadowCommandMode() == ShadowCommandMode.HOLD) {
                this.holdPosition();
            }
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.getEffect().is(PALE_RIDER_INFECTION) && super.canBeAffected(effect);
    }

    private void ensurePaleRiderInfectionImmunity() {
        if (this.level().isClientSide) {
            return;
        }
        CompoundTag data = this.getPersistentData();
        data.putLong(PALE_RIDER_INFECTION_IMMUNITY_TAG, Long.MAX_VALUE);

        boolean wasControlled = data.getBoolean(PALE_RIDER_CONTROLLED_TAG);
        boolean previousNoAi = data.getBoolean(PALE_RIDER_PREVIOUS_NO_AI_TAG);
        if (data.contains(PALE_RIDER_INFECTION_LEVEL_TAG) || wasControlled) {
            for (String tag : PALE_RIDER_INFECTION_STATE_TAGS) {
                data.remove(tag);
            }
            if (wasControlled) {
                this.setNoAi(previousNoAi);
                this.setTarget(null);
            }
        }
        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(PALE_RIDER_INFECTION)
            .filter(this::hasEffect)
            .ifPresent(this::removeEffect);
    }

    private void tickPassiveSize(@Nullable LivingEntity owner) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.shrinkAndRecoverMana(owner);
        } else {
            this.growFromStoredMana();
        }
    }

    private void shrinkAndRecoverMana(@Nullable LivingEntity owner) {
        float currentSize = this.getSummonSize();
        if (currentSize <= MIN_SPLIT_SIZE) {
            return;
        }

        float sizeChange = Math.min(PASSIVE_SIZE_STEP, currentSize - MIN_SPLIT_SIZE);
        double releasedMana = sizeChange / PASSIVE_SIZE_STEP * MANA_PER_SIZE_STEP;
        double restoredMana = owner instanceof ServerPlayer player
            ? TypeMoonIntegration.restoreMana(player, releasedMana)
            : 0.0D;
        this.addStoredGrowthMana(releasedMana - restoredMana);
        this.setSummonSize(currentSize - sizeChange);
        this.spawnPassiveSizeParticles(ParticleTypes.SQUID_INK);
    }

    private void growFromStoredMana() {
        float currentSize = this.getSummonSize();
        if (currentSize >= MAX_SUMMON_SIZE || this.storedGrowthMana <= 1.0E-6D) {
            return;
        }

        float affordableGrowth = (float)(this.storedGrowthMana / MANA_PER_SIZE_STEP * PASSIVE_SIZE_STEP);
        float sizeChange = Math.min(PASSIVE_SIZE_STEP, Math.min(MAX_SUMMON_SIZE - currentSize, affordableGrowth));
        if (sizeChange <= 0.0F) {
            return;
        }
        double manaUsed = sizeChange / PASSIVE_SIZE_STEP * MANA_PER_SIZE_STEP;
        this.storedGrowthMana = Math.max(0.0D, this.storedGrowthMana - manaUsed);
        this.setSummonSize(currentSize + sizeChange);
        this.spawnPassiveSizeParticles(ParticleTypes.PORTAL);
    }

    private void spawnPassiveSizeParticles(net.minecraft.core.particles.ParticleOptions particle) {
        if (this.tickCount % 4 != 0 || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        GrailParticleService.send(
            level,
            this,
            particle,
            this.getX(),
            this.getY() + this.getBbHeight() * 0.5D,
            this.getZ(),
            4,
            this.getBbWidth() * 0.35D,
            this.getBbHeight() * 0.25D,
            this.getBbWidth() * 0.35D,
            0.01D
        );
    }

    private boolean trySplit(ServerLevel level) {
        if (!this.isAlive()
            || this.isForming()
            || BlackMudHuntService.isHunting(this)
            || this.getSummonSize() <= MIN_SPLIT_SIZE
            || this.getHealth() > this.getMaxHealth() * SPLIT_HEALTH_RATIO) {
            return false;
        }

        LivingEntity inheritedTarget = this.splitTarget();

        ShadowFamiliarEntity sibling = ModEntities.SHADOW_FAMILIAR.get().create(level);
        if (sibling == null) {
            return false;
        }

        float splitSize = Math.max(MIN_SPLIT_SIZE, this.getSummonSize() * 0.5F);
        double splitStoredMana = this.storedGrowthMana * 0.5D;
        float angle = this.getRandom().nextFloat() * Mth.TWO_PI;
        double directionX = Mth.cos(angle);
        double directionZ = Mth.sin(angle);
        double separation = Math.max(0.25D, this.getBbWidth() * 0.3D);

        sibling.moveTo(
            this.getX() + directionX * separation,
            this.getY(),
            this.getZ() + directionZ * separation,
            this.getYRot(),
            this.getXRot()
        );
        sibling.setOwnerId(this.ownerId);
        sibling.ownerDismissalGeneration = this.ownerDismissalGeneration;
        sibling.voidAbsorptionCooldown = this.voidAbsorptionCooldown;
        sibling.shadowBindingCooldown = this.shadowBindingCooldown;
        sibling.lastObservedCommandMode = this.lastObservedCommandMode;
        sibling.storedGrowthMana = splitStoredMana;
        sibling.setSummonSize(splitSize);
        sibling.setHealth(sibling.getMaxHealth());
        sibling.setDeltaMovement(directionX * 0.15D, 0.2D, directionZ * 0.15D);

        if (!level.addFreshEntity(sibling)) {
            return false;
        }

        this.stopCombatAndMovement();
        this.storedGrowthMana = splitStoredMana;
        this.setSummonSize(splitSize);
        this.setHealth(this.getMaxHealth());
        this.setPos(
            this.getX() - directionX * separation,
            this.getY(),
            this.getZ() - directionZ * separation
        );
        this.setDeltaMovement(-directionX * 0.15D, 0.2D, -directionZ * 0.15D);
        if (inheritedTarget != null && inheritedTarget.isAlive()) {
            this.setTarget(inheritedTarget);
            sibling.setTarget(inheritedTarget);
        }

        GrailParticleService.send(
            level,
            this,
            ParticleTypes.PORTAL,
            (this.getX() + sibling.getX()) * 0.5D,
            this.getY() + this.getBbHeight() * 0.5D,
            (this.getZ() + sibling.getZ()) * 0.5D,
            12,
            this.getBbWidth() * 0.5D,
            this.getBbHeight() * 0.35D,
            this.getBbWidth() * 0.5D,
            0.04D
        );
        level.playSound(
            null,
            this.blockPosition(),
            SoundEvents.SLIME_SQUISH_SMALL,
            SoundSource.HOSTILE,
            0.8F,
            0.8F + this.getRandom().nextFloat() * 0.2F
        );
        return true;
    }

    @Nullable
    private LivingEntity splitTarget() {
        LivingEntity current = this.getTarget();
        if (this.canSelectTarget(current)) {
            return current;
        }
        LivingEntity attacker = this.getLastHurtByMob();
        return this.canSelectTarget(attacker) ? attacker : null;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel serverLevel
            && !this.retaliationOnly
            && !this.isForming()
            && this.getSummonSize() > MIN_SPLIT_SIZE) {
            this.setHealth(this.getMaxHealth() * SPLIT_HEALTH_RATIO);
            if (this.trySplit(serverLevel)) {
                return;
            }
            this.setHealth(0.0F);
        }

        boolean wasDeadOrDying = this.isDeadOrDying();
        super.die(source);
        if (!wasDeadOrDying && this.isDeadOrDying() && this.level() instanceof ServerLevel serverLevel) {
            this.stopCombatAndMovement();
            this.setNoAi(true);
            ImaginaryShadowService.playDissolutionVisual(serverLevel, this, this);
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            this.fallDistance = 0.0F;
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.deathTime >= GameplayConfig.SHADOW_DISSOLUTION_TICKS
            && !this.level().isClientSide
            && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    private void stopCombatAndMovement() {
        if (!this.level().isClientSide) {
            ShadowBindingService.releaseBySource(this);
        }
        this.setTarget(null);
        this.setAggressive(false);
        this.setVoidAbsorptionConnected(false);
        this.setShadowBindingConnected(false);
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        this.setAttackPhase(ATTACK_PHASE_IDLE);
    }

    public void onCommandModeApplied() {
        if (!this.level().isClientSide) {
            this.lastObservedCommandMode = this.getShadowCommandMode();
            this.stopCombatAndMovement();
        }
    }

    public double takeStoredGrowthMana() {
        double stored = this.storedGrowthMana;
        this.storedGrowthMana = 0.0D;
        return stored;
    }

    public void addStoredGrowthMana(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        this.storedGrowthMana = this.storedGrowthMana > Double.MAX_VALUE - amount
            ? Double.MAX_VALUE
            : this.storedGrowthMana + amount;
    }

    public void prepareForHunt(LivingEntity target, float uniformSize) {
        this.lastObservedCommandMode = ShadowCommandMode.HUNT;
        this.stopCombatAndMovement();
        this.setSummonSize(uniformSize);
        this.setTarget(target);
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
    }

    public void finishHunt() {
        this.setNoGravity(false);
        this.fallDistance = 0.0F;
        this.stopCombatAndMovement();
    }

    private void tickHuntMovement(BlackMudHuntService.HuntOrder order) {
        LivingEntity target = order.target();
        if (this.getTarget() != target) {
            this.setTarget(target);
        }
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.fallDistance = 0.0F;
        this.getLookControl().setLookAt(target, 45.0F, 45.0F);
        this.faceHuntTarget(target);

        Vec3 offset = order.destination().subtract(this.position());
        double distance = offset.length();
        if (distance <= 1.0E-4D) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        double speed = Math.min(1.1D, Math.max(0.12D, distance * 0.22D));
        Vec3 movement = offset.scale(speed / distance);
        this.move(MoverType.SELF, movement);
        this.setDeltaMovement(Vec3.ZERO);
        this.faceHuntTarget(target);
    }

    private void faceHuntTarget(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 sourceCenter = this.getBoundingBox().getCenter();
        double deltaX = targetCenter.x - sourceCenter.x;
        double deltaY = targetCenter.y - sourceCenter.y;
        double deltaZ = targetCenter.z - sourceCenter.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float)(Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float)(-(Mth.atan2(deltaY, horizontalDistance) * Mth.RAD_TO_DEG));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    private void holdPosition() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, movement.y, 0.0D);
    }

    private ShadowCommandMode getShadowCommandMode() {
        if (this.retaliationOnly) {
            return ShadowCommandMode.HOLD;
        }
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer player
            ? player.getData(ModAttachments.IMAGINARY_SPACE.get()).activeShadowCommandMode()
            : ShadowCommandMode.FREE;
    }

    private boolean stopsCommandMovement() {
        ShadowCommandMode mode = this.getShadowCommandMode();
        return mode == ShadowCommandMode.HOLD || mode == ShadowCommandMode.HUNT;
    }

    private boolean isAttackAroundEnabled() {
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowAttackAround();
    }

    private boolean hasUnlimitedOwnerRange() {
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putFloat("SummonSize", this.getSummonSize());
        tag.putInt("VoidAbsorptionCooldown", this.voidAbsorptionCooldown);
        tag.putInt("ShadowBindingCooldown", this.shadowBindingCooldown);
        tag.putInt("OwnerDismissalGeneration", this.ownerDismissalGeneration);
        tag.putDouble("StoredGrowthMana", this.storedGrowthMana);
        tag.putBoolean("RetaliationOnly", this.retaliationOnly);
        tag.putInt("TemporarySummonTicks", this.temporarySummonTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.setSummonSize(tag.contains("SummonSize") ? tag.getFloat("SummonSize") : MIN_SUMMON_SIZE);
        this.voidAbsorptionCooldown = Mth.clamp(tag.getInt("VoidAbsorptionCooldown"), 0, VOID_ABSORPTION_RETRY_TICKS);
        this.shadowBindingCooldown = Mth.clamp(tag.getInt("ShadowBindingCooldown"), 0, SHADOW_BINDING_RETRY_TICKS);
        this.ownerDismissalGeneration = tag.getInt("OwnerDismissalGeneration");
        double savedGrowthMana = tag.getDouble("StoredGrowthMana");
        this.storedGrowthMana = Double.isFinite(savedGrowthMana)
            ? Math.max(0.0D, savedGrowthMana)
            : 0.0D;
        this.retaliationOnly = tag.getBoolean("RetaliationOnly");
        this.temporarySummonTicks = Math.max(0, tag.getInt("TemporarySummonTicks"));

        // Charging state is intentionally transient. A restart must never leave an immortal preview behind.
        this.setForming(false);
        this.setInvulnerable(false);
        this.setAttackPhase(ATTACK_PHASE_IDLE);
    }

    private Vec3 ownerFollowPosition(LivingEntity owner, double driftForward, double driftSide) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        double distance = 3.0D + this.getSummonSize() * 0.7D + driftForward;
        double sideOffset = ((this.getId() & 1) == 0 ? 1.0D : -1.0D) * (1.0D + driftSide);
        Vec3 horizontalPosition = owner.position().add(forward.scale(-distance)).add(side.scale(sideOffset));
        Vec3 groundPosition = this.findGroundPosition(horizontalPosition.x, horizontalPosition.z, owner.getY());
        return groundPosition != null ? groundPosition : new Vec3(horizontalPosition.x, owner.getY(), horizontalPosition.z);
    }

    private Vec3 gatherPosition(LivingEntity owner) {
        double angle = Math.floorMod(this.getId(), 12) * (Math.PI * 2.0D / 12.0D);
        double radius = (owner.getBbWidth() + this.getBbWidth()) * 0.5D + 0.75D;
        double x = owner.getX() + Math.cos(angle) * radius;
        double z = owner.getZ() + Math.sin(angle) * radius;
        Vec3 groundPosition = this.findGroundPosition(x, z, owner.getY());
        return groundPosition != null ? groundPosition : new Vec3(x, owner.getY(), z);
    }

    private Vec3 commandFollowPosition(LivingEntity owner) {
        return this.getShadowCommandMode() == ShadowCommandMode.GATHER
            ? this.gatherPosition(owner)
            : this.ownerFollowPosition(owner, 0.0D, 0.0D);
    }

    private boolean tryTeleportNearOwner(LivingEntity owner) {
        double radius = 4.0D + this.getBbWidth() * 0.75D;
        int start = this.random.nextInt(12);
        for (int ring = 0; ring < 2; ring++) {
            double ringRadius = radius + ring * Math.max(2.0D, this.getBbWidth() * 0.5D);
            for (int index = 0; index < 12; index++) {
                double angle = (start + index) * (Math.PI * 2.0D / 12.0D);
                Vec3 candidate = this.findGroundPosition(
                    owner.getX() + Math.cos(angle) * ringRadius,
                    owner.getZ() + Math.sin(angle) * ringRadius,
                    owner.getY()
                );
                if (candidate == null) {
                    continue;
                }
                this.getNavigation().stop();
                this.setDeltaMovement(Vec3.ZERO);
                this.moveTo(candidate.x, candidate.y, candidate.z, owner.getYRot(), 0.0F);
                if (this.level() instanceof ServerLevel serverLevel) {
                    GrailParticleService.send(
                        serverLevel,
                        owner,
                        ParticleTypes.PORTAL,
                        this.getX(),
                        this.getY() + this.getBbHeight() * 0.5D,
                        this.getZ(),
                        12,
                        this.getBbWidth() * 0.25D,
                        this.getBbHeight() * 0.2D,
                        this.getBbWidth() * 0.25D,
                        0.04D
                    );
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Vec3 findGroundPosition(double x, double z, double referenceY) {
        int chunkX = SectionPos.blockToSectionCoord(Mth.floor(x));
        int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(z));
        if (!this.level().getChunkSource().hasChunk(chunkX, chunkZ)) {
            return null;
        }

        int referenceFloorY = Mth.floor(referenceY - 0.01D);
        int maximumOffset = Math.max(GROUND_SEARCH_UP, GROUND_SEARCH_DOWN);
        for (int offset = 0; offset <= maximumOffset; offset++) {
            int upwardY = referenceFloorY + offset;
            if (offset <= GROUND_SEARCH_UP) {
                Vec3 candidate = this.groundSurfaceAt(x, z, upwardY);
                if (candidate != null && this.isSafeGroundPosition(candidate)) {
                    return candidate;
                }
            }
            if (offset > 0 && offset <= GROUND_SEARCH_DOWN) {
                Vec3 candidate = this.groundSurfaceAt(x, z, referenceFloorY - offset);
                if (candidate != null && this.isSafeGroundPosition(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    @Nullable
    private Vec3 groundSurfaceAt(double x, double z, int blockY) {
        if (blockY < this.level().getMinBuildHeight() || blockY >= this.level().getMaxBuildHeight()) {
            return null;
        }
        BlockPos supportPos = BlockPos.containing(x, blockY, z);
        VoxelShape supportShape = this.level().getBlockState(supportPos).getCollisionShape(this.level(), supportPos);
        return supportShape.isEmpty()
            ? null
            : new Vec3(x, supportPos.getY() + supportShape.max(Direction.Axis.Y), z);
    }

    private boolean isSafeGroundPosition(Vec3 position) {
        AABB movedBounds = this.getBoundingBox().move(
            position.x - this.getX(),
            position.y - this.getY(),
            position.z - this.getZ()
        );
        if (!this.areBoundsLoaded(movedBounds)
            || !this.level().noCollision(this, movedBounds)
            || this.level().containsAnyLiquid(movedBounds)) {
            return false;
        }

        double halfWidth = Math.max(0.0D, this.getBbWidth() * 0.5D - 0.05D);
        return this.hasGroundSurface(position.x, position.y, position.z)
            && this.hasGroundSurface(position.x - halfWidth, position.y, position.z - halfWidth)
            && this.hasGroundSurface(position.x - halfWidth, position.y, position.z + halfWidth)
            && this.hasGroundSurface(position.x + halfWidth, position.y, position.z - halfWidth)
            && this.hasGroundSurface(position.x + halfWidth, position.y, position.z + halfWidth);
    }

    private boolean areBoundsLoaded(AABB bounds) {
        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));
        return this.level().getChunkSource().hasChunk(minChunkX, minChunkZ)
            && this.level().getChunkSource().hasChunk(minChunkX, maxChunkZ)
            && this.level().getChunkSource().hasChunk(maxChunkX, minChunkZ)
            && this.level().getChunkSource().hasChunk(maxChunkX, maxChunkZ);
    }

    private boolean hasGroundSurface(double x, double feetY, double z) {
        BlockPos supportPos = BlockPos.containing(x, feetY - 0.01D, z);
        VoxelShape supportShape = this.level().getBlockState(supportPos).getCollisionShape(this.level(), supportPos);
        return !supportShape.isEmpty()
            && Math.abs(supportPos.getY() + supportShape.max(Direction.Axis.Y) - feetY) < 0.01D;
    }

    private final class FamiliarTargetGoal extends Goal {
        private int ownerAttackTimestamp;
        private int ownerHurtTimestamp;
        private int familiarHurtTimestamp;
        private int scanCooldown;
        private TargetSource source = TargetSource.NONE;

        private FamiliarTargetGoal() {
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return !ShadowFamiliarEntity.this.isForming();
        }

        @Override
        public boolean canContinueToUse() {
            return !ShadowFamiliarEntity.this.isForming();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity owner = ShadowFamiliarEntity.this.getOwner();
            if (ShadowFamiliarEntity.this.ownerId != null && (owner == null || !owner.isAlive())) {
                this.clearTarget();
                return;
            }

            ShadowCommandMode commandMode = ShadowFamiliarEntity.this.getShadowCommandMode();
            if (commandMode == ShadowCommandMode.HUNT) {
                BlackMudHuntService.HuntOrder order = BlackMudHuntService.orderFor(ShadowFamiliarEntity.this);
                if (order == null) {
                    this.clearTarget();
                } else {
                    this.select(order.target(), TargetSource.HUNT);
                }
                return;
            }

            if (owner != null && !ShadowFamiliarEntity.this.retaliationOnly) {
                if (commandMode == ShadowCommandMode.GATHER
                    && !ShadowFamiliarEntity.this.isAttackAroundEnabled()) {
                    this.clearTarget();
                    return;
                }
                if (commandMode == ShadowCommandMode.HOLD) {
                    this.processOwnerlessRetaliation();
                } else {
                    this.processOwnerEvents(owner);
                }
            } else {
                this.processOwnerlessRetaliation();
                return;
            }

            LivingEntity current = ShadowFamiliarEntity.this.getTarget();
            if (this.source == TargetSource.ACTIVE_SCAN && !ShadowFamiliarEntity.this.isAttackAroundEnabled()) {
                this.clearTarget();
                current = null;
            }
            if (ShadowFamiliarEntity.this.getAttackPhase() == ATTACK_PHASE_VOID_ABSORPTION
                && ShadowFamiliarEntity.this.isVoidAbsorptionTarget(current)) {
                return;
            }
            if (ShadowFamiliarEntity.this.getAttackPhase() == ATTACK_PHASE_SHADOW_BINDING
                && current != null
                && ShadowBindingService.hasBinding(ShadowFamiliarEntity.this, current)) {
                return;
            }
            if (!ShadowFamiliarEntity.this.canSelectTarget(current)) {
                this.clearTarget();
            }

            if (ShadowFamiliarEntity.this.getTarget() == null
                && ShadowFamiliarEntity.this.isAttackAroundEnabled()
                && --this.scanCooldown <= 0) {
                this.scanCooldown = TARGET_SCAN_INTERVAL;
                this.findNearbyEnemy(owner);
            }
        }

        private void processOwnerEvents(LivingEntity owner) {
            int attackTimestamp = owner.getLastHurtMobTimestamp();
            LivingEntity ownerTarget = owner.getLastHurtMob();
            if (attackTimestamp != this.ownerAttackTimestamp) {
                this.ownerAttackTimestamp = attackTimestamp;
                if (ShadowFamiliarEntity.this.canSelectTarget(ownerTarget)) {
                    this.select(ownerTarget, TargetSource.OWNER_ATTACK);
                }
            }

            int hurtTimestamp = owner.getLastHurtByMobTimestamp();
            LivingEntity attacker = owner.getLastHurtByMob();
            if (hurtTimestamp != this.ownerHurtTimestamp) {
                this.ownerHurtTimestamp = hurtTimestamp;
                if (this.source.priority <= TargetSource.OWNER_HURT.priority
                    && ShadowFamiliarEntity.this.canSelectTarget(attacker)) {
                    this.select(attacker, TargetSource.OWNER_HURT);
                }
            }

            int selfTimestamp = ShadowFamiliarEntity.this.getLastHurtByMobTimestamp();
            LivingEntity selfAttacker = ShadowFamiliarEntity.this.getLastHurtByMob();
            if (selfTimestamp != this.familiarHurtTimestamp) {
                this.familiarHurtTimestamp = selfTimestamp;
                if (this.source.priority <= TargetSource.SELF_HURT.priority
                    && ShadowFamiliarEntity.this.canSelectTarget(selfAttacker)) {
                    this.select(selfAttacker, TargetSource.SELF_HURT);
                }
            }
        }

        private void processOwnerlessRetaliation() {
            int selfTimestamp = ShadowFamiliarEntity.this.getLastHurtByMobTimestamp();
            LivingEntity attacker = ShadowFamiliarEntity.this.getLastHurtByMob();
            if (selfTimestamp != this.familiarHurtTimestamp) {
                this.familiarHurtTimestamp = selfTimestamp;
                if (ShadowFamiliarEntity.this.canSelectTarget(attacker)) {
                    this.select(attacker, TargetSource.SELF_HURT);
                }
            }
        }

        private void findNearbyEnemy(LivingEntity owner) {
            LivingEntity searchCenter = ShadowFamiliarEntity.this.hasUnlimitedOwnerRange()
                ? ShadowFamiliarEntity.this
                : owner;
            AABB searchArea = searchCenter.getBoundingBox().inflate(FOLLOW_RANGE);
            List<LivingEntity> candidates = ShadowFamiliarEntity.this.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                candidate -> (candidate instanceof Mob || candidate instanceof Player)
                    && ShadowFamiliarEntity.this.canSelectTarget(candidate)
            );
            if (candidates.isEmpty()) {
                return;
            }
            List<ShadowFamiliarEntity> ownedFamiliars = ShadowFamiliarEntity.this.level().getEntitiesOfClass(
                ShadowFamiliarEntity.class,
                searchArea,
                familiar -> ShadowFamiliarEntity.this.ownerId != null
                    && ShadowFamiliarEntity.this.ownerId.equals(familiar.ownerId)
            );
            candidates.stream().min(
                Comparator.comparingInt((LivingEntity candidate) -> this.targetingCount(candidate, ownedFamiliars))
                    .thenComparingDouble(searchCenter::distanceToSqr)
            ).ifPresent(target -> this.select(target, TargetSource.ACTIVE_SCAN));
        }

        private int targetingCount(LivingEntity candidate, List<ShadowFamiliarEntity> ownedFamiliars) {
            int count = 0;
            for (ShadowFamiliarEntity familiar : ownedFamiliars) {
                if (familiar.getTarget() == candidate) {
                    count++;
                }
            }
            return count;
        }

        private void select(LivingEntity target, TargetSource newSource) {
            ShadowFamiliarEntity.this.setTarget(target);
            this.source = newSource;
        }

        private void clearTarget() {
            ShadowFamiliarEntity.this.setTarget(null);
            this.source = TargetSource.NONE;
        }
    }

    private enum TargetSource {
        NONE(0),
        ACTIVE_SCAN(1),
        SELF_HURT(2),
        OWNER_HURT(3),
        OWNER_ATTACK(4),
        HUNT(5);

        private final int priority;

        TargetSource(int priority) {
            this.priority = priority;
        }
    }

    private final class FollowOwnerGoal extends Goal {
        @Nullable
        private LivingEntity owner;
        private int recalculateTicks;

        private FollowOwnerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = ShadowFamiliarEntity.this.getOwner();
            if (candidate == null
                || !candidate.isAlive()
                || ShadowFamiliarEntity.this.isForming()
                || ShadowFamiliarEntity.this.stopsCommandMovement()
                || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.SPREAD
                || ShadowFamiliarEntity.this.hasUnlimitedOwnerRange()
                    && ShadowFamiliarEntity.this.getShadowCommandMode() != ShadowCommandMode.GATHER
                || ShadowFamiliarEntity.this.getTarget() != null) {
                return false;
            }
            Vec3 destination = ShadowFamiliarEntity.this.commandFollowPosition(candidate);
            if (ShadowFamiliarEntity.this.position().distanceToSqr(destination) < 6.25D
                && ShadowFamiliarEntity.this.distanceToSqr(candidate) < TELEPORT_RANGE * TELEPORT_RANGE) {
                return false;
            }
            this.owner = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                && this.owner.isAlive()
                && !ShadowFamiliarEntity.this.isForming()
                && !ShadowFamiliarEntity.this.stopsCommandMovement()
                && ShadowFamiliarEntity.this.getShadowCommandMode() != ShadowCommandMode.SPREAD
                && (!ShadowFamiliarEntity.this.hasUnlimitedOwnerRange()
                    || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.GATHER)
                && ShadowFamiliarEntity.this.getTarget() == null
                && ShadowFamiliarEntity.this.position().distanceToSqr(
                    ShadowFamiliarEntity.this.commandFollowPosition(this.owner)
                ) > 2.25D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.recalculateTicks = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            ShadowFamiliarEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            ShadowFamiliarEntity.this.getLookControl().setLookAt(this.owner, 20.0F, 20.0F);
            if (ShadowFamiliarEntity.this.distanceToSqr(this.owner) > TELEPORT_RANGE * TELEPORT_RANGE
                && ShadowFamiliarEntity.this.tryTeleportNearOwner(this.owner)) {
                return;
            }
            if (--this.recalculateTicks <= 0) {
                this.recalculateTicks = 8;
                Vec3 destination = ShadowFamiliarEntity.this.commandFollowPosition(this.owner);
                ShadowFamiliarEntity.this.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.0D);
            }
        }
    }

    private final class IdleDriftGoal extends Goal {
        @Nullable
        private Vec3 destination;
        private int duration;

        private IdleDriftGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity owner = ShadowFamiliarEntity.this.getOwner();
            if (owner == null
                || ShadowFamiliarEntity.this.isForming()
                || ShadowFamiliarEntity.this.stopsCommandMovement()
                || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.SPREAD
                || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.GATHER
                || ShadowFamiliarEntity.this.getTarget() != null
                || ShadowFamiliarEntity.this.random.nextInt(40) != 0) {
                return false;
            }
            Vec3 horizontalCandidate;
            double referenceY;
            if (ShadowFamiliarEntity.this.hasUnlimitedOwnerRange()) {
                double angle = ShadowFamiliarEntity.this.random.nextDouble() * Math.PI * 2.0D;
                double radius = 2.0D + ShadowFamiliarEntity.this.random.nextDouble() * 5.0D;
                horizontalCandidate = ShadowFamiliarEntity.this.position().add(
                    Math.cos(angle) * radius,
                    0.0D,
                    Math.sin(angle) * radius
                );
                referenceY = ShadowFamiliarEntity.this.getY();
            } else {
                horizontalCandidate = ShadowFamiliarEntity.this.ownerFollowPosition(
                    owner,
                    ShadowFamiliarEntity.this.random.nextDouble() * 1.5D - 0.75D,
                    ShadowFamiliarEntity.this.random.nextDouble() * 1.2D - 0.6D
                );
                referenceY = owner.getY();
            }
            Vec3 candidate = ShadowFamiliarEntity.this.findGroundPosition(
                horizontalCandidate.x,
                horizontalCandidate.z,
                referenceY
            );
            if (candidate == null) {
                return false;
            }
            this.destination = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.destination != null
                && this.duration > 0
                && !ShadowFamiliarEntity.this.stopsCommandMovement()
                && ShadowFamiliarEntity.this.getTarget() == null
                && !ShadowFamiliarEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.duration = 30;
            if (this.destination != null) {
                ShadowFamiliarEntity.this.getNavigation().moveTo(
                    this.destination.x,
                    this.destination.y,
                    this.destination.z,
                    0.65D
                );
            }
        }

        @Override
        public void tick() {
            this.duration--;
        }

        @Override
        public void stop() {
            this.destination = null;
            ShadowFamiliarEntity.this.getNavigation().stop();
        }
    }

    private final class SpreadOwnerGoal extends Goal {
        @Nullable
        private LivingEntity owner;
        @Nullable
        private Vec3 destination;
        private long nextSearchTick;
        private int recalculateTicks;
        private int travelTicks;

        private SpreadOwnerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidateOwner = ShadowFamiliarEntity.this.getOwner();
            if (candidateOwner == null
                || !candidateOwner.isAlive()
                || ShadowFamiliarEntity.this.isForming()
                || ShadowFamiliarEntity.this.getTarget() != null
                || ShadowFamiliarEntity.this.getShadowCommandMode() != ShadowCommandMode.SPREAD
                || ShadowFamiliarEntity.this.level().getGameTime() < this.nextSearchTick
                    && ShadowFamiliarEntity.this.distanceToSqr(candidateOwner) <= 45.0D * 45.0D) {
                return false;
            }
            Vec3 candidate = this.findDestination(candidateOwner);
            if (candidate == null) {
                this.nextSearchTick = ShadowFamiliarEntity.this.level().getGameTime() + 20L;
                return false;
            }
            this.owner = candidateOwner;
            this.destination = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                && this.owner.isAlive()
                && this.destination != null
                && this.travelTicks > 0
                && !ShadowFamiliarEntity.this.isForming()
                && ShadowFamiliarEntity.this.getTarget() == null
                && ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.SPREAD
                && this.owner.position().distanceToSqr(this.destination) <= SPREAD_RADIUS * SPREAD_RADIUS
                && ShadowFamiliarEntity.this.position().distanceToSqr(this.destination) > 2.25D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.travelTicks = 160;
            this.recalculateTicks = 0;
            this.nextSearchTick = ShadowFamiliarEntity.this.level().getGameTime() + 80L
                + ShadowFamiliarEntity.this.random.nextInt(81);
        }

        @Override
        public void tick() {
            this.travelTicks--;
            if (this.owner == null || this.destination == null) {
                return;
            }
            if (!ShadowFamiliarEntity.this.hasUnlimitedOwnerRange()
                && ShadowFamiliarEntity.this.distanceToSqr(this.owner) > 60.0D * 60.0D
                && ShadowFamiliarEntity.this.tickCount % 40 == 0
                && ShadowFamiliarEntity.this.tryTeleportNearOwner(this.owner)) {
                return;
            }
            if (--this.recalculateTicks <= 0) {
                this.recalculateTicks = 12;
                ShadowFamiliarEntity.this.getNavigation().moveTo(
                    this.destination.x,
                    this.destination.y,
                    this.destination.z,
                    0.8D
                );
            }
        }

        @Override
        public void stop() {
            this.owner = null;
            this.destination = null;
            ShadowFamiliarEntity.this.getNavigation().stop();
        }

        @Nullable
        private Vec3 findDestination(LivingEntity owner) {
            double usableRadius = Math.max(SPREAD_MIN_RADIUS, SPREAD_RADIUS - ShadowFamiliarEntity.this.getBbWidth());
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = ShadowFamiliarEntity.this.random.nextDouble() * Math.PI * 2.0D;
                double horizontalRadius = SPREAD_MIN_RADIUS
                    + ShadowFamiliarEntity.this.random.nextDouble() * (usableRadius - SPREAD_MIN_RADIUS);
                Vec3 candidate = ShadowFamiliarEntity.this.findGroundPosition(
                    owner.getX() + Math.cos(angle) * horizontalRadius,
                    owner.getZ() + Math.sin(angle) * horizontalRadius,
                    owner.getY()
                );
                if (candidate == null
                    || owner.position().distanceToSqr(candidate) > SPREAD_RADIUS * SPREAD_RADIUS
                    || this.hasNearbyOwnedFamiliar(candidate)) {
                    continue;
                }
                return candidate;
            }
            return null;
        }

        private boolean hasNearbyOwnedFamiliar(Vec3 candidate) {
            AABB candidateBounds = ShadowFamiliarEntity.this.getBoundingBox().move(
                candidate.x - ShadowFamiliarEntity.this.getX(),
                candidate.y - ShadowFamiliarEntity.this.getY(),
                candidate.z - ShadowFamiliarEntity.this.getZ()
            ).inflate(Math.max(3.0D, ShadowFamiliarEntity.this.getBbWidth() * 0.5D));
            return !ShadowFamiliarEntity.this.level().getEntitiesOfClass(
                ShadowFamiliarEntity.class,
                candidateBounds,
                familiar -> familiar != ShadowFamiliarEntity.this
                    && ShadowFamiliarEntity.this.ownerId != null
                    && ShadowFamiliarEntity.this.ownerId.equals(familiar.ownerId)
            ).isEmpty();
        }
    }

    private final class VoidAbsorptionGoal extends Goal {
        @Nullable
        private LivingEntity channelTarget;
        private int castTicks;
        private int damageTicks;
        private int pathTicks;
        private boolean connected;
        private boolean targetBound;

        private VoidAbsorptionGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !ShadowFamiliarEntity.this.isForming()
                && (ShadowFamiliarEntity.this.getShadowCommandMode() != ShadowCommandMode.GATHER
                    || ShadowFamiliarEntity.this.isAttackAroundEnabled())
                && ShadowFamiliarEntity.this.getServerPlayerOwner() != null
                && ShadowFamiliarEntity.this.shouldUseVoidAbsorption(ShadowFamiliarEntity.this.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            if (ShadowFamiliarEntity.this.isForming()
                || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.GATHER
                    && !ShadowFamiliarEntity.this.isAttackAroundEnabled()) {
                return false;
            }
            ServerPlayer owner = ShadowFamiliarEntity.this.getServerPlayerOwner();
            if (owner == null
                || !owner.isAlive()
                || this.channelTarget != ShadowFamiliarEntity.this.getTarget()
                || !ShadowFamiliarEntity.this.isVoidAbsorptionTarget(this.channelTarget)
                || PollutionService.shouldPrioritizeCorruption(this.channelTarget)) {
                return false;
            }
            if (this.connected || this.castTicks > 0) {
                return true;
            }
            return this.channelTarget == ShadowFamiliarEntity.this.getTarget()
                && ShadowFamiliarEntity.this.shouldUseVoidAbsorption(this.channelTarget);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.channelTarget = ShadowFamiliarEntity.this.getTarget();
            this.castTicks = 0;
            this.damageTicks = 0;
            this.pathTicks = 0;
            this.connected = false;
            this.targetBound = false;
            ShadowFamiliarEntity.this.setVoidAbsorptionConnected(false);
            ShadowFamiliarEntity.this.setAggressive(true);
        }

        private void beginCasting() {
            this.castTicks = VOID_ABSORPTION_CAST_TICKS;
            this.damageTicks = 0;
            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_VOID_ABSORPTION);
            ShadowFamiliarEntity.this.level().playSound(
                null,
                ShadowFamiliarEntity.this.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE,
                1.0F,
                0.55F
            );
        }

        @Override
        public void tick() {
            LivingEntity target = this.channelTarget;
            if (target == null) {
                return;
            }
            ShadowFamiliarEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.connected) {
                this.tickConnected(target);
                return;
            }

            if (this.castTicks > 0) {
                this.tickCasting(target);
                return;
            }

            if (!ShadowFamiliarEntity.this.isWithinVoidAbsorptionRange(target)) {
                if (--this.pathTicks <= 0) {
                    this.pathTicks = 8;
                    if (!ShadowFamiliarEntity.this.getNavigation().moveTo(target, 1.0D)
                        && ShadowFamiliarEntity.this.getSensing().hasLineOfSight(target)) {
                        ShadowFamiliarEntity.this.getMoveControl().setWantedPosition(
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            1.0D
                        );
                    }
                }
                return;
            }

            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            if (ShadowFamiliarEntity.this.voidAbsorptionCooldown == 0) {
                this.beginCasting();
            }
        }

        private void tickCasting(LivingEntity target) {
            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            if (ShadowFamiliarEntity.this.level() instanceof ServerLevel serverLevel && this.castTicks % 5 == 0) {
                Vec3 center = ShadowFamiliarEntity.this.getBoundingBox().getCenter();
                double spread = 0.8D + ShadowFamiliarEntity.this.getSummonSize() * 0.25D;
                GrailParticleService.send(
                    serverLevel,
                    ShadowFamiliarEntity.this,
                    ParticleTypes.REVERSE_PORTAL,
                    center.x,
                    center.y,
                    center.z,
                    12,
                    spread,
                    spread,
                    spread,
                    0.08D
                );
            }

            this.castTicks--;
            if (this.castTicks == 0) {
                if (!ShadowFamiliarEntity.this.isWithinVoidAbsorptionRange(target)) {
                    ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
                    return;
                }
                if (!ImaginaryShadowService.beginAuxiliaryBinding(ShadowFamiliarEntity.this.getUUID(), target)) {
                    this.disconnect();
                    return;
                }
                this.targetBound = true;
                this.connected = true;
                ShadowFamiliarEntity.this.setVoidAbsorptionConnected(true);
                this.damageTicks = 0;
            }
        }

        private void tickConnected(LivingEntity target) {
            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            if (!(ShadowFamiliarEntity.this.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!ImaginaryShadowService.maintainAuxiliaryBinding(ShadowFamiliarEntity.this.getUUID(), target)) {
                this.disconnect();
                return;
            }

            ImaginaryShadowService.syncVoidAbsorptionLink(ShadowFamiliarEntity.this, target);
            if (this.damageTicks > 0) {
                this.damageTicks--;
                return;
            }

            ServerPlayer owner = ShadowFamiliarEntity.this.getServerPlayerOwner();
            if (owner == null || !ShadowFamiliarEntity.this.pulseVoidAbsorption(serverLevel, owner, target)) {
                this.disconnect();
                return;
            }
            // Reapply the shared player-style lock after hurt() so the hit cannot impart velocity.
            ImaginaryShadowService.maintainAuxiliaryBinding(ShadowFamiliarEntity.this.getUUID(), target);
            this.damageTicks = VOID_ABSORPTION_DAMAGE_INTERVAL_TICKS - 1;
        }

        private void disconnect() {
            this.releaseTargetBinding();
            this.connected = false;
            ShadowFamiliarEntity.this.setVoidAbsorptionConnected(false);
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
            ShadowFamiliarEntity.this.voidAbsorptionCooldown = VOID_ABSORPTION_RETRY_TICKS;
            ShadowFamiliarEntity.this.setTarget(null);
        }

        private void releaseTargetBinding() {
            if (this.targetBound && this.channelTarget != null) {
                ImaginaryShadowService.endAuxiliaryBinding(
                    ShadowFamiliarEntity.this.getUUID(),
                    this.channelTarget
                );
            }
            this.targetBound = false;
        }

        @Override
        public void stop() {
            this.releaseTargetBinding();
            this.channelTarget = null;
            this.castTicks = 0;
            this.damageTicks = 0;
            this.connected = false;
            this.targetBound = false;
            ShadowFamiliarEntity.this.setVoidAbsorptionConnected(false);
            ShadowFamiliarEntity.this.setAggressive(false);
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
        }
    }

    private final class ShadowBindingGoal extends Goal {
        @Nullable
        private LivingEntity bindingTarget;
        private int castTicks;
        private int pathTicks;
        private boolean connected;

        private ShadowBindingGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ShadowFamiliarEntity.this.getTarget();
            return !ShadowFamiliarEntity.this.isForming()
                && ShadowFamiliarEntity.this.shadowBindingCooldown == 0
                && (ShadowFamiliarEntity.this.getShadowCommandMode() != ShadowCommandMode.GATHER
                    || ShadowFamiliarEntity.this.isAttackAroundEnabled())
                && ShadowFamiliarEntity.this.isValidCombatTarget(target)
                && ShadowFamiliarEntity.this.getSensing().hasLineOfSight(target)
                && ShadowBindingService.canBegin(ShadowFamiliarEntity.this, target);
        }

        @Override
        public boolean canContinueToUse() {
            if (ShadowFamiliarEntity.this.isForming()
                || ShadowFamiliarEntity.this.getShadowCommandMode() == ShadowCommandMode.GATHER
                    && !ShadowFamiliarEntity.this.isAttackAroundEnabled()
                || this.bindingTarget == null) {
                return false;
            }
            if (this.connected) {
                return ShadowBindingService.isControlling(
                    ShadowFamiliarEntity.this,
                    this.bindingTarget
                );
            }
            return this.bindingTarget == ShadowFamiliarEntity.this.getTarget()
                && ShadowFamiliarEntity.this.isValidCombatTarget(this.bindingTarget)
                && ShadowBindingService.canBegin(ShadowFamiliarEntity.this, this.bindingTarget);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.bindingTarget = ShadowFamiliarEntity.this.getTarget();
            this.castTicks = 0;
            this.pathTicks = 0;
            this.connected = false;
            ShadowFamiliarEntity.this.setShadowBindingConnected(false);
            ShadowFamiliarEntity.this.setAggressive(true);
        }

        private void beginCasting() {
            this.castTicks = SHADOW_BINDING_CAST_TICKS;
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_SHADOW_BINDING);
            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            ShadowFamiliarEntity.this.level().playSound(
                null,
                ShadowFamiliarEntity.this.blockPosition(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK,
                SoundSource.HOSTILE,
                0.55F,
                0.7F
            );
        }

        @Override
        public void tick() {
            LivingEntity target = this.bindingTarget;
            if (target == null) {
                return;
            }
            ShadowFamiliarEntity.this.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.connected) {
                ShadowFamiliarEntity.this.getNavigation().stop();
                ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
                if (!ShadowBindingService.hasBinding(ShadowFamiliarEntity.this, target)) {
                    this.disconnect(target);
                }
                return;
            }

            if (!ShadowFamiliarEntity.this.isWithinShadowBindingRange(target)) {
                this.castTicks = 0;
                ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
                if (--this.pathTicks <= 0) {
                    this.pathTicks = 8;
                    if (!ShadowFamiliarEntity.this.getNavigation().moveTo(target, 1.0D)
                        && ShadowFamiliarEntity.this.getSensing().hasLineOfSight(target)) {
                        ShadowFamiliarEntity.this.getMoveControl().setWantedPosition(
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            1.0D
                        );
                    }
                }
                return;
            }

            ShadowFamiliarEntity.this.getNavigation().stop();
            ShadowFamiliarEntity.this.setDeltaMovement(Vec3.ZERO);
            if (this.castTicks == 0) {
                this.beginCasting();
            }

            if (ShadowFamiliarEntity.this.level() instanceof ServerLevel serverLevel && this.castTicks % 4 == 0) {
                Vec3 center = ShadowFamiliarEntity.this.getBoundingBox().getCenter();
                GrailParticleService.send(
                    serverLevel,
                    ShadowFamiliarEntity.this,
                    ParticleTypes.SQUID_INK,
                    center.x,
                    center.y,
                    center.z,
                    8,
                    ShadowFamiliarEntity.this.getBbWidth() * 0.45D,
                    ShadowFamiliarEntity.this.getBbHeight() * 0.35D,
                    ShadowFamiliarEntity.this.getBbWidth() * 0.45D,
                    0.015D
                );
            }

            if (--this.castTicks > 0) {
                return;
            }
            if (!ShadowBindingService.begin(ShadowFamiliarEntity.this, target)) {
                this.disconnect(target);
                return;
            }
            this.connected = true;
            ShadowFamiliarEntity.this.setShadowBindingConnected(true);
        }

        private void disconnect(LivingEntity target) {
            ShadowBindingService.releaseBySource(ShadowFamiliarEntity.this);
            ShadowFamiliarEntity.this.setShadowBindingConnected(false);
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
            ShadowFamiliarEntity.this.shadowBindingCooldown = SHADOW_BINDING_RETRY_TICKS;
            ShadowFamiliarEntity.this.rejectTarget(target);
        }

        @Override
        public void stop() {
            LivingEntity target = this.bindingTarget;
            boolean wasActive = this.castTicks > 0 || this.connected;
            boolean preserveShell = this.connected
                && target != null
                && ShadowBindingService.hasDormantBinding(ShadowFamiliarEntity.this, target);
            if (!preserveShell) {
                ShadowBindingService.releaseBySource(ShadowFamiliarEntity.this);
            }
            this.bindingTarget = null;
            this.castTicks = 0;
            this.pathTicks = 0;
            this.connected = false;
            ShadowFamiliarEntity.this.setShadowBindingConnected(false);
            ShadowFamiliarEntity.this.setAggressive(false);
            ShadowFamiliarEntity.this.setAttackPhase(ATTACK_PHASE_IDLE);
            if (wasActive) {
                ShadowFamiliarEntity.this.shadowBindingCooldown = SHADOW_BINDING_RETRY_TICKS;
                if (target != null
                    && target.isAlive()
                    && ShadowFamiliarEntity.this.getTarget() == target
                    && !ShadowBindingService.isAtSourceCapacity(target)) {
                    ShadowFamiliarEntity.this.rejectTarget(target);
                }
            }
        }
    }
}
