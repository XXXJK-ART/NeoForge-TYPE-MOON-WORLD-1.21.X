package net.xxxjk.TYPE_MOON_WORLD.magic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MuramasaSlashHandlerTest {
   @Test
   void regularMuramasaDamageStopsAtOneHundred() {
      assertEquals(25.0F, MuramasaSlashHandler.muramasaDamage(1));
      assertEquals(100.0F, MuramasaSlashHandler.muramasaDamage(16));
      assertEquals(100.0F, MuramasaSlashHandler.muramasaDamage(100));
   }

   @Test
   void regularMuramasaManaBudgetIsTwoHundred() throws Exception {
      String source = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/item/custom/MuramasaItem.java"));
      assertTrue(source.contains("private static final double TOTAL_MANA_COST = 200.0;"));
      assertTrue(source.contains("private static final double MANA_COST_PER_TICK = TOTAL_MANA_COST / MAX_CHARGE_TICKS;"));
      assertTrue(source.contains("return (int)TOTAL_MANA_COST;"));
      assertTrue(source.contains("return MANA_COST_PER_TICK;"));
   }
}
