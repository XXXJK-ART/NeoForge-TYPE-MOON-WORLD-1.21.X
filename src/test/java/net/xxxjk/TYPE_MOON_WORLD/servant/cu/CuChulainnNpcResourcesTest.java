package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuChulainnNpcResourcesTest {
   private static JsonObject animation(String name)throws Exception {
      return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/typemoonworld/player_animations/"+name+".json"))).getAsJsonObject();
   }
   @Test void everyNpcActionHasACompleteTimelineAndCorrectHandTracks()throws Exception {
      for(int action=CuChulainnRules.NPC_SWEEP;action<=CuChulainnRules.NPC_SLASH;action++) {
         String name=CuChulainnRules.animation(action);var clip=animation(name);var emote=clip.getAsJsonObject("emote");
         int end=action<=CuChulainnRules.NPC_FLURRY?20:12;
         assertEquals(name,clip.get("name").getAsString());assertEquals(end,emote.get("endTick").getAsInt());
         var moves=emote.getAsJsonArray("moves");assertEquals(end+1,moves.size());
         for(int tick=0;tick<=end;tick++) {
            var frame=moves.get(tick).getAsJsonObject();assertEquals(tick,frame.get("tick").getAsInt());
            for(String track:new String[]{"torso","rightArm","leftArm","rightItem"})assertTrue(frame.has(track),name+" lacks "+track);
            assertFalse(frame.has("head"),"NPC must retain target tracking");assertFalse(frame.has("leftItem"),"adaptation must remain single-spear");
            assertFalse(frame.has("rightLeg"));assertFalse(frame.has("leftLeg"));
            for(var entry:frame.entrySet())if(entry.getValue().isJsonObject())for(var channel:entry.getValue().getAsJsonObject().entrySet())
               assertTrue(Double.isFinite(channel.getValue().getAsDouble()),name+" contains a non-finite transform");
         }
      }
   }
   @Test void npcDanceActuallyReusesDiarmuidsWeaponArm()throws Exception {
      var source=animation("diarmuid_red_dance").getAsJsonObject("emote").getAsJsonArray("moves");
      var dance=animation("cu_npc_dance").getAsJsonObject("emote").getAsJsonArray("moves");
      for(int tick:new int[]{2,5,8})for(String axis:new String[]{"x","y","z","pitch","yaw","roll"})
         assertEquals(source.get(tick).getAsJsonObject().getAsJsonObject("rightArm").get(axis).getAsDouble(),
            dance.get(tick).getAsJsonObject().getAsJsonObject("rightArm").get(axis).getAsDouble(),.0001);
      var flurry=animation("cu_npc_flurry").getAsJsonObject("emote").getAsJsonArray("moves");
      for(int[] pair:new int[][]{{4,2},{8,5},{12,8}})
         assertEquals(source.get(pair[1]).getAsJsonObject().getAsJsonObject("rightArm").get("pitch").getAsDouble(),
            flurry.get(pair[0]).getAsJsonObject().getAsJsonObject("rightArm").get("pitch").getAsDouble(),.0001);
   }
   @Test void everyNpcSpearTechniqueUsesTheExistingSpearTrail() {
      for(int action:new int[]{-1,8,9,10,11,13,15,20,21,22,23,24})assertTrue(CuChulainnRules.spearTrail(action));
   }
   @Test void thornHasAnExplicitChamberDriveAndRecovery()throws Exception {
      var emote=animation(CuChulainnRules.animation(CuChulainnRules.THORN_STRIKE)).getAsJsonObject("emote");
      assertEquals(CuChulainnRules.THORN_DURATION,emote.get("endTick").getAsInt());
      var moves=emote.getAsJsonArray("moves");assertEquals(19,moves.size());
      var chamber=moves.get(3).getAsJsonObject().getAsJsonObject("rightArm");
      var strike=moves.get(CuChulainnRules.THORN_HIT_TICK).getAsJsonObject().getAsJsonObject("rightArm");
      assertTrue(strike.get("pitch").getAsDouble()<chamber.get("pitch").getAsDouble()-40);
      assertTrue(moves.get(CuChulainnRules.THORN_HIT_TICK).getAsJsonObject().getAsJsonObject("torso").get("pitch").getAsDouble()>20);
      for(var frame:moves)assertFalse(frame.getAsJsonObject().has("rightLeg"));
   }
   @Test void recallFlourishReusesTheSingleSpearDance()throws Exception {
      assertEquals(CuChulainnRules.animation(CuChulainnRules.NPC_DANCE),CuChulainnRules.animation(CuChulainnRules.RECALL_CATCH));
      assertEquals(12,animation(CuChulainnRules.animation(CuChulainnRules.RECALL_CATCH)).getAsJsonObject("emote").get("endTick").getAsInt());
   }
}
