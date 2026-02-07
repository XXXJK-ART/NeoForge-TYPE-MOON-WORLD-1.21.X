package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.xxxjk.TYPE_MOON_WORLD.world.dimension.ModDimensions;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.SwordBarrelBlock;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.SwordBarrelBlockEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.magic.unlimited_blade_works.ChantHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm.MagicBrokenPhantasm;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("null")
public class SwordBarrelProjectileEntity extends ThrowableItemProjectile {

    private List<Entity> hitEntities = new ArrayList<>();

    public SwordBarrelProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public SwordBarrelProjectileEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.SWORD_BARREL_PROJECTILE.get(), shooter, level);
        this.setItem(stack);
    }

    public SwordBarrelProjectileEntity(Level level, double x, double y, double z) {
        super(ModEntities.SWORD_BARREL_PROJECTILE.get(), x, y, z, level);
    }



    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_SWORD; // Fallback
    }
    private static final EntityDataAccessor<Integer> HOVER_TICKS = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> IS_HOVERING = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN);
    // Target position for hovering sword to fly towards
    // Manually register OPTIONAL_VEC3 if not available in mapping, but typically it is.
    // If EntityDataSerializers.OPTIONAL_VEC3 is missing, use manual serializer.
    // However, it's likely a mapping issue. In recent versions, try OPTIONAL_VEC3 or just VEC3 if optional is not standard?
    // Actually, EntityDataSerializers.OPTIONAL_VEC3 was added in later versions. 
    // If it fails, we can use 3 floats for target X, Y, Z.
    
    // Fallback: Use 3 Floats for Target Pos (X, Y, Z) to avoid compatibility issues.
    private static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> HAS_TARGET = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    // Flag for Broken Phantasm Mode (UBW)
    private static final EntityDataAccessor<Boolean> IS_BROKEN_PHANTASM = SynchedEntityData.defineId(SwordBarrelProjectileEntity.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOVER_TICKS, 0);
        builder.define(IS_HOVERING, false);
        builder.define(TARGET_X, 0f);
        builder.define(TARGET_Y, 0f);
        builder.define(TARGET_Z, 0f);
        builder.define(HAS_TARGET, false);
        builder.define(IS_BROKEN_PHANTASM, false);
    }
    
    public void setBrokenPhantasm(boolean isBrokenPhantasm) {
        this.entityData.set(IS_BROKEN_PHANTASM, isBrokenPhantasm);
    }
    
    public boolean isBrokenPhantasm() {
        return this.entityData.get(IS_BROKEN_PHANTASM);
    }
    
    public void setHover(int ticks, Vec3 target) {
        this.entityData.set(HOVER_TICKS, ticks);
        this.entityData.set(IS_HOVERING, true);
        if (target != null) {
            this.entityData.set(HAS_TARGET, true);
            this.entityData.set(TARGET_X, (float)target.x);
            this.entityData.set(TARGET_Y, (float)target.y);
            this.entityData.set(TARGET_Z, (float)target.z);
        } else {
            this.entityData.set(HAS_TARGET, false);
        }
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    // Flag for Mode 1 Tracking
    private boolean isMode1Tracking = false;
    
    public void setMode1Tracking(boolean tracking) {
        this.isMode1Tracking = tracking;
    }

    // Flag for Mode 2 Tracking
    private boolean isMode2Tracking = false;
    
    public void setMode2Tracking(boolean tracking) {
        this.isMode2Tracking = tracking;
    }

    @Override
    public void tick() {
        if (this.entityData.get(IS_HOVERING)) {
            int ticks = this.entityData.get(HOVER_TICKS);
            
            // Client-side rotation update from SynchedData
            if (this.level().isClientSide) {
                if (this.entityData.get(HAS_TARGET)) {
                    // Smoothly interpolate towards target
                    Vec3 target = new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
                    Vec3 dir = target.subtract(this.position()).normalize();
                    
                    // Calculate desired rotation
                    // Inverted Pitch as per user request (Fix "Low head -> Sword Up" issue)
                    float targetXRot = (float)(Math.toDegrees(Math.asin(dir.y)));
                    
                    // Fix XY Plane singularity flip
                    double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0 ? 1.0E-5 : -1.0E-5) : dir.z;
                    // Inverted Yaw X as per user request (Fix "Left turn -> Sword Right turn" issue)
                    float targetYRot = (float)(Math.toDegrees(Math.atan2(dir.x, safeZ)));
                    
                    // Mode 2 specific: Apply flight rotation logic even during hover
                    // If it's Mode 2 (we can't check mode directly here easily without syncing another int, 
                    // but since Mode 1 and Mode 2 now share the same rotation logic, it's fine).
                    
                    this.setXRot(targetXRot);
                    this.setYRot(targetYRot);
                    
                    this.xRotO = this.getXRot();
                    this.yRotO = this.getYRot();
                }
            }

            if (ticks > 0) {
                this.entityData.set(HOVER_TICKS, ticks - 1);
                // Keep position fixed
                this.setDeltaMovement(Vec3.ZERO);
                
                // Update Target Position based on Owner's Look (if server side)
                if (!this.level().isClientSide) {
                    Entity owner = this.getOwner();
                    if (owner instanceof LivingEntity livingOwner) {
                        // Raytrace new target
                        double range = 40.0; // Default range
                        Vec3 eyePos = livingOwner.getEyePosition();
                        Vec3 lookVec = livingOwner.getLookAngle();
                        Vec3 endPos = eyePos.add(lookVec.scale(range));
                        
                        // Simple raytrace for block or just use look endpoint
                        HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(eyePos, endPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, livingOwner));
                        Vec3 newTarget = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : endPos;
                        
                        // Check for entity target (optional, but good for "aiming")
                        // For now just look point is fine.
                        
                        // Update Synched Data (This causes the sword to turn)
                        this.entityData.set(TARGET_X, (float)newTarget.x);
                        this.entityData.set(TARGET_Y, (float)newTarget.y);
                        this.entityData.set(TARGET_Z, (float)newTarget.z);
                    }
                    
                     if (this.entityData.get(HAS_TARGET)) {
                        Vec3 target = new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
                        Vec3 dir = target.subtract(this.position()).normalize();
                        
                        // Inverted Pitch as per user request
                        float targetXRot = (float)(Math.toDegrees(Math.asin(dir.y)));
                        double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0 ? 1.0E-5 : -1.0E-5) : dir.z;
                        // Inverted Yaw X as per user request
                        float targetYRot = (float)(Math.toDegrees(Math.atan2(dir.x, safeZ)));
                        
                        // Direct set for responsiveness
                        this.setXRot(targetXRot);
                        this.setYRot(targetYRot);
                    }
                }
                
                // Update position to prevent falling
                this.setPos(this.getX(), this.getY(), this.getZ());
                
                if (this.level().isClientSide) {
                     this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                }
                return; // Skip normal tick
            } else {
                // Hover finished
                this.entityData.set(IS_HOVERING, false);
                this.setNoGravity(false);
                
                // Launch towards target
                if (this.entityData.get(HAS_TARGET)) {
                    Vec3 target = new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
                    Vec3 dir = target.subtract(this.position()).normalize();
                    double speed = 2.0;
                    this.setDeltaMovement(dir.scale(speed));
                    this.level().playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
                    
                    // Force update rotation one last time before launching
                    // Inverted Pitch/Yaw
                    this.setXRot((float)(Math.toDegrees(Math.asin(-dir.y))));
                    
                    double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0 ? 1.0E-5 : -1.0E-5) : dir.z;
                    this.setYRot((float)(Math.toDegrees(Math.atan2(-dir.x, safeZ))));
                    
                    this.xRotO = this.getXRot();
                    this.yRotO = this.getYRot();
                }
            }
        } else {
            // Normal flight logic (not hovering)
            
            // Mode 1 or Mode 2 Tracking Logic
            // If enabled and owner is present, update velocity to curve towards owner's look target
            if ((this.isMode1Tracking || this.isMode2Tracking) && !this.level().isClientSide) {
                Entity owner = this.getOwner();
                if (owner instanceof LivingEntity livingOwner) {
                     // Calculate target point (Owner's look)
                     double range = 64.0;
                     Vec3 eyePos = livingOwner.getEyePosition();
                     Vec3 lookVec = livingOwner.getLookAngle();
                     Vec3 endPos = eyePos.add(lookVec.scale(range));
                     HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(eyePos, endPos, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, livingOwner));
                     Vec3 targetPos = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : endPos;
                     
                     // Steer towards target
                     Vec3 currentVel = this.getDeltaMovement();
                     double speed = currentVel.length();
                     if (speed > 0.1) {
                         Vec3 desiredDir = targetPos.subtract(this.position()).normalize();
                         // Interpolate direction (turn rate)
                         // Mode 1: 0.15 (Smoother)
                         // Mode 2: 0.25 (Faster turn for following look)
                         double turnRate = this.isMode2Tracking ? 0.25 : 0.15;
                         
                         Vec3 newDir = currentVel.normalize().lerp(desiredDir, turnRate).normalize();
                         this.setDeltaMovement(newDir.scale(speed));
                     }
                }
            }

            // Ensure rotation follows velocity for Mode 2
            Vec3 motion = this.getDeltaMovement();
            if (motion.lengthSqr() > 0.01) {
                 double vH = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                 
                 // Standard: XRot = atan2(y, horizontal) * (180/PI)
                 // YRot = atan2(x, z) * (180/PI)
                 // REVERTED to Standard as per user request for FLIGHT
                 
                 this.setXRot((float)(Math.toDegrees(Math.atan2(motion.y, vH)))); // Standard Y for pitch
                 
                 double safeZ = Math.abs(motion.z) < 1.0E-5 ? (motion.z >= 0 ? 1.0E-5 : -1.0E-5) : motion.z;
                 this.setYRot((float)(Math.toDegrees(Math.atan2(motion.x, safeZ)))); // Standard X for yaw
                 
                 // Previous pos rotation (for smooth interpolation)
                 this.xRotO = this.getXRot();
                 this.yRotO = this.getYRot(); 
            } else if (this.entityData.get(HAS_TARGET)) {
                 // Even if velocity is zero (e.g. just launched or blocked), keep facing target
                 // This covers the split second between hover end and velocity update
                 Vec3 target = new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
                 Vec3 dir = target.subtract(this.position()).normalize();
                 
                 // Standard logic for FLIGHT preparation (if no velocity yet)
                 this.setXRot((float)(Math.toDegrees(Math.asin(-dir.y))));
                 
                 double safeZ = Math.abs(dir.z) < 1.0E-5 ? (dir.z >= 0 ? 1.0E-5 : -1.0E-5) : dir.z;
                 this.setYRot((float)(Math.toDegrees(Math.atan2(-dir.x, safeZ))));
                 
                 this.xRotO = this.getXRot();
                 this.yRotO = this.getYRot();
            }
        }
        
        super.tick();
        
        // Max Lifetime check (5 seconds = 100 ticks)
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
        
        if (this.level().isClientSide) {
             this.level().addParticle(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
    }
    
    private float calculateDamage() {
        ItemStack stack = this.getItem();
        if (stack.isEmpty()) return 4.0f;
        
        ItemAttributeModifiers modifiers = stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double damage = modifiers.compute(1.0, EquipmentSlot.MAINHAND); 
        return (float)Math.max(4.0, damage);
    }

    private void triggerExplosion() {
        if (this.level().isClientSide) return;
        
        double cost = MagicBrokenPhantasm.calculateCost(this.getItem(), true); // Has sword attribute
        float explosionPower = (float)(cost / 20.0);
        
        // Fix for low mana/cost items having 0 explosion power
        // If power is too low (e.g. wooden sword), set a minimum base
        if (explosionPower < 1.0f) explosionPower = 1.0f;
        
        float damagePower = explosionPower; // Full power for damage
        float radiusPower = Math.min(explosionPower, 5.0f); // Cap radius to 5.0
        
        // Increase base radius slightly for better AOE feel
        if (radiusPower < 2.0f) radiusPower = 2.0f;
        
        // Explosion Effect (Block destruction is minimal/none for UBW usually, but let's follow standard)
        // Usually Broken Phantasm breaks blocks.
        // But UBW swords might be too spammy.
        // Let's use a standard explosion but maybe no block breaking? 
        // User didn't specify no block breaking, but said "Follow Broken Phantasm logic".
        // Broken Phantasm logic DOES break blocks.
        
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), radiusPower, Level.ExplosionInteraction.BLOCK);
        
        // Damage Entities (if explosion interaction doesn't cover it enough, or we want custom damage)
        // Standard explode() deals damage based on distance.
        // But we need "Damage: Power * 10" and "Unlimited Damage".
        // The standard explosion damage is roughly Power * 7 at center.
        // We should apply CUSTOM damage to ensure it matches the request.
        
        double damageRadius = radiusPower * 3.0; // Increased radius multiplier (was 2.0)
        AABB damageBox = this.getBoundingBox().inflate(damageRadius);
        List<Entity> entities = this.level().getEntities(this, damageBox);
        
        DamageSource explosionSource = this.damageSources().explosion(this, this.getOwner());
        
        for (Entity e : entities) {
            if (e instanceof LivingEntity) {
                // Check distance for falloff? Or full damage in radius?
                // "Damage unlimited" -> likely means full calculated damage.
                // Let's apply full damage if within radius.
                double distSqr = e.distanceToSqr(this.position());
                if (distSqr <= damageRadius * damageRadius) {
                    // Base Damage 5.0 + Power * 10
                    float totalDamage = 5.0f + damagePower * 10.0f;
                    e.invulnerableTime = 0;
                    e.hurt(explosionSource, totalDamage);
                    e.invulnerableTime = 0;
                }
            }
        }
        
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            // Check Broken Phantasm Mode
            if (isBrokenPhantasm()) {
                triggerExplosion();
                return;
            }
        
            // Check if hit entity is player (shooter)
            if (result.getType() == HitResult.Type.ENTITY) {
                net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) result;
                Entity target = entityHit.getEntity();

                // Ignore shooter (Owner)
                if (target.equals(this.getOwner())) {
                    return; 
                }
                
                // Damage other entities but DO NOT STOP
                if (!hitEntities.contains(target)) {
                    Entity owner = this.getOwner();
                    if (owner instanceof ServerPlayer serverPlayer && this.level() instanceof ServerLevel serverLevel) {
                        FakePlayer fakePlayer = new FakePlayer(serverLevel, new GameProfile(UUID.randomUUID(), "[UBW_Proxy]"));
                        fakePlayer.setPos(this.getX(), this.getY(), this.getZ());
                        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, this.getItem());
                        
                        fakePlayer.resetAttackStrengthTicker();
                        target.invulnerableTime = 0;
                        fakePlayer.attack(target);
                        
                        if (target instanceof LivingEntity livingTarget) {
                            livingTarget.setLastHurtByMob(serverPlayer);
                            if (livingTarget instanceof Mob mob) {
                                mob.setTarget(serverPlayer);
                            }
                            EntityUtils.triggerSwarmAnger(this.level(), serverPlayer, livingTarget);
                        }
                        
                        target.invulnerableTime = 0;
                        
                        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        fakePlayer.discard();
                    } else {
                        float damage = calculateDamage();
                        target.invulnerableTime = 0;
                        target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
                        target.invulnerableTime = 0;
                    }
                    hitEntities.add(target);
                }
                return; 
            }
            
            // If hit block
            if (result.getType() == HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) result;
                BlockPos hitPos = blockHit.getBlockPos();
                BlockState state = this.level().getBlockState(hitPos);
                
                // Pass through non-solid blocks
                if (state.getCollisionShape(this.level(), hitPos).isEmpty() || !state.canOcclude()) {
                     return; 
                }
                
                // Check if the block is already a SwordBarrelBlock (prevent stacking)
                if (state.getBlock() instanceof SwordBarrelBlock) {
                    this.discard();
                    return;
                }

                // Calculate Place Position (The air block adjacent to hit face)
                BlockPos placePos = hitPos.relative(blockHit.getDirection());
                
                // Check nearby entities before placing
                double range = 20.0;
                if (this.level().dimension().location().equals(ModDimensions.UBW_KEY.location())) {
                    range = 36.0;
                }
                
                AABB checkArea = new AABB(placePos).inflate(range);
                List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(LivingEntity.class, checkArea);
                nearbyEntities.removeIf(e -> e.equals(this.getOwner()));
                
                if (nearbyEntities.isEmpty()) {
                     this.discard();
                     return;
                }
                
                // Check if placeable (Air or replaceable)
                BlockState placeState = this.level().getBlockState(placePos);
                if (!placeState.canBeReplaced()) {
                     this.discard();
                     return;
                }
                
                // Determine Rotation based on Hit Direction
                Direction hitFace = blockHit.getDirection();
                // We want the sword to stick OUT of the face we hit, or INTO it?
                // Usually projectile sticks INTO the block.
                // So if we hit the NORTH face of a block, the sword is pointing SOUTH (entering).
                // The block we place is at SOUTH of the hit block.
                // So the sword block should be facing NORTH (towards the block it's stuck in).
                Direction facing = hitFace.getOpposite();
                
                // Chance to place block (reduced probability as requested)
                // "留在地上的概率减小一点"
                // Let's say 50% chance? Or 30%?
                if (this.random.nextFloat() > 0.5f) { // 50% chance to stay (0.5 threshold)
                    this.discard();
                    return;
                }
                
                BlockState newState = ModBlocks.SWORD_BARREL_BLOCK.get().defaultBlockState()
                        .setValue(SwordBarrelBlock.FACING, facing)
                        .setValue(SwordBarrelBlock.ROTATION_A, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_B, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_C, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_D, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_E, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_F, this.random.nextBoolean())
                        .setValue(SwordBarrelBlock.ROTATION_G, this.random.nextBoolean());
                
                if (this.level().setBlock(placePos, newState, 3)) {
                     // Set Tile Entity
                     if (this.level().getBlockEntity(placePos) instanceof SwordBarrelBlockEntity tile) {
                         tile.setStoredItem(this.getItem());
                         
                         // Store precise rotation for renderer
                         // We want to capture the projectile's rotation at impact
                         tile.setCustomRotation(this.getXRot(), this.getYRot());
                     }
                     
                     // Register to ChantHandler for tracking and cleanup
                     if (this.getOwner() instanceof ServerPlayer serverPlayer) {
                         ChantHandler.registerPlacedSword(serverPlayer.getUUID(), placePos);
                     }
                }

                this.discard();
                return;
            }

            this.discard();
        }
    }
}
