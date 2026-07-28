package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PlayerNoblePhantasmHelper;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.nightingale.NightingaleSupportService;

public final class ServantCardNightingaleSkills {
   private static final String CAST_END = "ServantCardNightingaleCastEnd";
   private static final String CAST_X = "ServantCardNightingaleCastX";
   private static final String CAST_Y = "ServantCardNightingaleCastY";
   private static final String CAST_Z = "ServantCardNightingaleCastZ";
   private static final String REFUND_READY = "ServantCardNightingaleRefundReady";
   private static final String REFUND_MANA = "ServantCardNightingaleRefundMana";
   private static final String REFUND_MASTER = "ServantCardNightingaleRefundMaster";
   private static final String REFUND_MASTER_MANA = "ServantCardNightingaleRefundMasterMana";
   private static final String VOICE_END = "ServantCardNightingaleVoiceEnd";
   private static final int FULL_VOICE_TICKS = 260;

   private ServantCardNightingaleSkills() {
   }

   public static boolean performSteelNursing(ServerPlayer player) {
      LivingEntity looked = findLookTarget(player, NightingaleRules.SUPPORT_RANGE);
      LivingEntity target = null;
      if (looked != null && NightingaleSupportService.isPaleRider(looked)) {
         target = looked;
      } else if (looked != null && NightingaleSupportService.isAlly(player, looked)
         && NightingaleRules.isNursingEligible(looked.getHealth(), looked.getMaxHealth())) {
         target = looked;
      } else if (NightingaleRules.isNursingEligible(player.getHealth(), player.getMaxHealth())) {
         target = player;
      }
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.nightingale.no_heal_target"), true);
         return false;
      }
      NightingaleSupportService.applyHealing(player, target, NightingaleRules.STEEL_NURSING_AMOUNT);
      return true;
   }

   public static boolean performAngelCry(ServerPlayer player) {
      long now = player.level().getGameTime();
      LivingEntity looked = findLookTarget(player, NightingaleRules.SUPPORT_RANGE);
      LivingEntity target = looked != null && NightingaleSupportService.isAlly(player, looked)
         && !NightingaleSupportService.hasAngelCry(looked, now) ? looked : null;
      if (target == null && !NightingaleSupportService.hasAngelCry(player, now)) target = player;
      if (target == null) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.nightingale.no_buff_target"), true);
         return false;
      }
      NightingaleSupportService.applyAngelCry(player, target, now);
      return true;
   }

   public static boolean performNoblePhantasmAction(ServerPlayer player,
                                                     TypeMoonWorldModVariables.PlayerVariables vars,
                                                     ServantCardSkillAction action) {
      if (!isNightingale(player) || isCasting(player)) return false;
      captureRefundSnapshot(player, vars);
      double cost = ServantCardSkillCostRules.effectiveMpCost(vars, action);
      if (!ServantCardManaService.consumeNoblePhantasm(player, vars, cost)) {
         clearRefundSnapshot(player);
         player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.not_enough_mp"), true);
         return false;
      }
      long now = player.level().getGameTime();
      CompoundTag data = player.getPersistentData();
      data.putLong(CAST_END, now + NightingaleRules.NOBLE_PHANTASM_WINDUP);
      data.putDouble(CAST_X, player.getX());
      data.putDouble(CAST_Y, player.getY());
      data.putDouble(CAST_Z, player.getZ());
      data.putLong(VOICE_END, now + FULL_VOICE_TICKS);
      PlayerNoblePhantasmHelper.startServantCardVoiceSession(player, "nightingale", ModSounds.NIGHTINGALE_VOICE_NP.get());
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.skill_activated",
         Component.translatable("skill.typemoonworld.servant_card.nightingale_pledge")), true);
      return true;
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      long now = player.level().getGameTime();
      if (now % 5L == 0L && player.level() instanceof ServerLevel level) {
         NightingaleSupportService.tickSafetyCircleVisuals(level, now);
      }
      if (!isNightingale(player)) {
         clear(player, true);
         return;
      }
      if (!isCasting(player)) {
         if (data.getLong(VOICE_END) > 0L && now >= data.getLong(VOICE_END)) stopVoice(player);
         return;
      }
      if (!player.isAlive() || hasInterruptingControl(player)) {
         interrupt(player);
         return;
      }
      player.setPos(data.getDouble(CAST_X), data.getDouble(CAST_Y), data.getDouble(CAST_Z));
      player.setDeltaMovement(Vec3.ZERO);
      player.fallDistance = 0.0F;
      if (player.level() instanceof ServerLevel level && now % 2L == 0L) {
         level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.5, player.getZ(), 5, 0.3, 0.45, 0.3, 0.02);
      }
      if (now < data.getLong(CAST_END)) return;

      clearCastTags(player);
      clearRefundSnapshot(player);
      ServantCardTransformManager.setNoblePhantasmCooldown(player, vars, NightingaleRules.NOBLE_PHANTASM_COOLDOWN);
      if (player.level() instanceof ServerLevel level) {
         NightingaleSupportService.createSafetyCircle(player, level, player.position(), now);
      }
      vars.syncPlayerVariables(player);
   }

   public static boolean isCasting(ServerPlayer player) {
      return player != null && player.getPersistentData().contains(CAST_END);
   }

   public static void interrupt(ServerPlayer player) {
      if (!isCasting(player)) return;
      restoreRefundSnapshot(player);
      clearCastTags(player);
      stopVoice(player);
      if (player.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 18, 0.4, 0.5, 0.4, 0.04);
         level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
      }
      player.displayClientMessage(Component.translatable("message.typemoonworld.nightingale.np_interrupted"), true);
   }

   public static void clear(ServerPlayer player, boolean refund) {
      if (refund && isCasting(player)) restoreRefundSnapshot(player);
      clearCastTags(player);
      clearRefundSnapshot(player);
      stopVoice(player);
   }

   private static boolean isNightingale(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "nightingale".equals(vars.servant_card_id);
   }

   private static LivingEntity findLookTarget(ServerPlayer player, double range) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle().normalize();
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.6);
      LivingEntity best = null;
      double bestScore = 0.78;
      for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
         entity -> entity != player && entity.isAlive())) {
         Vec3 to = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0).subtract(eye);
         double distance = to.length();
         if (distance <= 0.01 || distance > range) continue;
         double score = look.dot(to.normalize());
         if (score > bestScore) {
            bestScore = score;
            best = living;
         }
      }
      return best;
   }

   private static boolean hasInterruptingControl(ServerPlayer player) {
      return player.hasEffect(ModMobEffects.PETRIFIED)
         || player.hasEffect(ModMobEffects.BINDING)
         || player.hasEffect(ModMobEffects.STAGGER);
   }

   private static void captureRefundSnapshot(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      CompoundTag data = player.getPersistentData();
      data.putBoolean(REFUND_READY, true);
      data.putDouble(REFUND_MANA, vars.servant_card_mana);
      ServerPlayer master = ServantCardManaService.getMaster(player, vars);
      if (master != null) {
         data.putUUID(REFUND_MASTER, master.getUUID());
         data.putDouble(REFUND_MASTER_MANA,
            master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).player_mana);
      }
   }

   private static void restoreRefundSnapshot(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      if (!data.getBoolean(REFUND_READY)) return;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      vars.servant_card_mana = data.getDouble(REFUND_MANA);
      vars.syncMana(player);
      if (data.hasUUID(REFUND_MASTER) && player.getServer() != null) {
         ServerPlayer master = player.getServer().getPlayerList().getPlayer(data.getUUID(REFUND_MASTER));
         if (master != null) {
            TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            masterVars.player_mana = data.getDouble(REFUND_MASTER_MANA);
            masterVars.syncMana(master);
         }
      }
      clearRefundSnapshot(player);
   }

   private static void clearCastTags(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(CAST_END);
      data.remove(CAST_X);
      data.remove(CAST_Y);
      data.remove(CAST_Z);
   }

   private static void clearRefundSnapshot(ServerPlayer player) {
      CompoundTag data = player.getPersistentData();
      data.remove(REFUND_READY);
      data.remove(REFUND_MANA);
      data.remove(REFUND_MASTER);
      data.remove(REFUND_MASTER_MANA);
   }

   private static void stopVoice(ServerPlayer player) {
      player.getPersistentData().remove(VOICE_END);
      PlayerNoblePhantasmHelper.finishServantCardVoiceSession(
         player, "nightingale", ModSounds.NIGHTINGALE_VOICE_NP.get(), null);
   }
}
