package net.xxxjk.TYPE_MOON_WORLD.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.xxxjk.TYPE_MOON_WORLD.init.ModVillagers;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.util.ModTags;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "typemoonworld"
)
public class WanderingTraderTradesHandler {
   private static final float COMMON_PRICE_MULTIPLIER = 0.05F;
   private static final float RARE_PRICE_MULTIPLIER = 0.2F;
   private static final List<MagicTradeEntry> MAGIC_PAGES = List.of(
      new MagicTradeEntry("magic_page_detection", 10),
      new MagicTradeEntry("magic_page_airflow_blade", 20),
      new MagicTradeEntry("magic_page_magic_bullet", 20),
      new MagicTradeEntry("magic_page_earth", 25),
      new MagicTradeEntry("magic_page_fire", 25),
      new MagicTradeEntry("magic_page_reinforcement", 25),
      new MagicTradeEntry("magic_page_water", 25),
      new MagicTradeEntry("magic_page_wind", 25),
      new MagicTradeEntry("magic_page_structural_analysis", 30),
      new MagicTradeEntry("magic_page_healing", 35),
      new MagicTradeEntry("magic_page_projection", 35),
      new MagicTradeEntry("magic_page_binding", 45),
      new MagicTradeEntry("magic_page_suggestion", 45),
      new MagicTradeEntry("magic_page_spiritual_healing", 50),
      new MagicTradeEntry("magic_page_magic_analysis", 55),
      new MagicTradeEntry("magic_page_imaginary_dive", 55),
      new MagicTradeEntry("magic_page_black_key_fire_engraving", 60),
      new MagicTradeEntry("magic_page_stigma", 60),
      new MagicTradeEntry("magic_page_storage", 60),
      new MagicTradeEntry("magic_page_baptism_rite", 65),
      new MagicTradeEntry("magic_page_absorption", 70),
      new MagicTradeEntry("magic_page_storm", 72),
      new MagicTradeEntry("magic_page_kimaris", 75),
      new MagicTradeEntry("magic_page_imaginary_displacement", 78),
      new MagicTradeEntry("magic_page_zagan", 78),
      new MagicTradeEntry("magic_page_orias", 80),
      new MagicTradeEntry("magic_page_andrasias", 82),
      new MagicTradeEntry("magic_page_imaginary_space", 85),
      new MagicTradeEntry("magic_page_andrephius", 88),
      new MagicTradeEntry("magic_page_antores", 90),
      new MagicTradeEntry("magic_page_demon_god_gaze", 92),
      new MagicTradeEntry("magic_page_nega_summon", 95)
      , new MagicTradeEntry("magic_page_flame_array", 45)
      , new MagicTradeEntry("magic_page_azure_water_array", 45)
      , new MagicTradeEntry("magic_page_gale_wind_array", 45)
      , new MagicTradeEntry("magic_page_rock_earth_array", 45)
      , new MagicTradeEntry("magic_page_contract_magecraft", 35)
      , new MagicTradeEntry("magic_page_aerial_stasis", 12)
      , new MagicTradeEntry("magic_page_aerial_ascent", 18)
      , new MagicTradeEntry("magic_page_touko_travel", 40)
      , new MagicTradeEntry("magic_page_flight_magic", 85)
   );
   private static final List<MagicTradeEntry> MAGIC_BOOKS = MAGIC_PAGES.stream()
      .map(entry -> new MagicTradeEntry(entry.itemPath().replace("magic_page_", "magic_book_"), entry.complexity()))
      .toList();

   @SubscribeEvent
   public static void onWandererTrades(WandererTradesEvent event) {
      event.getGenericTrades().add(new BasicItemListing(4, new ItemStack(ModItems.MAGIC_SCROLL_BASIC_JEWEL_BROKEN.get()), 2, 1, 0.05F));
      event.getGenericTrades().add(new BasicItemListing(5, new ItemStack(ModItems.MAGIC_PAGE_REINFORCEMENT.get()), 2, 1, 0.05F));
      event.getRareTrades().add(new BasicItemListing(8, new ItemStack(ModItems.MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN.get()), 1, 2, 0.2F));
      event.getRareTrades().add(new BasicItemListing(10, new ItemStack(ModItems.MAGIC_SCROLL_PROJECTION_BROKEN.get()), 1, 2, 0.2F));
      event.getRareTrades().add(new BasicItemListing(14, new ItemStack(ModItems.MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN.get()), 1, 3, 0.2F));
   }

