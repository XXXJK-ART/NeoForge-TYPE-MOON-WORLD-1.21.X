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

   @Test
   void compactsSameTypeAndPersistsItsCount() {
      SoulLibrary library = new SoulLibrary();
      SoulSnapshot zombie = snapshot(SoulSnapshot.SoulKind.CREATURE);
      for (int i = 0; i < 50; i++) assertTrue(library.add(zombie));

      CompoundTag saved = library.save();
      assertEquals(1, saved.getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
      assertEquals(50, saved.getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getInt("Count"));
      assertFalse(saved.getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).contains("MaxHealth"));

      SoulLibrary loaded = new SoulLibrary();
      loaded.load(saved);
      assertEquals(50, loaded.size());
      assertEquals(50, loaded.takeStrongest(50).size());
      assertEquals(0, loaded.size());
   }

   @Test
   void enforcesCompactThousandSoulCapacity() {
      SoulLibrary library = new SoulLibrary();
      SoulSnapshot zombie = snapshot(SoulSnapshot.SoulKind.CREATURE);
      for (int i = 0; i < SoulLibrary.MAX_SOULS; i++) assertTrue(library.add(zombie));
      assertFalse(library.add(zombie));
      assertEquals(1000, library.size());
      assertEquals(1, library.save().getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
      assertEquals(1000, library.save().getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).getInt("Count"));
      assertEquals(50, SoulLibrary.MAX_MANIFESTED_SOULS);
   }

   @Test
   void loadingOversizedCountsStopsAtCapacity() {
      CompoundTag root = new CompoundTag();
      ListTag entries = new ListTag();
      CompoundTag entry = new CompoundTag();
      entry.putString("EntityType", "minecraft:zombie");
      entry.putString("Kind", SoulSnapshot.SoulKind.CREATURE.name());
      entry.putInt("Count", 5000);
      entries.add(entry);
      root.put("Entries", entries);

      SoulLibrary library = new SoulLibrary();
      library.load(root);

      assertEquals(SoulLibrary.MAX_SOULS, library.size());
      assertEquals(SoulLibrary.MAX_SOULS, library.takeStrongest(2000).size());
      assertEquals(0, library.size());
   }

   private static SoulSnapshot snapshot(SoulSnapshot.SoulKind kind) {
      return new SoulSnapshot(UUID.randomUUID(), "minecraft:zombie", "Soul", kind, "", "", 0.6F, 1.8F,
         20.0, 3.0, 0.23, 0.0, 0.0, 0.0);
   }
}
