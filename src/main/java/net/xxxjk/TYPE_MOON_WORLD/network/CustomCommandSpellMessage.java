package net.xxxjk.TYPE_MOON_WORLD.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.MasterServantLinkService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.typemoonworld.api.CommandSpellContext;
import net.xxxjk.typemoonworld.api.ExecutionResult;
import net.xxxjk.typemoonworld.api.event.CommandSpellEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Payload for addon command spells; legacy integer command actions remain supported. */
public record CustomCommandSpellMessage(ResourceLocation id, int spellIndex) implements CustomPacketPayload {
   public static final Type<CustomCommandSpellMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "custom_command_spell"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CustomCommandSpellMessage> STREAM_CODEC = StreamCodec.of(
      (buffer, value) -> { buffer.writeResourceLocation(value.id); buffer.writeVarInt(Math.max(0, Math.min(2, value.spellIndex))); },
      buffer -> new CustomCommandSpellMessage(buffer.readResourceLocation(), buffer.readVarInt()));
   @Override public Type<CustomCommandSpellMessage> type() { return TYPE; }
   public static void handleData(CustomCommandSpellMessage message, IPayloadContext context) {
      if (context.flow() != PacketFlow.SERVERBOUND) return;
      context.enqueueWork(() -> {
         if (!(context.player() instanceof ServerPlayer player) || message.id == null || !message.id.toString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return;
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         if (!vars.master_active || vars.master_command_spells <= 0) return;
         var servant = MasterServantLinkService.getLinkedServant(player, vars);
         CommandSpellContext spellContext = new CommandSpellContext(player, servant, message.spellIndex, player.level().getGameTime());
         var pre = NeoForge.EVENT_BUS.post(new CommandSpellEvent.Pre(message.id, spellContext));
         if (pre.isCanceled()) return;
         ExecutionResult result = ExtensionApiRegistry.command(message.id, spellContext);
         if (result.handled() && result.success()) {
            int cost = Math.max(1, (int)Math.ceil(result.resourceCost()));
            if (vars.master_command_spells < cost) return;
            vars.master_command_spells -= cost; vars.syncPlayerVariables(player);
         }
         NeoForge.EVENT_BUS.post(new CommandSpellEvent.Post(message.id, spellContext, result));
      });
   }
}
