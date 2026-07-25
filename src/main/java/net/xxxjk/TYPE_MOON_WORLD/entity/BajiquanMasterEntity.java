package net.xxxjk.TYPE_MOON_WORLD.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.BajiquanNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.NpcActionPose;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.jetbrains.annotations.Nullable;

public class BajiquanMasterEntity extends HumanNpcEntity implements NpcActionPose {
   private static final String TAG_PLAYER = "DuelPlayer";
   private static final String TAG_START = "DuelStart";
   private static final String TAG_ACTIVE = "DuelActive";
   private static final String PLAYER_MASTER = "TypeMoonBajiquanSparringMaster";
   private static final String INVITE_COOLDOWN = "TypeMoonBajiquanInvitationCooldown";
   private static final String HOME_X = "DojoHomeX";
   private static final String HOME_Y = "DojoHomeY";
   private static final String HOME_Z = "DojoHomeZ";
   private static final String TAG_DOJO_BOUND = "DojoBound";
   private static final String TAG_RETALIATION_TARGET = "RetaliationTarget";
   private static final String TAG_RETALIATION_UNTIL = "RetaliationUntil";
   private static final EntityDataAccessor<Integer> ACTION_POSE = SynchedEntityData.defineId(BajiquanMasterEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> ACTION_TICKS = SynchedEntityData.defineId(BajiquanMasterEntity.class, EntityDataSerializers.INT);

   public BajiquanMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
   }

