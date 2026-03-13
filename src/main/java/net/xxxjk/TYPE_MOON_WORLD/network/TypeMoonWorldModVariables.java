package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicClassification;
import org.jetbrains.annotations.NotNull;

public class TypeMoonWorldModVariables {
   public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(Keys.ATTACHMENT_TYPES, "typemoonworld");
   public static final Supplier<AttachmentType<TypeMoonWorldModVariables.PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register(
      "player_variables", () -> AttachmentType.serializable(TypeMoonWorldModVariables.PlayerVariables::new).build()
   );
   public static final Supplier<AttachmentType<TypeMoonWorldModVariables.UBWReturnData>> UBW_RETURN_DATA = ATTACHMENT_TYPES.register(
      "ubw_return_data", () -> AttachmentType.serializable(TypeMoonWorldModVariables.UBWReturnData::new).build()
   );
   public static final Supplier<AttachmentType<TypeMoonWorldModVariables.ReinforcementData>> REINFORCEMENT_DATA = ATTACHMENT_TYPES.register(
      "reinforcement_data", () -> AttachmentType.serializable(TypeMoonWorldModVariables.ReinforcementData::new).build()
   );

   @EventBusSubscriber
   public static class EventBusVariableHandlers {
      @SubscribeEvent
      public static void onPlayerLoggedInSyncPlayerVariables(PlayerLoggedInEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
            ((TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void onPlayerRespawnedSyncPlayerVariables(PlayerRespawnEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
            ((TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerChangedDimensionEvent event) {
         if (event.getEntity() instanceof ServerPlayer player) {
            ((TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES)).syncPlayerVariables(event.getEntity());
         }
      }

      @SubscribeEvent
      public static void onPlayerPickupItem(Post event) {
         if (event.getPlayer() instanceof ServerPlayer player) {
            TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(
               TypeMoonWorldModVariables.PLAYER_VARIABLES
            );
            if (!vars.is_magus) {
               ItemStack stack = event.getOriginalStack();
               ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
               if (id != null && "typemoonworld".equals(id.getNamespace())) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.hint"), true);
               }
            }
         }
      }

      @SubscribeEvent
      public static void clonePlayer(Clone event) {
         TypeMoonWorldModVariables.PlayerVariables original = (TypeMoonWorldModVariables.PlayerVariables)event.getOriginal()
            .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         TypeMoonWorldModVariables.PlayerVariables clone = new TypeMoonWorldModVariables.PlayerVariables();
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
         clone.selected_magics = new ArrayList<>(original.selected_magics);
         clone.current_magic_index = original.current_magic_index;
         clone.selected_magic_runtime_slot_indices = new ArrayList<>(original.selected_magic_runtime_slot_indices);
         clone.selected_magic_display_names = new ArrayList<>(original.selected_magic_display_names);
         clone.active_wheel_index = original.active_wheel_index;
         clone.magic_system_data_version = original.magic_system_data_version;
         clone.magic_wheels = new ArrayList<>();

         for (TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slotEntry : original.magic_wheels) {
            if (slotEntry != null) {
               clone.magic_wheels.add(slotEntry.copy());
            }
         }

         clone.magic_cooldown = original.magic_cooldown;
         clone.reinforcement_level = original.reinforcement_level;
         clone.proficiency_structural_analysis = original.proficiency_structural_analysis;
         clone.proficiency_projection = original.proficiency_projection;
         clone.proficiency_reinforcement = original.proficiency_reinforcement;
         clone.proficiency_jewel_magic_shoot = original.proficiency_jewel_magic_shoot;
         clone.proficiency_jewel_magic_release = original.proficiency_jewel_magic_release;
         clone.proficiency_unlimited_blade_works = original.proficiency_unlimited_blade_works;
         clone.proficiency_sword_barrel_full_open = original.proficiency_sword_barrel_full_open;
         clone.proficiency_gravity_magic = original.proficiency_gravity_magic;
         clone.proficiency_gander = original.proficiency_gander;
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
            clone.gandr_machine_gun_mode = original.gandr_machine_gun_mode;
            clone.jewel_magic_mode = original.jewel_magic_mode;
            clone.gravity_magic_target = original.gravity_magic_target;
            clone.gravity_magic_mode = original.gravity_magic_mode;
            clone.is_sword_barrel_active = false;
         } else {
            clone.is_chanting_ubw = false;
            clone.ubw_chant_progress = 0;
            clone.ubw_chant_timer = 0;
            clone.is_in_ubw = false;
            clone.ubw_return_x = 0.0;
            clone.ubw_return_y = 0.0;
            clone.ubw_return_z = 0.0;
            clone.ubw_return_dimension = "minecraft:overworld";
         }

         clone.learned_magics = new ArrayList<>(original.learned_magics);
         clone.analyzed_items = new ArrayList<>();

         for (ItemStack stack : original.analyzed_items) {
            clone.analyzed_items.add(stack.copy());
         }

         clone.projection_selected_item = original.projection_selected_item.copy();
         clone.analyzed_structures = new ArrayList<>();

         for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : original.analyzed_structures) {
            clone.analyzed_structures.add(structure.copy());
         }

         clone.projection_selected_structure_id = original.projection_selected_structure_id;

         for (int i = 0; i < original.mysticEyesInventory.getSlots(); i++) {
            clone.mysticEyesInventory.setStackInSlot(i, original.mysticEyesInventory.getStackInSlot(i).copy());
         }

         for (int i = 0; i < original.magicCrestInventory.getSlots(); i++) {
            clone.magicCrestInventory.setStackInSlot(i, original.magicCrestInventory.getStackInSlot(i).copy());
         }

         clone.crest_entries = new ArrayList<>();

         for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : original.crest_entries) {
            if (crestEntry != null) {
               clone.crest_entries.add(crestEntry.copy());
            }
         }

         clone.crest_practice_count = new HashMap<>(original.crest_practice_count);
         if (!event.isWasDeath()) {
            clone.player_mana = original.player_mana;
         }

         event.getEntity().setData(TypeMoonWorldModVariables.PLAYER_VARIABLES, clone);
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
      public static final Type<TypeMoonWorldModVariables.ManaSyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "mana_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.ManaSyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> {
            buffer.writeDouble(message.player_mana);
            buffer.writeDouble(message.player_max_mana);
            buffer.writeDouble(message.magic_cooldown);
            buffer.writeDouble(message.magic_circuit_open_timer);
            buffer.writeBoolean(message.is_magic_circuit_open);
            buffer.writeDouble(message.current_mana_regen_multiplier);
         },
         buffer -> new TypeMoonWorldModVariables.ManaSyncMessage(
            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readBoolean(), buffer.readDouble()
         )
      );

      public ManaSyncMessage(TypeMoonWorldModVariables.PlayerVariables vars) {
         this(
            vars.player_mana,
            vars.player_max_mana,
            vars.magic_cooldown,
            vars.magic_circuit_open_timer,
            vars.is_magic_circuit_open,
            vars.current_mana_regen_multiplier
         );
      }

