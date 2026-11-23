package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CuChulainnResourcesTest {
   private static JsonObject json(String path)throws Exception{return JsonParser.parseString(Files.readString(Path.of("src/main/resources/"+path))).getAsJsonObject();}
   @Test void identityAndRanksAreComplete()throws Exception {
      var definition=json("data/typemoonworld/servant/definitions/cu_chulainn.json");
      assertEquals("heaven",definition.get("faction").getAsString());assertEquals("mythology",definition.get("era").getAsString());
      String traits=definition.get("traits").toString();
      for(String trait:new String[]{"lawful","balanced","heaven_or_earth_servant","beloved","human_kind","seven_knights"})assertTrue(traits.contains('"'+trait+'"'));
      assertFalse(traits.contains("living_human"));assertEquals("B",definition.getAsJsonObject("parameters").get("noble_phantasm_rank").getAsString());
   }
   @Test void activeRankResourcesAgreeWithCommonRules()throws Exception {
      for(String rank:new String[]{"E","D","C","B","A"}){
         var value=Disengage(rank);var data=json("data/typemoonworld/servant/skills/recast_stance_"+rank.toLowerCase()+".json");
         assertEquals("active",data.get("type").getAsString());assertEquals(value.mana(),data.get("mp_cost").getAsInt());assertEquals(value.cooldown(),data.get("cooldown_ticks").getAsInt());
      }
   }
   private static net.xxxjk.TYPE_MOON_WORLD.servant.combat.DisengageService.Rank Disengage(String rank){return net.xxxjk.TYPE_MOON_WORLD.servant.combat.DisengageService.rank(rank);}
   @Test void animationTracksCoverTheAuthoritativeHitFrames()throws Exception {
      for(String name:new String[]{"cu_thrust","cu_sweep","cu_flurry","cu_raise","cu_throw","cu_rune"}){
         var animation=json("assets/typemoonworld/player_animations/"+name+".json").getAsJsonObject("emote");
         assertTrue(animation.getAsJsonArray("moves").size()>=13);
         assertTrue(animation.getAsJsonArray("moves").get(5).getAsJsonObject().has("rightItem"));
         if(name.equals("cu_sweep") || name.equals("cu_flurry"))assertEquals(20,animation.get("endTick").getAsInt());
      }
   }
   @Test void everySkillHasBothTranslations()throws Exception {
      for(String locale:new String[]{"zh_cn","en_us"}){
         var translations=json("assets/typemoonworld/lang/"+locale+".json");
         for(int slot=-1;slot<=9;slot++)assertTrue(translations.has("skill.typemoonworld.servant_card."+CuChulainnRules.skill(slot).id()),locale+" slot "+slot);
      }
   }
   @Test void throwAndRaiseOnlyAnimateTheWeaponArm()throws Exception {
      for(String name:new String[]{"cu_raise","cu_throw"})for(var move:json("assets/typemoonworld/player_animations/"+name+".json").getAsJsonObject("emote").getAsJsonArray("moves")) {
         var frame=move.getAsJsonObject();
         assertTrue(frame.has("rightArm"));assertTrue(frame.has("rightItem"));
         for(String part:new String[]{"body","torso","leftArm","rightLeg","leftLeg"})assertFalse(frame.has(part),name+" moves "+part);
      }
   }
}
