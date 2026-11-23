package net.xxxjk.TYPE_MOON_WORLD.servant.sasaki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SasakiKojiroResourcesTest {
   private static final Path RESOURCES = Path.of("src/main/resources");

   @Test
   void definitionMatchesTheFgoBaselineAndHasNoNoblePhantasm() throws Exception {
      JsonObject definition = json("data/typemoonworld/servant/definitions/sasaki_kojiro.json");
      assertEquals("assassin", definition.get("class_type").getAsString());
      assertEquals("human", definition.get("faction").getAsString());
      assertEquals("age_of_sail", definition.get("era").getAsString());
      assertEquals("", definition.get("noble_phantasm").getAsString());
      JsonObject params = definition.getAsJsonObject("parameters");
      assertEquals("C", params.get("strength").getAsString());
      assertEquals("E", params.get("endurance").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertTrue(params.get("agility_plus").getAsBoolean());
      assertEquals("E", params.get("magic").getAsString());
      assertEquals("A", params.get("luck").getAsString());
      assertEquals("", params.get("noble_phantasm_rank").getAsString());
      String traits = definition.getAsJsonArray("traits").toString();
      for (String trait : List.of("male", "human", "humanoid", "human_kind", "servant", "seven_knights", "neutral_alignment", "evil")) {
         assertTrue(traits.contains("\"" + trait + "\""), trait);
      }
      assertFalse(traits.contains("chaotic"));
      assertFalse(traits.contains("living_human"));
      assertEquals("", definition.getAsJsonObject("model").get("geometry").getAsString());
   }

   @Test
   void passiveRanksAndCardAnimationResourcesAreDefined() throws Exception {
      JsonObject mindEye = json("data/typemoonworld/servant/skills/mindseye_a.json");
      assertEquals("passive", mindEye.get("type").getAsString());
      assertTrue(mindEye.getAsJsonObject("defense").get("evasion").getAsBoolean());
      assertEquals("passive", json("data/typemoonworld/servant/skills/souwa_expertise_b.json").get("type").getAsString());
      assertEquals("active", json("data/typemoonworld/servant/skills/stealth_d.json").get("type").getAsString());
      assertEquals(18, json("data/typemoonworld/servant/skills/stealth_d.json").get("mp_cost").getAsInt());
      JsonObject transparency = json("data/typemoonworld/servant/skills/spiritualization_b_plus.json");
      assertEquals(0, transparency.get("duration_ticks").getAsInt());
      assertEquals("teleport_behind", transparency.getAsJsonArray("effects").get(1).getAsJsonObject().get("effect_type").getAsString());
      assertEquals(8.0, transparency.getAsJsonArray("effects").get(1).getAsJsonObject().getAsJsonObject("params").get("range").getAsDouble());

      for (SasakiKojiroAction action : SasakiKojiroAction.values()) {
         JsonObject animation = json("assets/typemoonworld/player_animations/" + action.playerAnimationPath() + ".json");
         assertEquals(action.playerAnimationPath(), animation.get("name").getAsString());
         assertTrue(animation.getAsJsonObject("emote").getAsJsonArray("moves").size() >= 3, action.name());
      }
      JsonObject sourceAnimation = json("assets/typemoonworld/animations/sasaki_tsubame_afterimages.animation.json")
         .getAsJsonObject("animations").getAsJsonObject("yanfan");
      assertEquals(0.625, sourceAnimation.get("animation_length").getAsDouble(), 0.0001);
      JsonObject sourceBones = sourceAnimation.getAsJsonObject("bones");
      for (String bone : List.of("Waist", "Waist2", "Waist3", "jian", "jian2", "jian3")) {
         assertTrue(sourceBones.has(bone), bone);
      }
      JsonObject tsubameEffect = json("assets/typemoonworld/effects/sasaki_tsubame.json");
      for (var emitter : tsubameEffect.getAsJsonArray("emitters")) {
         JsonObject effect = emitter.getAsJsonObject();
         assertEquals("BILLBOARD", effect.get("renderer").getAsString());
         assertTrue(effect.get("enable_trail").getAsBoolean());
         assertTrue(effect.getAsJsonArray("velocity").get(2).getAsDouble() > 0.0);
         assertTrue(effect.get("rate").getAsDouble() * effect.get("end_time").getAsDouble() <= 8,
            "Release sparks must not form an independent slash or spiral");
         assertTrue(effect.getAsJsonObject("component").get("radius").getAsDouble() <= 0.1,
            "Release sparks must stay at the grip; the client supplies the tip trails");
         assertEquals("typemoonworld:textures/particle/sasaki_tsubame_slash.png",
            effect.getAsJsonObject("material").get("texture").getAsString());
      }
      BufferedImage blade = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_blade_arc.png").toFile());
      assertNotNull(blade);
      BufferedImage tsubameSlash = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_tsubame_slash.png").toFile());
      assertNotNull(tsubameSlash);
      JsonObject actions = json("data/typemoonworld/servant/actions/sasaki_kojiro.json");
      assertFalse(actions.toString().contains("tsubame_gaeshi"));
      assertFalse(Files.readString(RESOURCES.resolve("data/minecraft/tags/damage_type/bypasses_invulnerability.json"))
         .contains("typemoonworld:ganryu_tsubame"));
   }

   @Test
   void restoredSasakiVoiceResourcesArePresent() throws Exception {
      for (String file : List.of("attack1.ogg", "attack2.ogg", "attack3.ogg", "fail1.ogg", "fail2.ogg",
         "victory1.ogg", "victory2.ogg", "tsurigameshi.ogg")) {
         Path voice = RESOURCES.resolve("assets/typemoonworld/sounds/voice/sasaki_kojiro").resolve(file);
         assertTrue(Files.isRegularFile(voice) && Files.size(voice) > 0, file);
      }
      String sounds = Files.readString(RESOURCES.resolve("assets/typemoonworld/sounds.json"));
      for (String event : List.of("sasaki_kojiro_voice_attack", "sasaki_kojiro_voice_fail",
         "sasaki_kojiro_voice_victory", "sasaki_kojiro_voice_tsurigameshi")) {
         assertTrue(sounds.contains("\"" + event + "\""), event);
      }
   }

   @Test
   void reimportedActionsContainFullBodyTracksAndTheTenTickSourceStance() throws Exception {
      JsonObject source = json("assets/typemoonworld/animations/sasaki_kojiro_source.animation.json")
         .getAsJsonObject("animations");
      for (SasakiKojiroAction action : SasakiKojiroAction.values()) {
         if (action == SasakiKojiroAction.TSUBAME_GAESHI) continue;
         if (action == SasakiKojiroAction.OKITA_MUMYOUDAN_ZUKI || action == SasakiKojiroAction.OKITA_THRUST) continue;
         JsonObject animation = json("assets/typemoonworld/player_animations/" + action.playerAnimationPath() + ".json");
         assertTrue(animation.has("source_file") && animation.has("source_animation"), action.name());
         assertTrue(source.has(action.animationName()), action.name());
         JsonObject emote = animation.getAsJsonObject("emote");
         assertEquals(action.durationTicks(), emote.get("stopTick").getAsInt());
         assertEquals(action.durationTicks() + 1, emote.getAsJsonArray("moves").size());
         for (var move : emote.getAsJsonArray("moves")) {
            for (String part : List.of("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "rightItem", "leftItem")) {
               JsonObject pose = move.getAsJsonObject().getAsJsonObject(part);
               for (String axis : List.of("x", "y", "z", "pitch", "yaw", "roll"))
                  assertTrue(pose.has(axis) && Double.isFinite(pose.get(axis).getAsDouble()), action + ":" + part + ":" + axis);
            }
         }
      }
      JsonObject okitaAnimation = json("assets/typemoonworld/player_animations/okita_mumyoudan_zuki.json");
      assertEquals("okita_mumyoudan_zuki", okitaAnimation.get("name").getAsString());
      assertEquals(15, okitaAnimation.getAsJsonObject("emote").get("stopTick").getAsInt());
      assertEquals(16, okitaAnimation.getAsJsonObject("emote").getAsJsonArray("moves").size());
      JsonObject okitaSource = json("assets/typemoonworld/animations/okita_souji_saber_source.animation.json")
         .getAsJsonObject("animations");
      assertTrue(okitaSource.has("mumyoudan_zuki"));
      JsonObject windup = json("assets/typemoonworld/player_animations/sasaki_kojiro_tsubame_windup.json");
      assertTrue(windup.get("source_file").getAsString().contains("xiaocilangyanliujiashi.animation.json"));
      assertEquals(10, windup.getAsJsonObject("emote").get("endTick").getAsInt());
      assertEquals(0.3333, windup.get("source_length").getAsDouble(), 0.0001);
   }

   @Test
   void mainBladeUsesTheStretchedDedicatedAtlasRegionAndSwordSoundsResolve() throws Exception {
      BufferedImage source = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_tsubame_slash.png").toFile());
      BufferedImage stretched = ImageIO.read(RESOURCES.resolve("assets/typemoonworld/textures/particle/sasaki_tsubame_blade_stretched.png").toFile());
      assertEquals(512, stretched.getWidth());
      assertEquals(96, stretched.getHeight());
      // The stretched image must keep the original purple crescent and its alpha, not the blank atlas margins.
      int opaque = 0, purple = 0;
      for (int y = 0; y < stretched.getHeight(); y++) for (int x = 0; x < stretched.getWidth(); x++) {
         int color = stretched.getRGB(x, y);
         if ((color >>> 24) > 100) {
            opaque++;
            if ((color & 255) > ((color >> 8) & 255) + 20) purple++;
         }
      }
      assertTrue(opaque > 4000 && purple > 1000);
      assertNotNull(source);
      JsonObject sounds = json("assets/typemoonworld/sounds.json");
      for (String event : List.of("sasaki_blade_draw", "sasaki_blade_swing", "sasaki_blade_impact", "sasaki_tsubame_release")) {
         assertTrue(sounds.getAsJsonObject(event).getAsJsonArray("sounds").size() > 0, event);
      }
   }

   private static JsonObject json(String path) throws Exception {
      Path file = RESOURCES.resolve(path);
      try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      }
   }
}
