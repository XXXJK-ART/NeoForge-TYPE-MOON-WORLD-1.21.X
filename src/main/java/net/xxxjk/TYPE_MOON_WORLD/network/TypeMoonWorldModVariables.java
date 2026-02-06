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
                        player.displayClientMessage(Component.literal("§b你感受到了一股微弱的魔力波动... 按下 §eX §f键以觉醒魔术回路."), true);
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
            
            clone.proficiency_structural_analysis = original.proficiency_structural_analysis;
            clone.proficiency_projection = original.proficiency_projection;
            clone.proficiency_jewel_magic = original.proficiency_jewel_magic;
            clone.proficiency_unlimited_blade_works = original.proficiency_unlimited_blade_works;
            
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
        public double player_mana = 0;
        public double player_max_mana = 0;
        public double player_mana_egenerated_every_moment = 0;
        public double player_restore_magic_moment = 0;
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

        // Proficiency Data
        public double proficiency_structural_analysis = 0;
        public double proficiency_projection = 0;
        public double proficiency_jewel_magic = 0;
        public double proficiency_unlimited_blade_works = 0;
        
        // Projection Magic Data
        public java.util.List<ItemStack> analyzed_items = new java.util.ArrayList<>();
        public ItemStack projection_selected_item = ItemStack.EMPTY;
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

        @Override
        public CompoundTag serializeNBT(HolderLookup.@NotNull Provider lookupProvider) {
            CompoundTag nbt = new CompoundTag();
            nbt.putDouble("player_mana", player_mana);
            nbt.putDouble("player_max_mana", player_max_mana);
            nbt.putDouble("player_mana_egenerated_every_moment", player_mana_egenerated_every_moment);
            nbt.putDouble("player_restore_magic_moment", player_restore_magic_moment);
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
            nbt.putDouble("proficiency_jewel_magic", proficiency_jewel_magic);
            nbt.putDouble("proficiency_unlimited_blade_works", proficiency_unlimited_blade_works);
            
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
            
            nbt.put("mysticEyesInventory", mysticEyesInventory.serializeNBT(lookupProvider));

            return nbt;
        }

        @Override
        public void deserializeNBT(HolderLookup.@NotNull Provider lookupProvider, CompoundTag nbt) {
            player_mana = nbt.getDouble("player_mana");
            player_max_mana = nbt.getDouble("player_max_mana");
            player_mana_egenerated_every_moment = nbt.getDouble("player_mana_egenerated_every_moment");
            player_restore_magic_moment = nbt.getDouble("player_restore_magic_moment");
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
            proficiency_jewel_magic = nbt.getDouble("proficiency_jewel_magic");
            proficiency_unlimited_blade_works = nbt.getDouble("proficiency_unlimited_blade_works");
            
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
            
            if (nbt.contains("mysticEyesInventory")) {
                mysticEyesInventory.deserializeNBT(lookupProvider, nbt.getCompound("mysticEyesInventory"));
            }
        }

        public void syncPlayerVariables(Entity entity) {
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

    public record ManaSyncMessage(double player_mana, double player_max_mana, double magic_cooldown, double magic_circuit_open_timer, boolean is_magic_circuit_open) implements CustomPacketPayload {
        public static final Type<ManaSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mana_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncMessage> STREAM_CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, ManaSyncMessage message) -> {
                    buffer.writeDouble(message.player_mana);
                    buffer.writeDouble(message.player_max_mana);
                    buffer.writeDouble(message.magic_cooldown);
                    buffer.writeDouble(message.magic_circuit_open_timer);
                    buffer.writeBoolean(message.is_magic_circuit_open);
                },
                (RegistryFriendlyByteBuf buffer) -> new ManaSyncMessage(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readBoolean()
                )
        );

        public ManaSyncMessage(PlayerVariables vars) {
            this(vars.player_mana, vars.player_max_mana, vars.magic_cooldown, vars.magic_circuit_open_timer, vars.is_magic_circuit_open);
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
                });
            }
        }
    }

    public record ProficiencySyncMessage(double structural_analysis, double projection, double jewel_magic, double unlimited_blade_works) implements CustomPacketPayload {
        public static final Type<ProficiencySyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "proficiency_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ProficiencySyncMessage> STREAM_CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buffer, ProficiencySyncMessage message) -> {
                    buffer.writeDouble(message.structural_analysis);
                    buffer.writeDouble(message.projection);
                    buffer.writeDouble(message.jewel_magic);
                    buffer.writeDouble(message.unlimited_blade_works);
                },
                (RegistryFriendlyByteBuf buffer) -> new ProficiencySyncMessage(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble()
                )
        );

        public ProficiencySyncMessage(PlayerVariables vars) {
            this(vars.proficiency_structural_analysis, vars.proficiency_projection, vars.proficiency_jewel_magic, vars.proficiency_unlimited_blade_works);
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
                    vars.proficiency_jewel_magic = message.jewel_magic;
                    vars.proficiency_unlimited_blade_works = message.unlimited_blade_works;
                });
            }
        }
    }
}