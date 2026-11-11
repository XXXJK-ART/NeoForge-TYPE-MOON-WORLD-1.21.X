package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantBaseAnimationMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;

@EventBusSubscriber(modid = "typemoonworld", value = Dist.CLIENT)
public final class ServantBaseAttackInputClient {
   private static ServantBaseAttackInput input = new ServantBaseAttackInput();
   private static Object activeLevel;

   private ServantBaseAttackInputClient() { }

   public static void attack() {
      Minecraft mc = Minecraft.getInstance();
      resetLevel(mc);
      if (mc.player != null && mc.level != null) {
         send(input.attack(mc.player.getMainHandItem().isEmpty(), mc.level.getGameTime()));
      }
   }

   public static boolean acceptsContinuousPlayback() { return input.isContinuous(); }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      resetLevel(mc);
      if (mc.player == null || mc.level == null) return;
      boolean allowed = mc.screen == null && mc.isWindowActive() && mc.player.isAlive()
         && mc.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES).servant_card_transformed
         && !mc.player.isUsingItem() && !ServantGuardPoseClient.isGuarding(mc.player)
         && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK);
      int action = input.tick(mc.options.keyAttack.isDown(), allowed, mc.level.getGameTime());
      send(action);
      ServantBaseAnimationClient.tick();
   }

   private static void send(int action) {
      Minecraft mc = Minecraft.getInstance();
      if (action == ServantBaseAction.NONE || mc.player == null) return;
      if (action == ServantBaseAction.STOP) ServantBaseAnimationClient.stopAttack(mc.player.getUUID());
      PacketDistributor.sendToServer(new ServantBaseAnimationMessage(mc.player.getUUID(), action));
   }

   private static void resetLevel(Minecraft mc) {
      if (activeLevel != mc.level) { input = new ServantBaseAttackInput(); activeLevel = mc.level; }
   }
}