   @SubscribeEvent
   public static void onVillagerTrades(VillagerTradesEvent event) {
      if (event.getType() == ModVillagers.MAGICIAN.get()) {
         addMagicianTrades(event);
         return;
      }

      if (event.getType() != VillagerProfession.LIBRARIAN) {
         return;
      }

      event.getTrades().get(1).add(new WeightedMagicTradeListing(entriesUpTo(MAGIC_PAGES, 35), false, 0.28D));
      event.getTrades().get(2).add(new WeightedMagicTradeListing(entriesBetween(MAGIC_PAGES, 20, 55), false, 0.22D));
      event.getTrades().get(3).add(new WeightedMagicTradeListing(entriesBetween(MAGIC_PAGES, 35, 75), false, 0.16D));
      event.getTrades().get(4).add(new WeightedMagicTradeListing(entriesBetween(MAGIC_PAGES, 50, 88), false, 0.10D));
      event.getTrades().get(4).add(new WeightedMagicTradeListing(entriesUpTo(MAGIC_BOOKS, 65), true, 0.006D));
      event.getTrades().get(5).add(new WeightedMagicTradeListing(MAGIC_PAGES, false, 0.07D));
      event.getTrades().get(5).add(new WeightedMagicTradeListing(MAGIC_BOOKS, true, 0.012D));
   }

   private static void addMagicianTrades(VillagerTradesEvent event) {
      event.getTrades().get(1).add(new MaterialPurchaseListing(1, 0.75D));
      event.getTrades().get(1).add(new MagicianMagicPageListing(entriesUpTo(MAGIC_PAGES, 45), 1.0D));
      event.getTrades().get(2).add(new MaterialPurchaseListing(2, 0.70D));
      event.getTrades().get(2).add(new MagicianMagicPageListing(entriesBetween(MAGIC_PAGES, 25, 60), 0.95D));
      event.getTrades().get(2).add(new BasicItemListing(3, new ItemStack(ModItems.SULFUR.get(), 2), 8, 0, COMMON_PRICE_MULTIPLIER));
      event.getTrades().get(3).add(new MaterialPurchaseListing(3, 0.65D));
      event.getTrades().get(3).add(new MagicianMagicPageListing(entriesBetween(MAGIC_PAGES, 45, 78), 0.90D));
      event.getTrades().get(3).add(new BasicItemListing(5, new ItemStack(ModItems.MERCURY_BOTTLE.get()), 6, 0, COMMON_PRICE_MULTIPLIER));
      event.getTrades().get(4).add(new MaterialPurchaseListing(4, 0.60D));
      event.getTrades().get(4).add(new MagicianMagicPageListing(entriesBetween(MAGIC_PAGES, 60, 88), 0.85D));
      event.getTrades().get(5).add(new MaterialPurchaseListing(5, 0.55D));
      event.getTrades().get(5).add(new MagicianMagicPageListing(MAGIC_PAGES, 0.80D));
      event.getTrades().get(5).add(new BasicItemListing(12, new ItemStack(ModItems.SELF_GEAS_SCROLL.get()), 2, 0, RARE_PRICE_MULTIPLIER));
   }

   private static List<MagicTradeEntry> entriesUpTo(List<MagicTradeEntry> entries, int maxComplexity) {
      return entries.stream().filter(entry -> entry.complexity() <= maxComplexity).toList();
   }

   private static List<MagicTradeEntry> entriesBetween(List<MagicTradeEntry> entries, int minComplexity, int maxComplexity) {
      return entries.stream().filter(entry -> entry.complexity() >= minComplexity && entry.complexity() <= maxComplexity).toList();
   }

   private record MagicTradeEntry(String itemPath, int complexity) {
      int weight() {
         if (this.complexity <= 20) return 16;
         if (this.complexity <= 35) return 12;
         if (this.complexity <= 50) return 8;
         if (this.complexity <= 65) return 5;
         if (this.complexity <= 80) return 3;
         if (this.complexity <= 89) return 2;
         return 1;
      }

