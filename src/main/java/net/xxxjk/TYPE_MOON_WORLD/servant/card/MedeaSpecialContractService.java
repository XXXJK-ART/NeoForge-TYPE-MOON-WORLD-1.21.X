package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;

public final class MedeaSpecialContractService {
   public static final String LINK_STATE = "medea_subservant";
   private static final String PLAYER_TARGET_TAG = "ServantCardMedeaSubServantUuid";
   private static final String SERVANT_OWNER_TAG = "ServantCardMedeaSubContractOwner";
   private static final String SERVANT_CONTRACT_TAG = "ServantCardMedeaSubContractId";

   private MedeaSpecialContractService() {
   }

   public static boolean bind(ServerPlayer medea, ServantEntity servant) {
      if (!isMedeaCard(medea)) {
         medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.medea_only"), true);
         return false;
      }
      if (servant == null || !servant.isAlive()) {
         return false;
      }
      ServantEntity existing = findLinkedServant(medea);
      if (existing != null && existing.isAlive()) {
         medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.already_bound"), true);
         return false;
      }
      clearLocal(medea, false);
      if (servant.getMasterUuid() != null || !servant.getContractId().isBlank()) {
         medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.target_occupied"), true);
         return false;
      }
      String owner = servant.getPersistentData().getString(SERVANT_OWNER_TAG);
      if (!owner.isBlank() && !owner.equals(medea.getUUID().toString())) {
         medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.target_occupied"), true);
         return false;
      }

      String contractId = UUID.randomUUID().toString();
      servant.bindMaster(medea);
      servant.setContractId(contractId);
      servant.getPersistentData().putString(SERVANT_OWNER_TAG, medea.getUUID().toString());
      servant.getPersistentData().putString(SERVANT_CONTRACT_TAG, contractId);
      medea.getPersistentData().putString(PLAYER_TARGET_TAG, servant.getUUID().toString());
      updateSnapshot(medea, servant, true);
      medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.bound", servant.getDisplayName()), true);
      return true;
   }

