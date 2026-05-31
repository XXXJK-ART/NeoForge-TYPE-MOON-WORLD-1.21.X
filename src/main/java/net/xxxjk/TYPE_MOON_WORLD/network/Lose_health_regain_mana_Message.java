package net.xxxjk.TYPE_MOON_WORLD.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicCircuitColorHelper;
import net.xxxjk.TYPE_MOON_WORLD.procedures.Manually_deduct_health_to_restore_mana;
import org.jetbrains.annotations.NotNull;

public record Lose_health_regain_mana_Message(int eventType, int pressed) implements CustomPacketPayload {
   public static final Type<Lose_health_regain_mana_Message> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("typemoonworld", "key_losehealthregainmana"));
   public static final StreamCodec<RegistryFriendlyByteBuf, Lose_health_regain_mana_Message> STREAM_CODEC = StreamCodec.of((buffer, message) -> {
      buffer.writeInt(message.eventType);
      buffer.writeInt(message.pressed);
   }, buffer -> new Lose_health_regain_mana_Message(buffer.readInt(), buffer.readInt()));

   @NotNull
   public Type<Lose_health_regain_mana_Message> type() {
      return TYPE;
   }

   public static void handleData(Lose_health_regain_mana_Message message, IPayloadContext context) {
      if (context.flow() == PacketFlow.SERVERBOUND) {
         context.enqueueWork(() -> pressAction(context.player(), message.eventType, message.pressed)).exceptionally(e -> {
            TYPE_MOON_WORLD.LOGGER.error("Failed to handle Lose_health_regain_mana_Message", e);
            return null;
         });
      }
   }

   public static void pressAction(Player entity, int type, int pressed) {
      Level world = entity.level();
      if (world.isLoaded(entity.blockPosition()) && entity instanceof ServerPlayer player) {
         TypeMoonWorldModVariables.PlayerVariables vars = (TypeMoonWorldModVariables.PlayerVariables)player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.is_magus) {
            boolean hasModItem = false;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
               ItemStack stack = player.getInventory().getItem(i);
               if (!stack.isEmpty()) {
                  ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                  if (id != null && "typemoonworld".equals(id.getNamespace())) {
                     hasModItem = true;
                     break;
                  }
               }
            }

            if (!hasModItem) {
               player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.missing_catalyst"), true);
               return;
            }

            vars.is_magus = true;
            Random random = new Random();
            vars.player_max_mana = Math.round((100.0 + random.nextDouble() * 900.0) * 10.0) / 10.0;
            vars.player_mana = vars.player_max_mana;
            double regen = 1.0 + random.nextDouble() * 9.0;
            vars.player_mana_egenerated_every_moment = Math.round(regen * 10.0) / 10.0;
            double restore = 1.0 + random.nextDouble() * 9.0;
            vars.player_restore_magic_moment = Math.round(restore * 10.0) / 10.0;
            vars.player_magic_attributes_earth = false;
            vars.player_magic_attributes_water = false;
            vars.player_magic_attributes_fire = false;
            vars.player_magic_attributes_wind = false;
            vars.player_magic_attributes_ether = false;
            vars.player_magic_attributes_none = false;
            vars.player_magic_attributes_imaginary_number = false;
            boolean isSpecial = false;
            if (random.nextInt(100) < 10) {
               if (random.nextBoolean()) {
                  vars.player_magic_attributes_none = true;
               } else {
                  vars.player_magic_attributes_imaginary_number = true;
               }

               isSpecial = true;
            }

            if (!isSpecial) {
               int roll = random.nextInt(100);
               int count = 1;
               if (roll >= 99) {
                  count = 5;
               } else if (roll >= 95) {
                  count = 4;
               } else if (roll >= 80) {
                  count = 3;
               } else if (roll >= 50) {
                  count = 2;
               }

               List<Integer> available = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
               Collections.shuffle(available);

               for (int ix = 0; ix < count; ix++) {
                  int attr = available.get(ix);
                  switch (attr) {
                     case 0:
                        vars.player_magic_attributes_earth = true;
                        break;
                     case 1:
                        vars.player_magic_attributes_water = true;
                        break;
                     case 2:
                        vars.player_magic_attributes_fire = true;
                        break;
                     case 3:
                        vars.player_magic_attributes_wind = true;
                        break;
                     case 4:
                        vars.player_magic_attributes_ether = true;
                  }
               }
            }

            vars.magic_circuit_color_rgb = MagicCircuitColorHelper.resolveColor(vars);
            vars.syncPlayerVariables(player);
            player.displayClientMessage(Component.translatable("message.typemoonworld.awaken.success"), false);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (world instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            }
         } else if (type == 0) {
            Manually_deduct_health_to_restore_mana.execute(entity);
         }
      }
   }
}