   @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTION_POSE, 0);
      builder.define(ACTION_TICKS, 0);
   }
   @Override public void tick() {
      super.tick();
      if (this.entityData.get(ACTION_TICKS) > 0) {
         this.entityData.set(ACTION_TICKS, this.entityData.get(ACTION_TICKS) - 1);
         if (this.entityData.get(ACTION_TICKS) <= 0) this.entityData.set(ACTION_POSE, 0);
      }
   }
   @Override public int getNpcActionPose() { return this.entityData.get(ACTION_POSE); }
   @Override public int getNpcActionPoseTicks() { return this.entityData.get(ACTION_TICKS); }
   @Override public void triggerNpcActionPose(int pose, int ticks) { this.entityData.set(ACTION_POSE, Math.max(0, pose)); this.entityData.set(ACTION_TICKS, Math.max(0, ticks)); }

   @Override protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, BajiquanNpcCombatController.combatGoal(this, this::combatProficiency, false));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false, monster -> !hasDuel()));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.30)
         .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 28.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData
   ) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
      NpcScaleHelper.ensureRandomScale(this);
      this.ensureRandomName();
      return data;
   }

   public void ensureRandomName() {
      if (!this.level().isClientSide() && (!this.hasCustomName() || ChineseNpcNameGenerator.isGenericEntityName(
         this.getCustomName(), "entity.typemoonworld.bajiquan_master"
      ))) {
         this.setCustomName(Component.literal(ChineseNpcNameGenerator.master(this.random)));
         this.setCustomNameVisible(true);
      }
   }

   @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
      if (this.level().isClientSide) return InteractionResult.SUCCESS;
      ServerPlayer serverPlayer = (ServerPlayer)player;
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack held = serverPlayer.getItemInHand(hand);
      if (held.is(ModItems.SPARRING_INVITATION.get())) {
         if (!BajiquanCombatService.isActive(serverPlayer) && !BajiquanCombatService.isActiveWithInvitation(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.duel.prepare"), true);
            return InteractionResult.CONSUME;
         }
         if (hasDuel()) return InteractionResult.CONSUME;
         clearRetaliation();
         if (!serverPlayer.getAbilities().instabuild) held.shrink(1);
         this.getPersistentData().putUUID(TAG_PLAYER, serverPlayer.getUUID());
         this.getPersistentData().putLong(TAG_START, this.level().getGameTime() + 100L);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.duel.countdown"), false);
         return InteractionResult.CONSUME;
      }
      if (!vars.bajiquan_learned) {
         if (!serverPlayer.getInventory().contains(new ItemStack(ModItems.RELIC_HAJIQUAN_MANUAL.get()))) {
            serverPlayer.getInventory().add(new ItemStack(ModItems.RELIC_HAJIQUAN_MANUAL.get()));
         }
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.master.manual"), false);
         return InteractionResult.CONSUME;
      }
      long now = this.level().getGameTime();
      if (serverPlayer.getPersistentData().getLong(INVITE_COOLDOWN) <= now
         && !serverPlayer.getInventory().contains(new ItemStack(ModItems.SPARRING_INVITATION.get()))) {
         serverPlayer.getInventory().add(new ItemStack(ModItems.SPARRING_INVITATION.get()));
         serverPlayer.getPersistentData().putLong(INVITE_COOLDOWN, now + 24000L);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.master.invitation"), false);
      } else {
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.master.wait"), true);
      }
      return InteractionResult.CONSUME;
   }

   @Override protected void customServerAiStep() {
      super.customServerAiStep();
      NpcScaleHelper.ensureRandomScale(this);
      this.ensureRandomName();
      CompoundTag data = this.getPersistentData();
      if (!data.hasUUID(TAG_PLAYER)) {
         LivingEntity retaliationTarget = getRetaliationTarget();
         long now = this.level().getGameTime();
         boolean mayContinue = retaliationTarget != null && retaliationTarget.isAlive();
         if (mayContinue && isDojoBound()) {
            mayContinue = now < data.getLong(TAG_RETALIATION_UNTIL)
               && retaliationTarget.position().distanceToSqr(homeCenter()) <= 400.0;
         }
         if (mayContinue) {
            this.setTarget(retaliationTarget);
            return;
         }
         clearRetaliation();
         if (isDojoBound()) returnHome();
         return;
      }
      ServerPlayer player = this.level().getServer().getPlayerList().getPlayer(data.getUUID(TAG_PLAYER));
      long now = this.level().getGameTime();
      if (player == null || !player.isAlive() || player.level() != this.level()
         || isDojoBound() && player.position().distanceToSqr(homeCenter()) > 400.0) {
         endDuel(player, false);
         return;
      }
      if (!data.getBoolean(TAG_ACTIVE) && now >= data.getLong(TAG_START)) {
         if (!BajiquanCombatService.isActive(player)
            && !(player.getAbilities().instabuild && BajiquanCombatService.isActiveWithInvitation(player))) {
            endDuel(player, false);
            return;
         }
         data.putBoolean(TAG_ACTIVE, true);
         player.getPersistentData().putBoolean("TypeMoonBajiquanSparring", true);
         player.getPersistentData().putUUID(PLAYER_MASTER, this.getUUID());
         this.setTarget(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.bajiquan.duel.start"), false);
      } else if (data.getBoolean(TAG_ACTIVE)) {
         this.setTarget(player);
      }
   }

   private double combatProficiency() {
      ServerPlayer player = getDuelPlayer();
      if (player == null) return 100.0;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.bajiquan_proficiency >= 80.0 ? 100.0 : vars.bajiquan_proficiency;
   }

   @Override public boolean hurt(DamageSource source, float amount) {
      if (!isDuelActive()) {
         boolean hurt = super.hurt(source, amount);
         if (hurt && source.getEntity() instanceof LivingEntity attacker
            && attacker != this && !this.isAlliedTo(attacker)
            && (!(attacker instanceof Player player) || !player.isCreative() && !player.isSpectator())) {
            this.setLastHurtByMob(attacker);
            beginRetaliation(attacker);
         }
         return hurt;
      }
      ServerPlayer player = getDuelPlayer();
      if (player == null || source.getEntity() != player || !BajiquanCombatService.isMartialDamage(player)) return false;
      if (this.getHealth() - amount <= 0.0F) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         BajiquanCombatService.addProficiency(player, 2.0);
         if (vars.bajiquan_proficiency >= 80.0 && !vars.bajiquan_tiger_unlocked) {
            vars.bajiquan_tiger_unlocked = true;
            TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.BAJIQUAN_NO_SECOND_STRIKE);
            vars.syncPlayerVariables(player);
         }
         endDuel(player, true);
         return false;
      }
      return super.hurt(source, amount);
   }

   @Override public boolean doHurtTarget(Entity entity) {
      boolean duelTarget = isDuelActive() && entity == getDuelPlayer();
      boolean retaliationTarget = !isDuelActive() && isRetaliationTarget(entity);
      if ((!duelTarget && !retaliationTarget) || !(entity instanceof LivingEntity target)) return false;
      boolean hit = super.doHurtTarget(entity);
      if (!hit) return false;
      ServerPlayer player = getDuelPlayer();
      double proficiency = retaliationTarget || player == null ? 100.0 : player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).bajiquan_proficiency;
      int roll = this.random.nextInt(100);
      if (proficiency >= 10.0 && roll < 45) target.addEffect(new MobEffectInstance(ModMobEffects.STAGGER, proficiency >= 80.0 ? 12 : 7, 0, false, true, true));
      if (proficiency >= 25.0 && roll >= 45 && roll < 70) {
         target.addEffect(new MobEffectInstance(ModMobEffects.OFF_BALANCE, proficiency >= 80.0 ? 40 : 25, 0, false, true, true));
         target.push(0.0, 0.35, 0.0);
      }
      if (proficiency >= 40.0 && roll >= 70) {
         Vec3 push = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
         target.push(push.x * (proficiency >= 80.0 ? 1.1 : 0.55), 0.15, push.z * (proficiency >= 80.0 ? 1.1 : 0.55));
      }
      if (proficiency >= 80.0 && roll >= 92) target.hurt(this.damageSources().mobAttack(this), 8.0F);
      if (this.level() instanceof net.minecraft.server.level.ServerLevel level) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
      }
      return true;
   }

   public void setDojoHome(BlockPos pos) {
      CompoundTag data = this.getPersistentData();
      data.putBoolean(TAG_DOJO_BOUND, true);
      data.putInt(HOME_X, pos.getX());
      data.putInt(HOME_Y, pos.getY());
      data.putInt(HOME_Z, pos.getZ());
   }

   public boolean isDojoBound() { return this.getPersistentData().getBoolean(TAG_DOJO_BOUND); }

   private BlockPos home() {
      CompoundTag data = this.getPersistentData();
      return new BlockPos(data.getInt(HOME_X), data.getInt(HOME_Y), data.getInt(HOME_Z));
   }

   private Vec3 homeCenter() { return Vec3.atBottomCenterOf(home()); }

   private void beginRetaliation(LivingEntity attacker) {
      this.getPersistentData().putUUID(TAG_RETALIATION_TARGET, attacker.getUUID());
      if (isDojoBound()) {
         this.getPersistentData().putLong(TAG_RETALIATION_UNTIL, this.level().getGameTime() + 240L);
      } else {
         this.getPersistentData().remove(TAG_RETALIATION_UNTIL);
      }
      this.setTarget(attacker);
   }

   private LivingEntity getRetaliationTarget() {
      if (!this.getPersistentData().hasUUID(TAG_RETALIATION_TARGET) || !(this.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
      return level.getEntity(this.getPersistentData().getUUID(TAG_RETALIATION_TARGET)) instanceof LivingEntity living ? living : null;
   }

   private boolean isRetaliationTarget(Entity entity) {
      return entity != null && this.getPersistentData().hasUUID(TAG_RETALIATION_TARGET)
         && entity.getUUID().equals(this.getPersistentData().getUUID(TAG_RETALIATION_TARGET));
   }

   private void clearRetaliation() {
      this.getPersistentData().remove(TAG_RETALIATION_TARGET);
      this.getPersistentData().remove(TAG_RETALIATION_UNTIL);
      this.setTarget(null);
   }

   private void returnHome() {
      double distance = this.position().distanceToSqr(homeCenter());
      if (distance > 400.0) {
         BlockPos home = home();
         this.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
      } else if (distance > 16.0) {
         this.getNavigation().moveTo(home().getX() + 0.5, home().getY(), home().getZ() + 0.5, 1.0);
      }
   }

   public boolean isDuelActive() { return this.getPersistentData().getBoolean(TAG_ACTIVE); }
   public boolean hasDuel() { return this.getPersistentData().hasUUID(TAG_PLAYER); }
   public ServerPlayer getDuelPlayer() {
      return this.getPersistentData().hasUUID(TAG_PLAYER) ? this.level().getServer().getPlayerList().getPlayer(this.getPersistentData().getUUID(TAG_PLAYER)) : null;
   }

   public boolean startDuelCommand(ServerPlayer player) {
      if (player == null || hasDuel()) return false;
      this.getPersistentData().putUUID(TAG_PLAYER, player.getUUID());
      this.getPersistentData().putLong(TAG_START, this.level().getGameTime() + 1L);
      return true;
   }

   public void endDuel(ServerPlayer player, boolean won) {
      if (player != null) {
         player.getPersistentData().remove("TypeMoonBajiquanSparring");
         player.getPersistentData().remove(PLAYER_MASTER);
         if (!won) {
            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (vars.bajiquan_proficiency < 80.0) BajiquanCombatService.addProficiency(player, 0.5);
         }
         player.displayClientMessage(Component.translatable(won ? "message.typemoonworld.bajiquan.duel.win" : "message.typemoonworld.bajiquan.duel.lose"), false);
      }
      this.getPersistentData().remove(TAG_PLAYER);
      this.getPersistentData().remove(TAG_START);
      this.getPersistentData().remove(TAG_ACTIVE);
      clearRetaliation();
      this.setTarget(null);
      this.setHealth(this.getMaxHealth());
      if (isDojoBound()) returnHome();
   }
}
