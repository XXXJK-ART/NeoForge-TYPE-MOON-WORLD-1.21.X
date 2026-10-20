package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaNobunagaActionAnimations.Action;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class OdaNobunagaActionAnimationClient {
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "oda_nobunaga_action"
   );

   private OdaNobunagaActionAnimationClient() {
   }

   @SubscribeEvent
   public static void registerPlayerAnimationLayer(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
         PLAYER_ANIMATION_LAYER, 1000, player -> new ModifierLayer<>()
      ));
   }

   public static void play(UUID playerId, int actionId) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || playerId == null) return;
      Action action = Action.byNetworkId(actionId);
      if (action == null) return;
      for (AbstractClientPlayer player : minecraft.level.players()) {
         if (player.getUUID().equals(playerId)) {
            play(player, action);
            return;
         }
      }
   }

   private static void play(AbstractClientPlayer player, Action action) {
      if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER)
         instanceof ModifierLayer<?> rawLayer)) {
         return;
      }
      ResourceLocation animationId = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, action.playerAnimationPath());
      if (!(PlayerAnimationRegistry.getAnimation(animationId) instanceof KeyframeAnimation animation)) return;
      @SuppressWarnings("unchecked")
      ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
      layer.setAnimation(new KeyframeAnimationPlayer(animation));
   }
}
