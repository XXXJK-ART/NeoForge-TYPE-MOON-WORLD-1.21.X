package io.github.typemoonaddon.entity;

import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.compat.TypeMoonContractBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.data.ImaginarySpaceData.ShadowCommandMode;
import io.github.typemoonaddon.item.BlackShadowSpawnEggItem;
import io.github.typemoonaddon.magic.BlackMudHuntService;
import io.github.typemoonaddon.magic.BlackMudService;
import io.github.typemoonaddon.magic.GrailParticleService;
import io.github.typemoonaddon.magic.ImaginaryShadowService;
import io.github.typemoonaddon.magic.MagicOutputService;
import io.github.typemoonaddon.magic.BlackShadowNightService;
import io.github.typemoonaddon.magic.PollutionService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowBindingMagicService;
import io.github.typemoonaddon.shadowlogic.magic.ShadowBindingService;
import io.github.typemoonaddon.magic.SummonBlackMudService;
import io.github.typemoonaddon.magic.TypeMoonIntegration;
import io.github.typemoonaddon.registry.ModAttachments;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A faceless hostile shade wrapped in black-and-crimson hanging folds. */
public final class BlackShadowEntity extends Monster {
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_WALK = 1;
    public static final byte ACTION_ATTACK = 2;

    public static final double TARGET_RANGE = 50.0D;
    public static final double HUNT_RANGE = 64.0D;
    public static final int TARGET_SCAN_INTERVAL = 20;
    public static final int ATTACK_WINDUP_TICKS = 10;
    public static final int ATTACK_TOTAL_TICKS = 22;
    public static final int ATTACK_COOLDOWN_TICKS = 30;
    public static final int TARGET_TURN_TICKS = 4;
    public static final int REPATH_INTERVAL = 8;
    public static final int MAX_PATH_FAILURES = 3;
    public static final double OWNER_LEASH_RANGE = 48.0D;
    public static final double SPREAD_MIN_RADIUS = 12.0D;
    public static final double SPREAD_MAX_RADIUS = 40.0D;

    private static final double CHASE_SPEED = 1.08D;
    private static final double WANDER_SPEED = 0.72D;
    private static final double COMMAND_SPEED = 0.88D;
    private static final double ATTACK_REACH_PADDING = 1.35D;
    private static final int REJECTED_TARGET_TICKS = 60;
    private static final int GROUND_SEARCH_UP = 6;
    private static final int GROUND_SEARCH_DOWN = 12;
    private static final int BLACK_MUD_COVERAGE_INTERVAL = 20;
    private static final int HEROIC_SPIRIT_DEVOURER_INITIAL_DELAY = 5 * 20;
    private static final int HEROIC_SPIRIT_DEVOURER_COOLDOWN = 30 * 20;
    private static final int HEROIC_SPIRIT_DEVOURER_RETRY = 5 * 20;
    private static final int SHADOW_BINDING_TARGET_LIMIT = 5;
    private static final int SHADOW_BINDING_SCAN_INTERVAL = 20;
    private static final double VOID_ABSORPTION_RANGE = 30.0D;
    private static final int VOID_ABSORPTION_CAST_TICKS = 30;
    private static final int VOID_ABSORPTION_DAMAGE_INTERVAL = 20;
    private static final int VOID_ABSORPTION_COOLDOWN = 40;
    private static final float VOID_ABSORPTION_DAMAGE = 150.0F;
    private static final double RANGED_STANDOFF_MIN_RANGE = 20.0D;
    private static final double RANGED_STANDOFF_MAX_RANGE = 30.0D;
    private static final double OPENING_TELEPORT_MIN_RADIUS = 1.0D;
    private static final double OPENING_TELEPORT_MAX_RADIUS = 3.0D;
    private static final int OPENING_TELEPORT_ATTEMPTS = 64;
    private static final int OPENING_SHADOW_ART_TIMEOUT_TICKS = 3 * 20;
    private static final int BLOCKED_TELEPORT_TICKS = 20;
    private static final double BLACK_MUD_ASCENT_SPEED = 0.16D;
    private static final double DARKNESS_AURA_RADIUS = 100.0D;
    private static final int DARKNESS_AURA_INTERVAL = 10;
    private static final int DARKNESS_AURA_DURATION = 30;
    public static final int MAGIC_OUTPUT_CHARGE_TICKS = GameplayConfig.MAGIC_OUTPUT_CHARGE_TICKS;
    private static final int MAGIC_OUTPUT_DISMISS_DELAY_TICKS =
        GameplayConfig.BLACK_SHADOW_MAGIC_OUTPUT_DISMISS_DELAY_TICKS;
    public static final float MAGIC_OUTPUT_ORB_START_RADIUS = GameplayConfig.MAGIC_OUTPUT_ORB_START_RADIUS;
    public static final float MAGIC_OUTPUT_ORB_END_RADIUS = GameplayConfig.MAGIC_OUTPUT_ORB_END_RADIUS;

    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(
        BlackShadowEntity.class,
        EntityDataSerializers.BYTE
    );
    private static final EntityDataAccessor<Integer> DATA_ACTION_START = SynchedEntityData.defineId(
        BlackShadowEntity.class,
        EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> DATA_MAGIC_OUTPUT_CHARGE = SynchedEntityData.defineId(
        BlackShadowEntity.class,
        EntityDataSerializers.INT
    );

    @Nullable
    private UUID ownerId;
    private boolean grailOwned;
    private int ownerDismissalGeneration;
    private int attackCooldown;
    @Nullable
    private UUID temporarilyRejectedTarget;
    @Nullable
    private UUID automaticTarget;
    @Nullable
    private UUID openingShadowArtTarget;
    private boolean openingTeleportAttempted;
    private int openingShadowArtTicks;
    @Nullable
    private UUID servantCheckTarget;
    private long servantCheckUntil;
    private boolean servantCheckResult;
    private long rejectedTargetUntil;
    private ShadowCommandMode lastObservedCommandMode = ShadowCommandMode.FREE;
    private final Set<UUID> shadowBindingTargets = new HashSet<>();
    private int blackMudCoverageCooldown;
    private int shadowBindingScanCooldown;
    private final int[] shadowArtRecoveryTicks = new int[GameplayConfig.SHADOW_ART_RIBBON_COUNT];
    private boolean blackMudCoverageReady;
    private int blackMudExpansionRadius = SummonBlackMudService.BLACK_SHADOW_INITIAL_RADIUS;
    @Nullable
    private BlockPos blackMudExpansionCenter;
    private int heroicSpiritDevourerCooldown = HEROIC_SPIRIT_DEVOURER_INITIAL_DELAY;
    @Nullable
    private UUID voidAbsorptionTarget;
    private int voidAbsorptionCastTicks;
    private int voidAbsorptionDamageTicks;
    private int voidAbsorptionCooldown;
    private boolean voidAbsorptionConnected;
    @Nullable
    private Vec3 magicOutputOrigin;
    @Nullable
    private Vec3 magicOutputDismissPosition;
    private int magicOutputDismissTicks;
    @Nullable
    private UUID nightMissionTarget;
    private double storedMana;

    public BlackShadowEntity(EntityType<? extends BlackShadowEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 8;
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0D)
            .add(Attributes.ATTACK_DAMAGE, 7.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.27D)
            .add(Attributes.FOLLOW_RANGE, TARGET_RANGE)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_IDLE);
        builder.define(DATA_ACTION_START, 0);
        builder.define(DATA_MAGIC_OUTPUT_CHARGE, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation navigation = new GroundPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(true);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        MobSpawnType reason,
        @Nullable SpawnGroupData spawnData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        if (reason == MobSpawnType.SPAWN_EGG) {
            BlackShadowSpawnEggItem.SpawnOwner owner = BlackShadowSpawnEggItem.currentOwner();
            if (owner != null) {
                this.ownerId = owner.ownerId();
                this.grailOwned = owner.grailAscended();
                LivingEntity livingOwner = this.getOwner();
                if (livingOwner instanceof ServerPlayer player) {
                    this.ownerDismissalGeneration = player.getData(
                        ModAttachments.IMAGINARY_SPACE.get()
                    ).shadowDismissalGeneration();
                }
                this.setPersistenceRequired();
            }
        }
        return result;
    }

