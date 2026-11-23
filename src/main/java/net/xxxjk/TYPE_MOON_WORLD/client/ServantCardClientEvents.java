package net.xxxjk.TYPE_MOON_WORLD.client;

import com.mojang.math.Axis;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterCarryService;

@EventBusSubscriber(value = {Dist.CLIENT})
public final class ServantCardClientEvents {
   private static final java.util.IdentityHashMap<PlayerModel<?>, boolean[]> MASK_ONLY_MODELS = new java.util.IdentityHashMap<>();
   private ServantCardClientEvents() {
   }

   @SubscribeEvent
   public static void onComputeFov(ComputeFovModifierEvent event) {
      if (event.getPlayer() != Minecraft.getInstance().player) {
         return;
      }
      if (event.getPlayer().getVehicle() instanceof HugeSeaMonsterEntity) {
         event.setNewFovModifier(event.getNewFovModifier() * 4.0F);
         return;
      }
      ItemStack using = event.getPlayer().getUseItem();
      if (!using.is(ModItems.NAMELESS_BOW.get()) && !using.is(ModItems.ARASH_BOW.get())) {
         return;
      }
      int useTicks = event.getPlayer().getTicksUsingItem();
      if (useTicks < 20) {
         return;
      }
      int zoomSteps = Math.min(3, (useTicks - 20) / 10);
      float magnification = 2.0F + zoomSteps;
      float zoom = 1.0F / magnification;
      event.setNewFovModifier(event.getNewFovModifier() * zoom);
   }

   @SubscribeEvent
   public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
      if (ServantCardConcealmentClient.isShadowHassanMaskOnly(event.getEntity())) {
         hideBaseModel(event.getRenderer());
      } else if (ServantCardConcealmentClient.isPerfectlyConcealed(event.getEntity())) {
         event.setCanceled(true);
      } else if (ServantMasterCarryService.isCarriedMaster(event.getEntity())) {
         event.getPoseStack().translate(0.0, 0.62, 0.0);
         event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(72.0F));
         event.getPoseStack().translate(0.0, -0.62, 0.0);
      }
   }

   @SubscribeEvent
   public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
      PlayerModel<?> model = event.getRenderer().getModel();
      boolean[] previous = MASK_ONLY_MODELS.remove(model);
      if (previous != null) restoreBaseModel(model, previous);
   }

   private static void hideBaseModel(PlayerRenderer renderer) {
      PlayerModel<?> model = renderer.getModel();
      boolean[] previous = new boolean[]{model.head.visible, model.hat.visible, model.body.visible,
         model.rightArm.visible, model.leftArm.visible, model.rightLeg.visible, model.leftLeg.visible,
         model.rightSleeve.visible, model.leftSleeve.visible, model.rightPants.visible, model.leftPants.visible,
         model.jacket.visible};
      MASK_ONLY_MODELS.put(model, previous);
      for (ModelPart part : new ModelPart[]{model.head, model.hat, model.body, model.rightArm, model.leftArm,
         model.rightLeg, model.leftLeg, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants, model.jacket}) {
         part.visible = false;
      }
   }

   private static void restoreBaseModel(PlayerModel<?> model, boolean[] previous) {
      ModelPart[] parts = new ModelPart[]{model.head, model.hat, model.body, model.rightArm, model.leftArm,
         model.rightLeg, model.leftLeg, model.rightSleeve, model.leftSleeve, model.rightPants, model.leftPants, model.jacket};
      for (int i = 0; i < parts.length; i++) parts[i].visible = previous[i];
   }

   @SubscribeEvent
   public static void onRenderNameTag(RenderNameTagEvent event) {
      if (event.getEntity() instanceof Player player && ServantCardConcealmentClient.isPerfectlyConcealed(player)) {
         event.setCanRender(TriState.FALSE);
      }
   }
}
