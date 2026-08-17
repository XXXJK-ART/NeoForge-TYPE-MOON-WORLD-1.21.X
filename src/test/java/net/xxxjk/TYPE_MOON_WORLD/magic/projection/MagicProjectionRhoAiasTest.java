package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MagicProjectionRhoAiasTest {
   private static final Path SOURCE = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/magic/projection/MagicProjection.java");
   private static final Path ANALYSIS = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/magic/projection/MagicStructuralAnalysis.java");
   private static final Path HELPER = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/magic/projection/RhoAiasProjectionHelper.java");
   private static final Path EMIYA_CARD = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardEmiyaSkills.java");

   @Test
   void rhoAiasProjectionSpawnsEntityShieldBeforeHandCheck() throws Exception {
      String source = Files.readString(SOURCE);
      int targetRead = source.indexOf("ItemStack target = vars.projection_selected_item == null ? ItemStack.EMPTY : vars.projection_selected_item;");
      int rhoBranch = source.indexOf("if (isRhoAiasProjectionTarget(target))", targetRead);
      int handCheck = source.indexOf("InteractionHand handToUse = findAvailableHand(player);", targetRead);

      assertTrue(targetRead >= 0);
      assertTrue(rhoBranch > targetRead);
      assertTrue(handCheck > rhoBranch);
      assertTrue(source.contains("RhoAiasProjectionHelper.spawn(player)"));
      assertTrue(source.contains("isRhoAiasProjectionTarget(analyzedTarget)"));
   }

   @Test
   void rhoAiasRequiresUnlimitedBladeWorksForAnalysisAndProjection() throws Exception {
      String projection = Files.readString(SOURCE);
      String analysis = Files.readString(ANALYSIS);

      assertTrue(analysis.contains("isRhoAiasTarget(target) && !vars.has_unlimited_blade_works"));
      assertTrue(analysis.contains("message.typemoonworld.structural_analysis.rho_aias_requires_ubw"));
      assertTrue(projection.contains("if (!vars.has_unlimited_blade_works)"));
      assertTrue(projection.contains("message.typemoonworld.projection.rho_aias_requires_ubw"));
   }

   @Test
   void rhoAiasHelperMatchesEmiyaCardShieldSpawnPath() throws Exception {
      String helper = Files.readString(HELPER);
      String card = Files.readString(EMIYA_CARD);

      assertTrue(helper.contains("new RhoAiasEntity(level, player, findLookTarget(player, 24.0, 2.0))"));
      assertTrue(helper.contains("MobEffects.MOVEMENT_SLOWDOWN, 320, 2"));
      assertTrue(helper.contains("SoundEvents.ENCHANTMENT_TABLE_USE"));
      assertTrue(card.contains("RhoAiasProjectionHelper.spawn(player);"));
   }
}
