package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Compact soul storage. A type has one summon template and a count; manifested
 * souls are expanded to snapshots only while the domain is being populated.
 */
public final class SoulLibrary {
   public static final int MAX_SOULS = 1000;
   public static final int MAX_MANIFESTED_SOULS = 50;
   private final Map<String, Entry> entries = new LinkedHashMap<>();
   private int totalCount;

   public boolean add(SoulSnapshot snapshot) {
      if (snapshot == null || snapshot.kind() == SoulSnapshot.SoulKind.SERVANT || size() >= MAX_SOULS) return false;
      String key = key(snapshot);
      Entry entry = this.entries.get(key);
      if (entry == null) {
         this.entries.put(key, new Entry(snapshot, 1));
      } else {
         // Keep the strongest template for deterministic "strongest 50" release.
         if (snapshot.threat() > entry.template.threat()) entry.template = snapshot;
         entry.count++;
      }
      this.totalCount++;
      return true;
   }

   public int size() {
      return this.totalCount;
   }

   public void clear() {
      this.entries.clear();
      this.totalCount = 0;
   }

   public List<SoulSnapshot> takeStrongest(int limit) {
      int remaining = Math.max(0, Math.min(limit, size()));
      if (remaining == 0) return List.of();
      List<SoulSnapshot> selected = new ArrayList<>(remaining);
      List<Entry> sorted = this.entries.values().stream()
         .sorted(Comparator.comparingDouble((Entry entry) -> entry.template.threat()).reversed())
         .toList();
      for (Entry entry : sorted) {
         int amount = Math.min(remaining, entry.count);
         for (int i = 0; i < amount; i++) {
            SoulSnapshot snapshot = copyWithNewId(entry.template);
            selected.add(snapshot);
         }
         entry.count -= amount;
         this.totalCount -= amount;
         remaining -= amount;
         if (remaining == 0) break;
      }
      this.entries.values().removeIf(entry -> entry.count <= 0);
      return selected;
   }

   public CompoundTag save() {
      CompoundTag root = new CompoundTag();
      ListTag entries = new ListTag();
      for (Entry entry : this.entries.values()) {
         CompoundTag tag = new CompoundTag();
         tag.putString("EntityType", entry.template.entityType());
         tag.putString("Kind", entry.template.kind().name());
         tag.putInt("Count", entry.count);
         entries.add(tag);
      }
      root.put("Entries", entries);
      return root;
   }

   public void load(CompoundTag root) {
      this.clear();
      ListTag list = root.getList("Entries", Tag.TAG_COMPOUND);
      for (int index = 0; index < list.size(); index++) {
         CompoundTag tag = list.getCompound(index);
         SoulSnapshot.SoulKind kind;
         try {
            kind = SoulSnapshot.SoulKind.valueOf(tag.getString("Kind"));
         } catch (IllegalArgumentException ignored) {
            kind = SoulSnapshot.SoulKind.CREATURE;
         }
         SoulSnapshot snapshot;
         if (tag.contains("MaxHealth", Tag.TAG_DOUBLE)) {
            snapshot = SoulSnapshot.load(tag);
         } else {
            try {
               snapshot = SoulSnapshot.fromEntityType(tag.getString("EntityType"), kind);
            } catch (Throwable ignored) {
               // Unit-test/headless environments do not bootstrap Minecraft registries.
               snapshot = new SoulSnapshot(UUID.randomUUID(), tag.getString("EntityType"), tag.getString("EntityType"),
                  kind, "", "", 0.6F, 1.8F, 20.0, 2.0, 0.23, 0.0, 0.0, 0.0);
            }
         }
         if (snapshot.kind() == SoulSnapshot.SoulKind.SERVANT) continue;
         int available = MAX_SOULS - this.totalCount;
         if (available <= 0) break;
         int count = tag.contains("Count", Tag.TAG_INT) ? tag.getInt("Count") : 1;
         count = Math.max(1, Math.min(available, count));
         String key = key(snapshot);
         Entry existing = this.entries.get(key);
         if (existing == null) this.entries.put(key, new Entry(snapshot, count));
         else {
            existing.count += count;
            if (snapshot.threat() > existing.template.threat()) existing.template = snapshot;
         }
         this.totalCount += count;
         if (this.totalCount >= MAX_SOULS) break;
      }
   }

   private static String key(SoulSnapshot snapshot) {
      return snapshot.kind().name() + "|" + snapshot.entityType();
   }

   private static SoulSnapshot copyWithNewId(SoulSnapshot source) {
      return new SoulSnapshot(UUID.randomUUID(), source.entityType(), source.displayName(), source.kind(),
         source.playerProfile(), source.playerUuid(), source.width(), source.height(), source.maxHealth(),
         source.attackDamage(), source.movementSpeed(), source.armor(), source.armorToughness(), source.knockbackResistance());
   }

   private static final class Entry {
      private SoulSnapshot template;
      private int count;

      private Entry(SoulSnapshot template, int count) {
         this.template = template;
         this.count = count;
      }
   }
}
