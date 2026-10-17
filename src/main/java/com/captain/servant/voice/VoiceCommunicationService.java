package com.captain.servant.voice;

import com.captain.servant.TypeMoonServant;
import com.captain.servant.item.MessageStoneItem;
import com.captain.servant.item.MessageStoneItem.Binding;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.MagicKnowledge;
import net.xxxjk.typemoonworld.api.MasterAccess;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

public final class VoiceCommunicationService {
    public static final ResourceLocation MESSAGE_MAGIC_ID =
            ResourceLocation.fromNamespaceAndPath(TypeMoonServant.MOD_ID, "message_magic");

    private static final Set<UUID> MESSAGE_MAGIC_MASTERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> TELEPATHY_MASTERS = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<RoutingSnapshot> ROUTING =
            new AtomicReference<>(RoutingSnapshot.EMPTY);
    private static volatile float voiceDistance = 48.0F;

    private final Map<UUID, ItemEntity> trackedDroppedStones = new HashMap<>();
    private final Map<UUID, PendingStoneRemoval> pendingStoneRemovals = new HashMap<>();

    public static boolean isAvailable() {
        return ModList.get().isLoaded("voicechat");
    }

    private static boolean requireVoicechat(ServerPlayer player) {
        if (isAvailable()) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.fatesupport.voicechat_missing"), true);
        return false;
    }

    public static boolean toggleMessageMagic(ServerPlayer player) {
        if (!requireVoicechat(player)) {
            return false;
        }
        if (!hasUsableMasterContract(player)) {
            MESSAGE_MAGIC_MASTERS.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("message.fatesupport.message_magic.unavailable"), true);
            return false;
        }

