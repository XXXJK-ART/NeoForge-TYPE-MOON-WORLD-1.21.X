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

public class TypeMoonWorldModVariables {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TYPE_MOON_WORLD.MOD_ID);
    public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables",
            () -> AttachmentType.serializable(PlayerVariables::new).build());

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
            clone.player_magic_attributes_earth = original.player_magic_attributes_earth;
            clone.player_magic_attributes_water = original.player_magic_attributes_water;
            clone.player_magic_attributes_fire = original.player_magic_attributes_fire;
            clone.player_magic_attributes_wind = original.player_magic_attributes_wind;
            clone.player_magic_attributes_ether = original.player_magic_attributes_ether;
            clone.player_magic_attributes_none = original.player_magic_attributes_none;
            clone.player_magic_attributes_imaginary_number = original.player_magic_attributes_imaginary_number;
            clone.player_magic_attributes_sword = original.player_magic_attributes_sword;
            clone.is_magic_circuit_open = original.is_magic_circuit_open;
            clone.magic_circuit_open_timer = original.magic_circuit_open_timer;
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
        public boolean player_magic_attributes_earth = false;
        public boolean player_magic_attributes_water = false;
        public boolean player_magic_attributes_fire = false;
        public boolean player_magic_attributes_wind = false;
        public boolean player_magic_attributes_ether = false;
        public boolean player_magic_attributes_none = false;
        public boolean player_magic_attributes_imaginary_number = false;
        public boolean player_magic_attributes_sword = false;
        public boolean is_magic_circuit_open = false;
        public double magic_circuit_open_timer = 0;

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider lookupProvider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putDouble("player_mana", player_mana);
            nbt.putDouble("player_max_mana", player_max_mana);
            nbt.putDouble("player_mana_egenerated_every_moment", player_mana_egenerated_every_moment);
            nbt.putDouble("player_restore_magic_moment", player_restore_magic_moment);
            nbt.putBoolean("player_magic_attributes_earth", player_magic_attributes_earth);
            nbt.putBoolean("player_magic_attributes_water", player_magic_attributes_water);
            nbt.putBoolean("player_magic_attributes_fire", player_magic_attributes_fire);
            nbt.putBoolean("player_magic_attributes_wind", player_magic_attributes_wind);
            nbt.putBoolean("player_magic_attributes_ether", player_magic_attributes_ether);
            nbt.putBoolean("player_magic_attributes_none", player_magic_attributes_none);
            nbt.putBoolean("player_magic_attributes_imaginary_number", player_magic_attributes_imaginary_number);
            nbt.putBoolean("player_magic_attributes_sword", player_magic_attributes_sword);
            nbt.putBoolean("is_magic_circuit_open", is_magic_circuit_open);
            nbt.putDouble("magic_circuit_open_timer", magic_circuit_open_timer);
            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider lookupProvider, CompoundTag nbt) {
            player_mana = nbt.getDouble("player_mana");
            player_max_mana = nbt.getDouble("player_max_mana");
            player_mana_egenerated_every_moment = nbt.getDouble("player_mana_egenerated_every_moment");
            player_restore_magic_moment = nbt.getDouble("player_restore_magic_moment");
            player_magic_attributes_earth = nbt.getBoolean("player_magic_attributes_earth");
            player_magic_attributes_water = nbt.getBoolean("player_magic_attributes_water");
            player_magic_attributes_fire = nbt.getBoolean("player_magic_attributes_fire");
            player_magic_attributes_wind = nbt.getBoolean("player_magic_attributes_wind");
            player_magic_attributes_ether = nbt.getBoolean("player_magic_attributes_ether");
            player_magic_attributes_none = nbt.getBoolean("player_magic_attributes_none");
            player_magic_attributes_imaginary_number = nbt.getBoolean("player_magic_attributes_imaginary_number");
            player_magic_attributes_sword = nbt.getBoolean("player_magic_attributes_sword");
            is_magic_circuit_open = nbt.getBoolean("is_magic_circuit_open");
            magic_circuit_open_timer = nbt.getDouble("magic_circuit_open_timer");
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