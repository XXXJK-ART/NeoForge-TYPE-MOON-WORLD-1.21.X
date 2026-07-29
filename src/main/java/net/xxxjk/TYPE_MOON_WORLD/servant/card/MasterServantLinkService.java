package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Collection;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class MasterServantLinkService {
   public enum UnlinkReason {
      CONTRACT_TERMINATED,
      MASTER_LOST,
      SERVANT_LOST
   }

   public static final double NORMAL_RANGE = 60.0;
   public static final double BREAK_RANGE = 150.0;
   public static final String STATE_NONE = "none";
   public static final String STATE_NORMAL = "normal";
   public static final String STATE_UNSTABLE = "unstable";
   public static final String STATE_BROKEN = "broken";
   public static final String STATE_INDEPENDENT = "independent_action";
   public static final String STATE_FORCED_DEATH = "forced_death";
   public static final String STATE_DECAYING = "decaying";
   public static final String SURVIVAL_NONE = "none";
   public static final String SURVIVAL_FORCED_DEATH = "forced_death";
   public static final String SURVIVAL_INDEPENDENT = "independent_action";
   public static final String SURVIVAL_DECAYING = "decaying";
   public static final int FORCED_DEATH_TICKS = 200;
   public static final float DECAY_DAMAGE_PER_SECOND = 5.0F;

   private static final ResourceLocation LINK_HEALTH_ID = id("servant_card_link_health_penalty");
   private static final ResourceLocation LINK_ATTACK_ID = id("servant_card_link_attack_penalty");
   private static final ResourceLocation LINK_SPEED_ID = id("servant_card_link_speed_penalty");
   private static final ResourceLocation LINK_ARMOR_ID = id("servant_card_link_armor_penalty");
   private static final ResourceLocation MASTER_BACKLASH_HEALTH_ID = id("master_servant_death_backlash");

   private MasterServantLinkService() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      tickBacklash(player, vars);
      removeLegacyServantPenalties(player);
      if (vars.master_active) {
         tickMaster(player, vars);
      } else if (vars.servant_card_transformed) {
         tickServant(player, vars);
      } else {
         clearSnapshot(vars);
         clearSurvival(vars);
      }
   }

   public static ServerPlayer getLinkedServant(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables vars) {
      return MasterStateManager.getBoundServant(master, vars);
   }

   public static ServerPlayer getLinkedMaster(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables vars) {
      return MasterStateManager.getMaster(servant, vars);
   }

   public static boolean canUseMasterMana(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars, ServerPlayer master) {
      LinkInfo info = info(servant, servantVars, master);
      return info.linked() && !info.broken();
   }

   public static double distanceDecay(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars, ServerPlayer master) {
      return info(servant, servantVars, master).decay();
   }

   public static double noblePhantasmCostMultiplier(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      return 1.0;
   }

   public static double linkedRegenMultiplier(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_transformed) {
         LinkInfo info = info(player, vars, getLinkedMaster(player, vars));
         return !info.linked() || info.broken() ? 0.0 : 0.5 * (1.0 - info.decay());
      }
      return 1.0;
   }

   public static void markDrawingMasterMana(ServerPlayer master, ServerPlayer servant) {
      if (master == null || servant == null) return;
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      masterVars.master_servant_link_drawing_mana = true;
      servantVars.master_servant_link_drawing_mana = true;
      if (masterVars.player_max_mana > 0.0 && masterVars.player_mana / masterVars.player_max_mana < 0.2) {
         long now = master.level().getGameTime();
         if (master.getPersistentData().getLong("MasterLowManaWarnTick") + 80L <= now) {
            master.getPersistentData().putLong("MasterLowManaWarnTick", now);
            servant.getPersistentData().putLong("MasterLowManaWarnTick", now);
            Component message = Component.translatable("message.typemoonworld.master.link_low_mana");
            master.displayClientMessage(message, true);
            servant.displayClientMessage(message, true);
         }
      }
   }

   public static void terminateContract(ServerPlayer master, ServerPlayer servant) {
      unlink(master, servant, UnlinkReason.CONTRACT_TERMINATED);
   }

   public static void consumePendingTransitions(ServerPlayer player) {
      if (player != null && player.getServer() != null) {
         PlayerLinkTransitionData.get(player.getServer()).consume(player);
      }
   }

   public static void repairPlayerLink(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (player == null || vars == null || player.getServer() == null) return;
      vars.master_servant_uuid = sanitizeUuid(vars.master_servant_uuid);
      vars.servant_card_master_uuid = sanitizeUuid(vars.servant_card_master_uuid);
      if (!vars.master_active) vars.master_servant_uuid = "";
      if (!vars.servant_card_transformed) {
         vars.servant_card_master_uuid = "";
         clearSurvival(vars);
         clearMasterPosition(vars);
      } else if (SURVIVAL_DECAYING.equals(vars.master_servant_survival_state)) {
         // Old saves used an endless damage state. Migrate it to the current ten-second death countdown.
         vars.master_servant_survival_state = SURVIVAL_FORCED_DEATH;
         vars.master_servant_survival_ticks = FORCED_DEATH_TICKS;
      } else if (!isValidSurvival(vars.master_servant_survival_state)
         || (!SURVIVAL_DECAYING.equals(vars.master_servant_survival_state) && vars.master_servant_survival_ticks <= 0)) {
         clearSurvival(vars);
      }

      UUID servantId = parse(vars.master_servant_uuid);
      if (servantId != null) {
         ServerPlayer servant = player.getServer().getPlayerList().getPlayer(servantId);
         if (servant != null) {
            TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (!servantVars.servant_card_transformed || !player.getUUID().toString().equals(servantVars.servant_card_master_uuid)) {
               vars.master_servant_uuid = "";
            }
         }
      }

      UUID masterId = parse(vars.servant_card_master_uuid);
      if (masterId != null) {
         ServerPlayer master = player.getServer().getPlayerList().getPlayer(masterId);
         if (master != null) {
            TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            if (!masterVars.master_active || !player.getUUID().toString().equals(masterVars.master_servant_uuid)) {
               vars.servant_card_master_uuid = "";
               clearMasterPosition(vars);
            }
         }
      }
      if (vars.servant_card_transformed && isBlank(vars.servant_card_master_uuid)
         && SURVIVAL_NONE.equals(vars.master_servant_survival_state)) {
         clearMasterPosition(vars);
      }
   }

   public static void onMasterLost(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables masterVars) {
      if (master == null || masterVars == null || isBlank(masterVars.master_servant_uuid)) {
         if (masterVars != null) clearSnapshot(masterVars);
         return;
      }
      UUID servantId = parse(masterVars.master_servant_uuid);
      ServerPlayer servant = servantId == null || master.getServer() == null
         ? null : master.getServer().getPlayerList().getPlayer(servantId);
      if (servant != null) {
         unlink(master, servant, UnlinkReason.MASTER_LOST);
      } else {
         if (servantId != null && master.getServer() != null) {
            PlayerLinkTransitionData.get(master.getServer()).queue(
               servantId, master.getUUID(), UnlinkReason.MASTER_LOST);
         }
         masterVars.master_servant_uuid = "";
         clearSnapshot(masterVars);
         masterVars.syncPlayerVariables(master);
      }
   }

   public static void onServantLost(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      if (servant == null || servantVars == null || isBlank(servantVars.servant_card_master_uuid)) {
         if (servantVars != null) {
            servantVars.servant_card_master_uuid = "";
            clearSnapshot(servantVars);
            clearSurvival(servantVars);
         }
         return;
      }
      UUID masterId = parse(servantVars.servant_card_master_uuid);
      ServerPlayer master = masterId == null || servant.getServer() == null
         ? null : servant.getServer().getPlayerList().getPlayer(masterId);
      if (master != null) {
         unlink(master, servant, UnlinkReason.SERVANT_LOST);
      } else {
         if (masterId != null && servant.getServer() != null) {
            PlayerLinkTransitionData.get(servant.getServer()).queue(
               masterId, servant.getUUID(), UnlinkReason.SERVANT_LOST);
         }
         servantVars.servant_card_master_uuid = "";
         clearSnapshot(servantVars);
         clearMasterPosition(servantVars);
         clearSurvival(servantVars);
         servantVars.syncPlayerVariables(servant);
      }
   }

   public static void onServantDeath(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      onServantLost(servant, servantVars);
   }

   public static void onEntityServantDeath(ServantEntity servant) {
      if (servant == null || servant.getMasterUuid() == null || servant.getServer() == null) return;
      UUID masterId = servant.getMasterUuid();
      ServerPlayer master = servant.getServer().getPlayerList().getPlayer(masterId);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (servant.getUUID().toString().equals(vars.master_servant_uuid)) {
            vars.master_servant_uuid = "";
            clearSnapshot(vars);
            if (vars.master_active) applyServantLossToMaster(master, vars);
            vars.syncPlayerVariables(master);
         }
      } else {
         PlayerLinkTransitionData.get(servant.getServer()).queue(
            masterId, servant.getUUID(), UnlinkReason.SERVANT_LOST);
      }
      servant.unbindMaster();
   }

   public static void unlink(ServerPlayer master, ServerPlayer servant, UnlinkReason reason) {
      TypeMoonWorldModVariables.PlayerVariables masterVars = master == null ? null
         : master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant == null ? null
         : servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean paired = master != null && servant != null && masterVars != null && servantVars != null
         && servant.getUUID().toString().equals(masterVars.master_servant_uuid)
         && master.getUUID().toString().equals(servantVars.servant_card_master_uuid);

      if (masterVars != null && (servant == null || servant.getUUID().toString().equals(masterVars.master_servant_uuid))) {
         masterVars.master_servant_uuid = "";
         clearSnapshot(masterVars);
      }
      if (servantVars != null && (master == null || master.getUUID().toString().equals(servantVars.servant_card_master_uuid))) {
         servantVars.servant_card_master_uuid = "";
         clearSnapshot(servantVars);
         clearMasterPosition(servantVars);
      }

      if (paired && reason == UnlinkReason.MASTER_LOST && servantVars.servant_card_transformed) {
         startMasterLossSurvival(servant, servantVars);
      } else if (servantVars != null) {
         clearSurvival(servantVars);
      }
      if (paired && reason == UnlinkReason.SERVANT_LOST && masterVars.master_active) {
         applyServantLossToMaster(master, masterVars);
      }

      if (masterVars != null) masterVars.syncPlayerVariables(master);
      if (servantVars != null) servantVars.syncPlayerVariables(servant);
   }

   static void applyPendingTransition(ServerPlayer player, UUID partnerId, UnlinkReason reason) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (reason == UnlinkReason.MASTER_LOST) {
         if (!partnerId.toString().equals(vars.servant_card_master_uuid)) return;
         vars.servant_card_master_uuid = "";
         clearSnapshot(vars);
         clearMasterPosition(vars);
         if (vars.servant_card_transformed) startMasterLossSurvival(player, vars);
      } else if (reason == UnlinkReason.SERVANT_LOST) {
         if (!partnerId.toString().equals(vars.master_servant_uuid)) return;
         vars.master_servant_uuid = "";
         clearSnapshot(vars);
         if (vars.master_active) applyServantLossToMaster(player, vars);
      }
      vars.syncPlayerVariables(player);
   }

   public static void startMasterLossSurvival(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables vars) {
      int duration = independentDurationTicks(vars.servant_card_id);
      if (duration > 0) {
         vars.master_servant_survival_state = SURVIVAL_INDEPENDENT;
         vars.master_servant_survival_ticks = duration;
         vars.master_servant_link_state = STATE_INDEPENDENT;
      } else {
         vars.master_servant_survival_state = SURVIVAL_FORCED_DEATH;
         vars.master_servant_survival_ticks = FORCED_DEATH_TICKS;
         vars.master_servant_link_state = STATE_FORCED_DEATH;
      }
      vars.master_servant_independent_ticks = 0;
      vars.master_servant_link_decay = 1.0;
      servant.getPersistentData().putBoolean("MasterServantIndependentActionState",
         SURVIVAL_INDEPENDENT.equals(vars.master_servant_survival_state));
   }

   public static void clearSurvival(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.master_servant_survival_state = SURVIVAL_NONE;
      vars.master_servant_survival_ticks = 0;
      vars.master_servant_independent_ticks = 0;
   }

   public static boolean isSurvivalActive(TypeMoonWorldModVariables.PlayerVariables vars) {
      return vars != null && !SURVIVAL_NONE.equals(sanitizeSurvival(vars.master_servant_survival_state));
   }

   private static void tickBacklash(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.master_servant_backlash_ticks > 0) {
         vars.master_servant_backlash_ticks--;
         applyMasterBacklash(player, vars.master_servant_backlash_ticks > 0);
      } else {
         applyMasterBacklash(player, false);
      }
   }

   private static void tickMaster(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.master_servant_link_drawing_mana = false;
      ServerPlayer servant = getLinkedServant(master, vars);
      if (servant == null) {
         clearSnapshot(vars);
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      LinkInfo info = info(servant, servantVars, master);
      updateSnapshots(master, vars, servant, servantVars, info);
   }

   private static void tickServant(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.master_servant_link_drawing_mana = false;
      ServerPlayer master = getLinkedMaster(servant, vars);
      LinkInfo info = info(servant, vars, master);
      if (info.linked()) {
         clearSurvival(vars);
         servant.getPersistentData().remove("MasterServantIndependentActionState");
         vars.master_servant_link_state = info.state();
         vars.master_servant_link_decay = info.decay();
         captureMasterPosition(master, vars);
         if (servant.tickCount % 20 == 0) vars.syncMana(servant);
         return;
      }

      if (!isBlank(vars.servant_card_master_uuid)) {
         vars.master_servant_master_position_online = false;
         if (servant.tickCount % 20 == 0) vars.syncMana(servant);
      }

      String survival = sanitizeSurvival(vars.master_servant_survival_state);
      if (SURVIVAL_FORCED_DEATH.equals(survival)) {
         vars.master_servant_link_state = STATE_FORCED_DEATH;
         vars.master_servant_link_decay = 1.0;
         if (--vars.master_servant_survival_ticks <= 0) forceMasterlessDeath(servant);
         return;
      }
      if (SURVIVAL_INDEPENDENT.equals(survival)) {
         vars.master_servant_link_state = STATE_INDEPENDENT;
         vars.master_servant_link_decay = 1.0;
         if (--vars.master_servant_survival_ticks <= 0) {
            vars.master_servant_survival_state = SURVIVAL_FORCED_DEATH;
            vars.master_servant_survival_ticks = FORCED_DEATH_TICKS;
            vars.master_servant_link_state = STATE_FORCED_DEATH;
            servant.getPersistentData().remove("MasterServantIndependentActionState");
         }
         return;
      }
      if (SURVIVAL_DECAYING.equals(survival)) {
         vars.master_servant_survival_state = SURVIVAL_FORCED_DEATH;
         vars.master_servant_survival_ticks = FORCED_DEATH_TICKS;
         vars.master_servant_link_state = STATE_FORCED_DEATH;
         vars.master_servant_link_decay = 1.0;
         return;
      }
      clearSnapshot(vars);
   }

   private static void forceMasterlessDeath(ServerPlayer servant) {
      var data = servant.getPersistentData();
      data.putBoolean("MasterLossForcedDeath", true);
      data.putBoolean("CausalSevered", true);
      data.putInt("GodHandLives", 0);
      data.remove("GodHandActive");
      data.remove("BattleContinuationActive");
      data.remove("BattleContinuationRecoveryActive");
      data.remove("EmiyaStyleBattleContinuationActive");
      servant.kill();
   }

   private static LinkInfo info(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars, ServerPlayer master) {
      if (servant == null || master == null || servantVars == null) return LinkInfo.NONE;
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!servantVars.servant_card_transformed || !masterVars.master_active
         || !isContractedPair(master, masterVars, servant, servantVars)) return LinkInfo.NONE;
      if (servant.level().dimension() != master.level().dimension()) {
         return new LinkInfo(true, true, 1.0, STATE_BROKEN);
      }
      double distanceSqr = servant.distanceToSqr(master);
      if (distanceSqr >= BREAK_RANGE * BREAK_RANGE) {
         return new LinkInfo(true, true, 1.0, STATE_BROKEN);
      }
      if (distanceSqr <= NORMAL_RANGE * NORMAL_RANGE) {
         return new LinkInfo(true, false, 0.0, STATE_NORMAL);
      }
      double distance = Math.sqrt(distanceSqr);
      double decay = 1.0 - manaSupplyFraction(distance, true);
      return new LinkInfo(true, false, decay, STATE_UNSTABLE);
   }

   private static boolean isContractedPair(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables masterVars,
                                           ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      return servant.getUUID().toString().equals(masterVars.master_servant_uuid)
         && master.getUUID().toString().equals(servantVars.servant_card_master_uuid);
   }

   private static void updateSnapshots(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables masterVars,
                                       ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars,
                                       LinkInfo info) {
      masterVars.master_servant_link_partner_uuid = servant.getUUID().toString();
      masterVars.master_servant_link_partner_hp = servant.getHealth();
      masterVars.master_servant_link_partner_max_hp = servant.getMaxHealth();
      masterVars.master_servant_link_partner_mana = servantVars.servant_card_mana;
      masterVars.master_servant_link_partner_max_mana = servantVars.servant_card_max_mana;
      masterVars.master_servant_link_state = info.state();
      masterVars.master_servant_link_decay = info.decay();
      servantVars.master_servant_link_partner_uuid = master.getUUID().toString();
      servantVars.master_servant_link_partner_hp = master.getHealth();
      servantVars.master_servant_link_partner_max_hp = master.getMaxHealth();
      servantVars.master_servant_link_partner_mana = masterVars.player_mana;
      servantVars.master_servant_link_partner_max_mana = masterVars.player_max_mana;
      servantVars.master_servant_link_state = info.state();
      servantVars.master_servant_link_decay = info.decay();
      captureMasterPosition(master, servantVars);
      if (master.tickCount % 20 == 0) {
         masterVars.syncMana(master);
         servantVars.syncMana(servant);
      }
   }

   public static void clearSnapshot(TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.master_servant_link_partner_uuid = "";
      vars.master_servant_link_partner_hp = 0.0;
      vars.master_servant_link_partner_max_hp = 0.0;
      vars.master_servant_link_partner_mana = 0.0;
      vars.master_servant_link_partner_max_mana = 0.0;
      vars.master_servant_link_state = STATE_NONE;
      vars.master_servant_link_decay = 0.0;
      vars.master_servant_link_drawing_mana = false;
   }

   static void captureMasterPosition(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      if (master == null || servantVars == null) return;
      servantVars.master_servant_master_position_valid = true;
      servantVars.master_servant_master_position_online = true;
      servantVars.master_servant_master_dimension = master.level().dimension().location().toString();
      servantVars.master_servant_master_x = master.getX();
      servantVars.master_servant_master_y = master.getY();
      servantVars.master_servant_master_z = master.getZ();
   }

   public static void clearMasterPosition(TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars == null) return;
      vars.master_servant_master_position_valid = false;
      vars.master_servant_master_position_online = false;
      vars.master_servant_master_dimension = "";
      vars.master_servant_master_x = 0.0;
      vars.master_servant_master_y = 0.0;
      vars.master_servant_master_z = 0.0;
   }

   static int independentDurationTicks(String servantId) {
      ServantDefinition definition = ServantDataRegistry.get(servantId);
      return definition == null ? 0 : independentDurationTicksForSkills(definition.skillIds());
   }

   static int independentDurationTicksForSkills(Collection<String> skillIds) {
      if (skillIds == null) return 0;
      if (skillIds.contains("independent_action_a")) return 7 * 24 * 60 * 60 * 20;
      if (skillIds.contains("independent_action_b")) return 48 * 60 * 60 * 20;
      if (skillIds.contains("independent_action_c")) return 24 * 60 * 60 * 20;
      return 0;
   }

   static double manaSupplyFraction(double distance, boolean sameDimension) {
      if (!sameDimension || !Double.isFinite(distance) || distance >= BREAK_RANGE) return 0.0;
      if (distance <= NORMAL_RANGE) return 1.0;
      return Mth.clamp((BREAK_RANGE - distance) / (BREAK_RANGE - NORMAL_RANGE), 0.0, 1.0);
   }

   private static void applyServantLossToMaster(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables vars) {
      vars.master_servant_backlash_ticks = Math.max(vars.master_servant_backlash_ticks, 1200);
      applyMasterBacklash(master, true);
      master.displayClientMessage(Component.translatable("message.typemoonworld.master.servant_lost")
         .withStyle(ChatFormatting.DARK_RED), false);
      master.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2F, 0.65F);
   }

   private static void removeLegacyServantPenalties(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), LINK_HEALTH_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), LINK_ATTACK_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), LINK_SPEED_ID);
      remove(player.getAttribute(Attributes.ARMOR), LINK_ARMOR_ID);
      if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
   }

   private static void applyMasterBacklash(ServerPlayer player, boolean active) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute == null) return;
      AttributeModifier existing = attribute.getModifier(MASTER_BACKLASH_HEALTH_ID);
      if (active) {
         if (existing == null || existing.amount() != -0.2
            || existing.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            attribute.removeModifier(MASTER_BACKLASH_HEALTH_ID);
            attribute.addPermanentModifier(new AttributeModifier(
               MASTER_BACKLASH_HEALTH_ID, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
         }
      } else if (existing != null) {
         attribute.removeModifier(MASTER_BACKLASH_HEALTH_ID);
      }
      if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
   }

   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null && attribute.getModifier(id) != null) attribute.removeModifier(id);
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, path);
   }

   private static UUID parse(String value) {
      try {
         return isBlank(value) ? null : UUID.fromString(value);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static String sanitizeUuid(String value) {
      UUID parsed = parse(value);
      return parsed == null ? "" : parsed.toString();
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   private static String sanitizeSurvival(String value) {
      return isValidSurvival(value) ? value : SURVIVAL_NONE;
   }

   private static boolean isValidSurvival(String value) {
      return SURVIVAL_FORCED_DEATH.equals(value) || SURVIVAL_INDEPENDENT.equals(value)
         || SURVIVAL_DECAYING.equals(value) || SURVIVAL_NONE.equals(value);
   }

   private record LinkInfo(boolean linked, boolean broken, double decay, String state) {
      private static final LinkInfo NONE = new LinkInfo(false, true, 1.0, STATE_NONE);
   }
}
