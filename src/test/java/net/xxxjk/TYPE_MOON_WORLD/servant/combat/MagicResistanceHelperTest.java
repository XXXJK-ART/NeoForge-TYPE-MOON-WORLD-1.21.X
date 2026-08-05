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
      assertTrue(!MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.E, MagicComplexity.SIMPLE_ACTION));
      assertTrue(MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.D, MagicComplexity.SIMPLE_ACTION));
      assertTrue(!MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.D, MagicComplexity.ONE_VERSE));
      assertTrue(MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.C, MagicComplexity.ONE_VERSE));
      assertTrue(!MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.C, MagicComplexity.TWO_VERSE));
      assertTrue(MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.B, MagicComplexity.TWO_VERSE));
      assertTrue(MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.A, MagicComplexity.THREE_VERSE));
      assertTrue(!MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.A, MagicComplexity.HIGH_THAUMATURGY));
      assertTrue(MagicResistanceHelper.cancelsOrdinaryMagic(MagicResistanceRank.EX, MagicComplexity.HIGH_THAUMATURGY));
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
