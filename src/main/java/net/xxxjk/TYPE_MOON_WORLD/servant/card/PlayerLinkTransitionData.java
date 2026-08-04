package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

final class PlayerLinkTransitionData extends SavedData {
   private static final String DATA_NAME = "typemoonworld_player_link_transitions";
   private static final Factory<PlayerLinkTransitionData> FACTORY = new Factory<>(
      PlayerLinkTransitionData::new, PlayerLinkTransitionData::load);
   private final Map<UUID, Pending> pendingByTarget = new HashMap<>();

   static PlayerLinkTransitionData get(MinecraftServer server) {
      return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
   }

   void queue(UUID target, UUID partner, MasterServantLinkService.UnlinkReason reason) {
      if (target == null || partner == null || reason == MasterServantLinkService.UnlinkReason.CONTRACT_TERMINATED) return;
      this.pendingByTarget.put(target, new Pending(partner, reason));
      this.setDirty();
   }

   void consume(ServerPlayer player) {
      Pending pending = this.pendingByTarget.remove(player.getUUID());
      if (pending == null) return;
      this.setDirty();
      MasterServantLinkService.applyPendingTransition(player, pending.partner(), pending.reason());
   }

   private static PlayerLinkTransitionData load(CompoundTag tag, HolderLookup.Provider provider) {
      PlayerLinkTransitionData data = new PlayerLinkTransitionData();
      ListTag list = tag.getList("Pending", 10);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag entry = list.getCompound(i);
         if (!entry.hasUUID("Target") || !entry.hasUUID("Partner")) continue;
         try {
            MasterServantLinkService.UnlinkReason reason = MasterServantLinkService.UnlinkReason.valueOf(entry.getString("Reason"));
            if (reason != MasterServantLinkService.UnlinkReason.CONTRACT_TERMINATED) {
               data.pendingByTarget.put(entry.getUUID("Target"), new Pending(entry.getUUID("Partner"), reason));
            }
         } catch (IllegalArgumentException ignored) {
         }
      }
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      ListTag list = new ListTag();
      this.pendingByTarget.forEach((target, pending) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Target", target);
         entry.putUUID("Partner", pending.partner());
         entry.putString("Reason", pending.reason().name());
         list.add(entry);
      });
      tag.put("Pending", list);
      return tag;
   }

   private record Pending(UUID partner, MasterServantLinkService.UnlinkReason reason) {
   }
}
