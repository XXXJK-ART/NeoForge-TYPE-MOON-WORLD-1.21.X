package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.typemoonworld.api.MagicComplexity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagecraftMetadataTest {
   @Test
   void noblePhantasmsOptInExplicitlyAndRoundTrip() {
      var ordinary = ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString("{\"id\":\"example\"}")).result().orElseThrow();
      assertFalse(ordinary.magicDamage());
      var magic = ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE,
         JsonParser.parseString("{\"id\":\"spell_np\",\"magic_damage\":true,\"magic_complexity\":\"three_verse\"}"))
         .result().orElseThrow();
      assertTrue(magic.magicDamage());
      assertEquals(MagicComplexity.THREE_VERSE, magic.magicComplexity());
      var encoded = ServantNoblePhantasmDefinition.CODEC.encodeStart(JsonOps.INSTANCE, magic).result().orElseThrow();
      assertEquals(magic, ServantNoblePhantasmDefinition.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow());
   }
}