    public byte getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    public float getAttackProgress(float ageInTicks) {
        if (this.getAction() != ACTION_ATTACK) {
            return 0.0F;
        }
        return Mth.clamp(
            (ageInTicks - this.entityData.get(DATA_ACTION_START)) / ATTACK_TOTAL_TICKS,
            0.0F,
            1.0F
        );
    }

    public int getMagicOutputChargeTicks() {
        return this.entityData.get(DATA_MAGIC_OUTPUT_CHARGE);
    }

    private void setAction(byte action) {
        if (this.entityData.get(DATA_ACTION) == action) {
            return;
        }
        this.entityData.set(DATA_ACTION, action);
        this.entityData.set(DATA_ACTION_START, this.tickCount);
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        if (ownerId != null) {
            this.setPersistenceRequired();
        }
    }

    public void setOwnerDismissalGeneration(int generation) {
        this.ownerDismissalGeneration = generation;
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Nullable
    public LivingEntity getOwner() {
        if (ownerId == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel level) {
            Entity entity = level.getEntity(ownerId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
            return level.getServer().getPlayerList().getPlayer(ownerId);
        }
        return this.level().getPlayerByUUID(ownerId);
    }

    public void beginNightMission(LivingEntity target) {
        this.nightMissionTarget = target.getUUID();
        this.temporarilyRejectedTarget = null;
        this.rejectedTargetUntil = 0L;
        this.automaticTarget = null;
        this.setTarget(target);
        this.setPersistenceRequired();
    }

    public void endNightMission() {
        this.nightMissionTarget = null;
        this.stopCombatAndMovement();
    }

    public boolean hasNightMission() {
        return this.nightMissionTarget != null;
    }

    public boolean isNightMissionTarget(@Nullable LivingEntity target) {
        return target != null
            && this.nightMissionTarget != null
            && this.nightMissionTarget.equals(target.getUUID());
    }

    @Nullable
    private LivingEntity getNightMissionTarget() {
        if (this.nightMissionTarget == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(this.nightMissionTarget);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Nullable
    private LivingEntity getActiveOwner() {
        LivingEntity owner = this.getOwner();
        return owner != null
            && owner.isAlive()
            && !owner.isRemoved()
            && owner.level() == this.level()
            && (!(owner instanceof Player player) || !player.isSpectator() || this.hasNightMission())
            ? owner
            : null;
    }

    public boolean isGrailOwned() {
        if (!grailOwned && this.getOwner() instanceof Player player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailWormAscended()) {
            grailOwned = true;
        }
        return grailOwned;
    }

    public int shadowArtRecoveryTicks(int index) {
        return index < 0 || index >= shadowArtRecoveryTicks.length ? 0 : shadowArtRecoveryTicks[index];
    }

    public void breakShadowArtRibbon(int index) {
        if (index >= 0 && index < shadowArtRecoveryTicks.length) {
            shadowArtRecoveryTicks[index] = GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS;
        }
    }

    public boolean tickShadowArtRecovery(int index) {
        if (index < 0 || index >= shadowArtRecoveryTicks.length || shadowArtRecoveryTicks[index] <= 0) {
            return false;
        }
        return --shadowArtRecoveryTicks[index] == 0;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == this || ownerId != null && ownerId.equals(other.getUUID())) {
            return true;
        }
        if (PollutionService.areShadowFactionAllies(this, other)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        if (owner instanceof ServerPlayer master
            && other instanceof LivingEntity servant
            && !PollutionService.isPolluted(servant)
            && TypeMoonContractBridge.isContractedTo(master, servant)) {
            return true;
        }
        return owner != null && (owner.isAlliedTo(other) || other.isAlliedTo(owner))
            || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return this.isLegalTarget(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (this.level().isClientSide || previous == target) {
            return;
        }
        if (previous != null) {
            if (!ShadowArtService.shouldKeepOwnedTarget(this, previous)) {
                ShadowArtService.releaseOwnedTarget(this, previous);
            }
        }
        if (target == null) {
            this.clearOpeningShadowArtAttack();
            return;
        }
        if (TypeMoonServantBridge.isServantOrCardUser(target)) {
            this.openingShadowArtTarget = target.getUUID();
            this.openingTeleportAttempted = false;
            this.openingShadowArtTicks = 0;
        } else {
            this.clearOpeningShadowArtAttack();
        }
    }

    public boolean canStartNightMissionAgainst(LivingEntity target) {
        return this.isLegalTarget(target, true);
    }

    private boolean isLegalTarget(@Nullable LivingEntity target) {
        return this.isLegalTarget(target, false);
    }

    private boolean isLegalTarget(@Nullable LivingEntity target, boolean ignoreOwnerLeash) {
        if (target == null
            || target == this
            || !target.isAlive()
            || target.isRemoved()
            || target.level() != this.level()
            || !target.isAttackable()
            || target.isInvulnerable()
            || target instanceof ShadowFamiliarEntity
            || target instanceof BlackShadowEntity
            || this.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        LivingEntity owner = this.ownerId == null ? null : this.getActiveOwner();
        return ignoreOwnerLeash
            || this.hasNightMission()
            || this.ownerId == null
            || owner != null && this.distanceToSqr(owner) <= OWNER_LEASH_RANGE * OWNER_LEASH_RANGE;
    }

    private boolean isServantTarget(LivingEntity target) {
        long gameTime = this.level().getGameTime();
        UUID targetId = target.getUUID();
        if (targetId.equals(this.servantCheckTarget) && gameTime < this.servantCheckUntil) {
            return this.servantCheckResult;
        }
        this.servantCheckTarget = targetId;
        this.servantCheckUntil = gameTime + 10L;
        this.servantCheckResult = TypeMoonServantBridge.isServantOrCardUser(target);
        return this.servantCheckResult;
    }

    private boolean isSelectableTarget(@Nullable LivingEntity target) {
        return this.isLegalTarget(target)
            && (this.isNightMissionTarget(target)
                || this.temporarilyRejectedTarget == null
                || !this.temporarilyRejectedTarget.equals(target.getUUID())
                || this.level().getGameTime() >= this.rejectedTargetUntil)
            && this.distanceToSqr(target) <= this.targetDropRangeSqr();
    }

    private double targetDropRangeSqr() {
        double range = this.hasNightMission()
            ? 80.0D
            : this.getShadowCommandMode() == ShadowCommandMode.HUNT ? HUNT_RANGE : TARGET_RANGE;
        return range * range;
    }

    private void rejectTarget(LivingEntity target) {
        this.temporarilyRejectedTarget = target.getUUID();
        this.rejectedTargetUntil = this.level().getGameTime() + REJECTED_TARGET_TICKS;
        if (this.getTarget() == target) {
            this.setTarget(null);
        }
    }

    private boolean canAct() {
        return this.isAlive()
            && !this.isNoAi()
            && this.magicOutputDismissTicks <= 0
            && (this.ownerId == null || this.getActiveOwner() != null)
            && (this.hasNightMission()
                || this.getShadowCommandMode() != ShadowCommandMode.HOLD
                    && this.getShadowCommandMode() != ShadowCommandMode.DISMISS);
    }

    private boolean canPursueTarget(@Nullable LivingEntity target) {
        return this.canAct() && this.isSelectableTarget(target);
    }

    private boolean mudPreparedForCombat() {
        return this.ownerId == null || this.blackMudCoverageReady;
    }

    private boolean permitsAutomaticTargeting() {
        if (!this.canAct()) {
            return false;
        }
        ShadowCommandMode mode = this.getShadowCommandMode();
        if (mode == ShadowCommandMode.HUNT) {
            return false;
        }
        if (this.ownerId == null || mode == ShadowCommandMode.FREE) {
            return true;
        }
        return this.isAttackAroundEnabled();
    }

    private ShadowCommandMode getShadowCommandMode() {
        if (this.hasNightMission()) {
            return ShadowCommandMode.FREE;
        }
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer player
            ? player.getData(ModAttachments.IMAGINARY_SPACE.get()).activeShadowCommandMode()
            : ShadowCommandMode.FREE;
    }

    private boolean isAttackAroundEnabled() {
        LivingEntity owner = this.getOwner();
        return owner instanceof ServerPlayer player
            && player.getData(ModAttachments.IMAGINARY_SPACE.get()).shadowAttackAround();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new EscapeWaterGoal());
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BlackShadowMeleeGoal());
        this.goalSelector.addGoal(3, new GatherOwnerGoal());
        this.goalSelector.addGoal(4, new SpreadOwnerGoal());
        this.goalSelector.addGoal(5, new SmoothWanderGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new BlackShadowTargetGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        this.tickBlackMudBuoyancy();
        this.tickDarknessAura();

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.voidAbsorptionCooldown > 0) {
            this.voidAbsorptionCooldown--;
        }
        if (this.heroicSpiritDevourerCooldown > 0) {
            this.heroicSpiritDevourerCooldown--;
        }
        LivingEntity owner = this.ownerId == null ? null : this.getActiveOwner();
        if (owner instanceof ServerPlayer player
            && this.ownerDismissalGeneration != player.getData(
                ModAttachments.IMAGINARY_SPACE.get()
            ).shadowDismissalGeneration()) {
            this.dismiss(player);
            return;
        }
        if (this.magicOutputDismissTicks > 0) {
            this.tickMagicOutputDismissal(owner);
            return;
        }

        ShadowCommandMode commandMode = this.getShadowCommandMode();
        if (commandMode != this.lastObservedCommandMode) {
            this.lastObservedCommandMode = commandMode;
            this.stopCombatAndMovement();
        }
        boolean mudReady = owner instanceof ServerPlayer player
            && this.tickBlackMudCoverage(player);
        boolean actionStopped = this.ownerId != null && owner == null || commandMode == ShadowCommandMode.HOLD;
        if (actionStopped) {
            this.stopCombatAndMovement();
        } else if (owner instanceof ServerPlayer player && this.canAct()) {
            this.tickCombatSkills(player, mudReady);
        }

        if (this.getAction() != ACTION_ATTACK) {
            Vec3 movement = this.getDeltaMovement();
            boolean moving = !this.getNavigation().isDone()
                && movement.x * movement.x + movement.z * movement.z > 1.0E-5D;
            this.setAction(moving ? ACTION_WALK : ACTION_IDLE);
        }
    }

    private void tickBlackMudBuoyancy() {
        if (!BlackMudService.isSubmergedInBlackMud(this)) {
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(movement.x, Math.max(movement.y, BLACK_MUD_ASCENT_SPEED), movement.z);
        this.fallDistance = 0.0F;
        this.hurtMarked = true;
    }

    private void tickDarknessAura() {
        if (this.tickCount % DARKNESS_AURA_INTERVAL != 0) {
            return;
        }
        double radiusSqr = DARKNESS_AURA_RADIUS * DARKNESS_AURA_RADIUS;
        for (LivingEntity entity : this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(DARKNESS_AURA_RADIUS),
            entity -> entity.isAlive()
                && entity.distanceToSqr(this) <= radiusSqr
                && !BlackMudService.isImmune(entity)
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
        )) {
            entity.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                DARKNESS_AURA_DURATION,
                0,
                false,
                false,
                false
            ));
            entity.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                DARKNESS_AURA_DURATION,
                0,
                false,
                false,
                false
            ));
        }
    }

