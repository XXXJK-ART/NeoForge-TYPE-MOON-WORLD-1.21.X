package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class SoulLibraryTest {
   @Test
   void rejectsServantsAndFiltersLegacyServantSnapshots() {
      SoulLibrary library = new SoulLibrary();
      SoulSnapshot creature = snapshot(SoulSnapshot.SoulKind.CREATURE);
      SoulSnapshot servant = snapshot(SoulSnapshot.SoulKind.SERVANT);

      assertTrue(library.add(creature));
      assertFalse(library.add(servant));
      assertEquals(1, library.size());

      CompoundTag legacy = new CompoundTag();
      ListTag entries = new ListTag();
      entries.add(creature.save());
      entries.add(servant.save());
      legacy.put("Entries", entries);

      SoulLibrary loaded = new SoulLibrary();
      loaded.load(legacy);
      assertEquals(1, loaded.size());
   }

   private static SoulSnapshot snapshot(SoulSnapshot.SoulKind kind) {
      return new SoulSnapshot(UUID.randomUUID(), "minecraft:zombie", "Soul", kind, "", "", 0.6F, 1.8F,
         20.0, 3.0, 0.23, 0.0, 0.0, 0.0);
   }
}
