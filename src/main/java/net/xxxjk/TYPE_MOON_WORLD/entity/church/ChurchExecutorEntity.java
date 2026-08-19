package net.xxxjk.TYPE_MOON_WORLD.entity.church;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.NpcScaleHelper;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicBinding;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicHealing;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSuggestion;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.BaptismRiteEventHandler;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.BlackKeyMiracleService;
import net.xxxjk.TYPE_MOON_WORLD.magic.church.MagicBaptismRite;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantEngagementService;
import org.jetbrains.annotations.Nullable;

public class ChurchExecutorEntity extends HumanNpcEntity implements net.minecraft.world.entity.monster.RangedAttackMob, StigmaBearer {
   public static final int WEAPON_BLACK_KEY = 0;
   public static final int WEAPON_CROSSBOW = 1;
   public static final int WEAPON_PISTOL = 2;
   private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> WEAPON = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> STIGMA = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> BAPTISM = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> REINFORCEMENT = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SPIRITUAL_HEALING = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> BINDING_MAGIC = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SUGGESTION_MAGIC = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> HEALING_MAGIC = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> THEOLOGY = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> BLACK_KEY_MAKING = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IRON_ARMOR_ACTION = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> CREMATION_RITE = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> RANGED_POSE_TICKS = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> BAPTISM_CHANT_TICKS = SynchedEntityData.defineId(ChurchExecutorEntity.class, EntityDataSerializers.INT);
   private static final double PACK_COORDINATION_RANGE = 18.0;
   private static final String BASIC_MAGIC_ROSTER_VERSION = "ChurchBasicMagicRosterV1";
   private static final String MIRACLE_ROSTER_VERSION = "ChurchMiracleRosterV1";
   private static final String SKIN_ROSTER_VERSION = "ChurchSkinRosterV2";
   private int magicCooldown;
   private boolean fireBlackKeyFromOffhand;
   @Nullable private UUID baptismTargetId;

   public ChurchExecutorEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