    private void tickCombatSkills(ServerPlayer owner, boolean mudReady) {
        boolean hasCombatTarget = this.getTarget() != null
            || ShadowArtService.hasRibbons(this)
            || this.voidAbsorptionTarget != null
            || this.getMagicOutputChargeTicks() > 0;
        if (!hasCombatTarget) {
            this.cleanupShadowBindings();
            return;
        }

        if (!ShadowArtService.payBlackShadowRibbonUpkeep(this)) {
            return;
        }

        if (this.tickMagicOutput(owner)) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (this.isDirectAbsorptionTarget(target)) {
            this.clearOpeningShadowArtAttack();
            this.cleanupShadowBindings();
            ShadowArtService.maintainBlackShadowHeldTargets(this);
            this.tickVoidAbsorption(owner, true);
            return;
        }
        boolean openingShadowArt = this.prepareOpeningShadowArtAttack(target);
        if (!openingShadowArt) {
            this.tickHeroicSpiritDevourer(owner);
        }
        this.cleanupShadowBindings();
        boolean targetInMud = target != null && BlackMudService.isOnOrInBlackMud(target);
        boolean canFullyCorrupt = targetInMud && PollutionService.canBecomeFullyCorrupted(target);
        if (canFullyCorrupt && !openingShadowArt) {
            ShadowArtService.releaseOwnedTarget(this, target);
            this.tickShadowBindings(owner, mudReady);
        } else {
            if (!this.shadowBindingTargets.isEmpty()) {
                ShadowBindingService.releaseBySource(this);
                this.shadowBindingTargets.clear();
            }
            ShadowArtService.tickBlackShadow(this, target, mudReady, openingShadowArt);
        }
        if (openingShadowArt) {
            this.finishOpeningShadowArtAttack(target);
        }
        if (this.isOpeningShadowArtAttack(target)) {
            return;
        }
        if (target != null && this.tickCount % 20 == 0
            && PollutionService.isPollutedBy(target, this)) {
            PollutionService.addBlackShadowPollution(this, GameplayConfig.BLACK_SHADOW_POLLUTION_PER_SECOND);
        }
        if (this.tryStartMagicOutput(owner)) {
            return;
        }
        this.tickVoidAbsorption(owner, mudReady);
    }

