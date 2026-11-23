package net.xxxjk.TYPE_MOON_WORLD.magic.special;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.ModNetwork;
import net.xxxjk.TYPE_MOON_WORLD.network.SharedSensesMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterStateManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import com.captain.servant.voice.VoiceCommunicationService;

@EventBusSubscriber(modid = "typemoonworld")
public final class SharedSensesService {
   public static final String MAGIC_ID = "shared_senses";
   private static final Set<UUID> ACTIVE_MASTERS = new HashSet<>();
   private static final java.util.Map<UUID, Integer> LAST_TARGETS = new java.util.HashMap<>();
   private SharedSensesService() { }

   public static boolean toggle(ServerPlayer master) {
      if (!validMaster(master)) return fail(master, "message.typemoonworld.shared_senses.unavailable");
      LivingEntity servant = boundServant(master);
      if (servant == null) return fail(master, "message.typemoonworld.shared_senses.no_servant");
      UUID id = master.getUUID();
      if (ACTIVE_MASTERS.remove(id)) {
         VoiceCommunicationService.setSharedSensesRoute(id, null);
         LAST_TARGETS.remove(id);
         sync(master, false, -1);
         master.displayClientMessage(Component.translatable("message.typemoonworld.shared_senses.disabled"), true);
      } else {
         ACTIVE_MASTERS.add(id);
         VoiceCommunicationService.setSharedSensesRoute(id, servant.getUUID());
         LAST_TARGETS.put(id, servant.getId());
         sync(master, true, servant.getId());
         master.displayClientMessage(Component.translatable("message.typemoonworld.shared_senses.enabled"), true);
      }
      return true;
   }

   @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
      if (event.getServer().getTickCount() % 10 != 0) return;
      for (UUID masterId : Set.copyOf(ACTIVE_MASTERS)) {
         ServerPlayer master = event.getServer().getPlayerList().getPlayer(masterId);
         LivingEntity servant = master == null ? null : boundServant(master);
         if (master == null || servant == null) {
            ACTIVE_MASTERS.remove(masterId);
            LAST_TARGETS.remove(masterId);
            VoiceCommunicationService.setSharedSensesRoute(masterId, null);
            if (master != null) sync(master, false, -1);
            continue;
         }
         VoiceCommunicationService.setSharedSensesRoute(masterId, servant.getUUID());
         int targetId = servant.getId();
         if (!Integer.valueOf(targetId).equals(LAST_TARGETS.put(masterId, targetId))) sync(master, true, targetId);
      }
   }

   @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      if (ACTIVE_MASTERS.remove(id)) VoiceCommunicationService.setSharedSensesRoute(id, null);
      LAST_TARGETS.remove(id);
   }

   private static boolean validMaster(ServerPlayer player) {
      if (player == null || !player.isAlive()) return false;
      var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return vars.master_active && vars.master_card_active
         && !"emiya_shirou".equals(vars.master_card_id) && !"uryu_ryunosuke".equals(vars.master_card_id);
   }

   private static LivingEntity boundServant(ServerPlayer master) {
      if (!validMaster(master)) return null;
      LivingEntity servant = MasterStateManager.getBoundServantEntity(master, master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES));
      if (servant == null || !servant.isAlive() || servant.level() != master.level()) return null;
      if (servant instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.servant_card_transformed || !master.getUUID().toString().equals(vars.servant_card_master_uuid)) return null;
      } else if (servant instanceof ServantEntity entity && !entity.isBoundTo(master)) return null;
      else return null;
      return servant;
   }

   private static void sync(ServerPlayer master, boolean active, int targetId) {
      ModNetwork.sendToPlayer(master, new SharedSensesMessage(active, targetId));
   }

   private static boolean fail(ServerPlayer player, String key) {
      if (player != null) player.displayClientMessage(Component.translatable(key), true);
      return false;
   }
}
