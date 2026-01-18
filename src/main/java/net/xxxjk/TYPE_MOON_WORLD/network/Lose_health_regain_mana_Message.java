package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.xxxjk.TYPE_MOON_WORLD.procedures.Manually_deduct_health_to_restore_mana;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

public record Lose_health_regain_mana_Message(int eventType, int pressed) implements CustomPacketPayload {
    public static final Type<Lose_health_regain_mana_Message> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID,
            "key_losehealthregainmana"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Lose_health_regain_mana_Message> STREAM_CODEC
            = StreamCodec.of((RegistryFriendlyByteBuf buffer, Lose_health_regain_mana_Message message) -> {
        buffer.writeInt(message.eventType);
        buffer.writeInt(message.pressed);
    }, (RegistryFriendlyByteBuf buffer)
            -> new Lose_health_regain_mana_Message(buffer.readInt(), buffer.readInt()));

    @Override
    public @NotNull Type<Lose_health_regain_mana_Message> type() {
        return TYPE;
    }

    public static void handleData(final Lose_health_regain_mana_Message message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() ->
                    pressAction(context.player(), message.eventType, message.pressed)).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void pressAction(Player entity, int type, int pressed) {
        Level world = entity.level();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        // security measure to prevent arbitrary chunk generation
        if (world.isLoaded(entity.blockPosition())) if (type == 0) {
            Manually_deduct_health_to_restore_mana.execute(entity);
        }
    }
}
