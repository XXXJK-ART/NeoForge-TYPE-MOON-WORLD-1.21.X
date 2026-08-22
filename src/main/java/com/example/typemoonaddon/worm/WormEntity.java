package com.example.typemoonaddon.worm;

import com.example.typemoonaddon.detection.DetectionService;
import com.example.typemoonaddon.registry.AddonItems;
import com.example.typemoonaddon.magic.WormMagicIntegration;
import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public final class WormEntity extends PathfinderMob {
    private WormType variant = WormType.SILVERFISH;
    private int guPower = WormType.SILVERFISH.defaultGu();
    @Nullable
    private UUID ownerId;
    private boolean manuallyControlled;
    private boolean sharedVisionActive;

    public WormEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ATTACK_SPEED, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            int roll = random.nextInt(100);
            setVariant(roll < 40 ? WormType.SILVERFISH
                    : roll < 65 ? WormType.ENDERMITE
                    : roll < 85 ? WormType.WINGED
                    : roll < 95 ? WormType.FIREPROOF
                    : WormType.DETECTION);
            setGuPower(getVariant().defaultGu() + random.nextInt(3));
            setPersistenceRequired();
        }
        return result;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, true));
        goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                entity -> entity != null && entity != getOwner()));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (variant == WormType.FIREPROOF && (isOnFire() || isInLava())) {
            clearFire();
        }
        if (variant == WormType.DETECTION) {
            this.setNoGravity(true);
            syncSharedVision();
            if (!manuallyControlled && ownerId != null) {
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null && distanceToSqr(owner) > 24.0D * 24.0D) {
                    moveToward(owner.position(), 0.8D);
                }
            }
        } else if (sharedVisionActive) {
            syncSharedVision();
        }
        if (ownerId != null && tickCount % 10 == 0 && variant != WormType.DETECTION) {
            LivingEntity target = level.getEntitiesOfClass(
                            LivingEntity.class,
                            getBoundingBox().inflate(10.0D),
                            entity -> entity != this && entity != getOwner() && entity.isAlive())
                    .stream()
                    .min(Comparator.comparingDouble(this::distanceToSqr))
                    .orElse(null);
            if (target != null) {
                setTarget(target);
            }
        }
    }

    private void moveToward(Vec3 point, double speed) {
        Vec3 delta = point.subtract(position());
        if (delta.lengthSqr() > 0.01D) {
            setDeltaMovement(delta.normalize().scale(speed).add(0.0D, 0.02D, 0.0D));
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level() instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        if (ownerId != null) {
            if (!ownerId.equals(player.getUUID())) {
                return InteractionResult.FAIL;
            }
            if (variant == WormType.DETECTION && player.isShiftKeyDown()) {
                WormMagicIntegration.openDetectionControl(serverPlayer, this);
                return InteractionResult.CONSUME;
            }
        } else if (!WormCaptureService.isControlledForCapture(this)) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.worm.capture_need_control"), true);
            return InteractionResult.FAIL;
        }
        UUID capturedOwner = ownerId == null ? player.getUUID() : ownerId;
        ItemStack captured = WormStackData.create(AddonItems.WORM.get(), variant, guPower, capturedOwner);
        if (!player.addItem(captured)) {
            player.drop(captured, false);
        }
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public boolean fireImmune() {
        return variant == WormType.FIREPROOF;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(WormStackData.TYPE, variant.id());
        tag.putInt(WormStackData.GU, guPower);
        if (ownerId != null) {
            tag.putUUID(WormStackData.OWNER, ownerId);
        }
        tag.putBoolean("Manual", manuallyControlled);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        variant = WormType.byId(tag.getString(WormStackData.TYPE));
        guPower = Math.max(1, tag.getInt(WormStackData.GU));
        ownerId = tag.hasUUID(WormStackData.OWNER) ? tag.getUUID(WormStackData.OWNER) : null;
        manuallyControlled = tag.getBoolean("Manual");
    }

    public WormType getVariant() {
        return variant;
    }

    public void setVariant(WormType variant) {
        this.variant = variant == null ? WormType.SILVERFISH : variant;
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.variant == WormType.DETECTION ? 0.65D : 0.35D);
        }
        if (getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.variant == WormType.WINGED ? 8.0D : 2.0D);
        }
        if (getAttribute(Attributes.ATTACK_SPEED) != null) {
            getAttribute(Attributes.ATTACK_SPEED).setBaseValue(this.variant == WormType.WINGED ? 4.0D : 2.0D);
        }
        syncSharedVision();
    }

    public int getGuPower() {
        return guPower;
    }

    public void setGuPower(int guPower) {
        this.guPower = Math.max(1, guPower);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        syncSharedVision();
    }

    @Nullable
    public LivingEntity getOwner() {
        return ownerId == null || !(level() instanceof ServerLevel level)
                ? null
                : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    public boolean isManuallyControlled() {
        return manuallyControlled;
    }

    public void setManuallyControlled(boolean manuallyControlled) {
        this.manuallyControlled = manuallyControlled;
        syncSharedVision();
    }

    private void syncSharedVision() {
        if (!(level() instanceof ServerLevel level) || ownerId == null) {
            if (sharedVisionActive) {
                sharedVisionActive = false;
            }
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        boolean shouldShare = variant == WormType.DETECTION && manuallyControlled && owner != null;
        if (shouldShare) {
            if (!sharedVisionActive || tickCount % 20 == 0) {
                sharedVisionActive = true;
                DetectionService.syncSharedVision(owner, this, true);
            }
            return;
        }
        if (sharedVisionActive) {
            sharedVisionActive = false;
            DetectionService.syncSharedVision(owner, this, false);
        }
    }
}
