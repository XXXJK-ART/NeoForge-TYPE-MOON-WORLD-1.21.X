package net.xxxjk.TYPE_MOON_WORLD.network;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.typemoonworld.api.MagicDefinitionData;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantSkillDataRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantSkillDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiDefinition;
import net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry;

public final class DefinitionSnapshotService {
   private static long revision;
   private static String cachedSnapshot;
   private static final GsonBuilder GSON = new GsonBuilder();
   private DefinitionSnapshotService() { }

   public static void invalidate() {
      cachedSnapshot = null;
   }

   public static void send(ServerPlayer player) {
      if (player == null || !NetworkRegistry.hasChannel(player.connection, DefinitionSnapshotMessage.TYPE.id())) return;
      try {
         if (cachedSnapshot == null) {
            cachedSnapshot = build();
            revision++;
         }
         PacketDistributor.sendToPlayer(player, new DefinitionSnapshotMessage(revision, cachedSnapshot));
      } catch (UnsupportedOperationException exception) {
         // The negotiated channel can disappear while a player disconnects or changes protocol state.
         TYPE_MOON_WORLD.LOGGER.debug("Skipped definition snapshot for unsupported connection {}", player.getGameProfile().getName());
      }
   }
   public static String build() {
      JsonObject root = new JsonObject();
      JsonObject magic = new JsonObject(); MagicDefinitionRegistry.all().forEach((id, def) -> MagicDefinitionData.CODEC.encodeStart(JsonOps.INSTANCE, def).result().ifPresent(json -> magic.add(id, json)));
      root.add("magic", magic);
      JsonObject servants = new JsonObject(); ServantDataRegistry.getAll().keySet().forEach(id -> servants.addProperty(id, true)); root.add("servants", servants);
      root.add("skills", encodeSkills()); root.add("noble_phantasms", encodeNps()); root.add("ai", encodeAi());
      JsonObject cards = new JsonObject(); CardActionRegistry.snapshotBindings().forEach(cards::addProperty); root.add("cards", cards);
      return GSON.create().toJson(root);
   }
   private static JsonObject encodeSkills() { JsonObject root = new JsonObject(); ServantSkillDataRegistry.all().forEach((id, def) -> ServantSkillDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).result().ifPresent(json -> root.add(id, json))); return root; }
   private static JsonObject encodeNps() { JsonObject root = new JsonObject(); ServantNoblePhantasmDataRegistry.all().forEach((id, def) -> ServantNoblePhantasmDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).result().ifPresent(json -> root.add(id, json))); return root; }
   private static JsonObject encodeAi() { JsonObject root = new JsonObject(); ServantAiDefinitionRegistry.all().forEach((id, def) -> ServantAiDefinition.CODEC.encodeStart(JsonOps.INSTANCE, def).result().ifPresent(json -> root.add(id, json))); return root; }
}
