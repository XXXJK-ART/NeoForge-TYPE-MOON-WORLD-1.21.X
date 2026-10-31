package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import net.xxxjk.typemoonworld.api.MagicComplexity;
import org.junit.jupiter.api.Test;

class MagicResistanceHelperTest {
   @Test
   void rankDefaultsMatchServantMagicResistanceBehavior() {
      assertEquals(0.85F, MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.EX), 0.0F);
      assertEquals(0.45F, MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.A), 0.0F);
      assertEquals(0.35F, MagicResistanceHelper.damageReductionForRank(MagicResistanceRank.B), 0.0F);
      assertEquals(0.75F, MagicResistanceHelper.debuffResistanceForRank(MagicResistanceRank.A), 0.0F);
      assertEquals(0.55F, MagicResistanceHelper.debuffResistanceForRank(MagicResistanceRank.B), 0.0F);
      assertEquals(0.35F, MagicResistanceHelper.debuffResistanceForRank(MagicResistanceRank.C), 0.0F);
      assertEquals(0, MagicResistanceHelper.harmfulMagicEffectDurationCap(MagicResistanceRank.A));
   }

   @Test
   void manaCapacityMapsOnlyOrdinaryManaUsers() {
      assertEquals(MagicResistanceRank.NONE, MagicResistanceHelper.rankFromManaCapacity(99.0));
      assertEquals(MagicResistanceRank.E, MagicResistanceHelper.rankFromManaCapacity(100.0));
      assertEquals(MagicResistanceRank.D, MagicResistanceHelper.rankFromManaCapacity(500.0));
      assertEquals(MagicResistanceRank.C, MagicResistanceHelper.rankFromManaCapacity(1000.0));
      assertEquals(MagicResistanceRank.B, MagicResistanceHelper.rankFromManaCapacity(3000.0));
      assertEquals(MagicResistanceRank.A, MagicResistanceHelper.rankFromManaCapacity(5000.0));
      assertEquals(MagicResistanceRank.EX, MagicResistanceHelper.rankFromManaCapacity(5000.1));
      assertEquals(MagicResistanceRank.EX, MagicResistanceRank.fromLevel(99));
   }

   @Test
   void complexityCancellationMatrixMatchesDesign() {
      for (MagicResistanceRank rank : MagicResistanceRank.values()) {
         for (MagicComplexity complexity : MagicComplexity.values()) {
            boolean immune = rank != MagicResistanceRank.NONE && complexity.level() < rank.level();
            assertEquals(immune, MagicResistanceHelper.cancelsOrdinaryMagic(rank, complexity), rank + "/" + complexity);
            if (immune) assertEquals(0, MagicResistanceHelper.harmfulMagicEffectDurationCap(rank, complexity));
         }
      }
   }

   @Test
   void deadApostleSpecialRanksAreExplicit() throws Exception {
      String helper = Files.readString(Path.of(
         "src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/combat/MagicResistanceHelper.java"));

      assertTrue(helper.contains("entity instanceof NeroChaosEntity || NeroChaosBeastLogic.isBeast(entity)"));
      assertTrue(helper.contains("return MagicResistanceRank.A;"));
      assertTrue(helper.contains("entity instanceof DeadApostleEntity"));
      assertTrue(helper.contains("return MagicResistanceRank.E;"));
   }
}
