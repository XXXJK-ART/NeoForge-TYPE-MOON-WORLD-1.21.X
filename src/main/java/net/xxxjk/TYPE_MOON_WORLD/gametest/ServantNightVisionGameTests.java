package net.xxxjk.TYPE_MOON_WORLD.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;
import net.xxxjk.TYPE_MOON_WORLD.servant.perception.ServantPassiveService;

@GameTestHolder("typemoonworld_perception")
@PrefixGameTestTemplate(false)
public final class ServantNightVisionGameTests {
   private ServantNightVisionGameTests() { }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void npcNightVisionStaysAboveTheVanillaBlinkWindow(GameTestHelper helper) {
      var npc = ModEntities.ARASH.get().create(helper.getLevel());
      npc.setPos(helper.absolutePos(new BlockPos(8, 8, 8)).getCenter());
      npc.setNoAi(true); npc.setNoGravity(true);
      helper.getLevel().addFreshEntity(npc);
      ServantPassiveService.tick(new EntityTickEvent.Post(npc));
      helper.assertTrue(npc.getEffect(MobEffects.NIGHT_VISION).getDuration() == 1200,
         "Servant night vision did not start with sixty seconds");
      for (int tick = 0; tick < 1300; tick++) {
         npc.tick();
         ServantPassiveService.tick(new EntityTickEvent.Post(npc));
         var effect = npc.getEffect(MobEffects.NIGHT_VISION);
         helper.assertTrue(effect != null && effect.getDuration() > 200,
            "Servant night vision entered the vanilla blink window before refresh");
      }
      npc.discard();
      helper.succeed();
   }

   @GameTest(template = "ancient_temple", timeoutTicks = 30)
   public static void cardReleaseClearsManagedNightVisionAndPreservesPotions(GameTestHelper helper) {
      var player = helper.makeMockServerPlayerInLevel();
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Card transform failed");
      player.removeEffect(MobEffects.NIGHT_VISION);
      ServantPassiveService.tick(new EntityTickEvent.Post(player));
      helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 1200,
         "Player servant night vision did not start with sixty seconds");
      ServantCardTransformManager.release(player, false);
      ServantPassiveService.tick(new EntityTickEvent.Post(player));
      helper.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Managed night vision remained after card release");
      player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0));
      helper.assertTrue(ServantCardTransformManager.transform(player, "arash"), "Second card transform failed");
      ServantPassiveService.tick(new EntityTickEvent.Post(player));
      ServantCardTransformManager.release(player, false);
      ServantPassiveService.tick(new EntityTickEvent.Post(player));
      helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION)
         && player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 2400, "Card release removed an external night vision potion");
      player.discard();
      helper.succeed();
   }
}