   public static AttributeSupplier.Builder createAttributes() {
      return PathfinderMob.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.28)
         .add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.ARMOR, 2.0)
         .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE);
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SKIN, 0); builder.define(WEAPON, WEAPON_BLACK_KEY);
      builder.define(STIGMA, false); builder.define(BAPTISM, false);
      builder.define(REINFORCEMENT, false); builder.define(SPIRITUAL_HEALING, false);
      builder.define(BINDING_MAGIC, false); builder.define(SUGGESTION_MAGIC, false); builder.define(HEALING_MAGIC, false);
      builder.define(THEOLOGY, false); builder.define(BLACK_KEY_MAKING, false);
      builder.define(IRON_ARMOR_ACTION, false); builder.define(CREMATION_RITE, false);
      builder.define(RANGED_POSE_TICKS, 0);
      builder.define(BAPTISM_CHANT_TICKS, 0);
   }

   @Override protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, new ChurchCombatGoal(this));
      goalSelector.addGoal(4, new ChurchPackRegroupGoal(this));
      goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, this::isChurchTarget));
   }

   private boolean isChurchTarget(LivingEntity target) {
      if (target instanceof ChurchExecutorEntity || target instanceof HumanNpcEntity) return false;
      return DeadApostleEntity.isDeadApostle(target) || target instanceof Monster;
   }

   @Override public boolean isAlliedTo(Entity entity) { return entity instanceof ChurchExecutorEntity || super.isAlliedTo(entity); }
   @Override public boolean canAttack(LivingEntity target) { return !(target instanceof ChurchExecutorEntity) && super.canAttack(target); }

   public int getSkinVariant() { return entityData.get(SKIN); }
   public boolean isFemale() { return getSkinVariant() == 3; }
   public int getWeaponType() { return entityData.get(WEAPON); }
   @Override public boolean hasStigma() { return entityData.get(STIGMA); }
   public boolean hasBaptism() { return entityData.get(BAPTISM); }
   public boolean hasReinforcement() { return entityData.get(REINFORCEMENT); }
   public boolean hasSpiritualHealing() { return entityData.get(SPIRITUAL_HEALING); }
   public boolean hasBindingMagic() { return entityData.get(BINDING_MAGIC); }
   public boolean hasSuggestionMagic() { return entityData.get(SUGGESTION_MAGIC); }
   public boolean hasHealingMagic() { return entityData.get(HEALING_MAGIC); }
   public boolean hasTheology() { return entityData.get(THEOLOGY); }
   public boolean hasBlackKeyMaking() { return entityData.get(BLACK_KEY_MAKING); }
   public boolean hasIronArmorAction() { return entityData.get(IRON_ARMOR_ACTION); }
   public boolean hasCremationRite() { return entityData.get(CREMATION_RITE); }
   public boolean isBaptismChanting() { return entityData.get(BAPTISM_CHANT_TICKS) > 0; }
   public boolean isCrossbowAiming() { return getWeaponType() == WEAPON_CROSSBOW && entityData.get(RANGED_POSE_TICKS) > 0; }

   @Override
   public void performRangedAttack(LivingEntity target, float velocity) {
      if (level().isClientSide || target == null || isBaptismChanting()) return;
      if (getWeaponType() == WEAPON_CROSSBOW) entityData.set(RANGED_POSE_TICKS, 12);
      InteractionHand firingHand = InteractionHand.MAIN_HAND;
      Projectile projectile;
      if (getWeaponType() == WEAPON_PISTOL) {
         ContenderBulletEntity bullet = new ContenderBulletEntity(level(), this, false);
         projectile = bullet;
      } else if (getWeaponType() == WEAPON_CROSSBOW) {
         ChurchBoltEntity arrow = new ChurchBoltEntity(level(), this);
         arrow.setBaseDamage(6.0);
         projectile = arrow;
      } else {
         if (getOffhandItem().getItem() instanceof BlackKeyItem) {
            firingHand = fireBlackKeyFromOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            fireBlackKeyFromOffhand = !fireBlackKeyFromOffhand;
         }
         ItemStack stack = getItemInHand(firingHand);
         projectile = new BlackKeyProjectileEntity(level(), this, stack.copyWithCount(1), false);
      }
      swing(firingHand, true);
      positionProjectileAtHand(projectile, firingHand);
      double dx = target.getX() - getX();
      double dz = target.getZ() - getZ();
      double dy = target.getY(0.5) - projectile.getY() + Math.sqrt(dx * dx + dz * dz) * 0.12;
      float projectileSpeed = getWeaponType() == WEAPON_PISTOL ? 3.5F : getWeaponType() == WEAPON_BLACK_KEY ? 3.0F : 2.0F;
      float inaccuracy = getWeaponType() == WEAPON_BLACK_KEY ? 0.5F : 2.0F;
      projectile.shoot(dx, dy, dz, projectileSpeed, inaccuracy);
      level().addFreshEntity(projectile);
   }

   private void positionProjectileAtHand(Projectile projectile, InteractionHand hand) {
      Vec3 look = getLookAngle();
      Vec3 right = new Vec3(-look.z, 0.0, look.x);
      if (right.lengthSqr() > 1.0E-6) right = right.normalize();
      boolean physicalRightHand = (hand == InteractionHand.MAIN_HAND) == (getMainArm() == HumanoidArm.RIGHT);
      double side = physicalRightHand ? 0.28 : -0.28;
      projectile.setPos(getX() + right.x * side, getEyeY() - 0.35, getZ() + right.z * side);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                       @Nullable SpawnGroupData groupData) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, groupData);
      NpcScaleHelper.ensureRandomScale(this);
      int skin = random.nextInt(4);
      entityData.set(SKIN, skin);
      if (!hasCustomName()) {
         setCustomName(Component.literal(ChurchNameGenerator.generate(random, skin == 3)));
         setCustomNameVisible(true);
      }
      int roll = random.nextInt(200);
      entityData.set(WEAPON, roll < 95 ? WEAPON_BLACK_KEY : roll < 190 ? WEAPON_CROSSBOW : WEAPON_PISTOL);
      entityData.set(BAPTISM, random.nextBoolean());
      entityData.set(REINFORCEMENT, random.nextBoolean());
      entityData.set(SPIRITUAL_HEALING, random.nextBoolean());
      entityData.set(STIGMA, random.nextBoolean());
      getPersistentData().putInt("ChurchBaptismProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchReinforcementProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchSpiritualHealingProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchStigmaProficiency", random.nextInt(101));
      rollBasicMagics();
      rollMiracles();
      equipWeapon();
      if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) setPersistenceRequired();
      return data;
   }

   private void rollBasicMagics() {
      entityData.set(BINDING_MAGIC, random.nextBoolean());
      entityData.set(SUGGESTION_MAGIC, random.nextBoolean());
      entityData.set(HEALING_MAGIC, random.nextBoolean());
      getPersistentData().putInt("ChurchBindingProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchSuggestionProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchHealingProficiency", random.nextInt(101));
      getPersistentData().putBoolean(BASIC_MAGIC_ROSTER_VERSION, true);
   }

   private void rollMiracles() {
      boolean theology = random.nextFloat() < 0.55F;
      entityData.set(THEOLOGY, theology);
      entityData.set(BLACK_KEY_MAKING, theology && random.nextFloat() < 0.70F);
      entityData.set(IRON_ARMOR_ACTION, theology && random.nextFloat() < 0.35F);
      entityData.set(CREMATION_RITE, theology && random.nextFloat() < 0.30F);
      getPersistentData().putInt("ChurchTheologyProficiency", theology ? 100 : random.nextInt(51));
      getPersistentData().putInt("ChurchBlackKeyMakingProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchIronArmorActionProficiency", random.nextInt(101));
      getPersistentData().putInt("ChurchCremationRiteProficiency", random.nextInt(101));
      getPersistentData().putBoolean(MIRACLE_ROSTER_VERSION, true);
   }

   private void equipWeapon() {
      ItemStack stack;
      if (getWeaponType() == WEAPON_BLACK_KEY) {
         stack = createBlackKeyStack();
         if (random.nextBoolean()) {
            setItemSlot(EquipmentSlot.OFFHAND, createBlackKeyStack());
            setDropChance(EquipmentSlot.OFFHAND, 0.0F);
         } else {
            setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
         }
      } else if (getWeaponType() == WEAPON_CROSSBOW) {
         stack = new ItemStack(Items.CROSSBOW);
         setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      } else {
         stack = new ItemStack(ModItems.THOMPSON_CONTENDER.get());
         setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      }
      setItemSlot(EquipmentSlot.MAINHAND, stack);
      setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   private ItemStack createBlackKeyStack() {
      ItemStack stack = new ItemStack(ModItems.BLACK_KEY.get(), 1 + random.nextInt(3));
      BlackKeyItem.setExpanded(stack, true);
      if (random.nextBoolean()) BlackKeyItem.setFireEngraved(stack, true);
      return stack;
   }

   @Override
   protected void customServerAiStep() {
      boolean tactical = net.xxxjk.TYPE_MOON_WORLD.combat.ai.NpcTacticalController.tick(this);
      if (!tactical) super.customServerAiStep();
      NpcScaleHelper.ensureRandomScale(this);
      if (!getPersistentData().getBoolean(BASIC_MAGIC_ROSTER_VERSION)) rollBasicMagics();
      if (!getPersistentData().getBoolean(MIRACLE_ROSTER_VERSION)) rollMiracles();
      if (!tactical && tickCount % 8 == 0) coordinatePackTargets();
      int poseTicks = entityData.get(RANGED_POSE_TICKS);
      if (poseTicks > 0) entityData.set(RANGED_POSE_TICKS, poseTicks - 1);
      if (magicCooldown > 0) magicCooldown--;
      if (tactical) return;
      if (isBaptismChanting()) {
         tickBaptismChant();
         return;
      }
      LivingEntity target = getTarget();
      if (magicCooldown <= 0 && hasHealingMagic() && tryCastHealingMagic()) {
         magicCooldown = 90;
      } else if (magicCooldown <= 0 && hasSpiritualHealing() && getHealth() < getMaxHealth() * 0.55F) {
         heal(4.0F + getPersistentData().getInt("ChurchSpiritualHealingProficiency") * 0.04F);
         magicCooldown = 100;
      } else if (magicCooldown <= 0 && hasReinforcement() && target != null && tryCastReinforcement()) {
         magicCooldown = 80;
      } else if (magicCooldown <= 0 && hasBaptism() && isDeadApostleTarget(target)
         && tryStartBaptism(target)) {
         magicCooldown = 160;
      } else if (magicCooldown <= 0 && target != null && tryCastControlMagic()) {
         magicCooldown = 100;
      } else if (magicCooldown <= 0 && isDeadApostleTarget(target)) {
         if (hasSpiritualHealing()) {
            target.hurt(damageSources().magic(), 6.0F + getPersistentData().getInt("ChurchSpiritualHealingProficiency") * 0.06F);
            magicCooldown = 80;
         }
      }
   }

   private static boolean isDeadApostleTarget(LivingEntity target) {
      return target != null && DeadApostleEntity.isDeadApostle(target);
   }

   private boolean tryStartBaptism(LivingEntity target) {
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchBaptismProficiency"), 0, 100);
      double range = MagicBaptismRite.targetRange(proficiency);
      if (!MagicBaptismRite.isValidRiteTarget(target) || !hasLineOfSight(target) || distanceToSqr(target) > range * range) return false;
      baptismTargetId = target.getUUID();
      entityData.set(BAPTISM_CHANT_TICKS, MagicBaptismRite.chantTicks(proficiency));
      getNavigation().stop();
      swing(InteractionHand.OFF_HAND, true);
      return true;
   }

   private void tickBaptismChant() {
      LivingEntity target = resolveBaptismTarget();
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchBaptismProficiency"), 0, 100);
      double range = MagicBaptismRite.targetRange(proficiency);
      if (target == null || !target.isAlive() || !MagicBaptismRite.isValidRiteTarget(target)
         || distanceToSqr(target) > range * range || !hasLineOfSight(target)) {
         interruptBaptismChant();
         return;
      }

      getNavigation().stop();
      getLookControl().setLookAt(target, 40.0F, 40.0F);
      int remaining = entityData.get(BAPTISM_CHANT_TICKS) - 1;
      entityData.set(BAPTISM_CHANT_TICKS, Math.max(remaining, 0));
      if (tickCount % 4 == 0 && level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY(0.7), getZ(),
            7, getBbWidth() * 0.55, getBbHeight() * 0.35, getBbWidth() * 0.55, 0.12);
      }
      if (remaining <= 0) {
         baptismTargetId = null;
         BaptismRiteEventHandler.applyRite(this, target, proficiency);
         magicCooldown = Math.max(magicCooldown, 100);
      }
   }

   @Nullable
   private LivingEntity resolveBaptismTarget() {
      if (baptismTargetId != null && level() instanceof ServerLevel serverLevel
         && serverLevel.getEntity(baptismTargetId) instanceof LivingEntity living) return living;
      return null;
   }

   public void interruptBaptismChant() {
      entityData.set(BAPTISM_CHANT_TICKS, 0);
      baptismTargetId = null;
      magicCooldown = Math.max(magicCooldown, 60);
   }

   private boolean tryCastHealingMagic() {
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchHealingProficiency"), 0, 100);
      double range = proficiency >= 50 ? 8.0 : 3.0;
      LivingEntity selected = getHealth() < getMaxHealth() * 0.8F ? this : null;
      double selectedRatio = selected == null ? 1.0 : getHealth() / getMaxHealth();
      for (ChurchExecutorEntity ally : level().getEntitiesOfClass(ChurchExecutorEntity.class,
         getBoundingBox().inflate(range), executor -> executor.isAlive() && executor != this
            && executor.getHealth() < executor.getMaxHealth() * 0.8F)) {
         if (!hasLineOfSight(ally) && distanceToSqr(ally) > 9.0) continue;
         double healthRatio = ally.getHealth() / ally.getMaxHealth();
         if (healthRatio < selectedRatio) {
            selected = ally;
            selectedRatio = healthRatio;
         }
      }
      if (selected == null) return false;
      swing(InteractionHand.OFF_HAND, true);
      return MagicHealing.healDirect(this, selected, proficiency);
   }

   private boolean tryCastControlMagic() {
      LivingEntity target = getTarget();
      if (target == null || !target.isAlive() || !hasLineOfSight(target) || distanceToSqr(target) > 16.0 * 16.0) return false;
      boolean bindingFirst = random.nextBoolean();
      if (bindingFirst) {
         if (tryCastBinding(target)) return true;
         return tryCastSuggestion(target);
      }
      if (tryCastSuggestion(target)) return true;
      return tryCastBinding(target);
   }

   private boolean tryCastBinding(LivingEntity target) {
      if (!hasBindingMagic() || distanceToSqr(target) > 14.0 * 14.0) return false;
      MobEffectInstance existing = target.getEffect(ModMobEffects.BINDING);
      if (existing != null && existing.getDuration() > 40) return false;
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchBindingProficiency"), 0, 100);
      boolean applied = MagicBinding.applyBinding(this, target, proficiency,
         living -> living == target || isChurchTarget(living));
      if (applied) swing(InteractionHand.OFF_HAND, true);
      return applied;
   }

   private boolean tryCastSuggestion(LivingEntity target) {
      if (!hasSuggestionMagic()) return false;
      MobEffectInstance existing = target.getEffect(ModMobEffects.SUGGESTION);
      if (existing != null && existing.getDuration() > 100) return false;
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchSuggestionProficiency"), 0, 100);
      MagicSuggestion.applySuggestion(this, target, proficiency);
      swing(InteractionHand.OFF_HAND, true);
      return true;
   }

   private boolean tryCastReinforcement() {
      int proficiency = Mth.clamp(getPersistentData().getInt("ChurchReinforcementProficiency"), 0, 100);
      int level = Math.min(5, 1 + proficiency / 20);
      int duration = (600 + proficiency * 10) * level;
      int amplifier = level - 1;
      LivingEntity target = getTarget();
      int preferredMode = getHealth() < getMaxHealth() * 0.55F ? 0
         : target != null && distanceToSqr(target) > 49.0 ? 2 : 1;
      int[] modeOrder = level().isDay()
         ? new int[]{preferredMode, 1, 2, 0}
         : new int[]{preferredMode, 1, 2, 0, 3};

      for (int mode : modeOrder) {
         MobEffectInstance effect = reinforcementEffect(mode, duration, amplifier);
         MobEffectInstance existing = getEffect(effect.getEffect());
         if (existing != null && existing.getAmplifier() >= amplifier && existing.getDuration() > 200) continue;
         addEffect(effect);
         if (mode == 3) addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, false));
         swing(InteractionHand.OFF_HAND, true);
         if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, getX(), getY(0.55), getZ(),
               18, getBbWidth() * 0.55, getBbHeight() * 0.35, getBbWidth() * 0.55, 0.18);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.65F, 1.15F);
         }
         return true;
      }
      return false;
   }

   private static MobEffectInstance reinforcementEffect(int mode, int duration, int amplifier) {
      return switch (mode) {
         case 0 -> new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_DEFENSE, duration, amplifier, false, false, true);
         case 2 -> new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_AGILITY, duration, amplifier, false, false, true);
         case 3 -> new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_SIGHT, duration, amplifier, false, false, true);
         default -> new MobEffectInstance(ModMobEffects.REINFORCEMENT_SELF_STRENGTH, duration, amplifier, false, false, true);
      };
   }

   private void coordinatePackTargets() {
      var allies = level().getEntitiesOfClass(ChurchExecutorEntity.class,
         getBoundingBox().inflate(PACK_COORDINATION_RANGE), executor -> executor.isAlive() && executor != this);
      LivingEntity sharedTarget = getTarget();
      if (sharedTarget == null || !sharedTarget.isAlive() || !canAttack(sharedTarget)) {
         sharedTarget = null;
         for (ChurchExecutorEntity ally : allies) {
            LivingEntity allyTarget = ally.getTarget();
            if (allyTarget != null && allyTarget.isAlive() && canAttack(allyTarget)) {
               sharedTarget = allyTarget;
               setTarget(sharedTarget);
               break;
            }
         }
      }
      if (sharedTarget == null) return;
      for (ChurchExecutorEntity ally : allies) {
         LivingEntity allyTarget = ally.getTarget();
         if ((allyTarget == null || !allyTarget.isAlive()) && ally.canAttack(sharedTarget)) {
            ally.setTarget(sharedTarget);
         }
      }
   }

   @Override public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("ChurchSkin", getSkinVariant()); tag.putInt("ChurchWeapon", getWeaponType());
      tag.putBoolean(SKIN_ROSTER_VERSION, true);
      tag.putBoolean("ChurchStigma", hasStigma()); tag.putBoolean("ChurchBaptism", hasBaptism());
      tag.putBoolean("ChurchReinforcement", hasReinforcement()); tag.putBoolean("ChurchSpiritualHealing", hasSpiritualHealing());
      tag.putBoolean("ChurchBindingMagic", hasBindingMagic()); tag.putBoolean("ChurchSuggestionMagic", hasSuggestionMagic());
      tag.putBoolean("ChurchHealingMagic", hasHealingMagic());
      tag.putBoolean("ChurchTheology", hasTheology()); tag.putBoolean("ChurchBlackKeyMaking", hasBlackKeyMaking());
      tag.putBoolean("ChurchIronArmorAction", hasIronArmorAction()); tag.putBoolean("ChurchCremationRite", hasCremationRite());
      tag.putInt("ChurchBaptismChantTicks", entityData.get(BAPTISM_CHANT_TICKS));
      if (baptismTargetId != null) tag.putUUID("ChurchBaptismTarget", baptismTargetId);
   }

   @Override public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      int savedSkin = tag.getInt("ChurchSkin");
      int migratedSkin = tag.getBoolean(SKIN_ROSTER_VERSION) ? Mth.clamp(savedSkin, 0, 3)
         : savedSkin == 4 ? 3 : savedSkin == 3 ? Math.floorMod(getUUID().hashCode(), 3) : Mth.clamp(savedSkin, 0, 2);
      entityData.set(SKIN, migratedSkin); entityData.set(WEAPON, tag.getInt("ChurchWeapon"));
      entityData.set(STIGMA, tag.getBoolean("ChurchStigma")); entityData.set(BAPTISM, tag.getBoolean("ChurchBaptism"));
      entityData.set(REINFORCEMENT, tag.getBoolean("ChurchReinforcement")); entityData.set(SPIRITUAL_HEALING, tag.getBoolean("ChurchSpiritualHealing"));
      entityData.set(BINDING_MAGIC, tag.getBoolean("ChurchBindingMagic")); entityData.set(SUGGESTION_MAGIC, tag.getBoolean("ChurchSuggestionMagic"));
      entityData.set(HEALING_MAGIC, tag.getBoolean("ChurchHealingMagic"));
      entityData.set(THEOLOGY, tag.getBoolean("ChurchTheology")); entityData.set(BLACK_KEY_MAKING, tag.getBoolean("ChurchBlackKeyMaking"));
      entityData.set(IRON_ARMOR_ACTION, tag.getBoolean("ChurchIronArmorAction")); entityData.set(CREMATION_RITE, tag.getBoolean("ChurchCremationRite"));
      entityData.set(BAPTISM_CHANT_TICKS, Math.max(0, tag.getInt("ChurchBaptismChantTicks")));
      baptismTargetId = tag.hasUUID("ChurchBaptismTarget") ? tag.getUUID("ChurchBaptismTarget") : null;
   }

   public double getMiracleProficiency(String magicId) {
      return switch (magicId) {
         case BlackKeyMiracleService.THEOLOGY -> getPersistentData().getInt("ChurchTheologyProficiency");
         case BlackKeyMiracleService.BLACK_KEY_MAKING -> getPersistentData().getInt("ChurchBlackKeyMakingProficiency");
         case BlackKeyMiracleService.IRON_ARMOR_ACTION -> getPersistentData().getInt("ChurchIronArmorActionProficiency");
         case BlackKeyMiracleService.CREMATION_RITE -> getPersistentData().getInt("ChurchCremationRiteProficiency");
         default -> 0.0;
      };
   }

   private static final class ChurchPackRegroupGoal extends Goal {
      private final ChurchExecutorEntity executor;
      private ChurchExecutorEntity leader;
      private int pathCooldown;

      private ChurchPackRegroupGoal(ChurchExecutorEntity executor) {
         this.executor = executor;
         setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override public boolean canUse() {
         if (executor.getTarget() != null) return false;
         leader = findLeader();
         return leader != null && executor.distanceToSqr(leader) > 25.0;
      }

      @Override public boolean canContinueToUse() {
         return executor.getTarget() == null && leader != null && leader.isAlive()
            && executor.distanceToSqr(leader) > 9.0
            && executor.distanceToSqr(leader) <= PACK_COORDINATION_RANGE * PACK_COORDINATION_RANGE * 2.0;
      }

      @Override public void stop() {
         executor.getNavigation().stop();
         leader = null;
      }

      @Override public void tick() {
         if (leader == null) return;
         executor.getLookControl().setLookAt(leader, 30.0F, 30.0F);
         if (pathCooldown-- <= 0) {
            executor.getNavigation().moveTo(leader, 1.12);
            pathCooldown = 8 + executor.getRandom().nextInt(6);
         }
      }

      private ChurchExecutorEntity findLeader() {
         ChurchExecutorEntity selected = executor;
         for (ChurchExecutorEntity candidate : executor.level().getEntitiesOfClass(ChurchExecutorEntity.class,
            executor.getBoundingBox().inflate(PACK_COORDINATION_RANGE), ChurchExecutorEntity::isAlive)) {
            if (candidate.getId() < selected.getId()) selected = candidate;
         }
         return selected == executor ? null : selected;
      }
   }

   private static final class ChurchCombatGoal extends Goal {
      private final ChurchExecutorEntity executor;
      private int pathCooldown;
      private int meleeCooldown;
      private int rangedCooldown;
      private int tacticTicks;
      private int strafeTicks;
      private float strafeDirection;
      private boolean closeAssault;

      private ChurchCombatGoal(ChurchExecutorEntity executor) {
         this.executor = executor;
         setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      @Override public boolean canUse() {
         if (executor.isBaptismChanting()) return false;
         LivingEntity target = executor.getTarget();
         return target != null && target.isAlive() && executor.canAttack(target);
      }

      @Override public boolean canContinueToUse() { return canUse(); }
      @Override public boolean requiresUpdateEveryTick() { return true; }

      @Override public void start() {
         closeAssault = executor.getWeaponType() == WEAPON_BLACK_KEY && executor.getRandom().nextBoolean();
         chooseTactic();
         chooseStrafeDirection();
         rangedCooldown = 8 + executor.getRandom().nextInt(10);
         executor.setSprinting(true);
      }

      @Override public void stop() {
         executor.getNavigation().stop();
         executor.setSprinting(false);
      }

      @Override public void tick() {
         if (executor.isBaptismChanting()) {
            executor.getNavigation().stop();
            return;
         }
         LivingEntity target = executor.getTarget();
         if (target == null) return;
         executor.getLookControl().setLookAt(target, 40.0F, 40.0F);
         if (pathCooldown > 0) pathCooldown--;
         if (meleeCooldown > 0) meleeCooldown--;
         if (rangedCooldown > 0) rangedCooldown--;
         if (--strafeTicks <= 0) chooseStrafeDirection();
         if (--tacticTicks <= 0) {
            if (executor.getWeaponType() == WEAPON_BLACK_KEY) closeAssault = !closeAssault;
            chooseTactic();
         }

         double distanceSqr = executor.distanceToSqr(target);
         double distance = Math.sqrt(distanceSqr);
         double meleeReach = executor.getBbWidth() * 1.7F + target.getBbWidth() + 0.8F;
         boolean visible = executor.getSensing().hasLineOfSight(target);
         boolean canMelee = distance <= meleeReach;

         if (canMelee) tryMelee(target, visible);
         if (executor.getWeaponType() == WEAPON_BLACK_KEY && closeAssault) {
            pursue(target, distanceSqr, meleeReach * meleeReach);
            if (visible && distance > meleeReach * 1.15 && distance <= 18.0) tryRanged(target, 24, 33);
            return;
         }

         double idealRange = executor.getWeaponType() == WEAPON_PISTOL ? 11.0
            : executor.getWeaponType() == WEAPON_CROSSBOW ? 8.5 : 7.0;
         maintainRange(target, distance, idealRange, visible);
         if (visible && distance <= 18.0) {
            int minimumCooldown = executor.getWeaponType() == WEAPON_PISTOL ? 25
               : executor.getWeaponType() == WEAPON_CROSSBOW ? 32 : 22;
            tryRanged(target, minimumCooldown, minimumCooldown + 10);
         }
      }

      private void pursue(LivingEntity target, double distanceSqr, double reachSqr) {
         if (distanceSqr > reachSqr && pathCooldown <= 0) {
            double distance = Math.sqrt(distanceSqr);
            if (ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED && distance > 7.0) {
               Vec3 intercept = ServantEngagementService.meleeApproachPoint(executor, target, executor.level().getGameTime());
               executor.getNavigation().moveTo(intercept.x, intercept.y, intercept.z, 1.46);
            } else {
               executor.getNavigation().moveTo(target, 1.34);
            }
            pathCooldown = 3 + executor.getRandom().nextInt(4);
         } else if (distanceSqr <= reachSqr) {
            executor.getNavigation().stop();
            executor.getMoveControl().strafe(0.24F, strafeDirection * 0.48F);
         }
         if (executor.horizontalCollision && executor.onGround() && executor.getRandom().nextFloat() < 0.20F) {
            executor.getJumpControl().jump();
         }
      }

      private void maintainRange(LivingEntity target, double distance, double idealRange, boolean visible) {
         ServantEngagementService.RangeBand band = ServantEngagementService.rangedBand(
            target, Math.max(3.0, idealRange - 2.0), idealRange, idealRange + 2.0);
         if (!visible) {
            if (pathCooldown <= 0) {
               executor.getNavigation().moveTo(target, 1.22);
               pathCooldown = 4 + executor.getRandom().nextInt(5);
            }
            return;
         }
         if (distance < band.minimum() || distance > band.maximum()) {
            if (pathCooldown <= 0) {
               Vec3 destination = ServantEngagementService.rangedDestination(
                  executor, target, executor.level().getGameTime(), band);
               executor.getNavigation().moveTo(destination.x, destination.y, destination.z,
                  distance < band.minimum() ? 1.30 : 1.22);
               pathCooldown = 4 + executor.getRandom().nextInt(5);
            }
            return;
         }
         executor.getNavigation().stop();
         boolean rangedDuel = ServantEngagementService.role(target) == ServantEngagementService.CombatRole.RANGED;
         float forward = rangedDuel ? 0.10F : distance < band.preferred() ? -0.35F : 0.14F;
         executor.getMoveControl().strafe(forward, strafeDirection * (rangedDuel ? 0.86F : 0.72F));
      }

      private void tryMelee(LivingEntity target, boolean visible) {
         if (!visible || meleeCooldown > 0) return;
         executor.swing(InteractionHand.MAIN_HAND);
         executor.doHurtTarget(target);
         meleeCooldown = executor.getWeaponType() == WEAPON_BLACK_KEY ? 11 : 16;
      }

      private void tryRanged(LivingEntity target, int minimumCooldown, int maximumCooldown) {
         if (rangedCooldown > 0) return;
         executor.performRangedAttack(target, 1.0F);
         rangedCooldown = minimumCooldown + executor.getRandom().nextInt(maximumCooldown - minimumCooldown + 1);
      }

      private void chooseTactic() {
         tacticTicks = 42 + executor.getRandom().nextInt(38);
      }

      private void chooseStrafeDirection() {
         strafeDirection = executor.getRandom().nextBoolean() ? 1.0F : -1.0F;
         strafeTicks = 16 + executor.getRandom().nextInt(28);
      }
   }
}
