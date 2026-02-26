
package net.xxxjk.TYPE_MOON_WORLD.network;

import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;

@SuppressWarnings("null")
public record MysticEyesToggleMessage(int eventType) implements CustomPacketPayload {
    public static final Type<MysticEyesToggleMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mystic_eyes_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MysticEyesToggleMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        MysticEyesToggleMessage::eventType,
        MysticEyesToggleMessage::new
    );

    @Override
    public Type<MysticEyesToggleMessage> type() {
        return TYPE;
    }

    public static void handleData(final MysticEyesToggleMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                pressAction(context.player(), message.eventType);
            }).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    public static void pressAction(Player player, int type) {
        TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        
        // Check if player has the Mystic Eyes item in the inventory
        if (vars.mysticEyesInventory.getSlots() > 0) {
            if (vars.mysticEyesInventory.getStackInSlot(0).getItem() instanceof MysticEyesItem) {
                // Toggle
                vars.is_mystic_eyes_active = !vars.is_mystic_eyes_active;
                
                // Optional: Send message to player
                if (vars.is_mystic_eyes_active) {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.activated"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.deactivated"), true);
                }
                
                // Sync
                vars.syncPlayerVariables(player);
            } else {
                // If item is not present, ensure it is off
                if (vars.is_mystic_eyes_active) {
                    vars.is_mystic_eyes_active = false;
                    vars.syncPlayerVariables(player);
                }
                player.displayClientMessage(Component.translatable("message.typemoonworld.mystic_eyes.not_equipped"), true);
            }
        }
    }
}
