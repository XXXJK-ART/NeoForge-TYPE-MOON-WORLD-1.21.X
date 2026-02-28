package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification;
import java.util.Set;

public record SelectMagicMessage(String magicId, boolean add) implements CustomPacketPayload {
    private static final int MAX_MAGIC_ID_LENGTH = 64;
    private static final Set<String> ALLOWED_MAGIC_IDS = MagicClassification.getAllMagicIds();

    public static final Type<SelectMagicMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD.MOD_ID, "select_magic"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectMagicMessage> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buffer, SelectMagicMessage message) -> {
                buffer.writeUtf(message.magicId);
                buffer.writeBoolean(message.add);
            },
            (RegistryFriendlyByteBuf buffer) -> new SelectMagicMessage(buffer.readUtf(MAX_MAGIC_ID_LENGTH), buffer.readBoolean())
    );

    @Override
    public Type<SelectMagicMessage> type() {
        return TYPE;
    }

    public static void handleData(final SelectMagicMessage message, final IPayloadContext context) {
        if (context.flow() == PacketFlow.SERVERBOUND) {
            context.enqueueWork(() -> {
                net.minecraft.world.entity.player.Player player = context.player();
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

                if (!isValidMagicId(message.magicId)) {
                    return;
                }

                boolean isLearned = vars.learned_magics.contains(message.magicId);
                if (!isLearned && "reinforcement".equals(message.magicId)) {
                    isLearned = vars.learned_magics.contains("reinforcement_self")
                            || vars.learned_magics.contains("reinforcement_other")
                            || vars.learned_magics.contains("reinforcement_item");
                }

                if (message.add) {
                    if (isLearned && !vars.selected_magics.contains(message.magicId) && vars.selected_magics.size() < 20) {
                        vars.selected_magics.add(message.magicId);
                    }
                } else {
                    vars.selected_magics.remove(message.magicId);
                    if (vars.current_magic_index >= vars.selected_magics.size()) {
                        vars.current_magic_index = Math.max(0, vars.selected_magics.size() - 1);
                    }
                }
                vars.syncPlayerVariables(player);
            }).exceptionally(e -> {
                context.connection().disconnect(net.minecraft.network.chat.Component.literal(e.getMessage()));
                return null;
            });
        }
    }

    private static boolean isValidMagicId(String magicId) {
        return magicId != null
                && !magicId.isEmpty()
                && magicId.length() <= MAX_MAGIC_ID_LENGTH
                && magicId.matches("[a-z0-9_]+")
                && ALLOWED_MAGIC_IDS.contains(magicId);
    }
}
