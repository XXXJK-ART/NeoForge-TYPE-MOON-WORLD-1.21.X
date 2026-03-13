package net.xxxjk.TYPE_MOON_WORLD.magic.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;

public final class NpcMagicFilterService {
   private static final Set<String> CANDIDATE_MAGIC_POOL = Set.of(
      "gander", "gandr_machine_gun", "gravity_magic", "reinforcement", "projection", "broken_phantasm", "jewel_random_shoot", "jewel_machine_gun"
   );
   private static final Set<String> HARD_EXCLUDED_MAGICS = Set.of(
      "unlimited_blade_works", "sword_barrel_full_open", "structural_analysis", "jewel_magic_shoot", "jewel_magic_release"
   );
   private static final List<ResourceLocation> PROJECTION_WEAPON_POOL = List.of(
      ResourceLocation.withDefaultNamespace("stone_sword"),
      ResourceLocation.withDefaultNamespace("iron_sword"),
      ResourceLocation.withDefaultNamespace("diamond_sword"),
      ResourceLocation.withDefaultNamespace("netherite_sword"),
      ResourceLocation.withDefaultNamespace("iron_axe"),
      ResourceLocation.withDefaultNamespace("diamond_axe"),
      ResourceLocation.withDefaultNamespace("trident")
   );

   private NpcMagicFilterService() {
   }

   public static Set<String> candidateMagicPool() {
      return CANDIDATE_MAGIC_POOL;
   }

   public static boolean isMagicAllowedForNpc(String magicId) {
      return magicId != null && !magicId.isEmpty() ? CANDIDATE_MAGIC_POOL.contains(magicId) && !HARD_EXCLUDED_MAGICS.contains(magicId) : false;
   }

   public static boolean isMagicHardExcluded(String magicId) {
      return magicId != null && HARD_EXCLUDED_MAGICS.contains(magicId);
   }

   public static CompoundTag buildRandomPresetForMagic(String magicId, Provider lookupProvider, RandomSource random) {
      CompoundTag payload = new CompoundTag();
      if (magicId != null && !magicId.isEmpty()) {
         if (random == null) {
            random = RandomSource.create();
         }

         switch (magicId) {
            case "reinforcement":
               payload.putInt("reinforcement_target", 0);
               payload.putInt("reinforcement_mode", random.nextInt(4));
               payload.putInt("reinforcement_level", 1 + random.nextInt(5));
               return payload;
            case "gravity_magic":
               payload.putInt("gravity_target", random.nextBoolean() ? 1 : 0);
               int[] modes = new int[]{-2, -1, 0, 1, 2};
               payload.putInt("gravity_mode", modes[random.nextInt(modes.length)]);
               return payload;
            case "gandr_machine_gun":
               payload.putInt("gandr_machine_gun_mode", random.nextBoolean() ? 1 : 0);
               return payload;
            case "projection":
               ItemStack stack = chooseRandomCombatProjectionItem(random);
               if (!stack.isEmpty()) {
                  payload.put("projection_item", stack.save(lookupProvider));
               }

               return payload;
            case "broken_phantasm":
               ItemStack bpStack = chooseRandomCombatProjectionItem(random);
               if (!bpStack.isEmpty()) {
                  payload.put("bp_item", bpStack.save(lookupProvider));
               }

               return payload;
            default:
               return payload;
         }
      } else {
         return payload;
      }
   }

