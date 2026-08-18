package com.example.typemoonaddon.entity;

import java.util.UUID;
import com.example.typemoonaddon.servant.GillesDeRaisCombatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HugeSeaMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final DustParticleOptions PURPLE_FOG =
            new DustParticleOptions(new Vector3f(0.34F, 0.04F, 0.58F), 4.0F);
    private static final DustParticleOptions DEEP_PURPLE_FOG =
            new DustParticleOptions(new Vector3f(0.08F, 0.01F, 0.14F), 3.2F);
    private static final EntityDataAccessor<Boolean> AWAKENED =
            SynchedEntityData.defineId(HugeSeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DISSOLVING =
            SynchedEntityData.defineId(HugeSeaMonsterEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int DISSOLVE_DURATION = 72;
    private static final int WALK_TERRAIN_BREAK_INTERVAL = 12;
    private static final int WALK_TERRAIN_BREAK_LIMIT = 96;
    private static final int ATTACK_TERRAIN_BREAK_LIMIT = 224;
    private static final int TARGET_REFRESH_INTERVAL = 30;
    private static final int BROOD_SUMMON_INTERVAL = 140;
    private static final int AURA_INTERVAL = 20;
    private static final int FOG_INTERVAL = 5;
    private static final double HUGE_MOVEMENT_SPEED = 0.11;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID sourceUuid;
    @Nullable
    private UUID masterUuid;
    @Nullable
    private LivingEntity cachedSourceEntity;
    private long cachedSourceEntityGameTime = Long.MIN_VALUE;
    private int dissolveTicks;
    private boolean deathEffectsApplied;

    public HugeSeaMonsterEntity(EntityType<HugeSeaMonsterEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0)
                .add(Attributes.ATTACK_DAMAGE, 60.0)
                .add(Attributes.MOVEMENT_SPEED, HUGE_MOVEMENT_SPEED)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKENED, true);
        builder.define(DISSOLVING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.85, true));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.55));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        if (this.isDissolving()) {
            this.tickDissolve();
            return;
        }
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.isStaggeredTick(WALK_TERRAIN_BREAK_INTERVAL, 0) && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
            this.breakTerrainAhead(level, 18.0, 13.0F, 16, WALK_TERRAIN_BREAK_LIMIT);
        }
        if (this.isStaggeredTick(AURA_INTERVAL, 0)) {
            this.pollutionAura(level);
        }
        if (this.isStaggeredTick(FOG_INTERVAL, 0)) {
            this.spawnUnknowableFog(level);
        }
        if (this.tickCount % 100 == 0) {
            this.heal(50.0F);
        }
        if (this.isStaggeredTick(TARGET_REFRESH_INTERVAL, 7) && !isValidTarget(this.getTarget())) {
            LivingEntity revenge = this.getLastHurtByMob();
            this.setTarget(isValidTarget(revenge) ? revenge : this.findNearestTarget(48.0));
        }
        if (this.isStaggeredTick(60, 0)) {
            this.areaSweep(level);
        }
        if (this.isStaggeredTick(80, 20)) {
            this.tentacleGrab(level);
        }
        if (this.isStaggeredTick(BROOD_SUMMON_INTERVAL, 40)) {
            this.trySummonBrood(level);
        }
    }

    public void setSource(@Nullable LivingEntity source) {
        this.sourceUuid = source == null ? null : source.getUUID();
        this.masterUuid = source instanceof GillesDeRaisEntity gilles && gilles.getEntityMaster() != null
                ? gilles.getEntityMaster().getUUID()
                : null;
        this.cachedSourceEntity = source;
        this.cachedSourceEntityGameTime = this.level().getGameTime();
    }

    public boolean isDissolving() {
        return this.entityData.get(DISSOLVING);
    }

    public float getDissolveProgress(float partialTick) {
        if (!this.isDissolving()) {
            return 0.0F;
        }
        return Math.min(1.0F, (this.dissolveTicks + partialTick) / (float) DISSOLVE_DURATION);
    }

    @Nullable
    public UUID getSourceUuid() {
        return this.sourceUuid;
    }

    @Nullable
    public UUID getMasterUuid() {
        return this.masterUuid;
    }

    public boolean isFriendlyTo(@Nullable Entity entity) {
        return this.isFriendly(entity);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty()
                && passenger instanceof GillesDeRaisEntity gilles
                && this.sourceUuid != null
                && this.sourceUuid.equals(gilles.getUUID());
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, this.getX(), this.getY() + this.getBbHeight() * 0.42, this.getZ());
        passenger.setYRot(this.getYRot());
        if (passenger instanceof LivingEntity living) {
            living.setYBodyRot(this.getYRot());
            living.setYHeadRot(this.getYRot());
            living.setXRot(0.0F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.isFriendly(target)) {
            return false;
        }
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.swing(InteractionHand.MAIN_HAND);
            this.triggerAnim("action_controller", "slam");
            if (this.level() instanceof ServerLevel level) {
                this.breakTerrainAhead(level, 22.0, 15.0F, 22, ATTACK_TERRAIN_BREAK_LIMIT);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isDissolving() || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return false;
        }
        if (this.isFriendly(source.getEntity()) || this.isFriendly(source.getDirectEntity())) {
            return false;
        }
        LivingEntity attacker = this.livingAttacker(source);
        if (!this.level().isClientSide() && isValidTarget(attacker)) {
            this.setTarget(attacker);
            GillesDeRaisCombatHelper.shareSeaMonsterRetaliation(this, attacker);
        }
        if (!this.level().isClientSide() && amount >= this.getHealth()) {
            this.beginDissolve(source);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAttackable() {
        return !this.isDissolving() && super.isAttackable();
    }

    @Override
    public boolean isPickable() {
        return !this.isDissolving() && super.isPickable();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || this.isFriendly(other);
    }

    @Override
    public void die(DamageSource cause) {
        this.beginDissolve(cause);
    }

    @Override
    protected void tickDeath() {
        if (this.isDissolving()) {
            this.tickDissolve();
            return;
        }
        super.tickDeath();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.sourceUuid != null) {
            tag.putUUID("GillesHugeSeaMonsterSource", this.sourceUuid);
        }
        if (this.masterUuid != null) {
            tag.putUUID("GillesHugeSeaMonsterMaster", this.masterUuid);
        }
        tag.putBoolean("GillesHugeSeaMonsterDissolving", this.isDissolving());
        tag.putInt("GillesHugeSeaMonsterDissolveTicks", this.dissolveTicks);
        tag.putBoolean("GillesHugeSeaMonsterDeathEffectsApplied", this.deathEffectsApplied);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("GillesHugeSeaMonsterSource")) {
            this.sourceUuid = tag.getUUID("GillesHugeSeaMonsterSource");
        }
        if (tag.hasUUID("GillesHugeSeaMonsterMaster")) {
            this.masterUuid = tag.getUUID("GillesHugeSeaMonsterMaster");
        }
        this.entityData.set(DISSOLVING, tag.getBoolean("GillesHugeSeaMonsterDissolving"));
        this.dissolveTicks = tag.getInt("GillesHugeSeaMonsterDissolveTicks");
        this.deathEffectsApplied = tag.getBoolean("GillesHugeSeaMonsterDeathEffectsApplied");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            String loop = event.isMoving() ? "animation.gilles_huge_sea_monster.walk" : "animation.gilles_huge_sea_monster.idle";
            return event.setAndContinue(RawAnimation.begin().thenLoop(loop));
        }));
        AnimationController<HugeSeaMonsterEntity> action = new AnimationController<>(this, "action_controller", 0, event -> PlayState.STOP);
        action.triggerableAnim("slam", RawAnimation.begin().thenPlay("animation.gilles_huge_sea_monster.slam"));
        action.triggerableAnim("grab", RawAnimation.begin().thenPlay("animation.gilles_huge_sea_monster.grab"));
        controllers.add(action);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    private void pollutionAura(ServerLevel level) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(10.0), this::isValidTarget)) {
            living.hurt(this.damageSources().magic(), 8.0F);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true, true));
        }
        level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 2.0, this.getZ(), 35, 2.4, 1.2, 2.4, 0.04);
    }

    private void spawnUnknowableFog(ServerLevel level) {
        double y = this.getY() + this.getBbHeight() * 0.62;
        level.sendParticles(PURPLE_FOG, this.getX(), y, this.getZ(), 90, 18.0, 14.0, 18.0, 0.025);
        level.sendParticles(DEEP_PURPLE_FOG, this.getX(), y + 2.0, this.getZ(), 70, 15.0, 12.0, 15.0, 0.018);
        if (this.tickCount % 8 == 0) {
            level.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), y, this.getZ(), 80, 19.0, 13.0, 19.0, 0.012);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), y + 1.0, this.getZ(), 60, 17.0, 11.0, 17.0, 0.018);
        }
    }

    private void areaSweep(ServerLevel level) {
        this.triggerAnim("action_controller", "slam");
        level.playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.6F, 0.55F);
        this.breakTerrainAhead(level, 24.0, 16.0F, 24, ATTACK_TERRAIN_BREAK_LIMIT);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(9.0), this::isValidTarget)) {
            living.invulnerableTime = 0;
            living.hurt(this.damageSources().mobAttack(this), 34.0F);
            living.push((living.getX() - this.getX()) * 0.28, 0.35, (living.getZ() - this.getZ()) * 0.28);
            living.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 8, 3.0, 0.4, 3.0, 0.02);
    }

    private void tentacleGrab(ServerLevel level) {
        this.triggerAnim("action_controller", "grab");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(13.0), this::isValidTarget)) {
            if (this.random.nextFloat() > 0.35F) {
                continue;
            }
            Vec3 pull = this.position().subtract(living.position()).normalize().scale(0.45);
            living.setDeltaMovement(living.getDeltaMovement().add(pull.x, 0.12, pull.z));
            living.hurtMarked = true;
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true, true));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, true, true));
            living.hurt(this.damageSources().magic(), 12.0F);
            level.sendParticles(ParticleTypes.SCULK_SOUL, living.getX(), living.getY() + 0.3, living.getZ(), 18, 0.4, 0.6, 0.4, 0.04);
        }
    }

    private void trySummonBrood(ServerLevel level) {
        int small = 0;
        int large = 0;
        for (SeaMonsterEntity seaMonster : level.getEntitiesOfClass(SeaMonsterEntity.class, this.getBoundingBox().inflate(96.0),
                seaMonster -> seaMonster.isAlive() && this.getUUID().equals(seaMonster.getControllerUuid()))) {
            if (seaMonster.isLarge()) {
                large++;
            } else {
                small++;
            }
            if (small >= 100 && large >= 10) {
                break;
            }
        }
        if (small < 100) {
            spawnBrood(level, false);
        }
        if (large < 10 && this.random.nextFloat() < 0.35F) {
            spawnBrood(level, true);
        }
    }

    private void spawnBrood(ServerLevel level, boolean large) {
        SeaMonsterEntity seaMonster = com.example.typemoonaddon.registry.AddonEntities.GILLES_SEA_MONSTER.get().create(level);
        if (seaMonster == null) {
            return;
        }
        Vec3 offset = new Vec3((this.random.nextDouble() - 0.5) * 10.0, 0.0, (this.random.nextDouble() - 0.5) * 10.0);
        seaMonster.moveTo(this.getX() + offset.x, this.getY(), this.getZ() + offset.z, this.getYRot(), 0.0F);
        seaMonster.setController(this);
        seaMonster.setLarge(large);
        level.addFreshEntity(seaMonster);
        level.sendParticles(ParticleTypes.SQUID_INK, seaMonster.getX(), seaMonster.getY() + 0.6, seaMonster.getZ(), large ? 24 : 12, 0.5, 0.5, 0.5, 0.05);
    }

    @Nullable
    private LivingEntity findNearestTarget(double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class, box, this::isValidTarget)) {
            double distance = this.distanceToSqr(candidate);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        return target != null && target != this && target.isAlive() && target.getVehicle() != this
                && !this.getPassengers().contains(target) && !target.isAlliedTo(this) && !this.isFriendly(target)
                && !EntityUtils.isImmunePlayerTarget(target);
    }

    private boolean isFriendly(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == this || entity.getVehicle() == this || this.getPassengers().contains(entity)) {
            return true;
        }
        LivingEntity master = this.getMasterEntity();
        if (entity == master || master != null && master.isAlliedTo(entity)) {
            return true;
        }
        LivingEntity source = this.getSourceEntity();
        if (source != null && (entity == source || source.isAlliedTo(entity))) {
            return true;
        }
        if (source instanceof GillesDeRaisEntity gilles) {
            ServerPlayer sourceMaster = gilles.getEntityMaster();
            if (entity == sourceMaster || sourceMaster != null && sourceMaster.isAlliedTo(entity)) {
                return true;
            }
        }
        if (entity instanceof SeaMonsterEntity seaMonster) {
            UUID controller = seaMonster.getControllerUuid();
            return this.getUUID().equals(controller) || (this.sourceUuid != null && this.sourceUuid.equals(controller));
        }
        return entity instanceof HugeSeaMonsterEntity other
                && this.sourceUuid != null
                && this.sourceUuid.equals(other.sourceUuid);
    }

    @Nullable
    private LivingEntity livingAttacker(DamageSource source) {
        if (source.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return source.getDirectEntity() instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private LivingEntity getMasterEntity() {
        if (this.masterUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(this.masterUuid);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Nullable
    private LivingEntity getSourceEntity() {
        if (this.sourceUuid == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        long now = level.getGameTime();
        if (this.cachedSourceEntityGameTime == now) {
            return this.cachedSourceEntity != null && this.cachedSourceEntity.isAlive() ? this.cachedSourceEntity : null;
        }
        this.cachedSourceEntityGameTime = now;
        Entity entity = level.getEntity(this.sourceUuid);
        this.cachedSourceEntity = entity instanceof LivingEntity living && living.isAlive() ? living : null;
        return this.cachedSourceEntity;
    }

    private void beginDissolve(@Nullable DamageSource cause) {
        if (this.isDissolving()) {
            return;
        }
        if (!this.level().isClientSide() && !this.deathEffectsApplied) {
            this.breakSourceSpellbook();
            com.example.typemoonaddon.servant.GillesDeRaisCombatHelper.addPollutionZone(
                    this.level().dimension(), this.position(), 9.0, 60 * 20, 14.0F, this.sourceUuid, this.masterUuid);
            this.deathEffectsApplied = true;
        }
        this.entityData.set(DISSOLVING, true);
        this.dissolveTicks = 0;
        this.setHealth(Math.max(1.0F, this.getHealth()));
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.setNoGravity(true);
        this.hurtTime = 0;
        this.hurtDuration = 0;
    }

    private void tickDissolve() {
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0.0, 0.0, 0.0);
        this.setNoGravity(true);
        this.hurtTime = 0;
        this.hurtDuration = 0;
        if (!this.level().isClientSide()) {
            this.dissolveTicks++;
            if (this.dissolveTicks >= DISSOLVE_DURATION) {
                this.discard();
            }
        }
    }

    private void breakTerrainAhead(ServerLevel level, double forwardDistance, float halfWidth, int height, int maxBlocks) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot()).normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        int broken = 0;
        int checks = 0;
        int maxChecks = maxBlocks * 6;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int startY = Mth.floor(this.getY());
        int stepHeight = Math.min(height, Mth.ceil(this.getBbHeight() * 0.45F));
        for (int y = 0; y < stepHeight && broken < maxBlocks && checks < maxChecks; y += 2) {
            for (double forwardOffset = 0.0; forwardOffset <= forwardDistance && broken < maxBlocks && checks < maxChecks; forwardOffset += 2.0) {
                for (double sideOffset = -halfWidth; sideOffset <= halfWidth && broken < maxBlocks && checks < maxChecks; sideOffset += 2.0) {
                    checks++;
                    Vec3 position = this.position()
                            .add(forward.scale(forwardOffset))
                            .add(right.scale(sideOffset));
                    mutable.set(Mth.floor(position.x), startY + y, Mth.floor(position.z));
                    if (this.tryBreakBlock(level, mutable)) {
                        broken++;
                    }
                }
            }
        }
    }

    private boolean isStaggeredTick(int interval, int offset) {
        return Math.floorMod(this.tickCount + this.getId(), interval) == offset;
    }

    private boolean tryBreakBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > 80.0F) {
            return false;
        }
        return level.destroyBlock(pos, false, this);
    }

    private void breakSourceSpellbook() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof GillesDeRaisEntity gilles) {
                gilles.loseSpellbook();
            }
        }
        if (this.sourceUuid != null && this.level() instanceof ServerLevel level) {
            Entity source = level.getEntity(this.sourceUuid);
            if (source instanceof GillesDeRaisEntity gilles) {
                gilles.loseSpellbook();
            }
        }
    }
}
