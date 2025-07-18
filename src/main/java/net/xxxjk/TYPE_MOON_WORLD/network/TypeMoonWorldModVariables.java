package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class TypeMoonWorldModVariables {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TYPE_MOON_WORLD.MOD_ID);
    public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables",
            () -> AttachmentType.serializable(PlayerVariables::new).build());

    @SubscribeEvent
    public static void init(FMLCommonSetupEvent event) {
        TYPE_MOON_WORLD.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
    }

    @EventBusSubscriber
    public static class EventBusVariableHandlers {
        @SubscribeEvent
        public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
            PlayerVariables clone = new PlayerVariables();
            clone.player_max_mana = original.player_max_mana;
            clone.player_mana_egenerated_every_moment = original.player_mana_egenerated_every_moment;
            clone.player_restore_magic_moment = original.player_restore_magic_moment;
            if (!event.isWasDeath()) {
                clone.player_mana = original.player_mana;
            }
            event.getEntity().setData(PLAYER_VARIABLES, clone);
        }
    }

    public static class PlayerVariables implements INBTSerializable<CompoundTag> {
        public double player_mana = 0;
        public double player_max_mana = 0;
        public double player_mana_egenerated_every_moment = 0;
        public double player_restore_magic_moment = 0;

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider lookupProvider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putDouble("player_mana", player_mana);
            nbt.putDouble("player_max_mana", player_max_mana);
            nbt.putDouble("player_mana_egenerated_every_moment", player_mana_egenerated_every_moment);
            nbt.putDouble("player_restore_magic_moment", player_restore_magic_moment);
            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider lookupProvider, CompoundTag nbt) {
            player_mana = nbt.getDouble("player_mana");
            player_max_mana = nbt.getDouble("player_max_mana");
            player_mana_egenerated_every_moment = nbt.getDouble("player_mana_egenerated_every_moment");
            player_restore_magic_moment = nbt.getDouble("player_restore_magic_moment");
        }

        public void syncPlayerVariables(Entity entity) {
            if (entity instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new PlayerVariablesSyncMessage(this));
        }
    }

    public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
        public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "player_variables_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec
                .of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess())), (RegistryFriendlyByteBuf buffer) -> {
                    PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
                    message.data.deserializeNBT(buffer.registryAccess(), Objects.requireNonNull(buffer.readNbt()));
                    return message;
                });

        @Override
        public @NotNull Type<PlayerVariablesSyncMessage> type() {
            return TYPE;
        }

        public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
            if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
                context.enqueueWork(() -> context.player().getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()))).exceptionally(e -> {
                    context.connection().disconnect(Component.literal(e.getMessage()));
                    return null;
                });
            }
        }
    }
}