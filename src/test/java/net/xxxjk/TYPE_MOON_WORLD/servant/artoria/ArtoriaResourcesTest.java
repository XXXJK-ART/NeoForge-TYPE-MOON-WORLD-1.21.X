package net.xxxjk.TYPE_MOON_WORLD.servant.artoria;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtoriaResourcesTest {
   private static JsonObject json(String file) throws Exception {
      return JsonParser.parseString(Files.readString(Path.of("src/main/resources/" + file))).getAsJsonObject();
   }
   @Test void identityAndParametersUseFgoSaber() throws Exception {
      var data = json("data/typemoonworld/servant/definitions/artoria_pendragon.json");
      assertEquals("earth", data.get("faction").getAsString());
      assertEquals("mythology", data.get("era").getAsString());
      var traits = data.getAsJsonArray("traits").asList().stream().map(v -> v.getAsString()).collect(Collectors.toSet());
      assertTrue(traits.containsAll(java.util.Set.of("arthur", "dragon", "human_kind", "round_table", "seven_knights", "heaven_or_earth_servant")));
      assertFalse(traits.contains("living_human"));
      var params = data.getAsJsonObject("parameters");
      assertEquals("B", params.get("strength").getAsString());
      assertEquals("B", params.get("endurance").getAsString());
      assertEquals("B", params.get("agility").getAsString());
      assertEquals("A++", params.get("noble_phantasm_rank").getAsString());
      assertTrue(params.get("luck_plus").getAsBoolean());
   }
   @Test void threeNoblePhantasmsKeepIndependentClassifications() throws Exception {
      var wind = json("data/typemoonworld/servant/noble_phantasms/invisible_air.json");
      assertEquals("barrier", wind.get("type").getAsString());
      var beam = json("data/typemoonworld/servant/noble_phantasms/excalibur_beam.json");
      assertEquals("castle", beam.get("type").getAsString());
      assertEquals(500, beam.get("pulse_damage").getAsInt());
      assertEquals(15, beam.get("pulse_count").getAsInt());
      assertEquals(152, beam.get("windup_ticks").getAsInt());
      assertEquals(212, json("assets/typemoonworld/np_casts.json").getAsJsonObject("artoria_excalibur").get("voice_ticks").getAsInt());
   }
   @Test void instinctAndLeadershipDoNotRetainOldActiveBuffs() throws Exception {
      var instinct = json("data/typemoonworld/servant/skills/artoria_instinct_a.json");
      assertEquals(.5, instinct.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params").get("certain_hit_or_death_chance").getAsDouble());
      assertEquals(0, json("data/typemoonworld/servant/skills/leadership_b.json").getAsJsonArray("effects").size());
      assertEquals("Arthur", json("assets/typemoonworld/lang/en_us.json").get("trait.typemoonworld.arthur").getAsString());
   }
   @Test void manaBurstBladeEffectUsesFullBladeBindingAndGoldenTrail() throws Exception {
      var effect = json("assets/typemoonworld/effects/artoria_mana_burst_blade.json");
      assertTrue(effect.get("duration").getAsFloat() >= 1.0F);
      var emitters = effect.getAsJsonArray("emitters");
      assertTrue(emitters.size() >= 2);
      for (var emitter : emitters) {
         var data = emitter.getAsJsonObject();
         assertEquals("artoria_blade", data.getAsJsonObject("binding").get("mode").getAsString());
         assertTrue(data.get("enable_trail").getAsBoolean());
      }
      assertEquals("MANA_BURST_EXCALIBUR_WAKE", emitters.get(1).getAsJsonObject().get("particle_role").getAsString());
      var bladeSamples = emitters.get(0).getAsJsonObject().getAsJsonObject("component");
      assertEquals("parametric", bladeSamples.get("type").getAsString());
      assertEquals(72, bladeSamples.get("segments").getAsInt());
   }
   @Test void invisibleAirHasVisibleTranslucentBladeParticles() throws Exception {
      var effect = json("assets/typemoonworld/effects/artoria_invisible_air.json");
      var emitters = effect.getAsJsonArray("emitters");
      assertEquals(3, emitters.size());
      for (var emitter : emitters) {
         var data = emitter.getAsJsonObject();
         assertEquals("artoria_blade", data.getAsJsonObject("binding").get("mode").getAsString());
         var color = data.getAsJsonArray("color_over_life").get(1).getAsJsonObject().get("color").getAsString();
         assertTrue(Integer.parseInt(color.substring(1, 3), 16) >= 70, "Invisible Air particle alpha is too faint");
      }
   }
   @Test void manaBurstUsesTripledStartAndSustainCosts() throws Exception {
      var skill = json("data/typemoonworld/servant/skills/mana_burst_a.json");
      assertEquals(ArtoriaRules.BURST_START_MP, skill.get("mp_cost").getAsInt());
      assertEquals(ArtoriaRules.BURST_DRAIN_MP,
         skill.getAsJsonArray("effects").get(0).getAsJsonObject().getAsJsonObject("params").get("drain_per_second").getAsInt());
   }
   @Test void windEffectsUseDenseTornadoAndForwardBlastLayers() throws Exception {
      var hammer = json("assets/typemoonworld/effects/artoria_wind_hammer.json").getAsJsonArray("emitters");
      assertTrue(hammer.size() >= 4);
      assertTrue(hammer.get(0).getAsJsonObject().get("rate").getAsInt() >= 1000);
      assertTrue(hammer.get(1).getAsJsonObject().get("particle_role").getAsString().contains("ROTATING"));
      assertTrue(hammer.get(hammer.size() - 1).getAsJsonObject().get("particle_role").getAsString().contains("FRONT"));
      assertTrue(json("assets/typemoonworld/effects/artoria_wind_release.json").getAsJsonArray("emitters").size() >= 3);
      assertTrue(json("assets/typemoonworld/effects/artoria_wind_defense.json").getAsJsonArray("emitters").get(0).getAsJsonObject().get("rate").getAsInt() >= 400);
   }
}
