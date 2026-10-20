package net.xxxjk.TYPE_MOON_WORLD.client;

import java.util.UUID;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.sasaki.SasakiKojiroAction;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class SasakiKojiroActionAnimationClient {
   private static final ResourceLocation PLAYER_ANIMATION_LAYER = ResourceLocation.fromNamespaceAndPath(
      TYPE_MOON_WORLD.MOD_ID, "sasaki_kojiro_action");

   private SasakiKojiroActionAnimationClient() { }

   @SubscribeEvent
   public static void registerPlayerAnimationLayer(FMLClientSetupEvent event) {
      event.enqueueWork(() -> PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
         PLAYER_ANIMATION_LAYER, 1000, player -> new ModifierLayer<>()));
   }

   public static void play(UUID playerId, int actionId) {
      Minecraft minecraft = Minecraft.getInstance();
      SasakiKojiroAction action = SasakiKojiroAction.byNetworkId(actionId);
      if (minecraft.level == null || playerId == null || action == null) return;
      if (action == SasakiKojiroAction.TSUBAME_GAESHI) {
         for (var entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity owner && playerId.equals(owner.getUUID())) {
               SasakiTsubameAfterimageClient.begin(owner);
               break;
            }
         }
      }
      for (AbstractClientPlayer player : minecraft.level.players()) {
         if (playerId.equals(player.getUUID())) {
            play(player, action);
            return;
         }
      }
   }

   private static void play(AbstractClientPlayer player, SasakiKojiroAction action) {
      if (!(PlayerAnimationAccess.getPlayerAssociatedData(player).get(PLAYER_ANIMATION_LAYER) instanceof ModifierLayer<?> rawLayer)) return;
      ResourceLocation animationId = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, action.playerAnimationPath());
      if (!(PlayerAnimationRegistry.getAnimation(animationId) instanceof KeyframeAnimation animation)) return;
      @SuppressWarnings("unchecked")
      ModifierLayer<KeyframeAnimationPlayer> layer = (ModifierLayer<KeyframeAnimationPlayer>)rawLayer;
      layer.setAnimation(new KeyframeAnimationPlayer(animation));
   }
}
