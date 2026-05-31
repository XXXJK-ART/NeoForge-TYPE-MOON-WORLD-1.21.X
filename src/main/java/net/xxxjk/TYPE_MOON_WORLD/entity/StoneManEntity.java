package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class StoneManEntity extends PathfinderMob implements GeoEntity {
   private static final EntityDataAccessor<Integer> MIMIC_BLOCK_ID = SynchedEntityData.defineId(StoneManEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ATTACK_TYPE = SynchedEntityData.defineId(StoneManEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ATTACK_ANIM_TICKS = SynchedEntityData.defineId(StoneManEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> TNT_FUSE_SYNC = SynchedEntityData.defineId(StoneManEntity.class, EntityDataSerializers.INT);
   private static final int ATTACK_NONE = 0;
   private static final int ATTACK_UPPERCUT = 1;
   private static final int ATTACK_GROUND_SLAM = 2;
   private static final int UPPERCUT_ANIM_TICKS = 11;
   private static final int SLAM_ANIM_TICKS = 18;
   private static final int SLAM_COOLDOWN_TICKS = 70;
   private static final float UPPERCUT_PUSH_Y = 0.64F;
   private static final float SLAM_RADIUS = 3.8F;
   private static final int SLAM_SPIKE_COUNT = 10;
   private static final int WALK_ANIM_GRACE_TICKS = 6;
   private static final double WALK_ANIM_MOVE_THRESHOLD_SQR = 2.5E-5;
   private static final int MAGMA_AURA_INTERVAL_TICKS = 20;
   private static final int MAGMA_FIRE_SPREAD_INTERVAL_TICKS = 24;
   private static final int ICE_MELT_INTERVAL_TICKS = 40;
   private static final int WATER_TRAIT_INTERVAL_TICKS = 20;
   private static final int PLANT_REGEN_INTERVAL_TICKS = 40;
   private static final int HONEY_AURA_INTERVAL_TICKS = 24;
   private static final int SPONGE_ABSORB_INTERVAL_TICKS = 30;
   private static final int TNT_FUSE_TICKS = 40;
   private static final int REDSTONE_TNT_LINK_INTERVAL_TICKS = 8;
   private static final double REDSTONE_TNT_LINK_RADIUS = 3.4;
   private static final int HONEY_BOTTLE_COOLDOWN_TICKS = 80;
   private static final int REDSTONE_OVERCLOCK_TICKS = 140;
   private static final int REDSTONE_LIGHTNING_OVERCLOCK_TICKS = 240;
   private static final int TNT_LIGHTNING_FUSE_TICKS = 16;
   private static final double EMPTY_HAND_BOUNCE_VELOCITY = 0.82;
   private static final int COPPER_OXIDIZE_INTERVAL_TICKS = 120;
   private static final float COPPER_OXIDIZE_CHANCE = 0.18F;
   private static final float TNT_CRITICAL_HEALTH_RATIO = 0.25F;
   private static final float TNT_CRITICAL_HEALTH_ABS = 2.5F;
   private static final float WOOD_FIRE_DAMAGE_MULTIPLIER = 1.65F;
   private static final float ICE_FIRE_DAMAGE_MULTIPLIER = 1.35F;
   private static final double DEFAULT_MOVE_SPEED = 0.18;
   private static final double DEFAULT_ARMOR = 8.0;
   private static final double DEFAULT_KNOCKBACK_RESIST = 0.6;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private boolean mimicBlockInitialized = false;
   private int slamCooldownTicks = 0;
   private int walkAnimGraceTicks = 0;
   private int lastAppliedTraitBlockId = Integer.MIN_VALUE;
   private int tntFuseTicks = -1;
   private int honeyBottleCooldownTicks = 0;

   public StoneManEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.85));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this, new Class[0]));
   }

   protected void playStepSound(BlockPos pos, BlockState blockState) {
      BlockState mimicState = this.getMimicBlockState();
      SoundType mimicSoundType = this.getMimicSoundType(mimicState);
      this.playSound(
         mimicSoundType.getStepSound(),
         Math.min(1.8F, Math.max(0.35F, mimicSoundType.getVolume() * 1.18F)),
         Math.max(0.22F, mimicSoundType.getPitch() * (0.58F + this.random.nextFloat() * 0.08F))
      );
      this.playSound(SoundEvents.IRON_GOLEM_STEP, 0.52F, 0.52F + this.random.nextFloat() * 0.06F);
      if (this.level() instanceof ServerLevel serverLevel) {
         BlockState var8 = blockState;
         if (blockState.isAir()) {
            var8 = this.level().getBlockState(pos.below());
         }

         if (var8.isAir()) {
            var8 = this.getMimicBlockState();
         }

         if (!var8.isAir()) {
            BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, var8);
            serverLevel.sendParticles(dust, this.getX(), this.getY() + 0.08, this.getZ(), 7, 0.24, 0.02, 0.24, 0.015);
         }

         serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.1, this.getZ(), 3, 0.18, 0.01, 0.18, 0.0);
      }
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.getMimicSoundType(this.getMimicBlockState()).getHitSound();
   }

   protected SoundEvent getDeathSound() {
      return this.getMimicSoundType(this.getMimicBlockState()).getBreakSound();
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 60.0)
         .add(Attributes.MOVEMENT_SPEED, 0.18)
         .add(Attributes.ATTACK_DAMAGE, 8.0)
         .add(Attributes.ARMOR, 8.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
         .add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(MIMIC_BLOCK_ID, BuiltInRegistries.BLOCK.getId(Blocks.STONE));
      builder.define(ATTACK_TYPE, 0);
      builder.define(ATTACK_ANIM_TICKS, 0);
      builder.define(TNT_FUSE_SYNC, -1);
   }

   public void tick() {
      super.tick();
      this.updateWalkAnimState();
      if (!this.level().isClientSide && !this.mimicBlockInitialized) {
         this.setMimicBlockFromTerrain();
         this.mimicBlockInitialized = true;
      }

      if (!this.level().isClientSide) {
         if (this.honeyBottleCooldownTicks > 0) {
            this.honeyBottleCooldownTicks--;
         }

         this.applyMimicAttributeProfile();
         this.tickMimicTraitEffects();
         int animTicks = (Integer)this.entityData.get(ATTACK_ANIM_TICKS);
         if (animTicks > 0) {
            this.entityData.set(ATTACK_ANIM_TICKS, --animTicks);
            if (animTicks <= 0) {
               this.entityData.set(ATTACK_TYPE, 0);
            }
         }

         if (this.slamCooldownTicks > 0) {
            this.slamCooldownTicks--;
         }
      }
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
   ) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      this.setMimicBlockFromTerrain();
      this.mimicBlockInitialized = true;
      return data;
   }

   public void setMimicBlockFromTerrain() {
      BlockPos belowPos = this.blockPosition().below();
      BlockState belowState = this.level().getBlockState(belowPos);
      BlockState picked = belowState.isAir() ? this.level().getBlockState(this.blockPosition()) : belowState;
      if (picked.isAir()) {
         picked = Blocks.STONE.defaultBlockState();
      }

      this.setMimicBlock(picked.getBlock());
   }

   public void setMimicBlockFromSpawnState(@Nullable BlockState clickedState) {
      if (clickedState != null && !clickedState.isAir()) {
         this.setMimicBlock(clickedState.getBlock());
         this.mimicBlockInitialized = true;
      } else {
         this.setMimicBlockFromTerrain();
         this.mimicBlockInitialized = true;
      }
   }

   public void setMimicBlock(Block block) {
      Block safeBlock = block == null ? Blocks.STONE : block;
      int blockId = BuiltInRegistries.BLOCK.getId(safeBlock);
      if (blockId < 0) {
         blockId = BuiltInRegistries.BLOCK.getId(Blocks.STONE);
      }

      this.entityData.set(MIMIC_BLOCK_ID, blockId);
      this.lastAppliedTraitBlockId = Integer.MIN_VALUE;
      if (!this.isTntMimic(this.getMimicBlockState())) {
         this.setTntFuseTicks(-1);
      }
   }

   public Block getMimicBlock() {
      Block block = (Block)BuiltInRegistries.BLOCK.byId((Integer)this.entityData.get(MIMIC_BLOCK_ID));
      return block == Blocks.AIR ? Blocks.STONE : block;
   }

   public BlockState getMimicBlockState() {
      return this.getMimicBlock().defaultBlockState();
   }

   public int getAttackType() {
      return (Integer)this.entityData.get(ATTACK_TYPE);
   }

   public int getAttackAnimTicks() {
      return (Integer)this.entityData.get(ATTACK_ANIM_TICKS);
   }

   public int getSyncedTntFuseTicks() {
      return (Integer)this.entityData.get(TNT_FUSE_SYNC);
   }

   public float getTntFlashAlpha(float partialTick) {
      if (!this.isTntMimic(this.getMimicBlockState())) {
         return 0.0F;
      } else {
         int fuse = this.getSyncedTntFuseTicks();
         if (fuse < 0) {
            return 0.0F;
         } else {
            float progress = 1.0F - (fuse - partialTick) / 40.0F;
            progress = Mth.clamp(progress, 0.0F, 1.0F);
            int interval = fuse > 30 ? 7 : (fuse > 20 ? 5 : (fuse > 10 ? 3 : 1));
            return (fuse / interval & 1) != 0 ? 0.0F : 0.14F + 0.72F * progress;
         }
      }
   }

   public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
      return this.isFallResetMimic(this.getMimicBlockState()) ? false : super.causeFallDamage(fallDistance, damageMultiplier, source);
   }

   public boolean hurt(DamageSource source, float amount) {
      BlockState mimicState = this.getMimicBlockState();
      if (source.is(DamageTypeTags.IS_FIRE)) {
         if (this.isFireproofMimic(mimicState)) {
            this.clearFire();
            return false;
         }

         if (this.isWoodLikeMimic(mimicState)) {
            amount *= 1.65F;
         }

         if (this.isIceMimic(mimicState)) {
            amount *= 1.35F;
         }
      } else {
         if (source.is(DamageTypeTags.IS_FREEZING) && this.isIceMimic(mimicState)) {
            return false;
         }

         if (source.is(DamageTypeTags.IS_DROWNING) && this.isWaterMimic(mimicState)) {
            return false;
         }
      }

      if (source.is(DamageTypeTags.IS_PROJECTILE) && this.isMetalMimic(mimicState)) {
         amount *= 0.72F;
      }

      if ((source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_PROJECTILE)) && this.isGlassMimic(mimicState)) {
         amount *= 1.35F;
      }

      if (source.is(DamageTypeTags.IS_LIGHTNING) && this.isMetalMimic(mimicState)) {
         amount *= 0.5F;
      }

      if (this.isCactusMimic(mimicState) && source.getEntity() instanceof LivingEntity attacker && attacker != this && !source.is(DamageTypeTags.IS_PROJECTILE)
         )
       {
         attacker.hurt(this.damageSources().cactus(), 2.0F);
      }

      if (this.isTntMimic(mimicState) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.primeTntFuse();
      }

      boolean hurt = super.hurt(source, amount);
      if (hurt && this.isFireproofMimic(mimicState) && this.isOnFire()) {
         this.clearFire();
      }

      return hurt;
   }

   public void thunderHit(ServerLevel level, LightningBolt lightning) {
      BlockState mimicState = this.getMimicBlockState();
      Block mimicBlock = mimicState.getBlock();
      if (this.isCopperMimic(mimicState)) {
         Block previous = (Block)WeatheringCopper.getPrevious(mimicBlock).orElse(null);
         if (previous != null && previous != mimicBlock) {
            this.setMimicBlock(previous);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.38, 0.42, 0.38, 0.0);
            this.playSound(SoundEvents.AXE_SCRAPE, 0.85F, 1.08F + this.random.nextFloat() * 0.08F);
         }
      }

      if (this.isRedstoneMimic(mimicState)) {
         this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 2, true, false));
         this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 1, true, false));
         this.tickRedstoneTntLink(level);
         level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 14, 0.45, 0.5, 0.45, 0.0);
      }

      if (this.isTntMimic(mimicState)) {
         boolean newlyPrimed = this.tntFuseTicks < 0;
         if (newlyPrimed || this.tntFuseTicks > 16) {
            this.setTntFuseTicks(16);
            if (newlyPrimed) {
               this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 1.15F + this.random.nextFloat() * 0.08F);
            }
         }
      }

      super.thunderHit(level, lightning);
      if (this.isFireproofMimic(this.getMimicBlockState()) && this.isOnFire()) {
         this.clearFire();
      }
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockState mimicState = this.getMimicBlockState();
      if (this.isTntMimic(mimicState) && stack.is(Items.SHEARS) && this.tntFuseTicks >= 0) {
         if (!this.level().isClientSide) {
            this.setTntFuseTicks(-1);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            this.playSound(SoundEvents.SHEEP_SHEAR, 0.8F, 0.9F + this.random.nextFloat() * 0.1F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.7F, 1.2F);
            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.05, this.getZ(), 8, 0.22, 0.16, 0.22, 0.01);
            }
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (!this.isTntMimic(mimicState) || !stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
         if (this.isMagmaMimic(mimicState) && stack.is(Items.WATER_BUCKET)) {
            if (!this.level().isClientSide) {
               this.setMimicBlock(Blocks.OBSIDIAN);
               this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.9F, 0.7F + this.random.nextFloat() * 0.08F);
               if (this.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 10, 0.28, 0.3, 0.28, 0.01);
               }

               if (!player.getAbilities().instabuild) {
                  player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
               }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else if ((this.isWaterMimic(mimicState) || this.isIceMimic(mimicState)) && stack.is(Items.LAVA_BUCKET)) {
            if (!this.level().isClientSide) {
               this.setMimicBlock(Blocks.MAGMA_BLOCK);
               this.playSound(SoundEvents.BUCKET_EMPTY_LAVA, 0.85F, 0.95F + this.random.nextFloat() * 0.08F);
               if (this.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.95, this.getZ(), 8, 0.25, 0.24, 0.25, 0.0);
               }

               if (!player.getAbilities().instabuild) {
                  player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
               }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else {
            if (this.isGlassMimic(mimicState) && stack.getItem() instanceof DyeItem dyeItem) {
               Block target = this.resolveStainedGlassForDye(dyeItem.getDyeColor());
               if (target != null && target != mimicState.getBlock()) {
                  if (!this.level().isClientSide) {
                     this.setMimicBlock(target);
                     if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                     }

                     this.playSound(SoundEvents.DYE_USE, 0.75F, 0.95F + this.random.nextFloat() * 0.1F);
                     if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.EFFECT, this.getX(), this.getY() + 1.05, this.getZ(), 10, 0.34, 0.35, 0.34, 0.0);
                     }
                  }

                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               }
            }

            if (this.isMudMimic(mimicState) && stack.is(Items.BRICK)) {
               if (!this.level().isClientSide) {
                  this.setMimicBlock(Blocks.BRICKS);
                  if (!player.getAbilities().instabuild) {
                     stack.shrink(1);
                  }

                  this.playSound(SoundEvents.MUD_PLACE, 0.72F, 1.22F + this.random.nextFloat() * 0.1F);
                  if (this.level() instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.9, this.getZ(), 7, 0.24, 0.2, 0.24, 0.01);
                  }
               }

               return InteractionResult.sidedSuccess(this.level().isClientSide);
            } else {
               if (this.isCopperMimic(mimicState)) {
                  InteractionResult copperResult = this.tryCopperMaintenanceInteraction(player, hand, stack);
                  if (copperResult.consumesAction()) {
                     return copperResult;
                  }
               }

               if (this.isHoneyMimic(mimicState) && stack.is(Items.GLASS_BOTTLE)) {
                  if (!this.level().isClientSide) {
                     if (this.honeyBottleCooldownTicks <= 0) {
                        this.honeyBottleCooldownTicks = 80;
                        ItemStack filled = ItemUtils.createFilledResult(stack, player, new ItemStack(Items.HONEY_BOTTLE));
                        player.setItemInHand(hand, filled);
                        this.playSound(SoundEvents.BOTTLE_FILL, 0.9F, 0.95F + this.random.nextFloat() * 0.1F);
                        if (this.level() instanceof ServerLevel serverLevel) {
                           serverLevel.sendParticles(ParticleTypes.DRIPPING_HONEY, this.getX(), this.getY() + 1.0, this.getZ(), 7, 0.28, 0.2, 0.28, 0.0);
                        }
                     } else {
                        this.playSound(SoundEvents.BEEHIVE_DRIP, 0.6F, 0.9F + this.random.nextFloat() * 0.1F);
                     }
                  }

                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               } else if (this.isPlantMimic(mimicState) && stack.is(Items.BONE_MEAL)) {
                  if (!this.level().isClientSide) {
                     this.heal(4.0F);
                     this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
                     if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                     }

                     this.playSound(SoundEvents.BONE_MEAL_USE, 0.85F, 0.95F + this.random.nextFloat() * 0.1F);
                     if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.levelEvent(2005, this.blockPosition().above(), 0);
                     }
                  }

                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               } else if (this.isRedstoneMimic(mimicState) && stack.is(Items.REDSTONE)) {
                  if (!this.level().isClientSide) {
                     this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 1, true, false));
                     this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0, true, false));
                     if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                     }

                     this.playSound(SoundEvents.REDSTONE_TORCH_BURNOUT, 0.55F, 1.05F + this.random.nextFloat() * 0.15F);
                     if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.34, 0.35, 0.34, 0.0);
                     }
                  }

                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               } else if (stack.isEmpty() && player.isShiftKeyDown() && this.isFallResetMimic(mimicState)) {
                  if (!this.level().isClientSide) {
                     player.setDeltaMovement(player.getDeltaMovement().x, Math.max(player.getDeltaMovement().y, 0.82), player.getDeltaMovement().z);
                     player.hurtMarked = true;
                     this.playSound(SoundEvents.SLIME_BLOCK_FALL, 0.8F, 1.0F + this.random.nextFloat() * 0.08F);
                  }

                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               } else {
                  return super.mobInteract(player, hand);
               }
            }
         }
      } else {
         if (!this.level().isClientSide) {
            boolean wasPrimed = this.tntFuseTicks >= 0;
            this.primeTntFuse();
            if (!wasPrimed) {
               this.playSound(SoundEvents.FLINTANDSTEEL_USE, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            }

            if (stack.is(Items.FLINT_AND_STEEL)) {
               stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            } else if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
   }

   public boolean doHurtTarget(Entity targetEntity) {
      if (!(targetEntity instanceof LivingEntity target && target.isAlive() && !EntityUtils.isImmunePlayerTarget(target))) {
         return false;
      } else if (!this.level().isClientSide && this.shouldUseGroundSlam(target)) {
         this.performGroundSlam(target);
         return true;
      } else {
         this.performUppercut(target);
         return true;
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      ResourceLocation key = BuiltInRegistries.BLOCK.getKey(this.getMimicBlock());
      if (key != null) {
         tag.putString("MimicBlock", key.toString());
      }

      tag.putBoolean("MimicBlockInitialized", this.mimicBlockInitialized);
      tag.putInt("TntFuseTicks", this.tntFuseTicks);
      tag.putInt("HoneyBottleCd", this.honeyBottleCooldownTicks);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("MimicBlock")) {
         ResourceLocation key = ResourceLocation.tryParse(tag.getString("MimicBlock"));
         if (key != null) {
            Block block = BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.STONE);
            this.setMimicBlock(block);
         }
      }

      this.mimicBlockInitialized = tag.getBoolean("MimicBlockInitialized");
      int loadedFuse = tag.contains("TntFuseTicks") ? tag.getInt("TntFuseTicks") : -1;
      if (!this.isTntMimic(this.getMimicBlockState())) {
         loadedFuse = -1;
      }

      this.setTntFuseTicks(loadedFuse);
      this.honeyBottleCooldownTicks = Math.max(0, tag.getInt("HoneyBottleCd"));
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController<>(
            this,
            "controller",
            0,
            event -> {
               int attackType = this.getAttackType();
               if (this.getAttackAnimTicks() > 0) {
                  if (attackType == 2) {
                     return event.setAndContinue(RawAnimation.begin().thenLoop("attack_ground_slam"));
                  }

                  if (attackType == 1) {
                     return event.setAndContinue(RawAnimation.begin().thenLoop("attack_uppercut"));
                  }
               }

               return this.isWalkAnimActive()
                  ? event.setAndContinue(RawAnimation.begin().thenLoop("walk"))
                  : event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
         )
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private void startAttackAnimation(int type, int ticks) {
      this.entityData.set(ATTACK_TYPE, type);
      this.entityData.set(ATTACK_ANIM_TICKS, ticks);
   }

   private void performUppercut(LivingEntity target) {
      this.startAttackAnimation(1, 11);
      DamageSource source = this.damageSources().mobAttack(this);
      float damage = this.rollDamage(1.15F);
      boolean hurt = target.hurt(source, damage);
      if (hurt) {
         this.applyOnHitMimicEffect(target);
         Vec3 away = target.position().subtract(this.position());
         if (away.lengthSqr() < 1.0E-6) {
            away = this.getLookAngle();
         }

         away = away.normalize();
         double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
         double knockScale = Math.max(0.0, 1.0 - resistance);
         target.setDeltaMovement(target.getDeltaMovement().add(away.x * 0.36 * knockScale, 0.64F * knockScale, away.z * 0.36 * knockScale));
         target.hurtMarked = true;
      }

      this.level()
         .playSound(
            null, this.getX(), this.getY(), this.getZ(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.92F + this.random.nextFloat() * 0.1F
         );
   }

   private void performGroundSlam(LivingEntity primaryTarget) {
      this.startAttackAnimation(2, 18);
      this.slamCooldownTicks = 70;
      Vec3 forward = this.getLookAngle();
      if (forward.lengthSqr() < 1.0E-6) {
         forward = new Vec3(0.0, 0.0, 1.0);
      } else {
         forward = forward.normalize();
      }

      Vec3 center = this.position().add(forward.scale(1.25)).add(0.0, 0.1, 0.0);
      DamageSource source = this.damageSources().mobAttack(this);
      AABB area = new AABB(center, center).inflate(3.8F, 1.6, 3.8F);

      for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
         if (victim != this && victim.isAlive() && !victim.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(victim)) {
            float damage = victim == primaryTarget ? this.rollDamage(1.0F) : this.rollDamage(0.75F);
            boolean hurt = victim.hurt(source, damage);
            if (hurt) {
               this.applyOnHitMimicEffect(victim);
               Vec3 push = victim.position().subtract(center);
               if (push.lengthSqr() < 1.0E-6) {
                  push = forward;
               } else {
                  push = push.normalize();
               }

               double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
               double knockScale = Math.max(0.0, 1.0 - resistance);
               victim.setDeltaMovement(victim.getDeltaMovement().add(push.x * 0.52 * knockScale, 0.3 * knockScale, push.z * 0.52 * knockScale));
               victim.hurtMarked = true;
            }
         }
      }

      if (this.level() instanceof ServerLevel serverLevel) {
         BlockState impactState = this.getMimicBlockState();
         this.spawnSlamShockwave(serverLevel, center, impactState);
         this.spawnSlamBlockUplift(serverLevel, center, impactState);
      }

      this.level()
         .playSound(
            null,
            center.x,
            center.y,
            center.z,
            (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.HOSTILE,
            0.95F,
            0.75F + this.random.nextFloat() * 0.1F
         );
   }

   private void spawnSlamShockwave(ServerLevel level, Vec3 center, BlockState impactState) {
      level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.15, center.z, 1, 0.0, 0.0, 0.0, 0.0);
      level.sendParticles(ParticleTypes.POOF, center.x, center.y + 0.15, center.z, 22, 0.7, 0.15, 0.7, 0.03);
      BlockParticleOption blockDust = new BlockParticleOption(ParticleTypes.BLOCK, impactState);

      for (int ring = 1; ring <= 3; ring++) {
         float radius = ring * 1.2666667F;

         for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2) * i / 16.0;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(blockDust, x, center.y + 0.05, z, 1, 0.08, 0.02, 0.08, 0.0);
            level.sendParticles(ParticleTypes.SMOKE, x, center.y + 0.1, z, 1, 0.05, 0.01, 0.05, 0.0);
         }
      }
   }

   private void spawnSlamBlockUplift(ServerLevel level, Vec3 center, BlockState fallbackState) {
      for (int i = 0; i < 10; i++) {
         double angle = this.random.nextDouble() * (Math.PI * 2);
         double dist = 1.1 + this.random.nextDouble() * 2.699999952316284;
         int x = Mth.floor(center.x + Math.cos(angle) * dist);
         int z = Mth.floor(center.z + Math.sin(angle) * dist);
         BlockPos groundPos = this.findSolidGroundAt(x, z);
         if (groundPos != null) {
            BlockState groundState = this.level().getBlockState(groundPos);
            if (!groundState.isAir()
               && groundState.isCollisionShapeFullBlock(this.level(), groundPos)
               && !groundState.hasBlockEntity()
               && groundState.getFluidState().isEmpty()
               && !(groundState.getDestroySpeed(this.level(), groundPos) < 0.0F)) {
               BlockPos spawnPos = groundPos.above();
               if (this.level().getBlockState(spawnPos).isAir()) {
                  BlockState visualState = groundState.isAir() ? fallbackState : groundState;
                  FallingBlockEntity uplift = FallingBlockEntity.fall(level, spawnPos, visualState);
                  uplift.disableDrop();
                  uplift.dropItem = false;
                  uplift.setDeltaMovement(
                     (this.random.nextDouble() - 0.5) * 0.1, 0.34 + this.random.nextDouble() * 0.18, (this.random.nextDouble() - 0.5) * 0.1
                  );
                  uplift.time = 1;
               }
            }
         }
      }
   }

   @Nullable
   private BlockPos findSolidGroundAt(int x, int z) {
      MutableBlockPos cursor = new MutableBlockPos(x, Mth.floor(this.getY()) + 2, z);
      int minY = this.level().getMinBuildHeight() + 1;

      while (cursor.getY() > minY) {
         BlockState state = this.level().getBlockState(cursor);
         if (!state.isAir() && state.isCollisionShapeFullBlock(this.level(), cursor)) {
            return cursor.immutable();
         }

         cursor.move(Direction.DOWN);
      }

      return null;
   }

   private boolean shouldUseGroundSlam(LivingEntity target) {
      if (this.slamCooldownTicks <= 0 && this.onGround()) {
         Vec3 center = this.position().add(this.getLookAngle().normalize().scale(1.25));
         AABB area = new AABB(center, center).inflate(3.8F, 1.6, 3.8F);
         int nearbyTargets = 0;

         for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (living != this && living.isAlive() && !living.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(living)) {
               if (++nearbyTargets >= 2) {
                  return true;
               }
            }
         }

         return this.distanceToSqr(target) <= 9.0 && this.random.nextFloat() < 0.28F;
      } else {
         return false;
      }
   }

   private void applyOnHitMimicEffect(LivingEntity victim) {
      BlockState mimicState = this.getMimicBlockState();
      if (this.isMagmaMimic(mimicState)) {
         victim.igniteForSeconds(2.0F);
      }

      if (this.isIceMimic(mimicState)) {
         victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
      }

      if (this.isHoneyMimic(mimicState)) {
         victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
      }

      if (this.isSoulMimic(mimicState)) {
         victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
      }

      if (this.isCactusMimic(mimicState) && this.random.nextFloat() < 0.25F) {
         victim.hurt(this.damageSources().cactus(), 1.5F);
      }
   }

   private void applyMimicAttributeProfile() {
      int mimicBlockId = (Integer)this.entityData.get(MIMIC_BLOCK_ID);
      if (this.lastAppliedTraitBlockId != mimicBlockId) {
         this.lastAppliedTraitBlockId = mimicBlockId;
         BlockState mimicState = this.getMimicBlockState();
         double speed = 0.18;
         double armor = 8.0;
         double knockbackResistance = 0.6;
         if (this.isLeafMimic(mimicState)) {
            speed += 0.035;
            armor -= 2.0;
            knockbackResistance -= 0.1;
         }

         if (this.isWoodLikeMimic(mimicState)) {
            armor--;
         }

         if (this.isSandLikeMimic(mimicState)) {
            speed -= 0.025;
            armor--;
            knockbackResistance -= 0.05;
         }

         if (this.isFireproofMimic(mimicState)) {
            speed -= 0.015;
            armor += 2.0;
            knockbackResistance += 0.08;
         }

         if (this.isIceMimic(mimicState)) {
            speed += 0.03;
            knockbackResistance -= 0.08;
         }

         if (this.isFallResetMimic(mimicState)) {
            speed -= 0.01;
            knockbackResistance += 0.1;
         }

         if (this.isMagmaMimic(mimicState)) {
            armor++;
         }

         if (this.isWaterMimic(mimicState)) {
            speed += 0.02;
            armor--;
         }

         if (this.isPlantMimic(mimicState)) {
            speed += 0.02;
            armor--;
            knockbackResistance -= 0.08;
         }

         if (this.isMetalMimic(mimicState)) {
            speed -= 0.015;
            armor += 3.0;
            knockbackResistance += 0.15;
         }

         if (this.isGlassMimic(mimicState)) {
            speed += 0.015;
            armor -= 3.0;
            knockbackResistance -= 0.1;
         }

         if (this.isMudMimic(mimicState)) {
            speed -= 0.02;
            knockbackResistance += 0.1;
         }

         if (this.isHoneyMimic(mimicState)) {
            speed -= 0.03;
            knockbackResistance += 0.08;
         }

         if (this.isSpongeMimic(mimicState)) {
            armor++;
            speed -= 0.01;
         }

         if (this.isTntMimic(mimicState)) {
            speed += 0.01;
            armor -= 2.0;
         }

         if (this.isCopperMimic(mimicState)) {
            armor++;
            knockbackResistance += 0.08;
         }

         if (this.isFunctionalMimic(mimicState)) {
            speed += 0.01;
         }

         AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
         if (movement != null) {
            movement.setBaseValue(Mth.clamp(speed, 0.08, 0.35));
         }

         AttributeInstance armorAttr = this.getAttribute(Attributes.ARMOR);
         if (armorAttr != null) {
            armorAttr.setBaseValue(Mth.clamp(armor, 1.0, 24.0));
         }

         AttributeInstance kbAttr = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
         if (kbAttr != null) {
            kbAttr.setBaseValue(Mth.clamp(knockbackResistance, 0.0, 1.0));
         }
      }
   }

   private void tickMimicTraitEffects() {
      if (this.level() instanceof ServerLevel serverLevel) {
         BlockState var3 = this.getMimicBlockState();
         if (this.isFireproofMimic(var3) && this.isOnFire()) {
            this.clearFire();
         }

         if (this.isMagmaMimic(var3)) {
            this.tickMagmaAura(serverLevel);
         }

         if (this.isIceMimic(var3)) {
            this.tickIceMelting(serverLevel);
         }

         if (this.isSoulMimic(var3)) {
            this.tickSoulSandAura(serverLevel);
         }

         if (this.isWaterMimic(var3)) {
            this.tickWaterTrait(serverLevel);
         }

         if (this.isPlantMimic(var3)) {
            this.tickPlantTrait(serverLevel);
         }

         if (this.isHoneyMimic(var3)) {
            this.tickHoneyTrait(serverLevel);
         }

         if (this.isSpongeMimic(var3)) {
            this.tickSpongeTrait(serverLevel);
         }

         if (this.isTntMimic(var3)) {
            this.tickTntTrait(serverLevel);
         }

         if (this.isCopperMimic(var3)) {
            this.tickCopperWeathering(serverLevel);
         }

         if (this.isFunctionalMimic(var3)) {
            this.tickFunctionalTrait(serverLevel);
         }

         if (this.isRedstoneMimic(var3)) {
            this.tickRedstoneTntLink(serverLevel);
         }
      }
   }

   private void tickMagmaAura(ServerLevel serverLevel) {
      if (this.tickCount % 20 == 0) {
         AABB aura = this.getBoundingBox().inflate(2.3, 0.8, 2.3);

         for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, aura)) {
            if (living != this && living.isAlive() && !living.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(living)) {
               living.igniteForSeconds(3.0F);
            }
         }

         serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 0.35, this.getZ(), 8, 0.45, 0.2, 0.45, 0.0);
      }

      if (this.tickCount % 24 == 0) {
         this.trySpreadFireAround(serverLevel);
      }
   }

   private void trySpreadFireAround(ServerLevel serverLevel) {
      if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
         int placed = 0;

         for (int i = 0; i < 8 && placed < 2; i++) {
            int ox = this.random.nextInt(5) - 2;
            int oz = this.random.nextInt(5) - 2;
            BlockPos firePos = this.blockPosition().offset(ox, 0, oz).above();
            if (serverLevel.getBlockState(firePos).isAir()) {
               BlockPos belowPos = firePos.below();
               BlockState belowState = serverLevel.getBlockState(belowPos);
               if (!belowState.isAir() && belowState.isFaceSturdy(serverLevel, belowPos, Direction.UP)) {
                  BlockState fireState = BaseFireBlock.getState(serverLevel, firePos);
                  if (!fireState.isAir() && fireState.canSurvive(serverLevel, firePos)) {
                     serverLevel.setBlockAndUpdate(firePos, fireState);
                     placed++;
                  }
               }
            }
         }
      }
   }

   private void tickIceMelting(ServerLevel serverLevel) {
      if (this.isInDirectSunlight()) {
         if (this.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.35, 0.35, 0.35, 0.0);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.25, 0.2, 0.25, 0.0);
         }

         if (this.tickCount % 40 == 0) {
            this.hurt(this.damageSources().onFire(), 1.0F);
            this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.6F + this.random.nextFloat() * 0.2F);
         }
      }
   }

   private void tickSoulSandAura(ServerLevel serverLevel) {
      if (this.tickCount % 30 == 0) {
         AABB aura = this.getBoundingBox().inflate(2.0, 0.5, 2.0);

         for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, aura)) {
            if (living != this && living.isAlive() && !living.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(living)) {
               living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
         }

         serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.35, 0.25, 0.35, 0.0);
      }
   }

   private void tickWaterTrait(ServerLevel serverLevel) {
      if (this.isOnFire()) {
         this.clearFire();
      }

      if (this.tickCount % 20 == 0) {
         if (this.isInWaterRainOrBubble()) {
            this.heal(0.5F);
         }

         serverLevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() + 0.9, this.getZ(), 5, 0.35, 0.3, 0.35, 0.0);
      }

      if (this.tickCount % 24 == 0) {
         for (int i = 0; i < 5; i++) {
            int ox = this.random.nextInt(5) - 2;
            int oy = this.random.nextInt(3) - 1;
            int oz = this.random.nextInt(5) - 2;
            BlockPos pos = this.blockPosition().offset(ox, oy, oz);
            BlockState state = serverLevel.getBlockState(pos);
            if (state.is(BlockTags.FIRE)) {
               serverLevel.removeBlock(pos, false);
            }
         }
      }
   }

   private void tickPlantTrait(ServerLevel serverLevel) {
      if (this.level().isDay()) {
         BlockPos headPos = this.blockPosition().above();
         if (this.level().canSeeSky(headPos) && !this.level().isRainingAt(headPos)) {
            if (this.tickCount % 40 == 0) {
               this.heal(0.4F);
               serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.1, this.getZ(), 4, 0.35, 0.35, 0.35, 0.0);
            }
         }
      }
   }

   private void tickHoneyTrait(ServerLevel serverLevel) {
      if (this.tickCount % 24 == 0) {
         AABB aura = this.getBoundingBox().inflate(1.8, 0.5, 1.8);

         for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, aura)) {
            if (living != this && living.isAlive() && !living.isAlliedTo(this) && !EntityUtils.isImmunePlayerTarget(living)) {
               living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
            }
         }

         serverLevel.sendParticles(ParticleTypes.DRIPPING_HONEY, this.getX(), this.getY() + 0.9, this.getZ(), 5, 0.3, 0.25, 0.3, 0.0);
      }
   }

   private void tickSpongeTrait(ServerLevel serverLevel) {
      if (this.tickCount % 30 == 0) {
         int removed = 0;
         BlockPos origin = this.blockPosition();

         for (int dx = -2; dx <= 2 && removed < 8; dx++) {
            for (int dy = -1; dy <= 1 && removed < 8; dy++) {
               for (int dz = -2; dz <= 2 && removed < 8; dz++) {
                  BlockPos pos = origin.offset(dx, dy, dz);
                  BlockState state = serverLevel.getBlockState(pos);
                  if (state.is(Blocks.WATER)
                     || state.is(Blocks.KELP)
                     || state.is(Blocks.KELP_PLANT)
                     || state.is(Blocks.SEAGRASS)
                     || state.is(Blocks.TALL_SEAGRASS)
                     || state.is(Blocks.BUBBLE_COLUMN)) {
                     serverLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                     removed++;
                  }
               }
            }
         }

         if (removed > 0) {
            this.playSound(SoundEvents.SPONGE_ABSORB, 0.8F, 0.95F + this.random.nextFloat() * 0.1F);
            serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.45, 0.35, 0.45, 0.0);
         }
      }
   }

   private void tickTntTrait(ServerLevel serverLevel) {
      boolean redstoneTriggered = this.isPoweredByRedstoneNow();
      if (redstoneTriggered || this.isOnFire() || this.isTntCriticalHealth()) {
         this.primeTntFuse();
      }

      if (this.tntFuseTicks >= 0) {
         this.setTntFuseTicks(this.tntFuseTicks - 1);
         serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.1, this.getZ(), 2, 0.12, 0.08, 0.12, 0.0);
         if (this.tntFuseTicks <= 0) {
            this.explodeAsTntGolem(serverLevel, redstoneTriggered);
         }
      }
   }

   private boolean isTntCriticalHealth() {
      float threshold = Math.max(2.5F, this.getMaxHealth() * 0.25F);
      return this.getHealth() <= threshold;
   }

   private void primeTntFuse() {
      if (this.tntFuseTicks < 0) {
         this.setTntFuseTicks(40);
         this.playSound(SoundEvents.TNT_PRIMED, 1.0F, 0.92F + this.random.nextFloat() * 0.1F);
      }
   }

   private void tickRedstoneTntLink(ServerLevel serverLevel) {
      if (this.tickCount % 8 == 0) {
         AABB linkBox = this.getBoundingBox().inflate(3.4, 1.2, 3.4);
         boolean linked = false;

         for (StoneManEntity other : serverLevel.getEntitiesOfClass(
            StoneManEntity.class, linkBox, candidate -> candidate != this && candidate.isAlive() && candidate.isTntMimic(candidate.getMimicBlockState())
         )) {
            int oldFuse = other.tntFuseTicks;
            other.primeTntFuse();
            if (oldFuse < 0 && other.tntFuseTicks >= 0) {
               linked = true;
               serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, other.getX(), other.getY() + 0.85, other.getZ(), 4, 0.2, 0.16, 0.2, 0.0);
            }
         }

         if (linked) {
            this.playSound(SoundEvents.REDSTONE_TORCH_BURNOUT, 0.35F, 0.95F + this.random.nextFloat() * 0.1F);
         }
      }
   }

   private boolean isPoweredByRedstoneNow() {
      BlockPos pos = this.blockPosition();
      return this.level().hasNeighborSignal(pos) || this.level().hasNeighborSignal(pos.above()) || this.level().hasNeighborSignal(pos.below());
   }

   private void explodeAsTntGolem(ServerLevel serverLevel, boolean boosted) {
      if (this.isAlive()) {
         float power = boosted ? 5.2F : 4.0F;
         serverLevel.explode(this, this.getX(), this.getY(0.35), this.getZ(), power, boosted, ExplosionInteraction.TNT);
         this.discard();
      }
   }

   private InteractionResult tryCopperMaintenanceInteraction(Player player, InteractionHand hand, ItemStack stack) {
      Block current = this.getMimicBlock();
      if (stack.is(Items.HONEYCOMB)) {
         Block waxed = this.resolveWaxedCopperVariant(current);
         if (waxed != null && waxed != current) {
            if (!this.level().isClientSide) {
               this.setMimicBlock(waxed);
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               this.playSound(SoundEvents.HONEYCOMB_WAX_ON, 0.8F, 0.9F + this.random.nextFloat() * 0.1F);
               if (this.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(ParticleTypes.WAX_ON, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
               }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      } else if (stack.getItem() instanceof AxeItem) {
         Block unwaxed = this.resolveUnwaxedCopperVariant(current);
         boolean waxOff = unwaxed != null;
         Block transformed = waxOff ? unwaxed : (Block)WeatheringCopper.getPrevious(current).orElse(null);
         if (transformed != null && transformed != current) {
            if (!this.level().isClientSide) {
               this.setMimicBlock(transformed);
               stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
               this.playSound(waxOff ? SoundEvents.AXE_WAX_OFF : SoundEvents.AXE_SCRAPE, 0.75F, 0.92F + this.random.nextFloat() * 0.1F);
               if (this.level() instanceof ServerLevel serverLevel) {
                  serverLevel.sendParticles(
                     waxOff ? ParticleTypes.WAX_OFF : ParticleTypes.SCRAPE, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.3, 0.3, 0.3, 0.0
                  );
               }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Nullable
   private Block resolveWaxedCopperVariant(Block base) {
      ResourceLocation key = BuiltInRegistries.BLOCK.getKey(base);
      if (key == null) {
         return null;
      } else {
         String path = key.getPath();
         if (path.contains("copper") && !path.startsWith("waxed_")) {
            ResourceLocation waxedKey = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "waxed_" + path);
            Block waxed = BuiltInRegistries.BLOCK.getOptional(waxedKey).orElse(Blocks.AIR);
            return waxed == Blocks.AIR ? null : waxed;
         } else {
            return null;
         }
      }
   }

   @Nullable
   private Block resolveUnwaxedCopperVariant(Block base) {
      ResourceLocation key = BuiltInRegistries.BLOCK.getKey(base);
      if (key == null) {
         return null;
      } else {
         String path = key.getPath();
         if (path.contains("copper") && path.startsWith("waxed_")) {
            String unwaxedPath = path.substring("waxed_".length());
            if (unwaxedPath.isEmpty()) {
               return null;
            } else {
               ResourceLocation unwaxedKey = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), unwaxedPath);
               Block unwaxed = BuiltInRegistries.BLOCK.getOptional(unwaxedKey).orElse(Blocks.AIR);
               return unwaxed == Blocks.AIR ? null : unwaxed;
            }
         } else {
            return null;
         }
      }
   }

   @Nullable
   private Block resolveStainedGlassForDye(DyeColor color) {
      return switch (color) {
         case WHITE -> Blocks.WHITE_STAINED_GLASS;
         case ORANGE -> Blocks.ORANGE_STAINED_GLASS;
         case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS;
         case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS;
         case YELLOW -> Blocks.YELLOW_STAINED_GLASS;
         case LIME -> Blocks.LIME_STAINED_GLASS;
         case PINK -> Blocks.PINK_STAINED_GLASS;
         case GRAY -> Blocks.GRAY_STAINED_GLASS;
         case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS;
         case CYAN -> Blocks.CYAN_STAINED_GLASS;
         case PURPLE -> Blocks.PURPLE_STAINED_GLASS;
         case BLUE -> Blocks.BLUE_STAINED_GLASS;
         case BROWN -> Blocks.BROWN_STAINED_GLASS;
         case GREEN -> Blocks.GREEN_STAINED_GLASS;
         case RED -> Blocks.RED_STAINED_GLASS;
         case BLACK -> Blocks.BLACK_STAINED_GLASS;
         default -> throw new MatchException(null, null);
      };
   }

   private void setTntFuseTicks(int ticks) {
      this.tntFuseTicks = ticks;
      this.entityData.set(TNT_FUSE_SYNC, ticks);
   }

   private void tickCopperWeathering(ServerLevel serverLevel) {
      if (this.tickCount % 120 == 0) {
         if (!(this.random.nextFloat() > 0.18F)) {
            Block block = this.getMimicBlock();
            if (!this.isWaxedCopperBlock(block)) {
               WeatheringCopper.getNext(block).ifPresent(next -> {
                  this.setMimicBlock(next);
                  serverLevel.sendParticles(ParticleTypes.WAX_OFF, this.getX(), this.getY() + 1.0, this.getZ(), 6, 0.35, 0.35, 0.35, 0.0);
                  this.playSound(SoundEvents.AXE_SCRAPE, 0.45F, 0.65F + this.random.nextFloat() * 0.1F);
               });
            }
         }
      }
   }

   private void tickFunctionalTrait(ServerLevel serverLevel) {
      if (this.tickCount % 20 == 0) {
         if (this.isPoweredByRedstoneNow()) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 0, true, false));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.28, 0.3, 0.28, 0.0);
         }
      }
   }

   private SoundType getMimicSoundType(BlockState state) {
      SoundType type = state.getSoundType();
      return type == null ? SoundType.STONE : type;
   }

   private boolean isInDirectSunlight() {
      if (this.level().isDay() && !this.level().dimensionType().ultraWarm()) {
         BlockPos headPos = this.blockPosition().above();
         return this.level().canSeeSky(headPos) && !this.level().isRainingAt(headPos) && !this.isInWaterRainOrBubble();
      } else {
         return false;
      }
   }

   private boolean isWoodLikeMimic(BlockState state) {
      return state.is(BlockTags.LOGS_THAT_BURN)
         || state.is(BlockTags.PLANKS)
         || state.is(BlockTags.WOODEN_STAIRS)
         || state.is(BlockTags.WOODEN_SLABS)
         || state.is(BlockTags.WOODEN_FENCES)
         || state.is(BlockTags.WOODEN_DOORS)
         || state.is(BlockTags.WOODEN_TRAPDOORS)
         || state.is(BlockTags.LEAVES)
         || state.is(BlockTags.WOOL);
   }

   private boolean isFireproofMimic(BlockState state) {
      Block block = state.getBlock();
      return this.isMagmaMimic(state)
         || state.is(BlockTags.BASE_STONE_NETHER)
         || state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)
         || state.is(BlockTags.INFINIBURN_NETHER)
         || block == Blocks.OBSIDIAN
         || block == Blocks.CRYING_OBSIDIAN
         || block == Blocks.NETHERITE_BLOCK
         || block == Blocks.ANCIENT_DEBRIS;
   }

   private boolean isMagmaMimic(BlockState state) {
      return state.is(Blocks.MAGMA_BLOCK);
   }

   private boolean isIceMimic(BlockState state) {
      return state.is(BlockTags.ICE) || state.is(BlockTags.SNOW);
   }

   private boolean isSoulMimic(BlockState state) {
      return state.is(BlockTags.SOUL_SPEED_BLOCKS);
   }

   private boolean isFallResetMimic(BlockState state) {
      return state.is(BlockTags.FALL_DAMAGE_RESETTING) || state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK);
   }

   private boolean isCactusMimic(BlockState state) {
      return state.is(Blocks.CACTUS);
   }

   private boolean isSandLikeMimic(BlockState state) {
      return state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL);
   }

   private boolean isLeafMimic(BlockState state) {
      return state.is(BlockTags.LEAVES);
   }

   private boolean isWaterMimic(BlockState state) {
      Block block = state.getBlock();
      return state.getFluidState().is(FluidTags.WATER)
         || block == Blocks.WATER
         || block == Blocks.KELP
         || block == Blocks.KELP_PLANT
         || block == Blocks.SEAGRASS
         || block == Blocks.TALL_SEAGRASS
         || block == Blocks.BUBBLE_COLUMN;
   }

   private boolean isPlantMimic(BlockState state) {
      return state.is(BlockTags.LEAVES)
         || state.is(BlockTags.SAPLINGS)
         || state.is(BlockTags.FLOWERS)
         || state.is(BlockTags.SMALL_FLOWERS)
         || state.is(BlockTags.TALL_FLOWERS)
         || state.is(BlockTags.CROPS)
         || state.is(BlockTags.CAVE_VINES)
         || state.is(Blocks.SHORT_GRASS)
         || state.is(Blocks.TALL_GRASS)
         || state.is(Blocks.FERN)
         || state.is(Blocks.LARGE_FERN);
   }

   private boolean isMetalMimic(BlockState state) {
      Block block = state.getBlock();
      return state.is(BlockTags.BEACON_BASE_BLOCKS)
         || block == Blocks.COPPER_BLOCK
         || block == Blocks.EXPOSED_COPPER
         || block == Blocks.WEATHERED_COPPER
         || block == Blocks.OXIDIZED_COPPER
         || block == Blocks.CUT_COPPER
         || block == Blocks.HEAVY_CORE;
   }

   private boolean isGlassMimic(BlockState state) {
      Block block = state.getBlock();
      return block == Blocks.GLASS
         || block == Blocks.TINTED_GLASS
         || block == Blocks.GLASS_PANE
         || block == Blocks.WHITE_STAINED_GLASS
         || block == Blocks.ORANGE_STAINED_GLASS
         || block == Blocks.MAGENTA_STAINED_GLASS
         || block == Blocks.LIGHT_BLUE_STAINED_GLASS
         || block == Blocks.YELLOW_STAINED_GLASS
         || block == Blocks.LIME_STAINED_GLASS
         || block == Blocks.PINK_STAINED_GLASS
         || block == Blocks.GRAY_STAINED_GLASS
         || block == Blocks.LIGHT_GRAY_STAINED_GLASS
         || block == Blocks.CYAN_STAINED_GLASS
         || block == Blocks.PURPLE_STAINED_GLASS
         || block == Blocks.BLUE_STAINED_GLASS
         || block == Blocks.BROWN_STAINED_GLASS
         || block == Blocks.GREEN_STAINED_GLASS
         || block == Blocks.RED_STAINED_GLASS
         || block == Blocks.BLACK_STAINED_GLASS;
   }

   private boolean isHoneyMimic(BlockState state) {
      return state.is(Blocks.HONEY_BLOCK);
   }

   private boolean isSpongeMimic(BlockState state) {
      return state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE);
   }

   private boolean isMudMimic(BlockState state) {
      return state.is(Blocks.MUD) || state.is(Blocks.PACKED_MUD) || state.is(Blocks.CLAY) || state.is(BlockTags.DIRT);
   }

   private boolean isTntMimic(BlockState state) {
      return state.is(Blocks.TNT);
   }

   private boolean isRedstoneMimic(BlockState state) {
      return state.is(Blocks.REDSTONE_BLOCK)
         || state.is(Blocks.REDSTONE_ORE)
         || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
         || state.is(Blocks.REDSTONE_LAMP)
         || state.is(Blocks.REDSTONE_TORCH)
         || state.is(Blocks.REDSTONE_WALL_TORCH)
         || state.is(Blocks.REDSTONE_WIRE)
         || state.is(Blocks.REPEATER)
         || state.is(Blocks.COMPARATOR)
         || state.is(Blocks.OBSERVER)
         || state.is(Blocks.LEVER)
         || state.is(Blocks.TARGET);
   }

   private boolean isCopperMimic(BlockState state) {
      Block block = state.getBlock();
      ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
      return key != null && key.getPath().contains("copper");
   }

   private boolean isWaxedCopperBlock(Block block) {
      ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
      return key != null && key.getPath().startsWith("waxed_") && key.getPath().contains("copper");
   }

   private boolean isFunctionalMimic(BlockState state) {
      return state.hasBlockEntity()
         || state.is(BlockTags.RAILS)
         || state.is(BlockTags.BUTTONS)
         || state.is(BlockTags.DOORS)
         || state.is(BlockTags.TRAPDOORS)
         || state.is(BlockTags.PRESSURE_PLATES)
         || state.is(BlockTags.CAULDRONS)
         || state.is(Blocks.REDSTONE_BLOCK)
         || state.is(Blocks.REDSTONE_LAMP)
         || state.is(Blocks.REDSTONE_TORCH)
         || state.is(Blocks.REPEATER)
         || state.is(Blocks.COMPARATOR)
         || state.is(Blocks.LEVER)
         || state.is(Blocks.OBSERVER)
         || state.is(Blocks.PISTON)
         || state.is(Blocks.STICKY_PISTON)
         || state.is(Blocks.DISPENSER)
         || state.is(Blocks.DROPPER)
         || state.is(Blocks.TARGET);
   }

   private float rollDamage(float multiplier) {
      float base = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float randomFactor = 0.85F + this.random.nextFloat() * 0.3F;
      return base * multiplier * randomFactor;
   }

   private void updateWalkAnimState() {
      double dx = this.getX() - this.xo;
      double dz = this.getZ() - this.zo;
      double movedSqr = dx * dx + dz * dz;
      if (movedSqr > 2.5E-5) {
         this.walkAnimGraceTicks = 6;
      } else if (this.walkAnimGraceTicks > 0) {
         this.walkAnimGraceTicks--;
      }
   }

   private boolean isWalkAnimActive() {
      return this.walkAnimGraceTicks > 0;
   }
}
