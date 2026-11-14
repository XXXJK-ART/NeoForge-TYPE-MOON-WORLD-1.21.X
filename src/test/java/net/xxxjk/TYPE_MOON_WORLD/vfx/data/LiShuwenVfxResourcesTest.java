package net.xxxjk.TYPE_MOON_WORLD.vfx.data;

import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiShuwenVfxResourcesTest {
   @Test
   void wuErDaUsesDedicatedValidEffectsInsteadOfTsubameAlias() throws Exception {
      Path effects = Path.of("src/main/resources/assets/typemoonworld/effects");
      for (String id : new String[]{"servant_li_shuwen_wu_er_da_windup", "servant_li_shuwen_wu_er_da_impact"}) {
         var json = JsonParser.parseString(Files.readString(effects.resolve(id + ".json"))).getAsJsonObject();
         var parsed = EffectLibrary.parseEffect(ResourceLocation.fromNamespaceAndPath("typemoonworld", id), json);
         assertTrue(parsed.duration() > 0.0F, id + " must have a positive duration");
         assertFalse(parsed.emitters().isEmpty(), id + " must define emitters");
      }

      Field aliasesField = EffectLibrary.class.getDeclaredField("LEGACY_EFFECT_ALIASES");
      aliasesField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, String> aliases = (Map<String, String>) aliasesField.get(null);
      assertFalse(aliases.containsKey("servant_li_shuwen_wu_er_da_windup"));
      assertFalse(aliases.containsKey("servant_li_shuwen_wu_er_da_impact"));
      assertFalse(aliases.containsKey("servant_li_wu_er_da"));
   }
}
