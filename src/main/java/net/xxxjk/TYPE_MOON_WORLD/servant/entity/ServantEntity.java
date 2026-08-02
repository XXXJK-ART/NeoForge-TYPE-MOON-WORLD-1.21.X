package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import java.util.UUID;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
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
import net.xxxjk.TYPE_MOON_WORLD.combat.ai.AiBrain;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiEngine;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantTacticalController;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantCombatSystem;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantAnimations;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantClassType;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantParams;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.CombatDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.MoralAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ObedienceAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.PrincipleAxis;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SpecialTargetPrinciple;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.SocialDisposition;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.vfx.VFXServerEffects;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactProfile;
import net.xxxjk.TYPE_MOON_WORLD.world.terrain.TerrainImpactService;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class ServantEntity extends PathfinderMob implements GeoEntity {
   private static final String ACTION_CONTROLLER = "action_controller";
   private static final int SPIRITUAL_DISSOLVE_DURATION = 50;
   private static final int WALK_ANIMATION_GRACE_TICKS = 6;
   private static final double WALK_ANIMATION_DELTA_THRESHOLD = 1.0E-5;
   private static final String LAST_MANA_HEAL_TICK_TAG = "ServantLastManaHealTick";
   private static final String NATURAL_REGEN_LAST_COMBAT_TICK_TAG = "ServantNaturalRegenLastCombatTick";
   private static final String BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG = "BattleContinuationRecoveryActive";
   private static final String LAST_FIRE_ESCAPE_SCAN_TICK_TAG = "ServantLastFireEscapeScanTick";
   private static final int MANA_HEAL_INTERVAL_TICKS = 20;
   private static final double MANA_HEAL_MP_COST = 1.0;
   private static final float MANA_HEAL_IN_COMBAT_AMOUNT = 5.0F;
   private static final float MANA_HEAL_OUT_OF_COMBAT_AMOUNT = 10.0F;
   private static final double MANA_HEAL_HEALTH_THRESHOLD = 0.60;
   private static final double MANA_HEAL_MP_THRESHOLD = 0.50;
   private static final int OUT_OF_COMBAT_HEAL_GRACE_TICKS = 100;
   private static final double COMBAT_HEAL_TARGET_RANGE_SQR = 24.0 * 24.0;
   private static final float NATURAL_REGEN_HEALTH_RATIO_PER_SECOND = 0.005F;
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private String servantId;
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
   private static final EntityDataAccessor<Boolean> SPIRITUAL_DISSOLVING = SynchedEntityData.defineId(
      ServantEntity.class, EntityDataSerializers.BOOLEAN
   );

   private final ServantAiEngine aiEngine = new ServantAiEngine();
   @Nullable
   private ServantDefinition cachedDefinition;

   // 鍔ㄧ敾鐘舵€佹爣璁?
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
   private int runeCastAnimationTicks = 0;
   private int gaeBolgThrowAnimationTicks = 0;
   // 鏀诲嚮鎽嗚噦锛? = 绌洪棽锛屽ぇ浜?0 鏃堕€?tick 閫掑噺

   private int attackSwingTicks = 0;
   private int basicAttackVariant = 0;
   private int spiritualDissolveTicks = 0;
   private int walkAnimationGraceTicks = 0;
   private long tacticalAiHandledTick = Long.MIN_VALUE;
   @Nullable private UUID masterUuid;
   private String contractId = "";
   private ServantCommandMode commandMode = ServantCommandMode.FOLLOW;
   private BlockPos stayAnchor = BlockPos.ZERO;
   private boolean masterNoblePhantasmPermission;

   public int getAttackSwingTicks() {
      return this.attackSwingTicks;
   }

   protected ServantEntity(EntityType<? extends ServantEntity> entityType, Level level, String servantId) {
      super(entityType, level);
      this.servantId = servantId == null ? "" : servantId;
      this.entityData.set(SERVANT_ID, this.servantId);
      this.setPathfindingMalus(PathType.WATER, -1.0F);
      this.setPersistenceRequired();
   }

   @Deprecated(forRemoval = false)
   public static void setPendingServantId(String id) {
   }

   @Nullable
   @Deprecated(forRemoval = false)
   public static String consumePendingServantId() {
      return null;
   }

   protected ServantAnimations getAnimationSet() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.animations() : ServantAnimations.empty();
   }

   private net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization getSpecialization() {
      ServantDefinition def = this.getDefinition();
      return def != null ? def.specialization() : net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSpecialization.empty();
   }

   public boolean hasActionAnimation(String key) {
      return this.getAnimationSet().actionAnimation(key).isPresent();
   }

   protected void playActionAnimation(String key) {
      if (this.hasActionAnimation(key)) {
         this.triggerAnim(ACTION_CONTROLLER, key);
      }
   }

   public void triggerNamedActionAnimation(String key) {
      this.playActionAnimation(key);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      // 濡傛灉鏈夊緟浼犻€掔殑 servantId锛堟潵鑷埛鎬泲锛夛紝浼樺厛浣跨敤瀹?
      builder.define(SERVANT_ID, "");
      builder.define(OBEDIENCE_AXIS, ObedienceAxis.COOPERATIVE.id());
      builder.define(PRINCIPLE_AXIS, PrincipleAxis.NEUTRAL.id());
      builder.define(SOCIAL_DISPOSITION, SocialDisposition.NORMAL.id());
      builder.define(COMBAT_DISPOSITION, CombatDisposition.BALANCED.id());
      builder.define(FAVOR, 50.0F);
      builder.define(CURRENT_MP, 0.0F);
      builder.define(SPIRITUAL_DISSOLVING, false);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   @Nullable
   public UUID getMasterUuid() {
      return masterUuid;
   }

   @Nullable
   public ServerPlayer getEntityMaster() {
      if (masterUuid == null || !(level() instanceof ServerLevel serverLevel)) return null;
      return serverLevel.getServer().getPlayerList().getPlayer(masterUuid);
   }

   public boolean isBoundTo(ServerPlayer player) {
      return player != null && player.getUUID().equals(masterUuid);
   }

   public void bindMaster(ServerPlayer master) {
      this.masterUuid = master.getUUID();
      this.commandMode = ServantCommandMode.FOLLOW;
      this.stayAnchor = blockPosition();
      this.masterNoblePhantasmPermission = false;
      setPersistenceRequired();
   }

   public String getContractId() {
      return contractId;
   }

   public void setContractId(String id) {
      contractId = id == null ? "" : id;
   }

   public void unbindMaster() {
      this.masterUuid = null;
      this.contractId = "";
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.clearContractTags(this);
      this.commandMode = ServantCommandMode.FOLLOW;
      this.stayAnchor = blockPosition();
      this.masterNoblePhantasmPermission = false;
   }

   public ServantCommandMode getCommandMode() {
      return commandMode;
   }

   public BlockPos getStayAnchor() {
      return stayAnchor;
   }

   public ServantCommandMode cycleCommandMode() {
      commandMode = commandMode.next();
      if (commandMode == ServantCommandMode.STAY) stayAnchor = blockPosition();
      return commandMode;
   }

   public boolean hasMasterNoblePhantasmPermission() {
      return masterUuid == null || masterNoblePhantasmPermission;
   }

   public boolean toggleMasterNoblePhantasmPermission() {
      masterNoblePhantasmPermission = !masterNoblePhantasmPermission;
      return masterNoblePhantasmPermission;
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      if (super.isAlliedTo(other)) return true;
      ServerPlayer master = getEntityMaster();
      if (master == null) return false;
      if (other == master || master.isAlliedTo(other)) return true;
      return other instanceof ServantEntity servant && master.getUUID().equals(servant.masterUuid);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 100.0)
         .add(Attributes.MOVEMENT_SPEED, 0.2)
         .add(Attributes.STEP_HEIGHT, 3.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.ARMOR, 4.0)
         .add(Attributes.ARMOR_TOUGHNESS, 0.0)
         .add(Attributes.FOLLOW_RANGE, 96.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   public void tick() {
      super.tick();
      net.xxxjk.TYPE_MOON_WORLD.servant.concealment.ServantConcealment.tick(this);
      this.updateWalkAnimationState();
      ArtoriaPendragonCombatHelper.tickSharedBuffCleanup(this);
      GawainCombatHelper.tickSharedBuffCleanup(this);
      ServantSprintCollisionHelper.tickNpcSprintCollision(this);
   }

   private void updateWalkAnimationState() {
      if (this.isSpiritualDissolving() || !this.isAlive()) {
         this.walkAnimationGraceTicks = 0;
         return;
      }

      double dx = this.getX() - this.xo;
      double dz = this.getZ() - this.zo;
      double positionDelta = dx * dx + dz * dz;
      double velocityDelta = this.getDeltaMovement().horizontalDistanceSqr();
      if (positionDelta > WALK_ANIMATION_DELTA_THRESHOLD || velocityDelta > WALK_ANIMATION_DELTA_THRESHOLD) {
         this.walkAnimationGraceTicks = WALK_ANIMATION_GRACE_TICKS;
      } else if (this.walkAnimationGraceTicks > 0) {
         this.walkAnimationGraceTicks--;
      }
   }

   protected boolean isWalkAnimationActive(boolean geckoMoving) {
      return geckoMoving || this.walkAnimationGraceTicks > 0;
   }

   @Override
   protected void customServerAiStep() {
      if (!this.level().isClientSide()) {
         if (this.hasEffect(ModMobEffects.PETRIFIED)) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            return;
         }
         if (this.isSpiritualDissolving()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(Vec3.ZERO);
            this.hurtTime = 0;
            this.hurtDuration = 0;
            return;
         }

         boolean combatHandled = ServantCombatSystem.tickBeforeAi(this);
         boolean tacticalHandled = !combatHandled && ServantTacticalController.tick(this);
         this.tacticalAiHandledTick = combatHandled || tacticalHandled ? this.level().getGameTime() : Long.MIN_VALUE;
         if (!combatHandled && !tacticalHandled) {
            super.customServerAiStep();
            this.aiEngine.tick(this);
         }
         this.tickManaHealthConversion();

         /* 鍔ㄧ敾 tick 閫掑噺 */         
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
         if (this.runeCastAnimationTicks > 0) {
            this.runeCastAnimationTicks--;
         }
         if (this.gaeBolgThrowAnimationTicks > 0) {
            this.gaeBolgThrowAnimationTicks--;
         }
         if (this.attackSwingTicks > 0) {
            this.attackSwingTicks--;
         }

         // 鑴辩鎴樻枟鍚庣紦鎱㈠洖琛€锛堟瘡绉掓仮澶?0.5% 鏈€澶х敓鍛藉€硷級
         this.tickNaturalHealthRegen();

         // 璺岃惤浼ゅ鍏嶇柅
         // 锛堥€氳繃 causeFallDamage override 瀹炵幇锛岃涓嬫柟锛?
         // 水/岩浆中紧急闪避（索敌时也生效）
         if (!combatHandled && !tacticalHandled && (this.isInWater() || this.isInLava())) {
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

         /* 着火时寻找水源自救 */
         if (!combatHandled && !tacticalHandled && this.isOnFire() && this.random.nextFloat() < 0.5F
            && this.tickCount - this.getPersistentData().getInt(LAST_FIRE_ESCAPE_SCAN_TICK_TAG) >= 20) {
            this.getPersistentData().putInt(LAST_FIRE_ESCAPE_SCAN_TICK_TAG, this.tickCount);
            BlockPos center = this.blockPosition();
            for (int attempt = 0; attempt < 18; attempt++) {
               BlockPos p = center.offset(
                  this.random.nextIntBetweenInclusive(-10, 10),
                  this.random.nextIntBetweenInclusive(-5, 2),
                  this.random.nextIntBetweenInclusive(-10, 10)
               );
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
   public boolean hurt(DamageSource source, float amount) {
      float healthBefore = this.getHealth();
      boolean hurt = super.hurt(source, amount);
      if (hurt && !this.level().isClientSide() && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
         float effectiveDamage = Math.max(0.0F, healthBefore - this.getHealth());
         ResourceLocation observedDamage = ResourceLocation.fromNamespaceAndPath(
            net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "ai/observed_damage");
         AiBrain.blackboard(this).observe(attacker.getUUID(), observedDamage, this.distanceTo(attacker), effectiveDamage, false,
            this.level().getGameTime());
      }
      return hurt;
   }

   private void tickManaHealthConversion() {
      if (this.level().isClientSide() || !this.isAlive() || this.isSpiritualDissolving()) {
         return;
      }

      long now = this.level().getGameTime();
      CompoundTag data = this.getPersistentData();
      if (data.getBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG)) {
         return;
      }
      long lastHealTick = data.getLong(LAST_MANA_HEAL_TICK_TAG);
      if (lastHealTick > 0L && now - lastHealTick < MANA_HEAL_INTERVAL_TICKS) {
         return;
      }
      double maxMp = Math.max(1.0, this.getMaxMp());
      if (this.getHealth() >= this.getMaxHealth() * MANA_HEAL_HEALTH_THRESHOLD
         || this.getCurrentMp() < maxMp * MANA_HEAL_MP_THRESHOLD
         || this.getCurrentMp() < MANA_HEAL_MP_COST) {
         return;
      }

      boolean inCombat = this.isManaHealingInCombat(data, now);
      float healAmount = inCombat ? MANA_HEAL_IN_COMBAT_AMOUNT : MANA_HEAL_OUT_OF_COMBAT_AMOUNT;
      this.setCurrentMp(Math.max(0.0, this.getCurrentMp() - MANA_HEAL_MP_COST));
      this.heal(healAmount);
      data.putLong(LAST_MANA_HEAL_TICK_TAG, now);

      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(inCombat ? ParticleTypes.ENCHANT : ParticleTypes.HAPPY_VILLAGER,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            inCombat ? 2 : 3, 0.22, 0.25, 0.22, 0.0);
      }
   }

   public boolean wasTacticalAiHandledThisTick() {
      return this.tacticalAiHandledTick == this.level().getGameTime();
   }

   private void tickNaturalHealthRegen() {
      if (this.tickCount % 20 != 0 || this.level().isClientSide() || !this.isAlive() || this.isSpiritualDissolving()) {
         return;
      }

      long now = this.level().getGameTime();
      CompoundTag data = this.getPersistentData();
      LivingEntity combatTarget = this.getTarget();
      if (combatTarget != null && combatTarget.isAlive() && this.distanceToSqr(combatTarget) <= COMBAT_HEAL_TARGET_RANGE_SQR) {
         data.putLong(NATURAL_REGEN_LAST_COMBAT_TICK_TAG, now);
         return;
      }

      if (data.getBoolean(BATTLE_CONTINUATION_RECOVERY_ACTIVE_TAG)) {
         return;
      }

      long lastCombat = this.getLastNaturalRegenCombatTick(data);
      if (lastCombat <= 0L || now - lastCombat <= OUT_OF_COMBAT_HEAL_GRACE_TICKS || this.getHealth() >= this.getMaxHealth()) {
         return;
      }

      this.heal(this.getMaxHealth() * NATURAL_REGEN_HEALTH_RATIO_PER_SECOND);
      if (this.level() instanceof ServerLevel sl) {
         sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            3, 0.3, 0.3, 0.3, 0.0);
      }
   }

   private boolean isManaHealingInCombat(CompoundTag data, long now) {
      LivingEntity combatTarget = this.getTarget();
      if (combatTarget != null && combatTarget.isAlive() && this.distanceToSqr(combatTarget) <= COMBAT_HEAL_TARGET_RANGE_SQR) {
         return true;
      }

      long lastCombat = Math.max(
         Math.max(data.getLong("TypeMoonCombatLastCombatTick"), data.getLong("CuLastCombatTick")),
         data.getLong("SasakiKojiroLastCombatTick")
      );
      return lastCombat > 0L && now - lastCombat < OUT_OF_COMBAT_HEAL_GRACE_TICKS;
   }

   private long getLastNaturalRegenCombatTick(CompoundTag data) {
      return Math.max(
         Math.max(data.getLong(NATURAL_REGEN_LAST_COMBAT_TICK_TAG), data.getLong("LastCombatTick")),
         Math.max(
            Math.max(data.getLong("TypeMoonCombatLastCombatTick"), data.getLong("CuLastCombatTick")),
            Math.max(data.getLong("SasakiKojiroLastCombatTick"), data.getLong("LastHurtTick"))
         )
      );
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

      // Keep launched combat targets inside the vanilla goal system's tracking envelope.
      if (this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
         this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(96.0);
      }

      // Berserker 职阶额外移速补正（狂化加护）
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
    * 生成时自动装备默认武器到主手    */
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

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public Vec3 getHandItemOffset() {
      Vec3 offset = this.getSpecialization().handItemOffset();
      if (offset.lengthSqr() == 0.0) {
         return new Vec3(0.15, -0.8, 0.01);
      }
      return offset;
   }

   /**
    * 赫拉克勒斯不受石斧剑的负面 Debuff 影响    */
   public boolean isExemptFromStoneAxeDebuff() {
      return this.getSpecialization().immuneToStoneAxeDebuff();
   }

   // ======================== 动画触发方法 ========================

   /**
    * 触发咆哮动画（3 秒）    */
   public void triggerRoarAnimation() {
      this.roarAnimationTicks = 60; // 3s
      playActionAnimation("roar");
      ServantVoiceHelper.tryPlayRoar(this);
      if (this.level() instanceof ServerLevel sl) {
         if (this instanceof HeraclesEntity) {
            VFXServerEffects.spawn(sl, "servant_heracles_roar", this, 128.0);
         }
         /* 咆哮粒子效果 */         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
            20, 0.8, 0.6, 0.8, 0.15);
         sl.sendParticles(ParticleTypes.POOF,
            this.getX(), this.getY() + 1.2, this.getZ(),
            10, 0.5, 0.5, 0.5, 0.1);
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            this.getX(), this.getY() + 0.5, this.getZ(),
            15, 1.0, 0.3, 1.0, 0.05);
         /* 地面碎裂效果 */         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(), this.getY() + 0.1, this.getZ(),
            20, 1.2, 0.1, 1.2, 0.08);
         // 鍜嗗摦闊虫晥
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.2F, 0.7F);
      }
   }

   /**
    * 触发砸地动画 + AOE 伤害    */
   public void triggerGroundSlam() {
      this.slamAnimationTicks = 40; // 2s
      playActionAnimation("slam");
      if (this.level() instanceof ServerLevel sl) {
         if (this instanceof HeraclesEntity) {
            VFXServerEffects.spawn(sl, "servant_heracles_slam", this.position(), 128.0);
         }
         /* 砸地粒子效果 */         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + 0.3, this.getZ(),
            30, 1.5, 0.3, 1.5, 0.3);
         sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.2, this.getZ(),
            15, 1.2, 0.4, 1.2, 0.1);
         sl.sendParticles(ParticleTypes.POOF,
            this.getX(), this.getY() + 0.5, this.getZ(),
            12, 1.0, 0.3, 1.0, 0.15);
         /* 同心圆冲击波 */         for (int ring = 1; ring <= 3; ring++) {
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
         TerrainImpactProfile.Tier tier = this instanceof HeraclesEntity
            ? TerrainImpactProfile.Tier.HEAVY : TerrainImpactProfile.Tier.MEDIUM;
         TerrainImpactService.impact(sl, this, this.position().add(0.0, 0.2, 0.0),
            TerrainImpactProfile.of(tier), TerrainImpactService.Shape.GROUND_LOWER_HEMISPHERE);
         // 鐮稿湴闊虫晥
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.5F);
         sl.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.6F);
      }
   }

   /**
    * 触发跳跃攻击动画    */
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
    * 触发冲刺攻击动画    */
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
    * 触发横扫攻击动画    */
   public void triggerSweepAnimation() {
      this.sweepAnimationTicks = 15; // 0.75s
      playActionAnimation("sweep");
      ServantVoiceHelper.tryPlayAttack(this);
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
      this.slashAnimationTicks = 10; // 0.5s
      playActionAnimation("slash");
      ServantVoiceHelper.tryPlayAttack(this);
      if (this.level() instanceof ServerLevel sl) {
         // 鏂╁嚮寮х嚎绮掑瓙
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
         /* 瞬移烟雾粒子（原位残留） */         sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
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
    * 触发跺脚动画    */
   public void triggerStompAnimation() {
      this.stompAnimationTicks = 20; // 1s
      playActionAnimation("stomp");
      if (this.level() instanceof ServerLevel sl) {
         // 璺鸿剼鍐插嚮娉?
         sl.sendParticles(ParticleTypes.CLOUD,
            this.getX(), this.getY() + 0.2, this.getZ(),
            25, 1.0, 0.2, 1.0, 0.25);
         sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            this.getX(), this.getY() + 0.1, this.getZ(),
            12, 0.8, 0.2, 0.8, 0.1);
         // 鍚屽績鍦嗘墿鏁?
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
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 0.7F);
      }
   }

   /**
    * 触发上挑动作：右臂抬到右上后挥砍到左下    */
   public void triggerUppercutAnimation() {
      this.uppercutAnimationTicks = 10; // 0.5s
      playActionAnimation("uppercut");
      ServantVoiceHelper.tryPlayAttack(this);
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
    * 触发横斩动作：右臂横在胸前后向右挥出    */
   public void triggerHorizontalSwingAnimation() {
      this.horizontalSwingAnimationTicks = 10; // 0.5s
      playActionAnimation("horizontal_swing");
      ServantVoiceHelper.tryPlayAttack(this);
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

   public void triggerRuneCastAnimation() {
      this.triggerRuneCastAnimation(16);
   }

   @Override
   public boolean isInvisibleTo(Player player) {
      return this.isInvisible() || super.isInvisibleTo(player);
   }

   @Override
   public boolean isCustomNameVisible() {
      return !this.isInvisible() && super.isCustomNameVisible();
   }

   @Override
   public boolean isCurrentlyGlowing() {
      return !this.isInvisible() && super.isCurrentlyGlowing();
   }

   @Override
   public boolean displayFireAnimation() {
      return !this.isInvisible() && super.displayFireAnimation();
   }

   public void triggerRuneCastAnimation(int durationTicks) {
      this.runeCastAnimationTicks = Math.max(this.runeCastAnimationTicks, Math.max(1, durationTicks));
      playActionAnimation("rune_cast");
   }

   public void triggerGaeBolgThrowAnimation() {
      this.triggerGaeBolgThrowAnimation(18);
   }

   public void triggerGaeBolgThrowAnimation(int durationTicks) {
      this.gaeBolgThrowAnimationTicks = Math.max(this.gaeBolgThrowAnimationTicks, durationTicks);
      playActionAnimation("gae_bolg_throw");
      ServantVoiceHelper.tryPlayGaeBolg(this);
   }

   public void faceToward(Vec3 target) {
      this.faceVector(target.subtract(this.position()));
   }

   public void faceVector(Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         return;
      }
      float yaw = (float)(Mth.atan2(horizontal.z, horizontal.x) * 180.0F / Math.PI) - 90.0F;
      this.setYRot(yaw);
      this.yRotO = yaw;
      this.setYHeadRot(yaw);
      this.yHeadRotO = yaw;
      this.yBodyRot = yaw;
      this.yBodyRotO = yaw;
   }

   /**
    * 近战攻击时触发右手大幅摆臂（无专属动作时的保底反馈）    */
   public void triggerAttackSwing() {
      if (this.hasActionAnimation("uppercut") || this.hasActionAnimation("horizontal_swing")) {
         this.triggerBasicAttackAnimation();
         return;
      }

      ServantVoiceHelper.tryPlayAttack(this);
      this.attackSwingTicks = 6; // 0.3s
   }

   public boolean isAttackSwinging() {
      return this.attackSwingTicks > 0;
   }

   public boolean isPerformingAction() {
      return this.jumpAttackAnimationTicks > 0 || this.chargeAnimationTicks > 0
         || this.sweepAnimationTicks > 0 || this.slashAnimationTicks > 0
         || this.teleportAnimationTicks > 0 || this.stompAnimationTicks > 0
         || this.uppercutAnimationTicks > 0 || this.horizontalSwingAnimationTicks > 0
         || this.runeCastAnimationTicks > 0 || this.gaeBolgThrowAnimationTicks > 0;
   }

   public boolean isSoftCombatActionActive() {
      return this.runeCastAnimationTicks > 0 || this.gaeBolgThrowAnimationTicks > 0;
   }

   public boolean isHardCombatActionActive() {
      return this.isPerformingAction() && !this.isSoftCombatActionActive();
   }

   public boolean isRoaring() {
      return this.roarAnimationTicks > 0;
   }

   public boolean isSlamming() {
      return this.slamAnimationTicks > 0;
   }

   public void triggerTsurigameshiAnimation() {
      playActionAnimation("tsurigameshi");
      ServantVoiceHelper.tryPlayTsurigameshi(this);
      if (this.level() instanceof ServerLevel sl) {
         VFXServerEffects.spawn(sl, "servant_sasaki_tsubame", this, 96.0);
      }
   }

   public void triggerBasicAttackAnimation() {
      if (this.isPerformingAction() || this.isRoaring() || this.isSlamming()) {
         this.attackSwingTicks = 6;
         return;
      }

      // Zhao Yun's basic attack chain is a four-part spear routine. Keep the
      // common attack entry point so all normal combat AI can use the extra
      // weapon work without registering a separate attack sound.
      if (this instanceof ZhaoYunRiderEntity && this.hasActionAnimation("spear_flourish")
         && this.hasActionAnimation("spear_dance")) {
         String animation = switch (this.basicAttackVariant++ & 3) {
            case 0 -> "uppercut";
            case 1 -> "horizontal_swing";
            case 2 -> "spear_flourish";
            default -> "spear_dance";
         };
         this.triggerNamedActionAnimation(animation);
         if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
               SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.1F, 0.9F);
         }
         return;
      }

      boolean hasDiagonal = this.hasActionAnimation("uppercut");
      boolean hasHorizontal = this.hasActionAnimation("horizontal_swing");
      if (!hasDiagonal && !hasHorizontal) {
         this.attackSwingTicks = 6;
         return;
      }

      this.basicAttackVariant++;
      if (hasDiagonal && hasHorizontal) {
         if ((this.basicAttackVariant & 1) == 0) {
            this.triggerUppercutAnimation();
         } else {
            this.triggerHorizontalSwingAnimation();
         }
         return;
      }

      if (hasDiagonal) {
         this.triggerUppercutAnimation();
      } else {
         this.triggerHorizontalSwingAnimation();
      }
   }

   public boolean isSpiritualDissolving() {
      return this.entityData.get(SPIRITUAL_DISSOLVING);
   }

   public float getSpiritualDissolveProgress(float partialTick) {
      if (!this.isSpiritualDissolving()) {
         return 0.0F;
      }

      return Math.min(1.0F, (this.spiritualDissolveTicks + partialTick) / (float)SPIRITUAL_DISSOLVE_DURATION);
   }

   // ======================== 姝讳骸鐗规晥 ========================

   @Override
   public void die(net.minecraft.world.damagesource.DamageSource cause) {
      super.die(cause);
      net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService.onEntityServantDeath(this);
      ServantVoiceHelper.tryPlayFail(this);
      this.entityData.set(SPIRITUAL_DISSOLVING, true);
      this.spiritualDissolveTicks = 0;
      this.deathTime = 0;
      this.hurtTime = 0;
      this.hurtDuration = 0;
      this.setPose(Pose.STANDING);
      this.setDeltaMovement(Vec3.ZERO);
      this.setNoGravity(true);
      this.getNavigation().stop();
      this.setTarget(null);
      if (this.level() instanceof ServerLevel sl) {
         // 鐏典綋娑堟暎绮掑瓙锛堝弬鑰?Fate 鑻辩伒娑堝け鏁堟灉锛?
         // 浠庤韩浣撳簳閮ㄥ悜涓婂崌璧风殑閲戣壊鍏夌矑
         sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
            this.getX(), this.getY() + 0.2, this.getZ(),
            40, 0.4, 0.8, 0.4, 0.06);
         // 闂厜绮掑瓙鐜粫涓婂崌
         sl.sendParticles(ParticleTypes.END_ROD,
            this.getX(), this.getY() + 0.5, this.getZ(),
            30, 0.5, 1.0, 0.5, 0.04);
         // 鐏甸瓊鐏劙绮掑瓙锛堢伒鍩烘秷鏁ｏ級
         sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
            this.getX(), this.getY() + 1.0, this.getZ(),
            25, 0.6, 0.8, 0.6, 0.03);
         /* 灵魂上升粒子 */         sl.sendParticles(ParticleTypes.SOUL,
            this.getX(), this.getY() + 1.5, this.getZ(),
            20, 0.3, 1.2, 0.3, 0.05);
         // 鐢卞唴鍚戝鎵╂暎鐨勭櫧鑹插厜鐜?
         sl.sendParticles(ParticleTypes.FLASH,
            this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
            5, 0.0, 0.0, 0.0, 0.0);
         // 韬綋閫愭笎閫忔槑鐨勭伆鐑晥鏋?
         sl.sendParticles(ParticleTypes.ASH,
            this.getX(), this.getY() + 0.5, this.getZ(),
            35, 0.8, 1.0, 0.8, 0.08);
         // 鍦伴潰鍏夋煴涓婂崌鏁堟灉
         sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
            this.getX(), this.getY() + 0.1, this.getZ(),
            20, 0.3, 0.5, 0.3, 0.06);
      }
   }

   @Override
   protected void tickDeath() {
      if (!this.isSpiritualDissolving()) {
         super.tickDeath();
         return;
      }

      this.deathTime = 0;
      this.hurtTime = 0;
      this.hurtDuration = 0;
      this.setPose(Pose.STANDING);
      this.setDeltaMovement(Vec3.ZERO);
      this.setNoGravity(true);
      this.getNavigation().stop();
      this.setTarget(null);
      this.spiritualDissolveTicks++;

      if (this.level() instanceof ServerLevel sl) {
         float progress = Math.min(1.0F, this.spiritualDissolveTicks / (float)SPIRITUAL_DISSOLVE_DURATION);
         double effectY = this.getY() + 0.1 + this.getBbHeight() * progress;
         if (this.spiritualDissolveTicks % 2 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
               this.getX(), effectY, this.getZ(),
               5, 0.18, 0.08, 0.18, 0.01);
            sl.sendParticles(ParticleTypes.SOUL,
               this.getX(), effectY - 0.08, this.getZ(),
               4, 0.22, 0.15, 0.22, 0.02);
         }

         if (this.spiritualDissolveTicks % 4 == 0) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
               this.getX(), effectY - 0.02, this.getZ(),
               3, 0.14, 0.12, 0.14, 0.015);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
               this.getX(), effectY, this.getZ(),
               2, 0.1, 0.05, 0.1, 0.01);
         }

         if (this.spiritualDissolveTicks >= SPIRITUAL_DISSOLVE_DURATION) {
            sl.sendParticles(ParticleTypes.FLASH,
               this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
               1, 0.0, 0.0, 0.0, 0.0);
            sl.sendParticles(ParticleTypes.END_ROD,
               this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
               20, 0.25, 0.45, 0.25, 0.04);
            sl.sendParticles(ParticleTypes.SOUL,
               this.getX(), this.getY() + this.getBbHeight() * 0.7, this.getZ(),
               16, 0.3, 0.55, 0.3, 0.03);
            this.discard();
         }
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
      if (masterUuid != null) tag.putUUID("EntityMaster", masterUuid);
      if (!contractId.isBlank()) tag.putString("EntityContractId", contractId);
      tag.putString("EntityCommandMode", commandMode.name());
      tag.putLong("EntityStayAnchor", stayAnchor.asLong());
      tag.putBoolean("EntityNpPermission", masterNoblePhantasmPermission);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      String loadedId = tag.getString("ServantId");
      this.setServantId(loadedId == null || loadedId.isEmpty() ? this.servantId : loadedId);
      this.xpReward = tag.getInt("Xp");
      this.entityData.set(OBEDIENCE_AXIS, tag.getInt("ObedienceAxis"));
      this.entityData.set(PRINCIPLE_AXIS, tag.getInt("PrincipleAxis"));
      this.entityData.set(SOCIAL_DISPOSITION, tag.getInt("SocialDisposition"));
      this.entityData.set(COMBAT_DISPOSITION, tag.getInt("CombatDisposition"));
      this.entityData.set(FAVOR, (float) tag.getDouble("Favor"));
      this.entityData.set(CURRENT_MP, (float) tag.getDouble("CurrentMp"));
      this.masterUuid = tag.hasUUID("EntityMaster") ? tag.getUUID("EntityMaster") : null;
      this.contractId = tag.getString("EntityContractId");
      this.commandMode = ServantCommandMode.byName(tag.getString("EntityCommandMode"));
      this.stayAnchor = tag.contains("EntityStayAnchor") ? BlockPos.of(tag.getLong("EntityStayAnchor")) : blockPosition();
      this.masterNoblePhantasmPermission = tag.getBoolean("EntityNpPermission");
      this.cachedDefinition = null;

      /* 兼容旧存档中没有 ServantId 的情况 */      if (loadedId == null || loadedId.isEmpty()) {
         this.entityData.set(SERVANT_ID, this.servantId);
         this.cachedDefinition = null;
      }

      this.equipDefaultWeapon();
      this.applyDefinitionAttributes(false);
   }

   // ======================== Getters / Setters ========================

   public String getServantId() {
      String synced = this.entityData.get(SERVANT_ID);
      return synced == null || synced.isEmpty() ? this.servantId : synced;
   }

   @Deprecated(forRemoval = false)
   public void setServantId(String id) {
      this.servantId = id == null ? "" : id;
      this.entityData.set(SERVANT_ID, this.servantId);
      this.cachedDefinition = null;
      /* 立即从 ServantDataRegistry 重新查找定义 */      if (!this.servantId.isEmpty()) {
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

   public MoralAxis getMoralAxis() {
      ServantDefinition definition = this.getDefinition();
      return definition != null ? definition.defaultMorality() : MoralAxis.NEUTRAL;
   }

   public boolean hasSpecialTargetPrinciple(SpecialTargetPrinciple principle) {
      if (principle == null) {
         return false;
      }
      ServantDefinition definition = this.getDefinition();
      return definition != null && definition.specialTargetPrinciples().contains(principle);
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
      if (SPIRITUAL_DISSOLVING.equals(key) && this.isSpiritualDissolving()) {
         this.spiritualDissolveTicks = 0;
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

   protected boolean useFloatingAnimation() {
      return false;
   }

   @Nullable
   protected String getLoopAnimationOverride(ServantAnimations animations, boolean moving) {
      return null;
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
      // 涓绘帶鍒跺櫒锛歩dle / walk
      controllers.add(new AnimationController<>(this, "controller", 0, event -> {
         var animations = this.getAnimationSet();
         String animation = null;
         boolean moving = this.isWalkAnimationActive(event.isMoving());
         String override = this.getLoopAnimationOverride(animations, moving);
         if (override != null && !override.isBlank()) {
            animation = override;
         }
         if (this.useFloatingAnimation()) {
            animation = animations.actionAnimation("fly").orElse(null);
         }
         if (animation == null) {
            animation = moving ? animations.walkAnimation().orElse(null) : animations.idleAnimation().orElse(null);
         }
         return animation != null ? event.setAndContinue(RawAnimation.begin().thenLoop(animation)) : PlayState.STOP;
      }));
      // 鍔ㄤ綔鎺у埗鍣紙transition = 0锛屾壙杞芥妧鑳戒笌鏀诲嚮鍔ㄤ綔锛?
      this.actionCtrl = new AnimationController<>(this, "action_controller", 0, event -> null);
      controllers.add(this.actionCtrl);
      for (var entry : this.getAnimationSet().actions().entrySet()) {
         String key = entry.getKey();
         String animation = entry.getValue();
         if (key != null && !key.isBlank() && animation != null && !animation.isBlank()) {
            this.actionCtrl.triggerableAnim(key, RawAnimation.begin().thenPlay(animation));
         }
      }
      /* 咆哮控制器（transition = 0，独立控制） */      controllers.add(new AnimationController<>(this, "roar_controller", 0, event -> {
         if (this.roarAnimationTicks > 0) {
            String animation = this.getAnimationSet().actionAnimation("roar").orElse(null);
            return animation != null ? event.setAndContinue(RawAnimation.begin().thenPlay(animation)) : null;
         }
         return null;
      }));
      // 鐮稿湴鎺у埗鍣紙transition = 0锛?
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

