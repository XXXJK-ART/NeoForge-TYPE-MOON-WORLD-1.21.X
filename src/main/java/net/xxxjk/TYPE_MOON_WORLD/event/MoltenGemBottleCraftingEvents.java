package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public final class MoltenGemBottleCraftingEvents {
   private MoltenGemBottleCraftingEvents() {
   }

   @SubscribeEvent
   public static void onItemCrafted(ItemCraftedEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer player)) {
         return;
      }
      boolean usedMoltenBottle = false;
      for (int slot = 0; slot < event.getInventory().getContainerSize(); slot++) {
         ItemStack input = event.getInventory().getItem(slot);
         if (input.is(ModItems.MOLTEN_RUBY_BOTTLE.get())
            || input.is(ModItems.MOLTEN_SAPPHIRE_BOTTLE.get())
            || input.is(ModItems.MOLTEN_EMERALD_BOTTLE.get())
            || input.is(ModItems.MOLTEN_TOPAZ_BOTTLE.get())
            || input.is(ModItems.MOLTEN_WHITE_GEMSTONE_BOTTLE.get())
            || input.is(ModItems.MOLTEN_CYAN_GEMSTONE_BOTTLE.get())) {
            usedMoltenBottle = true;
            break;
         }
      }
      if (usedMoltenBottle) {
         ItemStack slag = new ItemStack(ModItems.GEM_SLAG.get());
         if (!player.getInventory().add(slag)) {
            player.drop(slag, false);
         }
      }
   }
}
