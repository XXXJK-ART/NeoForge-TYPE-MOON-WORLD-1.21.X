package com.example.typemoonaddon.engravedworm;

import com.example.typemoonaddon.TypeMoonAddon;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenEngravedWormMenuPayload(int page) implements CustomPacketPayload {
    public static final Type<OpenEngravedWormMenuPayload> TYPE =
            new Type<>(TypeMoonAddon.id("open_engraved_worm_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEngravedWormMenuPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.page()),
            buffer -> new OpenEngravedWormMenuPayload(buffer.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenEngravedWormMenuPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> open(player, payload.page()));
        }
    }

    public static void open(ServerPlayer player, int page) {
        if (player == null) {
            return;
        }
        int normalized = Math.max(0, Math.min(EngravedWormService.maxPage(player), page));
        player.openMenu(new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                return Component.translatable("menu.typemoonworld.engraved_worms");
            }

            @Override
            public @NotNull AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player menuPlayer) {
                return new EngravedWormMenu(containerId, inventory, normalized);
            }
        }, buffer -> buffer.writeVarInt(normalized));
    }
}
