package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class WanderingTraderTradesHandler {
   private static final float COMMON_PRICE_MULTIPLIER = 0.05F;
   private static final float RARE_PRICE_MULTIPLIER = 0.2F;

   @SubscribeEvent
   public static void onWandererTrades(WandererTradesEvent event) {
      event.getGenericTrades().add(new BasicItemListing(4, new ItemStack((ItemLike)ModItems.MAGIC_SCROLL_BASIC_JEWEL_BROKEN.get()), 2, 1, 0.05F));
      event.getGenericTrades().add(new BasicItemListing(5, new ItemStack((ItemLike)ModItems.MAGIC_PAGE_REINFORCEMENT.get()), 2, 1, 0.05F));
      event.getRareTrades().add(new BasicItemListing(8, new ItemStack((ItemLike)ModItems.MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN.get()), 1, 2, 0.2F));
      event.getRareTrades().add(new BasicItemListing(10, new ItemStack((ItemLike)ModItems.MAGIC_SCROLL_PROJECTION_BROKEN.get()), 1, 2, 0.2F));
      event.getRareTrades().add(new BasicItemListing(14, new ItemStack((ItemLike)ModItems.MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN.get()), 1, 3, 0.2F));
   }
}
