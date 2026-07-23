package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import net.xxxjk.typemoonworld.api.datagen.MagicDefinitionBuilder;

class DatagenBuilderTest {
   @Test
   void writesNamespacedMagicDefinition() {
      var json = new MagicDefinitionBuilder(ResourceLocation.fromNamespaceAndPath("test", "meteor"))
         .manaCost(4).cooldown(20).npcAllowed(true).json();
      assertEquals("test:meteor", json.get("id").getAsString());
      assertEquals(4, json.get("mana_cost").getAsInt());
   }
}
