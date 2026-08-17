package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ServantCardEmiyaCopyRulesTest {
   private static final Path SOURCE = Path.of("src/main/java/net/xxxjk/TYPE_MOON_WORLD/servant/card/ServantCardEmiyaSkills.java");

   @Test
   void copyWeaponAcceptsAnyTypeMoonWorldItemHeldByTarget() throws IOException {
      String source = Files.readString(SOURCE);

      assertTrue(source.contains("ItemStack original = copyableHeldItem(target)"));
      assertTrue(source.contains("private static ItemStack copyableHeldItem(LivingEntity target)"));
      assertTrue(source.contains("private static boolean isTypeMoonWorldItem(ItemStack stack)"));
      assertTrue(source.contains("TYPE_MOON_WORLD.MOD_ID.equals(key.getNamespace())"));
      assertTrue(source.contains("return copyableHeldItem(target).isEmpty() ? null : target;"));
   }
}
