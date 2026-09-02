package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class CardActionRegistryTest {
   @Test
   void duplicateActionsAndInvalidSlotsAreRejected() {
      ResourceLocation action = ResourceLocation.fromNamespaceAndPath("api_test", "action");
      ResourceLocation servant = ResourceLocation.fromNamespaceAndPath("api_test", "servant");
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.register(action, context -> ExecutionResult.SUCCESS, "api_test"));
      assertFalse(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.register(action, context -> ExecutionResult.SUCCESS, "api_test"));
      assertFalse(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.bindSlot(servant, 10, action.toString(), "api_test"));
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.bindSlot(servant, 0, action.toString(), "skill.api_test.action", "api_test"));
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.contains(action.toString()));
   }

   @Test
   void builtInBareServantIdResolvesNamespacedDataBinding() {
      ResourceLocation servant = ResourceLocation.fromNamespaceAndPath("typemoonworld", "card_alias_test");
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.bindDataSlot(
         servant, 3, "api_test:aliased_action", "skill.api_test.aliased_action"));
      assertEquals("api_test:aliased_action",
         net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.actionIdForSlot("card_alias_test", 3));
      assertEquals("skill.api_test.aliased_action",
         net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry.translationKey("card_alias_test", 3));
   }
}