        if (MESSAGE_MAGIC_MASTERS.remove(player.getUUID())) {
            notifyPair(player, "message.fatesupport.message_magic.disabled");
        } else {
            MESSAGE_MAGIC_MASTERS.add(player.getUUID());
            notifyPair(player, "message.fatesupport.message_magic.enabled");
        }
        return true;
    }

    public static boolean toggleTelepathy(ServerPlayer servant) {
        if (!requireVoicechat(servant)) {
            return false;
        }
        ServerPlayer master = masterForServant(servant);
        if (master == null || !hasUsableMasterContract(master)
                || !servant.getUUID().equals(TypeMoonWorldApi.master(master).boundServant())) {
            servant.displayClientMessage(Component.translatable("message.fatesupport.telepathy.unavailable"), true);
            return false;
        }

        if (TELEPATHY_MASTERS.remove(master.getUUID())) {
            notifyPair(master, "message.fatesupport.telepathy.disabled");
        } else {
            TELEPATHY_MASTERS.add(master.getUUID());
            notifyPair(master, "message.fatesupport.telepathy.enabled");
        }
        return true;
    }

    public static PrivateVoiceRoute privateVoiceRoute(UUID speakerId) {
        return ROUTING.get().privateRoutes().get(speakerId);
    }

    public static List<VoiceRoute> voiceRoutes(UUID speakerId) {
        return ROUTING.get().stoneRoutes().getOrDefault(speakerId, List.of());
    }

    public static void setVoiceDistance(double distance) {
        if (Double.isFinite(distance)) {
            voiceDistance = (float) Math.max(1.0, Math.min(256.0, distance));
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!isAvailable()) {
            return;
        }
        MinecraftServer server = event.getServer();
        MessageStoneSavedData savedData = MessageStoneSavedData.get(server);

        if (server.getTickCount() % 20 == 0) {
            learnMessageMagic(server);
        }

        Map<UUID, StoneEndpoint> endpoints = scanStoneEndpoints(server, savedData);
        if (processPendingRemovals(server, savedData, endpoints)) {
            clearInvalidBindings(server, savedData);
            endpoints = scanStoneEndpoints(server, savedData);
        }
        ROUTING.set(buildRoutingSnapshot(server, endpoints));
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity itemEntity
                && itemEntity.getItem().getItem() instanceof MessageStoneItem) {
            trackedDroppedStones.put(itemEntity.getUUID(), itemEntity);
        }
    }

    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }

        trackedDroppedStones.remove(itemEntity.getUUID());
        Binding binding = MessageStoneItem.binding(itemEntity.getItem());
        RemovalReason reason = itemEntity.getRemovalReason();
        if (binding != null && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            MinecraftServer server = ((ServerLevel) event.getLevel()).getServer();
            pendingStoneRemovals.put(
                    binding.stoneId(),
                    new PendingStoneRemoval(binding.pairId(), (long) server.getTickCount() + 2L)
            );
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        trackedDroppedStones.clear();
        pendingStoneRemovals.clear();
        MESSAGE_MAGIC_MASTERS.clear();
        TELEPATHY_MASTERS.clear();
        ROUTING.set(RoutingSnapshot.EMPTY);
    }

    private static void learnMessageMagic(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                MasterAccess master = TypeMoonWorldApi.master(player);
                if (master.active()) {
                    MagicKnowledge knowledge = TypeMoonWorldApi.addon(TypeMoonServant.MOD_ID).magics().knowledge(player);
                    if (!knowledge.isLearned(MESSAGE_MAGIC_ID)) {
                        knowledge.learn(MESSAGE_MAGIC_ID);
                    }
                }
            } catch (RuntimeException exception) {
                TypeMoonServant.LOGGER.error("Failed to grant message magic to {}", player.getGameProfile().getName(), exception);
            }
        }
    }

    private static boolean hasUsableMasterContract(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.getServer() == null) {
            return false;
        }
        MasterAccess master = TypeMoonWorldApi.master(player);
        return master.active() && master.commandSpells() > 0 && master.boundServant() != null;
    }

    private static ServerPlayer onlineServant(ServerPlayer masterPlayer) {
        if (!hasUsableMasterContract(masterPlayer)) {
            return null;
        }
        UUID servantId = TypeMoonWorldApi.master(masterPlayer).boundServant();
        ServerPlayer servant = masterPlayer.getServer().getPlayerList().getPlayer(servantId);
        return servant != null && servant.isAlive() && TypeMoonWorldApi.servantForm(servant).transformed()
                ? servant
                : null;
    }

    private static ServerPlayer masterForServant(ServerPlayer servant) {
        if (servant == null || !servant.isAlive() || servant.getServer() == null) {
            return null;
        }
        TypeMoonWorldModVariables.PlayerVariables variables =
                servant.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (!variables.servant_card_transformed || variables.servant_card_master_uuid == null
                || variables.servant_card_master_uuid.isBlank()) {
            return null;
        }
        try {
            return servant.getServer().getPlayerList().getPlayer(UUID.fromString(variables.servant_card_master_uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void notifyPair(ServerPlayer master, String translationKey) {
        Component message = Component.translatable(translationKey);
        master.displayClientMessage(message, true);
        ServerPlayer servant = onlineServant(master);
        if (servant != null) {
            servant.displayClientMessage(message, true);
        }
    }

    private Map<UUID, StoneEndpoint> scanStoneEndpoints(MinecraftServer server, MessageStoneSavedData savedData) {
        Map<UUID, StoneEndpoint> endpoints = new HashMap<>();
        Set<UUID> notifiedPlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                Binding binding = MessageStoneItem.binding(stack);
                if (binding == null) {
                    continue;
                }
                if (savedData.isInvalid(binding.pairId())) {
                    MessageStoneItem.clearBinding(stack);
                    notifiedPlayers.add(player.getUUID());
                    continue;
                }
                Vec3 position = new Vec3(player.getX(), player.getEyeY() - 0.2, player.getZ());
                endpoints.putIfAbsent(
                        binding.stoneId(),
                        new StoneEndpoint(
                                binding,
                                player.level().dimension(),
                                position,
                                stack,
                                player.getGameProfile().getName(),
                                null
                        )
                );
            }
        }

        trackedDroppedStones.values().removeIf(ItemEntity::isRemoved);
        for (ItemEntity itemEntity : trackedDroppedStones.values()) {
            Binding binding = MessageStoneItem.binding(itemEntity.getItem());
            if (binding == null) {
                continue;
            }
            if (savedData.isInvalid(binding.pairId())) {
                ItemStack stack = itemEntity.getItem();
                MessageStoneItem.clearBinding(stack);
                itemEntity.setItem(stack);
                continue;
            }
            endpoints.putIfAbsent(
                    binding.stoneId(),
                    new StoneEndpoint(
                            binding,
                            itemEntity.level().dimension(),
                            itemEntity.position(),
                            itemEntity.getItem(),
                            null,
                            itemEntity
                    )
            );
        }

        synchronizePartnerHolderNames(endpoints);

        for (UUID playerId : notifiedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.fatesupport.message_stone.link_lost"), true);
            }
        }
        return endpoints;
    }

    private static void synchronizePartnerHolderNames(Map<UUID, StoneEndpoint> endpoints) {
        for (StoneEndpoint endpoint : endpoints.values()) {
            StoneEndpoint partner = endpoints.get(endpoint.binding().partnerId());
            String holderName = partner == null ? null : partner.holderName();
            if (MessageStoneItem.setPartnerHolderName(endpoint.stack(), holderName)
                    && endpoint.droppedEntity() != null) {
                endpoint.droppedEntity().setItem(endpoint.stack());
            }
        }
    }

    private boolean processPendingRemovals(
            MinecraftServer server,
            MessageStoneSavedData savedData,
            Map<UUID, StoneEndpoint> endpoints
    ) {
        long currentTick = server.getTickCount();
        boolean invalidated = false;
        var iterator = pendingStoneRemovals.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingStoneRemoval> entry = iterator.next();
            PendingStoneRemoval pending = entry.getValue();
            if (pending.checkAtTick() > currentTick) {
                continue;
            }
            if (!endpoints.containsKey(entry.getKey())) {
                invalidated |= savedData.invalidate(pending.pairId());
            }
            iterator.remove();
        }
        return invalidated;
    }

    private void clearInvalidBindings(MinecraftServer server, MessageStoneSavedData savedData) {
        Set<UUID> notifiedPlayers = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                Binding binding = MessageStoneItem.binding(stack);
                if (binding != null && savedData.isInvalid(binding.pairId())) {
                    MessageStoneItem.clearBinding(stack);
                    notifiedPlayers.add(player.getUUID());
                }
            }
        }
        for (ItemEntity itemEntity : trackedDroppedStones.values()) {
            ItemStack stack = itemEntity.getItem();
            Binding binding = MessageStoneItem.binding(stack);
            if (binding != null && savedData.isInvalid(binding.pairId())) {
                MessageStoneItem.clearBinding(stack);
                itemEntity.setItem(stack);
            }
        }
        for (UUID playerId : notifiedPlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.fatesupport.message_stone.link_lost"), true);
            }
        }
    }

    private static RoutingSnapshot buildRoutingSnapshot(MinecraftServer server, Map<UUID, StoneEndpoint> endpoints) {
        Map<UUID, List<VoiceRoute>> routes = new HashMap<>();
        Map<UUID, ServerPlayer> onlinePlayers = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.put(player.getUUID(), player);
        }
        Map<UUID, PrivateVoiceRoute> privateRoutes = buildPrivateRoutes(server, onlinePlayers.values());

        Set<UUID> activeStoneIds = new HashSet<>();
        for (ServerPlayer player : onlinePlayers.values()) {
            if (player.isUsingItem()) {
                Binding binding = MessageStoneItem.binding(player.getUseItem());
                if (binding != null && endpoints.containsKey(binding.stoneId())) {
                    activeStoneIds.add(binding.stoneId());
                }
            }
        }

        float range = voiceDistance;
        double rangeSqr = (double) range * range;
        for (UUID activeStoneId : activeStoneIds) {
            StoneEndpoint source = endpoints.get(activeStoneId);
            StoneEndpoint destination = endpoints.get(source.binding().partnerId());
            if (!isMutualPair(source, destination)) {
                continue;
            }

            List<ServerPlayer> speakers = playersNear(onlinePlayers.values(), source, rangeSqr);
            List<ServerPlayer> destinationListeners = playersNear(onlinePlayers.values(), destination, rangeSqr);
            for (ServerPlayer speaker : speakers) {
                Set<UUID> listeners = new HashSet<>();
                for (ServerPlayer listener : destinationListeners) {
                    listeners.add(listener.getUUID());
                }
                if (!listeners.isEmpty()) {
                    routes.computeIfAbsent(speaker.getUUID(), ignored -> new ArrayList<>())
                            .add(new VoiceRoute(
                                    stoneVoiceChannel(speaker.getUUID(), destination.binding().stoneId()),
                                    destination.position().x,
                                    destination.position().y,
                                    destination.position().z,
                                    range,
                                    Set.copyOf(listeners)
                            ));
                }
            }
        }

        Map<UUID, List<VoiceRoute>> immutableRoutes = new HashMap<>();
        routes.forEach((speaker, speakerRoutes) -> immutableRoutes.put(speaker, List.copyOf(speakerRoutes)));
        return new RoutingSnapshot(Map.copyOf(privateRoutes), Map.copyOf(immutableRoutes));
    }

    private static Map<UUID, PrivateVoiceRoute> buildPrivateRoutes(
            MinecraftServer server,
            Iterable<ServerPlayer> onlinePlayers
    ) {
        Map<UUID, PrivateVoiceRoute> privateRoutes = new HashMap<>();
        Set<UUID> enabledMasters = new HashSet<>(MESSAGE_MAGIC_MASTERS);
        enabledMasters.addAll(TELEPATHY_MASTERS);
        for (UUID masterId : enabledMasters) {
            ServerPlayer master = server.getPlayerList().getPlayer(masterId);
            if (master == null) {
                continue;
            }
            if (!hasUsableMasterContract(master)) {
                ServerPlayer servant = linkedServant(master);
                boolean messageMagicEnded = MESSAGE_MAGIC_MASTERS.remove(masterId);
                boolean telepathyEnded = TELEPATHY_MASTERS.remove(masterId);
                String endedKey = telepathyEnded && !messageMagicEnded
                        ? "message.fatesupport.telepathy.ended"
                        : "message.fatesupport.message_magic.ended";
                master.displayClientMessage(Component.translatable(endedKey), true);
                if (servant != null) {
                    servant.displayClientMessage(Component.translatable(endedKey), true);
                }
                continue;
            }
            ServerPlayer servant = onlineServant(master);
            if (servant != null) {
                addPrivateVoiceRoute(privateRoutes, master, servant.getUUID(), onlinePlayers);
                addPrivateVoiceRoute(privateRoutes, servant, masterId, onlinePlayers);
            }
        }
        return privateRoutes;
    }

    private static ServerPlayer linkedServant(ServerPlayer master) {
        TypeMoonWorldModVariables.PlayerVariables variables =
                master.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        if (master.getServer() == null || variables.master_servant_uuid == null
                || variables.master_servant_uuid.isBlank()) {
            return null;
        }
        try {
            return master.getServer().getPlayerList().getPlayer(UUID.fromString(variables.master_servant_uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void addPrivateVoiceRoute(
            Map<UUID, PrivateVoiceRoute> routes,
            ServerPlayer speaker,
            UUID targetId,
            Iterable<ServerPlayer> onlinePlayers
    ) {
        float range = voiceDistance;
        double rangeSqr = (double) range * range;
        Set<UUID> spectatorListeners = new HashSet<>();
        for (ServerPlayer player : onlinePlayers) {
            if (player.isSpectator()
                    && !player.getUUID().equals(speaker.getUUID())
                    && !player.getUUID().equals(targetId)
                    && player.level().dimension().equals(speaker.level().dimension())
                    && player.position().distanceToSqr(speaker.position()) <= rangeSqr) {
                spectatorListeners.add(player.getUUID());
            }
        }

        routes.put(
                speaker.getUUID(),
                new PrivateVoiceRoute(
                        targetId,
                        privateVoiceChannel(speaker.getUUID()),
                        speaker.getX(),
                        speaker.getEyeY() - 0.2,
                        speaker.getZ(),
                        range,
                        Set.copyOf(spectatorListeners)
                )
        );
    }

    private static boolean isMutualPair(StoneEndpoint first, StoneEndpoint second) {
        return first != null
                && second != null
                && first.binding().pairId().equals(second.binding().pairId())
                && first.binding().partnerId().equals(second.binding().stoneId())
                && second.binding().partnerId().equals(first.binding().stoneId());
    }

    private static List<ServerPlayer> playersNear(
            Iterable<ServerPlayer> players,
            StoneEndpoint endpoint,
            double rangeSqr
    ) {
        List<ServerPlayer> nearby = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (player.isAlive()
                    && player.level().dimension().equals(endpoint.dimension())
                    && player.position().distanceToSqr(endpoint.position()) <= rangeSqr) {
                nearby.add(player);
            }
        }
        return nearby;
    }

    private static UUID stoneVoiceChannel(UUID speakerId, UUID destinationStoneId) {
        String key = TypeMoonServant.MOD_ID + ":message_stone:" + speakerId + ":" + destinationStoneId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static UUID privateVoiceChannel(UUID speakerId) {
        String key = TypeMoonServant.MOD_ID + ":message_magic:spectator:" + speakerId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public record PrivateVoiceRoute(
            UUID targetId,
            UUID channelId,
            double x,
            double y,
            double z,
            float distance,
            Set<UUID> spectatorListeners
    ) {
    }

    public record VoiceRoute(UUID channelId, double x, double y, double z, float distance, Set<UUID> listeners) {
    }

    private record StoneEndpoint(
            Binding binding,
            ResourceKey<Level> dimension,
            Vec3 position,
            ItemStack stack,
            String holderName,
            ItemEntity droppedEntity
    ) {
    }

    private record PendingStoneRemoval(UUID pairId, long checkAtTick) {
    }

    private record RoutingSnapshot(
            Map<UUID, PrivateVoiceRoute> privateRoutes,
            Map<UUID, List<VoiceRoute>> stoneRoutes
    ) {
        private static final RoutingSnapshot EMPTY = new RoutingSnapshot(Map.of(), Map.of());
    }
}
