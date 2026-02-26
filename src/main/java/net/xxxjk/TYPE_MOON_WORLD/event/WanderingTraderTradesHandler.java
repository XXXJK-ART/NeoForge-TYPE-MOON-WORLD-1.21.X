package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class WanderingTraderTradesHandler {
    private static final float COMMON_PRICE_MULTIPLIER = 0.05F;
    private static final float RARE_PRICE_MULTIPLIER = 0.2F;

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(new BasicItemListing(4, new ItemStack(ModItems.MAGIC_SCROLL_BASIC_JEWEL_BROKEN.get()), 2, 1, COMMON_PRICE_MULTIPLIER));
        event.getGenericTrades().add(new BasicItemListing(5, new ItemStack(ModItems.MAGIC_PAGE_REINFORCEMENT.get()), 2, 1, COMMON_PRICE_MULTIPLIER));

        event.getRareTrades().add(new BasicItemListing(8, new ItemStack(ModItems.MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN.get()), 1, 2, RARE_PRICE_MULTIPLIER));
        event.getRareTrades().add(new BasicItemListing(10, new ItemStack(ModItems.MAGIC_SCROLL_PROJECTION_BROKEN.get()), 1, 2, RARE_PRICE_MULTIPLIER));
        event.getRareTrades().add(new BasicItemListing(14, new ItemStack(ModItems.MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN.get()), 1, 3, RARE_PRICE_MULTIPLIER));
    }
}
