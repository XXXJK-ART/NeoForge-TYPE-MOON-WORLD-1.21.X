package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

/** Clientbound grail screen open, or serverbound action envelope. */
public record GrailOpenPayload(InteractionHand hand, int action, String wish) implements CustomPacketPayload {
    public static final int ACTION_OPEN_FIRST = 0;
    public static final int ACTION_THIRD_MAGIC = 1;
    public static final int ACTION_WISH_OPEN = 2;
    public static final int ACTION_WISH_EXECUTE = 3;
    public static final Type<GrailOpenPayload> TYPE = new Type<>(TYPE_MOON_WORLD.id("grail_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GrailOpenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeByte(payload.hand == null ? -1 : payload.hand == InteractionHand.OFF_HAND ? 1 : 0);
                buf.writeVarInt(payload.action);
                buf.writeUtf(payload.wish == null ? "" : payload.wish, 1024);
            },
            buf -> {
                int hand = buf.readByte();
                InteractionHand decoded = hand == 1 ? InteractionHand.OFF_HAND : hand < 0 ? null : InteractionHand.MAIN_HAND;
                return new GrailOpenPayload(decoded, buf.readVarInt(), buf.readUtf(1024));
            }
    );

    public GrailOpenPayload(InteractionHand hand) { this(hand, ACTION_OPEN_FIRST, ""); }
    public GrailOpenPayload(int action) { this(null, action, ""); }
    public GrailOpenPayload(int action, String wish) { this(null, action, wish); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(GrailOpenPayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> handleClient(payload));
        } else if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
            context.enqueueWork(() -> net.xxxjk.TYPE_MOON_WORLD.grail.GrailWishService.handleAction(player, payload.action, payload.wish));
        }
    }

    /** Called by the client receiver registered below; kept separate from the server handler. */
    public static void handleClient(GrailOpenPayload payload) {
        net.xxxjk.TYPE_MOON_WORLD.client.GrailClientScreens.open(payload.hand);
    }
}
