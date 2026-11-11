package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.xxxjk.TYPE_MOON_WORLD.client.ServantBaseAttackInput;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantBaseAction;
import org.junit.jupiter.api.Test;

class ServantBaseAttackInputTest {
   @Test
   void tapUsesOneShortStrikeAndDoesNotCancelItsRecoveryOnRelease() {
      var input = new ServantBaseAttackInput();
      assertEquals(ServantBaseAction.SWORD, input.attack(false, 100));
      assertEquals(ServantBaseAction.NONE, input.tick(false, true, 101));
      assertEquals("servant_base_sword_huijian1", ServantBaseAction.animationId(ServantBaseAction.SWORD, 0));
      assertEquals("servant_base_sword_huijian1", ServantBaseAction.animationId(ServantBaseAction.SWORD, 1));
      assertEquals("servant_base_fist_qingji1", ServantBaseAction.animationId(ServantBaseAction.FIST, 0));
      assertEquals("servant_base_fist_qingji2", ServantBaseAction.animationId(ServantBaseAction.FIST, 1));
   }

   @Test
   void repeatedAttacksSwitchToComboRefreshWithoutRestartAndStopOnRelease() {
      var input = new ServantBaseAttackInput();
      assertEquals(ServantBaseAction.FIST, input.attack(true, 100));
      assertEquals(ServantBaseAction.NONE, input.tick(true, true, 105));
      assertEquals(ServantBaseAction.NONE, input.tick(true, true, 106));
      assertEquals(ServantBaseAction.FIST_COMBO, input.attack(true, 106));
      assertEquals(ServantBaseAction.NONE, input.tick(true, true, 107));
      assertEquals(ServantBaseAction.FIST_COMBO, input.tick(true, true, 111));
      assertEquals(ServantBaseAction.STOP, input.tick(false, true, 112));
      assertEquals(ServantBaseAction.NONE, input.tick(false, true, 113));
      assertEquals("servant_base_fist_lianji", ServantBaseAction.animationId(ServantBaseAction.FIST_COMBO, 0));
   }

   @Test
   void holdingAfterOneAttackDoesNotInventFurtherStrikes() {
      var input = new ServantBaseAttackInput();
      input.attack(false, 100);
      assertEquals(ServantBaseAction.NONE, input.tick(true, true, 106));
      assertEquals(ServantBaseAction.NONE, input.tick(true, true, 130));
      input.attack(false, 140);
      input.attack(false, 145);
      assertEquals(ServantBaseAction.STOP, input.tick(true, true, 156));
   }

   @Test
   void rapidClicksUseComboAndUiOrTransformationExitStopsIt() {
      var input = new ServantBaseAttackInput();
      assertEquals(ServantBaseAction.SWORD, input.attack(false, 100));
      input.tick(false, true, 101);
      assertEquals(ServantBaseAction.SWORD_COMBO, input.attack(false, 104));
      assertEquals(ServantBaseAction.STOP, input.tick(true, false, 105));
      assertEquals(ServantBaseAction.SWORD, input.attack(false, 200));
      assertEquals("servant_base_sword_huijian", ServantBaseAction.animationId(ServantBaseAction.SWORD_COMBO, 0));
   }
}
