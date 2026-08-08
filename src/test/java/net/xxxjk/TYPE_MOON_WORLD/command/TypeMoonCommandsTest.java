package net.xxxjk.TYPE_MOON_WORLD.command;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TypeMoonCommandsTest {
   @Test
   void churchMagicsAreIncludedInLearnAllAndPlayerMaxCatalog() throws ReflectiveOperationException {
      Field field = TypeMoonCommands.class.getDeclaredField("ALL_MAGICS");
      field.setAccessible(true);
      Set<String> ids = Set.copyOf(Arrays.asList((String[])field.get(null)));

      assertTrue(ids.contains("black_key_fire_engraving"));
      assertTrue(ids.contains("stigma"));
      assertTrue(ids.contains("ubw_sword_control"));
      assertTrue(ids.contains("entity_displacement"));
   }
}
