package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.projection.MagicStructuralAnalysis;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record SaveStructuralSelectionMessage(
        String structureName,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) implements CustomPacketPayload {
    public static final Type<SaveStructuralSelectionMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "save_structural_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveStructuralSelectionMessage> STREAM_CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeUtf(message.structureName, 64);
                buffer.writeInt(message.minX);
                buffer.writeInt(message.minY);
                buffer.writeInt(message.minZ);
                buffer.writeInt(message.maxX);
                buffer.writeInt(message.maxY);
                buffer.writeInt(message.maxZ);
            },
            buffer -> new SaveStructuralSelectionMessage(
                    buffer.readUtf(64),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt()
            )
    );

    @Override
    public @NotNull Type<SaveStructuralSelectionMessage> type() {
        return TYPE;
    }

    public static void handleData(final SaveStructuralSelectionMessage message, final IPayloadContext context) {
        if (context.flow() != PacketFlow.SERVERBOUND) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);

            String requestedName = message.structureName == null ? "" : message.structureName.trim();
            if (requestedName.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structure.save_name_empty"), true);
                return;
            }
            if (requestedName.length() > 32) {
                requestedName = requestedName.substring(0, 32);
            }

            int minX = Math.min(message.minX, message.maxX);
            int minY = Math.min(message.minY, message.maxY);
            int minZ = Math.min(message.minZ, message.maxZ);
            int maxX = Math.max(message.minX, message.maxX);
            int maxY = Math.max(message.minY, message.maxY);
            int maxZ = Math.max(message.minZ, message.maxZ);

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;

            int maxSide = vars.player_magic_attributes_sword ? 32 : 16;
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || sizeX > maxSide || sizeY > maxSide || sizeZ > maxSide) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                return;
            }

            BlockPos min = new BlockPos(minX, minY, minZ);
            BlockPos max = new BlockPos(maxX, maxY, maxZ);

            List<TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock> blocks = new ArrayList<>();
            ItemStack iconStack = ItemStack.EMPTY;
            double totalCost = 0.0;

            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = player.serverLevel().getBlockState(pos);
                        if (state.isAir()) {
                            continue;
                        }

                        Item blockItem = state.getBlock().asItem();
                        if (blockItem == Items.AIR) {
                            continue;
                        }

                        ItemStack analyzedStack = blockItem.getDefaultInstance();
                        analyzedStack.setCount(1);
                        totalCost += MagicStructuralAnalysis.calculateStructureCost(analyzedStack, vars.player_magic_attributes_sword);

                        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                        String blockStateProps = serializeBlockStateProperties(state);
                        blocks.add(new TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock(
                                x - min.getX(),
                                y - min.getY(),
                                z - min.getZ(),
                                blockId,
                                blockStateProps
                        ));

                        if (iconStack.isEmpty()) {
                            iconStack = analyzedStack.copy();
                        }
                    }
                }
            }

            if (blocks.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.no_target"), true);
                return;
            }

            if (blocks.size() > TypeMoonWorldModVariables.PlayerVariables.MAX_BLOCKS_PER_STRUCTURE) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                return;
            }

            int replacedIndex = -1;
            int replacedBlockCount = 0;
            int existingTotalBlocks = 0;
            for (int i = 0; i < vars.analyzed_structures.size(); i++) {
                TypeMoonWorldModVariables.PlayerVariables.SavedStructure existing = vars.analyzed_structures.get(i);
                if (existing == null) continue;
                int existingBlocks = existing.blocks == null ? 0 : existing.blocks.size();
                existingTotalBlocks += existingBlocks;
                if (existing.name != null && existing.name.toLowerCase(Locale.ROOT).equals(requestedName.toLowerCase(Locale.ROOT))) {
                    replacedIndex = i;
                    replacedBlockCount = existingBlocks;
                }
            }

            int predictedStructureCount = vars.analyzed_structures.size() + (replacedIndex == -1 ? 1 : 0);
            int predictedTotalBlocks = existingTotalBlocks - replacedBlockCount + blocks.size();
            if (predictedStructureCount > TypeMoonWorldModVariables.PlayerVariables.MAX_STRUCTURES
                    || predictedTotalBlocks > TypeMoonWorldModVariables.PlayerVariables.MAX_TOTAL_STRUCTURE_BLOCKS) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structure.invalid_range"), true);
                return;
            }

            if (!MagicStructuralAnalysis.consumeAnalysisManaOrFail(player, vars, totalCost)) {
                player.displayClientMessage(Component.translatable("message.typemoonworld.structural_analysis.failed"), true);
                return;
            }

            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = new TypeMoonWorldModVariables.PlayerVariables.SavedStructure();
            structure.id = UUID.randomUUID().toString();
            structure.name = requestedName;
            structure.source = TypeMoonWorldModVariables.PlayerVariables.TRUSTED_STRUCTURE_SOURCE;
            structure.sizeX = sizeX;
            structure.sizeY = sizeY;
            structure.sizeZ = sizeZ;
            structure.totalBlocks = blocks.size();
            structure.blocks.addAll(blocks);
            structure.icon = iconStack.isEmpty() ? new ItemStack(Items.STONE) : iconStack;

            if (replacedIndex >= 0) {
                vars.analyzed_structures.set(replacedIndex, structure);
            } else {
                vars.analyzed_structures.add(structure);
            }

            vars.projection_selected_structure_id = structure.id;

            double progressGain = Math.max(0.2, Math.min(2.0, blocks.size() / 256.0));
            vars.proficiency_structural_analysis = Math.min(100.0, vars.proficiency_structural_analysis + progressGain);
            vars.syncPlayerVariables(player);

            int roundedCost = (int) Math.ceil(totalCost);
            player.displayClientMessage(
                    Component.translatable("message.typemoonworld.structure.saved", requestedName, roundedCost, 0),
                    true
            );
        }).exceptionally(e -> {
            context.connection().disconnect(Component.literal(e.getMessage()));
            return null;
        });
    }

    private static String serializeBlockStateProperties(BlockState state) {
        if (state == null) {
            return "";
        }

        List<Property<?>> properties = new ArrayList<>(state.getProperties());
        properties.sort(java.util.Comparator.comparing(Property::getName));
        if (properties.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Property<?> property : properties) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(property.getName()).append('=').append(getPropertyValueName(state, property));
        }
        return sb.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String getPropertyValueName(BlockState state, Property<?> property) {
        return ((Property) property).getName(state.getValue((Property) property));
    }
}
