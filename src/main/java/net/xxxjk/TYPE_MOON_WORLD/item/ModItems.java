package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CommandSpellItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DirkSmallKnifeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomStartAttributesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomMagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GaeBulgItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HecatesStaffItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HeshikiriHasebeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LeylineSurveyMapItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ManaSurveyCompassItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MercurySwordItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessChainDaggerItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RuleBreakerItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardReleaseItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantMasterContractItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

    private static DeferredItem<Item> registerSimpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

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

    public static final DeferredItem<Item> MURAMASA = ITEMS.register("redsword",
            () -> new MuramasaItem(new Item.Properties().durability(2000).fireResistant().rarity(Rarity.RARE)
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

    public static final DeferredItem<Item> HESHIKIRI_HASEBE = ITEMS.register("heshikiri_hasebe",
            () -> new HeshikiriHasebeItem(new Item.Properties().durability(1800).fireResistant().rarity(Rarity.RARE)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "heshikiri_hasebe_damage"),
                                            10.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "heshikiri_hasebe_speed"),
                                            1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> MAGIC_FRAGMENTS = ITEMS.register("magic_fragments",
            () -> new Magic_fragmentsItem(new Item.Properties()));

    public static final DeferredItem<Item> RANDOM_GEM = ITEMS.register("random_gem",
            () -> new RandomGemItem(new Item.Properties()));
    public static final DeferredItem<Item> RANDOM_START_ATTRIBUTES = ITEMS.register("random_start_attributes",
            () -> new RandomStartAttributesItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CLAW_OF_CHAOS = registerSimpleItem("claw_of_chaos");
    public static final DeferredItem<Item> DRAGON_FANG = registerSimpleItem("dragon_fang");
    public static final DeferredItem<Item> DRAGONS_REVERSE_SCALE = registerSimpleItem("dragons_reverse_scale");
    public static final DeferredItem<Item> EVIL_BONE = registerSimpleItem("evil_bone");
    public static final DeferredItem<Item> HOLY_GRAIL = ITEMS.register("holy_grail",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> PHOENIX_FEATHER = registerSimpleItem("phoenix_feather");
    public static final DeferredItem<Item> PROOF_OF_HERO = registerSimpleItem("proof_of_hero");
    public static final DeferredItem<Item> QP = registerSimpleItem("qp");
    public static final DeferredItem<Item> REMNANTS_OF_MADNESS = registerSimpleItem("remnants_of_madness");
    public static final DeferredItem<Item> SEED_OF_YGGDRASIL = registerSimpleItem("seed_of_yggdrasil");
    public static final DeferredItem<Item> VOIDS_DUST = registerSimpleItem("voids_dust");

    public static final DeferredItem<Item> HOLY_SHROUD = ITEMS.register("holy_shroud",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BULLET = ITEMS.register("bullet",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ORIGIN_BULLET = ITEMS.register("origin_bullet",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> KIRITSUGU_BONE_POWDER = ITEMS.register("kiritsugu_bone_powder",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().durability(100)));

    public static final DeferredItem<Item> MANA_SURVEY_COMPASS = ITEMS.register("mana_survey_compass",
            () -> new ManaSurveyCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 80, 90));

    public static final DeferredItem<Item> COPPER_MANA_SURVEY_COMPASS = ITEMS.register("copper_mana_survey_compass",
            () -> new ManaSurveyCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), 50, 80));

    public static final DeferredItem<Item> LEYLINE_SURVEY_MAP = ITEMS.register("leyline_survey_map",
            () -> new LeylineSurveyMapItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> MANA_SURVEY_BASE = ITEMS.register("mana_survey_base",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MANA_SURVEY_POINTER = ITEMS.register("mana_survey_pointer",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COPPER_MANA_SURVEY_BASE = ITEMS.register("copper_mana_survey_base",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COPPER_MANA_SURVEY_POINTER = ITEMS.register("copper_mana_survey_pointer",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> MAGIC_CREST = ITEMS.register("magic_crest",
            () -> new MagicCrestItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> GANDER = ITEMS.register("gander",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<Item> SERVANT_MASTER_CONTRACT = ITEMS.register("servant_master_contract",
            () -> new ServantMasterContractItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> COMMAND_SPELL = ITEMS.register("command_spell",
            () -> new CommandSpellItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SERVANT_CARD_RELEASE = ITEMS.register("servant_card_release",
            () -> new ServantCardReleaseItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER = registerServantCard("emiya_archer");
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON = registerServantCard("artoria_pendragon");
    public static final DeferredItem<Item> SERVANT_CARD_SASAKI_KOJIRO = registerServantCard("sasaki_kojiro");
    public static final DeferredItem<Item> SERVANT_CARD_CU_CHULAINN = registerServantCard("cu_chulainn");
    public static final DeferredItem<Item> SERVANT_CARD_MEDEA = registerServantCard("medea");
    public static final DeferredItem<Item> SERVANT_CARD_MEDUSA = registerServantCard("medusa");
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN = registerServantCard("cursed_arm_hassan");
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES = registerServantCard("heracles");
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA = registerServantCard("oda_nobunaga");
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU = registerServantCard("enkidu");
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN = registerServantCard("gawain");
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS = registerServantCard("paracelsus");
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN = registerServantCard("li_shuwen");

    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER_CHEST = registerServantArmor("emiya_archer", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER_LEGS = registerServantArmor("emiya_archer", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON_CHEST = registerServantArmor("artoria_pendragon", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON_LEGS = registerServantArmor("artoria_pendragon", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_SASAKI_KOJIRO_CHEST = registerServantArmor("sasaki_kojiro", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_SASAKI_KOJIRO_LEGS = registerServantArmor("sasaki_kojiro", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_CU_CHULAINN_CHEST = registerServantArmor("cu_chulainn", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_CU_CHULAINN_LEGS = registerServantArmor("cu_chulainn", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_MEDEA_HEAD = registerServantArmor("medea", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_MEDEA_CHEST = registerServantArmor("medea", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_MEDEA_LEGS = registerServantArmor("medea", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_MEDUSA_HEAD = registerServantArmor("medusa", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_MEDUSA_CHEST = registerServantArmor("medusa", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_MEDUSA_LEGS = registerServantArmor("medusa", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN_HEAD = registerServantArmor("cursed_arm_hassan", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN_CHEST = registerServantArmor("cursed_arm_hassan", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN_LEGS = registerServantArmor("cursed_arm_hassan", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES_CHEST = registerServantArmor("heracles", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES_LEGS = registerServantArmor("heracles", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA_CHEST = registerServantArmor("oda_nobunaga", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA_LEGS = registerServantArmor("oda_nobunaga", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU_CHEST = registerServantArmor("enkidu", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU_LEGS = registerServantArmor("enkidu", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN_CHEST = registerServantArmor("gawain", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN_LEGS = registerServantArmor("gawain", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS_CHEST = registerServantArmor("paracelsus", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS_LEGS = registerServantArmor("paracelsus", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN_CHEST = registerServantArmor("li_shuwen", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN_LEGS = registerServantArmor("li_shuwen", net.minecraft.world.entity.EquipmentSlot.LEGS);

    private static DeferredItem<Item> registerServantCard(String servantId) {
        return ITEMS.register("servant_card_" + servantId,
                () -> new ServantCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), servantId));
    }

    private static DeferredItem<Item> registerServantArmor(String servantId, net.minecraft.world.entity.EquipmentSlot slot) {
        return ITEMS.register("servant_card_" + servantId + "_" + slot.getName(),
                () -> new ServantCardArmorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), servantId, slot));
    }

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

    // BLACK SHARD (Obsidian)
    public static final DeferredItem<Item> CARVED_BLACK_SHARD = ITEMS.register("carved_black_shard",
            () -> new CarvedGemItem(new Item.Properties(), GemType.BLACK_SHARD, GemQuality.NORMAL, () -> ModItems.CARVED_BLACK_SHARD_FULL.get()));
    public static final DeferredItem<Item> CARVED_BLACK_SHARD_FULL = ITEMS.register("carved_black_shard_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_BLACK_SHARD.get(), GemQuality.NORMAL, GemType.BLACK_SHARD));
    public static final DeferredItem<Item> CARVED_BLACK_SHARD_POOR = ITEMS.register("carved_black_shard_poor",
            () -> new CarvedGemItem(new Item.Properties(), GemType.BLACK_SHARD, GemQuality.POOR, () -> ModItems.CARVED_BLACK_SHARD_POOR_FULL.get()));
    public static final DeferredItem<Item> CARVED_BLACK_SHARD_POOR_FULL = ITEMS.register("carved_black_shard_poor_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_BLACK_SHARD_POOR.get(), GemQuality.POOR, GemType.BLACK_SHARD));
    public static final DeferredItem<Item> CARVED_BLACK_SHARD_HIGH = ITEMS.register("carved_black_shard_high",
            () -> new CarvedGemItem(new Item.Properties(), GemType.BLACK_SHARD, GemQuality.HIGH, () -> ModItems.CARVED_BLACK_SHARD_HIGH_FULL.get()));
    public static final DeferredItem<Item> CARVED_BLACK_SHARD_HIGH_FULL = ITEMS.register("carved_black_shard_high_full",
            () -> new FullManaCarvedGemItem(new Item.Properties(), () -> ModItems.CARVED_BLACK_SHARD_HIGH.get(), GemQuality.HIGH, GemType.BLACK_SHARD));

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
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, true, (String)null, 
                    "jewel_magic_shoot", "jewel_random_shoot"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_BASIC_JEWEL_BROKEN = ITEMS.register("magic_scroll_basic_jewel_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().durability(5), 0.5, (String)null, 
                    "jewel_magic_shoot", "jewel_random_shoot"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL = ITEMS.register("magic_scroll_advanced_jewel",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_magic_release"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN = ITEMS.register("magic_scroll_advanced_jewel_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().durability(5), 0.3, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_magic_release"));

    public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN = ITEMS.register("magic_scroll_machine_gun",
            () -> new MagicScrollItem(new Item.Properties().durability(1), 1.0, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_machine_gun"));

    public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN_BROKEN = ITEMS.register("magic_scroll_machine_gun_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().durability(1), 0.3, "jewel_magic_shoot", // Require Basic Jewel Magic, Low Chance
                    "jewel_machine_gun"));

    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION = ITEMS.register("magic_scroll_projection",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.5, true, (String)null, 
                    "projection", "structural_analysis"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION_BROKEN = ITEMS.register("magic_scroll_projection_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.2, true, (String)null, 
                    "projection", "structural_analysis"));

    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM = ITEMS.register("magic_scroll_broken_phantasm",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.5, false, "projection", // Require Projection
                    "broken_phantasm"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN = ITEMS.register("magic_scroll_broken_phantasm_broken",
            () -> new MagicScrollItem(new Item.Properties().durability(5), 0.1, false, "projection", // Require Projection
                    "broken_phantasm"));
    
    public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY = ITEMS.register("magic_scroll_gravity",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, false, (String)null, 
                    "gravity_magic"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY_BROKEN = ITEMS.register("magic_scroll_gravity_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().durability(5), 0.3, (String)null, 
                    "gravity_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_REINFORCEMENT = ITEMS.register("magic_book_reinforcement",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, // learnAll=false (default), no req
                    (String)null, "reinforcement"));

    public static final DeferredItem<Item> MAGIC_PAGE_REINFORCEMENT = ITEMS.register("magic_page_reinforcement",
            () -> new RandomMagicScrollItem(new Item.Properties().durability(5), 0.5, // no req
                    (String)null, "reinforcement", "reinforcement_self", "reinforcement_other", "reinforcement_item"));

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

    public static final DeferredItem<Item> BIZEN_NAGAMITSU = ITEMS.register("bizen_nagamitsu",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.BizenNagamitsuItem(new Item.Properties().rarity(Rarity.RARE).fireResistant().durability(1000)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "bizen_nagamitsu_damage"),
                                            10.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "bizen_nagamitsu_speed"),
                                            1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "bizen_nagamitsu_range"),
                                            1.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> GAE_BULG = ITEMS.register("gae_bulg",
            () -> new GaeBulgItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gae_bulg_damage"),
                                            10.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gae_bulg_speed"),
                                            1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gae_bulg_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> RULE_BREAKER = ITEMS.register("rule_breaker",
            () -> new RuleBreakerItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "rule_breaker_damage"),
                                            6.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "rule_breaker_speed"),
                                            1.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> HECATES_STAFF = ITEMS.register("hecates_staff",
            () -> new HecatesStaffItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hecates_staff_damage"),
                                            7.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hecates_staff_speed"),
                                            0.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> NAMELESS_CHAIN_DAGGER = ITEMS.register("nameless_chain_dagger",
            () -> new NamelessChainDaggerItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "nameless_chain_dagger_damage"),
                                            6.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "nameless_chain_dagger_speed"),
                                            1.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> DIRK_SMALL_KNIFE = ITEMS.register("dirk_small_knife",
            () -> new DirkSmallKnifeItem(new Item.Properties().rarity(Rarity.UNCOMMON).durability(100)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "dirk_small_knife_damage"),
                                            5.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "dirk_small_knife_speed"),
                                            1.8, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> GAN_JIANG = ITEMS.register("gan_jiang",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gan_jiang_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gan_jiang_speed"),
                                            1.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "gan_jiang"));
    public static final DeferredItem<Item> MO_YE = ITEMS.register("mo_ye",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mo_ye_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mo_ye_speed"),
                                            1.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "mo_ye"));
    public static final DeferredItem<Item> GAN_JIANG_OVEREDGE = ITEMS.register("gan_jiang_overedge",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gan_jiang_overedge_damage"),
                                            15.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gan_jiang_overedge_speed"),
                                            1.3, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "gan_jiang_overedge"));
    public static final DeferredItem<Item> MO_YE_OVEREDGE = ITEMS.register("mo_ye_overedge",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mo_ye_overedge_damage"),
                                            15.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mo_ye_overedge_speed"),
                                            1.3, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "mo_ye_overedge"));
    public static final DeferredItem<Item> NAMELESS_BOW = ITEMS.register("nameless_bow",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessBowItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> PSEUDO_SPIRAL_SWORD = ITEMS.register("pseudo_spiral_sword",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "pseudo_spiral_sword"));
    public static final DeferredItem<Item> CRIMSON_HOUND = ITEMS.register("crimson_hound",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "crimson_hound"));
    public static final DeferredItem<Item> PARACELSUS_SWORD = ITEMS.register("paracelsus_sword",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "paracelsus_sword_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "paracelsus_sword_speed"),
                                            1.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "paracelsus_sword"));
    public static final DeferredItem<Item> MERCURY_SWORD = ITEMS.register("mercury_sword",
            () -> new MercurySwordItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mercury_sword_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "mercury_sword_speed"),
                                            1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> THOMPSON_CONTENDER = ITEMS.register("thompson_contender",
            () -> new ThompsonContenderItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> EXCALIBUR_GALLATIN = ITEMS.register("excalibur_gallatin",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur_gallatin_damage"),
                                            16.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "excalibur_gallatin_speed"),
                                            0.15, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "excalibur_gallatin"));
    public static final DeferredItem<Item> RHO_AIAS = ITEMS.register("rho_aias",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "rho_aias"));
    public static final DeferredItem<Item> UBW_METAL_1 = ITEMS.register("ubw_metal_1",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1).fireResistant(), "ubw_metal_1"));
    public static final DeferredItem<Item> UBW_METAL_2 = ITEMS.register("ubw_metal_2",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1).fireResistant(), "ubw_metal_2"));
    public static final DeferredItem<Item> UBW_METAL_3 = ITEMS.register("ubw_metal_3",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1).fireResistant(), "ubw_metal_3"));

    public static final DeferredItem<Item> RYOUGI_SHIKI_SPAWN_EGG = ITEMS.register("ryougi_shiki_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.RYOUGI_SHIKI, 0x99CCFF, 0xCC0022, new Item.Properties()));

    public static final DeferredItem<Item> MERLIN_SPAWN_EGG = ITEMS.register("merlin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MERLIN, 0xFFFFFF, 0xD8B0FF, new Item.Properties()));

    public static final DeferredItem<Item> STONE_MAN_SPAWN_EGG = ITEMS.register("stone_man_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.STONE_MAN, 0x8A8A8A, 0x4C4C4C, new Item.Properties()));

    public static final DeferredItem<Item> MYSTIC_MAGICIAN_SPAWN_EGG = ITEMS.register("mystic_magician_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN, 0x000000, 0xC00000, new Item.Properties()));

    public static final DeferredItem<Item> HERACLES_SPAWN_EGG = ITEMS.register("heracles_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.HERACLES, 0x333333, 0x0D0D0D));

    public static final DeferredItem<Item> SASAKI_KOJIRO_SPAWN_EGG = ITEMS.register("sasaki_kojiro_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.SASAKI_KOJIRO, 0x3355AA, 0x6633AA));

    public static final DeferredItem<Item> CU_CHULAINN_SPAWN_EGG = ITEMS.register("cu_chulainn_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.CU_CHULAINN, 0x2B60C7, 0xC8D1DD));
    public static final DeferredItem<Item> MEDEA_SPAWN_EGG = ITEMS.register("medea_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MEDEA, 0x705F96, 0xDDD7E9));
    public static final DeferredItem<Item> MEDUSA_SPAWN_EGG = ITEMS.register("medusa_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MEDUSA, 0x2B2445, 0xC7BDE2));
    public static final DeferredItem<Item> CURSED_ARM_HASSAN_SPAWN_EGG = ITEMS.register("cursed_arm_hassan_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.CURSED_ARM_HASSAN, 0x1B1A1D, 0x6E1F2C));
    public static final DeferredItem<Item> DRAGONFANG_SOLDIER_SPAWN_EGG = ITEMS.register("dragonfang_soldier_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.DRAGONFANG_SOLDIER, 0xE8E0D2, 0x635A52));
    public static final DeferredItem<Item> EMIYA_ARCHER_SPAWN_EGG = ITEMS.register("emiya_archer_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.EMIYA_ARCHER, 0x8E2424, 0xD8D2C2));
    public static final DeferredItem<Item> ARTORIA_PENDRAGON_SPAWN_EGG = ITEMS.register("artoria_pendragon_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ARTORIA_PENDRAGON, 0x1E4FAF, 0xF4D35E));
    public static final DeferredItem<Item> ODA_NOBUNAGA_SPAWN_EGG = ITEMS.register("oda_nobunaga_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ODA_NOBUNAGA, 0xB01818, 0xF4C430));
    public static final DeferredItem<Item> ENKIDU_SPAWN_EGG = ITEMS.register("enkidu_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ENKIDU, 0xF4FFF7, 0x7FE7B2));
    public static final DeferredItem<Item> GAWAIN_SPAWN_EGG = ITEMS.register("gawain_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.GAWAIN, 0xC0C0C0, 0xFFD700));
    public static final DeferredItem<Item> LI_SHUWEN_SPAWN_EGG = ITEMS.register("li_shuwen_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.LI_SHUWEN, 0x1A1A1A, 0xD8D0C8));
    public static final DeferredItem<Item> PARACELSUS_SPAWN_EGG = ITEMS.register("paracelsus_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.PARACELSUS, 0xA9E7FF, 0xFFD36A));

    public static Item getNormalizedCarvedGem(GemType type) {
        return switch (type) {
            case RUBY -> CARVED_RUBY.get();
            case SAPPHIRE -> CARVED_SAPPHIRE.get();
            case EMERALD -> CARVED_EMERALD.get();
            case TOPAZ -> CARVED_TOPAZ.get();
            case WHITE_GEMSTONE -> CARVED_WHITE_GEMSTONE.get();
            case CYAN -> CARVED_CYAN_GEMSTONE.get();
            case BLACK_SHARD -> CARVED_BLACK_SHARD.get();
        };
    }

    public static Item getNormalizedFullCarvedGem(GemType type) {
        return switch (type) {
            case RUBY -> CARVED_RUBY_FULL.get();
            case SAPPHIRE -> CARVED_SAPPHIRE_FULL.get();
            case EMERALD -> CARVED_EMERALD_FULL.get();
            case TOPAZ -> CARVED_TOPAZ_FULL.get();
            case WHITE_GEMSTONE -> CARVED_WHITE_GEMSTONE_FULL.get();
            case CYAN -> CARVED_CYAN_GEMSTONE_FULL.get();
            case BLACK_SHARD -> CARVED_BLACK_SHARD_FULL.get();
        };
    }

    public static Item getServantCardArmor(String servantId, net.minecraft.world.entity.EquipmentSlot slot) {
        boolean head = slot == net.minecraft.world.entity.EquipmentSlot.HEAD;
        boolean legs = slot == net.minecraft.world.entity.EquipmentSlot.LEGS;
        return switch (servantId == null ? "" : servantId) {
            case "emiya_archer" -> (legs ? SERVANT_CARD_EMIYA_ARCHER_LEGS : SERVANT_CARD_EMIYA_ARCHER_CHEST).get();
            case "artoria_pendragon" -> (legs ? SERVANT_CARD_ARTORIA_PENDRAGON_LEGS : SERVANT_CARD_ARTORIA_PENDRAGON_CHEST).get();
            case "sasaki_kojiro" -> (legs ? SERVANT_CARD_SASAKI_KOJIRO_LEGS : SERVANT_CARD_SASAKI_KOJIRO_CHEST).get();
            case "cu_chulainn" -> (legs ? SERVANT_CARD_CU_CHULAINN_LEGS : SERVANT_CARD_CU_CHULAINN_CHEST).get();
            case "medea" -> (head ? SERVANT_CARD_MEDEA_HEAD : legs ? SERVANT_CARD_MEDEA_LEGS : SERVANT_CARD_MEDEA_CHEST).get();
            case "medusa" -> (head ? SERVANT_CARD_MEDUSA_HEAD : legs ? SERVANT_CARD_MEDUSA_LEGS : SERVANT_CARD_MEDUSA_CHEST).get();
            case "cursed_arm_hassan" -> (head ? SERVANT_CARD_CURSED_ARM_HASSAN_HEAD : SERVANT_CARD_CURSED_ARM_HASSAN_CHEST).get();
            case "heracles" -> (legs ? SERVANT_CARD_HERACLES_LEGS : SERVANT_CARD_HERACLES_CHEST).get();
            case "oda_nobunaga" -> (legs ? SERVANT_CARD_ODA_NOBUNAGA_LEGS : SERVANT_CARD_ODA_NOBUNAGA_CHEST).get();
            case "enkidu" -> (legs ? SERVANT_CARD_ENKIDU_LEGS : SERVANT_CARD_ENKIDU_CHEST).get();
            case "gawain" -> (legs ? SERVANT_CARD_GAWAIN_LEGS : SERVANT_CARD_GAWAIN_CHEST).get();
            case "paracelsus" -> (legs ? SERVANT_CARD_PARACELSUS_LEGS : SERVANT_CARD_PARACELSUS_CHEST).get();
            case "li_shuwen" -> (legs ? SERVANT_CARD_LI_SHUWEN_LEGS : SERVANT_CARD_LI_SHUWEN_CHEST).get();
            default -> (legs ? SERVANT_CARD_EMIYA_ARCHER_LEGS : SERVANT_CARD_EMIYA_ARCHER_CHEST).get();
        };
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
