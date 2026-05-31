package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NoblePhantasmItem;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RyougiShikiEntity extends PathfinderMob implements GeoEntity {
   private static final EntityDataAccessor<Boolean> IS_DEFENDING = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_BETRAYED = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> FRIENDSHIP_LEVEL = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> HAS_LEFT_ARM = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> HAS_SWORD = SynchedEntityData.defineId(RyougiShikiEntity.class, EntityDataSerializers.BOOLEAN);
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int blockingTimer = 0;
   private int slashCooldown = 0;
   private int ultimateCooldown = 0;
   private int swordRetrievalTimer = 0;
   private int knifeThrowCooldown = 0;
   private int lastDamageTime = 0;
   private NearestAttackableTargetGoal<Monster> attackMonstersGoal;
   private NearestAttackableTargetGoal<LivingEntity> attackPlayerEnemiesGoal;
   private NearestAttackableTargetGoal<LivingEntity> counterTargetGoal;
   private boolean isSerious = false;
   private boolean isGuerrilla = false;
   private int killCount = 0;
   private long lastKillTime = 0L;
   private int idleTimer = 0;
   private boolean isPanicState = false;
   private int forcedSlashTimer = 0;
   private boolean isWeakened = false;
   private int weaknessTimer = 0;
   private long lastSpeechTime = 0L;
   private Map<String, Long> messageCooldowns = new HashMap<>();
   private int escapeAttempts = 0;
   private long lastEscapeTime = 0L;
   private boolean isProcessingDamage = false;
   private Map<UUID, Long> lastPlayerInteractionTime = new HashMap<>();
   private Map<UUID, Integer> playerInteractionCount = new HashMap<>();

   public RyougiShikiEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
   }

   public int getFriendshipLevel() {
      return (Integer)this.entityData.get(FRIENDSHIP_LEVEL);
   }

   public void setFriendshipLevel(int value) {
      this.entityData.set(FRIENDSHIP_LEVEL, value);
   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      return false;
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.isOnFire()) {
            if (this.random.nextFloat() < 0.5F) {
               this.clearFire();
            } else {
               BlockPos waterPos = null;
               BlockPos center = this.blockPosition();

               for (BlockPos p : BlockPos.betweenClosed(center.offset(-10, -5, -10), center.offset(10, 5, 10))) {
                  if (this.level().getFluidState(p).is(FluidTags.WATER)) {
                     waterPos = p.immutable();
                     break;
                  }
               }

               if (waterPos != null) {
                  this.getNavigation().moveTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.5);
               }
            }
         }

         if (this.isPassenger() && (this.getTarget() != null || this.random.nextFloat() < 0.05F)) {
            this.stopRiding();
            this.playSound((SoundEvent)SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
         }

         if (this.isWeakened) {
            this.weaknessTimer--;
            if (this.weaknessTimer <= 0) {
               this.isWeakened = false;
            }
         }

         if (this.isPanicState) {
            this.forcedSlashTimer++;
            this.performPanicSlash();
            boolean safeFromLava = !this.isInLava();
            boolean safeFromSiege = !this.isBesieged();
            if (safeFromLava && safeFromSiege || this.forcedSlashTimer > 100) {
               this.isPanicState = false;
               this.isWeakened = true;
               this.weaknessTimer = 20;
               this.forcedSlashTimer = 0;
            }

            return;
         }

         if (this.getDeltaMovement().lengthSqr() < 0.001 && this.getTarget() == null) {
            this.idleTimer++;
            if (this.idleTimer > 600 && this.random.nextFloat() < 0.005F) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.idle", 20.0);
               this.idleTimer = 0;
            }
         } else {
            this.idleTimer = 0;
         }

         if (this.tickCount % 600 == 0 && this.getHealth() < this.getMaxHealth() * 0.2F) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.low_health", 20.0, 0.3F);
         }

         if (this.tickCount % 600 == 0 && this.random.nextFloat() < 0.1F) {
            if (this.level().isThundering()) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_thunder", 20.0);
            } else if (this.level().isNight() && this.level().canSeeSky(this.blockPosition())) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_night", 20.0);
            } else if (this.level().dimension() == Level.NETHER) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_nether", 20.0);
            } else if (this.level().dimension() == Level.END) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.environment_the_end", 20.0);
            }
         }

         if (this.tickCount % 40 == 0) {
            for (Player player : this.level().players()) {
               if (player.distanceToSqr(this) < 100.0 && this.isLookingAt(player, this)) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.player_looking", 20.0, 0.1F);
                  break;
               }
            }
         }

         if (this.tickCount % 20 == 0) {
            Player px = this.level().getNearestPlayer(this, 8.0);
            if (px != null && !(Boolean)this.entityData.get(IS_BETRAYED) && this.hasLineOfSight(px) && this.getTarget() == null) {
               ItemStack heldItem = px.getMainHandItem();
               if (heldItem.getItem() instanceof SwordItem) {
                  if (this.random.nextFloat() < 0.05F) {
                     this.getLookControl().setLookAt(px, 30.0F, 30.0F);
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_sword", 20.0);
                  }
               } else if (heldItem.getItem() instanceof ShieldItem) {
                  if (this.random.nextFloat() < 0.05F) {
                     this.getLookControl().setLookAt(px, 30.0F, 30.0F);
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_shield", 20.0);
                  }
               } else if (heldItem.is(Items.CLOCK)) {
                  if (this.random.nextFloat() < 0.05F) {
                     this.getLookControl().setLookAt(px, 30.0F, 30.0F);
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_clock", 20.0);
                  }
               } else if ((heldItem.is(Items.MILK_BUCKET) || heldItem.is(Items.STRING)) && this.random.nextFloat() < 0.1F) {
                  this.getLookControl().setLookAt(px, 30.0F, 30.0F);
                  String key = heldItem.is(Items.MILK_BUCKET)
                     ? "entity.typemoonworld.ryougi_shiki.speech.see_milk"
                     : "entity.typemoonworld.ryougi_shiki.speech.see_string";
                  this.broadcastToNearbyPlayers(key, 20.0);
               }
            }
         }

         this.checkDangerousBlocks();
         if (this.isInWater() || this.isInLava()) {
            boolean safe = false;

            for (int i = 0; i < 3; i++) {
               double angle = this.random.nextDouble() * Math.PI * 2.0;
               double tx = this.getX() + 2.0 * Math.cos(angle);
               double tz = this.getZ() + 2.0 * Math.sin(angle);
               double ty = this.getY();
               BlockPos targetPos = BlockPos.containing(tx, ty, tz);
               BlockState below = this.level().getBlockState(targetPos.below());
               BlockState at = this.level().getBlockState(targetPos);
               if (below.isFaceSturdy(this.level(), targetPos.below(), Direction.UP)
                  && !at.getFluidState().isSource()
                  && !at.isSuffocating(this.level(), targetPos)) {
                  this.teleportTo(tx, ty, tz);
                  safe = true;
                  break;
               }
            }

            if (!safe) {
               if (this.slashCooldown == 0) {
                  this.performSlash();
                  this.slashCooldown = 20;
               }

               if (this.isInLava()) {
                  this.startPanicState();
               }
            }

            if (this.random.nextFloat() < 0.05F) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hate_water", 15.0);
            }
         }

         if (!this.isWeakened && this.isBesieged() && this.tickCount % 20 == 0) {
            this.startPanicState();
         }

         if (this.blockingTimer > 0) {
            this.blockingTimer--;
            if (this.blockingTimer == 0) {
               this.entityData.set(IS_DEFENDING, false);
            }
         }

         if (this.level().isRainingAt(this.blockPosition()) && this.random.nextFloat() < 0.001F) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.rain", 20.0);
         }

         if (this.level().isNight() && !this.level().isRaining() && this.level().canSeeSky(this.blockPosition()) && this.random.nextFloat() < 5.0E-4F) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.moon", 20.0);
         }

         if (this.level().getBiome(this.blockPosition()).is(Biomes.CHERRY_GROVE) && this.random.nextFloat() < 0.002F) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.cherry_blossom", 20.0);
         }

         if (this.tickCount % 100 == 0) {
            Player px = this.level().getNearestPlayer(this, 10.0);
            if (px != null
               && px.getHealth() < px.getMaxHealth() * 0.3F
               && !(Boolean)this.entityData.get(IS_BETRAYED)
               && (Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 3) {
               px.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.player_low_health"), false);
            }

            if (px != null && px.hasEffect(MobEffects.INVISIBILITY) && this.random.nextFloat() < 0.3F) {
               px.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.player_invisible"), false);
            }
         }

         if (this.getHealth() < this.getMaxHealth() * 0.5F) {
            if (!this.hasEffect(MobEffects.MOVEMENT_SPEED)) {
               this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
            }

            if (!this.hasEffect(MobEffects.DAMAGE_BOOST)) {
               this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
            }
         }

         if (this.slashCooldown > 0) {
            this.slashCooldown--;
         }

         if (this.ultimateCooldown > 0) {
            this.ultimateCooldown--;
         }

         if (this.knifeThrowCooldown > 0) {
            this.knifeThrowCooldown--;
         }

         if (!this.hasSword()) {
            if (this.swordRetrievalTimer > 0) {
               this.swordRetrievalTimer--;
            } else {
               this.entityData.set(HAS_SWORD, true);
               this.playSound((SoundEvent)SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.retrieve_knife", 20.0);
            }
         }

         if (this.getHealth() <= 20.0F && this.getHealth() > 0.0F && this.hasLeftArm()) {
            this.entityData.set(HAS_LEFT_ARM, false);
            this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.5F);
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm", 30.0);
         }

         LivingEntity target = this.getTarget();
         if (target != null) {
            double distSqr = this.distanceToSqr(target);
            double dyThrow = target.getY() - this.getY();
            boolean isTargetInAir = dyThrow > 2.0 || !target.onGround();
            if (distSqr > 25.0 && this.hasSword() && this.knifeThrowCooldown == 0 && isTargetInAir) {
               long flyingEnemiesCount = this.level()
                  .getEntitiesOfClass(
                     LivingEntity.class,
                     this.getBoundingBox().inflate(20.0),
                     e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive() && (!e.onGround() || e.getY() > this.getY() + 2.0)
                  )
                  .size();
               if (flyingEnemiesCount >= 2L) {
                  if (this.random.nextFloat() < 0.2F) {
                     this.performMultiKnifeThrow(target, 3);
                  }
               } else if (this.random.nextFloat() < 0.05F) {
                  this.performKnifeThrow(target);
               }
            }

            double dy = target.getY() - this.getY();
            double distSqrToTarget = this.distanceToSqr(target);
            if (dy > 3.0 && dy < 8.0 && distSqrToTarget < 25.0 && this.onGround() && this.random.nextFloat() < 0.2F) {
               this.setDeltaMovement(this.getDeltaMovement().add(0.0, 1.2, 0.0));
               this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F);
               Vec3 vec = target.position().subtract(this.position()).normalize().scale(0.5);
               this.setDeltaMovement(this.getDeltaMovement().add(vec.x, 0.0, vec.z));
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.jump_attack", 20.0, 1.0F);
               if (this.slashCooldown == 0) {
                  this.performSlash();
                  this.slashCooldown = 40;
               }
            }

            boolean canUseSkills = !this.isGuerrilla;
            if (distSqrToTarget > 64.0 && this.random.nextFloat() < 0.05F) {
               if (canUseSkills) {
                  Vec3 look = this.getLookAngle();
                  double teleportDist = 2.0;
                  double tx = this.getX() + look.x * teleportDist;
                  double ty = this.getY();
                  double tz = this.getZ() + look.z * teleportDist;
                  BlockPos tpPos = BlockPos.containing(tx, ty, tz);
                  if (this.level().getBlockState(tpPos).isAir() || this.level().getBlockState(tpPos).getCollisionShape(this.level(), tpPos).isEmpty()) {
                     this.teleportTo(tx, ty, tz);
                     this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.dodge", 10.0, 0.1F);
                  }
               }
            } else if (this.isGuerrilla && this.random.nextFloat() < 0.1F) {
               Vec3 look = this.getLookAngle().reverse();
               double teleportDist = 2.0;
               double tx = this.getX() + look.x * teleportDist;
               double ty = this.getY();
               double tz = this.getZ() + look.z * teleportDist;
               BlockPos tpPos = BlockPos.containing(tx, ty, tz);
               if (this.level().getBlockState(tpPos).isAir() || this.level().getBlockState(tpPos).getCollisionShape(this.level(), tpPos).isEmpty()) {
                  this.teleportTo(tx, ty, tz);
                  this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
               }
            }

            if (this.slashCooldown == 0 && canUseSkills && (!isTargetInAir || !(this.random.nextFloat() > 0.2F))) {
               boolean shouldSlash = false;
               List<LivingEntity> enemies = this.level()
                  .getEntitiesOfClass(
                     LivingEntity.class, this.getBoundingBox().inflate(4.0), e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive()
                  );
               if (enemies.size() > 3) {
                  shouldSlash = true;
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.crowded", 10.0, 0.1F);
                  }
               }

               if (!shouldSlash && this.getNavigation().isStuck()) {
                  shouldSlash = true;
               }

               if (shouldSlash) {
                  this.performSlash();
                  this.slashCooldown = 60;
               }
            }

            List<LivingEntity> toughEnemies = this.level()
               .getEntitiesOfClass(
                  LivingEntity.class, this.getBoundingBox().inflate(10.0), e -> e != this && e instanceof Monster && e.getMaxHealth() > 100.0F && e.isAlive()
               );
            if (toughEnemies.size() >= 2) {
               if (this.tickCount % 100 < 40) {
                  if (!this.isGuerrilla) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.guerrilla_retreat", 15.0);
                  }

                  this.isGuerrilla = true;
               } else {
                  this.isGuerrilla = false;
               }
            } else {
               this.isGuerrilla = false;
            }
         }

         float healthPct = this.getHealth() / this.getMaxHealth();
         Player playerx = this.level().getNearestPlayer(this, 10.0);
         if (playerx != null && (playerx.getMainHandItem().is(Items.SWEET_BERRIES) || playerx.getOffhandItem().is(Items.SWEET_BERRIES))) {
            boolean betrayed = (Boolean)this.entityData.get(IS_BETRAYED);
            if (!betrayed) {
               if (this.getTarget() == playerx) {
                  this.setTarget(null);
               }

               this.getLookControl().setLookAt(playerx, 30.0F, 30.0F);
               if (this.getTarget() == null) {
                  this.getNavigation().stop();
               }
            }
         }

         if (healthPct < 0.8F && !this.isSerious) {
            this.isSerious = true;
            this.playSound(SoundEvents.ITEM_BREAK, 1.0F, 0.5F);
         } else if (healthPct >= 0.8F && this.isSerious) {
            this.isSerious = false;
         }

         if ((Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !(Boolean)this.entityData.get(IS_BETRAYED)) {
            this.targetSelector.addGoal(3, this.attackPlayerEnemiesGoal);
         } else {
            this.targetSelector.removeGoal(this.attackPlayerEnemiesGoal);
         }
      }

      if (this.level().isClientSide && (this.isSprinting() || this.isGuerrilla)) {
         this.level()
            .addParticle(
               ParticleTypes.CLOUD,
               this.getX() + (this.random.nextDouble() - 0.5),
               this.getY() + 0.1,
               this.getZ() + (this.random.nextDouble() - 0.5),
               0.0,
               0.0,
               0.0
            );
      }
   }

   private boolean isLookingAt(LivingEntity viewer, LivingEntity target) {
      Vec3 viewVec = viewer.getViewVector(1.0F).normalize();
      Vec3 dirToTarget = target.getEyePosition().subtract(viewer.getEyePosition()).normalize();
      return viewVec.dot(dirToTarget) > 0.95;
   }

   public static Builder createAttributes() {
      return Monster.createMonsterAttributes()
         .add(Attributes.MAX_HEALTH, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.15)
         .add(Attributes.ATTACK_DAMAGE, 12.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
         .add(Attributes.FOLLOW_RANGE, 64.0);
   }

   public void setTarget(@Nullable LivingEntity target) {
      LivingEntity oldTarget = this.getTarget();
      super.setTarget(target);
      if (target != null && target != oldTarget && !this.level().isClientSide) {
         String speechKey = null;
         if (target.isInvisible()) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_invisible";
         } else if (target.getMaxHealth() >= 50.0F && !(target instanceof WitherBoss) && !(target instanceof EnderDragon) && this.random.nextFloat() < 0.2F) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.seeing_lines";
         } else if (target instanceof Zombie) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_zombie";
         } else if (target instanceof Skeleton || target instanceof Stray) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_skeleton";
         } else if (target instanceof Creeper) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_creeper";
         } else if (target instanceof EnderMan) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_enderman";
         } else if (target instanceof Spider || target instanceof CaveSpider) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_spider";
         } else if (target instanceof Witch || target instanceof Evoker) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_witch";
         } else if (target instanceof Phantom) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_phantom";
         } else if (target instanceof Vex || target instanceof Blaze) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_spirit";
         } else if (target instanceof Slime || target instanceof MagmaCube) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_slime";
         } else if (target instanceof WitherBoss) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_wither";
         } else if (target instanceof EnderDragon) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_ender_dragon";
         } else if (target instanceof IronGolem) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_iron_golem";
         } else if (target instanceof Villager) {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_villager";
         } else if (!(target instanceof Animal) && !(target instanceof TamableAnimal)) {
            List<Monster> nearbyMonsters = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(15.0));
            if (nearbyMonsters.size() > 5) {
               speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_horde";
            }
         } else {
            speechKey = "entity.typemoonworld.ryougi_shiki.speech.target_animal";
         }

         if (speechKey != null && this.random.nextFloat() < 0.3F) {
            this.broadcastToNearbyPlayers(speechKey, 20.0);
         }
      }
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(IS_DEFENDING, false);
      builder.define(IS_BETRAYED, false);
      builder.define(FRIENDSHIP_LEVEL, 0);
      builder.define(HAS_LEFT_ARM, true);
      builder.define(HAS_SWORD, true);
   }

   public boolean hasLeftArm() {
      return (Boolean)this.entityData.get(HAS_LEFT_ARM);
   }

   public boolean hasSword() {
      return (Boolean)this.entityData.get(HAS_SWORD);
   }

   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.6, false) {
         public boolean canUse() {
            return !RyougiShikiEntity.this.hasSword() ? false : super.canUse() && !RyougiShikiEntity.this.isGuerrilla;
         }

         public boolean canContinueToUse() {
            return !RyougiShikiEntity.this.hasSword() ? false : super.canContinueToUse();
         }
      });
      this.goalSelector
         .addGoal(
            1,
            new AvoidEntityGoal<>(
               this, LivingEntity.class, 16.0F, 1.8, 2.0, entity -> entity == this.getTarget() && (this.isGuerrilla || !this.hasSword() || this.isWeakened)
            )
         );
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
      this.attackMonstersGoal = new NearestAttackableTargetGoal<>(this, Monster.class, true);
      this.targetSelector
         .addGoal(
            2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> e instanceof Enemy && !(e instanceof RyougiShikiEntity))
         );
      this.attackPlayerEnemiesGoal = new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> {
         Player p = this.level().getNearestPlayer(this, 20.0);
         return p != null && !this.entityData.get(IS_BETRAYED) && entity instanceof Mob mob ? mob.getTarget() == p : false;
      });
      this.counterTargetGoal = new NearestAttackableTargetGoal<>(
         this, LivingEntity.class, 10, true, false, entity -> entity instanceof Mob mob ? mob.getTarget() == this : false
      );
      this.targetSelector.addGoal(3, this.counterTargetGoal);
      this.goalSelector
         .addGoal(
            0,
            new Goal() {
               private int step = 0;
               private int timer = 0;

               public boolean canUse() {
                  LivingEntity t = RyougiShikiEntity.this.getTarget();
                  if (!RyougiShikiEntity.this.hasSword()) {
                     return false;
                  } else if (RyougiShikiEntity.this.isWeakened) {
                     return false;
                  } else if (RyougiShikiEntity.this.ultimateCooldown <= 0 && t != null && t.isAlive()) {
                     boolean lowHp = RyougiShikiEntity.this.getHealth() < RyougiShikiEntity.this.getMaxHealth() * 0.6F;
                     boolean strongTarget = t instanceof Player || t instanceof Monster && t.getMaxHealth() > 50.0F;
                     boolean longTimeNoDamage = RyougiShikiEntity.this.tickCount - RyougiShikiEntity.this.lastDamageTime > 200;
                     boolean execution = t.getHealth() < t.getMaxHealth() * 0.3F && RyougiShikiEntity.this.random.nextFloat() < 0.1F;
                     float chance = strongTarget ? 0.05F : 0.01F;
                     return RyougiShikiEntity.this.random.nextFloat() > chance ? false : lowHp || strongTarget || longTimeNoDamage || execution;
                  } else {
                     return false;
                  }
               }

               public void start() {
                  this.step = 0;
                  this.timer = 0;
                  RyougiShikiEntity.this.ultimateCooldown = 600;
                  RyougiShikiEntity.this.playSound(SoundEvents.WITHER_SPAWN, 1.0F, 0.5F);
                  RyougiShikiEntity.this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.ultimate_start", 30.0);
               }

               public boolean canContinueToUse() {
                  return this.step < 3;
               }

               public void tick() {
                  LivingEntity t = RyougiShikiEntity.this.getTarget();
                  if (t == null) {
                     this.step = 3;
                  } else {
                     this.timer++;
                     if (this.step == 0) {
                        if (this.timer == 1) {
                           Vec3 back = RyougiShikiEntity.this.getLookAngle().reverse().scale(3.0);
                           RyougiShikiEntity.this.teleportTo(
                              RyougiShikiEntity.this.getX() + back.x, RyougiShikiEntity.this.getY(), RyougiShikiEntity.this.getZ() + back.z
                           );
                           RyougiShikiEntity.this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                        }

                        if (this.timer > 10) {
                           this.step = 1;
                           this.timer = 0;
                        }
                     } else if (this.step == 1) {
                        if (this.timer == 1) {
                           Vec3 dir = t.position().subtract(RyougiShikiEntity.this.position()).normalize().scale(2.0);
                           RyougiShikiEntity.this.setDeltaMovement(dir);
                           RyougiShikiEntity.this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.ultimate_execute", 30.0);
                        }

                        AABB box = RyougiShikiEntity.this.getBoundingBox().inflate(1.0);
                        BlockPos.betweenClosedStream(box).forEach(pos -> {
                           if (!(pos.getY() < RyougiShikiEntity.this.getY() - 0.1)) {
                              BlockState state = RyougiShikiEntity.this.level().getBlockState(pos);
                              if (!state.isAir() && state.getDestroySpeed(RyougiShikiEntity.this.level(), pos) >= 0.0F) {
                                 RyougiShikiEntity.this.level().destroyBlock(pos, false, RyougiShikiEntity.this);
                              }
                           }
                        });
                        if (this.timer > 10) {
                           this.step = 2;
                           this.timer = 0;
                        }
                     } else if (this.step == 2) {
                        if (this.timer == 1) {
                           RyougiShikiEntity.this.performSlash();
                           RyougiShikiEntity.this.handleMysticEyesEffect(t, true);
                           t.hurt(RyougiShikiEntity.this.damageSources().mobAttack(RyougiShikiEntity.this), 20.0F);
                        }

                        if (this.timer > 20) {
                           this.step = 3;
                           RyougiShikiEntity.this.ultimateCooldown = 200;
                        }
                     }
                  }
               }
            }
         );
   }

   private void broadcastToNearbyPlayers(String baseKey, double radius) {
      if (!this.level().isClientSide) {
         long currentTime = this.level().getGameTime();
         if (currentTime - this.lastSpeechTime < 40L) {
            return;
         }

         if (this.messageCooldowns.containsKey(baseKey)) {
            long lastTime = this.messageCooldowns.get(baseKey);
            if (currentTime - lastTime < 1200L) {
               return;
            }
         }

         this.lastSpeechTime = currentTime;
         this.messageCooldowns.put(baseKey, currentTime);
         String finalKey = baseKey;
         if (baseKey.equals("entity.typemoonworld.ryougi_shiki.speech.death") || baseKey.equals("entity.typemoonworld.ryougi_shiki.speech.lost_arm")) {
            int variant = this.random.nextInt(5);
            if (variant > 0) {
               finalKey = baseKey + "_v" + (variant + 1);
            }
         } else if (this.random.nextBoolean()) {
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
         this.broadcastToNearbyPlayers(translationKey, radius);
      }
   }

   private void broadcastToNearbyPlayers(Component message, double radius, float probability) {
      if (this.random.nextFloat() < probability) {
         if (message.getContents() instanceof TranslatableContents tc) {
            this.broadcastToNearbyPlayers(tc.getKey(), radius);
         } else if (!this.level().isClientSide) {
            for (Player p : this.level().players()) {
               if (p.distanceToSqr(this) <= radius * radius) {
                  p.displayClientMessage(message, false);
               }
            }
         }
      }
   }

   private void performMultiKnifeThrow(LivingEntity target, int count) {
      if (!this.level().isClientSide) {
         if (this.isWeakened) {
            return;
         }

         if (target != this.getTarget()) {
            return;
         }

         for (int i = 0; i < count; i++) {
            Snowball projectile = new Snowball(this.level(), this) {
               protected void onHitEntity(EntityHitResult pResult) {
                  if (pResult.getEntity() instanceof LivingEntity living) {
                     if (living instanceof Player p
                        && (Integer)RyougiShikiEntity.this.entityData.get(RyougiShikiEntity.FRIENDSHIP_LEVEL) >= 5
                        && !(Boolean)RyougiShikiEntity.this.entityData.get(RyougiShikiEntity.IS_BETRAYED)) {
                        return;
                     }

                     if (living == RyougiShikiEntity.this.getTarget()) {
                        RyougiShikiEntity.this.handleMysticEyesEffect(living, true);
                        living.hurt(RyougiShikiEntity.this.damageSources().thrown(this, RyougiShikiEntity.this), 6.0F);
                     }
                  }
               }

               protected void onHit(HitResult pResult) {
                  super.onHit(pResult);
                  if (!this.level().isClientSide) {
                     this.discard();
                  }
               }

               public void tick() {
                  super.tick();
                  if (this.level().isClientSide) {
                     this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                     this.level().addParticle(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                     this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                  }
               }
            };
            projectile.setInvisible(true);
            projectile.setItem(ItemStack.EMPTY);
            double d0 = target.getX() - this.getX();
            double d1 = target.getEyeY() - 0.3333333333333333 - projectile.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            float spread = i == 0 ? 0.0F : 10.0F;
            projectile.shoot(d0, d1 + d3 * 0.2, d2, 1.6F, 14 - this.level().getDifficulty().getId() * 4 + spread);
            this.level().addFreshEntity(projectile);
         }

         this.playSound((SoundEvent)SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
         this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.throw_knife", 20.0);
         this.entityData.set(HAS_SWORD, false);
         this.swordRetrievalTimer = 40;
         this.knifeThrowCooldown = 200;
      }
   }

   private void performKnifeThrow(LivingEntity target) {
      if (!this.level().isClientSide) {
         if (this.isWeakened) {
            return;
         }

         if (target != this.getTarget()) {
            return;
         }

         Snowball projectile = new Snowball(this.level(), this) {
            protected void onHitEntity(EntityHitResult pResult) {
               if (pResult.getEntity() instanceof LivingEntity living) {
                  if (living instanceof Player p
                     && (Integer)RyougiShikiEntity.this.entityData.get(RyougiShikiEntity.FRIENDSHIP_LEVEL) >= 5
                     && !(Boolean)RyougiShikiEntity.this.entityData.get(RyougiShikiEntity.IS_BETRAYED)) {
                     return;
                  }

                  if (living == RyougiShikiEntity.this.getTarget()) {
                     RyougiShikiEntity.this.handleMysticEyesEffect(living, true);
                     living.hurt(RyougiShikiEntity.this.damageSources().thrown(this, RyougiShikiEntity.this), 6.0F);
                  }
               }
            }

            protected void onHit(HitResult pResult) {
               super.onHit(pResult);
               if (!this.level().isClientSide) {
                  this.discard();
               }
            }

            public void tick() {
               super.tick();
               if (this.level().isClientSide) {
                  this.level().addParticle(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                  this.level().addParticle(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                  this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
               }
            }
         };
         projectile.setInvisible(true);
         projectile.setItem(ItemStack.EMPTY);
         double d0 = target.getX() - this.getX();
         double d1 = target.getEyeY() - 0.3333333333333333 - projectile.getY();
         double d2 = target.getZ() - this.getZ();
         double d3 = Math.sqrt(d0 * d0 + d2 * d2);
         projectile.shoot(d0, d1 + d3 * 0.2, d2, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
         this.level().addFreshEntity(projectile);
         this.playSound((SoundEvent)SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
         this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.throw_knife", 20.0);
         this.entityData.set(HAS_SWORD, false);
         this.swordRetrievalTimer = 20;
         this.knifeThrowCooldown = 200;
      }
   }

   private void performSlash() {
      this.performSlash(false);
   }

   private void performSlash(boolean force) {
      if (!this.level().isClientSide) {
         if (!this.isWeakened || force) {
            if (this.hasSword() || force) {
               Vec3 lookVec = this.getLookAngle();
               Vec3 center = this.position().add(0.0, this.getEyeHeight() * 0.5, 0.0);
               ((ServerLevel)this.level())
                  .sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + lookVec.x, center.y + lookVec.y, center.z + lookVec.z, 5, 0.5, 0.5, 0.5, 0.0);

               for (int i = 0; i < 5; i++) {
                  double d0 = this.random.nextGaussian() * 0.02;
                  double d1 = this.random.nextGaussian() * 0.02;
                  double d2 = this.random.nextGaussian() * 0.02;
                  ((ServerLevel)this.level())
                     .sendParticles(
                        ParticleTypes.CRIT,
                        center.x + lookVec.x * 1.5 + (this.random.nextDouble() - 0.5),
                        center.y + lookVec.y * 1.5 + (this.random.nextDouble() - 0.5),
                        center.z + lookVec.z * 1.5 + (this.random.nextDouble() - 0.5),
                        1,
                        d0,
                        d1,
                        d2,
                        0.0
                     );
               }

               this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);

               for (double r = 0.0; r <= 3.0; r++) {
                  for (double theta = -Math.PI / 2; theta <= Math.PI / 2; theta += Math.PI / 8) {
                     double x = lookVec.x * Math.cos(theta) - lookVec.z * Math.sin(theta);
                     double z = lookVec.x * Math.sin(theta) + lookVec.z * Math.cos(theta);
                     Vec3 offset = new Vec3(x, 0.0, z).normalize().scale(r);
                     BlockPos pos = BlockPos.containing(center.add(offset));
                     BlockState state = this.level().getBlockState(pos);
                     if (!state.isAir() && (state.getDestroySpeed(this.level(), pos) >= 0.0F || state.getFluidState().isSource())) {
                        this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                     }

                     BlockPos posUp = pos.above();
                     if (!this.level().getBlockState(posUp).isAir()
                        && (
                           this.level().getBlockState(posUp).getDestroySpeed(this.level(), posUp) >= 0.0F
                              || this.level().getBlockState(posUp).getFluidState().isSource()
                        )) {
                        this.level().setBlock(posUp, Blocks.AIR.defaultBlockState(), 3);
                     }
                  }
               }

               for (LivingEntity e : this.level()
                  .getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0).move(lookVec.scale(1.5)), ex -> ex != this && ex.isAlive())) {
                  if (!(e instanceof Player p && (Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !(Boolean)this.entityData.get(IS_BETRAYED))) {
                     boolean isTarget = e == this.getTarget();
                     if (isTarget) {
                        this.handleMysticEyesEffect(e);
                        e.hurt(this.damageSources().mobAttack(this), 5.0F);
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isDefending() {
      return (Boolean)this.entityData.get(IS_DEFENDING);
   }

   public void heal(float healAmount) {
      if (!this.hasLeftArm() && this.getHealth() + healAmount > 20.0F) {
         float allowed = 20.0F - this.getHealth();
         if (allowed > 0.0F) {
            super.heal(allowed);
         }
      } else {
         super.heal(healAmount);
         if (healAmount > 1.0F && !this.level().isClientSide && this.getHealth() < this.getMaxHealth()) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.healed", 20.0, 0.2F);
         }
      }
   }

   public void setHealth(float health) {
      if (!this.hasLeftArm() && health > 20.0F) {
         health = 20.0F;
      }

      if (health <= 0.0F && !this.isProcessingDamage && this.getHealth() > 0.0F) {
         health = 1.0F;
         if (!this.level().isClientSide) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_causality", 20.0);
         }
      }

      super.setHealth(health);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
         this.setTarget(attacker);
      }

      this.isProcessingDamage = true;

      try {
         if (amount > 1000.0F && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            amount = this.getMaxHealth() * 0.2F;
            if (!this.level().isClientSide) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_infinite", 20.0);
            }
         }

         if (this.isNoblePhantasmAttack(source)) {
            float chance = this.random.nextFloat();
            if (chance < 0.5F) {
               this.triggerDefense();
               this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
               return false;
            } else if (!(chance < 0.9F)) {
               return super.hurt(source, amount);
            } else {
               float currentHealth = this.getHealth();
               float halfHealth = this.getMaxHealth() / 2.0F;
               if (currentHealth > halfHealth) {
                  amount = currentHealth - halfHealth;
               }

               this.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hurt_noble_phantasm", 20.0);
               }

               return super.hurt(source, amount);
            }
         } else {
            if (this.isGuerrilla && source.getEntity() instanceof LivingEntity attacker) {
               this.isGuerrilla = false;
               this.setTarget(attacker);
               if (this.random.nextBoolean() && this.slashCooldown == 0) {
                  this.performSlash();
                  this.slashCooldown = 40;
               }
            }

            if (!this.level().isClientSide) {
               boolean isTrap = source.is(DamageTypeTags.IS_FIRE)
                  || source.is(DamageTypes.LAVA)
                  || source.is(DamageTypes.IN_WALL)
                  || source.is(DamageTypes.CRAMMING)
                  || source.is(DamageTypes.HOT_FLOOR)
                  || source.is(DamageTypes.SWEET_BERRY_BUSH)
                  || source.is(DamageTypes.CACTUS)
                  || source.is(DamageTypes.DROWN)
                  || source.is(DamageTypes.FREEZE)
                  || source.is(DamageTypes.WITHER)
                  || source.getEntity() == null
                     && source.getDirectEntity() == null
                     && !source.is(DamageTypes.GENERIC_KILL)
                     && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
               if (isTrap) {
                  long currentTime = this.level().getGameTime();
                  if (currentTime - this.lastEscapeTime > 100L) {
                     this.escapeAttempts = 0;
                  }

                  if (this.escapeAttempts < 3) {
                     this.escapeAttempts++;
                     this.lastEscapeTime = currentTime;
                     String[] escapeLines = new String[]{
                        "entity.typemoonworld.ryougi_shiki.speech.escape_trap",
                        "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_1",
                        "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_2",
                        "entity.typemoonworld.ryougi_shiki.speech.unknown_danger_3"
                     };
                     this.broadcastToNearbyPlayers(escapeLines[this.random.nextInt(escapeLines.length)], 20.0);
                     BlockPos center = this.blockPosition();

                     for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                           for (int z = -1; z <= 1; z++) {
                              BlockPos p = center.offset(x, y, z);
                              BlockState state = this.level().getBlockState(p);
                              if (!state.isAir() && state.getDestroySpeed(this.level(), p) >= 0.0F) {
                                 this.level().destroyBlock(p, false, this);
                              }
                           }
                        }
                     }

                     AABB box = this.getBoundingBox().inflate(1.5);

                     for (LivingEntity e : this.level()
                        .getEntitiesOfClass(LivingEntity.class, box, ex -> ex != this && ex.isAlive() && !(ex instanceof Player px && px.isCreative()))) {
                        this.handleMysticEyesEffect(e, true);
                     }

                     ((ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 5, 1.0, 1.0, 1.0, 0.0);
                     this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.5F);
                     BlockPos bestPos = null;
                     BlockPos[] candidates = new BlockPos[]{
                        center.above(2),
                        center.north(2),
                        center.south(2),
                        center.east(2),
                        center.west(2),
                        center.north(2).above(),
                        center.south(2).above(),
                        center.east(2).above(),
                        center.west(2).above()
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
                        this.teleportTo(this.getX(), this.getY() + 2.0, this.getZ());
                     }

                     this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
                     return false;
                  }
               }
            }

            if ((source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) && this.getY() > this.level().getMinBuildHeight() - 64) {
               this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.resist_void", 20.0);
               }

               return false;
            } else if ((source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER))
               && this.random.nextFloat() < 0.8F) {
               this.removeAllEffects();
               this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
               if (!this.level().isClientSide) {
                  ((ServerLevel)this.level()).sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
               }

               return false;
            } else if (source.is(DamageTypes.LIGHTNING_BOLT)) {
               return false;
            } else {
               Entity directEntity = source.getDirectEntity();
               if (directEntity instanceof Projectile) {
                  if (this.random.nextFloat() < 0.9F) {
                     this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F);
                     this.triggerDefense();
                     directEntity.discard();
                     return false;
                  }
               } else if (directEntity instanceof LivingEntity attackerx && source.getDirectEntity() == source.getEntity()) {
                  float chance = this.random.nextFloat();
                  float immunityReduction = this.hasLeftArm() ? 0.0F : 0.2F;
                  if (chance < 0.8F - immunityReduction) {
                     this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
                     this.triggerDefense();
                     if (amount >= this.getHealth()) {
                        this.isGuerrilla = true;
                        if (this.slashCooldown == 0) {
                           this.performSlash();
                           this.slashCooldown = 60;
                        }
                     } else if (this.distanceToSqr(attackerx) <= 16.0 && this.random.nextFloat() < 0.5F) {
                        this.swing(InteractionHand.MAIN_HAND);
                        if (this.hasSword() && this.slashCooldown == 0) {
                           this.performSlash();
                           this.slashCooldown = 60;
                        } else {
                           this.doHurtTarget(attackerx);
                           if (!this.hasSword()) {
                           }
                        }
                     }

                     return false;
                  }

                  float dodgeLimit = this.isSerious ? 0.6F : 0.9F;
                  if (chance < dodgeLimit) {
                     this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 2.0F);
                     this.triggerDefense();
                     return false;
                  }
               } else if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && this.random.nextFloat() < 0.95F) {
                  this.triggerDefense();
                  Entity direct = source.getDirectEntity();
                  if (direct != null && !(direct instanceof LivingEntity)) {
                     direct.discard();
                     if (!this.level().isClientSide) {
                        ((ServerLevel)this.level())
                           .sendParticles(ParticleTypes.WITCH, direct.getX(), direct.getY() + 0.5, direct.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                     }
                  }

                  this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
                  return false;
               }

               boolean result = super.hurt(source, amount);
               if (result && amount > 8.0F && !this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.hurt_high", 20.0, 0.5F);
               }

               return result;
            }
         }
      } finally {
         this.isProcessingDamage = false;
      }
   }

   private void triggerDefense() {
      if (!this.level().isClientSide && !(Boolean)this.entityData.get(IS_DEFENDING)) {
         if (this.isOnFire()) {
            return;
         }

         this.entityData.set(IS_DEFENDING, true);
         this.blockingTimer = 10;
      }
   }

   public void die(DamageSource cause) {
      super.die(cause);
      if (!this.level().isClientSide) {
         this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.death", 30.0);
      }
   }

   public void kill() {
      if (!this.level().isClientSide && this.isAlive()) {
         this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
         if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.refused"), false);
         }
      }
   }

   public boolean canAttack(LivingEntity target) {
      return target instanceof Player player && this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !this.entityData.get(IS_BETRAYED)
         ? false
         : super.canAttack(target);
   }

   public boolean doHurtTarget(Entity pEntity) {
      if (pEntity instanceof Player player && (Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !(Boolean)this.entityData.get(IS_BETRAYED)) {
         return false;
      } else {
         this.lastDamageTime = this.tickCount;
         boolean flag = super.doHurtTarget(pEntity);
         if (!flag && pEntity instanceof LivingEntity living && living.isBlocking() && !this.level().isClientSide) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.attack_blocked", 20.0, 0.5F);
         }

         if (pEntity instanceof LivingEntity target && this.hasSword()) {
            target.removeAllEffects();
            if (target instanceof Vex || target instanceof Phantom) {
               float bonusDmg = 10.0F;
               target.hurt(this.damageSources().mobAttack(this), bonusDmg);
               this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 2.0F);
            }

            if (flag || target.isInvulnerable() || target instanceof Player p && p.isCreative()) {
               this.handleMysticEyesEffect(target);
               if (target.isDeadOrDying()) {
                  long time = this.level().getGameTime();
                  if (time - this.lastKillTime < 100L) {
                     this.killCount++;
                     if (this.killCount >= 3) {
                        this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.multi_kill", 20.0, 1.0F);
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
   }

   public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
   }

   public boolean canBeAffected(MobEffectInstance effectInstance) {
      if (effectInstance.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) {
         return false;
      } else {
         return !((MobEffect)effectInstance.getEffect().value()).isBeneficial() && this.random.nextFloat() < 0.5F ? false : super.canBeAffected(effectInstance);
      }
   }

   private void handleMysticEyesEffect(LivingEntity target) {
      this.handleMysticEyesEffect(target, false);
   }

   private void handleMysticEyesEffect(LivingEntity target, boolean boosted) {
      float health = target.getHealth();
      boolean killed = false;
      if (!(health <= 20.0F) && (!boosted || !(health <= 50.0F))) {
         float killChance = Math.max(0.01F, 20.0F / health);
         if (boosted) {
            killChance = Math.max(0.1F, 100.0F / health);
         }

         if (this.random.nextFloat() < killChance) {
            this.forceKill(target);
            killed = true;
         }
      } else {
         this.forceKill(target);
         killed = true;
      }

      if (!killed) {
         if (health > 20.0F) {
            float halfChance = Math.max(0.2F, 60.0F / health);
            if (this.random.nextFloat() < halfChance) {
               target.setHealth(health / 2.0F);
               this.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 2.0F);
            }
         }

         if (this.random.nextFloat() < 0.45F) {
            List<EquipmentSlot> validSlots = new ArrayList<>();

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               if (slot.getType() == Type.HUMANOID_ARMOR) {
                  ItemStack stack = target.getItemBySlot(slot);
                  if (!stack.isEmpty() && stack.isDamageableItem()) {
                     validSlots.add(slot);
                  }
               }
            }

            ItemStack mainHand = target.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
               validSlots.add(EquipmentSlot.MAINHAND);
            }

            if (!validSlots.isEmpty()) {
               EquipmentSlot selectedSlot = validSlots.get(this.random.nextInt(validSlots.size()));
               ItemStack targetItem = target.getItemBySlot(selectedSlot);
               if (this.random.nextFloat() < 0.11111112F) {
                  targetItem.hurtAndBreak(targetItem.getMaxDamage(), target, selectedSlot);
                  if (!targetItem.isEmpty()) {
                     target.setItemSlot(selectedSlot, ItemStack.EMPTY);
                  }
               } else {
                  int currentDamage = targetItem.getDamageValue();
                  int maxDamage = targetItem.getMaxDamage();
                  int remainingDurability = maxDamage - currentDamage;
                  int damageToAdd = remainingDurability / 2;
                  targetItem.hurtAndBreak(damageToAdd, target, selectedSlot);
               }
            }
         }
      }
   }

   private void forceKill(LivingEntity target) {
      this.triggerSwarmAnger(target);
      if (target instanceof Player player && player.isCreative()) {
         player.getAbilities().invulnerable = false;
         player.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
         player.setHealth(0.0F);
         player.die(this.damageSources().genericKill());
         player.getAbilities().invulnerable = true;
         player.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.kill_creative"), false);
      } else {
         if ((Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !(Boolean)this.entityData.get(IS_BETRAYED)) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 20.0);
            if (nearestPlayer != null && target instanceof Mob mob && mob.getTarget() == nearestPlayer) {
               nearestPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.help_kill"), false);
            }
         }

         target.setInvulnerable(false);
         target.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
         target.setHealth(0.0F);
         target.die(this.damageSources().genericKill());
      }

      this.playSound((SoundEvent)SoundEvents.TRIDENT_THUNDER.value(), 1.0F, 2.0F);
   }

   private void triggerSwarmAnger(LivingEntity target) {
      EntityUtils.triggerSwarmAnger(this.level(), this, target);
   }

   protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      long currentTime = this.level().getGameTime();
      UUID pid = pPlayer.getUUID();
      if (this.lastPlayerInteractionTime.containsKey(pid)) {
         long lastTime = this.lastPlayerInteractionTime.get(pid);
         if (currentTime - lastTime < 100L) {
            this.playerInteractionCount.put(pid, this.playerInteractionCount.getOrDefault(pid, 0) + 1);
         } else {
            this.playerInteractionCount.put(pid, 1);
         }
      } else {
         this.playerInteractionCount.put(pid, 1);
      }

      this.lastPlayerInteractionTime.put(pid, currentTime);
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (this.playerInteractionCount.get(pid) > 5) {
         if (!this.level().isClientSide) {
            this.playerInteractionCount.put(pid, 0);
            String[] annoyedLines = new String[]{
               "entity.typemoonworld.ryougi_shiki.speech.annoyed_1",
               "entity.typemoonworld.ryougi_shiki.speech.annoyed_2",
               "entity.typemoonworld.ryougi_shiki.speech.annoyed_3"
            };
            int idx = this.random.nextInt(annoyedLines.length);
            this.broadcastToNearbyPlayers(annoyedLines[idx], 20.0);
         }

         return InteractionResult.FAIL;
      } else {
         if (!this.hasLeftArm()) {
            if (itemstack.is(Items.IRON_BLOCK)) {
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm_fix_fail", 20.0);
               }

               return InteractionResult.SUCCESS;
            }

            if (itemstack.is(Items.CLOCK)) {
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.lost_arm_time", 20.0);
               }

               return InteractionResult.SUCCESS;
            }

            if (this.random.nextFloat() < 0.3F) {
               String[] lostArmLines = new String[]{
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
                  "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_1",
                  "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_2",
                  "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_3",
                  "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_4",
                  "entity.typemoonworld.ryougi_shiki.speech.lost_arm_interact_5"
               };
               if (!this.level().isClientSide) {
                  int idx = this.random.nextInt(lostArmLines.length);
                  this.broadcastToNearbyPlayers(lostArmLines[idx], 20.0);
               }
            }
         }

         if (itemstack.is(Items.SWEET_BERRIES)) {
            if ((Boolean)this.entityData.get(IS_BETRAYED)) {
               if (!this.level().isClientSide) {
                  pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.betrayed_ignore"), false);
               }

               return InteractionResult.FAIL;
            } else if (this.getHealth() < this.getMaxHealth()) {
               this.heal(5.0F);
               itemstack.shrink(1);
               this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
               int currentFriendship = (Integer)this.entityData.get(FRIENDSHIP_LEVEL);
               this.entityData.set(FRIENDSHIP_LEVEL, currentFriendship + 1);
               if (this.getTarget() == pPlayer) {
                  this.setTarget(null);
               }

               if (!this.level().isClientSide) {
                  if (currentFriendship + 1 >= 5) {
                     pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.feed_high_trust"), false);
                  } else {
                     pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.feed"), false);
                  }
               }

               return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
               if (!this.level().isClientSide) {
                  pPlayer.displayClientMessage(Component.translatable("entity.typemoonworld.ryougi_shiki.speech.full_health"), false);
               }

               return InteractionResult.CONSUME;
            }
         } else {
            this.getLookControl().setLookAt(pPlayer, 30.0F, 30.0F);
            if (itemstack.is(Items.WATER_BUCKET)) {
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_water", 20.0);
               }

               return InteractionResult.SUCCESS;
            } else if (!itemstack.is(Items.MAP) && !itemstack.is(Items.FILLED_MAP)) {
               if (itemstack.is(Items.APPLE)) {
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_apple", 20.0);
                  }

                  return InteractionResult.SUCCESS;
               } else if (itemstack.is(Items.RABBIT_FOOT)) {
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_rabbit_foot", 20.0);
                  }

                  return InteractionResult.SUCCESS;
               } else if (itemstack.is(Items.COMPASS)) {
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_compass", 20.0);
                  }

                  return InteractionResult.SUCCESS;
               } else if (itemstack.is(Items.GLASS_BOTTLE)) {
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_glass_bottle", 20.0);
                  }

                  return InteractionResult.SUCCESS;
               } else if (!itemstack.is(Items.POPPY) && !itemstack.is(Items.RED_TULIP)) {
                  if (itemstack.is(Items.SKELETON_SKULL)) {
                     if (!this.level().isClientSide) {
                        this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_skull", 20.0);
                     }

                     return InteractionResult.SUCCESS;
                  } else if (!itemstack.is(Items.ICE) && !itemstack.is(Items.PACKED_ICE)) {
                     if (itemstack.getItem() instanceof BedItem) {
                        if (!this.level().isClientSide) {
                           this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_bed", 20.0);
                        }

                        return InteractionResult.SUCCESS;
                     } else if (!itemstack.is(Items.BOOK) && !itemstack.is(Items.WRITABLE_BOOK)) {
                        if (itemstack.is(Items.SHEARS)) {
                           if (!this.level().isClientSide) {
                              this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_shears", 20.0);
                           }

                           return InteractionResult.SUCCESS;
                        } else {
                           return super.mobInteract(pPlayer, pHand);
                        }
                     } else {
                        if (!this.level().isClientSide) {
                           this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_book", 20.0);
                        }

                        return InteractionResult.SUCCESS;
                     }
                  } else {
                     if (!this.level().isClientSide) {
                        this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_ice", 20.0);
                     }

                     return InteractionResult.SUCCESS;
                  }
               } else {
                  if (!this.level().isClientSide) {
                     this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_flower", 20.0);
                  }

                  return InteractionResult.SUCCESS;
               }
            } else {
               if (!this.level().isClientSide) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.see_map", 20.0);
               }

               return InteractionResult.SUCCESS;
            }
         }
      }
   }

   private boolean isBesieged() {
      List<LivingEntity> enemies = this.level()
         .getEntitiesOfClass(
            LivingEntity.class, this.getBoundingBox().inflate(3.0), e -> e != this && (e instanceof Monster || e instanceof Player) && e.isAlive()
         );
      long strongCount = enemies.stream().filter(e -> e.getMaxHealth() > 100.0F).count();
      return enemies.size() > 5 || strongCount > 3L;
   }

   private void startPanicState() {
      if (!this.isPanicState) {
         this.isPanicState = true;
         this.forcedSlashTimer = 0;
         this.playSound(SoundEvents.WARDEN_ROAR, 1.0F, 2.0F);
         this.broadcastToNearbyPlayers("entity.typemoonworld.ryougi_shiki.speech.panic", 20.0);
      }
   }

   private void performPanicSlash() {
      if (!this.level().isClientSide) {
         if (this.tickCount % 4 == 0) {
            BlockPos center = this.blockPosition();

            for (int x = -1; x <= 1; x++) {
               for (int y = -1; y <= 1; y++) {
                  for (int z = -1; z <= 1; z++) {
                     if (x != 0 || y != -1 || z != 0) {
                        BlockPos p = center.offset(x, y, z);
                        BlockState state = this.level().getBlockState(p);
                        if (!state.isAir() && (state.getDestroySpeed(this.level(), p) >= 0.0F || state.getFluidState().isSource())) {
                           this.level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        }
                     }
                  }
               }
            }

            AABB box = this.getBoundingBox().inflate(2.5);

            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, ex -> ex != this && ex.isAlive())) {
               if (!(e instanceof Player p && (Integer)this.entityData.get(FRIENDSHIP_LEVEL) >= 5 && !(Boolean)this.entityData.get(IS_BETRAYED))
                  && e == this.getTarget()) {
                  if (this.random.nextFloat() < 0.2F) {
                     this.handleMysticEyesEffect(e);
                  }

                  e.hurt(this.damageSources().mobAttack(this), 6.0F);
               }
            }

            ((ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 3, 1.0, 1.0, 1.0, 0.0);
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);
         }
      }
   }

   private void checkDangerousBlocks() {
      if (!this.level().isClientSide && this.tickCount % 5 == 0) {
         Vec3 velocity = this.getDeltaMovement();
         Vec3 look = this.getLookAngle();
         Vec3 dir = velocity.lengthSqr() > 0.01 ? velocity.normalize() : look;
         BlockPos center = this.blockPosition();
         boolean dangerFound = false;

         for (int i = 1; i <= 2; i++) {
            double tx = center.getX() + dir.x * i;
            double tz = center.getZ() + dir.z * i;
            BlockPos target = BlockPos.containing(tx, center.getY(), tz);
            if (this.isDangerous(this.level().getBlockState(target))
               || this.isDangerous(this.level().getBlockState(target.below()))
               || this.isDangerous(this.level().getBlockState(target.above()))) {
               dangerFound = true;
               break;
            }
         }

         if (!dangerFound && (this.isDangerous(this.level().getBlockState(center)) || this.isDangerous(this.level().getBlockState(center.below())))) {
            dangerFound = true;
         }

         if (dangerFound) {
            this.performSlash(true);
            this.slashCooldown = 20;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
         }
      }
   }

   private boolean isDangerous(BlockState state) {
      if (state.isAir()) {
         return false;
      } else {
         return state.getFluidState().is(FluidTags.LAVA)
            ? true
            : state.is(Blocks.MAGMA_BLOCK)
               || state.is(Blocks.CACTUS)
               || state.is(Blocks.SWEET_BERRY_BUSH)
               || state.is(Blocks.FIRE)
               || state.is(Blocks.SOUL_FIRE)
               || state.is(Blocks.CAMPFIRE)
               || state.is(Blocks.SOUL_CAMPFIRE)
               || state.is(Blocks.WITHER_ROSE)
               || state.is(Blocks.POWDER_SNOW);
      }
   }

   private boolean isNoblePhantasmAttack(DamageSource source) {
      Entity direct = source.getDirectEntity();
      Entity attacker = source.getEntity();
      if (!(direct instanceof BrokenPhantasmProjectileEntity) && !(direct instanceof SwordBarrelProjectileEntity)) {
         if (attacker instanceof LivingEntity living) {
            ItemStack mainHand = living.getMainHandItem();
            if (mainHand.getItem() instanceof NoblePhantasmItem) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         String animationName = "1";
         if (this.hasLeftArm() && this.hasSword()) {
            animationName = "1";
         } else if (this.hasLeftArm() && !this.hasSword()) {
            animationName = "2";
         } else if (!this.hasLeftArm() && this.hasSword()) {
            animationName = "3";
         } else if (!this.hasLeftArm() && !this.hasSword()) {
            animationName = "4";
         }

         return event.setAndContinue(RawAnimation.begin().thenLoop(animationName));
      }));
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
