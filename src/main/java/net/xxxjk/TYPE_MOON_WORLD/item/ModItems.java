package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RedswordItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredItem<Item> TEMPLE_STONE_SWORD_AXE = ITEMS.register("temple_stone_sword_axe",
            () -> new TempleStoneSwordAxeItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "temple_stone_sword_axe_damage"),
                                            20.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "temple_stone_sword_axe_speed"),
                                            -3.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> REDSWORD = ITEMS.register("redsword",
            () -> new RedswordItem(new Item.Properties().durability(2000).fireResistant().rarity(Rarity.RARE)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "redsword_damage"), 
                                            9, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), 
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, 
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "redsword_speed"), 
                                            0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), 
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> TSUMUKARI_MURAMASA = ITEMS.register("tsumukari_muramasa",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.TsumukariMuramasaItem(new Item.Properties().durability(5000).fireResistant().rarity(Rarity.EPIC)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "tsumukari_damage"), 
                                            15, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), 
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, 
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "tsumukari_speed"), 
                                            0.3, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE), 
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> MAGIC_FRAGMENTS = ITEMS.register("magic_fragments",
            () -> new Magic_fragmentsItem(new Item.Properties()));

    public static final DeferredItem<Item> RANDOM_GEM = ITEMS.register("random_gem",
            () -> new RandomGemItem(new Item.Properties()));

    public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holy_shroud",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(100)));

    // EMERALD (Green)
    public static final DeferredItem<Item> CARVED_EMERALD = ITEMS.register("carved_emerald",
            () -> new CarvedGemItem(new Item.Properties(), GemType.EMERALD, GemQuality.NORMAL, () -> ModItems.CARVED_EMERALD_FULL.get()));
    public static final DeferredItem<Item> CARVED_EMERALD_FULL = ITEMS.register("carved_emerald_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_EMERALD.get(), GemQuality.NORMAL, GemType.EMERALD));
    public static final DeferredItem<Item> CARVED_EMERALD_POOR = ITEMS.register("carved_emerald_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.EMERALD, GemQuality.POOR, () -> ModItems.CARVED_EMERALD_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_EMERALD_POOR_FULL = ITEMS.register("carved_emerald_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_EMERALD_POOR.get(), GemQuality.POOR, GemType.EMERALD));
    public static final DeferredItem<Item> CARVED_EMERALD_HIGH = ITEMS.register("carved_emerald_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.EMERALD, GemQuality.HIGH, () -> ModItems.CARVED_EMERALD_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_EMERALD_HIGH_FULL = ITEMS.register("carved_emerald_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_EMERALD_HIGH.get(), GemQuality.HIGH, GemType.EMERALD));

    // RUBY (Red)
    public static final DeferredItem<Item> CARVED_RUBY = ITEMS.register("carved_ruby",
            () -> new CarvedGemItem(new Item.Properties(), GemType.RUBY, GemQuality.NORMAL, () -> ModItems.CARVED_RUBY_FULL.get()));
    public static final DeferredItem<Item> CARVED_RUBY_FULL = ITEMS.register("carved_ruby_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_RUBY.get(), GemQuality.NORMAL, GemType.RUBY));
    public static final DeferredItem<Item> CARVED_RUBY_POOR = ITEMS.register("carved_ruby_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.RUBY, GemQuality.POOR, () -> ModItems.CARVED_RUBY_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_RUBY_POOR_FULL = ITEMS.register("carved_ruby_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_RUBY_POOR.get(), GemQuality.POOR, GemType.RUBY));
    public static final DeferredItem<Item> CARVED_RUBY_HIGH = ITEMS.register("carved_ruby_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.RUBY, GemQuality.HIGH, () -> ModItems.CARVED_RUBY_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_RUBY_HIGH_FULL = ITEMS.register("carved_ruby_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_RUBY_HIGH.get(), GemQuality.HIGH, GemType.RUBY));

    // SAPPHIRE (Blue)
    public static final DeferredItem<Item> CARVED_SAPPHIRE = ITEMS.register("carved_sapphire",
            () -> new CarvedGemItem(new Item.Properties(), GemType.SAPPHIRE, GemQuality.NORMAL, () -> ModItems.CARVED_SAPPHIRE_FULL.get()));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_FULL = ITEMS.register("carved_sapphire_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_SAPPHIRE.get(), GemQuality.NORMAL, GemType.SAPPHIRE));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_POOR = ITEMS.register("carved_sapphire_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.SAPPHIRE, GemQuality.POOR, () -> ModItems.CARVED_SAPPHIRE_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_POOR_FULL = ITEMS.register("carved_sapphire_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_SAPPHIRE_POOR.get(), GemQuality.POOR, GemType.SAPPHIRE));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_HIGH = ITEMS.register("carved_sapphire_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.SAPPHIRE, GemQuality.HIGH, () -> ModItems.CARVED_SAPPHIRE_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_HIGH_FULL = ITEMS.register("carved_sapphire_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_SAPPHIRE_HIGH.get(), GemQuality.HIGH, GemType.SAPPHIRE));

    // TOPAZ (Yellow)
    public static final DeferredItem<Item> CARVED_TOPAZ = ITEMS.register("carved_topaz",
            () -> new CarvedGemItem(new Item.Properties(), GemType.TOPAZ, GemQuality.NORMAL, () -> ModItems.CARVED_TOPAZ_FULL.get()));
    public static final DeferredItem<Item> CARVED_TOPAZ_FULL = ITEMS.register("carved_topaz_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_TOPAZ.get(), GemQuality.NORMAL, GemType.TOPAZ));
    public static final DeferredItem<Item> CARVED_TOPAZ_POOR = ITEMS.register("carved_topaz_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.TOPAZ, GemQuality.POOR, () -> ModItems.CARVED_TOPAZ_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_TOPAZ_POOR_FULL = ITEMS.register("carved_topaz_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_TOPAZ_POOR.get(), GemQuality.POOR, GemType.TOPAZ));
    public static final DeferredItem<Item> CARVED_TOPAZ_HIGH = ITEMS.register("carved_topaz_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.TOPAZ, GemQuality.HIGH, () -> ModItems.CARVED_TOPAZ_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_TOPAZ_HIGH_FULL = ITEMS.register("carved_topaz_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_TOPAZ_HIGH.get(), GemQuality.HIGH, GemType.TOPAZ));

    // WHITE GEMSTONE (White)
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE = ITEMS.register("carved_white_gemstone",
            () -> new CarvedGemItem(new Item.Properties(), GemType.WHITE_GEMSTONE, GemQuality.NORMAL, () -> ModItems.CARVED_WHITE_GEMSTONE_FULL.get()));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_FULL = ITEMS.register("carved_white_gemstone_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_WHITE_GEMSTONE.get(), GemQuality.NORMAL, GemType.WHITE_GEMSTONE));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_POOR = ITEMS.register("carved_white_gemstone_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.WHITE_GEMSTONE, GemQuality.POOR, () -> ModItems.CARVED_WHITE_GEMSTONE_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_POOR_FULL = ITEMS.register("carved_white_gemstone_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_WHITE_GEMSTONE_POOR.get(), GemQuality.POOR, GemType.WHITE_GEMSTONE));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_HIGH = ITEMS.register("carved_white_gemstone_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.WHITE_GEMSTONE, GemQuality.HIGH, () -> ModItems.CARVED_WHITE_GEMSTONE_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_HIGH_FULL = ITEMS.register("carved_white_gemstone_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_WHITE_GEMSTONE_HIGH.get(), GemQuality.HIGH, GemType.WHITE_GEMSTONE));

    // CYAN GEMSTONE (Cyan)
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE = ITEMS.register("carved_cyan_gemstone",
            () -> new CarvedGemItem(new Item.Properties(), GemType.CYAN, GemQuality.NORMAL, () -> ModItems.CARVED_CYAN_GEMSTONE_FULL.get()));
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_FULL = ITEMS.register("carved_cyan_gemstone_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_CYAN_GEMSTONE.get(), GemQuality.NORMAL, GemType.CYAN));
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_POOR = ITEMS.register("carved_cyan_gemstone_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.CYAN, GemQuality.POOR, () -> ModItems.CARVED_CYAN_GEMSTONE_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_POOR_FULL = ITEMS.register("carved_cyan_gemstone_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_CYAN_GEMSTONE_POOR.get(), GemQuality.POOR, GemType.CYAN));
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_HIGH = ITEMS.register("carved_cyan_gemstone_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.CYAN, GemQuality.HIGH, () -> ModItems.CARVED_CYAN_GEMSTONE_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_CYAN_GEMSTONE_HIGH_FULL = ITEMS.register("carved_cyan_gemstone_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_CYAN_GEMSTONE_HIGH.get(), GemQuality.HIGH, GemType.CYAN));

    public static final DeferredItem<Item> RAW_CYAN_GEMSTONE = ITEMS.register("raw_cyan_gemstone",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_EMERALD = ITEMS.register("raw_emerald",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_RUBY = ITEMS.register("raw_ruby",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_SAPPHIRE = ITEMS.register("raw_sapphire",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_TOPAZ = ITEMS.register("raw_topaz",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_WHITE_GEMSTONE = ITEMS.register("raw_white_gemstone",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAGIC_SCROLL_BASIC_JEWEL = ITEMS.register("magic_scroll_basic_jewel",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, // Easy to learn (80%), High Durability (20)
                    "jewel_magic_shoot"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_BASIC_JEWEL_BROKEN = ITEMS.register("magic_scroll_basic_jewel_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.2, // Hard to learn (20%), Low Durability (5)
                    "jewel_magic_shoot"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL = ITEMS.register("magic_scroll_advanced_jewel",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, // Easy to learn (80%), High Durability (20)
                    "jewel_magic_release"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN = ITEMS.register("magic_scroll_advanced_jewel_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.2, // Hard to learn (20%), Low Durability (5)
                    "jewel_magic_release"));

    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION = ITEMS.register("magic_scroll_projection",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, // Easy to learn (80%), High Durability (20)
                    "projection", "structural_analysis"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION_BROKEN = ITEMS.register("magic_scroll_projection_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.2, // Hard to learn (20%), Low Durability (5)
                    "projection", "structural_analysis"));

    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM = ITEMS.register("magic_scroll_broken_phantasm",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, // Easy to learn (80%), High Durability (20)
                    "broken_phantasm"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN = ITEMS.register("magic_scroll_broken_phantasm_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.2, // Hard to learn (20%), Low Durability (5)
                    "broken_phantasm"));

    public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION = ITEMS.register("mystic_eyes_of_death_perception",
            () -> new MysticEyesItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION_NOBLE_COLOR = ITEMS.register("mystic_eyes_of_death_perception_noble_color",
            () -> new MysticEyesItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> AVALON = ITEMS.register("avalon",
            () -> new AvalonItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> EXCALIBUR = ITEMS.register("excalibur",
            () -> new ExcaliburItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur_speed"),
                                            1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> EXCALIBUR2 = ITEMS.register("excalibur2",
            () -> new ExcaliburGoldenItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur2_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur2_speed"),
                                            1, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> RYOUGI_SHIKI_SPAWN_EGG = ITEMS.register("ryougi_shiki_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.RYOUGI_SHIKI, 0x99CCFF, 0xCC0022, new Item.Properties()));

    public static final DeferredItem<Item> MERLIN_SPAWN_EGG = ITEMS.register("merlin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MERLIN, 0xFFFFFF, 0xD8B0FF, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
