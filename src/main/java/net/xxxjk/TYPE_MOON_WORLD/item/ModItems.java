package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RedswordItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

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

    public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holy_shroud",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(100)));

    public static final double GEM_MANA_AMOUNT = 100.0;

    public static final DeferredItem<Item> CARVED_EMERALD = ITEMS.register("carved_emerald",
            () -> new CarvedGemItem(new Item.Properties(), GemType.EMERALD, GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_RUBY = ITEMS.register("carved_ruby",
            () -> new CarvedGemItem(new Item.Properties(), GemType.RUBY, GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_SAPPHIRE = ITEMS.register("carved_sapphire",
            () -> new CarvedGemItem(new Item.Properties(), GemType.SAPPHIRE, GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_TOPAZ = ITEMS.register("carved_topaz",
            () -> new CarvedGemItem(new Item.Properties(), GemType.TOPAZ, GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE = ITEMS.register("carved_white_gemstone",
            () -> new CarvedGemItem(new Item.Properties(), GemType.WHITE_GEMSTONE, GEM_MANA_AMOUNT));

    public static final DeferredItem<Item> CARVED_EMERALD_FULL = ITEMS.register("carved_emerald_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> CARVED_EMERALD.get(), GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_RUBY_FULL = ITEMS.register("carved_ruby_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> CARVED_RUBY.get(), GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_SAPPHIRE_FULL = ITEMS.register("carved_sapphire_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> CARVED_SAPPHIRE.get(), GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_TOPAZ_FULL = ITEMS.register("carved_topaz_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> CARVED_TOPAZ.get(), GEM_MANA_AMOUNT));
    public static final DeferredItem<Item> CARVED_WHITE_GEMSTONE_FULL = ITEMS.register("carved_white_gemstone_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> CARVED_WHITE_GEMSTONE.get(), GEM_MANA_AMOUNT));

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
            () -> new MagicScrollItem(new Item.Properties(), 0.5,
                    "ruby_throw", "sapphire_throw", "emerald_use", "topaz_throw"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL = ITEMS.register("magic_scroll_advanced_jewel",
            () -> new MagicScrollItem(new Item.Properties(), 0.25,
                    "ruby_flame_sword", "sapphire_winter_frost", "emerald_winter_river", "topaz_reinforcement"));

    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION = ITEMS.register("magic_scroll_projection",
            () -> new MagicScrollItem(new Item.Properties(), 0.5,
                    "projection", "structural_analysis"));

    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM = ITEMS.register("magic_scroll_broken_phantasm",
            () -> new MagicScrollItem(new Item.Properties(), 0.5,
                    "broken_phantasm"));

    public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION = ITEMS.register("mystic_eyes_of_death_perception",
            () -> new MysticEyesItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> MYSTIC_EYES_OF_DEATH_PERCEPTION_NOBLE_COLOR = ITEMS.register("mystic_eyes_of_death_perception_noble_color",
            () -> new MysticEyesItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> AVALON = ITEMS.register("avalon",
            () -> new AvalonItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<Item> RYOUGI_SHIKI_SPAWN_EGG = ITEMS.register("ryougi_shiki_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.RYOUGI_SHIKI, 0x99CCFF, 0xCC0022, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
