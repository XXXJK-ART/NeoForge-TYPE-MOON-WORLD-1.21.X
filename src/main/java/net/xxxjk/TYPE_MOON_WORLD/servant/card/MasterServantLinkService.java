package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MasterServantLinkService {
   public static final double NORMAL_RANGE = 60.0;
   public static final double BREAK_RANGE = 150.0;
   public static final String STATE_NONE = "none";
   public static final String STATE_NORMAL = "normal";
   public static final String STATE_UNSTABLE = "unstable";
   public static final String STATE_BROKEN = "broken";
   public static final String STATE_INDEPENDENT = "independent_action";
   private static final ResourceLocation LINK_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_link_health_penalty");
   private static final ResourceLocation LINK_ATTACK_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_link_attack_penalty");
   private static final ResourceLocation LINK_SPEED_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_link_speed_penalty");
   private static final ResourceLocation LINK_ARMOR_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "servant_card_link_armor_penalty");
   private static final ResourceLocation MASTER_BACKLASH_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "master_servant_death_backlash");

   private MasterServantLinkService() {
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.master_servant_backlash_ticks > 0) {
         vars.master_servant_backlash_ticks--;
         applyMasterBacklash(player, vars.master_servant_backlash_ticks > 0);
      } else {
         applyMasterBacklash(player, false);
      }
      if (vars.master_active) {
         tickMaster(player, vars);
      } else if (vars.servant_card_transformed) {
         tickServant(player, vars);
      } else {
         clearSnapshot(vars);
         removeServantPenalties(player);
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
      ServerPlayer master = getLinkedMaster(servant, servantVars);
      LinkInfo info = info(servant, servantVars, master);
      if (!info.linked()) {
         return 1.0;
      }
      return info.broken() ? Double.POSITIVE_INFINITY : 1.0 + info.decay();
   }

   public static double linkedRegenMultiplier(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_transformed) {
         ServerPlayer master = getLinkedMaster(player, vars);
         LinkInfo info = info(player, vars, master);
         if (!info.linked() || info.broken()) {
            return 0.0;
         }
         return 0.5 * (1.0 - 0.5 * info.decay());
      }
      if (vars.master_active) {
         ServerPlayer servant = getLinkedServant(player, vars);
         if (servant == null) {
            return 1.0;
         }
         TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         LinkInfo info = info(servant, servantVars, player);
         return info.broken() ? 1.0 : 0.5 * (1.0 - 0.5 * info.decay());
      }
      return 1.0;
   }

   public static void markDrawingMasterMana(ServerPlayer master, ServerPlayer servant) {
      if (master == null || servant == null) {
         return;
      }
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

   public static void breakLink(ServerPlayer master, ServerPlayer servant, boolean independentAction) {
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (servant != null && servant.getUUID().toString().equals(masterVars.master_servant_uuid)) {
            masterVars.master_servant_uuid = "";
         }
         clearSnapshot(masterVars);
         masterVars.syncPlayerVariables(master);
      }
      if (servant != null) {
         TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         servantVars.servant_card_master_uuid = "";
         if (independentAction && servantVars.servant_card_transformed) {
            startIndependentAction(servant, servantVars);
         } else {
            clearSnapshot(servantVars);
            removeServantPenalties(servant);
         }
         servantVars.syncPlayerVariables(servant);
      }
   }

   public static void onServantDeath(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      ServerPlayer master = getLinkedMaster(servant, servantVars);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         masterVars.master_servant_backlash_ticks = Math.max(masterVars.master_servant_backlash_ticks, 1200);
         applyMasterBacklash(master, true);
         masterVars.syncPlayerVariables(master);
      }
   }

   public static void startIndependentAction(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables vars) {
      int duration = independentDurationTicks(servant);
      vars.master_servant_independent_ticks = Math.max(vars.master_servant_independent_ticks, duration);
      vars.master_servant_link_state = STATE_INDEPENDENT;
      vars.master_servant_link_decay = 1.0;
      servant.getPersistentData().putBoolean("MasterServantIndependentActionState", true);
      applyServantPenalties(servant, 1.0);
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
      if (info.linked() && info.broken()) {
         breakLink(master, servant, true);
         return;
      }
      if (vars.master_servant_independent_ticks > 0) {
         vars.master_servant_independent_ticks--;
         vars.master_servant_link_state = STATE_INDEPENDENT;
         vars.master_servant_link_decay = 1.0;
         applyServantPenalties(servant, 1.0);
         if (servant.tickCount % 20 == 0) {
            servant.hurt(servant.damageSources().magic(), 5.0F);
         }
         if (vars.master_servant_independent_ticks <= 0) {
            ServantCardTransformManager.release(servant, true);
         }
         return;
      }
      servant.getPersistentData().remove("MasterServantIndependentActionState");
      if (!info.linked()) {
         clearSnapshot(vars);
         removeServantPenalties(servant);
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      updateSnapshots(master, masterVars, servant, vars, info);
      applyServantPenalties(servant, info.decay());
   }

   private static LinkInfo info(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars, ServerPlayer master) {
      if (servant == null || master == null || servantVars == null) {
         return new LinkInfo(false, true, 1.0, STATE_NONE);
      }
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!servantVars.servant_card_transformed || !masterVars.master_active) {
         return new LinkInfo(false, true, 1.0, STATE_NONE);
      }
      if (!servant.getUUID().toString().equals(masterVars.master_servant_uuid) || !master.getUUID().toString().equals(servantVars.servant_card_master_uuid)) {
         return new LinkInfo(false, true, 1.0, STATE_NONE);
      }
      if (servant.level().dimension() != master.level().dimension()) {
         return new LinkInfo(true, true, 1.0, STATE_BROKEN);
      }
      double distance = servant.distanceTo(master);
      if (distance > BREAK_RANGE) {
         return new LinkInfo(true, true, 1.0, STATE_BROKEN);
      }
      double decay = Mth.clamp((distance - NORMAL_RANGE) / (BREAK_RANGE - NORMAL_RANGE), 0.0, 1.0);
      return new LinkInfo(true, false, decay, decay > 0.0 ? STATE_UNSTABLE : STATE_NORMAL);
   }

   private static void updateSnapshots(
      ServerPlayer master,
      TypeMoonWorldModVariables.PlayerVariables masterVars,
      ServerPlayer servant,
      TypeMoonWorldModVariables.PlayerVariables servantVars,
      LinkInfo info
   ) {
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
      if (master.tickCount % 20 == 0) {
         masterVars.syncPlayerVariables(master);
         servantVars.syncPlayerVariables(servant);
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

   private static int independentDurationTicks(ServerPlayer servant) {
      if (servant.getPersistentData().getBoolean("IndependentActionAActive")) {
         return 7 * 24 * 60 * 60 * 20;
      }
      if (servant.getPersistentData().getBoolean("IndependentActionActive")) {
         float crit = servant.getPersistentData().getFloat("IndependentActionCritDamageBonus");
         return crit >= 0.075F ? 48 * 60 * 60 * 20 : 24 * 60 * 60 * 20;
      }
      return 60 * 20;
   }

   private static void applyServantPenalties(ServerPlayer player, double decay) {
      double penalty = Mth.clamp(decay, 0.0, 1.0) * 0.2;
      addMultiplier(player.getAttribute(Attributes.MAX_HEALTH), LINK_HEALTH_ID, -penalty);
      addMultiplier(player.getAttribute(Attributes.ATTACK_DAMAGE), LINK_ATTACK_ID, -penalty);
      addMultiplier(player.getAttribute(Attributes.MOVEMENT_SPEED), LINK_SPEED_ID, -penalty);
      addMultiplier(player.getAttribute(Attributes.ARMOR), LINK_ARMOR_ID, -penalty);
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void removeServantPenalties(ServerPlayer player) {
      remove(player.getAttribute(Attributes.MAX_HEALTH), LINK_HEALTH_ID);
      remove(player.getAttribute(Attributes.ATTACK_DAMAGE), LINK_ATTACK_ID);
      remove(player.getAttribute(Attributes.MOVEMENT_SPEED), LINK_SPEED_ID);
      remove(player.getAttribute(Attributes.ARMOR), LINK_ARMOR_ID);
   }

   private static void applyMasterBacklash(ServerPlayer player, boolean active) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(MASTER_BACKLASH_HEALTH_ID);
      if (active) {
         attribute.addPermanentModifier(new AttributeModifier(MASTER_BACKLASH_HEALTH_ID, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void addMultiplier(AttributeInstance attribute, ResourceLocation id, double amount) {
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(id);
      if (Math.abs(amount) > 1.0E-6) {
         attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
      }
   }

   private static void remove(AttributeInstance attribute, ResourceLocation id) {
      if (attribute != null) {
         attribute.removeModifier(id);
      }
   }

   private record LinkInfo(boolean linked, boolean broken, double decay, String state) {
   }
}
