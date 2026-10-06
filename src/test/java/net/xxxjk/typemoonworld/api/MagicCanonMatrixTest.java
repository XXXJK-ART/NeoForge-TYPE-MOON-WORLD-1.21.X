package net.xxxjk.typemoonworld.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class MagicCanonMatrixTest {
   @Test
   void definitionRoundTripPreservesCanonConstraints() {
      MagicDefinitionData data = MagicDefinitionData.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
         {"id":"typemoonworld:ubw_test","canon_status":"character_exclusive","source_key":"fate/emiya",
          "magic_type":"reality_marble","required_origin":"sword","required_foundation":"projection",
          "required_catalyst":"sword","cast_mode":"ritual","resource_model":"personal_mana",
          "copy_policy":"knowledge_only","player_allowed":true,"npc_allowed":false}
         """)).result().orElseThrow();
      assertEquals("character_exclusive", data.canonStatus());
      assertEquals("fate/emiya", data.sourceKey());
      assertEquals("sword", data.requiredOrigin());
      assertEquals("ritual", data.castMode());
      assertEquals("knowledge_only", data.copyPolicy());
      assertTrue(!data.npcAllowed());
      assertTrue(data.toJson().has("required_foundation"));
   }

   @Test
   void aliasesKeepLegacyWheelIdsResolvable() {
      assertEquals("unlimited_blade_works", MagicIdMigration.normalize("typemoonworld:ubw"));
      assertEquals("gander", MagicIdMigration.normalize("gandr"));
      assertTrue(MagicCanonMatrix.forId("projection").playerAllowed());
   }
}
