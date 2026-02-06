package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;

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

    public RyougiShikiEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
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
        
        // Flee/Kiting goal when Guerrilla mode is active OR when weapon is lost
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, LivingEntity.class, 
            16.0F, 1.8D, 2.0D, // Increased speed for retreating (Base 0.25 * 1.8 = 0.45 / 2.0 = 0.5)
            (entity) -> entity == RyougiShikiEntity.this.getTarget() && (RyougiShikiEntity.this.isGuerrilla || !RyougiShikiEntity.this.hasSword())
        ));

        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Attack monsters by default (Active aggression)
        this.attackMonstersGoal = new NearestAttackableTargetGoal<>(this, Monster.class, true);
        this.targetSelector.addGoal(2, this.attackMonstersGoal); 
        
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
            // Check random chance to use variant
            if (this.random.nextBoolean()) {
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

    @Override
    public void tick() {
        super.tick();
        
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

        if (!this.level().isClientSide) {
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

            // Water Aversion Logic
            if (this.isInWater()) {
                this.jumpControl.jump();
                this.setDeltaMovement(this.getDeltaMovement().add(
                    (this.random.nextDouble() - 0.5) * 0.5, 
                    0.2, 
                    (this.random.nextDouble() - 0.5) * 0.5
                ));
                // Speak occasionally
                if (this.random.nextFloat() < 0.05F) { // 5% chance per tick
                    broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hate_water", 15.0);
                }
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
            if (this.getHealth() <= 20.0F && this.hasLeftArm()) {
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
                     // 5% chance per tick if conditions met (and far enough)
                     if (this.random.nextFloat() < 0.05F) {
                         this.performKnifeThrow(target);
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
                        if (this.level().getBlockState(tpPos).isAir() || !this.level().getBlockState(tpPos).blocksMotion()) {
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
                    if (this.level().getBlockState(tpPos).isAir() || !this.level().getBlockState(tpPos).blocksMotion()) {
                        this.teleportTo(tx, ty, tz);
                        this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
                    }
                }

                if (this.slashCooldown == 0 && canUseSkills) { // Only slash if not retreating
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
    }

    private void performKnifeThrow(LivingEntity target) {
        if (!this.level().isClientSide) {
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
                        // Apply Mystic Eyes Effect
                        RyougiShikiEntity.this.handleMysticEyesEffect(living, true); // Boosted effect
                        living.hurt(RyougiShikiEntity.this.damageSources().thrown(this, RyougiShikiEntity.this), 6.0F);
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
            this.swordRetrievalTimer = 100; // 5 seconds
            this.knifeThrowCooldown = 200; // 10 seconds cooldown for skill itself
        }
    }

    private void performSlash() {
        if (this.level().isClientSide) return;
        if (!this.hasSword()) return; // Cannot slash without sword
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
                if (!state.isAir() && state.getDestroySpeed(this.level(), pos) >= 0) {
                    this.level().destroyBlock(pos, false, this);
                }
                
                // Also clear 1 block up and down to ensure path
                BlockPos posUp = pos.above();
                if (!this.level().getBlockState(posUp).isAir() && this.level().getBlockState(posUp).getDestroySpeed(this.level(), posUp) >= 0) {
                     this.level().destroyBlock(posUp, false, this);
                }
            }
        }
        
        // Damage Entities
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D).move(lookVec.scale(1.5)), 
             e -> e != this && e.isAlive());
             
        for (LivingEntity e : targets) {
            // Safety Check: Do not harm non-hostile entities unless they are the target or targeting Shiki/Player
            // "Non-hostile" roughly means not a Monster and not targeting us.
            boolean isHostile = e instanceof Monster || e == this.getTarget();
            if (e instanceof net.minecraft.world.entity.Mob mob) {
                if (mob.getTarget() == this || mob.getTarget() instanceof Player) {
                    isHostile = true;
                }
            }
            // Also check for players (PvP safety) - Only attack if betrayed or if player is the target
            if (e instanceof Player) {
                isHostile = (this.entityData.get(IS_BETRAYED) || e == this.getTarget());
            }
            
            if (!isHostile) continue; // Skip friendly fire

            // Use Mystic Eyes effect logic (Force Kill or Heavy Damage)
            handleMysticEyesEffect(e);
            // Also apply a base damage to ensure aggro
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
    }
    
    @Override
    public void setHealth(float health) {
        if (!this.hasLeftArm() && health > 20.0F) {
            health = 20.0F;
        }
        super.setHealth(health);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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

        // Anti-Kill Command / Generic Kill
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
             // Only immune if it's not a legitimate void fall (y < -64)
             if (this.getY() > this.level().getMinBuildHeight() - 64) {
                 this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F); // Heavy sound
                 // Send message to server/players
                 if (!this.level().isClientSide) {
                      this.level().getServer().getPlayerList().broadcastSystemMessage(
                          Component.translatable("entity.typemoonworld.ryougi_shiki.speech.void_fall"), false
                      );
                 }
                 return false;
             }
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
                    if (this.random.nextFloat() < 0.5F) {
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

        return super.hurt(source, amount);
    }

    private void triggerDefense() {
        if (!this.level().isClientSide && !this.entityData.get(IS_DEFENDING)) {
            this.entityData.set(IS_DEFENDING, true);
            this.blockingTimer = 10; // Reduced to 0.5 seconds to avoid stun-lock
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
             this.level().getServer().getPlayerList().broadcastSystemMessage(
                 Component.translatable("entity.typemoonworld.ryougi_shiki.speech.refused"), false
             );
        }
        // super.kill(); // Intentionally removed
    }

    @Override
    public boolean doHurtTarget(Entity pEntity) {
        this.lastDamageTime = this.tickCount; // Update last damage time
        boolean flag = super.doHurtTarget(pEntity);
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
