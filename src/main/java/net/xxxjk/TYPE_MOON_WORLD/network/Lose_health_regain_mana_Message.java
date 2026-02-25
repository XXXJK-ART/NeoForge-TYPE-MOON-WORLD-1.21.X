package net.xxxjk.TYPE_MOON_WORLD.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

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

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

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
        // security measure to prevent arbitrary chunk generation
        if (world.isLoaded(entity.blockPosition())) {
            if (entity instanceof ServerPlayer player) {
                TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
                
                if (!vars.is_magus) {
                    // Check for Mod Item in Inventory
                    boolean hasModItem = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (id != null && TYPE_MOON_WORLD.MOD_ID.equals(id.getNamespace())) {
                                hasModItem = true;
                                break;
                            }
                        }
                    }

                    if (!hasModItem) {
                        player.displayClientMessage(Component.literal("§c你还没有获得魔术的媒介."), true);
                        return;
                    }

                    // Unlock Logic
                    vars.is_magus = true;
                    java.util.Random random = new java.util.Random();
                    
                    // Randomize Base Stats
                    vars.player_max_mana = Math.round((100.0 + random.nextDouble() * 900.0) * 10.0) / 10.0; // 100.0 to 1000.0
                    vars.player_mana = vars.player_max_mana;
                    
                    // Round to 1 decimal place
                    double regen = 1.0 + random.nextDouble() * 9.0; // 1.0 to 10.0
                    vars.player_mana_egenerated_every_moment = Math.round(regen * 10.0) / 10.0;
                    
                    double restore = 1.0 + random.nextDouble() * 9.0; // 1.0 to 10.0
                    vars.player_restore_magic_moment = Math.round(restore * 10.0) / 10.0;
                    
                    // Reset all first
                    vars.player_magic_attributes_earth = false;
                    vars.player_magic_attributes_water = false;
                    vars.player_magic_attributes_fire = false;
                    vars.player_magic_attributes_wind = false;
                    vars.player_magic_attributes_ether = false;
                    vars.player_magic_attributes_none = false;
                    vars.player_magic_attributes_imaginary_number = false;
                    
                    // Special Attribute Check (Rare)
                    // If special, no basic attributes
                    boolean isSpecial = false;
                    
                    // 10% chance for special attribute
                    if (random.nextInt(100) < 10) {
                        if (random.nextBoolean()) {
                            vars.player_magic_attributes_none = true;
                        } else {
                            vars.player_magic_attributes_imaginary_number = true;
                        }
                        isSpecial = true;
                    }
                    
                    if (!isSpecial) {
                        // Generate Basic Attributes (1-5 count)
                        // Weighted probability for count:
                        // 1: 50%, 2: 30%, 3: 15%, 4: 4%, 5: 1%
                        int roll = random.nextInt(100);
                        int count = 1;
                        if (roll >= 99) count = 5;
                        else if (roll >= 95) count = 4;
                        else if (roll >= 80) count = 3;
                        else if (roll >= 50) count = 2;
                        
                        java.util.List<Integer> available = new java.util.ArrayList<>(java.util.Arrays.asList(0, 1, 2, 3, 4));
                        java.util.Collections.shuffle(available);
                        
                        for (int i = 0; i < count; i++) {
                            int attr = available.get(i);
                            switch (attr) {
                                case 0 -> vars.player_magic_attributes_earth = true;
                                case 1 -> vars.player_magic_attributes_water = true;
                                case 2 -> vars.player_magic_attributes_fire = true;
                                case 3 -> vars.player_magic_attributes_wind = true;
                                case 4 -> vars.player_magic_attributes_ether = true;
                            }
                        }
                    }

                    vars.syncPlayerVariables(player);
                    vars.syncMana(player);
                    
                    player.displayClientMessage(Component.literal("§b魔术回路已接通. 你现在是一名魔术师了."), false);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                    
                    if (world instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                    }
                } else if (type == 0) {
                    Manually_deduct_health_to_restore_mana.execute(entity);
                }
            }
        }
    }
}
