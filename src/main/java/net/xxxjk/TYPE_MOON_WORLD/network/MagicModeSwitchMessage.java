package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.constants.MagicConstants;
import net.minecraft.network.chat.Component;

public record MagicModeSwitchMessage(boolean forward) implements CustomPacketPayload {
    public static final Type<MagicModeSwitchMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "magic_mode_switch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagicModeSwitchMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, MagicModeSwitchMessage message) -> {
                buffer.writeBoolean(message.forward);
            },
            (RegistryFriendlyByteBuf buffer) -> new MagicModeSwitchMessage(buffer.readBoolean())
    );

    @Override
    public Type<MagicModeSwitchMessage> type() {
        return TYPE;
    }

    public static void handleData(final MagicModeSwitchMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                
                if (!vars.selected_magics.isEmpty() && vars.current_magic_index >= 0 && vars.current_magic_index < vars.selected_magics.size()) {
                    String currentMagic = vars.selected_magics.get(vars.current_magic_index);
                    
                    if ("sword_barrel_full_open".equals(currentMagic)) {
                        // Switch between 5 modes (0, 1, 2, 3, 4)
                        int maxModes = 5;
                        
                        if (message.forward) {
                            vars.sword_barrel_mode = (vars.sword_barrel_mode + 1) % maxModes;
                        } else {
                            vars.sword_barrel_mode = (vars.sword_barrel_mode - 1 + maxModes) % maxModes;
                        }
                        
                        // Send feedback
                        String modeStr = String.valueOf(vars.sword_barrel_mode);
                        if (vars.sword_barrel_mode == 0) modeStr = "0 (AOE)";
                        else if (vars.sword_barrel_mode == 1) modeStr = "1 (Aiming)";
                        else if (vars.sword_barrel_mode == 2) modeStr = "2 (Focus)";
                        else if (vars.sword_barrel_mode == 3) modeStr = "3 (Broken Phantasm)";
                        else if (vars.sword_barrel_mode == 4) modeStr = "4 (Clear)";

                        player.displayClientMessage(Component.translatable(MagicConstants.MSG_MAGIC_SWORD_BARREL_MODE_CHANGE, modeStr), true);
                        vars.syncPlayerVariables(player);
                    }
                }
            }).exceptionally(e -> {
                context.connection().disconnect(Component.literal(e.getMessage()));
                return null;
            });
        }
    }
}
