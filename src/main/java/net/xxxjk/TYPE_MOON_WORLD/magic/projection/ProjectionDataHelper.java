package net.xxxjk.TYPE_MOON_WORLD.magic.projection;

import java.util.Collection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Keeps projection lifetime and marker components when an item changes form. */
public final class ProjectionDataHelper {
   private ProjectionDataHelper() { }

   public static boolean isProjected(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean("is_projected");
   }

   public static boolean isInfinite(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag != null && tag.getBoolean("is_infinite_projection");
   }

   public static long projectionTime(ItemStack stack) {
      CompoundTag tag = customTag(stack);
      return tag == null ? Long.MIN_VALUE : tag.getLong("projection_time");
   }

   /** Copies projection metadata without replacing the destination item's identity. */
   public static void inherit(ItemStack source, ItemStack destination, long now) {
      if (source == null || destination == null || source.isEmpty() || !isProjected(source)) return;
      CompoundTag merged = customTag(destination);
      if (merged == null) merged = new CompoundTag();
      CompoundTag sourceTag = customTag(source);
      CompoundTag sourceData = sourceTag == null ? new CompoundTag() : sourceTag.copy();
      sourceData.merge(merged);
      merged = sourceData;
      merged.putBoolean("is_projected", true);
      if (sourceTag.contains("is_infinite_projection")) {
         merged.putBoolean("is_infinite_projection", sourceTag.getBoolean("is_infinite_projection"));
      }
      long time = sourceTag.contains("projection_time") ? sourceTag.getLong("projection_time") : now;
      merged.putLong("projection_time", time);
      destination.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
      destination.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      copyComponents(source, destination);
   }

   /** Tags a recipe result as a finite projection using the earliest input lifetime. */
   public static void inheritFinite(ItemStack destination, Collection<ItemStack> inputs, long now) {
      if (destination == null || destination.isEmpty() || inputs == null) return;
      long earliest = Long.MAX_VALUE;
      boolean found = false;
      boolean infinite = false;
      for (ItemStack input : inputs) {
         if (!isProjected(input)) continue;
         found = true;
         infinite |= isInfinite(input);
         long time = projectionTime(input);
         if (time != Long.MIN_VALUE) earliest = Math.min(earliest, time);
      }
      if (!found) return;
      CompoundTag merged = customTag(destination);
      if (merged == null) merged = new CompoundTag();
      merged.putBoolean("is_projected", true);
      if (infinite) merged.putBoolean("is_infinite_projection", true);
      else merged.remove("is_infinite_projection");
      merged.putLong("projection_time", earliest == Long.MAX_VALUE ? now : earliest);
      destination.set(DataComponents.CUSTOM_DATA, CustomData.of(merged));
      destination.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      copyComponents(inputs.stream().filter(ProjectionDataHelper::isProjected).findFirst().orElse(ItemStack.EMPTY), destination);
   }

   private static void copyComponents(ItemStack source, ItemStack destination) {
      if (source == null || source.isEmpty()) return;
      copy(source, destination, DataComponents.ENCHANTMENTS);
      copy(source, destination, DataComponents.ATTRIBUTE_MODIFIERS);
      copy(source, destination, DataComponents.DAMAGE);
      copy(source, destination, DataComponents.CUSTOM_NAME);
      copy(source, destination, DataComponents.LORE);
      copy(source, destination, DataComponents.UNBREAKABLE);
   }

   private static <T> void copy(ItemStack source, ItemStack destination, DataComponentType<T> type) {
      T value = source.get(type);
      if (value != null) destination.set(type, value);
   }

   private static CompoundTag customTag(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return null;
      CustomData data = stack.get(DataComponents.CUSTOM_DATA);
      return data == null ? null : data.copyTag();
   }
}
