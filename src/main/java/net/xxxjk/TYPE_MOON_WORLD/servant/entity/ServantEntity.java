package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiEngine;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ObedienceAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SocialDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class ServantEntity extends PathfinderMob implements GeoEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private final String servantId;
   private static final EntityDataAccessor<String> SERVANT_ID = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.STRING
   );
   private static final EntityDataAccessor<Integer> OBEDIENCE_AXIS = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> PRINCIPLE_AXIS = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> SOCIAL_DISPOSITION = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Integer> COMBAT_DISPOSITION = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Float> FAVOR = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.FLOAT
   );
   private static final EntityDataAccessor<Float> CURRENT_MP = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.FLOAT
   );

   private final ServantAiEngine aiEngine = new ServantAiEngine();
   @Nullable
   private ServantDefinition cachedDefinition;

   // 动画状态标记
   private int roarAnimationTicks = 0;
   private int slamAnimationTicks = 0;
   private int jumpAttackAnimationTicks = 0;
   private int chargeAnimationTicks = 0;
   private int sweepAnimationTicks = 0;
   private int slashAnimationTicks = 0;
   private int teleportAnimationTicks = 0;
   private int stompAnimationTicks = 0;
   private int uppercutAnimationTicks = 0;
   private int horizontalSwingAnimationTicks = 0;
   // 攻击摆臂：0=空闲，>0=递减中，触发时设为正值
   private int attackSwingTicks = 0;

   public int getAttackSwingTicks() {
      return this.attackSwingTicks;
   }

   protected ServantEntity(EntityType<? extends ServantEntity> entityType, Level level, String servantId) {
      super(entityType, level);
      this.servantId = servantId == null ? "" : servantId;
      this.setPathfindingMalus(PathType.WATER, -1.0F);
   }

   @Deprecated(forRemoval = false)
   public static void setPendingServantId(String id) {
   }

   @Nullable
   @Deprecated(forRemoval = false)
   public static String consumePendingServantId() {
      return null;
   }

   private ServantAnimations getAnimationSet() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.animations() : ServantAnimations.empty();
   }

   private net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization getSpecialization() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.specialization() : net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization.empty();
   }

   private void playActionAnimation(String key) {
      this.getAnimationSet().actionAnimation(key).ifPresent(this::runActionAnim);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      // 如果有待传递的servantId（来自刷怪蛋），优先使用它
      builder.define(SERVANT_ID, "");
      builder.define(OBEDIENCE_AXIS, ObedienceAxis.COOPERATIVE.id());
      builder.define(PRINCIPLE_AXIS, PrincipleAxis.NEUTRAL.id());
      builder.define(SOCIAL_DISPOSITION, SocialDisposition.NORMAL.id());
      builder.define(COMBAT_DISPOSITION, CombatDisposition.BALANCED.id());
      builder.define(FAVOR, 50.0F);
      builder.define(CURRENT_MP, 0.0F);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
         this, net.minecraft.world.entity.monster.Monster.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
         .add(Attributes.MOVEMENT_SPEED, 0.2)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.FOLLOW_RANGE, 48.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (!this.level().isClientSide()) {
         this.aiEngine.tick(this);

         // 动画 tick 递减
         if (this.roarAnimationTicks > 0) {
            this.roarAnimationTicks--;
         }
         if (this.slamAnimationTicks > 0) {
            this.slamAnimationTicks--;
         }
         if (this.jumpAttackAnimationTicks > 0) {
            this.jumpAttackAnimationTicks--;
         }
         if (this.chargeAnimationTicks > 0) {
            this.chargeAnimationTicks--;
         }
         if (this.sweepAnimationTicks > 0) {
            this.sweepAnimationTicks--;
         }
         if (this.slashAnimationTicks > 0) {
            this.slashAnimationTicks--;
         }
         if (this.teleportAnimationTicks > 0) {
            this.teleportAnimationTicks--;
         }
         if (this.stompAnimationTicks > 0) {
            this.stompAnimationTicks--;
         }
         if (this.uppercutAnimationTicks > 0) {
            this.uppercutAnimationTicks--;
         }
         if (this.horizontalSwingAnimationTicks > 0) {
            this.horizontalSwingAnimationTicks--;
         }
         if (this.attackSwingTicks > 0) {
            this.attackSwingTicks--;
         }

         // 落落伤害免疫
         // （通过 causeFallDamage override 实现，见下方）

         // 水/岩浆中紧急闪避（索敌时也生效）
         if (this.isInWater() || this.isInLava()) {
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
                  break;
               }
            }
         }

         // 着火时寻找水源自救
         if (this.isOnFire() && this.random.nextFloat() < 0.5F) {
            BlockPos center = this.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-10, -5, -10), center.offset(10, 5, 10))) {
               if (this.level().getFluidState(p).is(FluidTags.WATER)) {
                  this.getNavigation().moveTo(p.getX(), p.getY(), p.getZ(), 1.5);
                  break;
               }
            }
            if (this.random.nextFloat() < 0.5F) {
               this.clearFire();
            }
         }
      }
   }

   @Override
   public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
      return false; // 英灵免疫摔落伤害
   }

   @Override
   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      if (!this.level().isClientSide()) {
         this.applyDefinitionAttributes(true);
         this.equipDefaultWeapon();
      }
      return result;
   }

   private void applyDefinitionAttributes(boolean initializeDefaults) {
      ServantDefinition def = this.getDefinition();
      if (def == null) {
         return;
      }

      ServantParams params = def.parameters();
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(params.maxHealth());
      if (initializeDefaults) {
         this.setHealth((float) params.maxHealth());
      } else if (this.getHealth() > this.getMaxHealth()) {
         this.setHealth(this.getMaxHealth());
      }
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(params.attackDamage());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(params.movementSpeed());
      this.getAttribute(Attributes.ARMOR).setBaseValue(params.armor());
      this.setCustomName(Component.literal(def.displayNameZh()));
      this.setCustomNameVisible(true);

      if (initializeDefaults) {
         this.entityData.set(OBEDIENCE_AXIS, def.defaultObedience().id());
         this.entityData.set(PRINCIPLE_AXIS, def.defaultPrinciple().id());
         this.entityData.set(SOCIAL_DISPOSITION, def.defaultSocial().id());
         this.entityData.set(COMBAT_DISPOSITION, def.defaultCombat().id());
         this.entityData.set(FAVOR, (float) def.startingFavor());
         this.entityData.set(CURRENT_MP, (float) params.manaPool());
      } else {
         this.entityData.set(FAVOR, (float) Math.min(100.0, Math.max(0.0, this.getFavor())));
         this.entityData.set(CURRENT_MP, (float) Math.min(this.getCurrentMp(), params.manaPool()));
      }

      // 确保索敌距离匹配定义的FOLLOW_RANGE
      if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
         this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(48.0);
      }

      // 狂战士职阶额外移速补偿（狂化加护）
      if (def.classType() == ServantClassType.BERSERKER) {
         double currentSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
         this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(currentSpeed + 0.1);
      }

      // 初始化被动技能
      ServantSkillRegistry.ensureInitialized();
      ServantExecutionContext ctx = new ServantExecutionContext(this, null, params.manaPool(), 0);
      for (String skillId : def.skillIds()) {
         ServantSkillRegistry.execute(skillId, ctx);
      }

      this.refreshDimensions();
   }

   /**
    * 生成时自动装备默认武器到右手
    */
   private void equipDefaultWeapon() {
      if (!this.getMainHandItem().isEmpty()) {
         return;
      }

      var specialization = this.getSpecialization();
      specialization.defaultWeaponItemIdOptional().ifPresentOrElse(weaponId -> {
         ResourceLocation rl = ResourceLocation.parse(weaponId);
         BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item -> this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item)));
      }, () -> {});
   }

   public Vec3 getHandItemOffset() {
      Vec3 offset = this.getSpecialization().handItemOffset();
      if (offset.lengthSqr() == 0.0) {
         return new Vec3(0.15, -0.8, 0.01);
      }
      return offset;
   }

   /**
    * 赫拉克勒斯不受石斧剑的负重debuff影响
    */
   public boolean isExemptFromStoneAxeDebuff() {
      return this.getSpecialization().immuneToStoneAxeDebuff();
   }

   // ======================== 动画触发方法 ========================

   /**
    * 触发吼叫动画（3 秒）
    */
   public void triggerRoarAnimation() {
      this.roarAnimationTicks = 60; // 3s
      if (this.level() instanceof ServerLevel sl) {
         // 吼叫粒子效果
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
            20, 0.8, 0.6, 0.8, 0.15);
         sl.sendParticles(ParticleTypes.POOF,
            this.getX(), this.getY() + 1.2, this.getZ(),
            10, 0.5, 0.5, 0.5, 0.1);
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            this.getX(), this.getY() + 0.5, this.getZ(),
            15, 1.0, 0.3, 1.0, 0.05);
         // 地面碎裂效果
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(), this.getY() + 0.1, this.getZ(),
            20, 1.2, 0.1, 1.2, 0.08);
         // 吼叫音效
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.2F, 0.7F);
      }
   }

   /**
    * 触发砸地动画 + AOE 伤害
    */
   public void triggerGroundSlam() {
      this.slamAnimationTicks = 40; // 2s
      if (this.level() instanceof ServerLevel sl) {
         // 砸地粒子效果
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + 0.3, this.getZ(),
            30, 1.5, 0.3, 1.5, 0.3);
         sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.2, this.getZ(),
            15, 1.2, 0.4, 1.2, 0.1);
         sl.sendParticles(ParticleTypes.POOF,
            this.getX(), this.getY() + 0.5, this.getZ(),
            12, 1.0, 0.3, 1.0, 0.15);
         // 同心圆冲击波
         for (int ring = 1; ring <= 3; ring++) {
            float radius = ring * 1.27F;
            for (int i = 0; i < 16; i++) {
               double angle = (Math.PI * 2) * i / 16.0;
               double px = this.getX() + Math.cos(angle) * radius;
               double pz = this.getZ() + Math.sin(angle) * radius;
               sl.sendParticles(ParticleTypes.CLOUD,
                  px, this.getY() + 0.2, pz,
                  1, 0.0, 0.0, 0.0, 0.03);
               sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                  px, this.getY() + 0.15, pz,
                  1, 0.0, 0.0, 0.0, 0.05);
            }
         }
         // FallingBlockEntity 飞溅
         for (int i = 0; i < 8; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.0 + this.random.nextDouble() * 2.0;
            int bx = (int) Math.floor(this.getX() + Math.cos(angle) * dist);
            int bz = (int) Math.floor(this.getZ() + Math.sin(angle) * dist);
            // 向下寻找实体方块
            for (int by = (int) this.getY(); by >= (int) this.getY() - 3; by--) {
               net.minecraft.world.level.block.state.BlockState ground =
                  this.level().getBlockState(new net.minecraft.core.BlockPos(bx, by, bz));
               if (!ground.isAir() && ground.isCollisionShapeFullBlock(this.level(), new net.minecraft.core.BlockPos(bx, by, bz))) {
                  net.minecraft.world.level.block.state.BlockState above =
                     this.level().getBlockState(new net.minecraft.core.BlockPos(bx, by + 1, bz));
                  if (above.isAir()) {
                     net.minecraft.world.entity.item.FallingBlockEntity fb =
                        net.minecraft.world.entity.item.FallingBlockEntity.fall(
                           sl, new net.minecraft.core.BlockPos(bx, by + 1, bz), ground);
                     fb.disableDrop();
                     fb.setDeltaMovement(
                        (this.random.nextDouble() - 0.5) * 0.1,
                        0.34 + this.random.nextDouble() * 0.18,
                        (this.random.nextDouble() - 0.5) * 0.1);
                     fb.time = 1;
                  }
                  break;
               }
            }
         }
         // 砸地音效
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5F, 0.5F);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.6F);
      }
   }

   /**
    * 触发跳跃攻击动画
    */
   public void triggerJumpAttackAnimation() {
      this.jumpAttackAnimationTicks = 40; // 2s
      playActionAnimation("jump_attack");
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
            15, 0.5, 0.5, 0.5, 0.1);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.6F);
      }
   }

   /**
    * 触发冲刺攻击动画
    */
   public void triggerChargeAnimation() {
      this.chargeAnimationTicks = 30; // 1.5s
      playActionAnimation("charge");
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + 0.5, this.getZ(),
            20, 0.3, 0.3, 0.3, 0.2);
      }
   }

   /**
    * 触发横扫攻击动画
    */
   public void triggerSweepAnimation() {
      this.sweepAnimationTicks = 30; // 1.5s
      playActionAnimation("sweep");
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   /**
    * 触发挥刀斩击动画
    */
   public void triggerSlashAnimation() {
      this.slashAnimationTicks = 20; // 1s
      playActionAnimation("slash");
      if (this.level() instanceof ServerLevel sl) {
         // 斩击弧线粒子
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
            2, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
            8, 0.4, 0.4, 0.4, 0.15);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
      }
   }

   /**
    * 触发瞬移动画
    */
   public void triggerTeleportAnimation() {
      this.teleportAnimationTicks = 15; // 0.75s
      playActionAnimation("teleport_behind");
      if (this.level() instanceof ServerLevel sl) {
         // 瞬移烟雾粒子（原位残留）
         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(), this.getY() + 0.5, this.getZ(),
            20, 0.3, 0.5, 0.3, 0.05);
         sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.8, this.getZ(),
            10, 0.2, 0.3, 0.2, 0.03);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.6F);
      }
   }

   /**
    * 触发跺脚动画
    */
   public void triggerStompAnimation() {
      this.stompAnimationTicks = 20; // 1s
      playActionAnimation("stomp");
      if (this.level() instanceof ServerLevel sl) {
         // 跺脚冲击波
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + 0.2, this.getZ(),
            25, 1.0, 0.2, 1.0, 0.25);
         sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.1, this.getZ(),
            12, 0.8, 0.2, 0.8, 0.1);
         // 同心圆扩散
         for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2) * i / 12.0;
            double px = this.getX() + Math.cos(angle) * 1.5;
            double pz = this.getZ() + Math.sin(angle) * 1.5;
            sl.sendParticles(ParticleTypes.CLOUD,
               px, this.getY() + 0.15, pz,
               2, 0.0, 0.0, 0.0, 0.02);
         }
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.5F, 0.5F);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 0.7F);
      }
   }

   /**
    * 触发上勾拳动画：右臂向右上方举起，往左下方挥去
    */
   public void triggerUppercutAnimation() {
      this.uppercutAnimationTicks = 20; // 1s
      playActionAnimation("uppercut");
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CRIT,
            this.getX(), this.getY() + this.getBbHeight() * 0.8, this.getZ(),
            6, 0.4, 0.4, 0.4, 0.12);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.3F, 0.9F);
      }
   }

   /**
    * 触发横挥动画：右臂横在胸前，向右边挥动
    */
   public void triggerHorizontalSwingAnimation() {
      this.horizontalSwingAnimationTicks = 20; // 1s
      playActionAnimation("horizontal_swing");
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + this.getBbHeight() * 0.55, this.getZ(),
            2, 0.0, 0.0, 0.0, 0.0);
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            5, 0.3, 0.3, 0.3, 0.05);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.85F);
      }
   }

   /**
    * 近战攻击时触发右手大幅摆动（保底视觉反馈）
    */
   public void triggerAttackSwing() {
      this.attackSwingTicks = 12; // 0.6s
   }

   public boolean isAttackSwinging() {
      return this.attackSwingTicks > 0;
   }

   public boolean isPerformingAction() {
      return this.jumpAttackAnimationTicks > 0 || this.chargeAnimationTicks > 0
         || this.sweepAnimationTicks > 0 || this.slashAnimationTicks > 0
         || this.teleportAnimationTicks > 0 || this.stompAnimationTicks > 0
         || this.uppercutAnimationTicks > 0 || this.horizontalSwingAnimationTicks > 0;
   }

   public boolean isRoaring() {
      return this.roarAnimationTicks > 0;
   }

   public boolean isSlamming() {
      return this.slamAnimationTicks > 0;
   }

   // ======================== 死亡特效 ========================

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource cause) {
      super.die(cause);
      if (this.level() instanceof ServerLevel sl) {
         // 灵体消散粒子（参考Fate英灵消失效果）
         // 从身体底部向上升起的金色光粒子
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            this.getX(), this.getY() + 0.2, this.getZ(),
            40, 0.4, 0.8, 0.4, 0.06);
         // 闪光粒子环绕上升
         sl.sendParticles(ParticleTypes.END_ROD,
            this.getX(), this.getY() + 0.5, this.getZ(),
            30, 0.5, 1.0, 0.5, 0.04);
         // 灵魂火焰粒子（灵基消散）
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            this.getX(), this.getY() + 1.0, this.getZ(),
            25, 0.6, 0.8, 0.6, 0.03);
         // 灵魂上升粒子
         sl.sendParticles(ParticleTypes.SOUL,
            this.getX(), this.getY() + 1.5, this.getZ(),
            20, 0.3, 1.2, 0.3, 0.05);
         // 从内向外扩散的白色光环
         sl.sendParticles(ParticleTypes.FLASH,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            5, 0.0, 0.0, 0.0, 0.0);
         // 身体逐渐透明的灰烬效果
         sl.sendParticles(ParticleTypes.ASH,
            this.getX(), this.getY() + 0.5, this.getZ(),
            35, 0.8, 1.0, 0.8, 0.08);
         // 地面光柱上升效果
         sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
            this.getX(), this.getY() + 0.1, this.getZ(),
            20, 0.3, 0.5, 0.3, 0.06);
         // 最终消散音效
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.5F, 1.2F);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.0F, 0.8F);
      }
   }

   // ======================== NBT ========================

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("ServantId", this.getServantId());
      tag.putFloat("Xp", (float) this.xpReward);
      tag.putInt("ObedienceAxis", this.getObedienceAxis().id());
      tag.putInt("PrincipleAxis", this.getPrincipleAxis().id());
      tag.putInt("SocialDisposition", this.getSocialDisposition().id());
      tag.putInt("CombatDisposition", this.getCombatDisposition().id());
      tag.putDouble("Favor", this.getFavor());
      tag.putDouble("CurrentMp", this.getCurrentMp());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      String loadedId = tag.getString("ServantId");
      this.entityData.set(SERVANT_ID, this.servantId);
      this.xpReward = tag.getInt("Xp");
      this.entityData.set(OBEDIENCE_AXIS, tag.getInt("ObedienceAxis"));
      this.entityData.set(PRINCIPLE_AXIS, tag.getInt("PrincipleAxis"));
      this.entityData.set(SOCIAL_DISPOSITION, tag.getInt("SocialDisposition"));
      this.entityData.set(COMBAT_DISPOSITION, tag.getInt("CombatDisposition"));
      this.entityData.set(FAVOR, (float) tag.getDouble("Favor"));
      this.entityData.set(CURRENT_MP, (float) tag.getDouble("CurrentMp"));
      this.cachedDefinition = null;

      // 对于旧存档中没有 ServantId 的实体，保持为空实体
      if (loadedId == null || loadedId.isEmpty()) {
         this.entityData.set(SERVANT_ID, this.servantId);
         this.cachedDefinition = null;
      }

      this.equipDefaultWeapon();
      this.applyDefinitionAttributes(false);
   }

   // ======================== Getters / Setters ========================

   public String getServantId() {
      return this.servantId;
   }

   @Deprecated(forRemoval = false)
   public void setServantId(String id) {
      this.entityData.set(SERVANT_ID, this.servantId);
      this.cachedDefinition = null;
      // 立即从ServantDataRegistry重新查找定义
      if (!this.servantId.isEmpty()) {
         ServantDefinition def = ServantDataRegistry.get(this.servantId);
         if (def != null) {
            this.cachedDefinition = def;
         } else {
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.warn(
               "Servant definition not found for fixed id='{}', entity={}", this.servantId, this);
         }
      }
      this.refreshDimensions();
   }

   @Nullable
   public ServantDefinition getDefinition() {
      if (this.cachedDefinition == null) {
         String id = this.getServantId();
         if (id != null && !id.isEmpty()) {
            ServantDefinition def = ServantDataRegistry.get(id);
            if (def != null) {
               this.cachedDefinition = def;
            } else {
               net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.LOGGER.warn(
                  "Servant definition not found for id='{}', entity={}", id, this);
            }
         }
      }
      return this.cachedDefinition;
   }

   public ObedienceAxis getObedienceAxis() {
      return ObedienceAxis.fromId(this.entityData.get(OBEDIENCE_AXIS));
   }

   public void setObedienceAxis(ObedienceAxis axis) {
      this.entityData.set(OBEDIENCE_AXIS, axis.id());
   }

   public PrincipleAxis getPrincipleAxis() {
      return PrincipleAxis.fromId(this.entityData.get(PRINCIPLE_AXIS));
   }

   public void setPrincipleAxis(PrincipleAxis axis) {
      this.entityData.set(PRINCIPLE_AXIS, axis.id());
   }

   public SocialDisposition getSocialDisposition() {
      return SocialDisposition.fromId(this.entityData.get(SOCIAL_DISPOSITION));
   }

   public CombatDisposition getCombatDisposition() {
      return CombatDisposition.fromId(this.entityData.get(COMBAT_DISPOSITION));
   }

   public void setCombatDisposition(CombatDisposition disposition) {
      this.entityData.set(COMBAT_DISPOSITION, disposition.id());
   }

   public double getFavor() {
      return this.entityData.get(FAVOR);
   }

   public void setFavor(double favor) {
      this.entityData.set(FAVOR, (float) Math.min(100.0, Math.max(0.0, favor)));
   }

   public double getCurrentMp() {
      return this.entityData.get(CURRENT_MP);
   }

   public void setCurrentMp(double mp) {
      this.entityData.set(CURRENT_MP, (float) mp);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (SERVANT_ID.equals(key)) {
         this.cachedDefinition = null;
         this.getDefinition();
         this.refreshDimensions();
      }
   }

   public double getMaxMp() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.parameters().manaPool() : 100.0;
   }

   public double getCritRate() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.parameters().critRatePercent() : 2.0;
   }

   @Override
   protected EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
      var specialization = this.getSpecialization();
      if (specialization.hasBodyDimensions()) {
         return specialization.bodyDimensions();
      }
      if (this.getServantId() == null || this.getServantId().isEmpty()) {
         return EntityDimensions.fixed(0.0F, 0.0F);
      }
      return super.getDefaultDimensions(pose);
   }

   // ======================== GeckoLib ========================

   private AnimationController<ServantEntity> actionCtrl;

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      // 主控制器：idle / walk（transition=3 正常过渡）
      controllers.add(new AnimationController<>(this, "controller", 3, event -> {
         var animations = this.getAnimationSet();
         String animation = event.isMoving() ? animations.walkAnimation().orElse(null) : animations.idleAnimation().orElse(null);
         return animation != null ? event.setAndContinue(RawAnimation.begin().thenLoop(animation)) : null;
      }));
      // 动作控制器（transition=0，通过 runActionAnim 触发）
      this.actionCtrl = new AnimationController<>(this, "action_controller", 0, event -> null);
      controllers.add(this.actionCtrl);
      // 吼叫控制器（transition=0，独立控制）
      controllers.add(new AnimationController<>(this, "roar_controller", 0, event -> {
         if (this.roarAnimationTicks > 0) {
            String animation = this.getAnimationSet().actionAnimation("roar").orElse(null);
            return animation != null ? event.setAndContinue(RawAnimation.begin().thenPlay(animation)) : null;
         }
         return null;
      }));
      // 砸地控制器（transition=0）
      controllers.add(new AnimationController<>(this, "slam_controller", 0, event -> {
         if (this.slamAnimationTicks > 0) {
            String animation = this.getAnimationSet().actionAnimation("slam").orElse(null);
            return animation != null ? event.setAndContinue(RawAnimation.begin().thenPlay(animation)) : null;
         }
         return null;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   private void runActionAnim(String anim) {
      if (this.actionCtrl != null) {
         this.actionCtrl.forceAnimationReset();
         this.actionCtrl.setAnimation(RawAnimation.begin().thenPlay(anim));
      }
   }
}