   public static CompoundTag sanitizePresetForNpc(String magicId, CompoundTag payload, Provider lookupProvider, RandomSource random) {
      CompoundTag source = payload == null ? new CompoundTag() : payload.copy();
      if (magicId != null && !magicId.isEmpty()) {
         if (random == null) {
            random = RandomSource.create();
         }

         switch (magicId) {
            case "reinforcement":
               CompoundTag outxxxx = new CompoundTag();
               outxxxx.putInt("reinforcement_target", 0);
               outxxxx.putInt(
                  "reinforcement_mode", Mth.clamp(source.contains("reinforcement_mode") ? source.getInt("reinforcement_mode") : random.nextInt(4), 0, 3)
               );
               outxxxx.putInt(
                  "reinforcement_level", Mth.clamp(source.contains("reinforcement_level") ? source.getInt("reinforcement_level") : 1 + random.nextInt(5), 1, 5)
               );
               return outxxxx;
            case "gravity_magic":
               CompoundTag outxxx = new CompoundTag();
               outxxx.putInt(
                  "gravity_target", Mth.clamp(source.contains("gravity_target") ? source.getInt("gravity_target") : (random.nextBoolean() ? 1 : 0), 0, 1)
               );
               outxxx.putInt("gravity_mode", Mth.clamp(source.contains("gravity_mode") ? source.getInt("gravity_mode") : 1, -2, 2));
               return outxxx;
            case "gandr_machine_gun":
               CompoundTag outxx = new CompoundTag();
               outxx.putInt("gandr_machine_gun_mode", Mth.clamp(source.contains("gandr_machine_gun_mode") ? source.getInt("gandr_machine_gun_mode") : 0, 0, 1));
               return outxx;
            case "projection":
               CompoundTag outx = new CompoundTag();
               ItemStack itemx = readItem(source, "projection_item", lookupProvider);
               if (!isCombatProjectionItem(itemx)) {
                  itemx = chooseRandomCombatProjectionItem(random);
               }

               if (!itemx.isEmpty()) {
                  outx.put("projection_item", itemx.save(lookupProvider));
               }

               return outx;
            case "broken_phantasm":
               CompoundTag out = new CompoundTag();
               ItemStack item = readItem(source, "bp_item", lookupProvider);
               if (item.isEmpty()) {
                  item = readItem(source, "projection_item", lookupProvider);
               }

               if (!isCombatProjectionItem(item)) {
                  item = chooseRandomCombatProjectionItem(random);
               }

               if (!item.isEmpty()) {
                  out.put("bp_item", item.save(lookupProvider));
               }

               return out;
            default:
               return source;
         }
      } else {
         return source;
      }
   }

   public static boolean isPresetValidForNpc(String magicId, CompoundTag payload, Provider lookupProvider) {
      if (magicId != null && !magicId.isEmpty()) {
         CompoundTag source = payload == null ? new CompoundTag() : payload;
         switch (magicId) {
            case "reinforcement":
               int target = source.contains("reinforcement_target") ? source.getInt("reinforcement_target") : 0;
               int mode = source.contains("reinforcement_mode") ? source.getInt("reinforcement_mode") : 0;
               int level = source.contains("reinforcement_level") ? source.getInt("reinforcement_level") : 1;
               return target == 0 && mode >= 0 && mode <= 3 && level >= 1 && level <= 5;
            case "projection":
               if (source.getBoolean("projection_lock_empty")) {
                  return false;
               } else {
                  if (source.contains("projection_structure_id")) {
                     return false;
                  }

                  ItemStack stack = readItem(source, "projection_item", lookupProvider);
                  return isCombatProjectionItem(stack);
               }
            case "broken_phantasm":
               ItemStack stack = readItem(source, "bp_item", lookupProvider);
               if (stack.isEmpty()) {
                  stack = readItem(source, "projection_item", lookupProvider);
               }

               return isCombatProjectionItem(stack);
            default:
               return true;
         }
      } else {
         return false;
      }
   }

   public static boolean isCombatProjectionItem(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         if (item instanceof BlockItem) {
            return false;
         } else if (!(item instanceof SwordItem)
            && !(item instanceof AxeItem)
            && !(item instanceof TridentItem)
            && !(item instanceof MaceItem)
            && !(item instanceof BowItem)
            && !(item instanceof CrossbowItem)) {
            ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            double baseDamage = modifiers.compute(0.0, EquipmentSlot.MAINHAND);
            return baseDamage > 1.0 || hasMainHandDamageModifier(modifiers);
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static ItemStack chooseRandomCombatProjectionItem(RandomSource random) {
      if (random == null) {
         random = RandomSource.create();
      }

      List<ItemStack> options = new ArrayList<>();

      for (ResourceLocation id : PROJECTION_WEAPON_POOL) {
         Item item = (Item)BuiltInRegistries.ITEM.get(id);
         if (item != null && item != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
            ItemStack stack = item.getDefaultInstance();
            if (isCombatProjectionItem(stack)) {
               options.add(stack);
            }
         }
      }

      return options.isEmpty() ? ItemStack.EMPTY : options.get(random.nextInt(options.size())).copy();
   }

   private static boolean hasMainHandDamageModifier(ItemAttributeModifiers modifiers) {
      for (Entry entry : modifiers.modifiers()) {
         if (entry != null && entry.attribute() != null && entry.modifier() != null) {
            String key = entry.attribute().unwrapKey().map(k -> k.location().toString()).orElse("");
            if ("minecraft:generic.attack_damage".equals(key)
               && (entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.HAND || entry.slot() == EquipmentSlotGroup.ANY)) {
               return true;
            }
         }
      }

      return false;
   }

   private static ItemStack readItem(CompoundTag source, String key, Provider lookupProvider) {
      return source != null && key != null && !key.isEmpty() && source.contains(key, 10)
         ? ItemStack.parse(lookupProvider, source.getCompound(key)).orElse(ItemStack.EMPTY)
         : ItemStack.EMPTY;
   }
}
