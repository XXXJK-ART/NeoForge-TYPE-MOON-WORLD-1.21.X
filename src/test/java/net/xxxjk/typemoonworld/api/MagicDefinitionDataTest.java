package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MagicDefinitionDataTest {
   @Test
   void codecUsesResourceIdWhenDefinitionOmitsId() {
      MagicDefinitionData parsed = MagicDefinitionData.CODEC.parse(
         JsonOps.INSTANCE,
         JsonParser.parseString("{\"mana_cost\":12.5,\"cooldown_ticks\":40,\"crest_allowed\":false}")
      ).result().orElseThrow().withId(ResourceLocation.fromNamespaceAndPath("addon", "meteor"));

      assertEquals("addon:meteor", parsed.id().toString());
      assertEquals(12.5, parsed.manaCost());
      assertEquals(40, parsed.cooldownTicks());
      assertTrue(!parsed.crestAllowed());
      assertEquals("magic.addon.meteor.name", parsed.nameKey());
      assertTrue(parsed.requiredAttributes().isEmpty());
   }

   @Test
   void codecLoadsNamespacedAttributeRequirements() {
      MagicDefinitionData parsed = MagicDefinitionData.CODEC.parse(
         JsonOps.INSTANCE,
         JsonParser.parseString("{\"required_attributes\":[\"typemoonworld:imaginary_number\"]}")
      ).result().orElseThrow();

      assertEquals(java.util.List.of(MagicAttributes.IMAGINARY_NUMBER), parsed.requiredAttributes());
   }

   @Test
   void valuesAreClampedAtTheApiBoundary() {
      MagicDefinitionData data = new MagicDefinitionData(
         ResourceLocation.fromNamespaceAndPath("addon", "bad"), "", null, null,
         -10.0, -4, true, true, true, true, false, -1, -2
      );
      assertEquals(0.0, data.manaCost());
      assertEquals(0, data.cooldownTicks());
      assertEquals(0, data.npcGlobalCooldown());
      assertEquals(0, data.npcCooldown());
   }
}