      @NotNull
      public Type<TypeMoonWorldModVariables.ManaSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.ManaSyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(
               () -> {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                     .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  vars.player_mana = message.player_mana;
                  vars.player_max_mana = message.player_max_mana;
                  vars.magic_cooldown = message.magic_cooldown;
                  vars.magic_circuit_open_timer = message.magic_circuit_open_timer;
                  vars.is_magic_circuit_open = message.is_magic_circuit_open;
                  vars.current_mana_regen_multiplier = message.current_mana_regen_multiplier;
               }
            );
         }
      }
   }

   public record ModeStateSyncMessage(
      int sword_barrel_mode,
      boolean ubw_broken_phantasm_enabled,
      int jewel_magic_mode,
      int gravity_magic_target,
      int gravity_magic_mode,
      int reinforcement_mode,
      int reinforcement_target,
      int reinforcement_level,
      int gandr_machine_gun_mode
   ) implements CustomPacketPayload {
      public static final Type<TypeMoonWorldModVariables.ModeStateSyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "mode_state_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.ModeStateSyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> {
            buffer.writeInt(message.sword_barrel_mode);
            buffer.writeBoolean(message.ubw_broken_phantasm_enabled);
            buffer.writeInt(message.jewel_magic_mode);
            buffer.writeInt(message.gravity_magic_target);
            buffer.writeInt(message.gravity_magic_mode);
            buffer.writeInt(message.reinforcement_mode);
            buffer.writeInt(message.reinforcement_target);
            buffer.writeInt(message.reinforcement_level);
            buffer.writeInt(message.gandr_machine_gun_mode);
         },
         buffer -> new TypeMoonWorldModVariables.ModeStateSyncMessage(
            buffer.readInt(),
            buffer.readBoolean(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt(),
            buffer.readInt()
         )
      );

      public ModeStateSyncMessage(TypeMoonWorldModVariables.PlayerVariables vars) {
         this(
            vars.sword_barrel_mode,
            vars.ubw_broken_phantasm_enabled,
            vars.jewel_magic_mode,
            vars.gravity_magic_target,
            vars.gravity_magic_mode,
            vars.reinforcement_mode,
            vars.reinforcement_target,
            vars.reinforcement_level,
            vars.gandr_machine_gun_mode
         );
      }

      @NotNull
      public Type<TypeMoonWorldModVariables.ModeStateSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.ModeStateSyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(
                  () -> {
                     TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                        .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                     vars.sword_barrel_mode = Mth.clamp(message.sword_barrel_mode, 0, 4);
                     vars.ubw_broken_phantasm_enabled = message.ubw_broken_phantasm_enabled;
                     vars.jewel_magic_mode = Mth.clamp(message.jewel_magic_mode, 0, 5);
                     vars.gravity_magic_target = Mth.clamp(message.gravity_magic_target, 0, 1);
                     vars.gravity_magic_mode = Mth.clamp(message.gravity_magic_mode, -2, 2);
                     vars.reinforcement_mode = Mth.clamp(message.reinforcement_mode, 0, 3);
                     vars.reinforcement_target = Mth.clamp(message.reinforcement_target, 0, 3);
                     vars.reinforcement_level = Mth.clamp(message.reinforcement_level, 1, 5);
                     vars.gandr_machine_gun_mode = Mth.clamp(message.gandr_machine_gun_mode, 0, 1);
                  }
               )
               .exceptionally(e -> {
                  TYPE_MOON_WORLD.LOGGER.error("Failed to handle ModeStateSyncMessage", e);
                  return null;
               });
         }
      }
   }

   public static class PlayerVariables implements INBTSerializable<CompoundTag> {
      public static final String TRUSTED_STRUCTURE_SOURCE = "typemoonworld:analysis_v2";
      public static final int MAX_BLOCKS_PER_STRUCTURE = 8192;
      public static final int MAX_TOTAL_STRUCTURE_BLOCKS = 10000;
      public static final int MAX_STRUCTURES = 48;
      public static final int MAGIC_WHEEL_COUNT = 10;
      public static final int MAGIC_WHEEL_SLOT_COUNT = 12;
      private static final int MAGIC_SYSTEM_DATA_VERSION = 1;
      private static final String TAG_CREST_ENTRIES = "TypeMoonCrestEntries";
      private static final String SOURCE_TYPE_SELF = "self";
      private static final String SOURCE_TYPE_CREST = "crest";
      private static final String CREST_SOURCE_SELF = "self";
      private static final String CREST_SOURCE_PLUNDER = "plunder";
      private static final Set<String> SELF_CREST_EXCLUDED_MAGICS = Set.of("unlimited_blade_works", "sword_barrel_full_open");
      public double player_mana = 0.0;
      public double player_max_mana = 0.0;
      public double player_mana_egenerated_every_moment = 0.0;
      public double player_restore_magic_moment = 0.0;
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
      public double magic_circuit_open_timer = 0.0;
      public List<String> selected_magics = new ArrayList<>();
      public int current_magic_index = 0;
      public List<Integer> selected_magic_runtime_slot_indices = new ArrayList<>();
      public List<String> selected_magic_display_names = new ArrayList<>();
      public int active_wheel_index = 0;
      public List<TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry> magic_wheels = new ArrayList<>();
      public int magic_system_data_version = 0;
      public double magic_cooldown = 0.0;
      public int reinforcement_level = 1;
      public double proficiency_structural_analysis = 0.0;
      public double proficiency_projection = 0.0;
      public double proficiency_reinforcement = 0.0;
      public double proficiency_jewel_magic_shoot = 0.0;
      public double proficiency_jewel_magic_release = 0.0;
      public double proficiency_unlimited_blade_works = 0.0;
      public double proficiency_sword_barrel_full_open = 0.0;
      public double proficiency_gravity_magic = 0.0;
      public double proficiency_gander = 0.0;
      public List<ItemStack> analyzed_items = new ArrayList<>();
      public ItemStack projection_selected_item = ItemStack.EMPTY;
      public List<TypeMoonWorldModVariables.PlayerVariables.SavedStructure> analyzed_structures = new ArrayList<>();
      public String projection_selected_structure_id = "";
      public ItemStackHandler mysticEyesInventory = new ItemStackHandler(1);
      public ItemStackHandler magicCrestInventory = new ItemStackHandler(1);
      public List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> crest_entries = new ArrayList<>();
      public Map<String, Integer> crest_practice_count = new HashMap<>();
      public boolean is_mystic_eyes_active = false;
      public List<String> learned_magics = new ArrayList<>();
      public boolean is_chanting_ubw = false;
      public int ubw_chant_progress = 0;
      public int ubw_chant_timer = 0;
      public boolean has_unlimited_blade_works = false;
      public boolean is_magus = false;
      public boolean is_in_ubw = false;
      public double ubw_return_x = 0.0;
      public double ubw_return_y = 0.0;
      public double ubw_return_z = 0.0;
      public String ubw_return_dimension = "minecraft:overworld";
      public int sword_barrel_mode = 0;
      public int gandr_machine_gun_mode = 0;
      public int jewel_magic_mode = 0;
      public int gravity_magic_target = 0;
      public int gravity_magic_mode = 0;
      public int reinforcement_mode = 0;
      public int reinforcement_target = 0;
      public boolean is_sword_barrel_active = false;
      public boolean ubw_broken_phantasm_enabled = false;
      public int merlin_favor = 0;
      public int merlin_talk_counter = 0;
      public boolean crest_cast_context = false;

      public TypeMoonWorldModVariables.PlayerVariables.SavedStructure getStructureById(String id) {
         if (id != null && !id.isEmpty()) {
            for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : this.analyzed_structures) {
               if (id.equals(structure.id) && isTrustedStructure(structure)) {
                  return structure;
               }
            }

            return null;
         } else {
            return null;
         }
      }

      public boolean removeStructureById(String id) {
         if (id != null && !id.isEmpty()) {
            for (int i = 0; i < this.analyzed_structures.size(); i++) {
               if (id.equals(this.analyzed_structures.get(i).id)) {
                  this.analyzed_structures.remove(i);
                  if (id.equals(this.projection_selected_structure_id)) {
                     this.projection_selected_structure_id = "";
                  }

                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      }

      public static boolean isTrustedStructure(TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure) {
         return structure != null && "typemoonworld:analysis_v2".equals(structure.source);
      }

      private static int safeBlockCount(TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure) {
         return structure != null && structure.blocks != null ? Math.max(0, structure.blocks.size()) : 0;
      }

      private void sanitizeAnalyzedStructures() {
         List<TypeMoonWorldModVariables.PlayerVariables.SavedStructure> sanitized = new ArrayList<>();
         int totalBlocks = 0;

         for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : this.analyzed_structures) {
            if (structure != null
               && isTrustedStructure(structure)
               && structure.id != null
               && !structure.id.isEmpty()
               && structure.blocks != null
               && !structure.blocks.isEmpty()) {
               int blockCount = safeBlockCount(structure);
               if (blockCount <= 8192 && totalBlocks + blockCount <= 10000) {
                  if (sanitized.size() >= 48) {
                     break;
                  }

                  totalBlocks += blockCount;
                  sanitized.add(structure);
               }
            }
         }

         this.analyzed_structures = sanitized;
         if (!this.projection_selected_structure_id.isEmpty() && this.getStructureById(this.projection_selected_structure_id) == null) {
            this.projection_selected_structure_id = "";
         }
      }

      private static int wheelFlatIndex(int wheelIndex, int slotIndex) {
         return wheelIndex * 12 + slotIndex;
      }

      private static boolean isValidWheelIndex(int wheelIndex) {
         return wheelIndex >= 0 && wheelIndex < 10;
      }

      private static boolean isValidWheelSlotIndex(int slotIndex) {
         return slotIndex >= 0 && slotIndex < 12;
      }

      private static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry createEmptyWheelSlot(int wheelIndex, int slotIndex) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(wheelIndex, slotIndex);
         entry.clear();
         return entry;
      }

      private static String sanitizeSourceType(String sourceType) {
         return "crest".equals(sourceType) ? "crest" : "self";
      }

      private static String sanitizeCrestSourceKind(String sourceKind) {
         return "plunder".equals(sourceKind) ? "plunder" : "self";
      }

      private static String ensureCrestEntryId(String id) {
         return id != null && !id.isEmpty() ? id : UUID.randomUUID().toString();
      }

      private static boolean isKnownMagicId(String magicId) {
         return magicId != null && MagicClassification.isKnownMagic(magicId);
      }

      private static boolean isPresetOptionMagic(String magicId) {
         return "reinforcement".equals(magicId) || "gravity_magic".equals(magicId) || "gandr_machine_gun".equals(magicId) || "projection".equals(magicId);
      }

      private static String canonicalSelfKnowledgeMagicId(String magicId) {
         return !"reinforcement_self".equals(magicId) && !"reinforcement_other".equals(magicId) && !"reinforcement_item".equals(magicId)
            ? magicId
            : "reinforcement";
      }

      private static int crestPresetSeed(TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry) {
         return Objects.hash(
            crestEntry.entryId == null ? "" : crestEntry.entryId,
            crestEntry.magicId == null ? "" : crestEntry.magicId,
            crestEntry.originOwnerUuid == null ? "" : crestEntry.originOwnerUuid,
            crestEntry.originOwnerName == null ? "" : crestEntry.originOwnerName
         );
      }

      private static CompoundTag buildDeterministicPlunderPreset(TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry) {
         CompoundTag payload = new CompoundTag();
         int seed = crestPresetSeed(crestEntry);
         if ("reinforcement".equals(crestEntry.magicId)) {
            int[] targets = new int[]{0, 1, 2};
            int target = targets[Math.floorMod(seed, targets.length)];
            int level = 1 + Math.floorMod(seed / 7, 5);
            payload.putInt("reinforcement_target", target);
            payload.putInt("reinforcement_mode", 0);
            payload.putInt("reinforcement_level", level);
         } else if ("gravity_magic".equals(crestEntry.magicId)) {
            int target = Math.floorMod(seed, 2);
            int[] modes = new int[]{-2, -1, 1, 2};
            int mode = modes[Math.floorMod(seed / 5, modes.length)];
            payload.putInt("gravity_target", target);
            payload.putInt("gravity_mode", mode);
         } else if ("gandr_machine_gun".equals(crestEntry.magicId)) {
            payload.putInt("gandr_machine_gun_mode", Math.floorMod(seed, 2));
         } else if ("projection".equals(crestEntry.magicId)) {
            payload.putBoolean("projection_lock_empty", true);
         }

         payload.putBoolean("preset_locked", true);
         return payload;
      }

      private static CompoundTag normalizePlunderPresetPayload(TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry) {
         CompoundTag payload = crestEntry.presetPayload == null ? new CompoundTag() : crestEntry.presetPayload.copy();
         String magicId = crestEntry.magicId == null ? "" : crestEntry.magicId;
         if ("projection".equals(magicId)) {
            payload = normalizeProjectionPresetPayload(payload);
            boolean hasStructure = payload.contains("projection_structure_id") && !payload.getString("projection_structure_id").isEmpty();
            boolean hasItem = payload.contains("projection_item", 10);
            if (!hasStructure && !hasItem) {
               payload.putBoolean("projection_lock_empty", true);
            }
         } else if (isPresetOptionMagic(magicId) && payload.isEmpty()) {
            payload = buildDeterministicPlunderPreset(crestEntry);
         }

         payload.putBoolean("preset_locked", true);
         return payload;
      }

      public static CompoundTag normalizeProjectionPresetPayload(CompoundTag rawPayload) {
         CompoundTag payload = rawPayload == null ? new CompoundTag() : rawPayload.copy();
         boolean hasStructure = payload.contains("projection_structure_id") && !payload.getString("projection_structure_id").isEmpty();
         boolean hasItem = payload.contains("projection_item", 10);
         boolean lockEmpty = payload.getBoolean("projection_lock_empty");
         if (lockEmpty) {
            payload.remove("projection_structure_id");
            payload.remove("projection_item");
            payload.putBoolean("projection_lock_empty", true);
            return payload;
         } else if (hasStructure) {
            payload.remove("projection_item");
            payload.remove("projection_lock_empty");
            return payload;
         } else if (hasItem) {
            payload.remove("projection_structure_id");
            payload.remove("projection_lock_empty");
            return payload;
         } else {
            payload.remove("projection_structure_id");
            payload.remove("projection_item");
            payload.remove("projection_lock_empty");
            return payload;
         }
      }

      private static void normalizeCrestEntry(TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry) {
         if (crestEntry != null) {
            crestEntry.entryId = ensureCrestEntryId(crestEntry.entryId);
            crestEntry.sourceKind = sanitizeCrestSourceKind(crestEntry.sourceKind);
            if (crestEntry.presetPayload == null) {
               crestEntry.presetPayload = new CompoundTag();
            }

            if ("plunder".equals(crestEntry.sourceKind)) {
               crestEntry.presetPayload = normalizePlunderPresetPayload(crestEntry);
            } else {
               crestEntry.presetPayload.remove("preset_locked");
               crestEntry.presetPayload.remove("projection_lock_empty");
            }
         }
      }

      public void ensureMagicSystemInitialized() {
         if (this.magic_wheels == null) {
            this.magic_wheels = new ArrayList<>();
         }

         if (this.selected_magic_runtime_slot_indices == null) {
            this.selected_magic_runtime_slot_indices = new ArrayList<>();
         }

         if (this.selected_magic_display_names == null) {
            this.selected_magic_display_names = new ArrayList<>();
         }

         if (this.crest_entries == null) {
            this.crest_entries = new ArrayList<>();
         }

         if (this.crest_practice_count == null) {
            this.crest_practice_count = new HashMap<>();
         }

         if (this.magicCrestInventory == null) {
            this.magicCrestInventory = new ItemStackHandler(1);
         }

         int totalSlots = 120;
         if (this.magic_wheels.size() != totalSlots) {
            Map<Integer, TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry> oldEntries = new HashMap<>();

            for (TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry old : this.magic_wheels) {
               if (old != null && isValidWheelIndex(old.wheelIndex) && isValidWheelSlotIndex(old.slotIndex)) {
                  oldEntries.put(wheelFlatIndex(old.wheelIndex, old.slotIndex), old.copy());
               }
            }

            this.magic_wheels = new ArrayList<>(totalSlots);

            for (int wheel = 0; wheel < 10; wheel++) {
               for (int slot = 0; slot < 12; slot++) {
                  int flat = wheelFlatIndex(wheel, slot);
                  TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = oldEntries.getOrDefault(flat, createEmptyWheelSlot(wheel, slot));
                  entry.wheelIndex = wheel;
                  entry.slotIndex = slot;
                  if (!isKnownMagicId(entry.magicId)) {
                     entry.clear();
                  } else {
                     entry.sourceType = sanitizeSourceType(entry.sourceType);
                  }

                  this.magic_wheels.add(entry);
               }
            }
         }

         this.active_wheel_index = Mth.clamp(this.active_wheel_index, 0, 9);

         for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : this.crest_entries) {
            normalizeCrestEntry(crestEntry);
         }
      }

      public TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry getWheelSlotEntry(int wheelIndex, int slotIndex) {
         this.ensureMagicSystemInitialized();
         return isValidWheelIndex(wheelIndex) && isValidWheelSlotIndex(slotIndex) ? this.magic_wheels.get(wheelFlatIndex(wheelIndex, slotIndex)) : null;
      }

      public void setWheelSlotEntry(int wheelIndex, int slotIndex, TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry input) {
         this.ensureMagicSystemInitialized();
         if (isValidWheelIndex(wheelIndex) && isValidWheelSlotIndex(slotIndex) && input != null) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry normalized = input.copy();
            normalized.wheelIndex = wheelIndex;
            normalized.slotIndex = slotIndex;
            normalized.sourceType = sanitizeSourceType(normalized.sourceType);
            if (!isKnownMagicId(normalized.magicId)) {
               normalized.clear();
            }

            this.magic_wheels.set(wheelFlatIndex(wheelIndex, slotIndex), normalized);
         }
      }

      public void clearWheelSlotEntry(int wheelIndex, int slotIndex) {
         this.ensureMagicSystemInitialized();
         if (isValidWheelIndex(wheelIndex) && isValidWheelSlotIndex(slotIndex)) {
            this.magic_wheels.set(wheelFlatIndex(wheelIndex, slotIndex), createEmptyWheelSlot(wheelIndex, slotIndex));
         }
      }

      public void swapWheelSlots(int wheelIndex, int slotA, int slotB) {
         this.ensureMagicSystemInitialized();
         if (isValidWheelIndex(wheelIndex) && isValidWheelSlotIndex(slotA) && isValidWheelSlotIndex(slotB) && slotA != slotB) {
            int flatA = wheelFlatIndex(wheelIndex, slotA);
            int flatB = wheelFlatIndex(wheelIndex, slotB);
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry a = this.magic_wheels.get(flatA).copy();
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry b = this.magic_wheels.get(flatB).copy();
            a.slotIndex = slotB;
            b.slotIndex = slotA;
            this.magic_wheels.set(flatA, b);
            this.magic_wheels.set(flatB, a);
         }
      }

      public void clearAllWheelSlots() {
         this.ensureMagicSystemInitialized();

         for (int wheel = 0; wheel < 10; wheel++) {
            for (int slot = 0; slot < 12; slot++) {
               this.magic_wheels.set(wheelFlatIndex(wheel, slot), createEmptyWheelSlot(wheel, slot));
            }
         }
      }

      public TypeMoonWorldModVariables.PlayerVariables.CrestEntry getCrestEntryById(String entryId) {
         if (entryId != null && !entryId.isEmpty()) {
            this.ensureMagicSystemInitialized();

            for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry : this.crest_entries) {
               if (entryId.equals(entry.entryId)) {
                  return entry;
               }
            }

            return null;
         } else {
            return null;
         }
      }

      public boolean hasValidImplantedCrest() {
         this.ensureMagicSystemInitialized();
         ItemStack stack = this.magicCrestInventory.getStackInSlot(0);
         return !stack.isEmpty() && stack.getItem() instanceof MagicCrestItem;
      }

      public boolean isWheelSlotEntryCastable(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slotEntry) {
         if (slotEntry == null || slotEntry.isEmpty() || !isKnownMagicId(slotEntry.magicId)) {
            return false;
         } else if (!"crest".equals(slotEntry.sourceType)) {
            return this.learned_magics.contains(slotEntry.magicId);
         } else if (!this.hasValidImplantedCrest()) {
            return false;
         } else {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = this.getCrestEntryById(slotEntry.crestEntryId);
            if (crestEntry == null || !crestEntry.active || !slotEntry.magicId.equals(crestEntry.magicId)) {
               return false;
            } else {
               return "plunder".equals(crestEntry.sourceKind) ? true : this.learned_magics.contains(slotEntry.magicId);
            }
         }
      }

      public void switchActiveWheel(int wheelIndex) {
         this.ensureMagicSystemInitialized();
         this.active_wheel_index = Mth.clamp(wheelIndex, 0, 9);
         this.rebuildSelectedMagicsFromActiveWheel();
      }

      public void rebuildSelectedMagicsFromActiveWheel() {
         this.ensureMagicSystemInitialized();
         int preferredSlot = -1;
         if (this.current_magic_index >= 0 && this.current_magic_index < this.selected_magic_runtime_slot_indices.size()) {
            preferredSlot = this.selected_magic_runtime_slot_indices.get(this.current_magic_index);
         }

         this.selected_magics.clear();
         this.selected_magic_runtime_slot_indices.clear();
         this.selected_magic_display_names.clear();

         for (int slot = 0; slot < 12; slot++) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slotEntry = this.getWheelSlotEntry(this.active_wheel_index, slot);
            if (this.isWheelSlotEntryCastable(slotEntry)) {
               this.selected_magics.add(slotEntry.magicId);
               this.selected_magic_runtime_slot_indices.add(slot);
               if (slotEntry.displayNameCache != null && !slotEntry.displayNameCache.isEmpty()) {
                  this.selected_magic_display_names.add(slotEntry.displayNameCache);
               } else {
                  this.selected_magic_display_names.add("");
               }
            }
         }

         if (this.selected_magics.isEmpty()) {
            this.current_magic_index = 0;
         } else if (preferredSlot >= 0) {
            int exactIndex = this.selected_magic_runtime_slot_indices.indexOf(preferredSlot);
            if (exactIndex >= 0) {
               this.current_magic_index = exactIndex;
            } else {
               int fallbackIndex = -1;
               int bestDelta = Integer.MAX_VALUE;

               for (int i = 0; i < this.selected_magic_runtime_slot_indices.size(); i++) {
                  int slotx = this.selected_magic_runtime_slot_indices.get(i);
                  int delta = preferredSlot - slotx;
                  if (delta >= 0 && delta < bestDelta) {
                     bestDelta = delta;
                     fallbackIndex = i;
                  }
               }

               this.current_magic_index = fallbackIndex >= 0 ? fallbackIndex : 0;
            }
         } else {
            this.current_magic_index = Mth.clamp(this.current_magic_index, 0, this.selected_magics.size() - 1);
         }
      }

      public TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry getCurrentRuntimeWheelEntry() {
         this.ensureMagicSystemInitialized();
         if (this.current_magic_index >= 0 && this.current_magic_index < this.selected_magic_runtime_slot_indices.size()) {
            int slot = this.selected_magic_runtime_slot_indices.get(this.current_magic_index);
            return this.getWheelSlotEntry(this.active_wheel_index, slot);
         } else {
            return null;
         }
      }

      public boolean isCurrentSelectionFromCrest() {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
         return current != null && "crest".equals(current.sourceType) && this.isWheelSlotEntryCastable(current);
      }

      public boolean isCurrentSelectionFromCrest(String magicId) {
         if (magicId != null && !magicId.isEmpty()) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
            return current != null && "crest".equals(current.sourceType) && this.isWheelSlotEntryCastable(current) && Objects.equals(current.magicId, magicId);
         } else {
            return false;
         }
      }

      public boolean canCastCurrentSelectionViaCrest(String magicId) {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
         if (current != null && "crest".equals(current.sourceType) && this.isWheelSlotEntryCastable(current) && Objects.equals(current.magicId, magicId)) {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = this.getCrestEntryById(current.crestEntryId);
            return crestEntry != null && "plunder".equals(crestEntry.sourceKind);
         } else {
            return false;
         }
      }

      public CompoundTag getCurrentCrestPresetPayload() {
         TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
         return current != null && "crest".equals(current.sourceType) && current.presetPayload != null ? current.presetPayload.copy() : new CompoundTag();
      }

      public void applyCurrentCrestPreset(Entity entity) {
         if (entity != null) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
            if (current != null && "crest".equals(current.sourceType) && this.isWheelSlotEntryCastable(current)) {
               CompoundTag payload = current.presetPayload == null ? new CompoundTag() : current.presetPayload;
               if ("reinforcement".equals(current.magicId)) {
                  if (payload.contains("reinforcement_target")) {
                     this.reinforcement_target = Mth.clamp(payload.getInt("reinforcement_target"), 0, 3);
                  }

                  if (payload.contains("reinforcement_mode")) {
                     this.reinforcement_mode = Mth.clamp(payload.getInt("reinforcement_mode"), 0, 3);
                  }

                  if (payload.contains("reinforcement_level")) {
                     this.reinforcement_level = Mth.clamp(payload.getInt("reinforcement_level"), 1, 5);
                  }
               } else if ("gravity_magic".equals(current.magicId)) {
                  if (payload.contains("gravity_target")) {
                     this.gravity_magic_target = Mth.clamp(payload.getInt("gravity_target"), 0, 1);
                  }

                  if (payload.contains("gravity_mode")) {
                     this.gravity_magic_mode = Mth.clamp(payload.getInt("gravity_mode"), -2, 2);
                  }
               } else if ("projection".equals(current.magicId)) {
                  CompoundTag projectionPayload = normalizeProjectionPresetPayload(payload);
                  if (projectionPayload.getBoolean("projection_lock_empty")) {
                     this.projection_selected_structure_id = "";
                     this.projection_selected_item = ItemStack.EMPTY;
                  } else {
                     if (projectionPayload.contains("projection_structure_id")) {
                        this.projection_selected_structure_id = projectionPayload.getString("projection_structure_id");
                        this.projection_selected_item = ItemStack.EMPTY;
                     }

                     if (projectionPayload.contains("projection_item", 10)) {
                        ItemStack.parse(entity.registryAccess(), projectionPayload.getCompound("projection_item")).ifPresent(stack -> {
                           this.projection_selected_item = stack;
                           this.projection_selected_structure_id = "";
                        });
                     }
                  }
               } else {
                  if ("gandr_machine_gun".equals(current.magicId) && payload.contains("gandr_machine_gun_mode")) {
                     this.gandr_machine_gun_mode = Mth.clamp(payload.getInt("gandr_machine_gun_mode"), 0, 1);
                  }
               }
            }
         }
      }

      public static CompoundTag buildProjectionPlunderPresetPayload(Provider lookupProvider, ItemStack projectionItem, String projectionStructureId) {
         CompoundTag payload = new CompoundTag();
         if (projectionStructureId != null && !projectionStructureId.isEmpty()) {
            payload.putString("projection_structure_id", projectionStructureId);
            return payload;
         } else if (projectionItem != null && !projectionItem.isEmpty()) {
            payload.put("projection_item", projectionItem.save(lookupProvider));
            return payload;
         } else {
            payload.putBoolean("projection_lock_empty", true);
            return payload;
         }
      }

      public boolean addPlunderCrestEntry(String magicId, CompoundTag presetPayload, UUID originOwnerUuid, String originOwnerType, String originOwnerName) {
         this.ensureMagicSystemInitialized();
         if (!isKnownMagicId(magicId)) {
            return false;
         } else {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
            entry.entryId = UUID.randomUUID().toString();
            entry.magicId = magicId;
            entry.presetPayload = presetPayload == null ? new CompoundTag() : presetPayload.copy();
            entry.sourceKind = "plunder";
            entry.originOwnerUuid = originOwnerUuid == null ? "" : originOwnerUuid.toString();
            entry.originOwnerType = originOwnerType != null && !originOwnerType.isEmpty() ? originOwnerType : "npc";
            entry.originOwnerName = originOwnerName == null ? "" : originOwnerName;
            entry.active = true;
            int before = this.crest_entries == null ? 0 : this.crest_entries.size();
            List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> incoming = new ArrayList<>();
            incoming.add(entry);
            this.mergeImplantedCrestEntries(incoming);
            return this.crest_entries.size() != before;
         }
      }

      public boolean addProjectionPlunderCrestEntry(
         Provider lookupProvider, ItemStack projectionItem, String projectionStructureId, UUID originOwnerUuid, String originOwnerType, String originOwnerName
      ) {
         CompoundTag payload = buildProjectionPlunderPresetPayload(lookupProvider, projectionItem, projectionStructureId);
         return this.addPlunderCrestEntry("projection", payload, originOwnerUuid, originOwnerType, originOwnerName);
      }

      public void syncSelfCrestEntriesFromKnowledge() {
         this.ensureMagicSystemInitialized();
         Map<String, TypeMoonWorldModVariables.PlayerVariables.CrestEntry> existingSelfEntries = new LinkedHashMap<>();
         List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> updated = new ArrayList<>();

         for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry : this.crest_entries) {
            if (entry != null) {
               normalizeCrestEntry(entry);
               if ("self".equals(entry.sourceKind)) {
                  String canonicalMagicId = canonicalSelfKnowledgeMagicId(entry.magicId);
                  if (isKnownMagicId(canonicalMagicId)) {
                     TypeMoonWorldModVariables.PlayerVariables.CrestEntry normalizedCopy = entry.copy();
                     normalizedCopy.magicId = canonicalMagicId;
                     existingSelfEntries.putIfAbsent(canonicalMagicId, normalizedCopy);
                  }
               } else {
                  updated.add(entry.copy());
               }
            }
         }

         Set<String> learnedSet = new HashSet<>();

         for (String learned : this.learned_magics) {
            String canonicalLearned = canonicalSelfKnowledgeMagicId(learned);
            if (isKnownMagicId(canonicalLearned) && !SELF_CREST_EXCLUDED_MAGICS.contains(canonicalLearned)) {
               learnedSet.add(canonicalLearned);
            }
         }

         for (String magicId : learnedSet) {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry selfEntry = existingSelfEntries.get(magicId);
            if (selfEntry == null) {
               selfEntry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
               selfEntry.entryId = UUID.randomUUID().toString();
               selfEntry.magicId = magicId;
               selfEntry.presetPayload = new CompoundTag();
            }

            selfEntry.entryId = ensureCrestEntryId(selfEntry.entryId);
            selfEntry.magicId = magicId;
            selfEntry.sourceKind = "self";
            selfEntry.originOwnerType = "player";
            if (selfEntry.originOwnerName == null) {
               selfEntry.originOwnerName = "";
            }

            if (selfEntry.presetPayload == null) {
               selfEntry.presetPayload = new CompoundTag();
            }

            selfEntry.active = true;
            updated.add(selfEntry);
         }

         this.crest_entries = updated;
      }

      public void pruneInvalidCrestWheelReferences() {
         this.ensureMagicSystemInitialized();

         for (int wheel = 0; wheel < 10; wheel++) {
            for (int slot = 0; slot < 12; slot++) {
               TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = this.getWheelSlotEntry(wheel, slot);
               if (entry != null && !entry.isEmpty() && "crest".equals(entry.sourceType)) {
                  TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = this.getCrestEntryById(entry.crestEntryId);
                  if (crestEntry == null || !crestEntry.active || !Objects.equals(crestEntry.magicId, entry.magicId)) {
                     this.clearWheelSlotEntry(wheel, slot);
                  }
               }
            }
         }
      }

      public int invalidateNpcOriginCrestEntries(UUID originUuid) {
         if (originUuid == null) {
            return 0;
         } else {
            this.ensureMagicSystemInitialized();
            String target = originUuid.toString();
            Set<String> invalidatedEntryIds = new HashSet<>();
            int invalidatedCount = 0;

            for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : this.crest_entries) {
               if (crestEntry != null
                  && crestEntry.active
                  && "plunder".equals(crestEntry.sourceKind)
                  && !"player".equalsIgnoreCase(crestEntry.originOwnerType)
                  && target.equalsIgnoreCase(crestEntry.originOwnerUuid)) {
                  crestEntry.active = false;
                  invalidatedEntryIds.add(crestEntry.entryId);
                  invalidatedCount++;
               }
            }

            if (invalidatedEntryIds.isEmpty()) {
               return 0;
            } else {
               for (int wheel = 0; wheel < 10; wheel++) {
                  for (int slot = 0; slot < 12; slot++) {
                     TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = this.getWheelSlotEntry(wheel, slot);
                     if (entry != null && !entry.isEmpty() && invalidatedEntryIds.contains(entry.crestEntryId)) {
                        this.clearWheelSlotEntry(wheel, slot);
                     }
                  }
               }

               this.rebuildSelectedMagicsFromActiveWheel();
               return invalidatedCount;
            }
         }
      }

      public static List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> readCrestEntriesFromStack(ItemStack stack) {
         List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> entries = new ArrayList<>();
         if (stack.isEmpty()) {
            return entries;
         } else {
            CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
               return entries;
            } else {
               CompoundTag tag = customData.copyTag();
               if (!tag.contains("TypeMoonCrestEntries", 9)) {
                  return entries;
               } else {
                  ListTag listTag = tag.getList("TypeMoonCrestEntries", 10);

                  for (int i = 0; i < listTag.size(); i++) {
                     TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry = TypeMoonWorldModVariables.PlayerVariables.CrestEntry.fromNBT(
                        listTag.getCompound(i)
                     );
                     if (isKnownMagicId(entry.magicId)) {
                        normalizeCrestEntry(entry);
                        entries.add(entry);
                     }
                  }

                  return entries;
               }
            }
         }
      }

      public static void writeCrestEntriesToStack(ItemStack stack, List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> entries) {
         if (!stack.isEmpty()) {
            CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
            ListTag listTag = new ListTag();

            for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry : entries) {
               if (entry != null && isKnownMagicId(entry.magicId)) {
                  listTag.add(entry.serializeNBT());
               }
            }

            tag.put("TypeMoonCrestEntries", listTag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
         }
      }

      public void mergeImplantedCrestEntries(List<TypeMoonWorldModVariables.PlayerVariables.CrestEntry> incomingEntries) {
         this.ensureMagicSystemInitialized();
         if (incomingEntries != null && !incomingEntries.isEmpty()) {
            Map<String, TypeMoonWorldModVariables.PlayerVariables.CrestEntry> merged = new LinkedHashMap<>();

            for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry existing : this.crest_entries) {
               if (existing != null && isKnownMagicId(existing.magicId)) {
                  TypeMoonWorldModVariables.PlayerVariables.CrestEntry copy = existing.copy();
                  normalizeCrestEntry(copy);
                  String key = copy.magicId
                     + "|"
                     + (copy.presetPayload == null ? "" : copy.presetPayload.toString())
                     + "|"
                     + copy.sourceKind
                     + "|"
                     + copy.originOwnerUuid;
                  merged.putIfAbsent(key, copy);
               }
            }

            for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry incoming : incomingEntries) {
               if (incoming != null && isKnownMagicId(incoming.magicId)) {
                  TypeMoonWorldModVariables.PlayerVariables.CrestEntry normalized = incoming.copy();
                  normalizeCrestEntry(normalized);
                  String key = normalized.magicId
                     + "|"
                     + (normalized.presetPayload == null ? "" : normalized.presetPayload.toString())
                     + "|"
                     + normalized.sourceKind
                     + "|"
                     + normalized.originOwnerUuid;
                  merged.putIfAbsent(key, normalized);
               }
            }

            this.crest_entries = new ArrayList<>(merged.values());
         }
      }

      public boolean recordCrestCastPractice(Entity entity, String magicId) {
         if (magicId != null && !magicId.isEmpty() && !this.learned_magics.contains(magicId)) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry current = this.getCurrentRuntimeWheelEntry();
            if (current != null && "crest".equals(current.sourceType)) {
               TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = this.getCrestEntryById(current.crestEntryId);
               if (crestEntry != null && crestEntry.active && "plunder".equals(crestEntry.sourceKind)) {
                  int currentCount = this.crest_practice_count.getOrDefault(magicId, 0);
                  this.crest_practice_count.put(magicId, ++currentCount);
                  int required = this.getRequiredCrestPracticeCasts(magicId);
                  if (currentCount < required) {
                     return false;
                  } else if (!this.learned_magics.contains(magicId)) {
                     this.learned_magics.add(magicId);
                     this.crest_practice_count.remove(magicId);
                     if (entity instanceof Player player && !player.level().isClientSide()) {
                        player.displayClientMessage(
                           Component.translatable(
                              "message.typemoonworld.magic.learned", new Object[]{Component.translatable("magic.typemoonworld." + magicId + ".name")}
                           ),
                           false
                        );
                     }

                     return true;
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public int getRequiredCrestPracticeCasts(String magicId) {
         double manaCostFactor = estimateBaseManaCost(magicId) * 0.8;
         int difficultyFactor = estimateDifficultyFactor(magicId);
         int rarityFactor = estimateRarityFactor(magicId);
         int required = (int)Math.round(10.0 + manaCostFactor + difficultyFactor + rarityFactor);
         return Mth.clamp(required, 20, 300);
      }

      private static double estimateBaseManaCost(String magicId) {
         if (magicId == null) {
            return 20.0;
         } else {
            return switch (magicId) {
               case "projection" -> 70.0;
               case "structural_analysis" -> 45.0;
               case "broken_phantasm" -> 120.0;
               case "reinforcement" -> 60.0;
               case "gravity_magic" -> 55.0;
               case "gander" -> 50.0;
               case "jewel_machine_gun", "gandr_machine_gun" -> 80.0;
               case "unlimited_blade_works", "sword_barrel_full_open" -> 160.0;
               default -> {
                  MagicClassification.ManaCostType costType = MagicClassification.getManaCostType(magicId);
                  yield costType == MagicClassification.ManaCostType.SUSTAINED_OR_INDIRECT ? 55.0 : 30.0;
               }
            };
         }
      }

      private static int estimateDifficultyFactor(String magicId) {
         if (magicId == null) {
            return 0;
         } else if ("unlimited_blade_works".equals(magicId) || "sword_barrel_full_open".equals(magicId)) {
            return 70;
         } else if ("broken_phantasm".equals(magicId) || "projection".equals(magicId)) {
            return 45;
         } else {
            return !"gander".equals(magicId) && !"gandr_machine_gun".equals(magicId) && !"jewel_machine_gun".equals(magicId) ? 15 : 28;
         }
      }

      private static int estimateRarityFactor(String magicId) {
         if (magicId == null) {
            return 0;
         } else if ("unlimited_blade_works".equals(magicId) || "sword_barrel_full_open".equals(magicId)) {
            return 80;
         } else if ("broken_phantasm".equals(magicId) || "gandr_machine_gun".equals(magicId)) {
            return 35;
         } else {
            return !"projection".equals(magicId) && !"gravity_magic".equals(magicId) && !"gander".equals(magicId) ? 10 : 22;
         }
      }

      public CompoundTag serializeNBT(@NotNull Provider lookupProvider) {
         this.ensureMagicSystemInitialized();
         CompoundTag nbt = new CompoundTag();
         nbt.putDouble("player_mana", this.player_mana);
         nbt.putDouble("player_max_mana", this.player_max_mana);
         nbt.putDouble("player_mana_egenerated_every_moment", this.player_mana_egenerated_every_moment);
         nbt.putDouble("player_restore_magic_moment", this.player_restore_magic_moment);
         nbt.putDouble("current_mana_regen_multiplier", this.current_mana_regen_multiplier);
         nbt.putBoolean("player_magic_attributes_earth", this.player_magic_attributes_earth);
         nbt.putBoolean("player_magic_attributes_water", this.player_magic_attributes_water);
         nbt.putBoolean("player_magic_attributes_fire", this.player_magic_attributes_fire);
         nbt.putBoolean("player_magic_attributes_wind", this.player_magic_attributes_wind);
         nbt.putBoolean("player_magic_attributes_ether", this.player_magic_attributes_ether);
         nbt.putBoolean("player_magic_attributes_none", this.player_magic_attributes_none);
         nbt.putBoolean("player_magic_attributes_imaginary_number", this.player_magic_attributes_imaginary_number);
         nbt.putBoolean("player_magic_attributes_sword", this.player_magic_attributes_sword);
         nbt.putBoolean("is_magic_circuit_open", this.is_magic_circuit_open);
         nbt.putDouble("magic_circuit_open_timer", this.magic_circuit_open_timer);
         nbt.putDouble("magic_cooldown", this.magic_cooldown);
         nbt.putDouble("proficiency_structural_analysis", this.proficiency_structural_analysis);
         nbt.putDouble("proficiency_projection", this.proficiency_projection);
         nbt.putDouble("proficiency_jewel_magic_shoot", this.proficiency_jewel_magic_shoot);
         nbt.putDouble("proficiency_jewel_magic_release", this.proficiency_jewel_magic_release);
         nbt.putDouble("proficiency_unlimited_blade_works", this.proficiency_unlimited_blade_works);
         nbt.putDouble("proficiency_sword_barrel_full_open", this.proficiency_sword_barrel_full_open);
         nbt.putDouble("proficiency_gravity_magic", this.proficiency_gravity_magic);
         nbt.putDouble("proficiency_gander", this.proficiency_gander);
         nbt.putDouble("proficiency_reinforcement", this.proficiency_reinforcement);
         nbt.putBoolean("is_chanting_ubw", this.is_chanting_ubw);
         nbt.putInt("ubw_chant_progress", this.ubw_chant_progress);
         nbt.putInt("ubw_chant_timer", this.ubw_chant_timer);
         nbt.putBoolean("has_unlimited_blade_works", this.has_unlimited_blade_works);
         nbt.putBoolean("is_magus", this.is_magus);
         nbt.putBoolean("is_in_ubw", this.is_in_ubw);
         nbt.putDouble("ubw_return_x", this.ubw_return_x);
         nbt.putDouble("ubw_return_y", this.ubw_return_y);
         nbt.putDouble("ubw_return_z", this.ubw_return_z);
         nbt.putString("ubw_return_dimension", this.ubw_return_dimension);
         nbt.putInt("sword_barrel_mode", this.sword_barrel_mode);
         nbt.putInt("gandr_machine_gun_mode", this.gandr_machine_gun_mode);
         nbt.putInt("jewel_magic_mode", this.jewel_magic_mode);
         nbt.putInt("gravity_magic_target", this.gravity_magic_target);
         nbt.putInt("gravity_magic_mode", this.gravity_magic_mode);
         nbt.putInt("reinforcement_mode", this.reinforcement_mode);
         nbt.putInt("reinforcement_target", this.reinforcement_target);
         nbt.putInt("reinforcement_level", this.reinforcement_level);
         nbt.putBoolean("is_sword_barrel_active", this.is_sword_barrel_active);
         nbt.putBoolean("ubw_broken_phantasm_enabled", this.ubw_broken_phantasm_enabled);
         nbt.putInt("merlin_favor", this.merlin_favor);
         nbt.putInt("merlin_talk_counter", this.merlin_talk_counter);
         nbt.putInt("magic_system_data_version", this.magic_system_data_version);
         nbt.putInt("active_wheel_index", this.active_wheel_index);
         ListTag wheelList = new ListTag();

         for (TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry slotEntry : this.magic_wheels) {
            wheelList.add(slotEntry.serializeNBT());
         }

         nbt.put("magic_wheels", wheelList);
         ListTag runtimeSlotList = new ListTag();

         for (Integer slotIndex : this.selected_magic_runtime_slot_indices) {
            runtimeSlotList.add(IntTag.valueOf(slotIndex == null ? 0 : slotIndex));
         }

         nbt.put("selected_magic_runtime_slot_indices", runtimeSlotList);
         ListTag runtimeDisplayList = new ListTag();

         for (String displayName : this.selected_magic_display_names) {
            runtimeDisplayList.add(StringTag.valueOf(displayName == null ? "" : displayName));
         }

         nbt.put("selected_magic_display_names", runtimeDisplayList);
         ListTag magicList = new ListTag();

         for (String magic : this.selected_magics) {
            magicList.add(StringTag.valueOf(magic));
         }

         nbt.put("selected_magics", magicList);
         nbt.putInt("current_magic_index", this.current_magic_index);
         ListTag analyzedList = new ListTag();

         for (ItemStack stack : this.analyzed_items) {
            if (!stack.isEmpty()) {
               analyzedList.add(stack.save(lookupProvider));
            }
         }

         nbt.put("analyzed_items", analyzedList);
         ListTag learnedList = new ListTag();

         for (String magic : this.learned_magics) {
            learnedList.add(StringTag.valueOf(magic));
         }

         nbt.put("learned_magics", learnedList);
         if (!this.projection_selected_item.isEmpty()) {
            nbt.put("projection_selected_item", this.projection_selected_item.save(lookupProvider));
         }

         nbt.putString("projection_selected_structure_id", this.projection_selected_structure_id);
         ListTag structuresList = new ListTag();

         for (TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure : this.analyzed_structures) {
            structuresList.add(structure.serializeNBT(lookupProvider));
         }

         nbt.put("analyzed_structures", structuresList);
         nbt.put("mysticEyesInventory", this.mysticEyesInventory.serializeNBT(lookupProvider));
         nbt.put("magicCrestInventory", this.magicCrestInventory.serializeNBT(lookupProvider));
         ListTag crestEntriesTag = new ListTag();

         for (TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry : this.crest_entries) {
            crestEntriesTag.add(crestEntry.serializeNBT());
         }

         nbt.put("crest_entries", crestEntriesTag);
         CompoundTag practiceTag = new CompoundTag();

         for (Entry<String, Integer> entry : this.crest_practice_count.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()) {
               practiceTag.putInt(entry.getKey(), Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
            }
         }

         nbt.put("crest_practice_count", practiceTag);
         return nbt;
      }

      public void deserializeNBT(@NotNull Provider lookupProvider, CompoundTag nbt) {
         this.player_mana = nbt.getDouble("player_mana");
         this.player_max_mana = nbt.getDouble("player_max_mana");
         this.player_mana_egenerated_every_moment = nbt.getDouble("player_mana_egenerated_every_moment");
         this.player_restore_magic_moment = nbt.getDouble("player_restore_magic_moment");
         this.current_mana_regen_multiplier = nbt.contains("current_mana_regen_multiplier") ? nbt.getDouble("current_mana_regen_multiplier") : 1.0;
         this.player_magic_attributes_earth = nbt.getBoolean("player_magic_attributes_earth");
         this.player_magic_attributes_water = nbt.getBoolean("player_magic_attributes_water");
         this.player_magic_attributes_fire = nbt.getBoolean("player_magic_attributes_fire");
         this.player_magic_attributes_wind = nbt.getBoolean("player_magic_attributes_wind");
         this.player_magic_attributes_ether = nbt.getBoolean("player_magic_attributes_ether");
         this.player_magic_attributes_none = nbt.getBoolean("player_magic_attributes_none");
         this.player_magic_attributes_imaginary_number = nbt.getBoolean("player_magic_attributes_imaginary_number");
         this.player_magic_attributes_sword = nbt.getBoolean("player_magic_attributes_sword");
         this.is_magic_circuit_open = nbt.getBoolean("is_magic_circuit_open");
         this.magic_circuit_open_timer = nbt.getDouble("magic_circuit_open_timer");
         this.magic_cooldown = nbt.getDouble("magic_cooldown");
         this.proficiency_structural_analysis = nbt.getDouble("proficiency_structural_analysis");
         this.proficiency_projection = nbt.getDouble("proficiency_projection");
         this.proficiency_jewel_magic_shoot = nbt.getDouble("proficiency_jewel_magic_shoot");
         this.proficiency_jewel_magic_release = nbt.getDouble("proficiency_jewel_magic_release");
         if (nbt.contains("proficiency_jewel_magic")) {
            double old = nbt.getDouble("proficiency_jewel_magic");
            if (this.proficiency_jewel_magic_shoot == 0.0) {
               this.proficiency_jewel_magic_shoot = old;
            }

            if (this.proficiency_jewel_magic_release == 0.0) {
               this.proficiency_jewel_magic_release = old;
            }
         }

         this.proficiency_unlimited_blade_works = nbt.getDouble("proficiency_unlimited_blade_works");
         this.proficiency_sword_barrel_full_open = nbt.getDouble("proficiency_sword_barrel_full_open");
         this.proficiency_gravity_magic = nbt.getDouble("proficiency_gravity_magic");
         this.proficiency_gander = nbt.getDouble("proficiency_gander");
         this.proficiency_reinforcement = nbt.getDouble("proficiency_reinforcement");
         this.is_chanting_ubw = nbt.getBoolean("is_chanting_ubw");
         this.ubw_chant_progress = nbt.getInt("ubw_chant_progress");
         this.ubw_chant_timer = nbt.getInt("ubw_chant_timer");
         this.has_unlimited_blade_works = nbt.getBoolean("has_unlimited_blade_works");
         if (nbt.contains("is_magus")) {
            this.is_magus = nbt.getBoolean("is_magus");
         }

         this.is_in_ubw = nbt.getBoolean("is_in_ubw");
         if (nbt.contains("ubw_return_x")) {
            this.ubw_return_x = nbt.getDouble("ubw_return_x");
         }

         if (nbt.contains("ubw_return_y")) {
            this.ubw_return_y = nbt.getDouble("ubw_return_y");
         }

         if (nbt.contains("ubw_return_z")) {
            this.ubw_return_z = nbt.getDouble("ubw_return_z");
         }

         if (nbt.contains("ubw_return_dimension")) {
            this.ubw_return_dimension = nbt.getString("ubw_return_dimension");
         }

         if (nbt.contains("sword_barrel_mode")) {
            this.sword_barrel_mode = nbt.getInt("sword_barrel_mode");
         }

         if (nbt.contains("gandr_machine_gun_mode")) {
            this.gandr_machine_gun_mode = nbt.getInt("gandr_machine_gun_mode");
         }

         this.gandr_machine_gun_mode = Math.max(0, Math.min(1, this.gandr_machine_gun_mode));
         if (nbt.contains("jewel_magic_mode")) {
            this.jewel_magic_mode = nbt.getInt("jewel_magic_mode");
         }

         if (nbt.contains("gravity_magic_target")) {
            this.gravity_magic_target = nbt.getInt("gravity_magic_target");
         }

         if (nbt.contains("gravity_magic_mode")) {
            this.gravity_magic_mode = nbt.getInt("gravity_magic_mode");
         }

         this.gravity_magic_mode = Math.max(-2, Math.min(2, this.gravity_magic_mode));
         if (nbt.contains("reinforcement_mode")) {
            this.reinforcement_mode = nbt.getInt("reinforcement_mode");
         }

         if (nbt.contains("reinforcement_target")) {
            this.reinforcement_target = nbt.getInt("reinforcement_target");
         }

         if (nbt.contains("reinforcement_level")) {
            this.reinforcement_level = nbt.getInt("reinforcement_level");
         }

         if (nbt.contains("is_sword_barrel_active")) {
            this.is_sword_barrel_active = nbt.getBoolean("is_sword_barrel_active");
         }

         if (nbt.contains("ubw_broken_phantasm_enabled")) {
            this.ubw_broken_phantasm_enabled = nbt.getBoolean("ubw_broken_phantasm_enabled");
         }

         if (nbt.contains("merlin_favor")) {
            int favor = nbt.getInt("merlin_favor");
            if (favor > 5) {
               favor = 5;
            }

            if (favor < -5) {
               favor = -5;
            }

            this.merlin_favor = favor;
         }

         if (nbt.contains("merlin_talk_counter")) {
            this.merlin_talk_counter = nbt.getInt("merlin_talk_counter");
         }

         this.magic_system_data_version = nbt.contains("magic_system_data_version") ? nbt.getInt("magic_system_data_version") : 0;
         this.active_wheel_index = nbt.contains("active_wheel_index") ? nbt.getInt("active_wheel_index") : 0;
         this.magic_wheels.clear();
         if (nbt.contains("magic_wheels", 9)) {
            ListTag wheelList = nbt.getList("magic_wheels", 10);

            for (int i = 0; i < wheelList.size(); i++) {
               this.magic_wheels.add(TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry.fromNBT(wheelList.getCompound(i)));
            }
         }

         this.selected_magic_runtime_slot_indices.clear();
         if (nbt.contains("selected_magic_runtime_slot_indices", 9)) {
            ListTag runtimeSlotList = nbt.getList("selected_magic_runtime_slot_indices", 3);

            for (int i = 0; i < runtimeSlotList.size(); i++) {
               this.selected_magic_runtime_slot_indices.add(runtimeSlotList.getInt(i));
            }
         }

         this.selected_magic_display_names.clear();
         if (nbt.contains("selected_magic_display_names", 9)) {
            ListTag runtimeDisplayList = nbt.getList("selected_magic_display_names", 8);

            for (int i = 0; i < runtimeDisplayList.size(); i++) {
               this.selected_magic_display_names.add(runtimeDisplayList.getString(i));
            }
         }

         this.selected_magics.clear();
         if (nbt.contains("selected_magics")) {
            ListTag magicList = nbt.getList("selected_magics", 8);

            for (int i = 0; i < magicList.size(); i++) {
               this.selected_magics.add(magicList.getString(i));
            }
         }

         this.current_magic_index = nbt.getInt("current_magic_index");
         this.analyzed_items.clear();
         if (nbt.contains("analyzed_items")) {
            ListTag analyzedList = nbt.getList("analyzed_items", 10);

            for (int i = 0; i < analyzedList.size(); i++) {
               Optional<ItemStack> stack = ItemStack.parse(lookupProvider, analyzedList.getCompound(i));
               stack.ifPresent(this.analyzed_items::add);
            }
         }

         this.learned_magics.clear();
         if (nbt.contains("learned_magics")) {
            ListTag learnedList = nbt.getList("learned_magics", 8);

            for (int i = 0; i < learnedList.size(); i++) {
               this.learned_magics.add(learnedList.getString(i));
            }
         }

         this.projection_selected_item = ItemStack.EMPTY;
         if (nbt.contains("projection_selected_item")) {
            Optional<ItemStack> stack = ItemStack.parse(lookupProvider, nbt.getCompound("projection_selected_item"));
            stack.ifPresent(s -> this.projection_selected_item = s);
         }

         this.projection_selected_structure_id = "";
         if (nbt.contains("projection_selected_structure_id")) {
            this.projection_selected_structure_id = nbt.getString("projection_selected_structure_id");
         }

         this.analyzed_structures.clear();
         if (nbt.contains("analyzed_structures")) {
            ListTag structuresList = nbt.getList("analyzed_structures", 10);

            for (int i = 0; i < structuresList.size(); i++) {
               TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = TypeMoonWorldModVariables.PlayerVariables.SavedStructure.fromNBT(
                  lookupProvider, structuresList.getCompound(i)
               );
               if (structure.id == null || structure.id.isEmpty()) {
                  structure.id = UUID.randomUUID().toString();
               }

               this.analyzed_structures.add(structure);
            }
         }

         this.sanitizeAnalyzedStructures();
         if (!this.projection_selected_structure_id.isEmpty() && this.getStructureById(this.projection_selected_structure_id) == null) {
            this.projection_selected_structure_id = "";
         }

         if (nbt.contains("mysticEyesInventory")) {
            this.mysticEyesInventory.deserializeNBT(lookupProvider, nbt.getCompound("mysticEyesInventory"));
         }

         if (nbt.contains("magicCrestInventory")) {
            this.magicCrestInventory.deserializeNBT(lookupProvider, nbt.getCompound("magicCrestInventory"));
         }

         this.crest_entries.clear();
         if (nbt.contains("crest_entries", 9)) {
            ListTag crestList = nbt.getList("crest_entries", 10);

            for (int i = 0; i < crestList.size(); i++) {
               TypeMoonWorldModVariables.PlayerVariables.CrestEntry crestEntry = TypeMoonWorldModVariables.PlayerVariables.CrestEntry.fromNBT(
                  crestList.getCompound(i)
               );
               if (isKnownMagicId(crestEntry.magicId)) {
                  normalizeCrestEntry(crestEntry);
                  this.crest_entries.add(crestEntry);
               }
            }
         }

         this.crest_practice_count.clear();
         if (nbt.contains("crest_practice_count", 10)) {
            CompoundTag practiceTag = nbt.getCompound("crest_practice_count");

            for (String key : practiceTag.getAllKeys()) {
               if (isKnownMagicId(key)) {
                  this.crest_practice_count.put(key, Math.max(0, practiceTag.getInt(key)));
               }
            }
         }

         this.ensureMagicSystemInitialized();
      }

      public void syncPlayerVariables(Entity entity) {
         this.sanitizeAnalyzedStructures();
         this.ensureMagicSystemInitialized();
         if (this.player_magic_attributes_sword) {
            if (!this.learned_magics.contains("unlimited_blade_works")) {
               this.learned_magics.add("unlimited_blade_works");
               this.has_unlimited_blade_works = true;
               if (entity instanceof Player player && !player.level().isClientSide()) {
                  player.displayClientMessage(Component.translatable("message.typemoonworld.unlimited_blade_works.awakened"), false);
               }
            } else if (!this.has_unlimited_blade_works) {
               this.has_unlimited_blade_works = true;
            }
         }

         boolean knowsOldJewelMagic = false;
         String[] oldJewels = new String[]{
            "ruby_throw",
            "sapphire_throw",
            "emerald_use",
            "topaz_throw",
            "cyan_throw",
            "ruby_flame_sword",
            "sapphire_winter_frost",
            "emerald_winter_river",
            "topaz_reinforcement",
            "cyan_wind"
         };

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

            if (!this.learned_magics.contains("jewel_random_shoot")) {
               this.learned_magics.add("jewel_random_shoot");
            }
         }

         if (this.learned_magics.contains("jewel_magic_shoot") && !this.learned_magics.contains("jewel_random_shoot")) {
            this.learned_magics.add("jewel_random_shoot");
         }

         if ((this.learned_magics.contains("jewel_magic_release") || this.learned_magics.contains("jewel_machine_gun"))
            && !this.learned_magics.contains("jewel_magic_shoot")) {
            this.learned_magics.add("jewel_magic_shoot");
         }

         if (this.learned_magics.contains("gandr_machine_gun") && !this.learned_magics.contains("gander")) {
            this.learned_magics.add("gander");
         }

         if (!this.crest_cast_context) {
            this.tryAutoUnlockJewelMachineGun(entity);
            this.tryAutoUnlockGandrMachineGun(entity);
         }

         boolean knowsReinforcementSubSkill = this.learned_magics.contains("reinforcement_self")
            || this.learned_magics.contains("reinforcement_other")
            || this.learned_magics.contains("reinforcement_item");
         if (knowsReinforcementSubSkill && !this.learned_magics.contains("reinforcement")) {
            this.learned_magics.add("reinforcement");
         }

         if (this.magic_system_data_version < 1) {
            this.clearAllWheelSlots();
            this.selected_magics.clear();
            this.selected_magic_runtime_slot_indices.clear();
            this.selected_magic_display_names.clear();
            this.current_magic_index = 0;
            this.active_wheel_index = 0;
            this.magic_system_data_version = 1;
         }

         this.syncSelfCrestEntriesFromKnowledge();
         ItemStack implantedCrest = this.magicCrestInventory.getStackInSlot(0);
         if (!implantedCrest.isEmpty() && implantedCrest.getItem() instanceof MagicCrestItem) {
            writeCrestEntriesToStack(implantedCrest, this.crest_entries);
         }

         this.pruneInvalidCrestWheelReferences();
         this.rebuildSelectedMagicsFromActiveWheel();
         this.gravity_magic_mode = Math.max(-2, Math.min(2, this.gravity_magic_mode));
         this.gandr_machine_gun_mode = Math.max(0, Math.min(1, this.gandr_machine_gun_mode));
         if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
               serverPlayer,
               new TypeMoonWorldModVariables.PlayerVariablesSyncMessage(this.serializeNBT(serverPlayer.registryAccess())),
               new CustomPacketPayload[0]
            );
         }
      }

      public void syncRuntimeSelection(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TypeMoonWorldModVariables.RuntimeSelectionSyncMessage(this), new CustomPacketPayload[0]);
         }
      }

      public void syncModeState(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TypeMoonWorldModVariables.ModeStateSyncMessage(this), new CustomPacketPayload[0]);
         }
      }

      public void syncMana(Entity entity) {
         if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new TypeMoonWorldModVariables.ManaSyncMessage(this), new CustomPacketPayload[0]);
         }
      }

      public void syncProficiency(Entity entity) {
         if (this.crest_cast_context) {
            if (entity instanceof ServerPlayer serverPlayer) {
               PacketDistributor.sendToPlayer(serverPlayer, new TypeMoonWorldModVariables.ProficiencySyncMessage(this), new CustomPacketPayload[0]);
            }
         } else {
            boolean autoUnlocked = this.tryAutoUnlockJewelMachineGun(entity) | this.tryAutoUnlockGandrMachineGun(entity);
            if (autoUnlocked) {
               this.syncPlayerVariables(entity);
            } else {
               if (entity instanceof ServerPlayer serverPlayer) {
                  PacketDistributor.sendToPlayer(serverPlayer, new TypeMoonWorldModVariables.ProficiencySyncMessage(this), new CustomPacketPayload[0]);
               }
            }
         }
      }

      public void syncProjectionDelta(Entity entity, int action, CompoundTag payload) {
         if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
               serverPlayer,
               new TypeMoonWorldModVariables.ProjectionDeltaSyncMessage(action, payload == null ? new CompoundTag() : payload),
               new CustomPacketPayload[0]
            );
         }
      }

      private boolean tryAutoUnlockJewelMachineGun(Entity entity) {
         if (!(this.proficiency_gander < 50.0) && this.learned_magics.contains("jewel_magic_release") && !this.learned_magics.contains("jewel_machine_gun")) {
            this.learned_magics.add("jewel_machine_gun");
            if (entity instanceof Player player && !player.level().isClientSide()) {
               player.displayClientMessage(
                  Component.translatable(
                     "message.typemoonworld.magic.learned", new Object[]{Component.translatable("magic.typemoonworld.jewel_machine_gun.name")}
                  ),
                  false
               );
            }

            return true;
         } else {
            return false;
         }
      }

      private boolean tryAutoUnlockGandrMachineGun(Entity entity) {
         if (!(this.proficiency_gander < 100.0) && this.learned_magics.contains("gander") && !this.learned_magics.contains("gandr_machine_gun")) {
            this.learned_magics.add("gandr_machine_gun");
            if (entity instanceof Player player && !player.level().isClientSide()) {
               player.displayClientMessage(
                  Component.translatable(
                     "message.typemoonworld.magic.learned", new Object[]{Component.translatable("magic.typemoonworld.gandr_machine_gun.name")}
                  ),
                  false
               );
            }

            return true;
         } else {
            return false;
         }
      }

      public static class CrestEntry {
         public String entryId = "";
         public String magicId = "";
         public CompoundTag presetPayload = new CompoundTag();
         public String sourceKind = "self";
         public String originOwnerUuid = "";
         public String originOwnerType = "npc";
         public String originOwnerName = "";
         public boolean active = true;

         public TypeMoonWorldModVariables.PlayerVariables.CrestEntry copy() {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry copy = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
            copy.entryId = this.entryId == null ? "" : this.entryId;
            copy.magicId = this.magicId == null ? "" : this.magicId;
            copy.presetPayload = this.presetPayload == null ? new CompoundTag() : this.presetPayload.copy();
            copy.sourceKind = this.sourceKind == null ? "self" : this.sourceKind;
            copy.originOwnerUuid = this.originOwnerUuid == null ? "" : this.originOwnerUuid;
            copy.originOwnerType = this.originOwnerType == null ? "npc" : this.originOwnerType;
            copy.originOwnerName = this.originOwnerName == null ? "" : this.originOwnerName;
            copy.active = this.active;
            return copy;
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("entry_id", this.entryId == null ? "" : this.entryId);
            tag.putString("magic_id", this.magicId == null ? "" : this.magicId);
            tag.put("preset_payload", this.presetPayload == null ? new CompoundTag() : this.presetPayload.copy());
            tag.putString("source_kind", this.sourceKind == null ? "self" : this.sourceKind);
            tag.putString("origin_owner_uuid", this.originOwnerUuid == null ? "" : this.originOwnerUuid);
            tag.putString("origin_owner_type", this.originOwnerType == null ? "npc" : this.originOwnerType);
            tag.putString("origin_owner_name", this.originOwnerName == null ? "" : this.originOwnerName);
            tag.putBoolean("active", this.active);
            return tag;
         }

         public static TypeMoonWorldModVariables.PlayerVariables.CrestEntry fromNBT(CompoundTag tag) {
            TypeMoonWorldModVariables.PlayerVariables.CrestEntry entry = new TypeMoonWorldModVariables.PlayerVariables.CrestEntry();
            entry.entryId = tag.contains("entry_id") ? tag.getString("entry_id") : "";
            entry.magicId = tag.contains("magic_id") ? tag.getString("magic_id") : "";
            entry.presetPayload = tag.contains("preset_payload", 10) ? tag.getCompound("preset_payload").copy() : new CompoundTag();
            entry.sourceKind = tag.contains("source_kind") ? tag.getString("source_kind") : "self";
            entry.originOwnerUuid = tag.contains("origin_owner_uuid") ? tag.getString("origin_owner_uuid") : "";
            entry.originOwnerType = tag.contains("origin_owner_type") ? tag.getString("origin_owner_type") : "npc";
            entry.originOwnerName = tag.contains("origin_owner_name") ? tag.getString("origin_owner_name") : "";
            entry.active = !tag.contains("active") || tag.getBoolean("active");
            return entry;
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
         public List<TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock> blocks = new ArrayList<>();

         public TypeMoonWorldModVariables.PlayerVariables.SavedStructure copy() {
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = new TypeMoonWorldModVariables.PlayerVariables.SavedStructure();
            structure.id = this.id;
            structure.name = this.name;
            structure.source = this.source;
            structure.sizeX = this.sizeX;
            structure.sizeY = this.sizeY;
            structure.sizeZ = this.sizeZ;
            structure.totalBlocks = this.totalBlocks;
            structure.icon = this.icon.copy();

            for (TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock block : this.blocks) {
               structure.blocks.add(block.copy());
            }

            return structure;
         }

         public CompoundTag serializeNBT(Provider lookupProvider) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.id);
            tag.putString("name", this.name);
            if (this.source != null && !this.source.isEmpty()) {
               tag.putString("source", this.source);
            }

            tag.putInt("size_x", this.sizeX);
            tag.putInt("size_y", this.sizeY);
            tag.putInt("size_z", this.sizeZ);
            tag.putInt("total_blocks", this.totalBlocks);
            if (!this.icon.isEmpty()) {
               tag.put("icon", this.icon.save(lookupProvider));
            }

            ListTag blockList = new ListTag();

            for (TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock block : this.blocks) {
               blockList.add(block.serializeNBT());
            }

            tag.put("blocks", blockList);
            return tag;
         }

         public static TypeMoonWorldModVariables.PlayerVariables.SavedStructure fromNBT(Provider lookupProvider, CompoundTag tag) {
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = new TypeMoonWorldModVariables.PlayerVariables.SavedStructure();
            if (tag.contains("id")) {
               structure.id = tag.getString("id");
            }

            if (tag.contains("name")) {
               structure.name = tag.getString("name");
            }

            if (tag.contains("source")) {
               structure.source = tag.getString("source");
            }

            structure.sizeX = Math.max(1, tag.getInt("size_x"));
            structure.sizeY = Math.max(1, tag.getInt("size_y"));
            structure.sizeZ = Math.max(1, tag.getInt("size_z"));
            structure.totalBlocks = Math.max(0, tag.getInt("total_blocks"));
            if (tag.contains("icon")) {
               Optional<ItemStack> iconStack = ItemStack.parse(lookupProvider, tag.getCompound("icon"));
               iconStack.ifPresent(stack -> structure.icon = stack);
            }

            if (tag.contains("blocks")) {
               ListTag blockList = tag.getList("blocks", 10);

               for (int i = 0; i < blockList.size(); i++) {
                  structure.blocks.add(TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock.fromNBT(blockList.getCompound(i)));
               }
            }

            return structure;
         }
      }

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

         public TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock copy() {
            return new TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock(this.x, this.y, this.z, this.blockId, this.blockStateProps);
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", this.x);
            tag.putInt("y", this.y);
            tag.putInt("z", this.z);
            tag.putString("block_id", this.blockId);
            if (this.blockStateProps != null && !this.blockStateProps.isEmpty()) {
               tag.putString("block_state_props", this.blockStateProps);
            }

            return tag;
         }

         public static TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock fromNBT(CompoundTag tag) {
            TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock block = new TypeMoonWorldModVariables.PlayerVariables.SavedStructureBlock();
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

      public static class WheelSlotEntry {
         public int wheelIndex = 0;
         public int slotIndex = 0;
         public String sourceType = "self";
         public String magicId = "";
         public CompoundTag presetPayload = new CompoundTag();
         public String crestEntryId = "";
         public String displayNameCache = "";

         public WheelSlotEntry() {
         }

         public WheelSlotEntry(int wheelIndex, int slotIndex) {
            this.wheelIndex = wheelIndex;
            this.slotIndex = slotIndex;
         }

         public TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry copy() {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry copy = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(
               this.wheelIndex, this.slotIndex
            );
            copy.sourceType = this.sourceType == null ? "self" : this.sourceType;
            copy.magicId = this.magicId == null ? "" : this.magicId;
            copy.presetPayload = this.presetPayload == null ? new CompoundTag() : this.presetPayload.copy();
            copy.crestEntryId = this.crestEntryId == null ? "" : this.crestEntryId;
            copy.displayNameCache = this.displayNameCache == null ? "" : this.displayNameCache;
            return copy;
         }

         public boolean isEmpty() {
            return this.magicId == null || this.magicId.isEmpty();
         }

         public void clear() {
            this.sourceType = "self";
            this.magicId = "";
            this.presetPayload = new CompoundTag();
            this.crestEntryId = "";
            this.displayNameCache = "";
         }

         public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("wheel", this.wheelIndex);
            tag.putInt("slot", this.slotIndex);
            tag.putString("source_type", this.sourceType == null ? "self" : this.sourceType);
            tag.putString("magic_id", this.magicId == null ? "" : this.magicId);
            tag.put("preset_payload", this.presetPayload == null ? new CompoundTag() : this.presetPayload.copy());
            tag.putString("crest_entry_id", this.crestEntryId == null ? "" : this.crestEntryId);
            tag.putString("display_name_cache", this.displayNameCache == null ? "" : this.displayNameCache);
            return tag;
         }

         public static TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry fromNBT(CompoundTag tag) {
            TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry entry = new TypeMoonWorldModVariables.PlayerVariables.WheelSlotEntry(
               tag.getInt("wheel"), tag.getInt("slot")
            );
            entry.sourceType = tag.contains("source_type") ? tag.getString("source_type") : "self";
            entry.magicId = tag.contains("magic_id") ? tag.getString("magic_id") : "";
            entry.presetPayload = tag.contains("preset_payload", 10) ? tag.getCompound("preset_payload").copy() : new CompoundTag();
            entry.crestEntryId = tag.contains("crest_entry_id") ? tag.getString("crest_entry_id") : "";
            entry.displayNameCache = tag.contains("display_name_cache") ? tag.getString("display_name_cache") : "";
            return entry;
         }
      }
   }

   public record PlayerVariablesSyncMessage(CompoundTag data) implements CustomPacketPayload {
      public static final Type<TypeMoonWorldModVariables.PlayerVariablesSyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "player_variables_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> buffer.writeNbt(message.data == null ? new CompoundTag() : message.data), buffer -> {
            CompoundTag tag = buffer.readNbt();
            return new TypeMoonWorldModVariables.PlayerVariablesSyncMessage(tag == null ? new CompoundTag() : tag);
         }
      );

      @NotNull
      public Type<TypeMoonWorldModVariables.PlayerVariablesSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.PlayerVariablesSyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
            context.enqueueWork(
                  () -> ((TypeMoonWorldModVariables.PlayerVariables)context.player().getData(TypeMoonWorldModVariables.PLAYER_VARIABLES))
                     .deserializeNBT(context.player().registryAccess(), message.data)
               )
               .exceptionally(e -> {
                  TYPE_MOON_WORLD.LOGGER.error("Failed to handle PlayerVariablesSyncMessage", e);
                  return null;
               });
         }
      }
   }

   public record ProficiencySyncMessage(
      double structural_analysis,
      double projection,
      double jewel_magic_shoot,
      double jewel_magic_release,
      double unlimited_blade_works,
      double sword_barrel_full_open,
      double gravity_magic,
      double gander,
      double reinforcement
   ) implements CustomPacketPayload {
      public static final Type<TypeMoonWorldModVariables.ProficiencySyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "proficiency_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.ProficiencySyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> {
            buffer.writeDouble(message.structural_analysis);
            buffer.writeDouble(message.projection);
            buffer.writeDouble(message.jewel_magic_shoot);
            buffer.writeDouble(message.jewel_magic_release);
            buffer.writeDouble(message.unlimited_blade_works);
            buffer.writeDouble(message.sword_barrel_full_open);
            buffer.writeDouble(message.gravity_magic);
            buffer.writeDouble(message.gander);
            buffer.writeDouble(message.reinforcement);
         },
         buffer -> new TypeMoonWorldModVariables.ProficiencySyncMessage(
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble(),
            buffer.readDouble()
         )
      );

      public ProficiencySyncMessage(TypeMoonWorldModVariables.PlayerVariables vars) {
         this(
            vars.proficiency_structural_analysis,
            vars.proficiency_projection,
            vars.proficiency_jewel_magic_shoot,
            vars.proficiency_jewel_magic_release,
            vars.proficiency_unlimited_blade_works,
            vars.proficiency_sword_barrel_full_open,
            vars.proficiency_gravity_magic,
            vars.proficiency_gander,
            vars.proficiency_reinforcement
         );
      }

      @NotNull
      public Type<TypeMoonWorldModVariables.ProficiencySyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.ProficiencySyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(
               () -> {
                  TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                     .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                  vars.proficiency_structural_analysis = message.structural_analysis;
                  vars.proficiency_projection = message.projection;
                  vars.proficiency_jewel_magic_shoot = message.jewel_magic_shoot;
                  vars.proficiency_jewel_magic_release = message.jewel_magic_release;
                  vars.proficiency_unlimited_blade_works = message.unlimited_blade_works;
                  vars.proficiency_sword_barrel_full_open = message.sword_barrel_full_open;
                  vars.proficiency_gravity_magic = message.gravity_magic;
                  vars.proficiency_gander = message.gander;
                  vars.proficiency_reinforcement = message.reinforcement;
               }
            );
         }
      }
   }

   public record ProjectionDeltaSyncMessage(int action, CompoundTag payload) implements CustomPacketPayload {
      public static final int ACTION_SAVE = 0;
      public static final int ACTION_DELETE = 1;
      public static final int ACTION_SELECTION = 2;
      public static final Type<TypeMoonWorldModVariables.ProjectionDeltaSyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "projection_delta_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.ProjectionDeltaSyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> {
            buffer.writeInt(message.action);
            buffer.writeNbt(message.payload == null ? new CompoundTag() : message.payload);
         }, buffer -> {
            int action = buffer.readInt();
            CompoundTag tag = buffer.readNbt();
            return new TypeMoonWorldModVariables.ProjectionDeltaSyncMessage(action, tag == null ? new CompoundTag() : tag);
         }
      );

      @NotNull
      public Type<TypeMoonWorldModVariables.ProjectionDeltaSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.ProjectionDeltaSyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(
                  () -> {
                     TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                        .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                     CompoundTag tag = message.payload == null ? new CompoundTag() : message.payload;
                     switch (message.action) {
                        case 0:
                           applySaveDelta(vars, context.player().registryAccess(), tag);
                           break;
                        case 1:
                           applyDeleteDelta(vars, tag);
                           break;
                        case 2:
                           applySelectionDelta(vars, context.player().registryAccess(), tag);
                     }
                  }
               )
               .exceptionally(e -> {
                  TYPE_MOON_WORLD.LOGGER.error("Failed to handle ProjectionDeltaSyncMessage", e);
                  return null;
               });
         }
      }

      private static void applySaveDelta(TypeMoonWorldModVariables.PlayerVariables vars, Provider lookupProvider, CompoundTag tag) {
         String replacedStructureId = tag.getString("replaced_structure_id");
         if (!replacedStructureId.isEmpty()) {
            vars.removeStructureById(replacedStructureId);
         }

         if (tag.contains("structure", 10)) {
            TypeMoonWorldModVariables.PlayerVariables.SavedStructure structure = TypeMoonWorldModVariables.PlayerVariables.SavedStructure.fromNBT(
               lookupProvider, tag.getCompound("structure")
            );
            if (structure.id != null && !structure.id.isEmpty()) {
               boolean replaced = false;

               for (int i = 0; i < vars.analyzed_structures.size(); i++) {
                  TypeMoonWorldModVariables.PlayerVariables.SavedStructure existing = vars.analyzed_structures.get(i);
                  if (existing != null && structure.id.equals(existing.id)) {
                     vars.analyzed_structures.set(i, structure);
                     replaced = true;
                     break;
                  }
               }

               if (!replaced) {
                  vars.analyzed_structures.add(structure);
               }
            }
         }

         applySelectionDelta(vars, lookupProvider, tag);
      }

      private static void applyDeleteDelta(TypeMoonWorldModVariables.PlayerVariables vars, CompoundTag tag) {
         String deletedId = tag.getString("deleted_structure_id");
         if (!deletedId.isEmpty()) {
            vars.removeStructureById(deletedId);
         }

         if (tag.contains("selected_structure_id")) {
            vars.projection_selected_structure_id = tag.getString("selected_structure_id");
         }

         if (!vars.projection_selected_structure_id.isEmpty() && vars.getStructureById(vars.projection_selected_structure_id) == null) {
            vars.projection_selected_structure_id = "";
         }
      }

      private static void applySelectionDelta(TypeMoonWorldModVariables.PlayerVariables vars, Provider lookupProvider, CompoundTag tag) {
         if (tag.contains("selected_structure_id")) {
            vars.projection_selected_structure_id = tag.getString("selected_structure_id");
         }

         if (tag.getBoolean("clear_selected_item")) {
            vars.projection_selected_item = ItemStack.EMPTY;
         } else if (tag.contains("selected_item", 10)) {
            ItemStack.parse(lookupProvider, tag.getCompound("selected_item")).ifPresent(stack -> vars.projection_selected_item = stack);
         }

         if (tag.getBoolean("clear_selected_structure")) {
            vars.projection_selected_structure_id = "";
         }
      }
   }

   public static class ReinforcementData implements INBTSerializable<CompoundTag> {
      public UUID casterUUID;

      public CompoundTag serializeNBT(Provider lookupProvider) {
         CompoundTag tag = new CompoundTag();
         if (this.casterUUID != null) {
            tag.putUUID("casterUUID", this.casterUUID);
         }

         return tag;
      }

      public void deserializeNBT(Provider lookupProvider, CompoundTag tag) {
         if (tag.hasUUID("casterUUID")) {
            this.casterUUID = tag.getUUID("casterUUID");
         }
      }
   }

   public record RuntimeSelectionSyncMessage(
      int active_wheel_index,
      int current_magic_index,
      List<String> selected_magics,
      List<Integer> selected_magic_runtime_slot_indices,
      List<String> selected_magic_display_names
   ) implements CustomPacketPayload {
      private static final int MAX_MAGIC_ID_LENGTH = 96;
      private static final int MAX_DISPLAY_NAME_LENGTH = 256;
      private static final int MAX_RUNTIME_SELECTION_SIZE = 12;
      public static final Type<TypeMoonWorldModVariables.RuntimeSelectionSyncMessage> TYPE = new Type<>(
         ResourceLocation.fromNamespaceAndPath("typemoonworld", "runtime_selection_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TypeMoonWorldModVariables.RuntimeSelectionSyncMessage> STREAM_CODEC = StreamCodec.of(
         (buffer, message) -> {
            buffer.writeInt(message.active_wheel_index);
            buffer.writeInt(message.current_magic_index);
            int magicCount = Math.min(12, message.selected_magics == null ? 0 : message.selected_magics.size());
            buffer.writeInt(magicCount);

            for (int i = 0; i < magicCount; i++) {
               String id = message.selected_magics.get(i);
               buffer.writeUtf(id == null ? "" : id, 96);
            }

            int slotCount = Math.min(12, message.selected_magic_runtime_slot_indices == null ? 0 : message.selected_magic_runtime_slot_indices.size());
            buffer.writeInt(slotCount);

            for (int i = 0; i < slotCount; i++) {
               Integer slot = message.selected_magic_runtime_slot_indices.get(i);
               buffer.writeInt(slot == null ? -1 : slot);
            }

            int displayCount = Math.min(12, message.selected_magic_display_names == null ? 0 : message.selected_magic_display_names.size());
            buffer.writeInt(displayCount);

            for (int i = 0; i < displayCount; i++) {
               String display = message.selected_magic_display_names.get(i);
               buffer.writeUtf(display == null ? "" : display, 256);
            }
         }, buffer -> {
            int activeWheel = buffer.readInt();
            int currentIndex = buffer.readInt();
            int magicCount = Math.max(0, Math.min(12, buffer.readInt()));
            List<String> magics = new ArrayList<>(magicCount);

            for (int i = 0; i < magicCount; i++) {
               magics.add(buffer.readUtf(96));
            }

            int slotCount = Math.max(0, Math.min(12, buffer.readInt()));
            List<Integer> slots = new ArrayList<>(slotCount);

            for (int i = 0; i < slotCount; i++) {
               slots.add(buffer.readInt());
            }

            int displayCount = Math.max(0, Math.min(12, buffer.readInt()));
            List<String> displays = new ArrayList<>(displayCount);

            for (int i = 0; i < displayCount; i++) {
               displays.add(buffer.readUtf(256));
            }

            return new TypeMoonWorldModVariables.RuntimeSelectionSyncMessage(activeWheel, currentIndex, magics, slots, displays);
         }
      );

      public RuntimeSelectionSyncMessage(TypeMoonWorldModVariables.PlayerVariables vars) {
         this(
            vars.active_wheel_index,
            vars.current_magic_index,
            new ArrayList<>(vars.selected_magics),
            new ArrayList<>(vars.selected_magic_runtime_slot_indices),
            new ArrayList<>(vars.selected_magic_display_names)
         );
      }

      @NotNull
      public Type<TypeMoonWorldModVariables.RuntimeSelectionSyncMessage> type() {
         return TYPE;
      }

      public static void handleData(TypeMoonWorldModVariables.RuntimeSelectionSyncMessage message, IPayloadContext context) {
         if (context.flow() == PacketFlow.CLIENTBOUND) {
            context.enqueueWork(
                  () -> {
                     TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)context.player()
                        .getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                     vars.ensureMagicSystemInitialized();
                     vars.active_wheel_index = Mth.clamp(message.active_wheel_index, 0, 9);
                     vars.selected_magics.clear();
                     vars.selected_magic_runtime_slot_indices.clear();
                     vars.selected_magic_display_names.clear();
                     int size = Math.min(
                        12,
                        Math.min(
                           message.selected_magics == null ? 0 : message.selected_magics.size(),
                           Math.min(
                              message.selected_magic_runtime_slot_indices == null ? 0 : message.selected_magic_runtime_slot_indices.size(),
                              message.selected_magic_display_names == null ? 0 : message.selected_magic_display_names.size()
                           )
                        )
                     );

                     for (int i = 0; i < size; i++) {
                        String id = message.selected_magics.get(i);
                        Integer slot = message.selected_magic_runtime_slot_indices.get(i);
                        String display = message.selected_magic_display_names.get(i);
                        if (id != null && !id.isEmpty() && slot != null && slot >= 0 && slot < 12) {
                           vars.selected_magics.add(id);
                           vars.selected_magic_runtime_slot_indices.add(slot);
                           vars.selected_magic_display_names.add(display == null ? "" : display);
                        }
                     }

                     if (vars.selected_magics.isEmpty()) {
                        vars.current_magic_index = 0;
                     } else {
                        vars.current_magic_index = Mth.clamp(message.current_magic_index, 0, vars.selected_magics.size() - 1);
                     }
                  }
               )
               .exceptionally(e -> {
                  TYPE_MOON_WORLD.LOGGER.error("Failed to handle RuntimeSelectionSyncMessage", e);
                  return null;
               });
         }
      }
   }

   public static class UBWReturnData implements INBTSerializable<CompoundTag> {
      public UUID ownerUUID;
      public double returnX;
      public double returnY;
      public double returnZ;
      public String returnDim = "";
      public boolean generated = false;

      public CompoundTag serializeNBT(Provider lookupProvider) {
         CompoundTag tag = new CompoundTag();
         if (this.ownerUUID != null) {
            tag.putUUID("ownerUUID", this.ownerUUID);
         }

         tag.putDouble("returnX", this.returnX);
         tag.putDouble("returnY", this.returnY);
         tag.putDouble("returnZ", this.returnZ);
         tag.putString("returnDim", this.returnDim);
         tag.putBoolean("generated", this.generated);
         return tag;
      }

      public void deserializeNBT(Provider lookupProvider, CompoundTag tag) {
         if (tag.hasUUID("ownerUUID")) {
            this.ownerUUID = tag.getUUID("ownerUUID");
         }

         this.returnX = tag.getDouble("returnX");
         this.returnY = tag.getDouble("returnY");
         this.returnZ = tag.getDouble("returnZ");
         this.returnDim = tag.getString("returnDim");
         this.generated = tag.getBoolean("generated");
      }
   }
}
