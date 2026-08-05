package net.xxxjk.TYPE_MOON_WORLD.servant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MedusaEyeStateTest {
   private static final Path ROOT = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD");

   @Test
   void entityStateUsesEyesAsTheInverseOfTheBlindfold() throws Exception {
      String source = Files.readString(ROOT.resolve("servant/entity/MedusaEntity.java"));
      assertTrue(source.contains("this.entityData.set(EYES_RELEASED, !sealed)"));
      assertTrue(source.contains("this.entityData.set(BLINDFOLD_SEALED, !eyesReleased)"));
   }

   @Test
   void armorRendererUsesSyncedNpcEyesInsteadOfPlayerVariables() throws Exception {
      String source = Files.readString(ROOT.resolve("item/custom/ServantCardArmorItem.java"));
      assertTrue(source.contains("living instanceof MedusaEntity medusa"));
      assertTrue(source.contains("return medusa.isEyesReleased()"));
      assertFalse(source.contains("return medusa.isBlindfoldSealed()"));
   }
}
