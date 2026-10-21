package net.xxxjk.TYPE_MOON_WORLD.servant.diarmuid;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiarmuidResourcesTest {
   private static final Path RES = Path.of("src/main/resources");
   private static JsonObject json(String path) throws Exception {
      return JsonParser.parseString(Files.readString(RES.resolve(path))).getAsJsonObject();
   }

   @Test void completeSharedSkillBarAndFinisher() {
      int[] mana = {6,12,10,14,14,16,12,18,16,24,36};
      int[] cooldowns = {20,80,70,100,100,120,100,260,180,180,400};
      int[] ends = {14,14,13,17,16,17,17,28,20,26,40};
      for (int slot = -1; slot <= 9; slot++) {
         DiarmuidAction action = DiarmuidAction.bySlot(slot);
         assertNotNull(action);
         assertEquals(mana[slot + 1], action.mana());
         assertEquals(cooldowns[slot + 1], action.cooldown());
         assertEquals(ends[slot + 1], action.duration());
         assertEquals(action, DiarmuidAction.bySkillId(action.skillId()));
         assertEquals(action, DiarmuidAction.byNetworkId(action.networkId()));
      }
      var finisher = DiarmuidAction.ROSE_DANCE.hits();
      assertEquals(List.of(8,12,16,20,26,30), finisher.stream().map(DiarmuidAction.Hit::tick).toList());
      assertEquals(92, finisher.stream().mapToDouble(DiarmuidAction.Hit::damage).sum());
      assertEquals(3, finisher.stream().filter(DiarmuidAction.Hit::red).count());
      assertEquals(3, finisher.stream().filter(hit -> !hit.red()).count());
      assertNull(DiarmuidAction.byNetworkId(-1));
      assertNull(DiarmuidAction.byNetworkId(100));
   }

   @Test void everyHitRespectsSpearRangeAndRecovery() {
      for (var action : DiarmuidAction.values()) {
         int previous = 0;
         for (var hit : action.hits()) {
            assertTrue(hit.tick() > previous && hit.tick() < action.duration(), action.name());
            assertTrue(hit.range() <= (hit.red() ? 5 : 3), action.name());
            assertTrue(hit.damage() > 0);
            previous = hit.tick();
         }
      }
      assertEquals(2, DiarmuidAction.RED_BASIC.hits().getFirst().tick());
      assertEquals(18, DiarmuidAction.RED_BASIC.hits().getFirst().damage());
      assertEquals(17, DiarmuidAction.YELLOW_BASIC.hits().getFirst().damage());
      assertEquals(8, DiarmuidAction.HAND_COOLDOWN);
      assertEquals(4, DiarmuidAction.RED_BASIC.duration());
   }

   @Test void actionMetadataCannotDriftFromExecutableDefinition() throws Exception {
      var actions = json("data/typemoonworld/servant/actions/diarmuid_ua_duibhne.json").getAsJsonArray("actions");
      assertEquals(DiarmuidAction.values().length, actions.size());
      for (var action : DiarmuidAction.values()) {
         var data = actions.get(action.ordinal()).getAsJsonObject();
         assertEquals(action.actionId(), data.get("id").getAsString());
         assertEquals(action.mana(), data.get("mana_cost").getAsDouble());
         assertEquals(action.range(), data.get("maximum_range").getAsDouble());
         var timing = data.getAsJsonObject("timing");
         assertEquals(action.duration(), timing.get("windup").getAsInt() + timing.get("active").getAsInt() + timing.get("recovery").getAsInt());
      }
   }

   @Test void everyTechniqueHasCompleteBodyAndIndependentItemTracks() throws Exception {
      for (var action : DiarmuidAction.values()) {
         var data = json("assets/typemoonworld/player_animations/" + action.animationPath() + ".json");
         assertEquals(action.animationPath(), data.get("name").getAsString());
         var emote = data.getAsJsonObject("emote");
         assertEquals(action.duration(), emote.get("endTick").getAsInt());
         var moves = emote.getAsJsonArray("moves");
         assertEquals(action.duration() + 1, moves.size());
         for (int tick = 0; tick <= action.duration(); tick++) {
            var move = moves.get(tick).getAsJsonObject();
            assertEquals(tick, move.get("tick").getAsInt());
            for (String bone : List.of("body","head","torso","rightArm","leftArm","rightLeg","leftLeg","rightItem","leftItem")) {
               var track = move.getAsJsonObject(bone);
               assertNotNull(track, action + ":" + bone);
               for (String channel : List.of("x","y","z","pitch","yaw","roll")) assertTrue(Double.isFinite(track.get(channel).getAsDouble()));
            }
         }
      }
      var zh = json("assets/typemoonworld/lang/zh_cn.json");
      var en = json("assets/typemoonworld/lang/en_us.json");
      for (var action : DiarmuidAction.values()) {
         assertTrue(zh.has("skill.typemoonworld.servant_card." + action.skillId()));
         assertTrue(en.has("skill.typemoonworld.servant_card." + action.skillId()));
      }
   }

   @Test void identityPassivesAndWeaponEffectsRemainExplicit() throws Exception {
      var def = json("data/typemoonworld/servant/definitions/diarmuid_ua_duibhne.json");
      assertEquals("earth", def.get("faction").getAsString());
      assertEquals("mythology", def.get("era").getAsString());
      assertEquals("lancer", def.get("class_type").getAsString());
      var params = def.getAsJsonObject("parameters");
      assertEquals("B", params.get("strength").getAsString());
      assertEquals("C", params.get("endurance").getAsString());
      assertEquals("A", params.get("agility").getAsString());
      assertTrue(params.get("agility_plus").getAsBoolean());
      assertEquals("D", params.get("magic").getAsString());
      assertEquals("E", params.get("luck").getAsString());
      assertEquals("B", params.get("noble_phantasm_rank").getAsString());
      var traits = def.getAsJsonArray("traits");
      for (String trait : List.of("earth","heaven_or_earth_servant","balanced","human_kind","seven_knights","beloved")) {
         assertTrue(traits.contains(JsonParser.parseString("\"" + trait + "\"")), trait);
      }
      assertFalse(traits.contains(JsonParser.parseString("\"neutral\"")));
      var skills = def.getAsJsonArray("skills");
      assertTrue(skills.contains(JsonParser.parseString("\"true_mind_eye_b\"")));
      assertFalse(skills.contains(JsonParser.parseString("\"riding_b\"")));
      for (String id : List.of("love_spot_c","knight_strategy_b","mana_burst_jump_a"))
         assertTrue(json("data/typemoonworld/servant/skills/" + id + ".json").getAsJsonArray("effects").isEmpty());
      assertEquals(200, DiarmuidCombatHelper.SPEAR_MAX_DURABILITY);
      assertEquals(5, DiarmuidCombatHelper.MAX_YELLOW_ROSE_STACKS);
      assertEquals(100, DiarmuidCombatHelper.YELLOW_ROSE_DAMAGE_INTERVAL_TICKS);
      assertEquals(0.1, DiarmuidCombatHelper.MAX_HEALTH_REDUCTION_PER_STACK);
      assertEquals(0.8, DiarmuidAction.YELLOW_MODEL_LENGTH);
      String items = Files.readString(Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java"));
      assertFalse(items.contains("gae_buidhe_range"));
   }

   @Test void importedVoicesUseMono48000HzVorbisAndRegisteredEvents() throws Exception {
      var sounds=json("assets/typemoonworld/sounds.json");
      for (String event : List.of("attack","fail","victory","np")) assertTrue(sounds.has("diarmuid_ua_duibhne_voice_"+event));
      for (String clip : List.of("attack1","attack2","fail1","fail2","victory1","victory2","np")) {
         byte[] bytes=Files.readAllBytes(RES.resolve("assets/typemoonworld/sounds/voice/diarmuid_ua_duibhne/"+clip+".ogg"));
         assertEquals("OggS",new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII));
         int marker=-1;
         for (int i=0;i<Math.min(bytes.length-16,128);i++) if (new String(bytes,i,6,java.nio.charset.StandardCharsets.US_ASCII).equals("vorbis")) { marker=i; break; }
         assertTrue(marker>0,clip);
         assertEquals(1,bytes[marker+10],clip);
         int rate=java.nio.ByteBuffer.wrap(bytes,marker+11,4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
         assertEquals(48000,rate,clip);
      }
   }
}
