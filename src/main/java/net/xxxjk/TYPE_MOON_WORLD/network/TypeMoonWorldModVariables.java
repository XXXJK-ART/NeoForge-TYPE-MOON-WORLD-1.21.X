package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;

import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

@SuppressWarnings("null")
public class TypeMoonWorldModVariables {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TYPE_MOON_WORLD.MOD_ID);
    public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables",
            () -> AttachmentType.serializable(PlayerVariables::new).build());
    
    public static final Supplier<AttachmentType<UBWReturnData>> UBW_RETURN_DATA = ATTACHMENT_TYPES.register("ubw_return_data",
            () -> AttachmentType.serializable(UBWReturnData::new).build());
    
    public static final Supplier<AttachmentType<ReinforcementData>> REINFORCEMENT_DATA = ATTACHMENT_TYPES.register("reinforcement_data",
            () -> AttachmentType.serializable(ReinforcementData::new).build());

    @EventBusSubscriber
    public static class EventBusVariableHandlers {
        @SubscribeEvent
        public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player)
                player.getData(PLAYER_VARIABLES).syncPlayerVariables(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerPickupItem(ItemEntityPickupEvent.Post event) {
            if (event.getPlayer() instanceof ServerPlayer player) {
                PlayerVariables vars = player.getData(PLAYER_VARIABLES);
                if (!vars.is_magus) {
                    ItemStack stack = event.getOriginalStack();
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    if (id != null && TYPE_MOON_WORLD.MOD_ID.equals(id.getNamespace())) {
                        player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.hint"), true);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void clonePlayer(PlayerEvent.Clone event) {
            PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
            PlayerVariables clone = new PlayerVariables();
            clone.player_max_mana = original.player_max_mana;
            clone.player_mana_egenerated_every_moment = original.player_mana_egenerated_every_moment;
            clone.player_restore_magic_moment = original.player_restore_magic_moment;
            clone.current_mana_regen_multiplier = original.current_mana_regen_multiplier;
            clone.player_magic_attributes_earth = original.player_magic_attributes_earth;
            clone.player_magic_attributes_water = original.player_magic_attributes_water;
            clone.player_magic_attributes_fire = original.player_magic_attributes_fire;
            clone.player_magic_attributes_wind = original.player_magic_attributes_wind;
            clone.player_magic_attributes_ether = original.player_magic_attributes_ether;
            clone.player_magic_attributes_none = original.player_magic_attributes_none;
            clone.player_magic_attributes_imaginary_number = original.player_magic_attributes_imaginary_number;
            clone.player_magic_attributes_sword = original.player_magic_attributes_sword;
            clone.is_magic_circuit_open = original.is_magic_circuit_open;
            clone.magic_circuit_open_timer = original.magic_circuit_open_timer;
            clone.selected_magics = new java.util.ArrayList<>(original.selected_magics);
            clone.current_magic_index = original.current_magic_index;
            clone.magic_cooldown = original.magic_cooldown;
            
            clone.reinforcement_level = original.reinforcement_level;
            
            clone.proficiency_structural_analysis = original.proficiency_structural_analysis;
            clone.proficiency_projection = original.proficiency_projection;
            clone.proficiency_reinforcement = original.proficiency_reinforcement;
            clone.proficiency_jewel_magic_shoot = original.proficiency_jewel_magic_shoot;
            clone.proficiency_jewel_magic_release = original.proficiency_jewel_magic_release;
            clone.proficiency_unlimited_blade_works = original.proficiency_unlimited_blade_works;
            clone.proficiency_sword_barrel_full_open = original.proficiency_sword_barrel_full_open;
            
            clone.has_unlimited_blade_works = original.has_unlimited_blade_works;
            clone.is_magus = original.is_magus;

            if (!event.isWasDeath()) {
                clone.is_chanting_ubw = original.is_chanting_ubw;
                clone.ubw_chant_progress = original.ubw_chant_progress;
                clone.ubw_chant_timer = original.ubw_chant_timer;
                
                clone.is_in_ubw = original.is_in_ubw;
                clone.ubw_return_x = original.ubw_return_x;
                clone.ubw_return_y = original.ubw_return_y;
                clone.ubw_return_z = original.ubw_return_z;
                clone.ubw_return_dimension = original.ubw_return_dimension;
                
                clone.sword_barrel_mode = original.sword_barrel_mode;
            clone.jewel_magic_mode = original.jewel_magic_mode;
            clone.is_sword_barrel_active = false; // Reset on death
            } else {
                // Reset UBW states on death
                clone.is_chanting_ubw = false;
                clone.ubw_chant_progress = 0;
                clone.ubw_chant_timer = 0;
                
                clone.is_in_ubw = false;
                // Return coordinates reset to defaults or keep them? 
                // Better to reset to prevent weird teleports if logic triggers
                clone.ubw_return_x = 0;
                clone.ubw_return_y = 0;
                clone.ubw_return_z = 0;
                clone.ubw_return_dimension = "minecraft:overworld";
            }
            
            clone.learned_magics = new java.util.ArrayList<>(original.learned_magics);

            // Clone Projection Data
            clone.analyzed_items = new java.util.ArrayList<>();
            for (ItemStack stack : original.analyzed_items) {
                clone.analyzed_items.add(stack.copy());
            }
            clone.projection_selected_item = original.projection_selected_item.copy();
            clone.analyzed_structures = new java.util.ArrayList<>();
            for (PlayerVariables.SavedStructure structure : original.analyzed_structures) {
                clone.analyzed_structures.add(structure.copy());
            }
            clone.projection_selected_structure_id = original.projection_selected_structure_id;

            // Clone Mystic Eyes Inventory
            for (int i = 0; i < original.mysticEyesInventory.getSlots(); i++) {
                clone.mysticEyesInventory.setStackInSlot(i, original.mysticEyesInventory.getStackInSlot(i).copy());
            }

            // Always sync mana if not death, or if we want to keep some on death?
            // Current request: Fix "unable to project analyzed items after death"
            // This implies analyzed_items were lost.
            // The code above ALREADY copies analyzed_items.
            // Wait, does `original` have the data?
            // If `event.isWasDeath()` is true, NeoForge might have already cleared capabilities or something?
            // No, `getOriginal()` should have the data.
            
            // However, let's make sure we are copying EVERYTHING correctly.
            
            if (!event.isWasDeath()) {
                clone.player_mana = original.player_mana;
            } else {
                // On Death, maybe keep some mana or reset to 0? Default is 0.
            }
            event.getEntity().setData(PLAYER_VARIABLES, clone);
        }
    }

    public static class PlayerVariables implements INBTSerializable<CompoundTag> {
        public static final String TRUSTED_STRUCTURE_SOURCE = "typemoonworld:analysis_v2";
        public static final int MAX_BLOCKS_PER_STRUCTURE = 8192;
        public static final int MAX_TOTAL_STRUCTURE_BLOCKS = 10000;
        public static final int MAX_STRUCTURES = 48;

        public double player_mana = 0;
        public double player_max_mana = 0;
        public double player_mana_egenerated_every_moment = 0;
        public double player_restore_magic_moment = 0;
        public double current_mana_regen_multiplier = 1.0;
        public boolean player_magic_attributes_earth = false;
        public boolean player_magic_attributes_water = false;
        public boolean player_magic_attributes_fire = false;
        public boolean player_magic_attributes_wind = false;
        public boolean player_magic_attributes_ether = false;
        public boolean player_magic_attributes_none = false;
        public boolean player_magic_attributes_imaginary_number = false;
        public boolean player_magic_attributes_sword = false;
        public boolean is_magic_circuit_open = false;
        public double magic_circuit_open_timer = 0;
        public java.util.List<String> selected_magics = new java.util.ArrayList<>();
        public int current_magic_index = 0;
        public double magic_cooldown = 0;

        public int reinforcement_level = 1;
        
        // Proficiency Data
        public double proficiency_structural_analysis = 0;
        public double proficiency_projection = 0;
        public double proficiency_reinforcement = 0;
        public double proficiency_jewel_magic_shoot = 0;
        public double proficiency_jewel_magic_release = 0;
        public double proficiency_unlimited_blade_works = 0;
        public double proficiency_sword_barrel_full_open = 0;
        
        // Projection Magic Data
        public java.util.List<ItemStack> analyzed_items = new java.util.ArrayList<>();
        public ItemStack projection_selected_item = ItemStack.EMPTY;
        public java.util.List<SavedStructure> analyzed_structures = new java.util.ArrayList<>();
        public String projection_selected_structure_id = "";
        public ItemStackHandler mysticEyesInventory = new ItemStackHandler(1);
        public boolean is_mystic_eyes_active = false;
        
        // Learned Magics
        public java.util.List<String> learned_magics = new java.util.ArrayList<>();
        
        // UBW Chant Data
        public boolean is_chanting_ubw = false;
        public int ubw_chant_progress = 0; // 0 to 8 lines (or similar steps)
        public int ubw_chant_timer = 0;
        
        // UBW State Data
        public boolean has_unlimited_blade_works = false;
        public boolean is_magus = false; // New variable for unlocking logic
        public boolean is_in_ubw = false;
        public double ubw_return_x = 0;
        public double ubw_return_y = 0;
        public double ubw_return_z = 0;
        public String ubw_return_dimension = "minecraft:overworld";

        // Magic Modes
        public int sword_barrel_mode = 0; // 0: Default, 1: Mode 2, etc.
        public int jewel_magic_mode = 0; // 0: Ruby, 1: Sapphire, 2: Emerald, 3: Topaz
        public int reinforcement_mode = 0; // 0: Body, 1: Hand, 2: Leg, 3: Eye
        public int reinforcement_target = 0; // 0: Self, 1: Other, 2: Item
        public boolean is_sword_barrel_active = false; // Toggle state for continuous fire
        public boolean ubw_broken_phantasm_enabled = false; // Toggle state for Broken Phantasm
        public int merlin_favor = 0;
        public int merlin_talk_counter = 0;

        public static class SavedStructureBlock {
            public int x;
            public int y;
            public int z;
            public String blockId = "minecraft:air";
            public String blockStateProps = "";

            public SavedStructureBlock() {
            }

            public SavedStructureBlock(int x, int y, int z, String blockId) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.blockId = blockId;
            }

            public SavedStructureBlock(int x, int y, int z, String blockId, String blockStateProps) {
                this.x = x;
                this.y = y;
                this.z = z;
                this.blockId = blockId;
                this.blockStateProps = blockStateProps == null ? "" : blockStateProps;
            }

            public SavedStructureBlock copy() {
                return new SavedStructureBlock(x, y, z, blockId, blockStateProps);
            }

            public CompoundTag serializeNBT() {
                CompoundTag tag = new CompoundTag();
                tag.putInt("x", x);
                tag.putInt("y", y);
                tag.putInt("z", z);
                tag.putString("block_id", blockId);
                if (blockStateProps != null && !blockStateProps.isEmpty()) {
                    tag.putString("block_state_props", blockStateProps);
                }
                return tag;
            }

            public static SavedStructureBlock fromNBT(CompoundTag tag) {
                SavedStructureBlock block = new SavedStructureBlock();
                block.x = tag.getInt("x");
                block.y = tag.getInt("y");
                block.z = tag.getInt("z");
                if (tag.contains("block_id")) {
                    block.blockId = tag.getString("block_id");
                }
                if (tag.contains("block_state_props")) {
                    block.blockStateProps = tag.getString("block_state_props");
                } else {
                    block.blockStateProps = "";
                }
                return block;
            }
        }

        public static class SavedStructure {
            public String id = "";
            public String name = "";
            public String source = "";
            public int sizeX = 1;
            public int sizeY = 1;
            public int sizeZ = 1;
            public int totalBlocks = 0;
            public ItemStack icon = ItemStack.EMPTY;
            public java.util.List<SavedStructureBlock> blocks = new java.util.ArrayList<>();

            public SavedStructure copy() {
                SavedStructure structure = new SavedStructure();
                structure.id = id;
                structure.name = name;
                structure.source = source;
                structure.sizeX = sizeX;
                structure.sizeY = sizeY;
                structure.sizeZ = sizeZ;
                structure.totalBlocks = totalBlocks;
                structure.icon = icon.copy();
                for (SavedStructureBlock block : blocks) {
                    structure.blocks.add(block.copy());
                }
                return structure;
            }

            public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
                CompoundTag tag = new CompoundTag();
                tag.putString("id", id);
                tag.putString("name", name);
                if (source != null && !source.isEmpty()) {
                    tag.putString("source", source);
                }
                tag.putInt("size_x", sizeX);
                tag.putInt("size_y", sizeY);
                tag.putInt("size_z", sizeZ);
                tag.putInt("total_blocks", totalBlocks);
                if (!icon.isEmpty()) {
                    tag.put("icon", icon.save(lookupProvider));
                }
                net.minecraft.nbt.ListTag blockList = new net.minecraft.nbt.ListTag();
                for (SavedStructureBlock block : blocks) {
                    blockList.add(block.serializeNBT());
                }
                tag.put("blocks", blockList);
                return tag;
            }

            public static SavedStructure fromNBT(HolderLookup.Provider lookupProvider, CompoundTag tag) {
                SavedStructure structure = new SavedStructure();
                if (tag.contains("id")) structure.id = tag.getString("id");
                if (tag.contains("name")) structure.name = tag.getString("name");
                if (tag.contains("source")) structure.source = tag.getString("source");
                structure.sizeX = Math.max(1, tag.getInt("size_x"));
                structure.sizeY = Math.max(1, tag.getInt("size_y"));
                structure.sizeZ = Math.max(1, tag.getInt("size_z"));
                structure.totalBlocks = Math.max(0, tag.getInt("total_blocks"));
                if (tag.contains("icon")) {
                    java.util.Optional<ItemStack> iconStack = ItemStack.parse(lookupProvider, tag.getCompound("icon"));
                    iconStack.ifPresent(stack -> structure.icon = stack);
                }
                if (tag.contains("blocks")) {
                    net.minecraft.nbt.ListTag blockList = tag.getList("blocks", 10);
                    for (int i = 0; i < blockList.size(); i++) {
                        structure.blocks.add(SavedStructureBlock.fromNBT(blockList.getCompound(i)));
                    }
                }
                return structure;
            }
        }

        public SavedStructure getStructureById(String id) {
            if (id == null || id.isEmpty()) return null;
            for (SavedStructure structure : analyzed_structures) {
                if (id.equals(structure.id) && isTrustedStructure(structure)) {
                    return structure;
                }
            }
            return null;
        }

        public boolean removeStructureById(String id) {
            if (id == null || id.isEmpty()) return false;
            for (int i = 0; i < analyzed_structures.size(); i++) {
                if (id.equals(analyzed_structures.get(i).id)) {
                    analyzed_structures.remove(i);
                    if (id.equals(projection_selected_structure_id)) {
                        projection_selected_structure_id = "";
                    }
                    return true;
                }
            }
            return false;
        }

        public static boolean isTrustedStructure(SavedStructure structure) {
            return structure != null && TRUSTED_STRUCTURE_SOURCE.equals(structure.source);
        }

        private static int safeBlockCount(SavedStructure structure) {
            if (structure == null || structure.blocks == null) return 0;
            return Math.max(0, structure.blocks.size());
        }

        private void sanitizeAnalyzedStructures() {
            java.util.List<SavedStructure> sanitized = new java.util.ArrayList<>();
            int totalBlocks = 0;
            for (SavedStructure structure : analyzed_structures) {
                if (structure == null) continue;
                if (!isTrustedStructure(structure)) continue;
                if (structure.id == null || structure.id.isEmpty()) continue;
                if (structure.blocks == null || structure.blocks.isEmpty()) continue;

                int blockCount = safeBlockCount(structure);
                if (blockCount > MAX_BLOCKS_PER_STRUCTURE) continue;
                if (totalBlocks + blockCount > MAX_TOTAL_STRUCTURE_BLOCKS) continue;
                if (sanitized.size() >= MAX_STRUCTURES) break;

                totalBlocks += blockCount;
                sanitized.add(structure);
            }
            analyzed_structures = sanitized;

            if (!projection_selected_structure_id.isEmpty() && getStructureById(projection_selected_structure_id) == null) {
                projection_selected_structure_id = "";
            }
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider lookupProvider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putDouble("player_mana", player_mana);
            nbt.putDouble("player_max_mana", player_max_mana);
            nbt.putDouble("player_mana_egenerated_every_moment", player_mana_egenerated_every_moment);
            nbt.putDouble("player_restore_magic_moment", player_restore_magic_moment);
            nbt.putDouble("current_mana_regen_multiplier", current_mana_regen_multiplier);
            nbt.putBoolean("player_magic_attributes_earth", player_magic_attributes_earth);
            nbt.putBoolean("player_magic_attributes_water", player_magic_attributes_water);
            nbt.putBoolean("player_magic_attributes_fire", player_magic_attributes_fire);
            nbt.putBoolean("player_magic_attributes_wind", player_magic_attributes_wind);
            nbt.putBoolean("player_magic_attributes_ether", player_magic_attributes_ether);
            nbt.putBoolean("player_magic_attributes_none", player_magic_attributes_none);
            nbt.putBoolean("player_magic_attributes_imaginary_number", player_magic_attributes_imaginary_number);
            nbt.putBoolean("player_magic_attributes_sword", player_magic_attributes_sword);
            nbt.putBoolean("is_magic_circuit_open", is_magic_circuit_open);
            nbt.putDouble("magic_circuit_open_timer", magic_circuit_open_timer);
            nbt.putDouble("magic_cooldown", magic_cooldown);
            
            nbt.putDouble("proficiency_structural_analysis", proficiency_structural_analysis);
            nbt.putDouble("proficiency_projection", proficiency_projection);
            nbt.putDouble("proficiency_jewel_magic_shoot", proficiency_jewel_magic_shoot);
            nbt.putDouble("proficiency_jewel_magic_release", proficiency_jewel_magic_release);
            nbt.putDouble("proficiency_unlimited_blade_works", proficiency_unlimited_blade_works);
            nbt.putDouble("proficiency_sword_barrel_full_open", proficiency_sword_barrel_full_open);
            nbt.putDouble("proficiency_reinforcement", proficiency_reinforcement);
            
            nbt.putBoolean("is_chanting_ubw", is_chanting_ubw);
            nbt.putInt("ubw_chant_progress", ubw_chant_progress);
            nbt.putInt("ubw_chant_timer", ubw_chant_timer);

            nbt.putBoolean("has_unlimited_blade_works", has_unlimited_blade_works);
            nbt.putBoolean("is_magus", is_magus);
            nbt.putBoolean("is_in_ubw", is_in_ubw);
            nbt.putDouble("ubw_return_x", ubw_return_x);
            nbt.putDouble("ubw_return_y", ubw_return_y);
            nbt.putDouble("ubw_return_z", ubw_return_z);
            nbt.putString("ubw_return_dimension", ubw_return_dimension);
            
            nbt.putInt("sword_barrel_mode", sword_barrel_mode);
            nbt.putInt("jewel_magic_mode", jewel_magic_mode);
            nbt.putInt("reinforcement_mode", reinforcement_mode);
            nbt.putInt("reinforcement_target", reinforcement_target);
            nbt.putInt("reinforcement_level", reinforcement_level);
            nbt.putBoolean("is_sword_barrel_active", is_sword_barrel_active);
            nbt.putBoolean("ubw_broken_phantasm_enabled", ubw_broken_phantasm_enabled);
            nbt.putInt("merlin_favor", merlin_favor);
            nbt.putInt("merlin_talk_counter", merlin_talk_counter);

            net.minecraft.nbt.ListTag magicList = new net.minecraft.nbt.ListTag();
            for (String magic : selected_magics) {
                magicList.add(net.minecraft.nbt.StringTag.valueOf(magic));
            }
            nbt.put("selected_magics", magicList);
            nbt.putInt("current_magic_index", current_magic_index);
            
            // Serialize Analyzed Items
            net.minecraft.nbt.ListTag analyzedList = new net.minecraft.nbt.ListTag();
            for (ItemStack stack : analyzed_items) {
                if (!stack.isEmpty()) {
                    analyzedList.add(stack.save(lookupProvider));
                }
            }
            nbt.put("analyzed_items", analyzedList);
            
            // Serialize Learned Magics
            net.minecraft.nbt.ListTag learnedList = new net.minecraft.nbt.ListTag();
            for (String magic : learned_magics) {
                learnedList.add(net.minecraft.nbt.StringTag.valueOf(magic));
            }
            nbt.put("learned_magics", learnedList);
            
            // Serialize Selected Projection Item
            if (!projection_selected_item.isEmpty()) {
                nbt.put("projection_selected_item", projection_selected_item.save(lookupProvider));
            }
            nbt.putString("projection_selected_structure_id", projection_selected_structure_id);

            net.minecraft.nbt.ListTag structuresList = new net.minecraft.nbt.ListTag();
            for (SavedStructure structure : analyzed_structures) {
                structuresList.add(structure.serializeNBT(lookupProvider));
            }
            nbt.put("analyzed_structures", structuresList);
            
            nbt.put("mysticEyesInventory", mysticEyesInventory.serializeNBT(lookupProvider));

            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider lookupProvider, CompoundTag nbt) {
            player_mana = nbt.getDouble("player_mana");
            player_max_mana = nbt.getDouble("player_max_mana");
            player_mana_egenerated_every_moment = nbt.getDouble("player_mana_egenerated_every_moment");
            player_restore_magic_moment = nbt.getDouble("player_restore_magic_moment");
            current_mana_regen_multiplier = nbt.contains("current_mana_regen_multiplier")
                    ? nbt.getDouble("current_mana_regen_multiplier")
                    : 1.0;
            player_magic_attributes_earth = nbt.getBoolean("player_magic_attributes_earth");
            player_magic_attributes_water = nbt.getBoolean("player_magic_attributes_water");
            player_magic_attributes_fire = nbt.getBoolean("player_magic_attributes_fire");
            player_magic_attributes_wind = nbt.getBoolean("player_magic_attributes_wind");
            player_magic_attributes_ether = nbt.getBoolean("player_magic_attributes_ether");
            player_magic_attributes_none = nbt.getBoolean("player_magic_attributes_none");
            player_magic_attributes_imaginary_number = nbt.getBoolean("player_magic_attributes_imaginary_number");
            player_magic_attributes_sword = nbt.getBoolean("player_magic_attributes_sword");
            is_magic_circuit_open = nbt.getBoolean("is_magic_circuit_open");
            magic_circuit_open_timer = nbt.getDouble("magic_circuit_open_timer");
            magic_cooldown = nbt.getDouble("magic_cooldown");

            proficiency_structural_analysis = nbt.getDouble("proficiency_structural_analysis");
            proficiency_projection = nbt.getDouble("proficiency_projection");
            proficiency_jewel_magic_shoot = nbt.getDouble("proficiency_jewel_magic_shoot");
            proficiency_jewel_magic_release = nbt.getDouble("proficiency_jewel_magic_release");
            // Migrate old proficiency
            if (nbt.contains("proficiency_jewel_magic")) {
                double old = nbt.getDouble("proficiency_jewel_magic");
                if (proficiency_jewel_magic_shoot == 0) proficiency_jewel_magic_shoot = old;
                if (proficiency_jewel_magic_release == 0) proficiency_jewel_magic_release = old;
            }
            proficiency_unlimited_blade_works = nbt.getDouble("proficiency_unlimited_blade_works");
            proficiency_sword_barrel_full_open = nbt.getDouble("proficiency_sword_barrel_full_open");
            proficiency_reinforcement = nbt.getDouble("proficiency_reinforcement");
            
            is_chanting_ubw = nbt.getBoolean("is_chanting_ubw");
            ubw_chant_progress = nbt.getInt("ubw_chant_progress");
            ubw_chant_timer = nbt.getInt("ubw_chant_timer");
            
            has_unlimited_blade_works = nbt.getBoolean("has_unlimited_blade_works");
            if (nbt.contains("is_magus")) is_magus = nbt.getBoolean("is_magus");
            is_in_ubw = nbt.getBoolean("is_in_ubw");
            if (nbt.contains("ubw_return_x")) ubw_return_x = nbt.getDouble("ubw_return_x");
            if (nbt.contains("ubw_return_y")) ubw_return_y = nbt.getDouble("ubw_return_y");
            if (nbt.contains("ubw_return_z")) ubw_return_z = nbt.getDouble("ubw_return_z");
            if (nbt.contains("ubw_return_dimension")) ubw_return_dimension = nbt.getString("ubw_return_dimension");
            
            if (nbt.contains("sword_barrel_mode")) sword_barrel_mode = nbt.getInt("sword_barrel_mode");
            if (nbt.contains("jewel_magic_mode")) jewel_magic_mode = nbt.getInt("jewel_magic_mode");
            if (nbt.contains("reinforcement_mode")) reinforcement_mode = nbt.getInt("reinforcement_mode");
            if (nbt.contains("reinforcement_target")) reinforcement_target = nbt.getInt("reinforcement_target");
            if (nbt.contains("reinforcement_level")) reinforcement_level = nbt.getInt("reinforcement_level");
            if (nbt.contains("is_sword_barrel_active")) is_sword_barrel_active = nbt.getBoolean("is_sword_barrel_active");
            if (nbt.contains("ubw_broken_phantasm_enabled")) ubw_broken_phantasm_enabled = nbt.getBoolean("ubw_broken_phantasm_enabled");
            if (nbt.contains("merlin_favor")) {
                int favor = nbt.getInt("merlin_favor");
                if (favor > 5) favor = 5;
                if (favor < -5) favor = -5;
                merlin_favor = favor;
            }
            if (nbt.contains("merlin_talk_counter")) merlin_talk_counter = nbt.getInt("merlin_talk_counter");
            
            selected_magics.clear();
            if (nbt.contains("selected_magics")) {
                net.minecraft.nbt.ListTag magicList = nbt.getList("selected_magics", 8); // 8 is StringTag type
                for (int i = 0; i < magicList.size(); i++) {
                    selected_magics.add(magicList.getString(i));
                }
            }
            current_magic_index = nbt.getInt("current_magic_index");
            
            // Deserialize Analyzed Items
            analyzed_items.clear();
            if (nbt.contains("analyzed_items")) {
                net.minecraft.nbt.ListTag analyzedList = nbt.getList("analyzed_items", 10); // 10 is CompoundTag
                for (int i = 0; i < analyzedList.size(); i++) {
                    java.util.Optional<ItemStack> stack = ItemStack.parse(lookupProvider, analyzedList.getCompound(i));
                    stack.ifPresent(analyzed_items::add);
                }
            }
            
            // Deserialize Learned Magics
            learned_magics.clear();
            if (nbt.contains("learned_magics")) {
                net.minecraft.nbt.ListTag learnedList = nbt.getList("learned_magics", 8); // 8 is StringTag
                for (int i = 0; i < learnedList.size(); i++) {
                    learned_magics.add(learnedList.getString(i));
                }
            }

            // Deserialize Selected Projection Item
            projection_selected_item = ItemStack.EMPTY;
            if (nbt.contains("projection_selected_item")) {
                java.util.Optional<ItemStack> stack = ItemStack.parse(lookupProvider, nbt.getCompound("projection_selected_item"));
                stack.ifPresent(s -> projection_selected_item = s);
            }

            projection_selected_structure_id = "";
            if (nbt.contains("projection_selected_structure_id")) {
                projection_selected_structure_id = nbt.getString("projection_selected_structure_id");
            }

            analyzed_structures.clear();
            if (nbt.contains("analyzed_structures")) {
                net.minecraft.nbt.ListTag structuresList = nbt.getList("analyzed_structures", 10);
                for (int i = 0; i < structuresList.size(); i++) {
                    SavedStructure structure = SavedStructure.fromNBT(lookupProvider, structuresList.getCompound(i));
                    if (structure.id == null || structure.id.isEmpty()) {
                        structure.id = java.util.UUID.randomUUID().toString();
                    }
                    analyzed_structures.add(structure);
                }
            }

            sanitizeAnalyzedStructures();

            if (!projection_selected_structure_id.isEmpty() && getStructureById(projection_selected_structure_id) == null) {
                projection_selected_structure_id = "";
            }
            
            if (nbt.contains("mysticEyesInventory")) {
                mysticEyesInventory.deserializeNBT(lookupProvider, nbt.getCompound("mysticEyesInventory"));
            }
        }

        public void syncPlayerVariables(Entity entity) {
            sanitizeAnalyzedStructures();
            // Auto-unlock Unlimited Blade Works if player has Sword Attribute
            if (this.player_magic_attributes_sword) {
                if (!this.learned_magics.contains("unlimited_blade_works")) {
                    this.learned_magics.add("unlimited_blade_works");
                    this.has_unlimited_blade_works = true;
                    if (entity instanceof net.minecraft.world.entity.player.Player player && !player.level().isClientSide()) {
                         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.typemoonworld.unlimited_blade_works.awakened"), false);
                    }
                } else if (!this.has_unlimited_blade_works) {
                    // Ensure boolean is synced if already in list
                    this.has_unlimited_blade_works = true;
                }
            }

            // Migrate Jewel Magics
            boolean knowsOldJewelMagic = false;
            String[] oldJewels = {"ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw", 
                                  "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement"};
            for (String old : oldJewels) {
                if (this.learned_magics.contains(old)) {
                    knowsOldJewelMagic = true;
                    break;
                }
            }
            
            if (knowsOldJewelMagic) {
                if (!this.learned_magics.contains("jewel_magic_shoot")) {
                    this.learned_magics.add("jewel_magic_shoot");
                }
                if (!this.learned_magics.contains("jewel_magic_release")) {
                    this.learned_magics.add("jewel_magic_release");
                }
            }

            // Reinforcement compatibility:
            // Legacy/fragment data may only contain reinforcement_* sub skills.
            // Ensure the selectable base entry "reinforcement" is always present.
            boolean knowsReinforcementSubSkill =
                    this.learned_magics.contains("reinforcement_self")
                            || this.learned_magics.contains("reinforcement_other")
                            || this.learned_magics.contains("reinforcement_item");
            if (knowsReinforcementSubSkill && !this.learned_magics.contains("reinforcement")) {
                this.learned_magics.add("reinforcement");
            }

            if (entity instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new PlayerVariablesSyncMessage(this));
        }

        public void syncMana(Entity entity) {
            if (entity instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new ManaSyncMessage(this));
        }

        public void syncProficiency(Entity entity) {
            if (entity instanceof ServerPlayer serverPlayer)
                PacketDistributor.sendToPlayer(serverPlayer, new ProficiencySyncMessage(this));
        }
    }

    public record PlayerVariablesSyncMessage(PlayerVariables data) implements CustomPacketPayload {
        public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "player_variables_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec
                .of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess())), (RegistryFriendlyByteBuf buffer) -> {
                    PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables());
                    message.data.deserializeNBT(buffer.registryAccess(), Objects.requireNonNull(buffer.readNbt()));
                    return message;
                });

        @Override
        public @NotNull Type<PlayerVariablesSyncMessage> type() {
            return TYPE;
        }

        public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
            if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
                context.enqueueWork(() -> context.player().getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()))).exceptionally(e -> {
                    context.connection().disconnect(Component.literal(e.getMessage()));
                    return null;
                });
            }
        }
    }

    public record ManaSyncMessage(
            double player_mana,
            double player_max_mana,
            double magic_cooldown,
            double magic_circuit_open_timer,
            boolean is_magic_circuit_open,
            double current_mana_regen_multiplier
    ) implements CustomPacketPayload {
        public static final Type<ManaSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mana_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncMessage> STREAM_CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, ManaSyncMessage message) -> {
                    buffer.writeDouble(message.player_mana);
                    buffer.writeDouble(message.player_max_mana);
                    buffer.writeDouble(message.magic_cooldown);
                    buffer.writeDouble(message.magic_circuit_open_timer);
                    buffer.writeBoolean(message.is_magic_circuit_open);
                    buffer.writeDouble(message.current_mana_regen_multiplier);
                },
                (RegistryFriendlyByteBuf buffer) -> new ManaSyncMessage(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readBoolean(),
                        buffer.readDouble()
                )
        );

        public ManaSyncMessage(PlayerVariables vars) {
            this(
                    vars.player_mana,
                    vars.player_max_mana,
                    vars.magic_cooldown,
                    vars.magic_circuit_open_timer,
                    vars.is_magic_circuit_open,
                    vars.current_mana_regen_multiplier
            );
        }

        @Override
        public @NotNull Type<ManaSyncMessage> type() {
            return TYPE;
        }

        public static void handleData(final ManaSyncMessage message, final IPayloadContext context) {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                context.enqueueWork(() -> {
                    PlayerVariables vars = context.player().getData(PLAYER_VARIABLES);
                    vars.player_mana = message.player_mana;
                    vars.player_max_mana = message.player_max_mana;
                    vars.magic_cooldown = message.magic_cooldown;
                    vars.magic_circuit_open_timer = message.magic_circuit_open_timer;
                    vars.is_magic_circuit_open = message.is_magic_circuit_open;
                    vars.current_mana_regen_multiplier = message.current_mana_regen_multiplier;
                });
            }
        }
    }

    public record ProficiencySyncMessage(double structural_analysis, double projection, double jewel_magic_shoot, double jewel_magic_release, double unlimited_blade_works, double sword_barrel_full_open, double reinforcement) implements CustomPacketPayload {
        public static final Type<ProficiencySyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "proficiency_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProficiencySyncMessage> STREAM_CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, ProficiencySyncMessage message) -> {
                    buffer.writeDouble(message.structural_analysis);
                    buffer.writeDouble(message.projection);
                    buffer.writeDouble(message.jewel_magic_shoot);
                    buffer.writeDouble(message.jewel_magic_release);
                    buffer.writeDouble(message.unlimited_blade_works);
                    buffer.writeDouble(message.sword_barrel_full_open);
                    buffer.writeDouble(message.reinforcement);
                },
                (RegistryFriendlyByteBuf buffer) -> new ProficiencySyncMessage(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                )
        );

        public ProficiencySyncMessage(PlayerVariables vars) {
            this(vars.proficiency_structural_analysis, vars.proficiency_projection, vars.proficiency_jewel_magic_shoot, vars.proficiency_jewel_magic_release, vars.proficiency_unlimited_blade_works, vars.proficiency_sword_barrel_full_open, vars.proficiency_reinforcement);
        }

        @Override
        public @NotNull Type<ProficiencySyncMessage> type() {
            return TYPE;
        }

        public static void handleData(final ProficiencySyncMessage message, final IPayloadContext context) {
            if (context.flow() == PacketFlow.CLIENTBOUND) {
                context.enqueueWork(() -> {
                    PlayerVariables vars = context.player().getData(PLAYER_VARIABLES);
                    vars.proficiency_structural_analysis = message.structural_analysis;
                    vars.proficiency_projection = message.projection;
                    vars.proficiency_jewel_magic_shoot = message.jewel_magic_shoot;
                    vars.proficiency_jewel_magic_release = message.jewel_magic_release;
                    vars.proficiency_unlimited_blade_works = message.unlimited_blade_works;
                    vars.proficiency_sword_barrel_full_open = message.sword_barrel_full_open;
                    vars.proficiency_reinforcement = message.reinforcement;
                });
            }
        }
    }

    public static class UBWReturnData implements INBTSerializable<CompoundTag> {
        public java.util.UUID ownerUUID;
        public double returnX, returnY, returnZ;
        public String returnDim = "";
        public boolean generated = false;

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
            CompoundTag tag = new CompoundTag();
            if (ownerUUID != null) tag.putUUID("ownerUUID", ownerUUID);
            tag.putDouble("returnX", returnX);
            tag.putDouble("returnY", returnY);
            tag.putDouble("returnZ", returnZ);
            tag.putString("returnDim", returnDim);
            tag.putBoolean("generated", generated);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag tag) {
            if (tag.hasUUID("ownerUUID")) ownerUUID = tag.getUUID("ownerUUID");
            returnX = tag.getDouble("returnX");
            returnY = tag.getDouble("returnY");
            returnZ = tag.getDouble("returnZ");
            returnDim = tag.getString("returnDim");
            generated = tag.getBoolean("generated");
        }
    }

    public static class ReinforcementData implements INBTSerializable<CompoundTag> {
        public java.util.UUID casterUUID;

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
            CompoundTag tag = new CompoundTag();
            if (casterUUID != null) tag.putUUID("casterUUID", casterUUID);
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag tag) {
            if (tag.hasUUID("casterUUID")) casterUUID = tag.getUUID("casterUUID");
        }
    }
}
