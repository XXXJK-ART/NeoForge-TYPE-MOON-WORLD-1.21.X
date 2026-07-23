package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
