package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SoulLibrary {
   public static final int MAX_SOULS = 999;
   private final List<SoulSnapshot> souls = new ArrayList<>();

   public boolean add(SoulSnapshot snapshot) {
      if (snapshot == null || this.souls.size() >= MAX_SOULS) {
         return false;
      }
      return this.souls.add(snapshot);
   }

   public boolean remove(UUID id) {
      return this.souls.removeIf(soul -> soul.id().equals(id));
   }

   public int size() {
      return this.souls.size();
   }

   public void clear() {
      this.souls.clear();
   }

   public List<SoulSnapshot> takeStrongest(int limit) {
      List<SoulSnapshot> selected = this.souls.stream()
         .sorted(Comparator.comparingDouble(SoulSnapshot::threat).reversed())
         .limit(Math.max(0, limit))
         .toList();
      this.souls.removeAll(selected);
      return selected;
   }

   public CompoundTag save() {
      CompoundTag root = new CompoundTag();
      ListTag entries = new ListTag();
      for (SoulSnapshot soul : this.souls) {
         entries.add(soul.save());
      }
      root.put("Entries", entries);
      return root;
   }

   public void load(CompoundTag root) {
      this.souls.clear();
      ListTag entries = root.getList("Entries", Tag.TAG_COMPOUND);
      for (int index = 0; index < entries.size() && this.souls.size() < MAX_SOULS; index++) {
         this.souls.add(SoulSnapshot.load(entries.getCompound(index)));
      }
   }
}
