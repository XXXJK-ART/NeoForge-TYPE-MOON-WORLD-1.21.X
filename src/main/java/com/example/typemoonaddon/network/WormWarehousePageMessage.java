package com.example.typemoonaddon.network;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.worm.WormWarehouseService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.WormWarehouseMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WormWarehousePageMessage(int page) implements CustomPacketPayload {
    public static final Type<WormWarehousePageMessage> TYPE = new Type<>(TypeMoonAddon.id("worm_warehouse_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WormWarehousePageMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeVarInt(message.page()),
            buffer -> new WormWarehousePageMessage(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WormWarehousePageMessage message, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> {
                if (player.containerMenu instanceof WormWarehouseMenu menu) {
                    menu.setPage(Math.max(0, Math.min(menu.getMaxPage(), message.page())));
                }
            });
        }
    }
}