      int emeraldCost(boolean book) {
         if (book) {
            return Math.max(24, Math.min(64, 18 + (int)Math.ceil(this.complexity * 0.55D)));
         }
         return Math.max(3, Math.min(34, 2 + (int)Math.ceil(this.complexity / 4.0D)));
      }
   }

   private record WeightedMagicTradeListing(List<MagicTradeEntry> entries, boolean book, double chance) implements ItemListing {
      @Nullable
      @Override
      public MerchantOffer getOffer(Entity trader, RandomSource random) {
         if (this.entries.isEmpty() || random.nextDouble() >= this.chance) {
            return null;
         }

         MagicTradeEntry entry = pickWeighted(this.entries, random);
         Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("typemoonworld", entry.itemPath()));
         if (item == Items.AIR) {
            return null;
         }

         ItemStack price = new ItemStack(Items.EMERALD, entry.emeraldCost(this.book));
         Optional<ItemCost> secondCost = Optional.empty();
         if (this.book) {
            ItemStack bookStack = new ItemStack(Items.BOOK);
            secondCost = Optional.of(new ItemCost(bookStack.getItemHolder(), bookStack.getCount(), DataComponentPredicate.EMPTY, bookStack));
         }
         ItemCost firstCost = new ItemCost(price.getItemHolder(), price.getCount(), DataComponentPredicate.EMPTY, price);
         return new MerchantOffer(firstCost, secondCost, new ItemStack(item), this.book ? 1 : 2, this.book ? 20 : 5, this.book ? RARE_PRICE_MULTIPLIER : COMMON_PRICE_MULTIPLIER);
      }

      private static MagicTradeEntry pickWeighted(List<MagicTradeEntry> entries, RandomSource random) {
         int total = entries.stream().mapToInt(MagicTradeEntry::weight).sum();
         int roll = random.nextInt(Math.max(1, total));
         for (MagicTradeEntry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
               return entry;
            }
         }
         return entries.get(entries.size() - 1);
      }
   }

   private record MagicianMagicPageListing(List<MagicTradeEntry> entries, double chance) implements ItemListing {
      @Nullable
      @Override
      public MerchantOffer getOffer(Entity trader, RandomSource random) {
         if (this.entries.isEmpty() || random.nextDouble() >= this.chance) {
            return null;
         }

         MagicTradeEntry entry = WeightedMagicTradeListing.pickWeighted(this.entries, random);
         Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("typemoonworld", entry.itemPath()));
         Item material = pickMagicMaterial(random);
         if (item == Items.AIR || material == Items.AIR) {
            return null;
         }

         ItemStack emeralds = new ItemStack(Items.EMERALD, entry.emeraldCost(false));
         ItemStack materialStack = new ItemStack(material, 16);
         return new MerchantOffer(
            cost(emeralds),
            Optional.of(cost(materialStack)),
            new ItemStack(item),
            1,
            0,
            COMMON_PRICE_MULTIPLIER
         );
      }
   }

   private record MaterialPurchaseListing(int emeralds, double chance) implements ItemListing {
      @Nullable
      @Override
      public MerchantOffer getOffer(Entity trader, RandomSource random) {
         if (random.nextDouble() >= this.chance) {
            return null;
         }

         Item material = pickMagicMaterial(random);
         if (material == Items.AIR) {
            return null;
         }

         return new MerchantOffer(
            cost(new ItemStack(material, 16)),
            new ItemStack(Items.EMERALD, this.emeralds),
            12,
            0,
            COMMON_PRICE_MULTIPLIER
         );
      }
   }

   private static ItemCost cost(ItemStack stack) {
      return new ItemCost(stack.getItemHolder(), stack.getCount(), DataComponentPredicate.EMPTY, stack);
   }

   private static Item pickMagicMaterial(RandomSource random) {
      List<Item> items = new ArrayList<>();
      for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModTags.Items.MAGIC_MATERIALS)) {
         Item item = holder.value();
         if (item != Items.AIR) {
            items.add(item);
         }
      }
      return items.isEmpty() ? Items.AIR : items.get(random.nextInt(items.size()));
   }
}
