package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MysticMagicianRankTest {
   @Test
   void configuredRangesMatchClockTowerRanks() {
      assertRange(MysticMagicianRank.GRAND, 10, 20, 90.0, 100.0, 500.0, 1000.0);
      assertRange(MysticMagicianRank.BRAND, 5, 10, 60.0, 100.0, 400.0, 1000.0);
      assertRange(MysticMagicianRank.PRIDE, 3, 6, 50.0, 80.0, 300.0, 1000.0);
      assertRange(MysticMagicianRank.FES, 3, 6, 40.0, 80.0, 100.0, 1000.0);
      assertRange(MysticMagicianRank.ADEPT, 2, 5, 30.0, 80.0, 100.0, 1000.0);
      assertRange(MysticMagicianRank.UMNOS, 2, 3, 20.0, 60.0, 100.0, 1000.0);
      assertRange(MysticMagicianRank.FRAME, 1, 3, 10.0, 40.0, 100.0, 1000.0);
   }

   @Test
   void fesHasSeparateEliteSpecialtyRange() {
      assertEquals(90.0, MysticMagicianRank.FES.minSpecialtyProficiency());
      assertEquals(100.0, MysticMagicianRank.FES.maxSpecialtyProficiency());
      assertEquals(30.0, MysticMagicianRank.ADEPT.minSpecialtyProficiency());
      assertEquals(80.0, MysticMagicianRank.ADEPT.maxSpecialtyProficiency());
   }

   @Test
   void oldEntityAndAllNewRegistrationsRemainPresent() throws Exception {
      String entities = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/init/ModEntities.java"));
      String items = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/item/ModItems.java"));
      String biome = Files.readString(Path.of(
         "src/main/resources/data/typemoonworld/neoforge/biome_modifier/mystic_magician.json"));

      assertTrue(entities.contains("MYSTIC_MAGICIAN ="));
      assertTrue(items.contains("MYSTIC_MAGICIAN_SPAWN_EGG"));
      for (MysticMagicianRank rank : MysticMagicianRank.values()) {
         String id = "mystic_magician_" + rank.id();
         assertTrue(entities.contains(id), id);
         assertTrue(items.contains(id + "_spawn_egg"), id);
         assertTrue(biome.contains(id), id);
      }
   }

   private static void assertRange(
      MysticMagicianRank rank,
      int minCount,
      int maxCount,
      double minProficiency,
      double maxProficiency,
      double minMana,
      double maxMana
   ) {
      assertEquals(minCount, rank.minMagicCount());
      assertEquals(maxCount, rank.maxMagicCount());
      assertEquals(minProficiency, rank.minProficiency());
      assertEquals(maxProficiency, rank.maxProficiency());
      assertEquals(minMana, rank.minMana());
      assertEquals(maxMana, rank.maxMana());
   }
}
