package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.jewel.GemEngravingService;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGravity;
import org.jetbrains.annotations.NotNull;

public record GemGravitySelfCastMessage(int hand, int mode) implements CustomPacketPayload {
    public static final Type<GemGravitySelfCastMessage> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gem_gravity_self_cast")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, GemGravitySelfCastMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, GemGravitySelfCastMessage message) -> {
                buffer.writeInt(message.hand);
                buffer.writeInt(message.mode);
            },
            (RegistryFriendlyByteBuf buffer) -> new GemGravitySelfCastMessage(
                    buffer.readInt(),
                    buffer.readInt()
            )
    );

    @Override
    public @NotNull Type<GemGravitySelfCastMessage> type() {
        return TYPE;
    }

    public static void handleData(final GemGravitySelfCastMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }

        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (message.mode < MagicGravity.MODE_ULTRA_LIGHT || message.mode > MagicGravity.MODE_ULTRA_HEAVY) {
                return;
            }

            InteractionHand interactionHand = message.hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            GemEngravingService.castGravitySelfFromGem(player, interactionHand, message.mode);
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
        });
    }
}
