package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.advancement.TypeMoonAdvancementHelper;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuMove;
import net.xxxjk.TYPE_MOON_WORLD.martial.GanryuNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MysteriousSwordsmanEntity extends HumanNpcEntity implements GeoEntity {
   private static final String TAG_PLAYER = "DuelPlayer";
   private static final String TAG_START = "DuelStart";
   private static final String TAG_ACTIVE = "DuelActive";
   private static final String PLAYER_MASTER = "TypeMoonGanryuSparringMaster";
   private static final String INVITE_COOLDOWN = "TypeMoonGanryuInvitationCooldown";
   private static final String HOME_X = "GraveyardHomeX";
   private static final String HOME_Y = "GraveyardHomeY";
   private static final String HOME_Z = "GraveyardHomeZ";
   private static final String TAG_BOUND = "GraveyardBound";
   private static final String TAG_RETALIATION_TARGET = "RetaliationTarget";
   private static final String TAG_RETALIATION_UNTIL = "RetaliationUntil";
   private static final String TAG_HEALTH_200_MIGRATED = "TypeMoonGanryuHealth200Migrated";
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public MysteriousSwordsmanEntity(EntityType<? extends MysteriousSwordsmanEntity> type, Level level) {
      super(type, level);
      this.setPersistenceRequired();
      this.setCustomName(Component.translatable("entity.typemoonworld.mysterious_swordsman"));
      this.setCustomNameVisible(true);
   }

   @Override protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, GanryuNpcCombatController.combatGoal(this, this::combatProficiency));
      this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.30)
         .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
   }

   @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
      if (this.level().isClientSide) return InteractionResult.SUCCESS;
      ServerPlayer serverPlayer = (ServerPlayer)player;
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack invitation = serverPlayer.getMainHandItem().is(ModItems.SPARRING_INVITATION.get())
         ? serverPlayer.getMainHandItem() : serverPlayer.getOffhandItem().is(ModItems.SPARRING_INVITATION.get()) ? serverPlayer.getOffhandItem() : ItemStack.EMPTY;
      if (!invitation.isEmpty()) {
         if (!GanryuCombatService.isReadyWithInvitation(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.duel.prepare"), true);
            return InteractionResult.CONSUME;
         }
         if (hasDuel()) return InteractionResult.CONSUME;
         clearRetaliation();
          consumeInvitation(serverPlayer, invitation);
         this.getPersistentData().putUUID(TAG_PLAYER, serverPlayer.getUUID());
         this.getPersistentData().putLong(TAG_START, this.level().getGameTime() + 100L);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.duel.countdown"), false);
         return InteractionResult.CONSUME;
      }
      if (!vars.ganryu_learned) {
         if (!serverPlayer.getInventory().contains(new ItemStack(ModItems.GANRYU_MANUAL.get()))) {
            serverPlayer.getInventory().add(new ItemStack(ModItems.GANRYU_MANUAL.get()));
         }
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.master.manual"), false);
         return InteractionResult.CONSUME;
      }
      long now = this.level().getGameTime();
      if (serverPlayer.getPersistentData().getLong(INVITE_COOLDOWN) <= now
         && !serverPlayer.getInventory().contains(new ItemStack(ModItems.SPARRING_INVITATION.get()))) {
         serverPlayer.getInventory().add(new ItemStack(ModItems.SPARRING_INVITATION.get()));
         serverPlayer.getPersistentData().putLong(INVITE_COOLDOWN, now + 24000L);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.master.invitation"), false);
      } else {
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.master.wait"), true);
      }
      return InteractionResult.CONSUME;
   }

   @Override protected void customServerAiStep() {
      super.customServerAiStep();
      migrateHealthTo200();
      ensureWeapon();
      if (!this.hasCustomName()) this.setCustomName(Component.translatable("entity.typemoonworld.mysterious_swordsman"));
      CompoundTag data = this.getPersistentData();
      if (!data.hasUUID(TAG_PLAYER)) {
         LivingEntity retaliationTarget = getRetaliationTarget();
         long now = this.level().getGameTime();
         boolean mayContinue = retaliationTarget != null && retaliationTarget.isAlive();
         if (mayContinue && isGraveyardBound()) {
            mayContinue = now < data.getLong(TAG_RETALIATION_UNTIL) && retaliationTarget.position().distanceToSqr(homeCenter()) <= 400.0;
         }
         if (mayContinue) {
            this.setTarget(retaliationTarget);
            return;
         }
         clearRetaliation();
         if (isGraveyardBound()) returnHome();
         return;
      }
      ServerPlayer player = this.level().getServer().getPlayerList().getPlayer(data.getUUID(TAG_PLAYER));
      long now = this.level().getGameTime();
      if (player == null || !player.isAlive() || player.level() != this.level()
         || !GanryuCombatService.isActive(player)
         || isGraveyardBound() && player.position().distanceToSqr(homeCenter()) > 400.0) {
         endDuel(player, false);
         return;
      }
      if (!data.getBoolean(TAG_ACTIVE) && now >= data.getLong(TAG_START)) {
         data.putBoolean(TAG_ACTIVE, true);
         player.getPersistentData().putBoolean("TypeMoonGanryuSparring", true);
         player.getPersistentData().putUUID(PLAYER_MASTER, this.getUUID());
         this.setTarget(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.duel.start"), false);
      } else if (data.getBoolean(TAG_ACTIVE)) {
         this.setTarget(player);
      }
   }

   private double combatProficiency() {
      ServerPlayer player = getDuelPlayer();
      if (player == null) return 100.0;
      double proficiency = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).ganryu_proficiency;
      return proficiency >= 80.0 ? 100.0 : proficiency;
   }

   @Override public boolean hurt(DamageSource source, float amount) {
      if (!isDuelActive()) {
         boolean hurt = super.hurt(source, amount);
         if (hurt && source.getEntity() instanceof LivingEntity attacker && attacker != this && !this.isAlliedTo(attacker)
            && (!(attacker instanceof Player player) || !player.isCreative() && !player.isSpectator())) {
            this.setLastHurtByMob(attacker);
            beginRetaliation(attacker);
         }
         return hurt;
      }
      ServerPlayer player = getDuelPlayer();
      if (player == null || source.getEntity() != player || !GanryuCombatService.isMartialDamage(player)) return false;
      if (this.getHealth() - amount <= 0.0F) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         GanryuCombatService.addProficiency(player, 2.0);
         if (vars.ganryu_proficiency >= 80.0 && !vars.ganryu_tsubame_unlocked) {
            vars.ganryu_tsubame_unlocked = true;
            TypeMoonAdvancementHelper.grant(player, TypeMoonAdvancementHelper.GANRYU_TSUBAME);
            player.displayClientMessage(Component.translatable("message.typemoonworld.ganryu.tsubame_unlocked"), false);
            vars.syncPlayerVariables(player);
         }
         endDuel(player, true);
         return false;
      }
      return super.hurt(source, amount);
   }

   public void triggerMove(GanryuMove move) {
      if (move != GanryuMove.STANCE) this.swing(InteractionHand.MAIN_HAND, true);
      String trigger = switch (move) {
         case TSUBAME_GAESHI -> "tsubame";
         case STANCE -> "stance";
         case SPARROW_SLASH, SPARROW_THRUST -> "thrust";
         case SPRING_BUD, SPRING_BUD_SECOND -> "horizontal";
         default -> "slash";
      };
      this.triggerAnim("action_controller", trigger);
   }

   public void setGraveyardHome(BlockPos pos) {
      CompoundTag data = this.getPersistentData();
      data.putBoolean(TAG_BOUND, true);
      data.putInt(HOME_X, pos.getX());
      data.putInt(HOME_Y, pos.getY());
      data.putInt(HOME_Z, pos.getZ());
   }

   public boolean isGraveyardBound() { return this.getPersistentData().getBoolean(TAG_BOUND); }
   private BlockPos home() { return new BlockPos(this.getPersistentData().getInt(HOME_X), this.getPersistentData().getInt(HOME_Y), this.getPersistentData().getInt(HOME_Z)); }
   private Vec3 homeCenter() { return Vec3.atBottomCenterOf(home()); }

   private void beginRetaliation(LivingEntity attacker) {
      this.getPersistentData().putUUID(TAG_RETALIATION_TARGET, attacker.getUUID());
      if (isGraveyardBound()) this.getPersistentData().putLong(TAG_RETALIATION_UNTIL, this.level().getGameTime() + 240L);
      else this.getPersistentData().remove(TAG_RETALIATION_UNTIL);
      this.setTarget(attacker);
   }

   private LivingEntity getRetaliationTarget() {
      if (!this.getPersistentData().hasUUID(TAG_RETALIATION_TARGET) || !(this.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
      return level.getEntity(this.getPersistentData().getUUID(TAG_RETALIATION_TARGET)) instanceof LivingEntity living ? living : null;
   }

   private void clearRetaliation() {
      this.getPersistentData().remove(TAG_RETALIATION_TARGET);
      this.getPersistentData().remove(TAG_RETALIATION_UNTIL);
      this.setTarget(null);
   }

   private void returnHome() {
      double distance = this.position().distanceToSqr(homeCenter());
      if (distance > 400.0) this.teleportTo(home().getX() + 0.5, home().getY(), home().getZ() + 0.5);
      else if (distance > 16.0) this.getNavigation().moveTo(home().getX() + 0.5, home().getY(), home().getZ() + 0.5, 1.0);
   }

   private void ensureWeapon() {
      if (!this.getMainHandItem().is(ModItems.BIZEN_NAGAMITSU.get())) {
         this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BIZEN_NAGAMITSU.get()));
         this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
      }
   }

   private void migrateHealthTo200() {
      CompoundTag data = this.getPersistentData();
      if (data.getBoolean(TAG_HEALTH_200_MIGRATED)) return;
      float oldMaximum = this.getMaxHealth();
      float healthRatio = oldMaximum > 0.0F ? this.getHealth() / oldMaximum : 1.0F;
      AttributeInstance maximumHealth = this.getAttribute(Attributes.MAX_HEALTH);
      if (maximumHealth != null && maximumHealth.getBaseValue() < 200.0) maximumHealth.setBaseValue(200.0);
      this.setHealth(this.getMaxHealth() * Math.min(1.0F, healthRatio));
      data.putBoolean(TAG_HEALTH_200_MIGRATED, true);
   }

   private static void consumeInvitation(ServerPlayer player, ItemStack invitation) {
      int remaining = Math.max(0, invitation.getCount() - (player.getAbilities().instabuild ? 0 : 1));
      ItemStack remainder = invitation.copy();
      invitation.setCount(0);
      if (remaining <= 0) return;
      remainder.setCount(remaining);
      for (int slot = 0; slot < 36; slot++) {
         if (slot != player.getInventory().selected && player.getInventory().getItem(slot).isEmpty()) {
            player.getInventory().setItem(slot, remainder);
            return;
         }
      }
      player.drop(remainder, false);
   }

   public boolean isDuelActive() { return this.getPersistentData().getBoolean(TAG_ACTIVE); }
   public boolean hasDuel() { return this.getPersistentData().hasUUID(TAG_PLAYER); }
   public ServerPlayer getDuelPlayer() {
      return this.getPersistentData().hasUUID(TAG_PLAYER) ? this.level().getServer().getPlayerList().getPlayer(this.getPersistentData().getUUID(TAG_PLAYER)) : null;
   }

   public void endDuel(ServerPlayer player, boolean won) {
      boolean wasActive = this.getPersistentData().getBoolean(TAG_ACTIVE);
      if (player != null) {
         player.getPersistentData().remove("TypeMoonGanryuSparring");
         player.getPersistentData().remove(PLAYER_MASTER);
         if (wasActive && !won && player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).ganryu_proficiency < 80.0) {
            GanryuCombatService.addProficiency(player, 0.5);
         }
         player.displayClientMessage(Component.translatable(won ? "message.typemoonworld.ganryu.duel.win" : "message.typemoonworld.ganryu.duel.lose"), false);
      }
      this.getPersistentData().remove(TAG_PLAYER);
      this.getPersistentData().remove(TAG_START);
      this.getPersistentData().remove(TAG_ACTIVE);
      clearRetaliation();
      this.setHealth(this.getMaxHealth());
      if (isGraveyardBound()) returnHome();
   }

   @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
      controllers.add(new AnimationController<>(this, "controller", 4, event -> event.setAndContinue(
         event.isMoving() ? RawAnimation.begin().thenLoop("animation.sasaki_kojiro.walk") : RawAnimation.begin().thenLoop("animation.sasaki_kojiro.idle")
      )));
      AnimationController<MysteriousSwordsmanEntity> action = new AnimationController<>(this, "action_controller", 0, event -> PlayState.STOP);
      action.triggerableAnim("stance", RawAnimation.begin().thenPlay("animation.sasaki_kojiro.idle"));
      action.triggerableAnim("thrust", RawAnimation.begin().thenPlay("animation.sasaki_kojiro.slash_diagonal"));
      action.triggerableAnim("slash", RawAnimation.begin().thenPlay("animation.sasaki_kojiro.slash_diagonal"));
      action.triggerableAnim("horizontal", RawAnimation.begin().thenPlay("animation.sasaki_kojiro.horizontal_swing"));
      action.triggerableAnim("tsubame", RawAnimation.begin().thenPlay("animation.sasaki_kojiro.swallow_return"));
      controllers.add(action);
   }

   @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
