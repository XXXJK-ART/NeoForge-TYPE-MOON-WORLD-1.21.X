package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LeylineSurveyMapItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ManaSurveyCompassItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomMagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.StoneManSpawnEggItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem;

public class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("typemoonworld");
   public static final DeferredItem<Item> TEMPLE_STONE_SWORD_AXE = ITEMS.register(
      "temple_stone_sword_axe",
      () -> new TempleStoneSwordAxeItem(
         new Properties()
            .rarity(Rarity.EPIC)
            .fireResistant()
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "temple_stone_sword_axe_damage"), 20.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "temple_stone_sword_axe_speed"), -3.5, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
      )
   );
   public static final DeferredItem<Item> MURAMASA = ITEMS.register(
      "redsword",
      () -> new MuramasaItem(
         new Properties()
            .durability(2000)
            .fireResistant()
            .rarity(Rarity.RARE)
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "redsword_damage"), 9.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "redsword_speed"), 0.5, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
      )
   );
   public static final DeferredItem<Item> TSUMUKARI_MURAMASA = ITEMS.register(
      "tsumukari_muramasa",
      () -> new TsumukariMuramasaItem(
         new Properties()
            .durability(5000)
            .fireResistant()
            .rarity(Rarity.EPIC)
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "tsumukari_damage"), 15.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "tsumukari_speed"), 0.3, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
      )
   );
   public static final DeferredItem<Item> MAGIC_FRAGMENTS = ITEMS.register("magic_fragments", () -> new Magic_fragmentsItem(new Properties()));
   public static final DeferredItem<Item> MAGIC_CREST = ITEMS.register("magic_crest", () -> new MagicCrestItem(new Properties().rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> GANDER = ITEMS.register(
      "gander", () -> new Item(new Properties().requiredFeatures(new FeatureFlag[]{FeatureFlags.TRADE_REBALANCE}))
   );
   public static final DeferredItem<Item> RANDOM_GEM = ITEMS.register("random_gem", () -> new RandomGemItem(new Properties()));
   public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holy_shroud", () -> new Item(new Properties().rarity(Rarity.UNCOMMON)));
   public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel", () -> new ChiselItem(new Properties().durability(100)));
   public static final DeferredItem<Item> MANA_SURVEY_COMPASS = ITEMS.register(
      "mana_survey_compass", () -> new ManaSurveyCompassItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 80, 80)
   );
   public static final DeferredItem<Item> LEYLINE_SURVEY_MAP = ITEMS.register(
      "leyline_survey_map", () -> new LeylineSurveyMapItem(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON))
   );
   public static final DeferredItem<Item> MANA_SURVEY_BASE = ITEMS.register("mana_survey_base", () -> new Item(new Properties()));
   public static final DeferredItem<Item> MANA_SURVEY_POINTER = ITEMS.register("mana_survey_pointer", () -> new Item(new Properties()));
   public static final DeferredItem<Item> COPPER_MANA_SURVEY_COMPASS = ITEMS.register(
      "copper_mana_survey_compass", () -> new ManaSurveyCompassItem(new Properties().stacksTo(1), 50, 80)
   );
   public static final DeferredItem<Item> COPPER_MANA_SURVEY_BASE = ITEMS.register("copper_mana_survey_base", () -> new Item(new Properties()));
   public static final DeferredItem<Item> COPPER_MANA_SURVEY_POINTER = ITEMS.register("copper_mana_survey_pointer", () -> new Item(new Properties()));
   public static final DeferredItem<Item> CARVED_EMERALD = ITEMS.register(
      "carved_emerald", () -> new CarvedGemItem(new Properties(), GemType.EMERALD, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_EMERALD_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_EMERALD_FULL = ITEMS.register(
      "carved_emerald_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_EMERALD.get(), GemQuality.NORMAL, GemType.EMERALD)
   );
   public static final DeferredItem<Item> CARVED_EMERALD_POOR = ITEMS.register(
      "carved_emerald_poor", () -> new CarvedGemItem(new Properties(), GemType.EMERALD, GemQuality.POOR, () -> (Item)ModItems.CARVED_EMERALD_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_EMERALD_POOR_FULL = ITEMS.register(
      "carved_emerald_poor_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_EMERALD_POOR.get(), GemQuality.POOR, GemType.EMERALD)
   );
   public static final DeferredItem<Item> CARVED_EMERALD_HIGH = ITEMS.register(
      "carved_emerald_high", () -> new CarvedGemItem(new Properties(), GemType.EMERALD, GemQuality.HIGH, () -> (Item)ModItems.CARVED_EMERALD_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_EMERALD_HIGH_FULL = ITEMS.register(
      "carved_emerald_high_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_EMERALD_HIGH.get(), GemQuality.HIGH, GemType.EMERALD)
   );
   public static final DeferredItem<Item> CARVED_RUBY = ITEMS.register(
      "carved_ruby", () -> new CarvedGemItem(new Properties(), GemType.RUBY, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_RUBY_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_RUBY_FULL = ITEMS.register(
      "carved_ruby_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_RUBY.get(), GemQuality.NORMAL, GemType.RUBY)
   );
   public static final DeferredItem<Item> CARVED_RUBY_POOR = ITEMS.register(
      "carved_ruby_poor", () -> new CarvedGemItem(new Properties(), GemType.RUBY, GemQuality.POOR, () -> (Item)ModItems.CARVED_RUBY_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_RUBY_POOR_FULL = ITEMS.register(
      "carved_ruby_poor_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_RUBY_POOR.get(), GemQuality.POOR, GemType.RUBY)
   );
   public static final DeferredItem<Item> CARVED_RUBY_HIGH = ITEMS.register(
      "carved_ruby_high", () -> new CarvedGemItem(new Properties(), GemType.RUBY, GemQuality.HIGH, () -> (Item)ModItems.CARVED_RUBY_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_RUBY_HIGH_FULL = ITEMS.register(
      "carved_ruby_high_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_RUBY_HIGH.get(), GemQuality.HIGH, GemType.RUBY)
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE = ITEMS.register(
      "carved_sapphire", () -> new CarvedGemItem(new Properties(), GemType.SAPPHIRE, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_SAPPHIRE_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE_FULL = ITEMS.register(
      "carved_sapphire_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_SAPPHIRE.get(), GemQuality.NORMAL, GemType.SAPPHIRE)
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE_POOR = ITEMS.register(
      "carved_sapphire_poor",
      () -> new CarvedGemItem(new Properties(), GemType.SAPPHIRE, GemQuality.POOR, () -> (Item)ModItems.CARVED_SAPPHIRE_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE_POOR_FULL = ITEMS.register(
      "carved_sapphire_poor_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_SAPPHIRE_POOR.get(), GemQuality.POOR, GemType.SAPPHIRE)
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE_HIGH = ITEMS.register(
      "carved_sapphire_high",
      () -> new CarvedGemItem(new Properties(), GemType.SAPPHIRE, GemQuality.HIGH, () -> (Item)ModItems.CARVED_SAPPHIRE_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_SAPPHIRE_HIGH_FULL = ITEMS.register(
      "carved_sapphire_high_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_SAPPHIRE_HIGH.get(), GemQuality.HIGH, GemType.SAPPHIRE)
   );
   public static final DeferredItem<Item> CARVED_TOPAZ = ITEMS.register(
      "carved_topaz", () -> new CarvedGemItem(new Properties(), GemType.TOPAZ, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_TOPAZ_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_TOPAZ_FULL = ITEMS.register(
      "carved_topaz_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_TOPAZ.get(), GemQuality.NORMAL, GemType.TOPAZ)
   );
   public static final DeferredItem<Item> CARVED_TOPAZ_POOR = ITEMS.register(
      "carved_topaz_poor", () -> new CarvedGemItem(new Properties(), GemType.TOPAZ, GemQuality.POOR, () -> (Item)ModItems.CARVED_TOPAZ_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_TOPAZ_POOR_FULL = ITEMS.register(
      "carved_topaz_poor_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_TOPAZ_POOR.get(), GemQuality.POOR, GemType.TOPAZ)
   );
   public static final DeferredItem<Item> CARVED_TOPAZ_HIGH = ITEMS.register(
      "carved_topaz_high", () -> new CarvedGemItem(new Properties(), GemType.TOPAZ, GemQuality.HIGH, () -> (Item)ModItems.CARVED_TOPAZ_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_TOPAZ_HIGH_FULL = ITEMS.register(
      "carved_topaz_high_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_TOPAZ_HIGH.get(), GemQuality.HIGH, GemType.TOPAZ)
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE = ITEMS.register(
      "carved_white_gemstone",
      () -> new CarvedGemItem(new Properties(), GemType.WHITE_GEMSTONE, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_WHITE_GEMSTONE_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_FULL = ITEMS.register(
      "carved_white_gemstone_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_WHITE_GEMSTONE.get(), GemQuality.NORMAL, GemType.WHITE_GEMSTONE)
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_POOR = ITEMS.register(
      "carved_white_gemstone_poor",
      () -> new CarvedGemItem(new Properties(), GemType.WHITE_GEMSTONE, GemQuality.POOR, () -> (Item)ModItems.CARVED_WHITE_GEMSTONE_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_POOR_FULL = ITEMS.register(
      "carved_white_gemstone_poor_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_WHITE_GEMSTONE_POOR.get(), GemQuality.POOR, GemType.WHITE_GEMSTONE)
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_HIGH = ITEMS.register(
      "carved_white_gemstone_high",
      () -> new CarvedGemItem(new Properties(), GemType.WHITE_GEMSTONE, GemQuality.HIGH, () -> (Item)ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_HIGH_FULL = ITEMS.register(
      "carved_white_gemstone_high_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_WHITE_GEMSTONE_HIGH.get(), GemQuality.HIGH, GemType.WHITE_GEMSTONE)
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE = ITEMS.register(
      "carved_cyan_gemstone", () -> new CarvedGemItem(new Properties(), GemType.CYAN, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_CYAN_GEMSTONE_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_FULL = ITEMS.register(
      "carved_cyan_gemstone_full", () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_CYAN_GEMSTONE.get(), GemQuality.NORMAL, GemType.CYAN)
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_POOR = ITEMS.register(
      "carved_cyan_gemstone_poor",
      () -> new CarvedGemItem(new Properties(), GemType.CYAN, GemQuality.POOR, () -> (Item)ModItems.CARVED_CYAN_GEMSTONE_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_POOR_FULL = ITEMS.register(
      "carved_cyan_gemstone_poor_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_CYAN_GEMSTONE_POOR.get(), GemQuality.POOR, GemType.CYAN)
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_HIGH = ITEMS.register(
      "carved_cyan_gemstone_high",
      () -> new CarvedGemItem(new Properties(), GemType.CYAN, GemQuality.HIGH, () -> (Item)ModItems.CARVED_CYAN_GEMSTONE_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_HIGH_FULL = ITEMS.register(
      "carved_cyan_gemstone_high_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_CYAN_GEMSTONE_HIGH.get(), GemQuality.HIGH, GemType.CYAN)
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD = ITEMS.register(
      "carved_black_shard",
      () -> new CarvedGemItem(new Properties(), GemType.BLACK_SHARD, GemQuality.NORMAL, () -> (Item)ModItems.CARVED_BLACK_SHARD_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD_FULL = ITEMS.register(
      "carved_black_shard_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_BLACK_SHARD.get(), GemQuality.NORMAL, GemType.BLACK_SHARD)
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD_POOR = ITEMS.register(
      "carved_black_shard_poor",
      () -> new CarvedGemItem(new Properties(), GemType.BLACK_SHARD, GemQuality.POOR, () -> (Item)ModItems.CARVED_BLACK_SHARD_POOR_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD_POOR_FULL = ITEMS.register(
      "carved_black_shard_poor_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_BLACK_SHARD_POOR.get(), GemQuality.POOR, GemType.BLACK_SHARD)
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD_HIGH = ITEMS.register(
      "carved_black_shard_high",
      () -> new CarvedGemItem(new Properties(), GemType.BLACK_SHARD, GemQuality.HIGH, () -> (Item)ModItems.CARVED_BLACK_SHARD_HIGH_FULL.get())
   );
   public static final DeferredItem<Item> CARVED_BLACK_SHARD_HIGH_FULL = ITEMS.register(
      "carved_black_shard_high_full",
      () -> new FullManaCarvedGemItem(new Properties(), () -> (Item)CARVED_BLACK_SHARD_HIGH.get(), GemQuality.HIGH, GemType.BLACK_SHARD)
   );
   public static final DeferredItem<Item> RAW_CYAN_GEMSTONE = ITEMS.register("raw_cyan_gemstone", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_EMERALD = ITEMS.register("raw_emerald", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_RUBY = ITEMS.register("raw_ruby", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_SAPPHIRE = ITEMS.register("raw_sapphire", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_TOPAZ = ITEMS.register("raw_topaz", () -> new Item(new Properties()));
   public static final DeferredItem<Item> RAW_WHITE_GEMSTONE = ITEMS.register("raw_white_gemstone", () -> new Item(new Properties()));
   public static final DeferredItem<Item> MAGIC_SCROLL_BASIC_JEWEL = ITEMS.register(
      "magic_scroll_basic_jewel",
      () -> new MagicScrollItem(new Properties().durability(20), 0.8, true, (String)null, "jewel_magic_shoot", "jewel_random_shoot")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_BASIC_JEWEL_BROKEN = ITEMS.register(
      "magic_scroll_basic_jewel_broken",
      () -> new RandomMagicScrollItem(new Properties().durability(5), 0.5, (String)null, "jewel_magic_shoot", "jewel_random_shoot")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL = ITEMS.register(
      "magic_scroll_advanced_jewel", () -> new MagicScrollItem(new Properties().durability(20), 0.8, "jewel_magic_shoot", "jewel_magic_release")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN = ITEMS.register(
      "magic_scroll_advanced_jewel_broken", () -> new RandomMagicScrollItem(new Properties().durability(5), 0.3, "jewel_magic_shoot", "jewel_magic_release")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN = ITEMS.register(
      "magic_scroll_machine_gun",
      () -> new MagicScrollItem(new Properties().durability(1), 1.0, false, new String[]{"jewel_magic_shoot", "gander"}, "jewel_machine_gun")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN_BROKEN = ITEMS.register(
      "magic_scroll_machine_gun_broken",
      () -> new RandomMagicScrollItem(new Properties().durability(1), 0.3, new String[]{"jewel_magic_shoot", "gander"}, "jewel_machine_gun")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION = ITEMS.register(
      "magic_scroll_projection", () -> new MagicScrollItem(new Properties().durability(20), 0.5, true, (String)null, "projection", "structural_analysis")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION_BROKEN = ITEMS.register(
      "magic_scroll_projection_broken", () -> new MagicScrollItem(new Properties().durability(5), 0.2, true, (String)null, "projection", "structural_analysis")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM = ITEMS.register(
      "magic_scroll_broken_phantasm", () -> new MagicScrollItem(new Properties().durability(20), 0.5, false, "projection", "broken_phantasm")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN = ITEMS.register(
      "magic_scroll_broken_phantasm_broken", () -> new MagicScrollItem(new Properties().durability(5), 0.1, false, "projection", "broken_phantasm")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY = ITEMS.register(
      "magic_scroll_gravity", () -> new MagicScrollItem(new Properties().durability(20), 0.8, false, (String)null, "gravity_magic")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY_BROKEN = ITEMS.register(
      "magic_scroll_gravity_broken", () -> new RandomMagicScrollItem(new Properties().durability(5), 0.3, (String)null, "gravity_magic")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GANDER = ITEMS.register(
      "magic_scroll_gander", () -> new MagicScrollItem(new Properties().durability(20), 0.8, false, (String)null, "gander")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GANDER_BROKEN = ITEMS.register(
      "magic_scroll_gander_broken", () -> new RandomMagicScrollItem(new Properties().durability(5), 0.3, (String)null, "gander")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GANDR_MACHINE_GUN = ITEMS.register(
      "magic_scroll_gandr_machine_gun", () -> new MagicScrollItem(new Properties().durability(1), 1.0, false, new String[]{"gander"}, "gandr_machine_gun")
   );
   public static final DeferredItem<Item> MAGIC_SCROLL_GANDR_MACHINE_GUN_BROKEN = ITEMS.register(
      "magic_scroll_gandr_machine_gun_broken",
      () -> new RandomMagicScrollItem(new Properties().durability(1), 0.3, new String[]{"gander"}, "gandr_machine_gun")
   );
   public static final DeferredItem<Item> MAGIC_BOOK_REINFORCEMENT = ITEMS.register(
      "magic_book_reinforcement", () -> new MagicScrollItem(new Properties().durability(20), 1.0, false, (String)null, "reinforcement")
   );
   public static final DeferredItem<Item> MAGIC_PAGE_REINFORCEMENT = ITEMS.register(
      "magic_page_reinforcement", () -> new RandomMagicScrollItem(new Properties().durability(5), 0.5, (String)null, "reinforcement")
   );
   public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION = ITEMS.register(
      "mystic_eyes_of_death_perception", () -> new MysticEyesItem(new Properties().rarity(Rarity.EPIC).stacksTo(1))
   );
   public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION_NOBLE_COLOR = ITEMS.register(
      "mystic_eyes_of_death_perception_noble_color", () -> new MysticEyesItem(new Properties().rarity(Rarity.EPIC).stacksTo(1))
   );
   public static final DeferredItem<Item> AVALON = ITEMS.register("avalon", () -> new AvalonItem(new Properties().rarity(Rarity.EPIC).stacksTo(1)));
   public static final DeferredItem<Item> EXCALIBUR = ITEMS.register(
      "excalibur",
      () -> new ExcaliburItem(
         new Properties()
            .rarity(Rarity.EPIC)
            .stacksTo(1)
            .fireResistant()
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur_damage"), 12.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur_speed"), 1.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
      )
   );
   public static final DeferredItem<Item> EXCALIBUR2 = ITEMS.register(
      "excalibur2",
      () -> new ExcaliburGoldenItem(
         new Properties()
            .rarity(Rarity.EPIC)
            .stacksTo(1)
            .fireResistant()
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur2_damage"), 12.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(ResourceLocation.fromNamespaceAndPath("typemoonworld", "excalibur2_speed"), 1.0, Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
      )
   );
   public static final DeferredItem<Item> RYOUGI_SHIKI_SPAWN_EGG = ITEMS.register(
      "ryougi_shiki_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.RYOUGI_SHIKI, 10079487, 13369378, new Properties())
   );
   public static final DeferredItem<Item> MERLIN_SPAWN_EGG = ITEMS.register(
      "merlin_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.MERLIN, 16777215, 14201087, new Properties())
   );
   public static final DeferredItem<Item> STONE_MAN_SPAWN_EGG = ITEMS.register(
      "stone_man_spawn_egg", () -> new StoneManSpawnEggItem(ModEntities.STONE_MAN, 7237230, 12566463, new Properties())
   );
   public static final DeferredItem<Item> MYSTIC_MAGICIAN_SPAWN_EGG = ITEMS.register(
      "mystic_magician_spawn_egg", () -> new DeferredSpawnEggItem(ModEntities.MYSTIC_MAGICIAN, 3022370, 10562106, new Properties())
   );

   public static Item getNormalizedCarvedGem(GemType type) {
      return switch (type) {
         case RUBY -> (Item)CARVED_RUBY.get();
         case SAPPHIRE -> (Item)CARVED_SAPPHIRE.get();
         case EMERALD -> (Item)CARVED_EMERALD.get();
         case TOPAZ -> (Item)CARVED_TOPAZ.get();
         case WHITE_GEMSTONE -> (Item)CARVED_WHITE_GEMSTONE.get();
         case CYAN -> (Item)CARVED_CYAN_GEMSTONE.get();
         case BLACK_SHARD -> (Item)CARVED_BLACK_SHARD.get();
      };
   }

   public static Item getNormalizedFullCarvedGem(GemType type) {
      return switch (type) {
         case RUBY -> (Item)CARVED_RUBY_FULL.get();
         case SAPPHIRE -> (Item)CARVED_SAPPHIRE_FULL.get();
         case EMERALD -> (Item)CARVED_EMERALD_FULL.get();
         case TOPAZ -> (Item)CARVED_TOPAZ_FULL.get();
         case WHITE_GEMSTONE -> (Item)CARVED_WHITE_GEMSTONE_FULL.get();
         case CYAN -> (Item)CARVED_CYAN_GEMSTONE_FULL.get();
         case BLACK_SHARD -> (Item)CARVED_BLACK_SHARD_FULL.get();
      };
   }

   public static void register(IEventBus eventBus) {
      ITEMS.register(eventBus);
   }
}
