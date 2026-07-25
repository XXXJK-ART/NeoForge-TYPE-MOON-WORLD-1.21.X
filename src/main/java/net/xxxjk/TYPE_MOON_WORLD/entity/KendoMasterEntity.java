package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoCombatService;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoNpcCombatController;
import net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public class KendoMasterEntity extends PathfinderMob {
   private static final String TAG_SCHOOL = "TypeMoonKendoMasterSchool";
   private static final String TAG_PLAYER = "KendoDuelPlayer";
   private static final String TAG_START = "KendoDuelStart";
   private static final String TAG_ACTIVE = "KendoDuelActive";
   private static final String TAG_PLAYER_MASTER = "TypeMoonKendoSparringMaster";
   private static final String INVITE_COOLDOWN = "TypeMoonKendoInvitationCooldown";
   private static final String TAG_HOME_X = "KendoHomeX", TAG_HOME_Y = "KendoHomeY", TAG_HOME_Z = "KendoHomeZ";
   public KendoMasterEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); setPersistenceRequired(); }
   @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); goalSelector.addGoal(1, KendoNpcCombatController.combatGoal(this, school(), () -> 100.0)); goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F)); goalSelector.addGoal(4, new RandomLookAroundGoal(this)); }
   public static AttributeSupplier.Builder createAttributes() { return createMobAttributes().add(Attributes.MAX_HEALTH, 200.0).add(Attributes.MOVEMENT_SPEED, 0.30).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ARMOR, 8.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.7).add(Attributes.SCALE, NpcScaleHelper.DEFAULT_RANDOM_SCALE); }
   public KendoSchool school() { return "tennen_rishin_ryu".equals(getPersistentData().getString(TAG_SCHOOL)) ? KendoSchool.TENNEN : KendoSchool.HOKUSHIN; }
   public void setSchool(KendoSchool school) { getPersistentData().putString(TAG_SCHOOL, school.id()); }
   public void setDojoHome(net.minecraft.core.BlockPos pos) { getPersistentData().putInt(TAG_HOME_X, pos.getX()); getPersistentData().putInt(TAG_HOME_Y, pos.getY()); getPersistentData().putInt(TAG_HOME_Z, pos.getZ()); }
   @Override protected void customServerAiStep() { super.customServerAiStep(); NpcScaleHelper.ensureRandomScale(this); if (!hasCustomName()) { setCustomName(Component.literal(JapaneseNpcNameGenerator.master(random))); setCustomNameVisible(true); } }
   @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND || level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
      TypeMoonWorldModVariables.PlayerVariables vars = serverPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      ItemStack invitation = serverPlayer.getMainHandItem().is(ModItems.SPARRING_INVITATION.get()) ? serverPlayer.getMainHandItem() : serverPlayer.getOffhandItem().is(ModItems.SPARRING_INVITATION.get()) ? serverPlayer.getOffhandItem() : ItemStack.EMPTY;
      if (!invitation.isEmpty()) {
         if (!KendoCombatService.isReadyWithInvitation(serverPlayer, school())) { serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.kendo.duel.prepare"), true); return InteractionResult.CONSUME; }
         if (!getPersistentData().hasUUID(TAG_PLAYER)) { consumeInvitation(serverPlayer, invitation); getPersistentData().putUUID(TAG_PLAYER, serverPlayer.getUUID()); getPersistentData().putLong(TAG_START, level().getGameTime() + 100L); getPersistentData().putBoolean(TAG_ACTIVE, false); serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.kendo.duel.countdown"), false); }
         return InteractionResult.CONSUME;
      }
      if (!school().learned(vars)) {
         ItemStack manual = school() == KendoSchool.HOKUSHIN ? new ItemStack(ModItems.HOKUSHIN_MANUAL.get()) : new ItemStack(ModItems.TENNEN_MANUAL.get());
         if (!serverPlayer.getInventory().contains(manual)) serverPlayer.getInventory().add(manual);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.kendo.master.manual", school().displayName()), false);
      } else if (serverPlayer.getPersistentData().getLong(INVITE_COOLDOWN) <= level().getGameTime()
         && !serverPlayer.getInventory().contains(new ItemStack(ModItems.SPARRING_INVITATION.get()))) {
         serverPlayer.getInventory().add(new ItemStack(ModItems.SPARRING_INVITATION.get()));
         serverPlayer.getPersistentData().putLong(INVITE_COOLDOWN, level().getGameTime() + 24000L);
         serverPlayer.displayClientMessage(Component.translatable("message.typemoonworld.kendo.master.invitation"), false);
      }
      return InteractionResult.CONSUME;
   }
   @Override public boolean hurt(DamageSource source, float amount) {
      if (getPersistentData().getBoolean(TAG_ACTIVE) && source.getEntity() instanceof ServerPlayer player
         && getPersistentData().getUUID(TAG_PLAYER).equals(player.getUUID()) && KendoCombatService.isMartialDamage(player)) {
         boolean dead = getHealth() - amount <= 0.0F;
         if (dead) { TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES); if (school() == KendoSchool.HOKUSHIN) vars.hokushin_master_defeated = true; else vars.tennen_master_defeated = true; KendoCombatService.addProficiency(player, school(), 2.0); vars.syncPlayerVariables(player); clearDuel(); return false; }
      }
      return super.hurt(source, amount);
   }
   @Override public void tick() {
      super.tick();
      if (level().isClientSide || !getPersistentData().hasUUID(TAG_PLAYER)) return;
      ServerPlayer player = level().getServer().getPlayerList().getPlayer(getPersistentData().getUUID(TAG_PLAYER));
      if (player == null || !player.isAlive() || player.level() != level() || !KendoCombatService.isActive(player, school())) { endDuel(player, false); return; }
      if (!getPersistentData().getBoolean(TAG_ACTIVE) && level().getGameTime() >= getPersistentData().getLong(TAG_START)) {
         getPersistentData().putBoolean(TAG_ACTIVE, true);
         player.getPersistentData().putBoolean("TypeMoonKendoSparring", true);
         player.getPersistentData().putUUID(TAG_PLAYER_MASTER, getUUID());
         player.getPersistentData().putString("TypeMoonKendoSparringSchool", school().id());
         player.displayClientMessage(Component.translatable("message.typemoonworld.kendo.duel.start"), false);
      }
      if (getPersistentData().getBoolean(TAG_ACTIVE)) setTarget(player);
   }

   public boolean hasDuel() { return getPersistentData().hasUUID(TAG_PLAYER); }
   public void endDuel(ServerPlayer player, boolean won) {
      if (player != null) {
         player.getPersistentData().remove("TypeMoonKendoSparring");
         player.getPersistentData().remove(TAG_PLAYER_MASTER);
         player.getPersistentData().remove("TypeMoonKendoSparringSchool");
         if (!won && getPersistentData().getBoolean(TAG_ACTIVE)) {
            KendoCombatService.addProficiency(player, school(), 0.5);
         }
         player.displayClientMessage(Component.translatable(won ? "message.typemoonworld.kendo.duel.win" : "message.typemoonworld.kendo.duel.lose"), false);
      }
      clearDuel();
      setHealth(getMaxHealth());
   }

   private void clearDuel() { getPersistentData().remove(TAG_PLAYER); getPersistentData().remove(TAG_START); getPersistentData().remove(TAG_ACTIVE); }

   private static void consumeInvitation(ServerPlayer player, ItemStack invitation) {
      if (player.getAbilities().instabuild) return;
      invitation.shrink(1);
   }
   @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
