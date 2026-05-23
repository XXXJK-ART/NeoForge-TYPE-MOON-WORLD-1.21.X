package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
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
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiEngine;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
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

public class ServantEntity extends PathfinderMob implements GeoEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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

   public ServantEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
      super(entityType, level);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SERVANT_ID, "heracles");
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
      this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
         .add(Attributes.MOVEMENT_SPEED, 0.2)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
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
      }
   }

   @Override
   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                       MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      if (!this.level().isClientSide()) {
         this.applyDefinitionAttributes();
         this.equipDefaultWeapon();
      }
      return result;
   }

   private void applyDefinitionAttributes() {
      ServantDefinition def = this.getDefinition();
      if (def == null) {
         return;
      }

      ServantParams params = def.parameters();
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(params.maxHealth());
      this.setHealth((float) params.maxHealth());
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(params.attackDamage());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(params.movementSpeed());
      this.getAttribute(Attributes.ARMOR).setBaseValue(params.armor());
      this.setCustomName(Component.literal(def.displayNameZh()));
      this.setCustomNameVisible(true);

      this.entityData.set(OBEDIENCE_AXIS, def.defaultObedience().id());
      this.entityData.set(PRINCIPLE_AXIS, def.defaultPrinciple().id());
      this.entityData.set(SOCIAL_DISPOSITION, def.defaultSocial().id());
      this.entityData.set(COMBAT_DISPOSITION, def.defaultCombat().id());
      this.entityData.set(FAVOR, (float) def.startingFavor());
      this.entityData.set(CURRENT_MP, (float) params.manaPool());

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
   }

   /**
    * 赫拉克勒斯生成时自动装备石斧剑到右手
    */
   private void equipDefaultWeapon() {
      String id = this.getServantId();
      if ("heracles".equals(id)) {
         this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TEMPLE_STONE_SWORD_AXE.get()));
      }
   }

   /**
    * 赫拉克勒斯不受石斧剑的负重debuff影响
    */
   public boolean isExemptFromStoneAxeDebuff() {
      return "heracles".equals(this.getServantId());
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
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   public boolean isPerformingAction() {
      return this.jumpAttackAnimationTicks > 0 || this.chargeAnimationTicks > 0
         || this.sweepAnimationTicks > 0;
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
      this.entityData.set(SERVANT_ID, loadedId);
      this.xpReward = tag.getInt("Xp");
      this.entityData.set(OBEDIENCE_AXIS, tag.getInt("ObedienceAxis"));
      this.entityData.set(PRINCIPLE_AXIS, tag.getInt("PrincipleAxis"));
      this.entityData.set(SOCIAL_DISPOSITION, tag.getInt("SocialDisposition"));
      this.entityData.set(COMBAT_DISPOSITION, tag.getInt("CombatDisposition"));
      this.entityData.set(FAVOR, (float) tag.getDouble("Favor"));
      this.entityData.set(CURRENT_MP, (float) tag.getDouble("CurrentMp"));
      this.cachedDefinition = null;

      // 对于旧存档中没有 ServantId 的实体，自动回退到 heracles
      if (loadedId == null || loadedId.isEmpty()) {
         this.entityData.set(SERVANT_ID, "heracles");
         this.cachedDefinition = null;
      }

      this.equipDefaultWeapon();
      this.applyDefinitionAttributes();
   }

   // ======================== Getters / Setters ========================

   public String getServantId() {
      return this.entityData.get(SERVANT_ID);
   }

   public void setServantId(String id) {
      this.entityData.set(SERVANT_ID, id);
      this.cachedDefinition = null;
   }

   @Nullable
   public ServantDefinition getDefinition() {
      if (this.cachedDefinition == null) {
         String id = this.getServantId();
         if (id != null && !id.isEmpty()) {
            this.cachedDefinition = ServantDataRegistry.get(id);
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

   public double getMaxMp() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.parameters().manaPool() : 100.0;
   }

   public double getCritRate() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.parameters().critRatePercent() : 2.0;
   }

   // ======================== GeckoLib ========================

   @Override
   public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      // 主控制器：idle / walk（transition=3 正常过渡）
      controllers.add(new AnimationController<>(this, "controller", 3, event -> {
         if (event.isMoving()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.heracles.walk"));
         }
         return event.setAndContinue(RawAnimation.begin().thenLoop("animation.heracles.idle"));
      }));
      // 动作控制器：jump_attack / charge / sweep（transition=0 立即切换）
      controllers.add(new AnimationController<>(this, "action_controller", 0, event -> {
         if (this.jumpAttackAnimationTicks > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.heracles.jump_attack"));
         }
         if (this.chargeAnimationTicks > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.heracles.charge"));
         }
         if (this.sweepAnimationTicks > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.heracles.sweep"));
         }
         return null;
      }));
      // 吼叫控制器（transition=0，独立控制）
      controllers.add(new AnimationController<>(this, "roar_controller", 0, event -> {
         if (this.roarAnimationTicks > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.heracles.roar"));
         }
         return null;
      }));
      // 砸地控制器（transition=0）
      controllers.add(new AnimationController<>(this, "slam_controller", 0, event -> {
         if (this.slamAnimationTicks > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.heracles.slam"));
         }
         return null;
      }));
   }

   @Override
   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
