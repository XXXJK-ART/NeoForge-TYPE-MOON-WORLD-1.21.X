package net.xxxjk.TYPE_MOON_WORLD.chain.entity;

import net.xxxjk.TYPE_MOON_WORLD.chain.config.ChainConfig;
import net.xxxjk.TYPE_MOON_WORLD.chain.service.BindingService;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HeavenChainBindingEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> CHAIN_UUID = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Optional<UUID>> TARGET_UUID = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Float> WIDTH_SCALE = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> HEIGHT_SCALE = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Integer> STACK_INDEX = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Boolean> ENUMA_FLOW = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.BOOLEAN
    );
    private static final EntityDataAccessor<Long> MINIMUM_BOUND_UNTIL = SynchedEntityData.defineId(
        HeavenChainBindingEntity.class, EntityDataSerializers.LONG
    );
    private static final RawAnimation BIND_ANIMATION = RawAnimation.begin().thenLoop("animation.chains_of_heaven.bind");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private boolean dissolved;

    public HeavenChainBindingEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = true;
        noCulling = true;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, ChainConfig.CHAIN_MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.ATTACK_DAMAGE, 0.0D)
            .add(Attributes.FOLLOW_RANGE, 1.0D);
    }

    public void initialize(HeavenChainEntity chain, LivingEntity target, int stackIndex, long minimumBoundUntil) {
        entityData.set(CHAIN_UUID, Optional.of(chain.getUUID()));
        entityData.set(TARGET_UUID, Optional.of(target.getUUID()));
        entityData.set(WIDTH_SCALE, Math.max(0.55F, target.getBbWidth() / 0.6F));
        entityData.set(HEIGHT_SCALE, Math.max(0.65F, target.getBbHeight() / 1.8F));
        entityData.set(STACK_INDEX, Math.max(0, stackIndex));
        entityData.set(ENUMA_FLOW, chain.isEnumaChain());
        entityData.set(MINIMUM_BOUND_UNTIL, minimumBoundUntil);
        follow(target);
        syncHealth(chain);
    }

    @Nullable
    public UUID chainUuid() {
        return entityData.get(CHAIN_UUID).orElse(null);
    }

    @Nullable
    public UUID targetUuid() {
        return entityData.get(TARGET_UUID).orElse(null);
    }

    public float widthScale() {
        return entityData.get(WIDTH_SCALE) * stackScale();
    }

    public float heightScale() {
        return entityData.get(HEIGHT_SCALE) * stackScale();
    }

    public boolean hasEnumaFlow() {
        return entityData.get(ENUMA_FLOW);
    }

    public long minimumBoundUntil() {
        return entityData.get(MINIMUM_BOUND_UNTIL);
    }

    private float stackScale() {
        return 1.0F + Math.min(5, entityData.get(STACK_INDEX) % 6) * 0.045F;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHAIN_UUID, Optional.empty());
        builder.define(TARGET_UUID, Optional.empty());
        builder.define(WIDTH_SCALE, 1.0F);
        builder.define(HEIGHT_SCALE, 1.0F);
        builder.define(STACK_INDEX, 0);
        builder.define(ENUMA_FLOW, false);
        builder.define(MINIMUM_BOUND_UNTIL, 0L);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity target = resolveTarget(serverLevel);
        HeavenChainEntity chain = resolveChain(serverLevel);
        if (target == null || chain == null || !chain.isAlive()) {
            if (tickCount >= 40) {
                dissolve();
            }
            return;
        }
        follow(target);
        syncHealth(chain);
        if (entityData.get(ENUMA_FLOW) != chain.hasEnumaFlow()) {
            entityData.set(ENUMA_FLOW, chain.hasEnumaFlow());
        }
        BindingService.ensureBinding(serverLevel, chain, target, this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || amount <= 0.0F || dissolved || !(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        HeavenChainEntity chain = resolveChain(serverLevel);
        LivingEntity target = resolveTarget(serverLevel);
        if (chain == null || target == null || source.getEntity() == null && source.getDirectEntity() == null) {
            return false;
        }
        boolean damaged = chain.hurtFromBinding(this, target, source, amount);
        if (chain.isAlive()) {
            syncHealth(chain);
        }
        return damaged;
    }

    public void dissolve() {
        if (dissolved) {
            return;
        }
        dissolved = true;
        if (level() instanceof ServerLevel level) {
            int waxCount = hasEnumaFlow() ? ChainConfig.ENUMA_BINDING_DISSOLVE_WAX_PARTICLES : 28;
            int endRodCount = hasEnumaFlow() ? ChainConfig.ENUMA_BINDING_DISSOLVE_END_ROD_PARTICLES : 10;
            level.sendParticles(ParticleTypes.WAX_ON, getX(), getY() + getBbHeight() * 0.5D, getZ(), waxCount,
                getBbWidth() * 0.55D, getBbHeight() * 0.4D, getBbWidth() * 0.55D, 0.05D);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5D, getZ(), endRodCount,
                getBbWidth() * 0.35D, getBbHeight() * 0.3D, getBbWidth() * 0.35D, 0.025D);
        }
        discard();
    }

    private void follow(LivingEntity target) {
        setPos(target.getX(), target.getY(), target.getZ());
        setYRot(target.getYRot() + entityData.get(STACK_INDEX) * 17.0F);
        yRotO = getYRot();
    }

    private void syncHealth(HeavenChainEntity chain) {
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && Math.abs(maxHealth.getBaseValue() - chain.chainMaxHealth()) > 1.0E-4D) {
            maxHealth.setBaseValue(chain.chainMaxHealth());
        }
        setHealth(Math.max(1.0F, Math.min(getMaxHealth(), chain.chainHealth())));
    }

    @Nullable
    private LivingEntity resolveTarget(ServerLevel level) {
        UUID id = targetUuid();
        Entity entity = id == null ? null : level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Nullable
    private HeavenChainEntity resolveChain(ServerLevel level) {
        UUID id = chainUuid();
        Entity entity = id == null ? null : level.getEntity(id);
        return entity instanceof HeavenChainEntity chain ? chain : null;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Chain")) {
            entityData.set(CHAIN_UUID, Optional.of(tag.getUUID("Chain")));
        }
        if (tag.hasUUID("Target")) {
            entityData.set(TARGET_UUID, Optional.of(tag.getUUID("Target")));
        }
        entityData.set(WIDTH_SCALE, Math.max(0.1F, tag.getFloat("WidthScale")));
        entityData.set(HEIGHT_SCALE, Math.max(0.1F, tag.getFloat("HeightScale")));
        entityData.set(STACK_INDEX, Math.max(0, tag.getInt("StackIndex")));
        entityData.set(ENUMA_FLOW, tag.getBoolean("EnumaFlow"));
        entityData.set(MINIMUM_BOUND_UNTIL, tag.getLong("MinimumBoundUntil"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID chain = chainUuid();
        UUID target = targetUuid();
        if (chain != null) {
            tag.putUUID("Chain", chain);
        }
        if (target != null) {
            tag.putUUID("Target", target);
        }
        tag.putFloat("WidthScale", entityData.get(WIDTH_SCALE));
        tag.putFloat("HeightScale", entityData.get(HEIGHT_SCALE));
        tag.putInt("StackIndex", entityData.get(STACK_INDEX));
        tag.putBoolean("EnumaFlow", hasEnumaFlow());
        tag.putLong("MinimumBoundUntil", minimumBoundUntil());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "binding", 0, state -> state.setAndContinue(BIND_ANIMATION)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}

