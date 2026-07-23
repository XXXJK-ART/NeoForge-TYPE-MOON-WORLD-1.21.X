package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GemApiRegistryTest {
   @Test
   void customAffinityIsNamespacedAndClamped() {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gem_test", "meteor");
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry.register(id, type -> 100));
      assertFalse(net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry.register(id, type -> 0));
      assertTrue(net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry.contains(id));
      assertEquals(99, net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry.calculate(id, GemType.RUBY, GemQuality.HIGH, 100.0));
   }
}