   public static void tick(ServerPlayer medea) {
      if (medea == null || medea.tickCount % 10 != 0) {
         return;
      }
      if (!isMedeaCard(medea)) {
         clear(medea, false, true);
         return;
      }
      String targetId = medea.getPersistentData().getString(PLAYER_TARGET_TAG);
      if (targetId.isBlank()) {
         if (LINK_STATE.equals(medea.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).master_servant_link_state)) {
            clearLocal(medea, true);
         }
         return;
      }
      ServantEntity servant = findServant(medea, targetId);
      if (servant == null || !servant.isAlive() || !isLinked(medea, servant)) {
         clearLocal(medea, true);
         return;
      }
      updateSnapshot(medea, servant, false);
   }

   public static boolean clear(ServerPlayer medea, boolean notify, boolean killIfNoIndependentAction) {
      if (medea == null) {
         return false;
      }
      ServantEntity servant = findLinkedServant(medea);
      if (servant != null) {
         unlinkServant(medea, servant, killIfNoIndependentAction);
      }
      clearLocal(medea, true);
      if (notify) {
         medea.displayClientMessage(Component.translatable("message.typemoonworld.medea_subservant.unbound"), true);
      }
      return true;
   }

   public static boolean isLinked(ServerPlayer medea, ServantEntity servant) {
      if (medea == null || servant == null) {
         return false;
      }
      String targetId = medea.getPersistentData().getString(PLAYER_TARGET_TAG);
      return servant.getUUID().toString().equals(targetId)
         && medea.getUUID().toString().equals(servant.getPersistentData().getString(SERVANT_OWNER_TAG));
   }

   public static boolean clearOnRuleBreakerHit(LivingEntity attacker, LivingEntity target) {
      if (!(target instanceof ServantEntity servant)) {
         return false;
      }
      String ownerId = servant.getPersistentData().getString(SERVANT_OWNER_TAG);
      if (ownerId.isBlank() || !(servant.level() instanceof ServerLevel level)) {
         return false;
      }
      ServerPlayer owner = null;
      try {
         owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerId));
      } catch (IllegalArgumentException ignored) {
      }
      if (owner != null) {
         clear(owner, true, true);
      } else {
         servant.unbindMaster();
         servant.getPersistentData().remove(SERVANT_OWNER_TAG);
         servant.getPersistentData().remove(SERVANT_CONTRACT_TAG);
         forceDeathIfNoIndependentAction(servant);
      }
      return true;
   }

   private static boolean isMedeaCard(ServerPlayer player) {
      if (player == null) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.servant_card_transformed && "medea".equals(vars.servant_card_id);
   }

   private static void updateSnapshot(ServerPlayer medea, ServantEntity servant, boolean force) {
      TypeMoonWorldModVariables.PlayerVariables vars = medea.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double mana = Math.max(0.0, servant.getCurrentMp());
      double maxMana = Math.max(1.0, servant.getMaxMp());
      boolean changed = force
         || !vars.servant_card_medea_sub_servant_active
         || !servant.getUUID().toString().equals(vars.servant_card_medea_sub_servant_uuid)
         || Math.abs(vars.servant_card_medea_sub_servant_mana - mana) > 0.5
         || Math.abs(vars.servant_card_medea_sub_servant_max_mana - maxMana) > 0.5;
      vars.servant_card_medea_sub_servant_uuid = servant.getUUID().toString();
      vars.servant_card_medea_sub_servant_mana = mana;
      vars.servant_card_medea_sub_servant_max_mana = maxMana;
      vars.servant_card_medea_sub_servant_active = true;
      if (changed) {
         vars.syncMana(medea);
      }
   }

   private static void clearLocal(ServerPlayer medea, boolean sync) {
      medea.getPersistentData().remove(PLAYER_TARGET_TAG);
      TypeMoonWorldModVariables.PlayerVariables vars = medea.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (vars.servant_card_medea_sub_servant_active
         || vars.servant_card_medea_sub_servant_max_mana > 0.0
         || !vars.servant_card_medea_sub_servant_uuid.isBlank()) {
         vars.servant_card_medea_sub_servant_uuid = "";
         vars.servant_card_medea_sub_servant_mana = 0.0;
         vars.servant_card_medea_sub_servant_max_mana = 0.0;
         vars.servant_card_medea_sub_servant_active = false;
         if (sync) {
            vars.syncMana(medea);
         }
      }
   }

   private static void unlinkServant(ServerPlayer medea, ServantEntity servant, boolean killIfNoIndependentAction) {
      if (isLinked(medea, servant) || servant.isBoundTo(medea)) {
         servant.unbindMaster();
         servant.getPersistentData().remove(SERVANT_OWNER_TAG);
         servant.getPersistentData().remove(SERVANT_CONTRACT_TAG);
         if (killIfNoIndependentAction) {
            forceDeathIfNoIndependentAction(servant);
         }
      }
   }

   private static ServantEntity findLinkedServant(ServerPlayer medea) {
      if (medea == null || medea.getServer() == null) {
         return null;
      }
      return findServant(medea, medea.getPersistentData().getString(PLAYER_TARGET_TAG));
   }

   private static ServantEntity findServant(ServerPlayer medea, String uuidText) {
      if (medea == null || medea.getServer() == null || uuidText == null || uuidText.isBlank()) {
         return null;
      }
      try {
         UUID uuid = UUID.fromString(uuidText);
         for (ServerLevel level : medea.getServer().getAllLevels()) {
            if (level.getEntity(uuid) instanceof ServantEntity servant) {
               return servant;
            }
         }
      } catch (IllegalArgumentException ignored) {
      }
      return null;
   }

   private static void forceDeathIfNoIndependentAction(ServantEntity servant) {
      if (servant == null || hasIndependentAction(servant)) {
         return;
      }
      servant.invulnerableTime = 0;
      servant.hurt(servant.damageSources().magic(), Float.MAX_VALUE);
      if (servant.isAlive()) {
         servant.setHealth(0.0F);
         servant.die(servant.damageSources().genericKill());
      }
   }

   private static boolean hasIndependentAction(ServantEntity servant) {
      ServantDefinition definition = servant.getDefinition();
      if (definition == null) {
         return false;
      }
      return definition.skillIds().contains("independent_action_a")
         || definition.skillIds().contains("independent_action_b")
         || definition.skillIds().contains("independent_action_c");
   }
}
