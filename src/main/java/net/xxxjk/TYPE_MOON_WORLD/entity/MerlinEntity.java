package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.block.custom.UBWWeaponBlock;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.MerlinWorldEventLimiter;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MerlinEntity extends PathfinderMob implements GeoEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private int lastDebuffTick = 0;
   private int supportAuraCooldown = 0;
   private int excaliburSlashCooldown = 0;
   private int sakuraBloomCooldown = 0;
   private int sakuraTeleportCooldown = 0;
   private final Set<UUID> hostilePlayers = new HashSet<>();
   private boolean isClone = false;
   private int cloneAgeTicks = 0;
   private UUID originalMerlinId = null;
   private int activeCloneCount = 0;
   private boolean isIllusionInvisible = false;
   private int illusionCooldown = 0;
   private boolean hadEnemyLastTick = false;
   private int farCloneCooldown = 0;
   private int illusionStunCooldown = 0;
   private int targetLockTicks = 0;
   private boolean avalonActive = false;
   private int avalonTicks = 0;
   private int avalonCooldown = 0;
   private BlockPos avalonCenter = null;
   private final Map<BlockPos, BlockState> avalonOriginalBlocks = new HashMap<>();
   private final Map<BlockPos, CompoundTag> avalonOriginalBlockEntities = new HashMap<>();
   private List<BlockPos> avalonRestorationQueue = new ArrayList<>();
   private List<BlockPos> avalonClearSurfaceQueue = new ArrayList<>();
   private int avalonFlowerLayerY = Integer.MIN_VALUE;
   private int avalonRestoreBlocksPerTick = 400;
   private double avalonCurrentRadius = 0.0;
   private double avalonPrevRadius = 0.0;
   private final Map<UUID, BlockPos> avalonLastInsidePositions = new HashMap<>();
   private double lastTargetDistSqr = 0.0;
   private int autoTeleportCooldown = 0;
   private int pendingCloneSpawns = 0;
   private int pendingCloneSpawnCooldown = 0;
   private UUID pendingCloneTargetId = null;
   private int mentalPulseCooldown = 0;
   private final Map<UUID, Integer> mentalAffectWindow = new HashMap<>();
   private long lastSpeechTime = 0L;
   private Map<String, Long> messageCooldowns = new HashMap<>();
   private boolean midHpLineSpoken = false;
   private boolean lowHpLineSpoken = false;
   private int hpHighCombatTicks = 0;
   private int siegeCheckCooldown = 0;
   private int infightingCooldown = 0;
   private int outOfCombatTicks = 0;
   private static final Map<String, Integer> SPEECH_VARIANTS = new HashMap<>();
   private static final long SPEECH_MIN_INTERVAL_TICKS = 40L;
   private static final long SPEECH_MIN_INTERVAL_COMBAT_TICKS = 14L;
   private static final long SPEECH_REPEAT_COOLDOWN_TICKS = 1200L;
   private static final long SPEECH_REPEAT_COOLDOWN_COMBAT_TICKS = 260L;

   public MerlinEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
   }

   public void onAddedToLevel() {
      super.onAddedToLevel();
      if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && !this.isClone) {
         double baseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
         serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), baseY, this.getZ(), 260, 2.4, 1.5, 2.4, 0.035);
      }
   }

   private void broadcastToNearbyPlayers(String baseKey, double radius) {
      if (!this.isClone) {
         if (!this.level().isClientSide) {
            boolean inCombat = this.isInCombatNow();
            boolean combatSpeech = this.isCombatSpeechKey(baseKey);
            long minInterval = inCombat && combatSpeech ? 14L : 40L;
            long repeatCooldown = inCombat && combatSpeech ? 260L : 1200L;
            long currentTime = this.level().getGameTime();
            if (currentTime - this.lastSpeechTime < minInterval) {
               return;
            }

            if (this.messageCooldowns.containsKey(baseKey)) {
               long lastTime = this.messageCooldowns.get(baseKey);
               if (currentTime - lastTime < repeatCooldown) {
                  return;
               }
            }

            this.lastSpeechTime = currentTime;
            this.messageCooldowns.put(baseKey, currentTime);
            String finalKey = baseKey;
            int variantCount = SPEECH_VARIANTS.getOrDefault(baseKey, 0);
            if (variantCount > 0) {
               int idx = 2 + this.random.nextInt(variantCount);
               finalKey = baseKey + "_v" + idx;
               if (variantCount == 1 && this.random.nextFloat() < 0.5F) {
                  finalKey = baseKey;
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
   }

   private void broadcastToNearbyPlayers(String translationKey, double radius, float probability) {
      if (!this.isClone) {
         float actualProbability = probability;
         if (this.isInCombatNow() && this.isCombatSpeechKey(translationKey)) {
            actualProbability = Math.min(1.0F, probability + 0.15F);
         }

         if (this.random.nextFloat() < actualProbability) {
            this.broadcastToNearbyPlayers(translationKey, radius);
         }
      }
   }

   private boolean isInCombatNow() {
      LivingEntity target = this.getTarget();
      return target != null && target.isAlive();
   }

   private boolean isCombatSpeechKey(String key) {
      return key.contains(".combat_")
         || key.contains(".target_")
         || key.contains(".hostile_player")
         || key.contains(".hurt_high")
         || key.contains(".low_health")
         || key.contains(".attack_blocked")
         || key.contains(".multi_kill")
         || key.contains(".avalon_");
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true) {
         public boolean canUse() {
            return MerlinEntity.this.getHealth() <= 900.0F && super.canUse();
         }

         public boolean canContinueToUse() {
            return MerlinEntity.this.getHealth() <= 900.0F && super.canContinueToUse();
         }
      });
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 1000.0)
         .add(Attributes.MOVEMENT_SPEED, 0.18)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.ARMOR, 1.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
         .add(Attributes.FOLLOW_RANGE, 48.0);
   }

   public void aiStep() {
      super.aiStep();
      this.clearFire();
      if (!this.level().isClientSide) {
         float health = this.getHealth();
         ItemStack mainHand = this.getMainHandItem();
         LivingEntity currentTarget = this.getTarget();
         boolean hasEnemy = currentTarget != null && currentTarget.isAlive();
         if (this.isClone) {
            if (this.level() instanceof ServerLevel serverLevel) {
               this.cloneAgeTicks++;
               if (this.cloneAgeTicks >= 200) {
                  this.handleCloneEnd(serverLevel, false);
                  return;
               }

               MerlinEntity original = null;
               if (this.originalMerlinId != null && serverLevel.getEntity(this.originalMerlinId) instanceof MerlinEntity merlin && merlin.isAlive()) {
                  original = merlin;
               }

               if (original == null) {
                  this.handleCloneEnd(serverLevel, false);
                  return;
               }

               this.hostilePlayers.clear();
               this.hostilePlayers.addAll(original.hostilePlayers);
               LivingEntity originalTarget = original.getTarget();
               if (originalTarget == null || !originalTarget.isAlive()) {
                  this.handleCloneEnd(serverLevel, false);
                  return;
               }

               this.setTarget(originalTarget);
               if (this.excaliburSlashCooldown > 0) {
                  this.excaliburSlashCooldown--;
               }

               if (this.sakuraTeleportCooldown > 0) {
                  this.sakuraTeleportCooldown--;
               }

               double distSqr = this.distanceToSqr(originalTarget);
               if (distSqr > 36.0 && this.sakuraTeleportCooldown <= 0 && this.sakuraTeleport(serverLevel, originalTarget)) {
                  this.sakuraTeleportCooldown = 40;
               }

               if (this.excaliburSlashCooldown <= 0 && distSqr <= 256.0) {
                  this.performExcaliburSlash(serverLevel);
                  this.excaliburSlashCooldown = 40;
               }
            }

            return;
         }

         if (this.illusionCooldown > 0) {
            this.illusionCooldown--;
         }

         if (this.farCloneCooldown > 0) {
            this.farCloneCooldown--;
         }

         if (this.illusionStunCooldown > 0) {
            this.illusionStunCooldown--;
         }

         if (this.avalonCooldown > 0) {
            this.avalonCooldown--;
         }

         if (this.supportAuraCooldown > 0) {
            this.supportAuraCooldown--;
         }

         if (this.excaliburSlashCooldown > 0) {
            this.excaliburSlashCooldown--;
         }

         if (this.sakuraBloomCooldown > 0) {
            this.sakuraBloomCooldown--;
         }

         if (this.sakuraTeleportCooldown > 0) {
            this.sakuraTeleportCooldown--;
         }

         if (this.autoTeleportCooldown > 0) {
            this.autoTeleportCooldown--;
         }

         if (this.pendingCloneSpawnCooldown > 0) {
            this.pendingCloneSpawnCooldown--;
         }

         if (this.mentalPulseCooldown > 0) {
            this.mentalPulseCooldown--;
         }

         if (this.level() instanceof ServerLevel serverLevelGround) {
            this.handleEnvironmentTeleport(serverLevelGround, currentTarget, hasEnemy);
            if (this.isLeashed() || this.isPassenger()) {
               try {
                  this.dropLeash(true, true);
               } catch (Throwable var23) {
               }

               try {
                  this.stopRiding();
               } catch (Throwable var22) {
               }

               double angle = this.random.nextDouble() * Math.PI * 2.0;
               double dist = 6.0 + this.random.nextDouble() * 4.0;
               BlockPos base = this.blockPosition().offset((int)(Math.cos(angle) * dist), 0, (int)(Math.sin(angle) * dist));
               BlockPos ground = this.findGround(serverLevelGround, base);
               BlockPos target = ground != null ? ground.above() : base.above();
               double tx = target.getX() + 0.5;
               double ty = target.getY();
               double tz = target.getZ() + 0.5;
               this.teleportTo(tx, ty, tz);
               serverLevelGround.sendParticles(ParticleTypes.CHERRY_LEAVES, tx, ty + 0.8, tz, 30, 0.8, 0.6, 0.8, 0.02);
               serverLevelGround.playSound(null, tx, ty, tz, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.05F);
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.escape_trap", 16.0, 1.0F);
            }

            double radius = 16.0;
            AABB box = this.getBoundingBox().inflate(radius);

            for (Player player : serverLevelGround.getEntitiesOfClass(Player.class, box, LivingEntity::isAlive)) {
               if (player instanceof ServerPlayer serverPlayer) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  int favor = vars.merlin_favor;
                  if (!hasEnemy && favor <= -5) {
                     this.setTarget(player);
                     this.hostilePlayers.add(player.getUUID());
                     ItemStack mainHandNow = this.getMainHandItem();
                     if (mainHandNow.isEmpty() || mainHandNow.getItem() != ModItems.EXCALIBUR.get()) {
                        this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXCALIBUR.get()));
                     }

                     this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.hostile_player", 20.0);
                  }
               }
            }

            CompoundTag tag = this.getPersistentData();
            if (tag.getBoolean("TypeMoonHelperClone") || tag.getBoolean("TypeMoonSummonedFull")) {
               String ownerStr = tag.getString("TypeMoonHelperOwner");
               UUID ownerId = null;
               if (ownerStr != null && !ownerStr.isEmpty()) {
                  try {
                     ownerId = UUID.fromString(ownerStr);
                  } catch (Exception var21) {
                  }
               }

               Player owner = null;
               if (ownerId != null) {
                  for (Player p : serverLevelGround.players()) {
                     if (p.getUUID().equals(ownerId)) {
                        owner = p;
                        break;
                     }
                  }
               }

               double r = 24.0;
               AABB mobBox = new AABB(this.getX() - r, this.getY() - 4.0, this.getZ() - r, this.getX() + r, this.getY() + 4.0, this.getZ() + r);
               List<Monster> mobs = serverLevelGround.getEntitiesOfClass(Monster.class, mobBox, m -> true);
               boolean any = false;

               for (Monster m : mobs) {
                  if (m.getTarget() == this || owner != null && m.getTarget() == owner) {
                     any = true;
                     break;
                  }
               }

               boolean clear = !any && this.getTarget() == null;
               int ct = tag.getInt("TypeMoonHelperClearTicks");
               if (clear) {
                  tag.putInt("TypeMoonHelperClearTicks", ++ct);
                  if (ct >= 40) {
                     if (!this.level().isClientSide) {
                        serverLevelGround.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 0.8, this.getZ(), 40, 1.2, 0.8, 1.2, 0.03);
                        this.discard();
                     }

                     return;
                  }
               } else {
                  tag.putInt("TypeMoonHelperClearTicks", 0);
               }
            }
         }

         if (hasEnemy && this.getTarget() instanceof ServerPlayer creativePlayer && creativePlayer.isCreative()) {
            CompoundTag tag = this.getPersistentData();
            int ct = tag.getInt("TypeMoonCreativeAttackTicks");
            tag.putInt("TypeMoonCreativeAttackTicks", ++ct);
            if (ct >= 80) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.kill_creative", 20.0);
               this.hostilePlayers.remove(creativePlayer.getUUID());
               this.setTarget(null);
               tag.putInt("TypeMoonCreativeAttackTicks", 0);
            }
         } else {
            this.getPersistentData().putInt("TypeMoonCreativeAttackTicks", 0);
         }

         if (hasEnemy) {
            this.targetLockTicks++;
         } else {
            this.targetLockTicks = 0;
         }

         if (hasEnemy) {
            this.outOfCombatTicks = 0;
            if (this.getHealth() > 900.0F) {
               this.hpHighCombatTicks++;
               if (this.hpHighCombatTicks >= 400) {
                  if (this.getTarget() instanceof Player px) {
                     this.hostilePlayers.remove(px.getUUID());
                  }

                  this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.giveup_player", 20.0);
                  this.setTarget(null);
                  this.hpHighCombatTicks = 0;
               }
            } else {
               this.hpHighCombatTicks = 0;
            }
         } else {
            this.hpHighCombatTicks = 0;
            this.outOfCombatTicks++;
            if (this.outOfCombatTicks >= 200) {
               float maxHp = (float)this.getAttributeValue(Attributes.MAX_HEALTH);
               float curHp = this.getHealth();
               if (curHp < maxHp && this.tickCount % 10 == 0) {
                  float inc = 25.0F;
                  this.setHealth(Math.min(maxHp, curHp + inc));
               }
            }
         }

         if (hasEnemy && this.targetLockTicks == 1) {
            LivingEntity t = this.getTarget();
            if (t instanceof Player) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_player_taunt", 22.0);
            } else if (t instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null || t instanceof TamableAnimal tamable && tamable.isTame()) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_summon_taunt", 20.0);
            } else if (t instanceof EnderDragon || t instanceof WitherBoss) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_boss_taunt", 24.0);
            } else if (t instanceof Zombie) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_zombie", 20.0);
            } else if (t instanceof Skeleton) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_skeleton", 20.0);
            } else if (t instanceof Creeper) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_creeper", 20.0);
            } else if (t instanceof EnderMan) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_enderman", 20.0);
            } else if (t instanceof Spider) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_spider", 20.0);
            } else if (t instanceof Witch) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_witch", 20.0);
            } else if (t instanceof Phantom) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_phantom", 20.0);
            } else if (t instanceof Animal) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_animal", 16.0);
            } else if (t instanceof Villager) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_villager", 16.0);
            } else if (t instanceof IronGolem) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_iron_golem", 20.0);
            } else if (t instanceof Monster && this.level() instanceof ServerLevel serverLevel) {
               List<Monster> nearbyMonsters = serverLevel.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(15.0));
               if (nearbyMonsters.size() > 5) {
                  this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.target_horde", 20.0);
               }
            }
         }

         if (hasEnemy
            && this.targetLockTicks >= 40
            && !this.isIllusionInvisible
            && this.illusionCooldown <= 0
            && this.level() instanceof ServerLevel serverLevelx) {
            boolean swordState = health <= 900.0F && mainHand.getItem() == ModItems.EXCALIBUR.get();
            int cloneCount = swordState ? 2 + this.random.nextInt(2) : 1;
            float chance = swordState ? 0.04F : 0.02F;
            if (this.random.nextFloat() < chance) {
               this.spawnIllusionClones(serverLevelx, currentTarget, cloneCount);
               this.illusionCooldown = 200;
               this.targetLockTicks = 0;
            }
         }

         if (this.pendingCloneSpawns > 0
            && this.level() instanceof ServerLevel serverLevelClone
            && this.pendingCloneSpawnCooldown <= 0
            && this.activeCloneCount < 10) {
            LivingEntity pendingTarget = null;
            if (this.pendingCloneTargetId != null && serverLevelClone.getEntity(this.pendingCloneTargetId) instanceof LivingEntity le && le.isAlive()) {
               pendingTarget = le;
            }

            this.spawnSingleIllusionClone(serverLevelClone, pendingTarget);
            this.pendingCloneSpawns--;
            this.pendingCloneSpawnCooldown = 20;
         }

         if (hasEnemy && this.level() instanceof ServerLevel serverLevelxx) {
            if (this.siegeCheckCooldown > 0) {
               this.siegeCheckCooldown--;
            }

            if (this.siegeCheckCooldown <= 0 && !this.isClone) {
               int nearbyEnemies = this.countNearbyEnemies(serverLevelxx, 8.0);
               if (nearbyEnemies >= 3) {
                  this.spawnSingleIllusionClone(serverLevelxx, currentTarget);
                  double angle = this.random.nextDouble() * Math.PI * 2.0;
                  double dist = 16.0 + this.random.nextDouble() * 16.0;
                  BlockPos farPos = this.blockPosition().offset((int)(Math.cos(angle) * dist), 0, (int)(Math.sin(angle) * dist));
                  BlockPos safePos = this.findNearestSolidGround(serverLevelxx, farPos, 12);
                  if (safePos != null) {
                     this.teleportTo(safePos.getX() + 0.5, safePos.getY() + 1.0, safePos.getZ() + 0.5);
                     serverLevelxx.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 0.8, this.getZ(), 40, 1.0, 1.0, 1.0, 0.03);
                     this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.0F);
                     this.siegeCheckCooldown = 100;
                     this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.teleport", 12.0);
                  }
               }
            }

            if (this.infightingCooldown > 0) {
               this.infightingCooldown--;
            }

            if (health > 900.0F && this.infightingCooldown <= 0) {
               AABB box = this.getBoundingBox().inflate(16.0);
               List<Monster> mobs = serverLevelxx.getEntitiesOfClass(Monster.class, box, mx -> mx.isAlive() && mx.getTarget() == this);
               if (mobs.size() >= 2) {
                  if (!MerlinWorldEventLimiter.tryConsume(serverLevelxx)) {
                     this.infightingCooldown = 80;
                  } else {
                     Collections.shuffle(mobs);

                     for (int i = 0; i < mobs.size(); i++) {
                        Monster m1 = mobs.get(i);
                        Monster m2 = mobs.get((i + 1) % mobs.size());
                        m1.setTarget(m2);
                        serverLevelxx.sendParticles(ParticleTypes.ANGRY_VILLAGER, m1.getX(), m1.getEyeY(), m1.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                     }

                     this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.prank_positive", 16.0);
                     this.infightingCooldown = 300;
                  }
               }
            }

            double distSqrx = this.distanceToSqr(currentTarget);
            if (distSqrx > 256.0 && this.farCloneCooldown <= 0 && this.random.nextFloat() < 0.35F) {
               this.spawnCloneNearTarget(serverLevelxx, currentTarget);
               this.farCloneCooldown = 200;
            }

            if (this.illusionStunCooldown <= 0 && this.random.nextFloat() < 0.05F) {
               currentTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
               double stunBaseY = currentTarget.getY() + currentTarget.getBbHeight() * 0.4 + this.random.nextDouble() * currentTarget.getBbHeight() * 0.4;
               serverLevelxx.sendParticles(ParticleTypes.CHERRY_LEAVES, currentTarget.getX(), stunBaseY, currentTarget.getZ(), 30, 0.7, 0.9, 0.7, 0.03);
               this.illusionStunCooldown = 100;
            }
         }

         if (health > 900.0F && hasEnemy && this.level() instanceof ServerLevel serverLevelxx && this.supportAuraCooldown <= 0) {
            if (!MerlinWorldEventLimiter.tryConsume(serverLevelxx)) {
               this.supportAuraCooldown = 100;
            } else {
               double radius = 12.0;
               AABB box = new AABB(this.getX() - radius, this.getY() - 2.0, this.getZ() - radius, this.getX() + radius, this.getY() + 2.0, this.getZ() + radius);

               for (Player playerx : serverLevelxx.getEntitiesOfClass(Player.class, box, px -> px.isAlive() && !px.isSpectator())) {
                  if (!this.hostilePlayers.contains(playerx.getUUID())) {
                     playerx.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
                     playerx.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
                     int enchantCount = 10 + this.random.nextInt(15);
                     int sakuraCount = 8 + this.random.nextInt(16);
                     double playerBaseY = playerx.getY() + 0.6 + this.random.nextDouble() * 0.6;
                     serverLevelxx.sendParticles(ParticleTypes.ENCHANT, playerx.getX(), playerBaseY + 0.4, playerx.getZ(), enchantCount, 0.6, 0.6, 0.6, 0.12);
                     serverLevelxx.sendParticles(ParticleTypes.CHERRY_LEAVES, playerx.getX(), playerBaseY, playerx.getZ(), sakuraCount, 1.0, 0.8, 1.0, 0.03);
                  }
               }

               for (MerlinEntity clone : serverLevelxx.getEntitiesOfClass(MerlinEntity.class, box, e -> e.isAlive() && e.isClone)) {
                  clone.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
                  clone.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
                  int sakuraCount = 8 + this.random.nextInt(16);
                  double cloneBaseY = clone.getY() + 0.8 + this.random.nextDouble() * 0.8;
                  serverLevelxx.sendParticles(ParticleTypes.CHERRY_LEAVES, clone.getX(), cloneBaseY, clone.getZ(), sakuraCount, 1.0, 0.9, 1.0, 0.03);
               }

               int selfSakura = 20 + this.random.nextInt(20);
               double selfBaseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
               serverLevelxx.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), selfBaseY, this.getZ(), selfSakura, 1.2, 0.9, 1.2, 0.03);
               this.supportAuraCooldown = 100;
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.support_aura", 18.0);
            }
         }

         if (this.level() instanceof ServerLevel serverLevelxxx && this.sakuraBloomCooldown <= 0 && hasEnemy) {
            this.spawnSakuraBloom(serverLevelxxx);
            this.sakuraBloomCooldown = 40;
         }

         if (this.avalonActive && !hasEnemy && this.hadEnemyLastTick && this.level() instanceof ServerLevel endLevel) {
            this.avalonTicks = Math.max(this.avalonTicks, 600);
         }

         this.hadEnemyLastTick = hasEnemy;
         if (this.level() instanceof ServerLevel serverLevelAvalon) {
            if (this.avalonActive) {
               this.tickAvalonField(serverLevelAvalon);
               this.tickAvalonBarrier(serverLevelAvalon);
            } else if (this.avalonCooldown <= 0) {
               boolean criticalTrigger = health < 100.0F && !this.isClone;
               if (criticalTrigger) {
                  this.startAvalonField(serverLevelAvalon);
               } else if (health <= 300.0F && hasEnemy) {
                  int enemyCount = this.countNearbyEnemies(serverLevelAvalon, 16.0);
                  boolean highThreat = this.isHighThreatGroup(serverLevelAvalon, 16.0);
                  if (enemyCount >= 4 || highThreat) {
                     this.startAvalonField(serverLevelAvalon);
                  }
               }
            }
         }

         if (hasEnemy) {
            float maxHealth = (float)this.getAttributeValue(Attributes.MAX_HEALTH);
            if (!this.midHpLineSpoken && health <= maxHealth * 0.5F && health > maxHealth * 0.25F) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.hurt_high", 20.0);
               this.midHpLineSpoken = true;
            }

            if (!this.lowHpLineSpoken && health <= maxHealth * 0.25F) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.low_health", 20.0);
               this.lowHpLineSpoken = true;
            }
         } else {
            float maxHealthx = (float)this.getAttributeValue(Attributes.MAX_HEALTH);
            if (health >= maxHealthx * 0.95F) {
               this.midHpLineSpoken = false;
               this.lowHpLineSpoken = false;
            }
         }

         if (!(health <= 900.0F)) {
            if (!mainHand.isEmpty() && mainHand.getItem() == ModItems.EXCALIBUR.get()) {
               this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
         } else {
            if (mainHand.isEmpty() || mainHand.getItem() != ModItems.EXCALIBUR.get()) {
               this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXCALIBUR.get()));
            }

            if (this.excaliburSlashCooldown <= 0 && this.level() instanceof ServerLevel serverLevelxxx) {
               LivingEntity target = this.getTarget();
               if (target != null && target.isAlive()) {
                  double distSqrxx = this.distanceToSqr(target);
                  if (distSqrxx <= 256.0) {
                     this.performExcaliburSlash(serverLevelxxx);
                     this.excaliburSlashCooldown = 80;
                  }
               }
            }
         }

         if (this.level() instanceof ServerLevel serverLevelPulse && this.mentalPulseCooldown <= 0) {
            this.castMentalInfluencePulse(serverLevelPulse);
            this.mentalPulseCooldown = this.avalonActive ? 240 : 400;
         }

         if (this.isIllusionInvisible && this.level() instanceof ServerLevel serverLevelInvis) {
            double selfBaseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
            serverLevelInvis.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), selfBaseY, this.getZ(), 8, 0.6, 0.8, 0.6, 0.02);
         }

         if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            if (health <= 900.0F && this.getMainHandItem().getItem() == ModItems.EXCALIBUR.get()) {
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
            } else {
               this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.18);
            }
         }
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide
         && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
         && source.getEntity() instanceof LivingEntity attacker
         && this.sakuraTeleportCooldown <= 0
         && this.random.nextFloat() < 0.5F
         && this.level() instanceof ServerLevel serverLevel
         && this.sakuraTeleport(serverLevel, attacker)) {
         this.sakuraTeleportCooldown = 60;
         this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.combat_dodge", 12.0);
         return false;
      } else {
         if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            boolean naturalHazard = source.is(DamageTypes.IN_WALL)
               || source.is(DamageTypes.LAVA)
               || source.is(DamageTypes.ON_FIRE)
               || source.is(DamageTypes.IN_FIRE)
               || source.is(DamageTypes.HOT_FLOOR)
               || source.is(DamageTypes.CACTUS)
               || source.is(DamageTypes.DROWN)
               || source.is(DamageTypes.SWEET_BERRY_BUSH)
               || source.is(DamageTypes.STALAGMITE)
               || source.is(DamageTypes.FLY_INTO_WALL)
               || source.is(DamageTypes.FALLING_ANVIL)
               || source.is(DamageTypes.FALLING_BLOCK)
               || source.is(DamageTypes.FALLING_STALACTITE)
               || source.is(DamageTypes.LIGHTNING_BOLT)
               || source.is(DamageTypes.FALL)
               || source.is(DamageTypes.FREEZE);
            if (naturalHazard) {
               if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                  BlockPos safePos = this.findNearestSolidGround(serverLevel, this.blockPosition(), 12);
                  if (safePos != null) {
                     Vec3 from = this.position();
                     double fromBaseY = from.y + 0.8 + this.random.nextDouble() * 0.8;
                     serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, from.x, fromBaseY, from.z, 40, 1.0, 0.9, 1.0, 0.03);
                     boolean teleported = this.randomTeleport(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, true);
                     if (teleported) {
                        Vec3 to = this.position();
                        double toBaseY = to.y + 0.8 + this.random.nextDouble() * 0.8;
                        serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, to.x, toBaseY, to.z, 40, 1.0, 0.9, 1.0, 0.03);
                        this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.15F);
                     }
                  }
               }

               return false;
            }
         }

         if (source.getEntity() instanceof LivingEntity attacker && attacker != this) {
            this.setTarget(attacker);
            if (attacker instanceof Player player) {
               this.hostilePlayers.add(player.getUUID());
               if (!this.level().isClientSide) {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
                     TypeMoonWorldModVariables.PLAYER_VARIABLES
                  );
                  if (this.getHealth() <= 900.0F && vars.merlin_favor > -5) {
                     vars.merlin_favor = Math.max(-5, vars.merlin_favor - 1);
                     vars.syncPlayerVariables(player);
                  }
               }
            }

            if (this.tickCount - this.lastDebuffTick >= 20) {
               this.lastDebuffTick = this.tickCount;
               if (this.getHealth() > 900.0F) {
                  attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                  attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1));
                  if (this.level() instanceof ServerLevel serverLevelx) {
                     serverLevelx.sendParticles(ParticleTypes.WITCH, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 10, 0.5, 0.5, 0.5, 0.0);
                  }
               } else {
                  attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
                  attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
               }
            }
         }

         return super.hurt(source, amount);
      }
   }

   public void die(DamageSource source) {
      if (this.isClone && this.level() instanceof ServerLevel serverLevelClone) {
         this.handleCloneEnd(serverLevelClone, true);
      } else {
         if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            if (source.getEntity() instanceof Player killer) {
               TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)killer.getData(
                  TypeMoonWorldModVariables.PLAYER_VARIABLES
               );
               if (vars.merlin_favor > -5) {
                  vars.merlin_favor = Math.max(-5, vars.merlin_favor - 2);
                  vars.syncPlayerVariables(killer);
               }
            }

            if (this.avalonActive || !this.avalonOriginalBlocks.isEmpty()) {
               this.restoreAvalonInstantly(serverLevel);
            }

            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.combat_death", 24.0);
            double radius = 64.0;
            AABB box = new AABB(this.getX() - radius, this.getY() - 16.0, this.getZ() - radius, this.getX() + radius, this.getY() + 16.0, this.getZ() + radius);

            for (MerlinEntity clone : serverLevel.getEntitiesOfClass(
               MerlinEntity.class, box, e -> e.isAlive() && e.isClone && this.getUUID().equals(e.originalMerlinId)
            )) {
               clone.handleCloneEnd(serverLevel, false);
            }

            double baseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), baseY, this.getZ(), 300, 2.5, 1.6, 2.5, 0.035);
         }

         super.die(source);
         this.discard();
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("TypeMoonMerlinIsClone", this.isClone);
      CompoundTag data = this.getPersistentData();
      if (data.getBoolean("TypeMoonHelperClone")) {
         tag.putBoolean("TypeMoonMerlinHelperClone", true);
      }

      if (data.getBoolean("TypeMoonSummonedFull")) {
         tag.putBoolean("TypeMoonMerlinSummonedFull", true);
      }

      if (this.originalMerlinId != null) {
         tag.putUUID("TypeMoonMerlinOriginalId", this.originalMerlinId);
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      boolean savedClone = tag.getBoolean("TypeMoonMerlinIsClone");
      boolean savedHelper = tag.getBoolean("TypeMoonMerlinHelperClone");
      boolean savedSummoned = tag.getBoolean("TypeMoonMerlinSummonedFull");
      if (savedHelper) {
         this.getPersistentData().putBoolean("TypeMoonHelperClone", true);
      }

      if (savedSummoned) {
         this.getPersistentData().putBoolean("TypeMoonSummonedFull", true);
      }

      if (!savedClone && !savedHelper && !savedSummoned) {
         if (tag.hasUUID("TypeMoonMerlinOriginalId")) {
            this.originalMerlinId = tag.getUUID("TypeMoonMerlinOriginalId");
         }
      } else {
         if (!this.level().isClientSide) {
            this.discard();
         }
      }
   }

   private void restoreAvalonInstantly(ServerLevel serverLevel) {
      if (this.avalonCenter != null && !this.avalonOriginalBlocks.isEmpty()) {
         BlockPos center = this.avalonCenter;
         double maxRadius = 25.0;
         AABB box = new AABB(
            center.getX() + 0.5 - maxRadius,
            center.getY() - 8.0,
            center.getZ() + 0.5 - maxRadius,
            center.getX() + 0.5 + maxRadius,
            center.getY() + 8.0,
            center.getZ() + 0.5 + maxRadius
         );

         for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, box)) {
            item.discard();
         }

         int centerY = center.getY();
         int r = (int)Math.ceil(maxRadius);

         for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
               double distSqr = x * x + z * z;
               if (!(distSqr > maxRadius * maxRadius)) {
                  BlockPos flowerPos = new BlockPos(center.getX() + x, centerY, center.getZ() + z);
                  BlockState current = serverLevel.getBlockState(flowerPos);
                  if (current.is(BlockTags.FLOWERS)) {
                     serverLevel.setBlock(flowerPos, Blocks.AIR.defaultBlockState(), 18);
                  }
               }
            }
         }

         if (this.avalonRestorationQueue.isEmpty()) {
            this.avalonRestorationQueue = new ArrayList<>(this.avalonOriginalBlocks.keySet());
            this.avalonRestorationQueue.sort((p1, p2) -> {
               double d1 = p1.distSqr(center);
               double d2 = p2.distSqr(center);
               return Double.compare(d2, d1);
            });
         }

         while (!this.avalonRestorationQueue.isEmpty()) {
            int idx = this.avalonRestorationQueue.size() - 1;
            BlockPos pos = this.avalonRestorationQueue.remove(idx);
            BlockState state = this.avalonOriginalBlocks.remove(pos);
            if (state != null) {
               serverLevel.setBlock(pos, state, 18);
               CompoundTag nbt = this.avalonOriginalBlockEntities.remove(pos);
               if (nbt != null) {
                  BlockEntity be = serverLevel.getBlockEntity(pos);
                  if (be != null) {
                     be.loadWithComponents(nbt, serverLevel.registryAccess());
                  }
               }
            }
         }

         this.avalonActive = false;
         if (center != null) {
            double baseY = center.getY() + 0.8 + this.random.nextDouble() * 0.8;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, center.getX() + 0.5, baseY, center.getZ() + 0.5, 400, 3.0, 1.8, 3.0, 0.04);
         }

         this.avalonCenter = null;
         this.avalonLastInsidePositions.clear();
         this.avalonOriginalBlockEntities.clear();
      } else {
         this.avalonActive = false;
         this.avalonCenter = null;
         this.avalonLastInsidePositions.clear();
         this.avalonOriginalBlockEntities.clear();
         this.avalonRestorationQueue.clear();
      }
   }

   private void spawnSakuraBloom(ServerLevel serverLevel) {
      double radius = 6.0;
      BlockPos center = this.blockPosition();

      for (int i = 0; i < 12; i++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double dist = 2.0 + this.random.nextDouble() * (radius - 2.0);
         int dx = (int)Math.round(Math.cos(angle) * dist);
         int dz = (int)Math.round(Math.sin(angle) * dist);
         BlockPos pos = center.offset(dx, -1, dz);
         BlockState state = serverLevel.getBlockState(pos);
         if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.PODZOL)) {
            BlockPos above = pos.above();
            if (serverLevel.getBlockState(above).isAir()) {
               int choice = this.random.nextInt(5);
               Block block;
               if (choice == 0) {
                  block = Blocks.PINK_PETALS;
               } else if (choice == 1) {
                  block = Blocks.DANDELION;
               } else if (choice == 2) {
                  block = Blocks.POPPY;
               } else if (choice == 3) {
                  block = Blocks.AZURE_BLUET;
               } else {
                  block = Blocks.BLUE_ORCHID;
               }

               serverLevel.setBlock(above, block.defaultBlockState(), 3);
               double bloomBaseY = above.getY() + 0.1 + this.random.nextDouble() * 0.4;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, above.getX() + 0.5, bloomBaseY, above.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.01);
            }
         }
      }

      double ringBaseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
      serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), ringBaseY, this.getZ(), 25, 1.5, 0.9, 1.5, 0.02);
   }

   private boolean sakuraTeleport(ServerLevel serverLevel, LivingEntity attacker) {
      Vec3 from = this.position();

      for (int attempt = 0; attempt < 8; attempt++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double dist = 4.0 + this.random.nextDouble() * 4.0;
         double tx = this.getX() + Math.cos(angle) * dist;
         double tz = this.getZ() + Math.sin(angle) * dist;
         double ty = this.getY();
         BlockPos basePos = BlockPos.containing(tx, ty, tz);
         MutableBlockPos mutable = new MutableBlockPos().set(basePos);
         boolean found = false;

         for (int dy = -2; dy <= 2; dy++) {
            mutable.set(basePos.getX(), basePos.getY() + dy, basePos.getZ());
            BlockState below = serverLevel.getBlockState(mutable.below());
            BlockState here = serverLevel.getBlockState(mutable);
            BlockState above = serverLevel.getBlockState(mutable.above());
            if (!below.getCollisionShape(serverLevel, mutable.below()).isEmpty()
               && here.getCollisionShape(serverLevel, mutable).isEmpty()
               && above.getCollisionShape(serverLevel, mutable.above()).isEmpty()) {
               basePos = mutable.immutable();
               found = true;
               break;
            }
         }

         if (found) {
            double fromBaseY = from.y + 0.8 + this.random.nextDouble() * 0.8;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, from.x, fromBaseY, from.z, 40, 1.0, 0.9, 1.0, 0.03);
            boolean teleported = this.randomTeleport(basePos.getX() + 0.5, basePos.getY(), basePos.getZ() + 0.5, true);
            if (teleported) {
               Vec3 to = this.position();
               double toBaseY = to.y + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, to.x, toBaseY, to.z, 40, 1.0, 0.9, 1.0, 0.03);
               this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.2F);
               if (attacker != null && attacker.isAlive()) {
                  double atkBaseY = attacker.getY() + attacker.getBbHeight() * 0.4 + this.random.nextDouble() * attacker.getBbHeight() * 0.4;
                  serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, attacker.getX(), atkBaseY, attacker.getZ(), 15, 0.5, 0.7, 0.5, 0.02);
               }

               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.teleport", 12.0);
               return true;
            }
         }
      }

      return false;
   }

   private void spawnIllusionClones(ServerLevel serverLevel, LivingEntity target, int cloneCount) {
      int maxClones = 10;
      int availableSlots = Math.max(0, maxClones - this.activeCloneCount);
      if (availableSlots > 0) {
         int totalRequested = this.avalonActive ? maxClones : cloneCount;
         int totalClones = Math.min(totalRequested, availableSlots);
         if (totalClones > 0) {
            this.pendingCloneSpawns = 0;
            this.pendingCloneSpawnCooldown = 0;
            this.pendingCloneTargetId = target != null ? target.getUUID() : null;
            this.spawnSingleIllusionClone(serverLevel, target);
            if (totalClones > 1) {
               this.pendingCloneSpawns = totalClones - 1;
               this.pendingCloneSpawnCooldown = 20;
            }

            this.startIllusionInvisibility(serverLevel, target);
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.clone_spawn", 20.0, 0.25F);
         }
      }
   }

   private void spawnSingleIllusionClone(ServerLevel serverLevel, LivingEntity target) {
      int maxClones = 10;
      if (this.activeCloneCount >= maxClones) {
         this.pendingCloneSpawns = 0;
      } else {
         MerlinEntity clone = (MerlinEntity)((EntityType)ModEntities.MERLIN.get()).create(serverLevel);
         if (clone != null) {
            Vec3 origin = this.position();
            float yaw = this.getYRot();
            double radius = 1.5;
            double angleOffset = (this.random.nextDouble() - 0.5) * 120.0;
            double rad = Math.toRadians(yaw + angleOffset);
            double dx = Math.cos(rad) * radius;
            double dz = Math.sin(rad) * radius;
            clone.moveTo(origin.x + dx, origin.y, origin.z + dz, yaw, this.getXRot());
            clone.initClone(target, this);
            this.activeCloneCount++;
            serverLevel.addFreshEntity(clone);
         }
      }
   }

   private void startIllusionInvisibility(ServerLevel serverLevel, LivingEntity target) {
      this.isIllusionInvisible = true;
      this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 220, 0, true, false));
      double baseY = this.getY() + 0.8 + this.random.nextDouble() * 0.8;
      serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), baseY, this.getZ(), 60, 1.5, 1.0, 1.5, 0.03);
      AABB redirectBox = this.getBoundingBox().inflate(32.0);
      List<MerlinEntity> clones = serverLevel.getEntitiesOfClass(
         MerlinEntity.class, redirectBox, e -> e.isAlive() && e.isClone && this.getUUID().equals(e.originalMerlinId)
      );
      if (!clones.isEmpty()) {
         MerlinEntity mainClone = clones.get(0);

         for (Mob mob : serverLevel.getEntitiesOfClass(Mob.class, redirectBox, m -> m.getTarget() == this)) {
            mob.setTarget(mainClone);
         }
      }

      if (target != null && target.isAlive()) {
         this.sakuraTeleport(serverLevel, target);
      }

      this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.invisible", 16.0);
   }

   private void spawnCloneNearTarget(ServerLevel serverLevel, LivingEntity target) {
      Vec3 targetPos = target.position();
      MerlinEntity clone = (MerlinEntity)((EntityType)ModEntities.MERLIN.get()).create(serverLevel);
      if (clone != null) {
         int maxClones = 10;
         if (this.activeCloneCount < maxClones) {
            double dx = (this.random.nextDouble() - 0.5) * 2.0;
            double dz = (this.random.nextDouble() - 0.5) * 2.0;
            clone.moveTo(targetPos.x + dx, targetPos.y, targetPos.z + dz, target.getYRot(), target.getXRot());
            clone.initClone(target, this);
            this.activeCloneCount++;
            serverLevel.addFreshEntity(clone);
            double baseY = targetPos.y + 0.8 + this.random.nextDouble() * 0.8;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, targetPos.x, baseY, targetPos.z, 40, 1.0, 1.0, 1.0, 0.03);
         }
      }
   }

   private void initClone(LivingEntity target, MerlinEntity original) {
      this.isClone = true;
      this.cloneAgeTicks = 0;
      this.originalMerlinId = original.getUUID();
      this.hostilePlayers.addAll(original.hostilePlayers);
      if (this.getAttribute(Attributes.MAX_HEALTH) != null) {
         this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
      }

      this.setHealth(20.0F);
      if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
         this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.34);
      }

      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EXCALIBUR.get()));
      this.setTarget(target);
   }

   private void handleCloneEnd(ServerLevel serverLevel, boolean killed) {
      Vec3 pos = this.position();
      int count = killed ? 80 : 40;
      double baseY = pos.y + 0.8 + this.random.nextDouble() * 0.8;
      serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.x, baseY, pos.z, count, 1.5, 1.0, 1.5, 0.03);
      this.notifyOriginalOnCloneEnd(serverLevel);
      this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.clone_end", 20.0);
      this.discard();
   }

   private int countNearbyEnemies(ServerLevel serverLevel, double radius) {
      AABB box = new AABB(this.getX() - radius, this.getY() - 2.0, this.getZ() - radius, this.getX() + radius, this.getY() + 2.0, this.getZ() + radius);
      List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != this);
      int count = 0;

      for (LivingEntity entity : entities) {
         boolean hostileMob = entity instanceof Mob mob && mob.getTarget() == this;
         boolean hostilePlayer = entity instanceof Player player && this.hostilePlayers.contains(player.getUUID());
         if (hostileMob || hostilePlayer) {
            count++;
         }
      }

      return count;
   }

   private boolean isHighThreatGroup(ServerLevel serverLevel, double radius) {
      AABB box = new AABB(this.getX() - radius, this.getY() - 2.0, this.getZ() - radius, this.getX() + radius, this.getY() + 2.0, this.getZ() + radius);
      List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != this);
      boolean hasHostilePlayer = false;
      int highHealthHostileMobs = 0;

      for (LivingEntity entity : entities) {
         boolean hostileMob = entity instanceof Mob mob && mob.getTarget() == this;
         boolean hostilePlayer = entity instanceof Player player && this.hostilePlayers.contains(player.getUUID());
         if (hostilePlayer) {
            hasHostilePlayer = true;
         }

         if (hostileMob) {
            double maxHealth = entity.getAttributeValue(Attributes.MAX_HEALTH);
            if (maxHealth >= 200.0) {
               highHealthHostileMobs++;
            }
         }
      }

      return hasHostilePlayer || highHealthHostileMobs >= 2;
   }

   private void handleEnvironmentTeleport(ServerLevel serverLevel, LivingEntity currentTarget, boolean hasEnemy) {
      if (this.getY() < -64.0 && hasEnemy && currentTarget != null) {
         this.teleportNearTarget(serverLevel, currentTarget);
         this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.combat_recovery", 20.0);
      } else {
         BlockPos feetPos = this.blockPosition();
         BlockState feetState = serverLevel.getBlockState(feetPos);
         boolean inWaterOrLava = this.isInWaterOrBubble()
            || this.isInLava()
            || feetState.getFluidState().isSource() && (feetState.getFluidState().is(Fluids.WATER) || feetState.getFluidState().is(Fluids.LAVA));
         if (inWaterOrLava) {
            BlockPos safePos = this.findNearestSolidGround(serverLevel, feetPos, 12);
            if (safePos != null) {
               Vec3 from = this.position();
               double fromBaseY = from.y + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, from.x, fromBaseY, from.z, 40, 1.0, 0.9, 1.0, 0.03);
               this.randomTeleport(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5, true);
               Vec3 to = this.position();
               double toBaseY = to.y + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, to.x, toBaseY, to.z, 40, 1.0, 0.9, 1.0, 0.03);
               this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.15F);
               return;
            }
         }

         if (hasEnemy && currentTarget != null) {
            double distSqr = this.distanceToSqr(currentTarget);
            if (this.lastTargetDistSqr > 0.0) {
               double delta = distSqr - this.lastTargetDistSqr;
               if (delta >= 9.0 && distSqr > 25.0 && this.autoTeleportCooldown <= 0) {
                  this.teleportNearTarget(serverLevel, currentTarget);
                  this.autoTeleportCooldown = 80;
               }
            }

            if (distSqr > 400.0 && this.autoTeleportCooldown <= 0) {
               this.teleportNearTarget(serverLevel, currentTarget);
               this.autoTeleportCooldown = 80;
            }

            this.lastTargetDistSqr = distSqr;
         } else {
            this.lastTargetDistSqr = 0.0;
         }
      }
   }

   private BlockPos findNearestSolidGround(ServerLevel serverLevel, BlockPos origin, int maxRadius) {
      BlockPos bestPos = null;
      double bestDistSqr = Double.MAX_VALUE;

      for (int r = 1; r <= maxRadius; r++) {
         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               if (Math.abs(dx) == r || Math.abs(dz) == r) {
                  BlockPos columnBase = origin.offset(dx, 0, dz);

                  for (int dy = -4; dy <= 4; dy++) {
                     BlockPos feet = columnBase.offset(0, dy, 0);
                     BlockPos ground = feet.below();
                     BlockState groundState = serverLevel.getBlockState(ground);
                     if (!groundState.getCollisionShape(serverLevel, ground).isEmpty()
                        && groundState.getFluidState().isEmpty()
                        && !groundState.is(Blocks.CACTUS)
                        && !groundState.is(Blocks.MAGMA_BLOCK)
                        && !groundState.is(Blocks.SWEET_BERRY_BUSH)) {
                        BlockState feetState = serverLevel.getBlockState(feet);
                        BlockState headState = serverLevel.getBlockState(feet.above());
                        if (feetState.getCollisionShape(serverLevel, feet).isEmpty()
                           && headState.getCollisionShape(serverLevel, feet.above()).isEmpty()
                           && feetState.getFluidState().isEmpty()
                           && headState.getFluidState().isEmpty()) {
                           double distSqr = origin.distSqr(feet);
                           if (distSqr < bestDistSqr) {
                              bestDistSqr = distSqr;
                              bestPos = feet;
                           }
                        }
                     }
                  }
               }
            }
         }

         if (bestPos != null) {
            break;
         }
      }

      return bestPos;
   }

   private void teleportNearTarget(ServerLevel serverLevel, LivingEntity target) {
      Vec3 targetPos = target.position();

      for (int attempt = 0; attempt < 10; attempt++) {
         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double dist = 2.0 + this.random.nextDouble() * 2.0;
         double tx = targetPos.x + Math.cos(angle) * dist;
         double tz = targetPos.z + Math.sin(angle) * dist;
         double ty = targetPos.y;
         BlockPos basePos = BlockPos.containing(tx, ty, tz);
         BlockPos ground = this.findGround(serverLevel, basePos);
         if (ground != null) {
            BlockPos here = ground.above();
            BlockPos above = here.above();
            BlockState hereState = serverLevel.getBlockState(here);
            BlockState aboveState = serverLevel.getBlockState(above);
            if (hereState.getCollisionShape(serverLevel, here).isEmpty() && aboveState.getCollisionShape(serverLevel, above).isEmpty()) {
               Vec3 from = this.position();
               double fromBaseY = from.y + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, from.x, fromBaseY, from.z, 40, 1.0, 0.9, 1.0, 0.03);
               boolean teleported = this.randomTeleport(here.getX() + 0.5, here.getY(), here.getZ() + 0.5, true);
               if (teleported) {
                  Vec3 to = this.position();
                  double toBaseY = to.y + 0.8 + this.random.nextDouble() * 0.8;
                  serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, to.x, toBaseY, to.z, 40, 1.0, 0.9, 1.0, 0.03);
                  this.playSound(SoundEvents.PLAYER_TELEPORT, 1.0F, 1.15F);
                  this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.teleport_near", 12.0);
               }

               return;
            }
         }
      }
   }

   private void startAvalonField(ServerLevel serverLevel) {
      this.avalonActive = true;
      this.avalonTicks = 0;
      this.avalonCooldown = 1200;
      this.avalonCenter = this.blockPosition();
      this.avalonLastInsidePositions.clear();
      this.avalonOriginalBlocks.clear();
      this.avalonOriginalBlockEntities.clear();
      this.avalonRestorationQueue.clear();
      this.avalonClearSurfaceQueue.clear();
      this.avalonFlowerLayerY = Integer.MIN_VALUE;
      this.avalonRestoreBlocksPerTick = 400;
      this.avalonCurrentRadius = 0.0;
      this.avalonPrevRadius = 0.0;
      serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.0, this.getZ(), 120, 2.5, 1.5, 2.5, 0.04);
      this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_start", 24.0);
   }

   private void tickAvalonField(ServerLevel serverLevel) {
      if (this.avalonCenter == null) {
         this.avalonActive = false;
         this.avalonLastInsidePositions.clear();
         this.avalonOriginalBlocks.clear();
      } else {
         this.avalonTicks++;
         if (this.avalonTicks >= 600) {
            if (this.avalonRestorationQueue.isEmpty() && !this.avalonOriginalBlocks.isEmpty()) {
               this.startAvalonRestoration();
            }

            this.processAvalonRestoration(serverLevel);
         } else {
            double maxRadius = 25.0;
            this.avalonPrevRadius = this.avalonCurrentRadius;
            double expandDurationTicks = 100.0;
            if (this.avalonTicks <= expandDurationTicks) {
               this.avalonCurrentRadius = maxRadius * (this.avalonTicks / expandDurationTicks);
            } else {
               this.avalonCurrentRadius = maxRadius;
            }

            double currentRadius = this.avalonCurrentRadius;
            BlockPos center = this.avalonCenter;
            int centerY = center.getY();
            int floorY = centerY - 1;
            double cx = center.getX() + 0.5;
            double cy = centerY;
            double cz = center.getZ() + 0.5;
            int r = (int)Math.ceil(currentRadius);
            int prevR = (int)Math.floor(this.avalonPrevRadius);
            if (r > prevR) {
               for (int x = -r; x <= r; x++) {
                  for (int z = -r; z <= r; z++) {
                     double distSqr = x * x + z * z;
                     if (!(distSqr > currentRadius * currentRadius)) {
                        BlockPos pos = center.offset(x, 0, z);
                        BlockPos floorPos = new BlockPos(pos.getX(), floorY, pos.getZ());
                        if (!this.avalonOriginalBlocks.containsKey(floorPos)) {
                           BlockState oldState = serverLevel.getBlockState(floorPos);
                           if (oldState.getDestroySpeed(serverLevel, floorPos) >= 0.0F) {
                              this.avalonOriginalBlocks.put(floorPos, oldState);
                              BlockEntity be = serverLevel.getBlockEntity(floorPos);
                              if (be != null) {
                                 CompoundTag nbt = be.saveWithFullMetadata(serverLevel.registryAccess());
                                 this.avalonOriginalBlockEntities.put(floorPos, nbt);
                              }

                              serverLevel.setBlock(floorPos, Blocks.GRASS_BLOCK.defaultBlockState(), 18);
                           }
                        }

                        BlockPos basePos = floorPos.below();
                        if (!this.avalonOriginalBlocks.containsKey(basePos)) {
                           BlockState oldState = serverLevel.getBlockState(basePos);
                           if (oldState.getDestroySpeed(serverLevel, basePos) >= 0.0F) {
                              this.avalonOriginalBlocks.put(basePos, oldState);
                              BlockEntity be = serverLevel.getBlockEntity(basePos);
                              if (be != null) {
                                 CompoundTag nbt = be.saveWithFullMetadata(serverLevel.registryAccess());
                                 this.avalonOriginalBlockEntities.put(basePos, nbt);
                              }

                              serverLevel.setBlock(basePos, Blocks.DIRT.defaultBlockState(), 18);
                           }
                        }

                        int maxY = (int)Math.sqrt(Math.max(0.0, maxRadius * maxRadius - distSqr));

                        for (int yOffset = 0; yOffset <= maxY; yOffset++) {
                           BlockPos airPos = new BlockPos(pos.getX(), centerY + yOffset, pos.getZ());
                           if (!this.avalonOriginalBlocks.containsKey(airPos)) {
                              BlockState oldState = serverLevel.getBlockState(airPos);
                              if (oldState.getBlock() instanceof UBWWeaponBlock) {
                                 continue;
                              }

                              if (!oldState.isAir() && oldState.getDestroySpeed(serverLevel, airPos) >= 0.0F) {
                                 this.avalonOriginalBlocks.put(airPos, oldState);
                                 BlockEntity be = serverLevel.getBlockEntity(airPos);
                                 if (be != null) {
                                    CompoundTag nbt = be.saveWithFullMetadata(serverLevel.registryAccess());
                                    this.avalonOriginalBlockEntities.put(airPos, nbt);
                                 }

                                 serverLevel.setBlock(airPos, Blocks.AIR.defaultBlockState(), 18);
                              }
                           }

                           BlockPos flowerPos = new BlockPos(pos.getX(), centerY, pos.getZ());
                           BlockState flowerState = serverLevel.getBlockState(flowerPos);
                           if (flowerState.isAir() && this.random.nextFloat() < 0.35F) {
                              int choice = this.random.nextInt(5);
                              Block flower;
                              if (choice == 0) {
                                 flower = Blocks.PINK_PETALS;
                              } else if (choice == 1) {
                                 flower = Blocks.DANDELION;
                              } else if (choice == 2) {
                                 flower = Blocks.POPPY;
                              } else if (choice == 3) {
                                 flower = Blocks.AZURE_BLUET;
                              } else {
                                 flower = Blocks.BLUE_ORCHID;
                              }

                              serverLevel.setBlock(flowerPos, flower.defaultBlockState(), 18);
                           }
                        }
                     }
                  }
               }
            }

            double ringBaseY = cy + 1.0 + this.random.nextDouble() * 2.0;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, cx, ringBaseY, cz, 120, currentRadius, 2.0, currentRadius, 0.03);
            if (this.avalonTicks % 40 == 0) {
               this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_expand", 24.0, 0.3F);
            }

            if (this.avalonTicks % 10 == 0) {
               AABB box = new AABB(cx - currentRadius, cy - 1.0, cz - currentRadius, cx + currentRadius, cy + 3.0, cz + currentRadius);

               for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive())) {
                  boolean isMerlin = entity == this;
                  boolean isClone = entity instanceof MerlinEntity m && m.isClone;
                  boolean hostileMob = entity instanceof Mob mob && mob.getTarget() == this;
                  boolean hostilePlayer = entity instanceof Player player && this.hostilePlayers.contains(player.getUUID());
                  if (isMerlin || isClone || entity instanceof Player playerx && !this.hostilePlayers.contains(playerx.getUUID())) {
                     entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
                     entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, true));
                     entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, true));
                  } else if (hostileMob || hostilePlayer) {
                     int type = this.random.nextInt(4);
                     if (type == 0) {
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                     } else if (type == 1) {
                        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                     } else if (type == 2) {
                        entity.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                     } else {
                        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                     }
                  }
               }
            }
         }
      }
   }

   private void startAvalonRestoration() {
      if (this.avalonCenter != null && !this.avalonOriginalBlocks.isEmpty()) {
         this.avalonRestorationQueue = new ArrayList<>(this.avalonOriginalBlocks.keySet());
         BlockPos center = this.avalonCenter;
         this.avalonRestorationQueue.sort((p1, p2) -> {
            double d1 = p1.distSqr(center);
            double d2 = p2.distSqr(center);
            return Double.compare(d1, d2);
         });
         int total = this.avalonRestorationQueue.size();
         int durationTicks = 100;
         this.avalonRestoreBlocksPerTick = Math.max(1, (int)Math.ceil((double)total / durationTicks));
         this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_restore", 24.0);
      } else {
         this.avalonActive = false;
         this.avalonCenter = null;
         this.avalonOriginalBlockEntities.clear();
         this.avalonRestorationQueue.clear();
         this.avalonClearSurfaceQueue.clear();
         this.avalonLastInsidePositions.clear();
      }
   }

   private void castMentalInfluencePulse(ServerLevel serverLevel) {
      double radius = this.avalonActive ? 24.0 : 16.0;
      AABB box = new AABB(this.getX() - radius, this.getY() - 2.0, this.getZ() - radius, this.getX() + radius, this.getY() + 2.0, this.getZ() + radius);
      List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive());
      this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.mental_pulse", radius + 4.0, 0.8F);
      boolean bonus = this.isIllusionInvisible || this.activeCloneCount >= 3;
      Iterator var7 = entities.iterator();

      while (true) {
         LivingEntity entity;
         double baseY;
         UUID id;
         while (true) {
            if (!var7.hasNext()) {
               return;
            }

            entity = (LivingEntity)var7.next();
            if (entity != this) {
               boolean hostileMob = entity instanceof Mob mob && (mob.getTarget() == this || mob.getTarget() instanceof MerlinEntity m && m.isClone);
               boolean hostilePlayer = entity instanceof Player player && this.hostilePlayers.contains(player.getUUID());
               if (hostileMob || hostilePlayer) {
                  id = entity.getUUID();
                  Integer last = this.mentalAffectWindow.get(id);
                  if (last == null || this.tickCount - last >= 600) {
                     double dx = entity.getX() - this.getX();
                     double dz = entity.getZ() - this.getZ();
                     double dist = Math.sqrt(dx * dx + dz * dz);
                     double norm = Math.min(1.0, dist / radius);
                     double baseChance = hostileMob ? 0.3 + this.random.nextDouble() * 0.3 : 0.25 + this.random.nextDouble() * 0.2;
                     double chance = baseChance * (1.0 - 0.5 * norm);
                     if (this.avalonActive) {
                        chance += 0.15;
                     }

                     if (bonus) {
                        chance += 0.15;
                     }

                     if (entity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                        chance -= 0.2;
                     }

                     if (entity instanceof Player p && p.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                        chance -= 0.1;
                     }

                     chance = Math.max(0.0, Math.min(0.95, chance));
                     boolean applied = this.random.nextDouble() < chance;
                     baseY = entity.getY() + entity.getBbHeight() * 0.5;
                     if (applied) {
                        int dur = 80 + this.random.nextInt(81);
                        if (hostileMob && entity instanceof Mob mobx) {
                           boolean retarget = this.random.nextDouble() < 0.5;
                           if (!retarget) {
                              mobx.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
                              mobx.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 0));
                              break;
                           }

                           MerlinEntity nearestClone = null;
                           double best = Double.MAX_VALUE;

                           for (MerlinEntity c : serverLevel.getEntitiesOfClass(
                              MerlinEntity.class, box, e -> e.isAlive() && e.isClone && this.getUUID().equals(e.originalMerlinId)
                           )) {
                              double d = c.distanceToSqr(entity);
                              if (d < best) {
                                 best = d;
                                 nearestClone = c;
                              }
                           }

                           Player nearestFriend = null;
                           double bestP = Double.MAX_VALUE;

                           for (Player p : serverLevel.getEntitiesOfClass(Player.class, box, px -> px.isAlive() && !this.hostilePlayers.contains(px.getUUID()))) {
                              double d = p.distanceToSqr(entity);
                              if (d < bestP) {
                                 bestP = d;
                                 nearestFriend = p;
                              }
                           }

                           if (nearestClone != null) {
                              mobx.setTarget(nearestClone);
                           } else if (nearestFriend != null) {
                              mobx.setTarget(nearestFriend);
                           } else {
                              mobx.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0));
                              mobx.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 0));
                           }
                           break;
                        }

                        if (!(entity instanceof Player playerx)) {
                           break;
                        }

                        if (!playerx.isSpectator()) {
                           playerx.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 0, true, true));
                           playerx.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 0, true, true));
                           break;
                        }
                     } else {
                        serverLevel.sendParticles(ParticleTypes.END_ROD, entity.getX(), baseY, entity.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                     }
                  }
               }
            }
         }

         serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), baseY + 0.2, entity.getZ(), 12, 0.5, 0.4, 0.5, 0.0);
         serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, entity.getX(), baseY, entity.getZ(), 16, 0.8, 0.6, 0.8, 0.02);
         this.mentalAffectWindow.put(id, this.tickCount);
      }
   }

   private void processAvalonRestoration(ServerLevel serverLevel) {
      if (this.avalonRestorationQueue.isEmpty()) {
         if (this.avalonOriginalBlocks.isEmpty()) {
            if (this.avalonCenter != null) {
               BlockPos centerFinish = this.avalonCenter;
               double baseY = centerFinish.getY() + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, centerFinish.getX() + 0.5, baseY, centerFinish.getZ() + 0.5, 400, 3.0, 1.8, 3.0, 0.04);
            }

            this.avalonActive = false;
            this.avalonCenter = null;
            this.avalonLastInsidePositions.clear();
            this.avalonOriginalBlockEntities.clear();
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_end", 24.0);
         }
      } else {
         int blocksPerTick = this.avalonRestoreBlocksPerTick;
         int processed = 0;
         if (this.avalonCenter != null) {
            double maxRadius = 25.0;
            AABB box = new AABB(
               this.avalonCenter.getX() + 0.5 - maxRadius,
               this.avalonCenter.getY() - 8.0,
               this.avalonCenter.getZ() + 0.5 - maxRadius,
               this.avalonCenter.getX() + 0.5 + maxRadius,
               this.avalonCenter.getY() + 8.0,
               this.avalonCenter.getZ() + 0.5 + maxRadius
            );

            for (ItemEntity item : serverLevel.getEntitiesOfClass(ItemEntity.class, box)) {
               item.discard();
            }

            int centerY = this.avalonCenter.getY();
            int r = (int)Math.ceil(maxRadius);

            for (int x = -r; x <= r; x++) {
               for (int z = -r; z <= r; z++) {
                  double distSqr = x * x + z * z;
                  if (!(distSqr > maxRadius * maxRadius)) {
                     BlockPos flowerPos = new BlockPos(this.avalonCenter.getX() + x, centerY, this.avalonCenter.getZ() + z);
                     BlockState current = serverLevel.getBlockState(flowerPos);
                     if (current.is(BlockTags.FLOWERS)) {
                        serverLevel.setBlock(flowerPos, Blocks.AIR.defaultBlockState(), 18);
                     }
                  }
               }
            }
         }

         for (; processed < blocksPerTick && !this.avalonRestorationQueue.isEmpty(); processed++) {
            int idx = this.avalonRestorationQueue.size() - 1;
            BlockPos pos = this.avalonRestorationQueue.remove(idx);
            BlockState state = this.avalonOriginalBlocks.remove(pos);
            if (state != null) {
               serverLevel.setBlock(pos, state, 18);
               CompoundTag nbt = this.avalonOriginalBlockEntities.remove(pos);
               if (nbt != null) {
                  BlockEntity be = serverLevel.getBlockEntity(pos);
                  if (be != null) {
                     be.loadWithComponents(nbt, serverLevel.registryAccess());
                  }
               }
            }
         }

         if (this.avalonRestorationQueue.isEmpty() && this.avalonOriginalBlocks.isEmpty()) {
            if (this.avalonCenter != null) {
               BlockPos centerFinish = this.avalonCenter;
               double baseY = centerFinish.getY() + 0.8 + this.random.nextDouble() * 0.8;
               serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, centerFinish.getX() + 0.5, baseY, centerFinish.getZ() + 0.5, 400, 3.0, 1.8, 3.0, 0.04);
            }

            this.avalonActive = false;
            this.avalonCenter = null;
            this.avalonLastInsidePositions.clear();
            this.avalonOriginalBlockEntities.clear();
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_end", 24.0);
         }
      }
   }

   private BlockPos findGround(ServerLevel serverLevel, BlockPos base) {
      MutableBlockPos mutable = new MutableBlockPos();

      for (int dy = -3; dy <= 3; dy++) {
         mutable.set(base.getX(), base.getY() + dy, base.getZ());
         BlockState below = serverLevel.getBlockState(mutable);
         BlockState above = serverLevel.getBlockState(mutable.above());
         if (!below.isAir() && above.isAir()) {
            return mutable.immutable();
         }
      }

      return null;
   }

   private void tickAvalonBarrier(ServerLevel serverLevel) {
      if (this.avalonActive && this.avalonCenter != null) {
         double radius = this.avalonCurrentRadius;
         if (!(radius <= 1.0)) {
            BlockPos center = this.avalonCenter;
            double cx = center.getX() + 0.5;
            double cz = center.getZ() + 0.5;
            double extra = 2.0;
            AABB box = new AABB(cx - radius - extra, center.getY() - 8.0, cz - radius - extra, cx + radius + extra, center.getY() + 8.0, cz + radius + extra);
            List<Player> players = serverLevel.getEntitiesOfClass(Player.class, box, p -> p.isAlive() && !p.isSpectator());
            Set<UUID> present = new HashSet<>();

            for (Player player : players) {
               if (player instanceof ServerPlayer serverPlayer) {
                  UUID id = serverPlayer.getUUID();
                  present.add(id);
                  double dx = serverPlayer.getX() - cx;
                  double dz = serverPlayer.getZ() - cz;
                  double distSqr = dx * dx + dz * dz;
                  boolean inside = distSqr <= radius * radius;
                  if (inside) {
                     this.avalonLastInsidePositions.put(id, serverPlayer.blockPosition());
                  } else {
                     BlockPos last = this.avalonLastInsidePositions.get(id);
                     if (last != null) {
                        BlockPos ground = this.findGround(serverLevel, last);
                        BlockPos target = ground != null ? ground.above() : last;
                        double x = target.getX() + 0.5;
                        double y = target.getY();
                        double z = target.getZ() + 0.5;
                        serverPlayer.teleportTo(x, y, z);
                        serverPlayer.setDeltaMovement(Vec3.ZERO);
                        double baseY = y + 0.8 + this.random.nextDouble() * 0.8;
                        serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, x, baseY, z, 40, 1.0, 0.9, 1.0, 0.03);
                        serverLevel.playSound(null, x, y, z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.1F);
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, true));
                        serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, true));
                        this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_barrier", 16.0, 1.0F);
                        this.avalonLastInsidePositions.put(id, target);
                     }
                  }
               }
            }

            this.avalonLastInsidePositions.keySet().removeIf(idx -> !present.contains(idx));
         }
      }
   }

   private void notifyOriginalOnCloneEnd(ServerLevel serverLevel) {
      if (this.originalMerlinId != null) {
         if (serverLevel.getEntity(this.originalMerlinId) instanceof MerlinEntity original) {
            original.onCloneFinished();
         }
      }
   }

   private void onCloneFinished() {
      if (this.activeCloneCount > 0) {
         this.activeCloneCount--;
      }

      if (this.activeCloneCount <= 0) {
         this.isIllusionInvisible = false;
         this.removeEffect(MobEffects.INVISIBILITY);
      }
   }

   private void performExcaliburSlash(ServerLevel serverLevel) {
      Vec3 lookVec = this.getLookAngle();
      Vec3 center = this.position().add(0.0, this.getEyeHeight() * 0.5, 0.0);

      for (double r = 1.5; r <= 4.0; r += 0.5) {
         for (double theta = -Math.PI / 3; theta <= Math.PI / 3; theta += Math.PI / 18) {
            double x = lookVec.x * Math.cos(theta) - lookVec.z * Math.sin(theta);
            double z = lookVec.x * Math.sin(theta) + lookVec.z * Math.cos(theta);
            Vec3 offset = new Vec3(x, 0.0, z).normalize().scale(r);
            double px = center.x + offset.x;
            double py = center.y;
            double pz = center.z + offset.z;
            int sweepCount = 1 + this.random.nextInt(2);
            int critCount = this.random.nextInt(2);
            int sakuraCount = 2 + this.random.nextInt(4);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, sweepCount, 0.1, 0.1, 0.1, 0.0);
            if (critCount > 0) {
               serverLevel.sendParticles(ParticleTypes.CRIT, px, py + 0.1, pz, critCount, 0.1, 0.1, 0.1, 0.0);
            }

            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, px, py + 0.2, pz, sakuraCount, 0.6, 0.3, 0.6, 0.02);
         }
      }

      Vec3 horizontalLook = new Vec3(lookVec.x, 0.0, lookVec.z).normalize();
      Vec3 boxCenter = center.add(horizontalLook.scale(2.5));
      double halfWidth = 3.0;
      double halfHeight = 1.5;
      double halfDepth = 2.5;
      AABB slashBox = new AABB(
         boxCenter.x - halfWidth, boxCenter.y - halfHeight, boxCenter.z - halfDepth, boxCenter.x + halfWidth, boxCenter.y + halfHeight, boxCenter.z + halfDepth
      );

      for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, slashBox, e -> e != this && e.isAlive())) {
         if (!EntityUtils.isSpectatorPlayer(target)
            && !(target instanceof MerlinEntity)
            && !(target instanceof Player player && !this.hostilePlayers.contains(player.getUUID()))) {
            target.invulnerableTime = 0;
            float baseDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float damage = baseDamage + 16.0F;
            target.hurt(this.damageSources().mobAttack(this), damage);
            target.invulnerableTime = 0;
            Vec3 knockback = horizontalLook.scale(1.2);
            target.push(knockback.x, 0.4, knockback.z);
         }
      }

      this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 0, event -> event.setAndContinue(RawAnimation.begin().thenLoop("animation"))));
   }

   protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (itemstack.isEmpty()) {
         return super.mobInteract(pPlayer, pHand);
      } else if (this.isClone) {
         return InteractionResult.PASS;
      } else if (this.getTarget() != null) {
         return InteractionResult.PASS;
      } else {
         this.getLookControl().setLookAt(pPlayer, 30.0F, 30.0F);
         if (pPlayer instanceof ServerPlayer serverPlayer && !this.level().isClientSide) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)serverPlayer.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (vars.merlin_favor <= -5) {
               vars.syncPlayerVariables(serverPlayer);
               return InteractionResult.PASS;
            }

            vars.merlin_talk_counter++;
            if (vars.merlin_talk_counter >= 2 && vars.merlin_favor < 5) {
               vars.merlin_favor = Math.min(5, vars.merlin_favor + 1);
               vars.merlin_talk_counter = 0;
            }

            vars.syncPlayerVariables(serverPlayer);
         }

         if (itemstack.getItem() instanceof SwordItem) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_sword", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.getItem() instanceof ShieldItem) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_shield", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.getItem() instanceof BedItem) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_bed", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(Items.BOOK) || itemstack.is(Items.WRITABLE_BOOK)) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_book", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(Items.CLOCK)) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_clock", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(Items.MAP) || itemstack.is(Items.FILLED_MAP)) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_map", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(Items.COMPASS)) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_compass", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(Items.CHERRY_SAPLING) || itemstack.is(Items.PINK_PETALS) || itemstack.is(Items.PEONY) || itemstack.is(Items.AZURE_BLUET)) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_flower", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.AVALON.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.avalon_start", 24.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.EXCALIBUR.get()) || itemstack.is(ModItems.EXCALIBUR2.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.trace_on", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.MYSTIC_EYES_OF_DEATH_PERCEPTION.get())
            || itemstack.is(ModItems.MYSTIC_EYES_OF_DEATH_PERCEPTION_NOBLE_COLOR.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_mystic_eyes", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.getItem() instanceof MagicScrollItem) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_magic_scroll", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.TSUMUKARI_MURAMASA.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_muramasa", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.getItem() instanceof CarvedGemItem || itemstack.getItem() instanceof FullManaCarvedGemItem) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_carved_gem", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.RANDOM_GEM.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_random_gem", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.CHISEL.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_chisel", 20.0);
            return InteractionResult.SUCCESS;
         } else if (itemstack.is(ModItems.HOLY_SHROUD.get())) {
            this.broadcastToNearbyPlayers("entity.typemoonworld.merlin.speech.see_holy_shroud", 20.0);
            return InteractionResult.SUCCESS;
         } else {
            return super.mobInteract(pPlayer, pHand);
         }
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   static {
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.idle", 2);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.greet", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_sword", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_shield", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_book", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_bed", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.environment_night", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.environment_thunder", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.cherry_blossom", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.teleport", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.teleport_near", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.support_aura", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.mental_pulse", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.prank_positive", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.prank_negative", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.hostile_player", 11);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.buff_support_high", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.guard_summon", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.crowded", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.player_looking", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_compass", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_map", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_clock", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.see_flower", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_zombie", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_skeleton", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_creeper", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_enderman", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_player_taunt", 7);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_boss_taunt", 5);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.target_summon_taunt", 5);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.avalon_barrier", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.death", 1);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.combat_dodge", 9);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.combat_recovery", 9);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.combat_avalon_hit", 9);
      SPEECH_VARIANTS.put("entity.typemoonworld.merlin.speech.combat_death", 9);
   }
}
