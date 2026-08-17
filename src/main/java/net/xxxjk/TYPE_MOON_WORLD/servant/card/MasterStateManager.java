package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.xxxjk.typemoonworld.api.event.ServantContractEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class MasterStateManager {
   public static final int MAX_COMMAND_SPELLS = 3;
   public static final int MIN_MASTER_MP = 200;
   public static final int MAX_MASTER_MP = 1000;
   public static final double MASTER_MAX_HEALTH = 100.0;
   private static final String[] COMMAND_SPELL_STYLES = new String[]{
      "default", "illya", "kiritsugu", "shirou", "bazett", "sakura", "rin", "luvia", "kirei", "tokiomi", "waver"
   };
   private static final ResourceLocation MASTER_HEALTH_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "master_max_health");

   private MasterStateManager() {
   }

   public static boolean activate(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.master_active) {
         vars.master_command_spells += MAX_COMMAND_SPELLS;
         vars.master_command_spell_pose_active = false;
         vars.syncPlayerVariables(player);
         MasterVisualStateSync.broadcast(player, vars);
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.command_spells_added", vars.master_command_spells), true);
         return true;
      }
      if (vars.servant_card_transformed) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.servant_cannot_master"), true);
         return false;
      }
      vars.master_active = true;
      vars.master_servant_uuid = "";
      vars.master_servant_contract_id = "";
      MasterServantLinkService.clearContractTags(player);
      vars.master_command_spells = MAX_COMMAND_SPELLS;
      vars.master_command_spell_style = randomCommandSpellStyle(player);
      vars.master_command_spell_pose_active = false;
      vars.master_revive_available = true;
      applyAttributes(player);
      player.setHealth((float)MASTER_MAX_HEALTH);
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master.activated", (int)vars.player_max_mana), true);
      return true;
   }

   public static boolean activateProfile(ServerPlayer player, String commandSpellStyle) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_transformed) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.servant_cannot_master"), true);
         return false;
      }
      if (vars.master_active && !isBlank(vars.master_servant_uuid)) {
         MasterServantLinkService.onMasterLost(player, vars);
      }
      vars.master_active = true;
      vars.master_servant_uuid = "";
      vars.master_servant_contract_id = "";
      MasterServantLinkService.clearContractTags(player);
      vars.master_command_spells = Math.max(vars.master_command_spells, MAX_COMMAND_SPELLS);
      vars.master_command_spell_style = sanitizeCommandSpellStyle(commandSpellStyle);
      vars.master_command_spell_pose_active = false;
      vars.master_revive_available = true;
      applyAttributes(player);
      player.setHealth((float)MASTER_MAX_HEALTH);
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      return true;
   }

   public static boolean release(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.master_card_active) {
         return releaseMasterCardProfile(player, vars);
      }
      if (!vars.master_active) {
         return false;
      }
      ServerPlayer boundServant = MasterServantLinkService.getLinkedServant(player, vars);
      if (boundServant != null) {
         MasterServantLinkService.onMasterLost(player, vars);
      } else {
         MasterServantLinkService.onMasterLost(player, vars);
      }
      removeAttributes(player);
      vars.master_active = false;
      vars.master_servant_uuid = "";
      vars.master_servant_contract_id = "";
      vars.master_command_spells = 0;
      vars.master_command_spell_style = "default";
      vars.master_command_spell_pose_active = false;
      vars.master_revive_available = false;
      vars.master_artificial_leyline_bonus_active = false;
      vars.master_artificial_leyline_dimension = "";
      vars.master_saved_player_mana = 0.0;
      vars.master_saved_player_max_mana = 0.0;
      vars.master_saved_player_mana_regen = 0.0;
      vars.master_saved_player_restore_magic_moment = 0.0;
      vars.master_saved_is_magus = false;
      vars.master_saved_magic_circuit_open = false;
      vars.master_saved_magic_circuit_open_timer = 0.0;
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master.released"), true);
      return true;
   }

   private static boolean releaseMasterCardProfile(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServerPlayer boundServant = MasterServantLinkService.getLinkedServant(player, vars);
      if (boundServant != null) {
         MasterServantLinkService.onMasterLost(player, vars);
      } else {
         MasterServantLinkService.onMasterLost(player, vars);
      }
      removeAttributes(player);
      MasterCardProfile.restoreOriginalState(player, vars);
      if (vars.master_active) {
         applyAttributes(player);
      }
      if (player.getHealth() > player.getMaxHealth()) {
         player.setHealth(player.getMaxHealth());
      }
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      player.displayClientMessage(Component.translatable("message.typemoonworld.master_card.released"), true);
      return true;
   }

   public static void tick(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.master_active) {
         return;
      }
      if (vars.master_card_active) MasterCardProfile.ensureTags(player, vars.master_card_id);
      applyAttributes(player);
   }

   public static boolean bindByContract(ServerPlayer actor, ServerPlayer target) {
      TypeMoonWorldModVariables.PlayerVariables actorVars = actor.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables targetVars = target.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (actorVars.master_card_active && targetVars.servant_card_transformed) {
         return bind(actor, target);
      }
      if (actorVars.servant_card_transformed && targetVars.master_card_active) {
         return bind(target, actor);
      }
      actor.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_invalid"), true);
      return false;
   }

   public static boolean bind(ServerPlayer master, ServerPlayer servant) {
      return bindInternal(master, servant, false);
   }

   /**
    * Binds a player servant summoned by a catalyst. A normal master form may
    * perform this ritual even when no master card is active; direct card
    * contracts continue to use the stricter card-to-card validation above.
    */
   public static boolean bindForSummoning(ServerPlayer master, ServerPlayer servant) {
      return bindInternal(master, servant, true);
   }

   private static boolean bindInternal(ServerPlayer master, ServerPlayer servant, boolean summoned) {
      if (master == servant) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      MasterServantLinkService.repairPlayerLink(master, masterVars);
      MasterServantLinkService.repairPlayerLink(servant, servantVars);
      boolean validPair = summoned
         ? servantVars.servant_card_transformed && !servantVars.master_card_active
         : isPlayerCardContractPair(masterVars.master_card_active, masterVars.servant_card_transformed,
            servantVars.master_card_active, servantVars.servant_card_transformed);
      if (!masterVars.master_active || !validPair) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_invalid"), true);
         return false;
      }
      if (!isBlank(masterVars.master_servant_uuid) || !isBlank(servantVars.servant_card_master_uuid)) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_occupied"), true);
         servant.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_occupied"), true);
         return false;
      }
      if (NeoForge.EVENT_BUS.post(new ServantContractEvent.Pre(master, servant)).isCanceled()) return false;
      masterVars.master_servant_uuid = servant.getUUID().toString();
      servantVars.servant_card_master_uuid = master.getUUID().toString();
      MasterServantLinkService.establishContract(master, masterVars, servant, servantVars);
      servantVars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_CONTRACTED;
      MasterServantLinkService.clearSurvival(servantVars);
      MasterServantLinkService.captureMasterPosition(master, servantVars);
      servant.getPersistentData().remove("MasterServantIndependentActionState");
      masterVars.syncPlayerVariables(master);
      servantVars.syncPlayerVariables(servant);
      servant.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.master_bound", master.getGameProfile().getName()), true);
      master.displayClientMessage(Component.translatable("message.typemoonworld.servant_card.servant_bound", servant.getGameProfile().getName()), true);
      NeoForge.EVENT_BUS.post(new ServantContractEvent.Post(master, servant));
      return true;
   }

   public static void clearServantSide(ServerPlayer servant, TypeMoonWorldModVariables.PlayerVariables servantVars) {
      ServerPlayer master = getMaster(servant, servantVars);
      if (master != null) {
         TypeMoonWorldModVariables.PlayerVariables masterVars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (servant.getUUID().toString().equals(masterVars.master_servant_uuid)) {
            masterVars.master_servant_uuid = "";
            masterVars.master_servant_contract_id = "";
            MasterServantLinkService.clearContractTags(master);
            masterVars.syncPlayerVariables(master);
         }
      }
      servantVars.servant_card_master_uuid = "";
      servantVars.servant_card_contract_id = "";
      servantVars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_MASTERLESS;
      MasterServantLinkService.clearContractTags(servant);
      MasterServantLinkService.clearMasterPosition(servantVars);
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

   public static LivingEntity getBoundServantEntity(ServerPlayer master, TypeMoonWorldModVariables.PlayerVariables vars) {
      ServerPlayer playerServant = getBoundServant(master, vars);
      if (playerServant != null) return playerServant;
      if (master == null || master.getServer() == null || !vars.master_active || isBlank(vars.master_servant_uuid)) return null;
      try {
         UUID uuid = UUID.fromString(vars.master_servant_uuid);
         for (ServerLevel level : master.getServer().getAllLevels()) {
            if (level.getEntity(uuid) instanceof ServantEntity servant && servant.isBoundTo(master)) return servant;
         }
      } catch (IllegalArgumentException ignored) {
      }
      return null;
   }

   public static boolean bindEntityServant(ServerPlayer master, ServantEntity servant) {
      if (master == null || servant == null || !servant.isAlive()) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active || !isBlank(vars.master_servant_uuid) || servant.getMasterUuid() != null) {
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.contract_occupied"), true);
         return false;
      }
      if (NeoForge.EVENT_BUS.post(new ServantContractEvent.Pre(master, servant)).isCanceled()) return false;
      vars.master_servant_uuid = servant.getUUID().toString();
      servant.bindMaster(master);
      MasterServantLinkService.establishEntityContract(master, vars, servant);
      vars.syncPlayerVariables(master);
      master.displayClientMessage(Component.translatable("message.typemoonworld.entity_servant.bound", servant.getDisplayName()), true);
      NeoForge.EVENT_BUS.post(new ServantContractEvent.Post(master, servant));
      return true;
   }

   public static boolean unbindEntityServant(ServerPlayer master, ServantEntity servant) {
      if (master == null || servant == null || !servant.isBoundTo(master)) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (servant.getUUID().toString().equals(vars.master_servant_uuid)) {
         vars.master_servant_uuid = "";
         vars.master_servant_contract_id = "";
         MasterServantLinkService.clearContractTags(master);
         vars.syncPlayerVariables(master);
      }
      servant.unbindMaster();
      master.displayClientMessage(Component.translatable("message.typemoonworld.entity_servant.unbound"), true);
      return true;
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
      return MasterServantLinkService.canUseMasterMana(servant, servantVars, master);
   }

   public static boolean useCommandSpell(ServerPlayer master, int action) {
      TypeMoonWorldModVariables.PlayerVariables vars = master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active || vars.master_command_spells <= 0) {
         vars.master_command_spell_pose_active = false;
         MasterVisualStateSync.broadcast(master, vars);
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.no_command_spells"), true);
         return false;
      }
      if (action == 3) {
         return extractSingleCommandSpell(master, vars);
      }
      ServerPlayer servant = MasterServantLinkService.getLinkedServant(master, vars);
      if (servant == null) {
         vars.master_command_spell_pose_active = false;
         MasterVisualStateSync.broadcast(master, vars);
         master.displayClientMessage(Component.translatable("message.typemoonworld.master.no_servant"), true);
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables servantVars = servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      boolean success = switch (action) {
         case 0 -> {
            servantVars.servant_card_mana = Math.min(
               servantVars.servant_card_max_mana,
               servantVars.servant_card_mana + servantVars.servant_card_max_mana
            );
            ServantCardUnlimitedMode.clearCooldowns(servant);
            servantVars.syncPlayerVariables(servant);
            yield true;
         }
         case 1 -> {
            if (master.level() instanceof ServerLevel level) {
               Vec3 destination = findSafeRecallDestination(level, master, servant);
               servant.teleportTo(level, destination.x, destination.y, destination.z, Set.<RelativeMovement>of(), servant.getYRot(), servant.getXRot());
               yield true;
            }
            yield false;
         }
         case 2 -> {
            if (net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper.tryProtectWithAvalon(servant)) {
               yield true;
            }
            servant.getPersistentData().putBoolean("CausalSevered", true);
            servant.getPersistentData().putInt("GodHandLives", 0);
            servant.getPersistentData().remove("GodHandActive");
            servant.setHealth(0.0F);
            servant.die(servant.damageSources().genericKill());
            yield true;
         }
         default -> false;
      };
      if (!success) {
         return false;
      }
      vars.master_command_spells = Math.max(0, vars.master_command_spells - 1);
      vars.master_command_spell_pose_active = false;
      vars.syncPlayerVariables(master);
      MasterVisualStateSync.broadcast(master, vars);
      master.displayClientMessage(Component.translatable("message.typemoonworld.master.command_spell_used", vars.master_command_spells), true);
      return true;
   }

   public static boolean replaceCommandSpells(ServerPlayer player, int count, String style) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.command_spell_requires_master"), true);
         return false;
      }
      vars.master_command_spells = Math.max(0, count);
      vars.master_command_spell_style = style;
      vars.master_command_spell_pose_active = false;
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      return true;
   }

   public static boolean addSingleCommandSpell(ServerPlayer player) {
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.master_active) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.command_spell_requires_master"), true);
         return false;
      }
      vars.master_command_spells++;
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
      return true;
   }

   private static boolean extractSingleCommandSpell(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!vars.master_active || !"supervisor".equals(vars.master_command_spell_style) || vars.master_command_spells <= 0) {
         return false;
      }
      ItemStack spell = new ItemStack(ModItems.SINGLE_COMMAND_SPELL.get());
      if (!player.getInventory().add(spell)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.master.inventory_full"), true);
         return false;
      }
      vars.master_command_spells--;
      vars.master_command_spell_pose_active = false;
      vars.syncPlayerVariables(player);
      MasterVisualStateSync.broadcast(player, vars);
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
      LivingEntity servant = getBoundServantEntity(master, masterVars);
      if (servant instanceof ServerPlayer servantPlayer) {
         TypeMoonWorldModVariables.PlayerVariables servantVars = servantPlayer.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         servantVars.servant_card_master_uuid = "";
         servantVars.servant_card_contract_id = "";
         servantVars.servant_card_contract_state = MasterServantLinkService.SERVANT_CONTRACT_MASTERLESS;
         MasterServantLinkService.clearContractTags(servantPlayer);
         servantVars.syncPlayerVariables(servantPlayer);
      } else if (servant instanceof ServantEntity entityServant) {
         entityServant.unbindMaster();
         MasterServantLinkService.clearEntityContract(entityServant);
      }
   }

   private static void applyAttributes(ServerPlayer player) {
      AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
      if (attribute == null) {
         return;
      }
      double amount = MASTER_MAX_HEALTH - player.getAttributeBaseValue(Attributes.MAX_HEALTH);
      AttributeModifier existing = attribute.getModifier(MASTER_HEALTH_ID);
      if (existing == null || existing.amount() != amount || existing.operation() != AttributeModifier.Operation.ADD_VALUE) {
         attribute.removeModifier(MASTER_HEALTH_ID);
         attribute.addPermanentModifier(new AttributeModifier(MASTER_HEALTH_ID, amount, AttributeModifier.Operation.ADD_VALUE));
      }
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

   private static Vec3 findSafeRecallDestination(ServerLevel level, ServerPlayer master, ServerPlayer servant) {
      Vec3 nearMaster = findNearbySafeRecallDestination(level, master, servant);
      return nearMaster == null ? master.position() : nearMaster;
   }

   private static Vec3 findNearbySafeRecallDestination(ServerLevel level, ServerPlayer master, ServerPlayer servant) {
      BlockPos center = master.blockPosition();
      Vec3 masterPos = master.position();
      Vec3 preferred = masterPos.add(master.getLookAngle().multiply(-1.4, 0.0, -1.4));
      BlockPos preferredFeet = BlockPos.containing(preferred.x, master.getY(), preferred.z);
      Vec3 safe = safeRecallCenter(level, servant, preferredFeet);
      if (safe != null) return safe;

      for (int radius = 1; radius <= 3; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
               for (int dy = 1; dy >= -2; dy--) {
                  safe = safeRecallCenter(level, servant, center.offset(dx, dy, dz));
                  if (safe != null) return safe;
               }
            }
         }
      }

      return safeRecallCenter(level, servant, center);
   }

   private static Vec3 safeRecallCenter(ServerLevel level, ServerPlayer servant, BlockPos feet) {
      if (!level.getWorldBorder().isWithinBounds(feet)) return null;
      if (!isSafeRecallFeet(level, feet)) return null;
      Vec3 destination = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
      return level.noCollision(servant, servant.getBoundingBox().move(destination.subtract(servant.position()))) ? destination : null;
   }

   private static boolean isSafeRecallFeet(ServerLevel level, BlockPos feet) {
      BlockPos floor = feet.below();
      if (!level.getWorldBorder().isWithinBounds(floor)) return false;
      BlockState floorState = level.getBlockState(floor);
      BlockState feetState = level.getBlockState(feet);
      BlockState headState = level.getBlockState(feet.above());
      return floorState.isFaceSturdy(level, floor, Direction.UP)
         && feetState.getFluidState().isEmpty()
         && headState.getFluidState().isEmpty()
         && (feetState.isAir() || feetState.canBeReplaced())
         && (headState.isAir() || headState.canBeReplaced());
   }

   static boolean isPlayerCardContractPair(boolean masterCard, boolean masterServantCard,
                                           boolean servantMasterCard, boolean servantCard) {
      return masterCard && !masterServantCard && !servantMasterCard && servantCard;
   }

   private static String randomCommandSpellStyle(ServerPlayer player) {
      return COMMAND_SPELL_STYLES[player.getRandom().nextInt(COMMAND_SPELL_STYLES.length)];
   }

   private static String sanitizeCommandSpellStyle(String style) {
      if (style == null || style.isBlank()) {
         return "default";
      }
      if ("elsa_saijo".equals(style) || "supervisor".equals(style)) {
         return style;
      }
      for (String known : COMMAND_SPELL_STYLES) {
         if (known.equals(style)) {
            return known;
         }
      }
      return "default";
   }
}