    private boolean prepareOpeningShadowArtAttack(@Nullable LivingEntity target) {
        if (target == null
            || this.openingShadowArtTarget == null
            || !this.openingShadowArtTarget.equals(target.getUUID())) {
            return false;
        }
        if (!this.openingTeleportAttempted) {
            this.openingTeleportAttempted = true;
            this.teleportAround(
                target,
                OPENING_TELEPORT_MIN_RADIUS,
                OPENING_TELEPORT_MAX_RADIUS,
                OPENING_TELEPORT_ATTEMPTS
            );
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.openingShadowArtTicks++;
        return true;
    }

    private void finishOpeningShadowArtAttack(@Nullable LivingEntity target) {
        if (ShadowArtService.ownedHoldingRibbonCount(this, target)
                >= GameplayConfig.SHADOW_ART_HOLD_RIBBON_COUNT
            || this.openingShadowArtTicks >= OPENING_SHADOW_ART_TIMEOUT_TICKS) {
            this.clearOpeningShadowArtAttack();
        }
    }

    private boolean isOpeningShadowArtAttack(@Nullable LivingEntity target) {
        return target != null
            && this.openingShadowArtTarget != null
            && this.openingShadowArtTarget.equals(target.getUUID());
    }

    private void clearOpeningShadowArtAttack() {
        this.openingShadowArtTarget = null;
        this.openingTeleportAttempted = false;
        this.openingShadowArtTicks = 0;
    }

    private boolean tickMagicOutput(ServerPlayer owner) {
        int chargeTicks = this.getMagicOutputChargeTicks();
        if (chargeTicks <= 0) {
            return false;
        }

        this.getNavigation().stop();
        if (this.magicOutputOrigin == null) {
            this.magicOutputOrigin = this.position();
        }
        this.setPos(this.magicOutputOrigin.x, this.magicOutputOrigin.y, this.magicOutputOrigin.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        this.hurtMarked = true;
        ShadowArtService.maintainBlackShadowHeldTargets(this);
        if (this.level() instanceof ServerLevel level && chargeTicks % 5 == 0) {
            Vec3 center = this.magicOutputCenter();
            float orbRadius = this.magicOutputOrbRadius(0.0F);
            GrailParticleService.send(
                level,
                this,
                chargeTicks % 10 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SQUID_INK,
                center.x,
                center.y,
                center.z,
                28,
                orbRadius * 0.75D,
                orbRadius * 0.75D,
                orbRadius * 0.75D,
                0.015D
            );
        }
        chargeTicks--;
        this.entityData.set(DATA_MAGIC_OUTPUT_CHARGE, chargeTicks);
        if (chargeTicks == 0) {
            this.releaseMagicOutput(owner);
        }
        return true;
    }

    private boolean tryStartMagicOutput(ServerPlayer owner) {
        LivingEntity target = this.getTarget();
        boolean shadowArtExecution = PollutionService.isUnpollutableServantOrCardUser(target)
            && ShadowArtService.isOwnedPinnedTarget(this, target);
        if (!shadowArtExecution) {
            return false;
        }
        this.startMagicOutput(owner);
        return true;
    }

    private void startMagicOutput(ServerPlayer owner) {
        this.disconnectVoidAbsorption(false);
        this.magicOutputOrigin = this.position();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.entityData.set(DATA_MAGIC_OUTPUT_CHARGE, MAGIC_OUTPUT_CHARGE_TICKS);
        this.level().playSound(
            null,
            this.blockPosition(),
            SoundEvents.RESPAWN_ANCHOR_CHARGE,
            SoundSource.HOSTILE,
            2.0F,
            0.35F
        );
    }

    private void releaseMagicOutput(ServerPlayer owner) {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }
        Vec3 center = this.magicOutputBlastCenter();
        MagicOutputService.release(this, owner, center);
        this.magicOutputDismissPosition = this.position();
        this.magicOutputOrigin = null;
        this.magicOutputDismissTicks = MAGIC_OUTPUT_DISMISS_DELAY_TICKS;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setAggressive(false);
        this.setAction(ACTION_IDLE);
    }

    private void tickMagicOutputDismissal(@Nullable LivingEntity owner) {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        if (this.magicOutputDismissPosition == null) {
            this.magicOutputDismissPosition = this.position();
        }
        setPos(
            this.magicOutputDismissPosition.x,
            this.magicOutputDismissPosition.y,
            this.magicOutputDismissPosition.z
        );
        this.setDeltaMovement(Vec3.ZERO);
        this.fallDistance = 0.0F;
        this.hurtMarked = true;
        ShadowArtService.maintainBlackShadowHeldTargets(this);
        if (--this.magicOutputDismissTicks <= 0) {
            this.magicOutputDismissTicks = 0;
            this.dismiss(owner == null ? this : owner);
        }
    }

    private Vec3 magicOutputBlastCenter() {
        Vec3 origin = this.magicOutputOrigin == null ? this.position() : this.magicOutputOrigin;
        return new Vec3(origin.x, origin.y + this.getBbHeight(), origin.z);
    }

    private Vec3 magicOutputCenter() {
        Vec3 origin = this.magicOutputOrigin == null ? this.position() : this.magicOutputOrigin;
        return new Vec3(
            origin.x,
            origin.y + this.getBbHeight() + this.magicOutputOrbRadius(0.0F),
            origin.z
        );
    }

    public float magicOutputOrbRadius(float partialTick) {
        int chargeTicks = this.getMagicOutputChargeTicks();
        float elapsed = MAGIC_OUTPUT_CHARGE_TICKS - chargeTicks + partialTick;
        float progress = Math.clamp(elapsed / MAGIC_OUTPUT_CHARGE_TICKS, 0.0F, 1.0F);
        return Mth.lerp(progress, MAGIC_OUTPUT_ORB_START_RADIUS, MAGIC_OUTPUT_ORB_END_RADIUS);
    }

    private void tickHeroicSpiritDevourer(ServerPlayer owner) {
        LivingEntity target = this.getTarget();
        if (target == null || this.heroicSpiritDevourerCooldown > 0) {
            return;
        }
        int summoned = PollutionService.summonRandomCorruptedServants(owner, this, target);
        this.heroicSpiritDevourerCooldown = summoned > 0
            ? HEROIC_SPIRIT_DEVOURER_COOLDOWN
            : HEROIC_SPIRIT_DEVOURER_RETRY;
    }

    private boolean tickBlackMudCoverage(ServerPlayer owner) {
        if (--this.blackMudCoverageCooldown > 0) {
            return this.blackMudCoverageReady;
        }
        this.blackMudCoverageCooldown = BLACK_MUD_COVERAGE_INTERVAL;
        BlockPos center = this.blockPosition();
        if (this.blackMudExpansionCenter == null
            || this.blackMudExpansionCenter.distSqr(center) > 16.0D) {
            this.blackMudExpansionCenter = center.immutable();
            this.blackMudExpansionRadius = SummonBlackMudService.BLACK_SHADOW_INITIAL_RADIUS;
            this.blackMudCoverageReady = false;
        }
        if (this.blackMudCoverageReady
            && this.blackMudExpansionRadius >= SummonBlackMudService.BLACK_SHADOW_MAX_RADIUS
            && BlackMudService.hasMudCoverage(this)) {
            return true;
        }
        int requestedRadius = this.blackMudCoverageReady
            ? Math.min(
                SummonBlackMudService.BLACK_SHADOW_MAX_RADIUS,
                this.blackMudExpansionRadius + 2
            )
            : this.blackMudExpansionRadius;
        boolean deployed = SummonBlackMudService.deployAround(this, owner, requestedRadius);
        this.blackMudCoverageReady = BlackMudService.hasMudCoverage(this)
            || deployed && BlackMudService.isOnOrInBlackMud(this);
        if (this.blackMudCoverageReady) {
            this.blackMudExpansionRadius = requestedRadius;
        }
        return this.blackMudCoverageReady;
    }

    private void tickShadowBindings(ServerPlayer owner, boolean mudReady) {
        this.cleanupShadowBindings();
        LivingEntity primaryTarget = this.getTarget();
        if (!mudReady
            || primaryTarget == null
            || this.shadowBindingTargets.size() >= SHADOW_BINDING_TARGET_LIMIT
            || --this.shadowBindingScanCooldown > 0) {
            return;
        }
        this.shadowBindingScanCooldown = SHADOW_BINDING_SCAN_INTERVAL;

        boolean expandToNearbyTargets = this.permitsAutomaticTargeting();
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(TARGET_RANGE),
            candidate -> this.isLegalTarget(candidate)
                && this.distanceToSqr(candidate) <= TARGET_RANGE * TARGET_RANGE
                && BlackMudService.isOnOrInBlackMud(candidate)
                && PollutionService.canBecomeFullyCorrupted(candidate)
                && !this.shadowBindingTargets.contains(candidate.getUUID())
                && (candidate == primaryTarget || expandToNearbyTargets)
                && this.getSensing().hasLineOfSight(candidate)
                && ShadowBindingService.canBegin(this, candidate)
        );
        candidates.sort(Comparator.comparingDouble(this::distanceToSqr));
        for (LivingEntity candidate : candidates) {
            if (this.shadowBindingTargets.size() >= SHADOW_BINDING_TARGET_LIMIT) {
                break;
            }
            if (!this.canAffordActionMana(owner, ShadowBindingMagicService.MANA_COST)) {
                break;
            }
            if (ShadowBindingService.begin(this, candidate)
                && this.tryConsumeActionMana(owner, ShadowBindingMagicService.MANA_COST)) {
                this.shadowBindingTargets.add(candidate.getUUID());
            } else {
                ShadowBindingService.release(this, candidate);
            }
        }
    }

    private void cleanupShadowBindings() {
        if (!(this.level() instanceof ServerLevel level) || this.shadowBindingTargets.isEmpty()) {
            return;
        }
        Iterator<UUID> iterator = this.shadowBindingTargets.iterator();
        while (iterator.hasNext()) {
            Entity entity = level.getEntity(iterator.next());
            if (!(entity instanceof LivingEntity target)
                || !this.isLegalTarget(target)
                || this.distanceToSqr(target) > TARGET_RANGE * TARGET_RANGE
                || !ShadowBindingService.isControlling(this, target)) {
                if (entity instanceof LivingEntity target) {
                    ShadowBindingService.release(this, target);
                }
                iterator.remove();
            }
        }
    }

