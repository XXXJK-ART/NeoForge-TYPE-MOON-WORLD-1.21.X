package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;
import org.junit.jupiter.api.Test;

class ServantTraitAttackEvaluatorTest {
   @Test
   void identityWithoutAnExplicitConditionGivesNoBonus() {
      assertEquals(1, ServantTraitAttackEvaluator.computeMultiplier(null, List.of(ServantTraitTag.values())));
      assertEquals(1, ServantTraitAttackEvaluator.computeMultiplier(List.of(), List.of(ServantTraitTag.values())));
   }

   @Test
   void theSameExplicitConditionCannotMultiplyDamageTwice() {
      assertEquals(1.5, ServantTraitAttackEvaluator.computeMultiplier(
         List.of(ServantTraitTag.MALE, ServantTraitTag.MALE), List.of(ServantTraitTag.MALE)));
      assertEquals(2.25, ServantTraitAttackEvaluator.computeMultiplier(
         List.of(ServantTraitTag.MALE, ServantTraitTag.DIVINE), List.of(ServantTraitTag.MALE, ServantTraitTag.DIVINE)));
      assertEquals(1, ServantTraitAttackEvaluator.computeMultiplier(
         List.of(ServantTraitTag.DRAGON), List.of(ServantTraitTag.MALE)));
   }

   @Test
   void unknownNpConditionsNeverTurnIntoAntiServantDamage() {
      var json = JsonParser.parseString("""
         {"special_attack_conditions":["male","male","curse_medium","requires_spellbook_mana_2000"]}
         """);
      var definition = ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
      assertEquals(List.of(ServantTraitTag.MALE), definition.specialAttackConditions());
      assertEquals(1, ServantTraitAttackEvaluator.computeMultiplier(
         definition.specialAttackConditions(), List.of(ServantTraitTag.SERVANT)));
   }
}
