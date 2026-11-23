package net.xxxjk.TYPE_MOON_WORLD.servant;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.codec.AnimationCodecs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZhaoYunActionResourcesTest {
   private static final Path RES = Path.of("src/main/resources");

   @Test
   void changbanpoPlaysAtNaturalSpeedAndTracksTheFullClip() throws Exception {
      var policy = read("assets/typemoonworld/np_casts.json").getAsJsonObject("zhao_yun_changbanpo");
      var sound = read("assets/typemoonworld/sounds.json").getAsJsonObject("np_chant_zhao_yun_changbanpo")
         .getAsJsonArray("sounds").get(0).getAsJsonObject();
      assertEquals(1.0, sound.get("pitch").getAsDouble(), 0.001, "NP chant must not slow down the supplied recording");
      assertEquals(1.0, policy.get("pitch").getAsDouble(), 0.001);
      // ffprobe: the supplied recording lasts 15.998583 seconds at natural speed.
      assertEquals(320, policy.get("voice_ticks").getAsInt());
   }

   @Test
   void spearActionIdsResolveToDedicatedMotions() {
      assertEquals("zhao_yun_spear_thrust", ServantBaseAction.animationId(11, 0));
      assertEquals("zhao_yun_spear_sweep", ServantBaseAction.animationId(12, 0));
      assertEquals("zhao_yun_spear_dance", ServantBaseAction.animationId(13, 0));
      assertFalse(ServantBaseAction.isContinuous(13), "a short flourish must finish after one tap or NPC attack");
      assertTrue(ServantBaseAction.isContinuous(14), "held attacks must refresh until input stops");
      assertEquals("zhao_yun_spear_combo", ServantBaseAction.animationId(14, 0));
   }

   @Test
   void spearRemappingPreservesStopsAndNonSpearActions() {
      assertEquals(11, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 0));
      assertEquals(12, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 1));
      assertEquals(11, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 2));
      assertEquals(12, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 3));
      assertEquals(11, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 4));
      assertEquals(13, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 5));
      assertEquals(11, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD, 6));
      assertEquals(14, ServantBaseAction.zhaoYunAttack(ServantBaseAction.SWORD_COMBO, 0));
      for (int action : new int[]{ServantBaseAction.STOP, ServantBaseAction.FIST, 2, 8}) {
         assertEquals(action, ServantBaseAction.zhaoYunAttack(action, 2));
      }
   }

   @Test
   void skillDanceLoopsIndependentlyOfBasicInputAndRecoveryFinishes() throws Exception {
      assertTrue(ServantBaseAction.isContinuous(15), "skill 6 needs continuous playback");
      assertEquals("zhao_yun_spear_held_dance", ServantBaseAction.animationId(15, 0));
      assertFalse(ServantBaseAction.isContinuous(16), "release must finish its recovery");
      assertEquals("zhao_yun_spear_recovery", ServantBaseAction.animationId(16, 0));
      for (String name : List.of("held_dance", "recovery")) {
         try (var stream = Files.newInputStream(RES.resolve("assets/typemoonworld/player_animations/zhao_yun_spear_" + name + ".json"))) {
            KeyframeAnimation clip = (KeyframeAnimation)AnimationCodecs.deserialize("json", stream).iterator().next();
            assertEquals(name.equals("held_dance"), clip.isInfinite, name);
            var player = new KeyframeAnimationPlayer(clip);
            for (int tick = 0; tick < clip.endTick * 3; tick++) player.tick();
            assertEquals(name.equals("held_dance"), player.isActive(), name);
         }
      }
   }

   @Test
   void realAnimationCodecKeepsLocomotionAndOnlyLoopsHeldActions() throws Exception {
      for (String name : List.of("thrust", "sweep", "dance", "combo", "held_dance", "recovery")) {
         try (var stream = Files.newInputStream(RES.resolve("assets/typemoonworld/player_animations/zhao_yun_spear_" + name + ".json"))) {
            KeyframeAnimation clip = (KeyframeAnimation)AnimationCodecs.deserialize("json", stream).iterator().next();
            boolean loop = name.equals("combo") || name.equals("held_dance");
            assertEquals(loop, clip.isInfinite, name);
            var player = new KeyframeAnimationPlayer(clip);
            for (int tick = 0; tick < clip.endTick * 3; tick++) {
               player.setupAnim(0.5F);
               for (String bone : List.of("head", "leftLeg", "rightLeg")) {
                  var original = new Vec3f(0.3F, -0.2F, 0.1F);
                  var actual = player.get3DTransform(bone, TransformType.ROTATION, 0.5F, original);
                  assertEquals(original.getX(), actual.getX(), 1.0E-5, name + ":" + bone);
                  assertEquals(original.getY(), actual.getY(), 1.0E-5);
                  assertEquals(original.getZ(), actual.getZ(), 1.0E-5);
               }
               player.tick();
            }
            assertEquals(loop, player.isActive(), name);
         }
      }
   }

   @Test
   void attackVoiceEventAndAllProvidedClipSlotsExist() throws Exception {
      JsonObject sounds = read("assets/typemoonworld/sounds.json");
      JsonObject attack = sounds.getAsJsonObject("zhao_yun_voice_attack");
      assertEquals(3, attack.getAsJsonArray("sounds").size());
      for (String clip : List.of("attack1", "attack2", "attack3", "fail1", "fail2", "victory1", "victory2", "np")) {
         assertTrue(Files.isRegularFile(RES.resolve("assets/typemoonworld/sounds/voice/zhao_yun/" + clip + ".ogg")), clip);
      }
   }

   @Test
   void spearMotionsHaveStationaryRootAndCompleteKeyframes() throws Exception {
      for (String action : List.of("zhao_yun_spear_thrust", "zhao_yun_spear_sweep", "zhao_yun_spear_dance")) {
         JsonObject emote = read("assets/typemoonworld/player_animations/" + action + ".json").getAsJsonObject("emote");
         var moves = emote.getAsJsonArray("moves");
         assertTrue(moves.size() >= 5, action + " needs enough poses to read as a weapon action");
         assertEquals(0, moves.get(0).getAsJsonObject().get("tick").getAsInt(), action);
         assertEquals(emote.get("endTick").getAsInt(), moves.get(moves.size() - 1).getAsJsonObject().get("tick").getAsInt(), action);
         for (var value : moves) {
            var pose = value.getAsJsonObject();
            for (String bone : List.of("body", "torso")) {
               for (String axis : List.of("x", "y", "z")) assertEquals(0, pose.getAsJsonObject(bone).get(axis).getAsDouble(), action + ":" + bone);
            }
         }
      }
   }

   @Test
   void hakuryuGallopHasPronouncedBodyLegAndTailMotion() throws Exception {
      JsonObject gallop = read("assets/typemoonworld/animations/zhao_yun_hakuryu.animation.json")
         .getAsJsonObject("animations").getAsJsonObject("gallop").getAsJsonObject("bones");
      assertTrue(Math.abs(gallop.getAsJsonObject("body").getAsJsonObject("rotation")
         .getAsJsonObject("0.125").getAsJsonArray("vector").get(0).getAsDouble()) >= 5.0);
      assertTrue(Math.abs(gallop.getAsJsonObject("Left front leg").getAsJsonObject("rotation")
         .getAsJsonObject("0.125").getAsJsonArray("vector").get(0).getAsDouble()) >= 50.0);
      assertTrue(Math.abs(gallop.getAsJsonObject("Tail").getAsJsonObject("rotation")
         .getAsJsonObject("0.25").getAsJsonArray("vector").get(1).getAsDouble()) >= 20.0);
   }

   private static JsonObject read(String path) throws Exception {
      return JsonParser.parseString(Files.readString(RES.resolve(path))).getAsJsonObject();
   }
}