    private void tickVoidAbsorption(ServerPlayer owner, boolean mudReady) {
        LivingEntity target = this.voidAbsorptionTargetEntity();
        if (this.voidAbsorptionTarget != null
            && (target == null
                || !this.isLegalTarget(target)
                || !this.canUseVoidAbsorptionOn(target)
                || this.distanceToSqr(target) > VOID_ABSORPTION_RANGE * VOID_ABSORPTION_RANGE)) {
            this.disconnectVoidAbsorption(true);
            target = null;
        }

        if (this.voidAbsorptionConnected && target != null) {
            if (!ImaginaryShadowService.maintainAuxiliaryBinding(this.getUUID(), target)) {
                this.disconnectVoidAbsorption(true);
                return;
            }
            ImaginaryShadowService.syncVoidAbsorptionLink(this, target);
            if (this.voidAbsorptionDamageTicks > 0) {
                this.voidAbsorptionDamageTicks--;
            } else if (this.pulseVoidAbsorption(owner, target)) {
                ImaginaryShadowService.maintainAuxiliaryBinding(this.getUUID(), target);
                this.voidAbsorptionDamageTicks = VOID_ABSORPTION_DAMAGE_INTERVAL - 1;
            } else {
                this.disconnectVoidAbsorption(true);
            }
            return;
        }

        if (this.voidAbsorptionCastTicks > 0 && target != null) {
            if (!this.getSensing().hasLineOfSight(target)) {
                this.disconnectVoidAbsorption(true);
                return;
            }
            if (this.level() instanceof ServerLevel level && this.voidAbsorptionCastTicks % 5 == 0) {
                Vec3 center = this.getBoundingBox().getCenter();
                GrailParticleService.send(
                    level,
                    this,
                    ParticleTypes.REVERSE_PORTAL,
                    center.x,
                    center.y,
                    center.z,
                    12,
                    0.9D,
                    0.9D,
                    0.9D,
                    0.07D
                );
            }
            if (--this.voidAbsorptionCastTicks == 0) {
                if (ImaginaryShadowService.beginAuxiliaryBinding(this.getUUID(), target)) {
                    this.voidAbsorptionConnected = true;
                    this.voidAbsorptionDamageTicks = 0;
                } else {
                    this.disconnectVoidAbsorption(true);
                }
            }
            return;
        }

        LivingEntity primaryTarget = this.getTarget();
        if ((mudReady || this.isDirectAbsorptionTarget(primaryTarget))
            && this.voidAbsorptionCooldown == 0
            && this.isLegalTarget(primaryTarget)
            && this.canUseVoidAbsorptionOn(primaryTarget)
            && this.distanceToSqr(primaryTarget) <= VOID_ABSORPTION_RANGE * VOID_ABSORPTION_RANGE
            && this.getSensing().hasLineOfSight(primaryTarget)) {
            this.voidAbsorptionTarget = primaryTarget.getUUID();
            this.voidAbsorptionCastTicks = VOID_ABSORPTION_CAST_TICKS;
            this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.HOSTILE,
                1.0F,
                0.5F
            );
        }
    }

    private boolean canUseVoidAbsorptionOn(@Nullable LivingEntity target) {
        return this.isDirectAbsorptionTarget(target)
            || PollutionService.isBlackShadowAbsorptionTarget(target);
    }

    private boolean isDirectAbsorptionTarget(@Nullable LivingEntity target) {
        return target != null && !TypeMoonServantBridge.isServantOrCardUser(target);
    }

    private boolean pulseVoidAbsorption(ServerPlayer owner, LivingEntity target) {
        if (PollutionService.shouldPrioritizeCorruption(target)) {
            return false;
        }
        float damage = PollutionService.limitDamageForCorruptionPriority(target, VOID_ABSORPTION_DAMAGE);
        if (damage <= 0.0F) {
            return false;
        }
        double maximumManaDrain = Math.min(
            GameplayConfig.ABSORPTION_MANA_DRAIN_PER_SECOND,
            damage
        );
        float healthBefore = target.getHealth();
        boolean hit = ShadowBindingService.absorbDirectDamage(target, owner, damage)
            || target.hurt(this.damageSources().indirectMagic(this, owner), damage);
        if (!hit) {
            return false;
        }
        if (!ShadowBindingService.hasBlackShadowSource(target)) {
            this.rewardOwnerManaFromDamage(healthBefore - target.getHealth());
        }
        TypeMoonIntegration.drainMana(target, maximumManaDrain);
        PollutionService.expose(target, this, true);
        if (this.level() instanceof ServerLevel level) {
            GrailParticleService.send(
                level,
                this,
                ParticleTypes.SQUID_INK,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(),
                8,
                target.getBbWidth() * 0.4D,
                target.getBbHeight() * 0.3D,
                target.getBbWidth() * 0.4D,
                0.015D
            );
            level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.HOSTILE,
                0.75F,
                0.65F
            );
        }
        return true;
    }

    private boolean hasSuccessfulRangedSkill(LivingEntity target) {
        return ShadowArtService.isOwnedHoldingTarget(this, target)
            || this.shadowBindingTargets.contains(target.getUUID())
                && ShadowBindingService.hasBinding(this, target)
            || this.voidAbsorptionConnected
                && this.voidAbsorptionTarget != null
                && this.voidAbsorptionTarget.equals(target.getUUID());
    }

    private double combatDistanceToSqr(LivingEntity target) {
        return this.getBoundingBox().getCenter().distanceToSqr(target.getBoundingBox().getCenter());
    }

    @Nullable
    private LivingEntity voidAbsorptionTargetEntity() {
        if (this.voidAbsorptionTarget == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(this.voidAbsorptionTarget);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private void disconnectVoidAbsorption(boolean applyCooldown) {
        LivingEntity target = this.voidAbsorptionTargetEntity();
        if (this.voidAbsorptionConnected && target != null) {
            ImaginaryShadowService.endAuxiliaryBinding(this.getUUID(), target);
        }
        this.voidAbsorptionTarget = null;
        this.voidAbsorptionCastTicks = 0;
        this.voidAbsorptionDamageTicks = 0;
        this.voidAbsorptionConnected = false;
        if (applyCooldown) {
            this.voidAbsorptionCooldown = VOID_ABSORPTION_COOLDOWN;
        }
    }

    public void rewardOwnerManaFromDamage(double actualDamage) {
        if (!Double.isFinite(actualDamage) || actualDamage <= 0.0D) {
            return;
        }
        LivingEntity owner = this.getActiveOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        double restored = TypeMoonIntegration.restoreMana(player, actualDamage);
        if (!player.getData(ModAttachments.IMAGINARY_SPACE.get()).grailErosionFull()) {
            this.addStoredMana(actualDamage - restored);
        }
    }

    public boolean tryConsumeActionMana(double amount) {
        LivingEntity owner = this.getActiveOwner();
        return owner instanceof ServerPlayer player && this.tryConsumeActionMana(player, amount);
    }

    private boolean canAffordActionMana(ServerPlayer owner, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return true;
        }
        double remaining = Math.max(0.0D, amount - this.storedMana);
        double ownerMana = TypeMoonIntegration.currentMana(owner);
        return Double.isFinite(ownerMana) && ownerMana + 1.0E-6D >= remaining;
    }

    private boolean tryConsumeActionMana(ServerPlayer owner, double amount) {
        if (!this.canAffordActionMana(owner, amount)) {
            return false;
        }
        double fromStored = Math.min(this.storedMana, amount);
        double fromOwner = amount - fromStored;
        if (fromOwner > 1.0E-6D && !TypeMoonIntegration.tryConsumeMana(owner, fromOwner)) {
            return false;
        }
        this.storedMana = Math.max(0.0D, this.storedMana - fromStored);
        return true;
    }

    private void addStoredMana(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        this.storedMana = this.storedMana > Double.MAX_VALUE - amount
            ? Double.MAX_VALUE
            : this.storedMana + amount;
    }

    @Override
    public void die(DamageSource source) {
        this.stopCombatAndMovement();
        super.die(source);
        if (!this.level().isClientSide && this.isDeadOrDying()) {
            BlackShadowNightService.shadowKilled(this);
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            SummonBlackMudService.blackShadowRemoved(this);
        }
        super.remove(reason);
    }

    private void stopCombatAndMovement() {
        ShadowArtService.blackShadowUnavailable(this);
        ShadowBindingService.releaseBySource(this);
        this.shadowBindingTargets.clear();
        this.disconnectVoidAbsorption(false);
        this.setTarget(null);
        this.automaticTarget = null;
        this.setAggressive(false);
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        Vec3 movement = this.getDeltaMovement();
        this.setDeltaMovement(0.0D, movement.y, 0.0D);
        this.setAction(ACTION_IDLE);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(source);
    }

    public void onCommandModeApplied() {
        if (!this.level().isClientSide) {
            this.lastObservedCommandMode = this.getShadowCommandMode();
            this.stopCombatAndMovement();
        }
    }

    public void dismiss(Entity source) {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) {
            return;
        }
        this.stopCombatAndMovement();
        ImaginaryShadowService.playDissolutionVisual(level, this, source);
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        tag.putBoolean("GrailOwned", grailOwned);
        tag.putInt("OwnerDismissalGeneration", this.ownerDismissalGeneration);
        tag.putIntArray("ShadowArtRecovery", this.shadowArtRecoveryTicks);
        tag.putDouble("StoredMana", this.storedMana);
        tag.putInt("MagicOutputDismissTicks", this.magicOutputDismissTicks);
        if (this.magicOutputDismissPosition != null) {
            tag.putDouble("MagicOutputDismissX", this.magicOutputDismissPosition.x);
            tag.putDouble("MagicOutputDismissY", this.magicOutputDismissPosition.y);
            tag.putDouble("MagicOutputDismissZ", this.magicOutputDismissPosition.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        grailOwned = tag.getBoolean("GrailOwned");
        this.ownerDismissalGeneration = tag.getInt("OwnerDismissalGeneration");
        double savedStoredMana = tag.getDouble("StoredMana");
        this.storedMana = Double.isFinite(savedStoredMana) ? Math.max(0.0D, savedStoredMana) : 0.0D;
        this.magicOutputDismissTicks = Math.clamp(
            tag.getInt("MagicOutputDismissTicks"),
            0,
            MAGIC_OUTPUT_DISMISS_DELAY_TICKS
        );
        this.magicOutputDismissPosition = tag.contains("MagicOutputDismissX")
            && tag.contains("MagicOutputDismissY")
            && tag.contains("MagicOutputDismissZ")
                ? new Vec3(
                    tag.getDouble("MagicOutputDismissX"),
                    tag.getDouble("MagicOutputDismissY"),
                    tag.getDouble("MagicOutputDismissZ")
                )
                : null;
        int[] savedRecovery = tag.getIntArray("ShadowArtRecovery");
        for (int index = 0; index < shadowArtRecoveryTicks.length && index < savedRecovery.length; index++) {
            shadowArtRecoveryTicks[index] = Math.clamp(savedRecovery[index], 0, GameplayConfig.SHADOW_ART_RIBBON_RESPAWN_TICKS);
        }
        this.setAction(ACTION_IDLE);
    }

    private boolean isWithinAttackReach(LivingEntity target) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        double reach = ATTACK_REACH_PADDING + (this.getBbWidth() + target.getBbWidth()) * 0.5D;
        boolean verticalOverlap = target.getBoundingBox().maxY >= this.getBoundingBox().minY - 0.35D
            && target.getBoundingBox().minY <= this.getBoundingBox().maxY + 0.35D;
        return verticalOverlap && deltaX * deltaX + deltaZ * deltaZ <= reach * reach;
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
            if (offset <= GROUND_SEARCH_UP) {
                Vec3 candidate = this.groundSurfaceAt(x, z, referenceFloorY + offset);
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
        if (!this.level().noCollision(this, movedBounds) || this.level().containsAnyLiquid(movedBounds)) {
            return false;
        }
        BlockPos support = BlockPos.containing(position.x, position.y - 0.01D, position.z);
        var state = this.level().getBlockState(support);
        return !state.is(Blocks.LAVA)
            && !state.is(Blocks.FIRE)
            && !state.is(Blocks.SOUL_FIRE)
            && !state.is(Blocks.MAGMA_BLOCK)
            && !state.is(Blocks.CACTUS)
            && !state.is(Blocks.SWEET_BERRY_BUSH);
    }

    public boolean teleportAround(
        LivingEntity center,
        double minimumRadius,
        double maximumRadius,
        int attempts
    ) {
        if (!(this.level() instanceof ServerLevel)
            || minimumRadius < 0.0D
            || maximumRadius < minimumRadius) {
            return false;
        }
        double minimumDistanceSqr = minimumRadius * minimumRadius;
        double maximumDistanceSqr = maximumRadius * maximumRadius;
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            double radius = minimumRadius + this.random.nextDouble() * (maximumRadius - minimumRadius);
            Vec3 candidate = this.findGroundPosition(
                center.getX() + Math.cos(angle) * radius,
                center.getZ() + Math.sin(angle) * radius,
                center.getY()
            );
            if (candidate == null) {
                continue;
            }
            double distanceSqr = candidate.distanceToSqr(center.position());
            if (distanceSqr < minimumDistanceSqr || distanceSqr > maximumDistanceSqr) {
                continue;
            }
            this.teleportTo(candidate.x, candidate.y, candidate.z);
            this.getNavigation().stop();
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        return false;
    }

    private final class BlackShadowTargetGoal extends Goal {
        private int scanCooldown;
        private int hurtTimestamp;

        private BlackShadowTargetGoal() {
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return BlackShadowEntity.this.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return BlackShadowEntity.this.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!BlackShadowEntity.this.canAct()) {
                BlackShadowEntity.this.setTarget(null);
                return;
            }

            if (BlackShadowEntity.this.hasNightMission()) {
                LivingEntity missionTarget = BlackShadowEntity.this.getNightMissionTarget();
                BlackShadowEntity.this.setTarget(
                    BlackShadowEntity.this.isSelectableTarget(missionTarget) ? missionTarget : null
                );
                return;
            }

            if (BlackShadowEntity.this.getShadowCommandMode() == ShadowCommandMode.HUNT) {
                LivingEntity huntTarget = BlackMudHuntService.targetFor(BlackShadowEntity.this);
                BlackShadowEntity.this.automaticTarget = null;
                BlackShadowEntity.this.setTarget(
                    BlackShadowEntity.this.isSelectableTarget(huntTarget) ? huntTarget : null
                );
                return;
            }

            int currentHurtTimestamp = BlackShadowEntity.this.getLastHurtByMobTimestamp();
            if (currentHurtTimestamp != this.hurtTimestamp) {
                this.hurtTimestamp = currentHurtTimestamp;
                LivingEntity attacker = BlackShadowEntity.this.getLastHurtByMob();
                if (BlackShadowEntity.this.isSelectableTarget(attacker)) {
                    BlackShadowEntity.this.automaticTarget = null;
                    BlackShadowEntity.this.setTarget(attacker);
                }
            }

            LivingEntity current = BlackShadowEntity.this.getTarget();
            if (current != null
                && current.getUUID().equals(BlackShadowEntity.this.automaticTarget)
                && !BlackShadowEntity.this.permitsAutomaticTargeting()) {
                BlackShadowEntity.this.automaticTarget = null;
                BlackShadowEntity.this.setTarget(null);
                current = null;
            }
            if (!BlackShadowEntity.this.isSelectableTarget(current)) {
                BlackShadowEntity.this.automaticTarget = null;
                BlackShadowEntity.this.setTarget(null);
            }
            if (BlackShadowEntity.this.getTarget() != null
                || !BlackShadowEntity.this.permitsAutomaticTargeting()
                || --this.scanCooldown > 0) {
                return;
            }

            this.scanCooldown = TARGET_SCAN_INTERVAL;
            AABB searchArea = BlackShadowEntity.this.getBoundingBox().inflate(TARGET_RANGE);
            LivingEntity nearestCorruptible = null;
            LivingEntity nearestFallback = null;
            LivingEntity nearestAbsorption = null;
            double nearestCorruptibleDistance = Double.MAX_VALUE;
            double nearestFallbackDistance = Double.MAX_VALUE;
            double nearestAbsorptionDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : BlackShadowEntity.this.level().getEntitiesOfClass(
                LivingEntity.class,
                searchArea,
                BlackShadowEntity.this::isSelectableTarget
            )) {
                double distance = BlackShadowEntity.this.distanceToSqr(candidate);
                if (!BlackShadowEntity.this.isServantTarget(candidate)) {
                    if (distance < nearestAbsorptionDistance) {
                        nearestAbsorption = candidate;
                        nearestAbsorptionDistance = distance;
                    }
                } else if (PollutionService.canBecomeFullyCorrupted(candidate)) {
                    if (distance < nearestCorruptibleDistance) {
                        nearestCorruptible = candidate;
                        nearestCorruptibleDistance = distance;
                    }
                } else if (PollutionService.isUnpollutableServantOrCardUser(candidate)
                    && distance < nearestFallbackDistance) {
                    nearestFallback = candidate;
                    nearestFallbackDistance = distance;
                }
            }
            LivingEntity nearest = nearestCorruptible != null
                ? nearestCorruptible
                : nearestFallback != null ? nearestFallback : nearestAbsorption;
            BlackShadowEntity.this.automaticTarget = nearest == null ? null : nearest.getUUID();
            BlackShadowEntity.this.setTarget(nearest);
        }
    }

    private final class BlackShadowMeleeGoal extends Goal {
        @Nullable
        private LivingEntity combatTarget;
        private int repathTicks;
        private int failedPaths;
        private int attackTicks;
        private int turnTicks;
        private int blockedTicks;
        private boolean hitResolved;

        private BlackShadowMeleeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = BlackShadowEntity.this.getTarget();
            if (!BlackShadowEntity.this.mudPreparedForCombat()
                || !BlackShadowEntity.this.canPursueTarget(target)) {
                return false;
            }
            this.combatTarget = target;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return BlackShadowEntity.this.mudPreparedForCombat()
                && BlackShadowEntity.this.canPursueTarget(this.combatTarget)
                && BlackShadowEntity.this.getTarget() == this.combatTarget;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.failedPaths = 0;
            this.attackTicks = 0;
            this.turnTicks = TARGET_TURN_TICKS;
            this.blockedTicks = 0;
            this.hitResolved = false;
            BlackShadowEntity.this.setAggressive(true);
        }

        @Override
        public void stop() {
            this.combatTarget = null;
            this.attackTicks = 0;
            this.hitResolved = false;
            BlackShadowEntity.this.setAggressive(false);
            BlackShadowEntity.this.getNavigation().stop();
            BlackShadowEntity.this.setAction(ACTION_IDLE);
        }

        @Override
        public void tick() {
            LivingEntity target = this.combatTarget;
            if (target == null) {
                return;
            }
            BlackShadowEntity.this.getLookControl().setLookAt(target, 35.0F, 30.0F);

            if (BlackShadowEntity.this.isOpeningShadowArtAttack(target)) {
                BlackShadowEntity.this.getNavigation().stop();
                return;
            }

            if (this.attackTicks > 0) {
                BlackShadowEntity.this.getNavigation().stop();
                this.attackTicks++;
                if (!this.hitResolved && this.attackTicks > ATTACK_WINDUP_TICKS) {
                    this.hitResolved = true;
                    if (BlackShadowEntity.this.isWithinAttackReach(target)
                        && BlackShadowEntity.this.getSensing().hasLineOfSight(target)
                        && BlackShadowEntity.this.isLegalTarget(target)
                        && !BlackShadowEntity.this.isDirectAbsorptionTarget(target)
                        && !PollutionService.shouldPrioritizeCorruption(target)) {
                        BlackShadowEntity.this.doHurtTarget(target);
                    }
                }
                if (this.attackTicks > ATTACK_TOTAL_TICKS) {
                    this.attackTicks = 0;
                    this.hitResolved = false;
                    BlackShadowEntity.this.attackCooldown = ATTACK_COOLDOWN_TICKS;
                    BlackShadowEntity.this.setAction(ACTION_IDLE);
                }
                return;
            }

            if (this.turnTicks > 0) {
                this.turnTicks--;
                BlackShadowEntity.this.getNavigation().stop();
                return;
            }

            double combatDistanceSqr = BlackShadowEntity.this.combatDistanceToSqr(target);
            if (BlackShadowEntity.this.hasSuccessfulRangedSkill(target)) {
                double minimumSqr = RANGED_STANDOFF_MIN_RANGE * RANGED_STANDOFF_MIN_RANGE;
                double maximumSqr = RANGED_STANDOFF_MAX_RANGE * RANGED_STANDOFF_MAX_RANGE;
                if (combatDistanceSqr >= minimumSqr && combatDistanceSqr <= maximumSqr) {
                    BlackShadowEntity.this.getNavigation().stop();
                    this.failedPaths = 0;
                    this.blockedTicks = 0;
                    return;
                }
                if (combatDistanceSqr < minimumSqr) {
                    if (--this.repathTicks <= 0) {
                        this.repathTicks = REPATH_INTERVAL;
                        Vec3 targetCenter = target.getBoundingBox().getCenter();
                        Vec3 away = BlackShadowEntity.this.getBoundingBox().getCenter().subtract(targetCenter);
                        away = new Vec3(away.x, 0.0D, away.z);
                        if (away.lengthSqr() < 1.0E-4D) {
                            double angle = BlackShadowEntity.this.random.nextDouble() * Mth.TWO_PI;
                            away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                        }
                        Vec3 desired = targetCenter.add(away.normalize().scale(25.0D));
                        Vec3 ground = BlackShadowEntity.this.findGroundPosition(
                            desired.x,
                            desired.z,
                            target.getY()
                        );
                        boolean movingAway = ground != null && BlackShadowEntity.this.getNavigation().moveTo(
                            ground.x,
                            ground.y,
                            ground.z,
                            CHASE_SPEED
                        );
                        if (!movingAway) {
                            BlackShadowEntity.this.teleportAround(
                                target,
                                RANGED_STANDOFF_MIN_RANGE,
                                RANGED_STANDOFF_MAX_RANGE,
                                32
                            );
                        }
                    }
                    return;
                }
            }

            if (BlackShadowEntity.this.attackCooldown <= 0
                && !BlackShadowEntity.this.isDirectAbsorptionTarget(target)
                && !PollutionService.shouldPrioritizeCorruption(target)
                && BlackShadowEntity.this.isWithinAttackReach(target)
                && BlackShadowEntity.this.getSensing().hasLineOfSight(target)) {
                this.attackTicks = 1;
                this.hitResolved = false;
                BlackShadowEntity.this.getNavigation().stop();
                BlackShadowEntity.this.setAction(ACTION_ATTACK);
                return;
            }

            boolean movementBlocked = BlackShadowEntity.this.horizontalCollision
                || BlackShadowEntity.this.getNavigation().isDone()
                    && !BlackShadowEntity.this.isWithinAttackReach(target);
            this.blockedTicks = movementBlocked ? this.blockedTicks + 1 : 0;
            if (this.blockedTicks >= BLOCKED_TELEPORT_TICKS) {
                this.blockedTicks = 0;
                this.failedPaths = 0;
                if (BlackShadowEntity.this.teleportAround(
                    target,
                    RANGED_STANDOFF_MIN_RANGE,
                    RANGED_STANDOFF_MAX_RANGE,
                    32
                )) {
                    this.repathTicks = REPATH_INTERVAL;
                    return;
                }
            }

            if (--this.repathTicks <= 0) {
                this.repathTicks = REPATH_INTERVAL;
                boolean pathStarted = BlackShadowEntity.this.getNavigation().moveTo(target, CHASE_SPEED);
                if (pathStarted) {
                    this.failedPaths = 0;
                } else if (++this.failedPaths >= MAX_PATH_FAILURES) {
                    this.failedPaths = 0;
                    if (!BlackShadowEntity.this.teleportAround(
                        target,
                        RANGED_STANDOFF_MIN_RANGE,
                        RANGED_STANDOFF_MAX_RANGE,
                        32
                    )) {
                        BlackShadowEntity.this.rejectTarget(target);
                    }
                }
            }
        }
    }

    private final class GatherOwnerGoal extends Goal {
        @Nullable
        private LivingEntity owner;
        private int recalculateTicks;
        private int failures;
        private long nextRetryTick;

        private GatherOwnerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidate = BlackShadowEntity.this.getActiveOwner();
            if (candidate == null
                || BlackShadowEntity.this.getShadowCommandMode() != ShadowCommandMode.GATHER
                || BlackShadowEntity.this.getTarget() != null
                || BlackShadowEntity.this.level().getGameTime() < this.nextRetryTick
                || BlackShadowEntity.this.distanceToSqr(candidate) <= 9.0D) {
                return false;
            }
            this.owner = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                && this.owner == BlackShadowEntity.this.getActiveOwner()
                && BlackShadowEntity.this.getShadowCommandMode() == ShadowCommandMode.GATHER
                && BlackShadowEntity.this.getTarget() == null
                && this.failures < MAX_PATH_FAILURES
                && BlackShadowEntity.this.distanceToSqr(this.owner) > 4.0D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.recalculateTicks = 0;
            this.failures = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            BlackShadowEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            BlackShadowEntity.this.getLookControl().setLookAt(this.owner, 20.0F, 20.0F);
            if (--this.recalculateTicks <= 0) {
                this.recalculateTicks = 10;
                if (BlackShadowEntity.this.getNavigation().moveTo(this.owner, COMMAND_SPEED)) {
                    this.failures = 0;
                } else if (++this.failures >= MAX_PATH_FAILURES) {
                    this.nextRetryTick = BlackShadowEntity.this.level().getGameTime() + 40L;
                }
            }
        }
    }

    private final class SpreadOwnerGoal extends Goal {
        @Nullable
        private LivingEntity owner;
        @Nullable
        private Vec3 destination;
        private long nextSearchTick;
        private int travelTicks;
        private int repathTicks;
        private int failures;

        private SpreadOwnerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity candidateOwner = BlackShadowEntity.this.getActiveOwner();
            if (candidateOwner == null
                || BlackShadowEntity.this.getShadowCommandMode() != ShadowCommandMode.SPREAD
                || BlackShadowEntity.this.getTarget() != null
                || BlackShadowEntity.this.level().getGameTime() < this.nextSearchTick) {
                return false;
            }
            Vec3 candidate = this.findDestination(candidateOwner);
            if (candidate == null) {
                this.nextSearchTick = BlackShadowEntity.this.level().getGameTime() + 30L;
                return false;
            }
            this.owner = candidateOwner;
            this.destination = candidate;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null
                && this.owner == BlackShadowEntity.this.getActiveOwner()
                && this.destination != null
                && this.travelTicks > 0
                && this.failures < MAX_PATH_FAILURES
                && BlackShadowEntity.this.getShadowCommandMode() == ShadowCommandMode.SPREAD
                && BlackShadowEntity.this.getTarget() == null
                && BlackShadowEntity.this.position().distanceToSqr(this.destination) > 2.25D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.travelTicks = 180;
            this.repathTicks = 0;
            this.failures = 0;
            this.nextSearchTick = BlackShadowEntity.this.level().getGameTime() + 100L
                + BlackShadowEntity.this.random.nextInt(81);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.destination = null;
            BlackShadowEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.travelTicks--;
            if (this.destination == null || --this.repathTicks > 0) {
                return;
            }
            this.repathTicks = 12;
            if (BlackShadowEntity.this.getNavigation().moveTo(
                this.destination.x,
                this.destination.y,
                this.destination.z,
                COMMAND_SPEED
            )) {
                this.failures = 0;
            } else {
                this.failures++;
            }
        }

        @Nullable
        private Vec3 findDestination(LivingEntity owner) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = BlackShadowEntity.this.random.nextDouble() * Mth.TWO_PI;
                double radius = SPREAD_MIN_RADIUS + BlackShadowEntity.this.random.nextDouble()
                    * (SPREAD_MAX_RADIUS - SPREAD_MIN_RADIUS);
                Vec3 candidate = BlackShadowEntity.this.findGroundPosition(
                    owner.getX() + Mth.cos((float)angle) * radius,
                    owner.getZ() + Mth.sin((float)angle) * radius,
                    owner.getY()
                );
                if (candidate == null || this.hasNearbyOwnedShadow(candidate)) {
                    continue;
                }
                return candidate;
            }
            return null;
        }

        private boolean hasNearbyOwnedShadow(Vec3 candidate) {
            AABB area = new AABB(candidate, candidate).inflate(3.0D);
            return !BlackShadowEntity.this.level().getEntitiesOfClass(
                BlackShadowEntity.class,
                area,
                shadow -> shadow != BlackShadowEntity.this
                    && BlackShadowEntity.this.ownerId != null
                    && BlackShadowEntity.this.ownerId.equals(shadow.ownerId)
            ).isEmpty();
        }
    }

    private final class SmoothWanderGoal extends Goal {
        @Nullable
        private Vec3 destination;
        private int pauseTicks;
        private int travelTicks;
        private int failures;

        private SmoothWanderGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!BlackShadowEntity.this.canAct()
                || BlackShadowEntity.this.getTarget() != null
                || BlackShadowEntity.this.getShadowCommandMode() != ShadowCommandMode.FREE
                || BlackShadowEntity.this.isInWaterOrBubble()
                || BlackShadowEntity.this.random.nextInt(80) != 0) {
                return false;
            }
            if (BlackShadowEntity.this.random.nextInt(4) == 0) {
                this.pauseTicks = 25 + BlackShadowEntity.this.random.nextInt(36);
                this.destination = null;
                return true;
            }
            Vec3 randomPosition = DefaultRandomPos.getPos(BlackShadowEntity.this, 12, 5);
            if (randomPosition == null) {
                return false;
            }
            this.destination = BlackShadowEntity.this.findGroundPosition(
                randomPosition.x,
                randomPosition.z,
                randomPosition.y
            );
            return this.destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!BlackShadowEntity.this.canAct()
                || BlackShadowEntity.this.getTarget() != null
                || BlackShadowEntity.this.getShadowCommandMode() != ShadowCommandMode.FREE) {
                return false;
            }
            return this.pauseTicks > 0
                || this.destination != null
                    && this.travelTicks > 0
                    && this.failures < MAX_PATH_FAILURES
                    && BlackShadowEntity.this.position().distanceToSqr(this.destination) > 1.5D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.travelTicks = 140;
            this.failures = 0;
            if (this.destination != null && !BlackShadowEntity.this.getNavigation().moveTo(
                this.destination.x,
                this.destination.y,
                this.destination.z,
                WANDER_SPEED
            )) {
                this.failures++;
            }
        }

        @Override
        public void tick() {
            if (this.pauseTicks > 0) {
                this.pauseTicks--;
                BlackShadowEntity.this.getNavigation().stop();
                return;
            }
            this.travelTicks--;
            if (this.destination != null
                && BlackShadowEntity.this.getNavigation().isDone()
                && this.travelTicks % REPATH_INTERVAL == 0
                && BlackShadowEntity.this.position().distanceToSqr(this.destination) > 1.5D) {
                if (!BlackShadowEntity.this.getNavigation().moveTo(
                    this.destination.x,
                    this.destination.y,
                    this.destination.z,
                    WANDER_SPEED
                )) {
                    this.failures++;
                }
            }
        }

        @Override
        public void stop() {
            this.destination = null;
            this.pauseTicks = 0;
            BlackShadowEntity.this.getNavigation().stop();
        }
    }

    private final class EscapeWaterGoal extends Goal {
        @Nullable
        private Vec3 destination;
        private int repathTicks;
        private int failures;

        private EscapeWaterGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!BlackShadowEntity.this.canAct() || !BlackShadowEntity.this.isInWaterOrBubble()) {
                return false;
            }
            this.destination = this.findNearestLand();
            return this.destination != null;
        }

        @Override
        public boolean canContinueToUse() {
            return BlackShadowEntity.this.isInWaterOrBubble()
                && this.destination != null
                && this.failures < MAX_PATH_FAILURES;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.repathTicks = 0;
            this.failures = 0;
        }

        @Override
        public void tick() {
            if (this.destination == null || --this.repathTicks > 0) {
                return;
            }
            this.repathTicks = 10;
            if (BlackShadowEntity.this.getNavigation().moveTo(
                this.destination.x,
                this.destination.y,
                this.destination.z,
                1.15D
            )) {
                this.failures = 0;
            } else {
                this.failures++;
                this.destination = this.findNearestLand();
            }
        }

        @Override
        public void stop() {
            this.destination = null;
            BlackShadowEntity.this.getNavigation().stop();
        }

        @Nullable
        private Vec3 findNearestLand() {
            Vec3 nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            int angleOffset = BlackShadowEntity.this.random.nextInt(16);
            for (int radius = 3; radius <= 15; radius += 3) {
                for (int index = 0; index < 16; index++) {
                    double angle = (angleOffset + index) * Mth.TWO_PI / 16.0D;
                    Vec3 candidate = BlackShadowEntity.this.findGroundPosition(
                        BlackShadowEntity.this.getX() + Math.cos(angle) * radius,
                        BlackShadowEntity.this.getZ() + Math.sin(angle) * radius,
                        BlackShadowEntity.this.getY()
                    );
                    if (candidate == null) {
                        continue;
                    }
                    double distance = BlackShadowEntity.this.position().distanceToSqr(candidate);
                    if (distance < nearestDistance) {
                        nearest = candidate;
                        nearestDistance = distance;
                    }
                }
                if (nearest != null) {
                    return nearest;
                }
            }
            return null;
        }
    }
}
