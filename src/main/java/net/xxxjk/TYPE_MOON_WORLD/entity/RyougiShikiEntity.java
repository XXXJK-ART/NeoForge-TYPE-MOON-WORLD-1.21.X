package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;

import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;

public class RyougiShikiEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Boolean> IS_DEFENDING = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_BETRAYED = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN); // Track if betrayed (attacked after feeding)
    private static final EntityDataAccessor<Integer> FRIENDSHIP_LEVEL = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.INT); // Track friendship level (berry feeds)
    private static final EntityDataAccessor<Boolean> HAS_LEFT_ARM = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HAS_SWORD = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int blockingTimer = 0;
    private int slashCooldown = 0;
    private int ultimateCooldown = 0; // New Ultimate Skill Cooldown
    private int swordRetrievalTimer = 0; // Timer for retrieving sword
    private int knifeThrowCooldown = 0; // Cooldown for throwing knife skill
    private int lastDamageTime = 0; // Track time since last damage dealt
    private NearestAttackableTargetGoal<Monster> attackMonstersGoal;
    private NearestAttackableTargetGoal<LivingEntity> attackPlayerEnemiesGoal; // New goal to attack player's enemies
    private NearestAttackableTargetGoal<LivingEntity> counterTargetGoal; // Goal to attack entities targeting Shiki
    private boolean isSerious = false;
    private boolean isGuerrilla = false; // Hit and run mode for tough enemies

    private int killCount = 0;
    private long lastKillTime = 0;
    private int idleTimer = 0;
    
    private boolean isPanicState = false;
    private int forcedSlashTimer = 0;
    private boolean isWeakened = false;
    private int weaknessTimer = 0;

    public RyougiShikiEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        // Cat-like reflexes: Immune to fall damage
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Fire Extinguish Logic (50% chance per tick)
            if (this.isOnFire()) {
                if (this.random.nextFloat() < 0.5F) {
                    this.clearFire();
                } else {
                    // Seek Water (if still on fire)
                    // Try to find water in 10 block radius
                    BlockPos waterPos = null;
                    BlockPos center = this.blockPosition();
                    // Simple scan
                    for(BlockPos p : BlockPos.betweenClosed(center.offset(-10, -5, -10), center.offset(10, 5, 10))) {
                        if (this.level().getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
                            waterPos = p.immutable();
                            break;
                        }
                    }
                    
                    if (waterPos != null) {
                        this.getNavigation().moveTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.5D);
                    }
                }
            }

            // Vehicle Escape Logic
            if (this.isPassenger()) {
                // If has target or annoyed, dismount
                if (this.getTarget() != null || this.random.nextFloat() < 0.05F) {
                    this.stopRiding();
                    this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
                }
            }

            // 1. Manage Weakness
            if (this.isWeakened) {
                this.weaknessTimer--;
                if (this.weaknessTimer <= 0) {
                    this.isWeakened = false;
                }
            }

            // 2. Manage Panic State
            if (this.isPanicState) {
                this.forcedSlashTimer++;
                this.performPanicSlash();
                
                // Exit Conditions
                boolean safeFromLava = !this.isInLava();
                boolean safeFromSiege = !this.isBesieged();
                
                if ((safeFromLava && safeFromSiege) || this.forcedSlashTimer > 100) {
                    this.isPanicState = false;
                    this.isWeakened = true;
                    this.weaknessTimer = 20; // 1 second weakness
                    this.forcedSlashTimer = 0;
                }
                // Suppress other behaviors in panic mode
                return;
            }

            // Idle Check
            if (this.getDeltaMovement().lengthSqr() < 0.001 && this.getTarget() == null) {
                this.idleTimer++;
                if (this.idleTimer > 600 && this.random.nextFloat() < 0.005F) { // 30s idle
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.idle", 20.0);
                    this.idleTimer = 0;
                }
            } else {
                this.idleTimer = 0;
            }

            // Low Health Check (Periodically)
            if (this.tickCount % 600 == 0 && this.getHealth() < this.getMaxHealth() * 0.2F) {
                broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.low_health", 20.0, 0.3F);
            }

            // Environment Check (Every ~30s)
            if (this.tickCount % 600 == 0 && this.random.nextFloat() < 0.1F) {
                if (this.level().isThundering()) {
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_thunder", 20.0);
                } else if (this.level().isNight() && this.level().canSeeSky(this.blockPosition())) {
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_night", 20.0);
                } else if (this.level().dimension() == Level.NETHER) {
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_nether", 20.0);
                } else if (this.level().dimension() == Level.END) {
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_the_end", 20.0);
                }
            }

            // Player Looking Check
            if (this.tickCount % 40 == 0) {
                for (Player player : this.level().players()) {
                    if (player.distanceToSqr(this) < 100 && isLookingAt(player, this)) {
                        broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.player_looking", 20.0, 0.1F); // Low chance
                        break;
                    }
                }
            }
            
            // Passive Interaction with Held Items (Check every 20 ticks / 1s)
            if (this.tickCount % 20 == 0) {
                Player p = this.level().getNearestPlayer(this, 8.0D);
                // Only talk if NOT in combat
                if (p != null && !this.entityData.get(IS_BETRAYED) && this.hasLineOfSight(p) && this.getTarget() == null) {
                    ItemStack heldItem = p.getMainHandItem();
                    
                    // Sword (Lore: Weapon appreciation)
                    if (heldItem.getItem() instanceof net.minecraft.world.item.SwordItem) {
                        if (this.random.nextFloat() < 0.05F) { // 5% chance per second
                            this.getLookControl().setLookAt(p, 30.0F, 30.0F);
                            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_sword", 20.0);
                        }
                    }
                    
                    // Shield (Lore: Defense comment)
                    else if (heldItem.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                        if (this.random.nextFloat() < 0.05F) {
                            this.getLookControl().setLookAt(p, 30.0F, 30.0F);
                            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_shield", 20.0);
                        }
                    }
                    
                    // Clock (Lore: Waiting)
                    else if (heldItem.is(net.minecraft.world.item.Items.CLOCK)) {
                        if (this.random.nextFloat() < 0.05F) {
                            this.getLookControl().setLookAt(p, 30.0F, 30.0F);
                            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_clock", 20.0);
                        }
                    }
                    
                    // Cat-like reactions (Milk, String)
                    else if (heldItem.is(net.minecraft.world.item.Items.MILK_BUCKET) || heldItem.is(net.minecraft.world.item.Items.STRING)) {
                         // Higher chance for cat items (10%)
                         if (this.random.nextFloat() < 0.1F) {
                             this.getLookControl().setLookAt(p, 30.0F, 30.0F);
                             String key = heldItem.is(net.minecraft.world.item.Items.MILK_BUCKET) ? 
                                 "entity.typemoonworld.ryougi_shiki.speech.see_milk" : "entity.typemoonworld.ryougi_shiki.speech.see_string";
                             broadcastToNearbyPlayers(key, 20.0);
                         }
                    }
                }
            }

            // Proactive Dangerous Block Avoidance
            this.checkDangerousBlocks();

            // Water/Lava Aversion Logic (Teleport to land if stuck in fluid)
            if (this.isInWater() || this.isInLava()) {
                // Try to teleport to nearest land (Horizontal 2 blocks, Max 3 attempts)
                boolean safe = false;
                for (int i = 0; i < 3; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double tx = this.getX() + 2.0 * Math.cos(angle);
                    double tz = this.getZ() + 2.0 * Math.sin(angle);
                    double ty = this.getY();
                    
                    // Check if target is safe (Solid block below, Air at feet)
                    BlockPos targetPos = BlockPos.containing(tx, ty, tz);
                    BlockState below = this.level().getBlockState(targetPos.below());
                    BlockState at = this.level().getBlockState(targetPos);
                    
                    if (below.isFaceSturdy(this.level(), targetPos.below(), Direction.UP) && !at.getFluidState().isSource() && !at.isSuffocating(this.level(), targetPos)) {
                        this.teleportTo(tx, ty, tz);
                        safe = true;
                        break;
                    }
                }
                
                if (!safe) {
                    // If still stuck, use Slash to clear terrain (replace fluids with Air)
                    if (this.slashCooldown == 0) {
                        this.performSlash();
                        this.slashCooldown = 20; // Fast cooldown to clear path
                    }
                    
                    // If stuck in Lava, trigger Panic State
                    if (this.isInLava()) {
                        this.startPanicState();
                    }
                }
                
                // Speak occasionally
                if (this.random.nextFloat() < 0.05F) { // 5% chance per tick
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hate_water", 15.0);
                }
            }
            
            // Panic Trigger: Besieged
            if (!this.isWeakened && this.isBesieged() && this.tickCount % 20 == 0) {
                 this.startPanicState();
            }

            // Defense timer logic
            if (this.blockingTimer > 0) {
                this.blockingTimer--;
                if (this.blockingTimer == 0) {
                    this.entityData.set(IS_DEFENDING, false);
                }
            }

            // Environmental Interaction: Rain
            if (this.level().isRainingAt(this.blockPosition()) && this.random.nextFloat() < 0.001F) { // Rare chance in rain
                 // 0.1% per tick -> approx every 50 seconds in rain
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.rain", 20.0);
            }

            // Environmental Interaction: Moon (Night + Clear Sky + Surface)
            if (this.level().isNight() && !this.level().isRaining() && this.level().canSeeSky(this.blockPosition()) && this.random.nextFloat() < 0.0005F) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.moon", 20.0);
            }
            
            // Environmental Interaction: Cherry Grove (If in biome)
            // Note: Cherry Grove is 1.20+, ensure compatibility. Biome check.
            if (this.level().getBiome(this.blockPosition()).is(net.minecraft.world.level.biome.Biomes.CHERRY_GROVE) && this.random.nextFloat() < 0.002F) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.cherry_blossom", 20.0);
            }

            // Status Interaction: Low Health Player Nearby
            if (this.tickCount % 100 == 0) { // Every 5 seconds
                Player p = this.level().getNearestPlayer(this, 10.0D);
                if (p != null && p.getHealth() < p.getMaxHealth() * 0.3F && !this.entityData.get(IS_BETRAYED)) {
                     // Only speak if friendship is high enough to care
                     if (this.entityData.get(FRIENDSHIP_LEVEL) >= 3) {
                         p.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.player_low_health"), false);
                     }
                }
                
                // Status Interaction: Invisible Player
                if (p != null && p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
                     // She has Mystic Eyes, she can likely perceive presence or lines of death even if invisible
                     if (this.random.nextFloat() < 0.3F) {
                         p.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.player_invisible"), false);
                     }
                }
            }

            // Autosuggestion Logic (Self-Buffs when HP < 50%)
            if (this.getHealth() < this.getMaxHealth() * 0.5F) {
                if (!this.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 200, 1));
                }
                if (!this.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST)) {
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 1));
                }
            }

            // Slash Logic
            if (this.slashCooldown > 0) this.slashCooldown--;
            if (this.ultimateCooldown > 0) this.ultimateCooldown--;
            if (this.knifeThrowCooldown > 0) this.knifeThrowCooldown--;
            
            // Sword Retrieval Logic
            if (!this.hasSword()) {
                if (this.swordRetrievalTimer > 0) {
                    this.swordRetrievalTimer--;
                } else {
                    // Retrieve Sword
                    this.entityData.set(HAS_SWORD, true);
                    this.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.retrieve_knife", 20.0);
                }
            }

            // Limb Loss Logic Check (Continuous check)
            // Ensure we are alive (health > 0) to avoid conflict with death logic
            if (this.getHealth() <= 20.0F && this.getHealth() > 0.0F && this.hasLeftArm()) {
                this.entityData.set(HAS_LEFT_ARM, false);
                this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.5F);
                broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm", 30.0);
            }
            
            LivingEntity target = this.getTarget();
            if (target != null) {
                // Knife Throw Logic
                // Conditions: Distance > 5, Has Sword, Cooldown 0
                // New Condition: Target must be in air (Y difference > 2.0) or flying/jumping
                double distSqr = this.distanceToSqr(target);
                double dyThrow = target.getY() - this.getY();
                
                boolean isTargetInAir = dyThrow > 2.0D || !target.onGround();
                
                if (distSqr > 25.0D && this.hasSword() && this.knifeThrowCooldown == 0 && isTargetInAir) {
                     // Check for multiple flying enemies
                     long flyingEnemiesCount = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(20.0D), 
                         e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive() && (!e.onGround() || e.getY() > this.getY() + 2.0D)).size();

                     if (flyingEnemiesCount >= 2) {
                         // Multiple flying enemies: Throw 3 knives
                         if (this.random.nextFloat() < 0.2F) { // 20% chance
                             this.performMultiKnifeThrow(target, 3);
                         }
                     } else {
                         // Single target or just normal air check
                         // 5% chance per tick if conditions met (and far enough)
                         if (this.random.nextFloat() < 0.05F) {
                             this.performKnifeThrow(target);
                         }
                     }
                }

                // Anti-Air Attack Logic
                // If target is above (3 to 8 blocks) and close horizontally
                double dy = target.getY() - this.getY();
                double distSqrToTarget = this.distanceToSqr(target);
                if (dy > 3.0D && dy < 8.0D && distSqrToTarget < 25.0D && this.onGround() && this.random.nextFloat() < 0.2F) {
                      // Jump
                      this.setDeltaMovement(this.getDeltaMovement().add(0, 1.2, 0));
                      this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F);
                      
                      // Dash towards
                      Vec3 vec = target.position().subtract(this.position()).normalize().scale(0.5);
                      this.setDeltaMovement(this.getDeltaMovement().add(vec.x, 0, vec.z));
                      
                      broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.jump_attack", 20.0, 1.0F);
                      
                      // Trigger Slash immediately
                      if (this.slashCooldown == 0) {
                          this.performSlash();
                          this.slashCooldown = 40; // Shorter cooldown for air attack
                      }
                }

                // Teleport logic (When far away OR retreating)
                // If in Guerrilla mode (retreating), allow teleporting backwards or away?
                // The AvoidEntityGoal handles movement, but we can add small blinks here.
                
                boolean canUseSkills = !this.isGuerrilla; // Disable offensive skills when retreating

                if (distSqrToTarget > 64.0D && this.random.nextFloat() < 0.05F) { // > 8 blocks away, 5% chance per tick
                    // Only teleport forward if NOT retreating
                    if (canUseSkills) {
                        Vec3 look = this.getLookAngle();
                        double teleportDist = 2.0D;
                        // Teleport 2 blocks forward
                        double tx = this.getX() + look.x * teleportDist;
                        double ty = this.getY();
                        double tz = this.getZ() + look.z * teleportDist;
                        
                        BlockPos tpPos = BlockPos.containing(tx, ty, tz);
                        if (this.level().getBlockState(tpPos).isAir() || this.level().getBlockState(tpPos).getCollisionShape(this.level(), tpPos).isEmpty()) {
                            this.teleportTo(tx, ty, tz);
                            this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
                            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.dodge", 10.0, 0.1F);
                        }
                    }
                } else if (this.isGuerrilla && this.random.nextFloat() < 0.1F) { // Retreating teleport
                    // Teleport away/backwards
                    Vec3 look = this.getLookAngle().reverse(); // Backwards
                    double teleportDist = 2.0D;
                    double tx = this.getX() + look.x * teleportDist;
                    double ty = this.getY();
                    double tz = this.getZ() + look.z * teleportDist;
                    
                    BlockPos tpPos = BlockPos.containing(tx, ty, tz);
                    if (this.level().getBlockState(tpPos).isAir() || this.level().getBlockState(tpPos).getCollisionShape(this.level(), tpPos).isEmpty()) {
                        this.teleportTo(tx, ty, tz);
                        this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
                    }
                }

                if (this.slashCooldown == 0 && canUseSkills) { // Only slash if not retreating
                    // If target is in air, significantly reduce chance of using Ground Slash
                    // 20% chance if in air, 100% if on ground
                    if (isTargetInAir && this.random.nextFloat() > 0.2F) {
                        // Skip slash attempt
                    } else {
                        boolean shouldSlash = false;
                        
                        // Check enemies count (> 3)
                        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0D), 
                            e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive());
                        if (enemies.size() > 3) {
                            shouldSlash = true;
                            // Dialogue when crowded (Reduced probability: 10%)
                            if (!this.level().isClientSide) {
                                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.crowded", 10.0, 0.1F);
                            }
                        }
                        
                        // Check obstruction (Path stuck)
                        if (!shouldSlash && this.getNavigation().isStuck()) {
                            shouldSlash = true;
                        }
                        
                        if (shouldSlash) {
                            performSlash();
                            this.slashCooldown = 60; // 3 seconds cooldown
                        }
                    }
                }

                List<LivingEntity> toughEnemies = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(10.0D), 
                    e -> e != this && e instanceof Monster && e.getMaxHealth() > 100.0F && e.isAlive());
                
                if (toughEnemies.size() >= 2) {
                    // Too many tough enemies, hit and run!
                    // Logic: Attack for a bit, then flee, then attack
                    if (this.tickCount % 100 < 40) { // 40 ticks run, 60 ticks fight
                        if (!this.isGuerrilla) {
                            // Just entered flee mode
                            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.guerrilla_retreat", 15.0);
                        }
                        this.isGuerrilla = true;
                    } else {
                        this.isGuerrilla = false;
                    }
                } else {
                    this.isGuerrilla = false;
                }
            }
            
            // AI State Machine logic
            float healthPct = this.getHealth() / this.getMaxHealth();
            
            // Passive behavior when player holds Sweet Berries
            Player player = this.level().getNearestPlayer(this, 10.0D);
            if (player != null && (player.getMainHandItem().is(net.minecraft.world.item.Items.SWEET_BERRIES) || player.getOffhandItem().is(net.minecraft.world.item.Items.SWEET_BERRIES))) {
                 // Check if betrayed. If betrayed, ignore berries (unless fed again maybe? But user said "next time no use")
                 // User said: "next time take out sweet berries it is useless" if attacked.
                 boolean betrayed = this.entityData.get(IS_BETRAYED);
                 
                 if (!betrayed) {
                     // Clear target to stop attacking
                     if (this.getTarget() == player) {
                         this.setTarget(null);
                     }
                     
                     // Look at player
                     this.getLookControl().setLookAt(player, 30.0F, 30.0F);
                     
                     // Stop moving if no other enemies around
                     if (this.getTarget() == null) {
                         this.getNavigation().stop();
                     }
                 }
            }
            
            if (healthPct < 0.8F && !this.isSerious) {
                // Enter Serious Mode: More aggressive, less dodge
                this.isSerious = true;
                this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.5F); // Sound cue for mode switch
            } else if (healthPct >= 0.8F) {
                 if (this.isSerious) {
                    // Enter Passive Mode: Cat-like, high dodge, only counter-attack
                    this.isSerious = false;
                }
            }
            
            // Friendship Logic: If friendship >= 5, help player
            // But do not follow (as requested). Just attack hostile mobs targeting player.
            if (this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) {
                 // Add protection goal if not present
                 // Note: Ideally check if goal is running or added, but addGoal handles duplicates gracefully usually?
                 // Actually, best to manage it like attackMonstersGoal
                 this.targetSelector.addGoal(3, this.attackPlayerEnemiesGoal);
            } else {
                 this.targetSelector.removeGoal(this.attackPlayerEnemiesGoal);
            }
        }
        
        // Client-side visual effects
        if (this.level().isClientSide) {
            // Sprint/Attack Trail
            if (this.isSprinting() || this.isGuerrilla) {
                 this.level().addParticle(ParticleTypes.CLOUD, 
                    this.getX() + (this.random.nextDouble() - 0.5), 
                    this.getY() + 0.1, 
                    this.getZ() + (this.random.nextDouble() - 0.5), 
                    0, 0, 0);
            }
        }
    }
    
    private boolean isLookingAt(LivingEntity viewer, LivingEntity target) {
        Vec3 viewVec = viewer.getViewVector(1.0F).normalize();
        Vec3 dirToTarget = target.getEyePosition().subtract(viewer.getEyePosition()).normalize();
        return viewVec.dot(dirToTarget) > 0.95; // ~18 degrees cone
    }




    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D) // Increased HP
                .add(Attributes.MOVEMENT_SPEED, 0.15D) // Moderate base speed
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public void setTarget(@javax.annotation.Nullable LivingEntity target) {
        LivingEntity oldTarget = this.getTarget();
        super.setTarget(target);
        
        if (target != null && target != oldTarget && !this.level().isClientSide) {
            String speechKey = null;
            
            // Priority Checks
            if (target.isInvisible()) {
                speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_invisible";
            } else if (target.getMaxHealth() >= 50 && !(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss) && !(target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) && this.random.nextFloat() < 0.2F) {
                // High HP non-boss check (Seeing Lines), low chance to override specific type
                speechKey = "entity.typemoonworld.ryougi_shiki.speech.seeing_lines";
            } else {
                // Type Checks
                if (target instanceof net.minecraft.world.entity.monster.Zombie) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_zombie";
                } else if (target instanceof net.minecraft.world.entity.monster.Skeleton || target instanceof net.minecraft.world.entity.monster.Stray) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_skeleton";
                } else if (target instanceof net.minecraft.world.entity.monster.Creeper) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_creeper";
                } else if (target instanceof net.minecraft.world.entity.monster.EnderMan) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_enderman";
                } else if (target instanceof net.minecraft.world.entity.monster.Spider || target instanceof net.minecraft.world.entity.monster.CaveSpider) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_spider";
                } else if (target instanceof net.minecraft.world.entity.monster.Witch || target instanceof net.minecraft.world.entity.monster.Evoker) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_witch";
                } else if (target instanceof net.minecraft.world.entity.monster.Phantom) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_phantom";
                } else if (target instanceof net.minecraft.world.entity.monster.Vex || target instanceof net.minecraft.world.entity.monster.Blaze) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_spirit";
                } else if (target instanceof net.minecraft.world.entity.monster.Slime || target instanceof net.minecraft.world.entity.monster.MagmaCube) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_slime";
                } else if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_wither";
                } else if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_ender_dragon";
                } else if (target instanceof net.minecraft.world.entity.animal.IronGolem) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_iron_golem";
                } else if (target instanceof net.minecraft.world.entity.npc.Villager) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_villager";
                } else if (target instanceof net.minecraft.world.entity.animal.Animal || target instanceof net.minecraft.world.entity.TamableAnimal) {
                    speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_animal";
                } else {
                    // Check horde condition
                    List<Monster> nearbyMonsters = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(15.0D));
                    if (nearbyMonsters.size() > 5) {
                        speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_horde";
                    }
                }
            }
            
            if (speechKey != null) {
                // 30% chance to speak specific line on target switch to avoid constant spam
                if (this.random.nextFloat() < 0.3F) {
                    broadcastToNearbyPlayers(speechKey, 20.0);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_DEFENDING, false);
        builder.define(IS_BETRAYED, false);
        builder.define(FRIENDSHIP_LEVEL, 0);
        builder.define(HAS_LEFT_ARM, true);
        builder.define(HAS_SWORD, true);
    }

    public boolean hasLeftArm() {
        return this.entityData.get(HAS_LEFT_ARM);
    }

    public boolean hasSword() {
        return this.entityData.get(HAS_SWORD);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        
        // Guerrilla Tactics: Avoid target if HP is low or enemy is tough (handled in tick)
        // We swap between MeleeAttack and AvoidEntity based on state
        
        // Attack Speed: Base 0.25 * 1.6 = 0.4 (Very Fast)
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6D, false) {
            @Override
            public boolean canUse() {
                // Must have sword to attack
                if (!RyougiShikiEntity.this.hasSword()) return false;
                // Note: Weakened entity can still melee attack (defensive/counter), but skills are disabled.
                return super.canUse() && !RyougiShikiEntity.this.isGuerrilla;
            }
            
            @Override
            public boolean canContinueToUse() {
                 if (!RyougiShikiEntity.this.hasSword()) return false;
                 return super.canContinueToUse();
            }
            
            // Override speed logic if needed, but 1.2D is standard speed multiplier
            // If we want faster approach when targeting, we can increase the modifier in constructor
            // Or dynamically adjust attribute.
        });
        
        // Flee/Kiting goal when Guerrilla mode is active OR when weapon is lost OR when Weakened
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, LivingEntity.class, 
            16.0F, 1.8D, 2.0D, // Increased speed for retreating (Base 0.25 * 1.8 = 0.45 / 2.0 = 0.5)
            (entity) -> entity == RyougiShikiEntity.this.getTarget() && (RyougiShikiEntity.this.isGuerrilla || !RyougiShikiEntity.this.hasSword() || RyougiShikiEntity.this.isWeakened)
        ));

        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Attack all Enemies (Monsters, Slimes, etc.) by default
        this.attackMonstersGoal = new NearestAttackableTargetGoal<>(this, Monster.class, true); 
        // We use LivingEntity.class and filter for Enemy interface to catch Slimes, Magma Cubes, etc.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, 
            (e) -> e instanceof Enemy && !(e instanceof RyougiShikiEntity))); 
        
        // Custom goal: Attack entities that attacked the player (Owner-like behavior but without taming)
        this.attackPlayerEnemiesGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, 
            (entity) -> {
                // Find nearest player
                Player p = this.level().getNearestPlayer(this, 20.0D);
                if (p != null && !this.entityData.get(IS_BETRAYED)) {
                    // Check if entity is targeting that player
                    if (entity instanceof net.minecraft.world.entity.Mob mob) {
                        return mob.getTarget() == p;
                    }
                }
                return false;
            });

        // Goal: Attack any LivingEntity that is targeting Shiki (even if they haven't dealt damage yet)
        this.counterTargetGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
            (entity) -> {
                if (entity instanceof net.minecraft.world.entity.Mob mob) {
                    return mob.getTarget() == this;
                }
                return false;
            });
        this.targetSelector.addGoal(3, this.counterTargetGoal);
        
        // Ultimate Skill Goal: Backstep -> Charge -> Slash
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.Goal() {
            private int step = 0;
            private int timer = 0;

            @Override
            public boolean canUse() {
                LivingEntity t = RyougiShikiEntity.this.getTarget();
                // Conditions:
                // 0. Must have sword
                if (!RyougiShikiEntity.this.hasSword()) return false;
                if (RyougiShikiEntity.this.isWeakened) return false;

                // 1. Cooldown must be 0
                if (RyougiShikiEntity.this.ultimateCooldown > 0 || t == null || !t.isAlive()) return false;
                
                // 2. Triggers:
                // a. HP < 60%
                boolean lowHp = RyougiShikiEntity.this.getHealth() < RyougiShikiEntity.this.getMaxHealth() * 0.6F;
                // b. Target is Player or Boss (Use more often against bosses)
                boolean strongTarget = t instanceof Player || (t instanceof Monster && t.getMaxHealth() > 50);
                // c. Long time no damage (e.g. 200 ticks = 10s)
                boolean longTimeNoDamage = RyougiShikiEntity.this.tickCount - RyougiShikiEntity.this.lastDamageTime > 200;
                // d. Target HP is low (Execution, < 30%)
                boolean execution = t.getHealth() < t.getMaxHealth() * 0.3F && RyougiShikiEntity.this.random.nextFloat() < 0.1F; // 10% chance if low
                
                // Frequency Check: If strong target, increase chance; else keep low
                float chance = strongTarget ? 0.05F : 0.01F; // 5% per tick for bosses, 1% for others (if conditions met)
                if (RyougiShikiEntity.this.random.nextFloat() > chance) return false;

                return lowHp || strongTarget || longTimeNoDamage || execution;
            }

            @Override
            public void start() {
                this.step = 0;
                this.timer = 0;
                RyougiShikiEntity.this.ultimateCooldown = 600; // 30 seconds cooldown
                RyougiShikiEntity.this.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 0.5F);
                // 100% chance for Ultimate Start line
                RyougiShikiEntity.this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.ultimate_start", 30.0);
            }

            @Override
            public boolean canContinueToUse() {
                return this.step < 3;
            }

            @Override
            public void tick() {
                LivingEntity t = RyougiShikiEntity.this.getTarget();
                if (t == null) {
                    this.step = 3; 
                    return;
                }
                
                this.timer++;
                
                if (this.step == 0) {
                    // Phase 1: Backstep (Teleport back)
                    if (this.timer == 1) {
                        Vec3 back = RyougiShikiEntity.this.getLookAngle().reverse().scale(3.0);
                        RyougiShikiEntity.this.teleportTo(RyougiShikiEntity.this.getX() + back.x, RyougiShikiEntity.this.getY(), RyougiShikiEntity.this.getZ() + back.z);
                        RyougiShikiEntity.this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                    }
                    if (this.timer > 10) {
                        this.step = 1;
                        this.timer = 0;
                    }
                } else if (this.step == 1) {
                    // Phase 2: Charge (Dash forward)
                    if (this.timer == 1) {
                         Vec3 dir = t.position().subtract(RyougiShikiEntity.this.position()).normalize().scale(2.0); // Fast dash
                         RyougiShikiEntity.this.setDeltaMovement(dir);
                         RyougiShikiEntity.this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.ultimate_execute", 30.0);
                    }
                    
                    // Break blocks in path (Noclip-like behavior)
                    // Restrict breaking to horizontal or upward to prevent falling into void/holes
                    AABB box = RyougiShikiEntity.this.getBoundingBox().inflate(1.0);
                    BlockPos.betweenClosedStream(box).forEach(pos -> {
                        // Do not break blocks below the entity's feet level
                        if (pos.getY() < RyougiShikiEntity.this.getY() - 0.1) {
                            return;
                        }
                        
                        BlockState state = RyougiShikiEntity.this.level().getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(RyougiShikiEntity.this.level(), pos) >= 0) {
                             RyougiShikiEntity.this.level().destroyBlock(pos, false, RyougiShikiEntity.this);
                        }
                    });
                    
                    if (this.timer > 10) {
                        this.step = 2;
                        this.timer = 0;
                    }
                } else if (this.step == 2) {
                    // Phase 3: Slash / Execute
            if (this.timer == 1) {
                RyougiShikiEntity.this.performSlash();
                // Extra Damage / Kill logic specific to Ultimate (Boosted!)
                RyougiShikiEntity.this.handleMysticEyesEffect(t, true); // Boosted = true
                t.hurt(RyougiShikiEntity.this.damageSources().mobAttack(RyougiShikiEntity.this), 20.0F); // Bonus damage
            }
            if (this.timer > 20) {
                this.step = 3; // End
                RyougiShikiEntity.this.ultimateCooldown = 200; // 10 seconds cooldown after execution
            }
        }
    }
});
            
        // No default attack goal for Player or other mobs unless provoked (HurtByTargetGoal handles provocation)
    }

    // Helper method for broadcasting messages to nearby players only
    // Added cooldown mechanism to prevent spam
    private long lastSpeechTime = 0;
    // Map to track cooldown per message key (String -> Timestamp)
    private java.util.Map<String, Long> messageCooldowns = new java.util.HashMap<>();
    
    // Add variants for speech keys
    // No need to store variants in map, handled in switch
    
    // Improved broadcast method that supports variants
    private void broadcastToNearbyPlayers(String baseKey, double radius) {
         if (!this.level().isClientSide) {
            long currentTime = this.level().getGameTime();
            
            // Global CD
            if (currentTime - lastSpeechTime < 40) return;
            
            // Specific CD (Check base key to prevent spam of same TOPIC, even if variant differs)
            if (messageCooldowns.containsKey(baseKey)) {
                long lastTime = messageCooldowns.get(baseKey);
                if (currentTime - lastTime < 1200) return; // 60s
            }
            
            lastSpeechTime = currentTime;
            messageCooldowns.put(baseKey, currentTime);
            
            // Resolve Variant
            String finalKey = baseKey;
            
            // Special handling for keys with 5 variants
            if (baseKey.equals("entity.typemoonworld.ryougi_shiki.speech.death") || 
                baseKey.equals("entity.typemoonworld.ryougi_shiki.speech.lost_arm")) {
                int variant = this.random.nextInt(5); // 0 to 4
                if (variant > 0) {
                    finalKey = baseKey + "_v" + (variant + 1); // _v2, _v3, _v4, _v5 (base is v1)
                }
                // variant 0 keeps baseKey
            } 
            // Standard 2-variant logic
            else if (this.random.nextBoolean()) {
                // 50% chance to use variant for selected keys
                switch (baseKey) {
                    case "entity.typemoonworld.ryougi_shiki.speech.see_sword":
                    case "entity.typemoonworld.ryougi_shiki.speech.see_shield":
                    case "entity.typemoonworld.ryougi_shiki.speech.see_clock":
                    case "entity.typemoonworld.ryougi_shiki.speech.see_map":
                    case "entity.typemoonworld.ryougi_shiki.speech.see_apple":
                    case "entity.typemoonworld.ryougi_shiki.speech.dodge":
                    case "entity.typemoonworld.ryougi_shiki.speech.throw_knife":
                    case "entity.typemoonworld.ryougi_shiki.speech.retrieve_knife":
                    case "entity.typemoonworld.ryougi_shiki.speech.crowded":
                        finalKey = baseKey + "_v2";
                        break;
                }
            }
            
            Component message = Component.translatable(finalKey);
            for (Player p : this.level().players()) {
                if (p.distanceToSqr(this) <= radius * radius) {
                    p.displayClientMessage(message, false);
                }
            }
         }
    }
    
    private void broadcastToNearbyPlayers(String translationKey, double radius, float probability) {
        if (this.random.nextFloat() < probability) {
            broadcastToNearbyPlayers(translationKey, radius);
        }
    }

    private void broadcastToNearbyPlayers(Component message, double radius, float probability) {
        if (this.random.nextFloat() < probability) {
            // Check if it's a translatable component and get key if possible, otherwise use full string?
            // Actually, for Component, we can't easily get the key back unless we cast.
            // But wait, the error is: broadcastToNearbyPlayers(Component, double) is not defined?
            // Ah, I removed the original (Component, double) overload and replaced it with (String, double).
            // So this method is trying to call a non-existent method.
            
            // We should just use the String version if we can, OR restore the Component version but make it extract key.
            // Since we migrated almost everything to String, let's just deprecate/remove this if unused, 
            // OR fix it to call the String version if the component is translatable.
            
            if (message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc) {
                 broadcastToNearbyPlayers(tc.getKey(), radius);
            } else {
                 // Fallback for literal text? (Should avoid literals for localization)
                 // Just do the old logic without cooldown tracking for literals?
                 if (!this.level().isClientSide) {
                    for (Player p : this.level().players()) {
                        if (p.distanceToSqr(this) <= radius * radius) {
                            p.displayClientMessage(message, false);
                        }
                    }
                 }
            }
        }
    }



    private void performMultiKnifeThrow(LivingEntity target, int count) {
        if (!this.level().isClientSide) {
            if (this.isWeakened) return;
            // Check target is current target
            if (target != this.getTarget()) return;

            // Throw Multiple Knives
            for (int i = 0; i < count; i++) {
                // Delay subsequent throws? Or burst instant?
                // Instant burst is easier to implement.
                
                net.minecraft.world.entity.projectile.Snowball projectile = new net.minecraft.world.entity.projectile.Snowball(this.level(), this) {
                    @Override
                    protected void onHitEntity(net.minecraft.world.phys.EntityHitResult pResult) {
                        if (pResult.getEntity() instanceof LivingEntity living) {
                            if (living instanceof Player p && RyougiShikiEntity.this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !RyougiShikiEntity.this.entityData.get(IS_BETRAYED)) return;
                            if (living == RyougiShikiEntity.this.getTarget()) {
                                RyougiShikiEntity.this.handleMysticEyesEffect(living, true); 
                                living.hurt(RyougiShikiEntity.this.damageSources().thrown(this, RyougiShikiEntity.this), 6.0F);
                            }
                        }
                    }
                    @Override
                    protected void onHit(net.minecraft.world.phys.HitResult pResult) {
                        super.onHit(pResult);
                        if (!this.level().isClientSide) this.discard();
                    }
                    @Override
                    public void tick() {
                        super.tick();
                        if (this.level().isClientSide) {
                             this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                             this.level().addParticle(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                             this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                        }
                    }
                };
                projectile.setInvisible(true); 
                projectile.setItem(ItemStack.EMPTY);

                double d0 = target.getX() - this.getX();
                double d1 = target.getEyeY() - 0.3333333333333333D - projectile.getY();
                double d2 = target.getZ() - this.getZ();
                double d3 = Math.sqrt(d0 * d0 + d2 * d2);
                
                // Add spread for multi-throw
                float spread = i == 0 ? 0 : 10.0F; 
                // Adjust angle for spread? Snowball shoot has uncertainty parameter (last float)
                // We can increase uncertainty for 2nd and 3rd knife
                
                projectile.shoot(d0, d1 + d3 * 0.2D, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4) + spread);
                
                this.level().addFreshEntity(projectile);
            }
            
            this.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.throw_knife", 20.0);
            
            // Set State
            this.entityData.set(HAS_SWORD, false);
            this.swordRetrievalTimer = 40; // 2 seconds (Increased for multi-throw)
            this.knifeThrowCooldown = 200; // 10 seconds cooldown
        }
    }

    private void performKnifeThrow(LivingEntity target) {
        if (!this.level().isClientSide) {
            if (this.isWeakened) return;
            // Check target is current target
            if (target != this.getTarget()) return;

            // Throw Logic
            // Use Snowball instead of Arrow to avoid "arrow" properties, but render it invisible
            // We want an "Empty Entity" that carries damage and effects.
            // Snowball is a good candidate for a simple projectile.
            net.minecraft.world.entity.projectile.Snowball projectile = new net.minecraft.world.entity.projectile.Snowball(this.level(), this) {
                @Override
                protected void onHitEntity(net.minecraft.world.phys.EntityHitResult pResult) {
                    // Do NOT call super.onHitEntity if we want custom damage logic without snowball damage (0)
                    // But snowball deals 0 damage by default to non-blazes.
                    // We manually apply damage.
                    
                    if (pResult.getEntity() instanceof LivingEntity living) {
                        // Absolute Safety Check
                        if (living instanceof Player p && RyougiShikiEntity.this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !RyougiShikiEntity.this.entityData.get(IS_BETRAYED)) return;

                        // Only affect if it's the target
                        if (living == RyougiShikiEntity.this.getTarget()) {
                            // Apply Mystic Eyes Effect
                            RyougiShikiEntity.this.handleMysticEyesEffect(living, true); // Boosted effect
                            living.hurt(RyougiShikiEntity.this.damageSources().thrown(this, RyougiShikiEntity.this), 6.0F);
                        }
                    }
                }
                
                @Override
                protected void onHit(net.minecraft.world.phys.HitResult pResult) {
                    super.onHit(pResult);
                    // Discard on any hit
                    if (!this.level().isClientSide) {
                        this.discard();
                    }
                }

                @Override
                public void tick() {
                    super.tick();
                    // Add Special Effect Particles (Empty Entity Visuals)
                    if (this.level().isClientSide) {
                         // Trail particles
                         this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                         this.level().addParticle(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                         // Some extra "magic" particles
                         this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                    }
                }
            };
            
            // Set invisible. Note: Snowball renders sprite by default. 
            // We might need to rely on the fact that we can't easily change the renderer without a client-side renderer registry.
            // However, `setInvisible(true)` should hide the sprite for most entities.
            projectile.setInvisible(true); 
            // To be double sure, we can set the item to AIR if it uses getItem() for rendering
            projectile.setItem(ItemStack.EMPTY);

            double d0 = target.getX() - this.getX();
            double d1 = target.getEyeY() - 0.3333333333333333D - projectile.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            projectile.shoot(d0, d1 + d3 * 0.2D, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
            
            this.level().addFreshEntity(projectile);
            
            this.playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.throw_knife", 20.0);
            
            // Set State
            this.entityData.set(HAS_SWORD, false);
            this.swordRetrievalTimer = 20; // 1 second
            this.knifeThrowCooldown = 200; // 10 seconds cooldown for skill itself
        }
    }

    private void performSlash() {
        performSlash(false);
    }

    private void performSlash(boolean force) {
        if (this.level().isClientSide) return;
        if (this.isWeakened && !force) return;
        if (!this.hasSword() && !force) return; // Cannot slash without sword
        // Force allows bypassing cooldown and sword check (if needed for survival)
        
        Vec3 lookVec = this.getLookAngle();
        Vec3 center = this.position().add(0, this.getEyeHeight() * 0.5, 0);
        
        // Visuals (Server side particles - might not be visible without custom packet, 
        // but SWEEP_ATTACK is usually visible if spawned correctly)
        ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, 
            center.x + lookVec.x, center.y + lookVec.y, center.z + lookVec.z, 
            5, 0.5, 0.5, 0.5, 0.0);
            
        // Additional slash trail particles
        for (int i = 0; i < 5; i++) {
             double d0 = this.random.nextGaussian() * 0.02D;
             double d1 = this.random.nextGaussian() * 0.02D;
             double d2 = this.random.nextGaussian() * 0.02D;
             ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.CRIT, 
                 center.x + lookVec.x * 1.5 + (this.random.nextDouble() - 0.5), 
                 center.y + lookVec.y * 1.5 + (this.random.nextDouble() - 0.5), 
                 center.z + lookVec.z * 1.5 + (this.random.nextDouble() - 0.5), 
                 1, d0, d1, d2, 0.0);
        }
            
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);

        // Break blocks in semi-circle in front
        // Radius 3, 180 degrees
        for (double r = 0; r <= 3.0; r += 1.0) {
            for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 8) {
                // Rotate lookVec by theta around Y axis
                double x = lookVec.x * Math.cos(theta) - lookVec.z * Math.sin(theta);
                double z = lookVec.x * Math.sin(theta) + lookVec.z * Math.cos(theta);
                Vec3 offset = new Vec3(x, 0, z).normalize().scale(r);
                
                BlockPos pos = BlockPos.containing(center.add(offset));
                BlockState state = this.level().getBlockState(pos);
                // Replace with AIR if destroyable
                // To prevent lava/water issues, we set to AIR.
                // However, user said "prevent being burned by lava", so maybe replace FLUIDS with AIR too?
                // "Destroy terrain slash will directly replace with air block (prevent lava burn)"
                // So if it's lava, replace with AIR.
                if (!state.isAir() && (state.getDestroySpeed(this.level(), pos) >= 0 || state.getFluidState().isSource())) {
                    this.level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
                
                // Also clear 1 block up and down to ensure path
                BlockPos posUp = pos.above();
                if (!this.level().getBlockState(posUp).isAir() && (this.level().getBlockState(posUp).getDestroySpeed(this.level(), posUp) >= 0 || this.level().getBlockState(posUp).getFluidState().isSource())) {
                     this.level().setBlock(posUp, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        
        // Damage Entities
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D).move(lookVec.scale(1.5)), 
             e -> e != this && e.isAlive());
             
        for (LivingEntity e : targets) {
            // Absolute Safety Check
            if (e instanceof Player p && this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) continue;

            // Safety Check: Do not harm non-hostile entities unless they are the target or targeting Shiki/Player
            // Only attack if it is the current target
            boolean isTarget = (e == this.getTarget());
            
            // Or if it's hostile and we are in combat?
            // User requirement: "Skills only affect targeted creatures" (索敌的生物)
            // So strictly enforce isTarget check for skills?
            // "Skills only have effect on targeted mobs"
            
            if (!isTarget) continue; // Skip if not the main target

            // Use Mystic Eyes effect logic (Force Kill or Heavy Damage)
            handleMysticEyesEffect(e);
            // Also apply a base damage to ensure aggro (but suppress it)
            e.hurt(this.damageSources().mobAttack(this), 5.0F);
        }
    }

    public boolean isDefending() {
        return this.entityData.get(IS_DEFENDING);
    }

    @Override
    public void heal(float healAmount) {
        // Cannot heal past 20 if left arm is lost
        if (!this.hasLeftArm()) {
            if (this.getHealth() + healAmount > 20.0F) {
                float allowed = 20.0F - this.getHealth();
                if (allowed > 0) {
                    super.heal(allowed);
                }
                return;
            }
        }
        super.heal(healAmount);
        
        if (healAmount > 1.0F && !this.level().isClientSide && this.getHealth() < this.getMaxHealth()) {
             // 20% chance to thank if healed significantly
             broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.healed", 20.0, 0.2F);
        }
    }
    
    @Override
    public void setHealth(float health) {
        if (!this.hasLeftArm() && health > 20.0F) {
            health = 20.0F;
        }
        
        // Anti-Causality Protection: Prevent setHealth(0) from external mods
        if (health <= 0.0F && !this.isProcessingDamage && this.getHealth() > 0.0F) {
             health = 1.0F; // Keep at 1 HP
             if (!this.level().isClientSide) {
                  broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_causality", 20.0);
             }
        }
        
        super.setHealth(health);
    }

    private int escapeAttempts = 0;
    private long lastEscapeTime = 0;
    private boolean isProcessingDamage = false; // Flag to track internal damage processing

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Force target the attacker regardless of immunity (e.g. perfect defense)
        if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.setTarget(attacker);
        }

        this.isProcessingDamage = true;
        try {
            // 0. Causality/Infinite Damage Protection
            // Cap damage to prevent one-shots from OP mods (Draconic Evolution, Avaritia, etc.)
            // If damage is absurdly high (> 1000), cap it to a survivable amount (e.g., 20% of Max HP)
            if (amount > 1000.0F && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                amount = this.getMaxHealth() * 0.2F; // Cap to 20% max HP
                if (!this.level().isClientSide) {
                     broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_infinite", 20.0);
                }
            }

            // Noble Phantasm Defense Logic
            if (isNoblePhantasmAttack(source)) {
                float chance = this.random.nextFloat();
                if (chance < 0.5F) {
                    // 50% Block (Defense Success)
                    this.triggerDefense();
                    this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
                    return false;
                } else if (chance < 0.9F) {
                    // 40% Half Blood (Take damage to reach 50% HP)
                    float currentHealth = this.getHealth();
                    float halfHealth = this.getMaxHealth() / 2.0F;
                    
                    if (currentHealth > halfHealth) {
                        amount = currentHealth - halfHealth;
                    } 
                    // If already below half health, take original damage (amount)
                    
                    this.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
                    if (!this.level().isClientSide) {
                        broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hurt_noble_phantasm", 20.0);
                    }
                    return super.hurt(source, amount);
                } else {
                    // 10% Fail (Take full damage)
                    return super.hurt(source, amount);
                }
            }

            // Handle Guerrilla Retreat Counter-Attack Logic
        if (this.isGuerrilla && source.getEntity() instanceof LivingEntity attacker) {
             // If attacked while retreating, counter attack or skill!
             this.isGuerrilla = false; // Stop retreating momentarily
             this.setTarget(attacker);
             
             // 50% chance to Slash, 50% chance to just turn and fight (handled by setting target)
             if (this.random.nextBoolean() && this.slashCooldown == 0) {
                 this.performSlash();
                 this.slashCooldown = 40;
             }
             // After counter, she resumes normal AI state, which might decide to flee again next tick
        }

        // Environmental/Trap Escape Logic
        if (!this.level().isClientSide) {
             boolean isTrap = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || 
                              source.is(DamageTypes.LAVA) || 
                              source.is(DamageTypes.IN_WALL) || 
                              source.is(DamageTypes.CRAMMING) || 
                              source.is(DamageTypes.HOT_FLOOR) || 
                              source.is(DamageTypes.SWEET_BERRY_BUSH) || 
                              source.is(DamageTypes.CACTUS) || 
                              source.is(net.minecraft.world.damagesource.DamageTypes.DROWN) ||
                              source.is(DamageTypes.FREEZE) || 
                              source.is(DamageTypes.WITHER) ||
                              (source.getEntity() == null && source.getDirectEntity() == null && !source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD));
            
             if (isTrap) {
                 long currentTime = this.level().getGameTime();
                 // Reset attempts if long time passed (5 seconds)
                 if (currentTime - lastEscapeTime > 100) {
                     escapeAttempts = 0;
                 }
                 
                 if (escapeAttempts < 3) {
                     escapeAttempts++;
                     lastEscapeTime = currentTime;
                     
                     // 1. Voice (Randomized for variety and mod compatibility)
                     String[] escapeLines = {
                         "entity.typemoonworld.ryougi_shiki.speech.escape_trap",
                         "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_1",
                         "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_2",
                         "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_3"
                     };
                     broadcastToNearbyPlayers(escapeLines[this.random.nextInt(escapeLines.length)], 20.0);
                     
                     // 2. 3x3x3 Slash (Center on self)
                     BlockPos center = this.blockPosition();
                     // Destroy Blocks
                     for (int x = -1; x <= 1; x++) {
                         for (int y = -1; y <= 1; y++) {
                             for (int z = -1; z <= 1; z++) {
                                 BlockPos p = center.offset(x, y, z);
                                 BlockState state = this.level().getBlockState(p);
                                 if (!state.isAir() && state.getDestroySpeed(this.level(), p) >= 0) {
                                     this.level().destroyBlock(p, false, this); // No drops
                                 }
                             }
                         }
                     }
                     // Kill Entities
                     AABB box = this.getBoundingBox().inflate(1.5);
                     List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive());
                     for (LivingEntity e : targets) {
                         handleMysticEyesEffect(e, true); // Boosted effect (Instant Kill likely)
                     }
                     
                     // Visuals
                     ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, 
                         this.getX(), this.getY() + 1.0, this.getZ(), 5, 1.0, 1.0, 1.0, 0.0);
                     this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
                     
                     // 3. Teleport (2 blocks towards safety)
                     // Try to find a safe spot (Air) in 2 block radius
                     // Prefer Up/Sides
                     BlockPos bestPos = null;
                     
                     // Simple search: check 2 blocks in cardinal directions + Up
                     BlockPos[] candidates = {
                         center.above(2), center.north(2), center.south(2), center.east(2), center.west(2),
                         center.north(2).above(), center.south(2).above(), center.east(2).above(), center.west(2).above()
                     };
                     
                     for (BlockPos p : candidates) {
                         if (this.level().getBlockState(p).isAir() && this.level().getBlockState(p.above()).isAir()) {
                             bestPos = p;
                             break;
                         }
                     }
                     
                     if (bestPos != null) {
                         this.teleportTo(bestPos.getX() + 0.5, bestPos.getY(), bestPos.getZ() + 0.5);
                     } else {
                         // Fallback: Just teleport Up 2 blocks
                         this.teleportTo(this.getX(), this.getY() + 2.0, this.getZ());
                     }
                     
                     this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                     
                     return false; // Negate damage
                 }
             }
        }

        // Anti-Kill Command / Generic Kill
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
             // Only immune if it's not a legitimate void fall (y < -64)
             if (this.getY() > this.level().getMinBuildHeight() - 64) {
                 this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F); // Heavy sound
                 // Send message to server/players
                 if (!this.level().isClientSide) {
                      broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_void", 20.0);
                 }
                 return false;
             }
        }
        
        // Status Effect / Magic Countermeasure (Kill the Poison/Curse)
        // Probability: 80% chance to negate and remove effects
        if ((source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || 
            source.is(DamageTypes.WITHER)) && this.random.nextFloat() < 0.8F) { 
            
            this.removeAllEffects(); // "Kill" the status effect
            this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
            
            // Visuals
            if (!this.level().isClientSide) {
                ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.WITCH, 
                    this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
            }
            return false;
        }
        
        // Lightning Immunity
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }

        Entity directEntity = source.getDirectEntity();
        
        // Projectile Immunity (90%) and Destruction
        if (directEntity instanceof Projectile) {
            if (this.random.nextFloat() < 0.9F) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F); // Deflect sound
                this.triggerDefense();
                directEntity.discard(); // Kill projectile
                return false; // Immune
            }
        }
        
        // Melee Immunity and Counterattack
        // Check if it's a direct attack from a living entity (melee usually)
        else if (directEntity instanceof LivingEntity attacker && source.getDirectEntity() == source.getEntity()) {
            float chance = this.random.nextFloat();
            
            // If arm is lost, immunity drops
            float immunityReduction = this.hasLeftArm() ? 0.0F : 0.2F; // 20% drop
            
            // 80% Chance (minus reduction): Immune + Counterattack
            if (chance < (0.80F - immunityReduction)) {
                this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
                this.triggerDefense();
                
                // If damage is fatal (or very high), force Guerrilla Mode immediately
                if (amount >= this.getHealth()) {
                    this.isGuerrilla = true;
                    // Attempt one last slash before running
                    if (this.slashCooldown == 0) {
                        this.performSlash();
                        this.slashCooldown = 60;
                    }
                } else {
                    // Probability Active Attack (Counter)
                    // Instead of automatic reflection, we attempt a normal attack swing
                    // Check distance: Only counter if attacker is within melee range (approx 4 blocks)
                    if (this.distanceToSqr(attacker) <= 16.0D && this.random.nextFloat() < 0.5F) {
                        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        if (this.hasSword() && this.slashCooldown == 0) {
                            this.performSlash();
                            this.slashCooldown = 60;
                        } else {
                            // Melee hit (No sword = basic damage, Sword on cooldown = basic damage)
                            // If no sword, doHurtTarget will handle basic damage, but we need to ensure Mystic Eyes logic isn't applied fully or modified
                            this.doHurtTarget(attacker);
                            
                            // If no sword, flee after counter
                            if (!this.hasSword()) {
                                // Add flee behavior
                                // Assuming Goal 1 (AvoidEntity) will pick it up next tick
                            }
                        }
                    }
                }
                
                return false; // Immune
            } 
            
            // Variable Dodge Chance based on Health State
            // HP > 80%: 90% Dodge (Total chance including counter = 90%)
            // HP < 80%: 60% Dodge (Total chance including counter = 60%) - Reduced dodge, more aggressive
            float dodgeLimit = this.isSerious ? 0.60F : 0.90F;
            
            if (chance < dodgeLimit) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F); // Dodge sound
                this.triggerDefense();
                return false; // Immune
            }
        }
        // General Immunity (95%) for other damage types (e.g. magic, explosions, environment)
        // Excluding starvation, void, or creative player attacks which usually bypass invulnerability
        else if (!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
             if (this.random.nextFloat() < 0.95F) {
                 this.triggerDefense();
                 
                 // Kill "Future" or "Magic" (Remove source entity if possible)
                 Entity direct = source.getDirectEntity();
                 if (direct != null && !(direct instanceof LivingEntity)) {
                     // Discard non-living entities (AreaEffectCloud, EvokerFangs, etc.)
                     direct.discard();
                     
                     // Visuals for "Killing Magic"
                     if (!this.level().isClientSide) {
                         ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.WITCH, 
                             direct.getX(), direct.getY() + 0.5, direct.getZ(), 
                             10, 0.2, 0.2, 0.2, 0.0);
                     }
                 }
                 
                 this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F); // Magic breaking sound
                 return false; // Immune
             }
        }

        boolean result = super.hurt(source, amount);
        if (result && amount > 8.0F && !this.level().isClientSide) {
             broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hurt_high", 20.0, 0.5F);
        }
        return result;
        } finally {
            this.isProcessingDamage = false;
        }
    }

    private void triggerDefense() {
        if (!this.level().isClientSide && !this.entityData.get(IS_DEFENDING)) {
            // Cannot defend if on fire
            if (this.isOnFire()) return;
            
            this.entityData.set(IS_DEFENDING, true);
            this.blockingTimer = 10; // Reduced to 0.5 seconds to avoid stun-lock
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!this.level().isClientSide) {
            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.death", 30.0);
        }
    }

    /**
     * Prevent direct kill() calls (often used by /kill command)
     */
    @Override
    public void kill() {
        // Do nothing. Or play a sound.
        if (!this.level().isClientSide && this.isAlive()) {
             this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
             if (this.level() instanceof ServerLevel serverLevel) {
                 serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                     Component.translatable("entity.typemoonworld.ryougi_shiki.speech.refused"), false
                 );
             }
        }
        // super.kill(); // Intentionally removed
    }

    // Helper to prevent retaliation/aggro when Shiki attacks
    // Removed suppressRetaliation method as per request to allow normal aggro/swarm logic

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player player) {
            // Check friendship: Level 5+ implies absolute safety
            if (this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) {
                return false;
            }
        }
        return super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        // Absolute Safety Check for Friends
        if (pEntity instanceof Player player) {
             if (this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) {
                 return false;
             }
        }

        this.lastDamageTime = this.tickCount; // Update last damage time
        boolean flag = super.doHurtTarget(pEntity);
        
        if (!flag && pEntity instanceof LivingEntity living && living.isBlocking() && !this.level().isClientSide) {
             broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.attack_blocked", 20.0, 0.5F);
        }
        
        // Apply Mystic Eyes effect even if normal attack failed (e.g., Creative Mode, Invulnerable)
        // Only apply Mystic Eyes if has sword
        if (pEntity instanceof LivingEntity target && this.hasSword()) {
            // Remove all active potion effects (Buffs/Debuffs) - Concept of "Killing Effects"
            target.removeAllEffects();
            
            // Magical Prosthetic Arm: Bonus damage to Vex and Phantoms (Spirits)
            if (target instanceof net.minecraft.world.entity.monster.Vex || target instanceof net.minecraft.world.entity.monster.Phantom) {
                 float bonusDmg = 10.0F;
                 target.hurt(this.damageSources().mobAttack(this), bonusDmg);
                 this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 2.0F); // Heavy hit sound
            }

            if (flag || target.isInvulnerable() || (target instanceof Player p && p.isCreative())) {
                handleMysticEyesEffect(target);
                // Multi-kill logic hook: if entity dies, increment count
                if (target.isDeadOrDying()) {
                     long time = this.level().getGameTime();
                     if (time - this.lastKillTime < 100) { // 5 seconds window
                         this.killCount++;
                         if (this.killCount >= 3) {
                             broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.multi_kill", 20.0, 1.0F);
                             this.killCount = 0;
                         }
                     } else {
                         this.killCount = 1;
                     }
                     this.lastKillTime = time;
                }
                return true;
            }
        }
        return flag;
    }

    // Immune to block slowdown (e.g. Soul Sand, Cobweb)
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        // Do nothing, preventing slowdown
    }
    
    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effectInstance) {
        if (effectInstance.getEffect() == net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) {
            return false;
        }
        // 50% chance to cancel harmful effects (Debuff resistance)
        if (!effectInstance.getEffect().value().isBeneficial() && this.random.nextFloat() < 0.5F) {
             return false;
        }
        return super.canBeAffected(effectInstance);
    }

    private void handleMysticEyesEffect(LivingEntity target) {
        this.handleMysticEyesEffect(target, false);
    }

    private void handleMysticEyesEffect(LivingEntity target, boolean boosted) {
        float health = target.getHealth();
        
        // 1. Instant Kill Logic
        boolean killed = false;
        if (health <= 20.0F || (boosted && health <= 50.0F)) { // Boosted: Threshold increased to 50
            // Direct Kill
            this.forceKill(target);
            killed = true;
        } else {
            // Probability Kill (Min 1%)
            // Formula: 20 / Health. e.g. 100HP -> 20%, 2000HP -> 1%
            float killChance = Math.max(0.01F, 20.0F / health);
            if (boosted) {
                killChance = Math.max(0.1F, 100.0F / health); // Boosted: Min 10%, formula improved
            }
            if (this.random.nextFloat() < killChance) {
                this.forceKill(target);
                killed = true;
            }
        }

        if (killed) return;

        // 2. Half-HP Logic (Min 20%)
        // Only if not killed
        if (health > 20.0F) {
             // Formula: Scale so it's higher than kill chance. 
             // Let's use 60 / Health, capped at min 0.2.
             float halfChance = Math.max(0.2F, 60.0F / health);
             if (this.random.nextFloat() < halfChance) {
                 target.setHealth(health / 2.0F);
                 this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 2.0F); // Effect sound
             }
        }

        // 3. Equipment Break Logic (Original)
        // Kept as a bonus effect
        if (this.random.nextFloat() < 0.45F) { // 40% + 5% = 45% total chance to affect equipment
            List<EquipmentSlot> validSlots = new ArrayList<>();
            
            // Check armor slots
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    ItemStack stack = target.getItemBySlot(slot);
                    if (!stack.isEmpty() && stack.isDamageableItem()) {
                        validSlots.add(slot);
                    }
                }
            }
            
            // Check main hand
            ItemStack mainHand = target.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
                validSlots.add(EquipmentSlot.MAINHAND);
            }

            if (!validSlots.isEmpty()) {
                EquipmentSlot selectedSlot = validSlots.get(this.random.nextInt(validSlots.size()));
                ItemStack targetItem = target.getItemBySlot(selectedSlot);
                
                if (this.random.nextFloat() < (0.05F / 0.45F)) { // 5% overall chance (approx 11% of the 45% trigger)
                    // Destroy item (set damage to max durability)
                    targetItem.hurtAndBreak(targetItem.getMaxDamage(), target, selectedSlot);
                    if (!targetItem.isEmpty()) {
                        target.setItemSlot(selectedSlot, ItemStack.EMPTY);
                    }
                } else {
                    // Halve remaining durability (increase damage)
                    int currentDamage = targetItem.getDamageValue();
                    int maxDamage = targetItem.getMaxDamage();
                    int remainingDurability = maxDamage - currentDamage;
                    int damageToAdd = remainingDurability / 2;
                    
                    targetItem.hurtAndBreak(damageToAdd, target, selectedSlot);
                }
            }
        }
    }

    private void forceKill(LivingEntity target) {
        // Manually trigger swarm anger for specific mobs before killing
        // This ensures that even if the kill is instant/silent, the swarm is alerted
        triggerSwarmAnger(target);

        if (target instanceof Player player && player.isCreative()) {
            // Bypass Creative Mode
            player.getAbilities().invulnerable = false; // Temporarily disable invulnerability
            player.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
            player.setHealth(0);
            player.die(this.damageSources().genericKill());
            
            player.getAbilities().invulnerable = true; // Restore immediately after dealing fatal damage?
            // But if we restore it too fast, maybe the damage is cancelled?
            // Damage is processed instantly.
            
            player.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.kill_creative"), false);
        } else {
            // Check if this entity was targeting a trusted player (Friendship >= 5)
            // If so, send "I handled it" message to that player
            if (this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) {
                 Player nearestPlayer = this.level().getNearestPlayer(this, 20.0D);
                 if (nearestPlayer != null) {
                      // Verify if the dead target was actually targeting this player
                      // (Simplified check: if it was a mob, it likely was)
                      if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == nearestPlayer) {
                           nearestPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.help_kill"), false);
                      }
                 }
            }

            // Bypass normal invulnerability
            target.setInvulnerable(false);
            target.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
            target.setHealth(0);
            target.die(this.damageSources().genericKill());
        }
        this.playSound(SoundEvents.TRIDENT_THUNDER.value(), 1.0F, 2.0F); // Kill sound
    }

    private void triggerSwarmAnger(LivingEntity target) {
        EntityUtils.triggerSwarmAnger(this.level(), this, target);
    }

    // Map to track interaction frequency (Player -> Last Interaction Time)
    private java.util.Map<java.util.UUID, Long> lastPlayerInteractionTime = new java.util.HashMap<>();
    // Map to track how many times a player has interacted recently (Player -> Count)
    private java.util.Map<java.util.UUID, Integer> playerInteractionCount = new java.util.HashMap<>();
    
    // Interaction
    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player pPlayer, net.minecraft.world.InteractionHand pHand) {
        long currentTime = this.level().getGameTime();
        java.util.UUID pid = pPlayer.getUUID();
        
        // Update interaction counts for "Annoyed" logic
        if (lastPlayerInteractionTime.containsKey(pid)) {
            long lastTime = lastPlayerInteractionTime.get(pid);
            if (currentTime - lastTime < 100) { // Interacted within 5 seconds
                playerInteractionCount.put(pid, playerInteractionCount.getOrDefault(pid, 0) + 1);
            } else {
                // Reset count if interaction was long ago
                playerInteractionCount.put(pid, 1);
            }
        } else {
            playerInteractionCount.put(pid, 1);
        }
        lastPlayerInteractionTime.put(pid, currentTime);

        ItemStack itemstack = pPlayer.getItemInHand(pHand);

        // Check for Annoyed (Spam Click)
        if (playerInteractionCount.get(pid) > 5) {
             // Too frequent!
             if (!this.level().isClientSide) {
                 // Reset count partially so she doesn't spam this line either
                 playerInteractionCount.put(pid, 0); 
                 
                 String[] annoyedLines = {
                     "entity.typemoonworld.ryougi_shiki.speech.annoyed_1",
                     "entity.typemoonworld.ryougi_shiki.speech.annoyed_2",
                     "entity.typemoonworld.ryougi_shiki.speech.annoyed_3"
                 };
                 int idx = this.random.nextInt(annoyedLines.length);
                 // Bypass global cooldown for annoyed response? Or enforce it? 
                 // Enforcing it is better to prevent chat spam.
                 broadcastToNearbyPlayers(annoyedLines[idx], 20.0);
             }
             return net.minecraft.world.InteractionResult.FAIL; // Stop interaction
        }

        // Special "Lost Arm" Interaction (Right Click)
        if (!this.hasLeftArm()) {
            // Check for prosthetic replacement attempt (e.g. Iron Block or specific item)
            // Or just special interaction lines
            
            // Interaction with Iron Block in hand (Attempt to fix?)
            if (itemstack.is(net.minecraft.world.item.Items.IRON_BLOCK)) {
                if (!this.level().isClientSide) {
                     broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm_fix_fail", 20.0);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            
            // Interaction with Clock (Time/Memory)
            if (itemstack.is(net.minecraft.world.item.Items.CLOCK)) {
                 if (!this.level().isClientSide) {
                     broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm_time", 20.0);
                 }
                 return net.minecraft.world.InteractionResult.SUCCESS;
            }

             if (this.random.nextFloat() < 0.3F) { // Increased chance (30%)
                 String[] lostArmLines = {
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_1",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_2",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_3",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_4",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_5",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_6",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_7",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_8",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_9",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_10",
                     // New lines
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_1",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_2",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_3",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_4",
                     "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_5"
                 };
                 if (!this.level().isClientSide) {
                     int idx = this.random.nextInt(lostArmLines.length);
                     broadcastToNearbyPlayers(lostArmLines[idx], 20.0);
                 }
             }
        }

        if (itemstack.is(net.minecraft.world.item.Items.SWEET_BERRIES)) {
            // Check betrayal
            if (this.entityData.get(IS_BETRAYED)) {
                 // Useless if betrayed
                 if (!this.level().isClientSide) {
                     pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.betrayed_ignore"), false);
                 }
                 return net.minecraft.world.InteractionResult.FAIL;
            }

            if (this.getHealth() < this.getMaxHealth()) {
                this.heal(5.0F);
                itemstack.shrink(1);
                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                
                // Increase Friendship
                int currentFriendship = this.entityData.get(FRIENDSHIP_LEVEL);
                this.entityData.set(FRIENDSHIP_LEVEL, currentFriendship + 1);

                // Remove aggro if targeted
                if (this.getTarget() == pPlayer) {
                    this.setTarget(null);
                }
                
                if (!this.level().isClientSide) {
                     // Different dialogue based on friendship
                     if (currentFriendship + 1 >= 5) {
                         pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.feed_high_trust"), false);
                     } else {
                         pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.feed"), false);
                     }
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
                // Already full health interaction
                if (!this.level().isClientSide) {
                    pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.full_health"), false);
                }
                return net.minecraft.world.InteractionResult.CONSUME; // Just consume action, don't eat
            }
        }
        
        // Reaction to specific items in hand (Lore)
        // Force look at player when talking
        this.getLookControl().setLookAt(pPlayer, 30.0F, 30.0F);

        // Water Bucket (Affinity)
        if (itemstack.is(net.minecraft.world.item.Items.WATER_BUCKET)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_water", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }
        
        // Map (Lost)
        if (itemstack.is(net.minecraft.world.item.Items.MAP) || itemstack.is(net.minecraft.world.item.Items.FILLED_MAP)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_map", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 1. Apple (Forbidden Fruit Reference)
        if (itemstack.is(net.minecraft.world.item.Items.APPLE)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_apple", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 2. Rabbit Foot (Reference to 2010 movie? Or general luck)
        if (itemstack.is(net.minecraft.world.item.Items.RABBIT_FOOT)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_rabbit_foot", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 3. Compass (Direction)
        if (itemstack.is(net.minecraft.world.item.Items.COMPASS)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_compass", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 4. Glass Bottle (Empty)
        if (itemstack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_glass_bottle", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 5. Flower (Poppy or Red Tulip) - Aesthetic
        if (itemstack.is(net.minecraft.world.item.Items.POPPY) || itemstack.is(net.minecraft.world.item.Items.RED_TULIP)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_flower", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 6. Skeleton Skull (Death)
        if (itemstack.is(net.minecraft.world.item.Items.SKELETON_SKULL)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_skull", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 7. Ice (Cold personality)
        if (itemstack.is(net.minecraft.world.item.Items.ICE) || itemstack.is(net.minecraft.world.item.Items.PACKED_ICE)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_ice", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        // 8. Bed (Sleep/Dream)
        if (itemstack.getItem() instanceof net.minecraft.world.item.BedItem) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_bed", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }
        
        // 9. Book (Reading)
        if (itemstack.is(net.minecraft.world.item.Items.BOOK) || itemstack.is(net.minecraft.world.item.Items.WRITABLE_BOOK)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_book", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }
        
        // 10. Shear (Cutting)
        if (itemstack.is(net.minecraft.world.item.Items.SHEARS)) {
             if (!this.level().isClientSide) {
                 broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_shears", 20.0);
             }
             return net.minecraft.world.InteractionResult.SUCCESS;
        }

        return super.mobInteract(pPlayer, pHand);
    }

    private boolean isBesieged() {
        List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D), 
            e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive());
            
        long strongCount = enemies.stream().filter(e -> e.getMaxHealth() > 100).count();
        
        return enemies.size() > 5 || strongCount > 3;
    }

    private void startPanicState() {
        if (!this.isPanicState) {
            this.isPanicState = true;
            this.forcedSlashTimer = 0;
            this.playSound(SoundEvents.WARDEN_ROAR, 1.0F, 2.0F); // Panic sound
            broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.panic", 20.0);
        }
    }

    private void performPanicSlash() {
        if (this.level().isClientSide) return;
        
        // Slash every 0.2s (4 ticks)
        if (this.tickCount % 4 != 0) return;

        BlockPos center = this.blockPosition();
        // 3x3x3 Area
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // Skip feet block (0, -1, 0) relative to center
                    // center is usually at feet.
                    if (x == 0 && y == -1 && z == 0) continue;
                    
                    BlockPos p = center.offset(x, y, z);
                    BlockState state = this.level().getBlockState(p);
                    // Destroy if not air and destroyable
                    // Replace fluids with AIR
                    if (!state.isAir() && (state.getDestroySpeed(this.level(), p) >= 0 || state.getFluidState().isSource())) {
                         this.level().setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        
        // Damage Entities (Continuous, low damage per tick, relies on invulnerability frames to throttle)
        AABB box = this.getBoundingBox().inflate(2.5); 
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive());
        for (LivingEntity e : targets) {
            // Absolute Safety Check
            if (e instanceof Player p && this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)) continue;
            
            if (e == this.getTarget()) {
                 // Mystic Eyes Effect (Reduced Chance)
                 // Normal chance is 20/HP or 1%. Here we reduce it further.
                 // Let's say 20% of normal chance.
                 if (this.random.nextFloat() < 0.2F) {
                     this.handleMysticEyesEffect(e);
                 }
                 e.hurt(this.damageSources().mobAttack(this), 6.0F);
            }
        }
        
        // Visuals (Throttled)
        ((net.minecraft.server.level.ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, 
             this.getX(), this.getY() + 1.0, this.getZ(), 3, 1.0, 1.0, 1.0, 0.0);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);
    }

    private void checkDangerousBlocks() {
        if (this.level().isClientSide || this.tickCount % 5 != 0) return; // Check every 5 ticks

        Vec3 velocity = this.getDeltaMovement();
        Vec3 look = this.getLookAngle();
        // Use velocity if moving significantly, otherwise look direction
        Vec3 dir = velocity.lengthSqr() > 0.01 ? velocity.normalize() : look;

        BlockPos center = this.blockPosition();
        boolean dangerFound = false;

        // Check 2 blocks ahead and 1 block down/up
        for (int i = 1; i <= 2; i++) {
            double tx = center.getX() + dir.x * i;
            double tz = center.getZ() + dir.z * i;
            BlockPos target = BlockPos.containing(tx, center.getY(), tz);
            
            if (isDangerous(this.level().getBlockState(target)) || 
                isDangerous(this.level().getBlockState(target.below())) ||
                isDangerous(this.level().getBlockState(target.above()))) {
                dangerFound = true;
                break;
            }
        }
        
        // Also check current position (in case standing in it)
        if (!dangerFound) {
            if (isDangerous(this.level().getBlockState(center)) || isDangerous(this.level().getBlockState(center.below()))) {
                dangerFound = true;
            }
        }

        if (dangerFound) {
             // Proactive Slash
             // Force it even if cooldown is active (Survival priority)
             this.performSlash(true);
             this.slashCooldown = 20; // Set short cooldown
             
             // Stop movement momentarily to avoid walking into it while slashing?
             this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        }
    }

    private boolean isDangerous(BlockState state) {
        if (state.isAir()) return false;
        if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return true;
        
        if (state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) || 
            state.is(net.minecraft.world.level.block.Blocks.CACTUS) || 
            state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || 
            state.is(net.minecraft.world.level.block.Blocks.FIRE) || 
            state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE) || 
            state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE) || 
            state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE) || 
            state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE) ||
            state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
            return true;
        }
        return false;
    }

    private boolean isNoblePhantasmAttack(DamageSource source) {
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        
        if (direct instanceof BrokenPhantasmProjectileEntity || direct instanceof SwordBarrelProjectileEntity) {
            return true;
        }
        
        if (attacker instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            if (mainHand.getItem() instanceof NoblePhantasmItem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, event -> {
            // Logic:
            // 1: Has Arm + Has Sword
            // 2: Has Arm + No Sword
            // 3: No Arm + Has Sword
            // 4: No Arm + No Sword
            
            String animationName = "1"; // Default
            if (this.hasLeftArm() && this.hasSword()) {
                animationName = "1";
            } else if (this.hasLeftArm() && !this.hasSword()) {
                animationName = "2";
            } else if (!this.hasLeftArm() && this.hasSword()) {
                animationName = "3";
            } else if (!this.hasLeftArm() && !this.hasSword()) {
                animationName = "4";
            }
            
            // Play the state animation (which defines the body pose/scale/bones)
            return event.setAndContinue(RawAnimation.begin().thenLoop(animationName));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
