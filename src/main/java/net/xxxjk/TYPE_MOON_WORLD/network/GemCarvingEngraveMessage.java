package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.world.inventory.GemCarvingTableMenu;
import org.jetbrains.annotations.NotNull;

public record GemCarvingEngraveMessage(
        String magicId,
        int reinforcementPart,
        int reinforcementLevel,
        int projectionMode,
        int projectionItemIndex,
        String projectionStructureId
) implements CustomPacketPayload {
    private static final int MAX_MAGIC_ID_LENGTH = 64;
    private static final int MAX_STRUCTURE_ID_LENGTH = 128;

    public static final Type<GemCarvingEngraveMessage> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gem_carving_engrave")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, GemCarvingEngraveMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, GemCarvingEngraveMessage message) -> {
                buffer.writeUtf(message.magicId);
                buffer.writeInt(message.reinforcementPart);
                buffer.writeInt(message.reinforcementLevel);
                buffer.writeInt(message.projectionMode);
                buffer.writeInt(message.projectionItemIndex);
                buffer.writeUtf(message.projectionStructureId == null ? "" : message.projectionStructureId, MAX_STRUCTURE_ID_LENGTH);
            },
            (RegistryFriendlyByteBuf buffer) -> new GemCarvingEngraveMessage(
                    buffer.readUtf(MAX_MAGIC_ID_LENGTH),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readUtf(MAX_STRUCTURE_ID_LENGTH)
            )
    );

    @Override
    public @NotNull Type<GemCarvingEngraveMessage> type() {
        return TYPE;
    }

    public static void handleData(final GemCarvingEngraveMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }

        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!isValidMagicId(message.magicId)) {
                return;
            }
            if (player.containerMenu instanceof GemCarvingTableMenu menu) {
                menu.tryEngrave(
                        player,
                        message.magicId,
                        message.reinforcementPart,
                        message.reinforcementLevel,
                        message.projectionMode,
                        message.projectionItemIndex,
                        message.projectionStructureId
                );
            }
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
        });
    }

    private static boolean isValidMagicId(String magicId) {
        return magicId != null
                && !magicId.isEmpty()
                && magicId.length() <= MAX_MAGIC_ID_LENGTH
                && magicId.matches("[a-z0-9_]+");
    }
}
