package net.xxxjk.TYPE_MOON_WORLD.servant.cu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.DisengageService;

class CuChulainnRulesTest {
   @Test void disengageRanksHaveExactValues() {
      String[] ranks={"E","D","C","B","A"};double[] heal={.06,.08,.10,.12,.15};
      for(int i=0;i<5;i++) {
         var r=DisengageService.rank(ranks[i]);
         assertEquals(heal[i],r.healing());assertEquals(2+i*.5,r.retreat());
         assertEquals(.1+i*.05,r.speed(),1e-9);assertEquals(10+i*4,r.mana());assertEquals(1400-i*200,r.cooldown());
      }
      assertThrows(IllegalArgumentException.class,()->DisengageService.rank("EX"));
   }
   @Test void fourBranchesIsBoundedAndDoesNotDependOnDamageSize() {
      assertEquals(1,CuChulainnRules.fourBranches(100,100));
      assertEquals(1.15,CuChulainnRules.fourBranches(50,100),1e-9);
      assertEquals(1.24,CuChulainnRules.fourBranches(20,100),1e-9);
      assertEquals(1.3,CuChulainnRules.fourBranches(-5,100),1e-9);
      assertEquals(1,CuChulainnRules.fourBranches(50,0));
   }
   @Test void inputModeUsesStartCrouchAndInclusiveThresholds() {
      assertEquals(CuChulainnRules.Input.LOCK,CuChulainnRules.input(false,19));
      assertEquals(CuChulainnRules.Input.THORN,CuChulainnRules.input(false,20));
      assertEquals(CuChulainnRules.Input.THORN,CuChulainnRules.input(false,40));
      assertEquals(CuChulainnRules.Input.THROW,CuChulainnRules.input(true,19));
      assertEquals(CuChulainnRules.Input.THROW,CuChulainnRules.input(true,39));
      assertEquals(CuChulainnRules.Input.FLIGHT,CuChulainnRules.input(true,40));
      assertEquals(CuChulainnRules.Input.FLIGHT,CuChulainnRules.input(true,70));
   }
   @Test void flightCapsPaymentBeforeDamage() {
      assertEquals(300,CuChulainnRules.flightCost(40));assertEquals(590,CuChulainnRules.flightCost(69));
      assertEquals(600,CuChulainnRules.flightCost(70));assertEquals(600,CuChulainnRules.flightCost(Integer.MAX_VALUE));
      assertEquals(0,CuChulainnRules.flightPayment(70,299));assertEquals(450,CuChulainnRules.flightPayment(70,450));
      assertEquals(750,CuChulainnRules.flightDamage(450));assertEquals(1000,CuChulainnRules.flightDamage(900));
   }
   @Test void lockAndReleaseRangesRemainIndependent() {
      assertTrue(CuChulainnRules.lockDistance(4));assertFalse(CuChulainnRules.lockDistance(4.001));
      assertTrue(CuChulainnRules.thornDistance(5));assertFalse(CuChulainnRules.thornDistance(5.001));
      assertFalse(CuChulainnRules.lockDistance(Double.NaN));assertEquals(600,CuChulainnRules.LOCK_TICKS);
   }
   @Test void eachSkillOwnsItsCooldownAndSpearRequirement() {
      assertEquals(18,CuChulainnRules.skill(-1).cooldown());assertEquals(600,CuChulainnRules.skill(6).cooldown());
      assertEquals(60,CuChulainnRules.skill(7).cooldown());assertEquals(1000,CuChulainnRules.skill(5).cooldown());
      assertNotEquals(CuChulainnRules.skill(-1).slot(),CuChulainnRules.skill(6).slot());
      for(int slot=0;slot<=7;slot++)assertFalse(CuChulainnRules.skill(slot).spear());
      assertTrue(CuChulainnRules.skill(8).spear());assertTrue(CuChulainnRules.skill(9).spear());
   }
}
