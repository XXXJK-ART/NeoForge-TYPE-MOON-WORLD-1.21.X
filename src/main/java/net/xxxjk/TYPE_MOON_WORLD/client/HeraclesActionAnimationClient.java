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

import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HeraclesActionAnimationClient {
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "heracles_action"
   );

   private HeraclesActionAnimationClient() {
   }

   @SubscribeEvent
   public static void registerPlayerAnimationLayer(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
         PLAYER_ANIMATION_LAYER, 1000, player -> ServantActionAnimationLayer.create()
      ));
   }

   public static void play(UUID playerId, int actionId) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null || playerId == null) return;
      if (actionId < -1 || actionId > 8) return;
      for (AbstractClientPlayer player : minecraft.level.players()) {
         if (player.getUUID().equals(playerId)) {
            play(player, actionId);
            return;
         }
      }
   }

   private static void play(AbstractClientPlayer player, int slot) {
      if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER)
         instanceof ModifierLayer<?> rawLayer)) {
         return;
      }
      if (slot == -1) { rawLayer.setAnimation(null); return; }
      ResourceLocation animationId = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "heracles_action_" + slot);
      if (!(PlayerAnimationRegistry.getAnimation(animationId) instanceof KeyframeAnimation animation)) return;
      @SuppressWarnings("unchecked")
      ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
      layer.setAnimation(new KeyframeAnimationPlayer(animation));
   }
}
