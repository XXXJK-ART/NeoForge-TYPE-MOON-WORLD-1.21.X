package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.MagicResistanceRank;
import org.junit.jupiter.api.Test;

class ServantCardTraitServiceClassLoadingTest {
   @Test
   void cardResistanceLoadsWithoutCompilerGeneratedCompanionClasses() throws Exception {
      String name = "net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTraitService";
      ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
         @Override
         protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
            if (requested.startsWith(name + "$")) {
               throw new ClassNotFoundException(requested);
            }
            if (!requested.equals(name)) {
               return super.loadClass(requested, resolve);
            }
            Class<?> type = findLoadedClass(requested);
            if (type == null) {
               try (InputStream input = getParent().getResourceAsStream(requested.replace('.', '/') + ".class")) {
                  if (input == null) {
                     throw new ClassNotFoundException(requested);
                  }
                  byte[] bytes = input.readAllBytes();
                  type = defineClass(requested, bytes, 0, bytes.length);
               } catch (IOException e) {
                  throw new ClassNotFoundException(requested, e);
               }
            }
            if (resolve) {
               resolveClass(type);
            }
            return type;
         }
      };
      var resistance = loader.loadClass(name).getDeclaredMethod("debuffResistanceForRank", MagicResistanceRank.class);
      resistance.setAccessible(true);
      assertEquals(0.75F, (float) resistance.invoke(null, MagicResistanceRank.A));
      assertEquals(0.55F, (float) resistance.invoke(null, MagicResistanceRank.B));
      assertEquals(0.35F, (float) resistance.invoke(null, MagicResistanceRank.C));
      assertEquals(0.15F, (float) resistance.invoke(null, MagicResistanceRank.D));
      assertEquals(0.06F, (float) resistance.invoke(null, MagicResistanceRank.E));
      assertEquals(0.90F, (float) resistance.invoke(null, MagicResistanceRank.EX));
      for (MagicResistanceRank rank : new MagicResistanceRank[] {
         MagicResistanceRank.NONE, null
      }) {
         assertEquals(0.0F, (float) resistance.invoke(null, rank));
      }
   }
}
