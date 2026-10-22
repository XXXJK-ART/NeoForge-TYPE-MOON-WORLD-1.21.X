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
      int[] mana = {6,12,10,14,14,16,12,18,0,24,36};
      int[] cooldowns = {20,80,70,100,100,120,100,260,20,180,400};
      int[] ends = {14,14,13,17,16,17,17,120,12,300,40};
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

   @Test void animationPositionsStayOnEntityAndSweepUsesShouldersInsteadOfSpinningWrists() throws Exception {
      for (var action : DiarmuidAction.values()) {
         var moves=json("assets/typemoonworld/player_animations/"+action.animationPath()+".json").getAsJsonObject("emote").getAsJsonArray("moves");
         for (var frame : moves) for (String bone : List.of("body","rightItem","leftItem"))
            for (String axis : List.of("x","y","z")) assertEquals(0,frame.getAsJsonObject().getAsJsonObject(bone).get(axis).getAsDouble(),action+":"+bone);
      }
      var sweep=json("assets/typemoonworld/player_animations/diarmuid_red_sweep.json").getAsJsonObject("emote").getAsJsonArray("moves");
      double wristTurn=0, shoulderTurn=0;
      for(var frame:sweep){
         var pose=frame.getAsJsonObject();
         wristTurn=Math.max(wristTurn,Math.abs(pose.getAsJsonObject("rightItem").get("yaw").getAsDouble()));
         shoulderTurn=Math.max(shoulderTurn,Math.abs(pose.getAsJsonObject("rightArm").get("yaw").getAsDouble()));
      }
      assertTrue(wristTurn<40,"sweep spun the wrist");
      assertTrue(shoulderTurn>20,"sweep did not drive the shoulder");
      double minimum=180, maximum=-180;
      for (var frame : sweep) {
         double yaw=frame.getAsJsonObject().getAsJsonObject("rightArm").get("yaw").getAsDouble();
         minimum=Math.min(minimum,yaw); maximum=Math.max(maximum,yaw);
      }
      assertTrue(maximum-minimum>210,"sweep shoulder arc remained narrow");
      var paired=json("assets/typemoonworld/player_animations/diarmuid_landing_sweep.json").getAsJsonObject("emote").getAsJsonArray("moves");
      assertTrue(paired.get(10).getAsJsonObject().getAsJsonObject("leftArm").get("yaw").getAsDouble()<0,"landing sweep did not pair both arms");
      assertTrue(paired.get(5).getAsJsonObject().getAsJsonObject("torso").get("pitch").getAsDouble()>0,"sweep torso could not lean");
      var continuous=DiarmuidAction.FIGURE_EIGHT.hits();
      assertEquals(74,continuous.size());
      assertEquals(37,continuous.stream().filter(DiarmuidAction.Hit::red).count());
      assertEquals(298,continuous.getLast().tick());
      assertEquals(120,DiarmuidAction.MANA_LEAP.duration());
      assertEquals(17,DiarmuidAction.LANDING_SWEEP.duration());
      for (var dance : List.of(DiarmuidAction.RED_DANCE,DiarmuidAction.YELLOW_DANCE)) {
         assertEquals(List.of(2,5,8),dance.hits().stream().map(DiarmuidAction.Hit::tick).toList());
         assertEquals(dance==DiarmuidAction.RED_DANCE?18:17,dance.hits().stream().mapToDouble(DiarmuidAction.Hit::damage).sum());
      }
   }

   @Test void pairedSweepsOpenWithoutChestCrossingThenBraceAndTwirlHome() throws Exception {
      for (var action : List.of(DiarmuidAction.LOW_SPIN, DiarmuidAction.LANDING_SWEEP)) {
         var moves = json("assets/typemoonworld/player_animations/" + action.animationPath() + ".json")
            .getAsJsonObject("emote").getAsJsonArray("moves");
         double previous = 0;
         for (int tick = 0; tick <= action.duration(); tick++) {
            var frame = moves.get(tick).getAsJsonObject();
            var right = frame.getAsJsonObject("rightArm");
            var left = frame.getAsJsonObject("leftArm");
            double angle = right.get("yaw").getAsDouble();
            assertTrue(angle >= 0 && angle <= 120, action + ": sweep exceeded its shoulder arc");
            assertEquals(-angle, left.get("yaw").getAsDouble(), 1.0E-4, action + ": sweeps lost symmetry");
            assertEquals(right.get("pitch").getAsDouble(), left.get("pitch").getAsDouble(), 1.0E-4);
            assertEquals(-right.get("x").getAsDouble(), left.get("x").getAsDouble(), 1.0E-4);
            assertEquals(0, frame.getAsJsonObject("torso").get("yaw").getAsDouble(), action + ": torso crossed sweep sectors");
            if (tick == 2) {
               assertEquals(0, angle, action + ": windup must face forward in separate lanes");
               assertEquals(0, right.get("roll").getAsDouble(), action + ": windup crosses the chest");
            }
            if (tick > 2 && tick <= 6) assertTrue(angle >= previous, action + ": outward sweep reversed");
            if (tick >= 6 && tick <= 9) assertEquals(120, angle, action + ": sweep must open in four ticks");
            previous = angle;
         }
         var brace = moves.get(9).getAsJsonObject().getAsJsonObject("torso");
         assertTrue(brace.get("pitch").getAsDouble() > 0 && brace.get("pitch").getAsDouble() <= 7,
            action + ": sweep must only lean slightly");
         assertTrue(brace.get("y").getAsDouble() <= 0.35, action + ": sweep crouches too far");
         for (String item : List.of("rightItem", "leftItem")) {
            double before = moves.get(10).getAsJsonObject().getAsJsonObject(item).get("pitch").getAsDouble();
            double after = moves.get(15).getAsJsonObject().getAsJsonObject(item).get("pitch").getAsDouble();
            assertTrue(Math.abs(after - before) > 280, action + ": missing recovery twirl");
         }
      }
   }

   @Test void retainedDanceAndSweepKeepRecoveryTwirls() throws Exception {
      for (var action : List.of(DiarmuidAction.ROSE_DANCE, DiarmuidAction.RED_SWEEP)) {
         var moves = json("assets/typemoonworld/player_animations/" + action.animationPath() + ".json")
            .getAsJsonObject("emote").getAsJsonArray("moves");
         var recovery = moves.get(action.duration() - 1).getAsJsonObject();
         for (var hit : action.hits()) assertTrue(Math.abs(recovery.getAsJsonObject(hit.red() ? "rightItem" : "leftItem")
            .get("pitch").getAsDouble()) > 300, action + ": missing spear twirl after the final strike");
      }
   }

   @Test void stanceKeepsBothArmsLowAndBehindWithoutMovingTheBody() throws Exception {
      var moves = json("assets/typemoonworld/player_animations/diarmuid_double_spear_stance.json")
         .getAsJsonObject("emote").getAsJsonArray("moves");
      for (var value : moves) {
         var frame = value.getAsJsonObject();
         for (String track : List.of("body", "torso")) for (String axis : List.of("x", "y", "z", "pitch", "yaw", "roll"))
            assertEquals(0, frame.getAsJsonObject(track).get(axis).getAsDouble(), track + ":" + axis);
         assertTrue(frame.getAsJsonObject("rightArm").get("pitch").getAsDouble() > 0, "red arm must rest behind");
         assertTrue(frame.getAsJsonObject("leftArm").get("pitch").getAsDouble() > 0, "yellow arm must rest behind");
      }
   }

   @Test void optimizedThrustsUseReadyPositionAndYellowStabsDoNotSweep() throws Exception {
      for (var action : List.of(DiarmuidAction.CROSS_THRUST, DiarmuidAction.ADVANCING_THRUST,
         DiarmuidAction.YELLOW_DOUBLE, DiarmuidAction.RISING_THRUST, DiarmuidAction.RETREATING_THRUST,
         DiarmuidAction.RED_BASIC, DiarmuidAction.YELLOW_BASIC, DiarmuidAction.GUARD_COUNTER)) {
         var moves = json("assets/typemoonworld/player_animations/" + action.animationPath() + ".json")
            .getAsJsonObject("emote").getAsJsonArray("moves");
         var rest = moves.get(0).getAsJsonObject();
         for (String track : List.of("rightArm", "leftArm", "rightItem", "leftItem"))
            for (String axis : List.of("x", "y", "z", "pitch", "yaw", "roll")) {
               double a = rest.getAsJsonObject(track).get(axis).getAsDouble();
               double b = moves.get(action.visualDuration()).getAsJsonObject().getAsJsonObject(track).get(axis).getAsDouble();
               double difference = b - a;
               if (List.of("pitch", "yaw", "roll").contains(axis)) difference = Math.IEEEremainder(difference, 360);
               assertEquals(0, difference, 1.0E-3, action + ": recovery " + track + ":" + axis);
            }
         for (var value : moves) {
            var frame = value.getAsJsonObject();
            assertTrue(frame.getAsJsonObject("rightArm").get("roll").getAsDouble() <= 0, action + ": red crosses inward");
            assertTrue(frame.getAsJsonObject("leftArm").get("roll").getAsDouble() >= 0, action + ": yellow crosses inward");
            if (action == DiarmuidAction.YELLOW_DOUBLE) {
               double yaw = frame.getAsJsonObject("leftArm").get("yaw").getAsDouble();
               assertTrue(yaw >= 0 && yaw <= 56, "yellow short stabs became a broad sweep");
            }
         }
      }
   }

   @Test void thrustsLeanTowardTheStrikingSideAndTakeoffPoseReleasesInFlight() throws Exception {
      for (var action : List.of(DiarmuidAction.CROSS_THRUST,DiarmuidAction.ADVANCING_THRUST,
         DiarmuidAction.YELLOW_DOUBLE,DiarmuidAction.RETREATING_THRUST,DiarmuidAction.ROSE_DANCE,
         DiarmuidAction.GUARD_COUNTER)) {
         var moves=json("assets/typemoonworld/player_animations/"+action.animationPath()+".json")
            .getAsJsonObject("emote").getAsJsonArray("moves");
         for (var hit : action.hits()) {
            var frame=moves.get(hit.tick()).getAsJsonObject();
            var torso=frame.getAsJsonObject("torso");
            assertTrue(torso.get("pitch").getAsDouble()>=15,action+": forward lean missing");
            assertTrue(torso.get("yaw").getAsDouble()*(hit.red()?-1:1)>20,action+": wrong striking side");
            var arm=frame.getAsJsonObject(hit.red()?"rightArm":"leftArm");
            assertTrue(arm.get("pitch").getAsDouble()<(hit.red()?-90:-70),action+": striking arm did not extend");
            assertTrue(arm.get("z").getAsDouble()<(hit.red()?-2:-1.5),action+": striking shoulder did not advance");
         }
      }
      var leap=json("assets/typemoonworld/player_animations/diarmuid_mana_leap.json")
         .getAsJsonObject("emote").getAsJsonArray("moves");
      for (int tick=14;tick<DiarmuidAction.MANA_LEAP.duration();tick++)
         for (String bone : List.of("body","torso","rightArm","leftArm"))
            for (String axis : List.of("pitch","yaw","roll"))
               assertEquals(0.0,leap.get(tick).getAsJsonObject().getAsJsonObject(bone).get(axis).getAsDouble(),1.0E-8,bone+":"+tick);
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

   @Test void everyTechniqueLeavesVanillaHeadAndLocomotionWithIndependentItemTracks() throws Exception {
      for (var action : DiarmuidAction.values()) {
         var data = json("assets/typemoonworld/player_animations/" + action.animationPath() + ".json");
         assertEquals(action.animationPath(), data.get("name").getAsString());
         var emote = data.getAsJsonObject("emote");
         assertEquals(action.visualDuration(), emote.get("endTick").getAsInt());
         var moves = emote.getAsJsonArray("moves");
         assertEquals(action.visualDuration() + 1, moves.size());
         for (int tick = 0; tick <= action.visualDuration(); tick++) {
            var move = moves.get(tick).getAsJsonObject();
            assertEquals(tick, move.get("tick").getAsInt());
            for (String bone : List.of("head","rightLeg","leftLeg")) assertFalse(move.has(bone),action+": overrides vanilla "+bone);
            for (String axis : List.of("pitch","yaw","roll")) assertEquals(0,move.getAsJsonObject("body").get(axis).getAsDouble(),action+": tilts vanilla locomotion");
            for (String bone : List.of("body","torso","rightArm","leftArm","rightItem","leftItem")) {
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
