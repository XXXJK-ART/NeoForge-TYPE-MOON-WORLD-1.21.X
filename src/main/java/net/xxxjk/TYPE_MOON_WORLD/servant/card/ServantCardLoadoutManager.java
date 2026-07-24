package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

public final class ServantCardLoadoutManager {
   private static final String MAIN_HAND = "mainhand";
   private static final String OFF_HAND = "offhand";

   private ServantCardLoadoutManager() {
   }

   public static void saveAndEquip(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars, String servantId) {
      saveHands(player, vars);
      equip(player, servantId);
   }

   public static void restore(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      HolderLookup.Provider lookup = player.registryAccess();
      player.setItemInHand(InteractionHand.MAIN_HAND, parse(vars.servant_card_saved_hands, lookup, MAIN_HAND));
      player.setItemInHand(InteractionHand.OFF_HAND, parse(vars.servant_card_saved_hands, lookup, OFF_HAND));
      vars.servant_card_saved_hands = new CompoundTag();
   }

   private static void saveHands(ServerPlayer player, TypeMoonWorldModVariables.PlayerVariables vars) {
      HolderLookup.Provider lookup = player.registryAccess();
      CompoundTag tag = new CompoundTag();
      ItemStack main = player.getMainHandItem();
      ItemStack off = player.getOffhandItem();
      if (!main.isEmpty()) {
         tag.put(MAIN_HAND, main.save(lookup));
      }
      if (!off.isEmpty()) {
         tag.put(OFF_HAND, off.save(lookup));
      }
      vars.servant_card_saved_hands = tag;
   }

   private static ItemStack parse(CompoundTag tag, HolderLookup.Provider lookup, String key) {
      if (tag != null && tag.contains(key, 10)) {
         return ItemStack.parseOptional(lookup, tag.getCompound(key));
      }
      return ItemStack.EMPTY;
   }

   private static void equip(ServerPlayer player, String servantId) {
      ItemStack main = ItemStack.EMPTY;
      ItemStack off = ItemStack.EMPTY;
      switch (servantId == null ? "" : servantId) {
         case "artoria_pendragon" -> {
            main = veiledExcalibur();
         }
         case "cu_chulainn" -> main = stack(ModItems.GAE_BULG.get());
         case "heracles" -> main = stack(ModItems.TEMPLE_STONE_SWORD_AXE.get());
         case "medea" -> {
            off = stack(ModItems.HECATES_STAFF.get());
         }
         case "medusa" -> main = stack(ModItems.NAMELESS_CHAIN_DAGGER.get());
         case "cursed_arm_hassan" -> off = stack(ModItems.DIRK_SMALL_KNIFE.get());
         case "emiya_archer" -> {
         }
         case "gilgamesh" -> {
         }
         case "sasaki_kojiro" -> main = stack(ModItems.BIZEN_NAGAMITSU.get());
         case "oda_nobunaga" -> main = stack(ModItems.HESHIKIRI_HASEBE.get());
         case "gawain" -> main = stack(ModItems.EXCALIBUR_GALLATIN.get());
         case "paracelsus" -> main = stack(ModItems.PARACELSUS_SWORD.get());
         case "ushiwakamaru_rider" -> main = stack(ModItems.SPIDER_CUTTER.get());
         default -> {
         }
      }
      ServantCardTransformManager.markGeneratedItem(main, true, false);
      ServantCardTransformManager.markGeneratedItem(off, true, false);
      player.setItemInHand(InteractionHand.MAIN_HAND, main);
      player.setItemInHand(InteractionHand.OFF_HAND, off);
   }

   private static ItemStack stack(Item item) {
      return item == null ? ItemStack.EMPTY : new ItemStack(item);
   }

   private static ItemStack veiledExcalibur() {
      ItemStack stack = stack(ModItems.EXCALIBUR.get());
      if (!stack.isEmpty()) {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("ServantCardArtoriaWindVeiled", true);
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      }
      return stack;
   }
}
