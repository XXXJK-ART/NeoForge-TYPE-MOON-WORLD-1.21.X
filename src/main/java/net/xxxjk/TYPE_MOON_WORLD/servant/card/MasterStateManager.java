package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class MasterStateManager {
   public static final int MAX_COMMAND_SPELLS = 3;
   public static final int MIN_MASTER_MP = 200;
   public static final int MAX_MASTER_MP = 1000;
   public static final double MASTER_MAX_HEALTH = 100.0;
   public static final double MANA_LINK_RANGE = 64.0;
   private static final ResourceLocation MASTER_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "master_max_health");

   private MasterStateManager() {
   }

   public static boolean activate(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.master_active) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.already_active"), true);
         return false;
      }
      if (vars.servant_card_transformed) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.servant_cannot_master"), true);
         return false;
      }
      vars.master_saved_player_mana = vars.player_mana;
      vars.master_saved_player_max_mana = vars.player_max_mana;
      vars.master_saved_player_mana_regen = vars.player_mana_egenerated_every_moment;
      vars.master_saved_player_restore_magic_moment = vars.player_restore_magic_moment;
      vars.master_saved_is_magus = vars.is_magus;
      vars.master_saved_magic_circuit_open = vars.is_magic_circuit_open;
      vars.master_saved_magic_circuit_open_timer = vars.magic_circuit_open_timer;

      int maxMp = MIN_MASTER_MP + player.getRandom().nextInt(MAX_MASTER_MP - MIN_MASTER_MP + 1);
      vars.master_active = true;
      vars.master_servant_uuid = "";
      vars.master_command_spells = MAX_COMMAND_SPELLS;
      vars.master_revive_available = true;
      vars.is_magus = true;
      vars.is_magic_circuit_open = false;
      vars.magic_circuit_open_timer = 0.0;
      vars.player_max_mana = maxMp;
      vars.player_mana = maxMp;
      vars.player_restore_magic_moment = 20.0;
      vars.player_mana_egenerated_every_moment = maxMp / 180.0;
      applyAttributes(player);
      player.setHealth((float)MASTER_MAX_HEALTH);
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master.activated", maxMp), true);
      return true;
   }

   public static boolean release(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active) {
         return false;
      }
      clearBoundServant(player, vars);
      removeAttributes(player);
      vars.master_active = false;
      vars.master_servant_uuid = "";
      vars.master_command_spells = 0;
      vars.master_revive_available = false;
      vars.player_mana = vars.master_saved_player_mana;
      vars.player_max_mana = vars.master_saved_player_max_mana;
      vars.player_mana_egenerated_every_moment = vars.master_saved_player_mana_regen;
      vars.player_restore_magic_moment = vars.master_saved_player_restore_magic_moment;
      vars.is_magus = vars.master_saved_is_magus;
      vars.is_magic_circuit_open = vars.master_saved_magic_circuit_open;
      vars.magic_circuit_open_timer = vars.master_saved_magic_circuit_open_timer;
      vars.master_saved_player_mana = 0.0;
      vars.master_saved_player_max_mana = 0.0;
      vars.master_saved_player_mana_regen = 0.0;
      vars.master_saved_player_restore_magic_moment = 0.0;
      vars.master_saved_is_magus = false;
      vars.master_saved_magic_circuit_open = false;
      vars.master_saved_magic_circuit_open_timer = 0.0;
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master.released"), true);
      return true;
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.master_active) {
         return;
      }
      applyAttributes(player);
      ServantCardTransformManager.normalizeFood(player);
      if (vars.player_restore_magic_moment != 20.0) {
         vars.player_restore_magic_moment = 20.0;
      }
      double expectedRegen = Math.max(0.0, vars.player_max_mana / 180.0);
      if (Math.abs(vars.player_mana_egenerated_every_moment - expectedRegen) > 1.0E-6) {
         vars.player_mana_egenerated_every_moment = expectedRegen;
      }
   }

   public static boolean bindByContract(ServerPlayer actor, ServerPlayer target) {
      TypeMoonWorldModVariables.PlayerVariables actorVars = actor.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables targetVars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (actorVars.master_active && targetVars.servant_card_transformed) {
         return bind(actor, target);
      }
      if (actorVars.servant_card_transformed && targetVars.master_active) {
         return bind(target, actor);
      }
      actor.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_invalid"), true);
      return false;
   }

   public static boolean bind(ServerPlayer master, ServerPlayer servant) {
      if (master == servant) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!masterVars.master_active || !servantVars.servant_card_transformed) {
         return false;
      }
      if (!isBlank(masterVars.master_servant_uuid) || !isBlank(servantVars.servant_card_master_uuid)) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_occupied"), true);
         servant.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_occupied"), true);
         return false;
      }
      masterVars.master_servant_uuid = servant.getUUID().toString();
      servantVars.servant_card_master_uuid = master.getUUID().toString();
      masterVars.syncPlayerVariables(master);
      servantVars.syncPlayerVariables(servant);
      servant.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.master_bound", master.getGameProfile().getName()), true);
      master.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.servant_bound", servant.getGameProfile().getName()), true);
      return true;
   }

   public static void clearServantSide(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      ServerPlayer master = getMaster(servant, servantVars);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (servant.getUUID().toString().equals(masterVars.master_servant_uuid)) {
            masterVars.master_servant_uuid = "";
            masterVars.syncPlayerVariables(master);
         }
      }
      servantVars.servant_card_master_uuid = "";
   }

   public static ServerPlayer getBoundServant(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (master == null || master.getServer() == null || !vars.master_active || isBlank(vars.master_servant_uuid)) {
         return null;
      }
      try {
         ServerPlayer servant = master.getServer().getPlayerList().getPlayer(UUID.fromString(vars.master_servant_uuid));
         if (servant == null) {
            return null;
         }
         TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return master.getUUID().toString().equals(servantVars.servant_card_master_uuid) ? servant : null;
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   public static ServerPlayer getMaster(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (servant == null || servant.getServer() == null || isBlank(vars.servant_card_master_uuid)) {
         return null;
      }
      try {
         ServerPlayer master = servant.getServer().getPlayerList().getPlayer(UUID.fromString(vars.servant_card_master_uuid));
         if (master == null) {
            return null;
         }
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return servant.getUUID().toString().equals(masterVars.master_servant_uuid) && masterVars.master_active ? master : null;
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   public static boolean canDrawMasterMana(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars, ServerPlayer master) {
      if (master == null) {
         return false;
      }
      if (servant.getPersistentData().getBoolean("IndependentActionActive")) {
         return true;
      }
      if (servant.level().dimension() != master.level().dimension()) {
         return false;
      }
      return servant.distanceToSqr(master) <= MANA_LINK_RANGE * MANA_LINK_RANGE;
   }

   public static boolean useCommandSpell(ServerPlayer master, int action) {
      TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active || vars.master_command_spells <= 0) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.no_command_spells"), true);
         return false;
      }
      ServerPlayer servant = getBoundServant(master, vars);
      if (servant == null) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.no_servant"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean success = switch (action) {
         case 0 -> {
            servantVars.servant_card_mana += servantVars.servant_card_max_mana;
            servantVars.syncPlayerVariables(servant);
            yield true;
         }
         case 1 -> {
            if (master.level() instanceof ServerLevel level) {
               servant.teleportTo(level, master.getX(), master.getY(), master.getZ(), Set.<RelativeMovement>of(), servant.getYRot(), servant.getXRot());
               yield true;
            }
            yield false;
         }
         case 2 -> {
            servant.hurt(servant.damageSources().magic(), Float.MAX_VALUE);
            yield true;
         }
         default -> false;
      };
      if (!success) {
         return false;
      }
      vars.master_command_spells = Math.max(0, vars.master_command_spells - 1);
      if (vars.master_command_spells <= 0) {
         clearBoundServant(master, vars);
         vars.master_servant_uuid = "";
      }
      vars.syncPlayerVariables(master);
      master.displayClientMessage(Component.translatable("message.typemoonworld.master.command_spell_used", vars.master_command_spells), true);
      return true;
   }

   public static boolean tryRevive(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.master_active || !vars.master_revive_available) {
         return false;
      }
      vars.master_revive_available = false;
      player.setHealth((float)MASTER_MAX_HEALTH);
      player.invulnerableTime = 60;
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 2, false, true, true));
      player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 2, false, true, true));
      vars.syncPlayerVariables(player);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master.revived"), true);
      return true;
   }

   private static void clearBoundServant(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables masterVars) {
      ServerPlayer servant = getBoundServant(master, masterVars);
      if (servant != null) {
         TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         servantVars.servant_card_master_uuid = "";
         servantVars.syncPlayerVariables(servant);
      }
   }

   private static void applyAttributes(ServerPlayer player) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute == null) {
         return;
      }
      attribute.removeModifier(MASTER_HEALTH_ID);
      attribute.addPermanentModifier(new AttributeModifier(MASTER_HEALTH_ID, MASTER_MAX_HEALTH - player.getAttributeBaseValue(Attributes.MAX_HEALTH), AttributeModifier.Operation.ADD_VALUE));
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static void removeAttributes(ServerPlayer player) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute != null) {
         attribute.removeModifier(MASTER_HEALTH_ID);
      }
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }
}
