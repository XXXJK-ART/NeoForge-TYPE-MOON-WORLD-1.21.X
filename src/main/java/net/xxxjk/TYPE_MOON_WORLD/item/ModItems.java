package net.xxxjk.TYPE_MOON_WORLD.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChiselItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CommandSpellItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.DirkSmallKnifeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BlackKeyItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.Magic_fragmentsItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomStartAttributesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.CarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.FullManaCarvedGemItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemType;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GemQuality;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RandomMagicScrollItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.PartitionedThoughtFragmentItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.AvalonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.TempleStoneSwordAxeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MuramasaItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticEyesItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ExcaliburGoldenItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.EmiyaProjectionItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GaeBulgItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshHarmlessGaeBulgItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HecatesStaffItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.HeshikiriHasebeItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RubyStaffItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SpiderCutterItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.JapaneseSwordItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.LeylineSurveyMapItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ManaSurveyCompassItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MagicCrestItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MasterCardItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MercurySwordItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.MysticMercuryItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NamelessChainDaggerItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.RuleBreakerItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardArmorItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GenericServantSummonItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantCardReleaseItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantMasterContractItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ThompsonContenderItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.NightingaleGunItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.BajiquanManualItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GanryuManualItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.ChalkItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.OdaMatchlockCatalystItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.SummoningRelicItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.YajiaoQiangItem;
import net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshSlateItem;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(TYPE_MOON_WORLD.MOD_ID);

    private static DeferredItem<Item> registerSimpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> registerSummoningRelic(String name) {
        return ITEMS.register(name, () -> new SummoningRelicItem(new Item.Properties().rarity(Rarity.RARE)));
    }

    private static DeferredItem<Item> registerJapaneseSword(String name, double damage, double speed, double interactionRange) {
        return ITEMS.register(name, () -> new JapaneseSwordItem(name, new Item.Properties().durability(250)
                .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, name + "_damage"),
                                        damage, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, name + "_speed"),
                                        speed, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, name + "_range"),
                                        interactionRange, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                        .build())));
    }

    public static final DeferredItem<Item> WAKIZASHI = registerJapaneseSword("wakizashi", 4.0, -2.2, -0.25);
    public static final DeferredItem<Item> KATANA = registerJapaneseSword("katana", 5.0, -2.4, 0.0);
    public static final DeferredItem<Item> NODACHI = registerJapaneseSword("nodachi", 6.0, -2.6, 0.5);

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
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "temple_stone_sword_axe_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
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
    public static final DeferredItem<Item> UNKNOWN_MAGIC_PAGE = ITEMS.register("unknown_magic_page",
            () -> new Item(new Item.Properties().stacksTo(3)));
    public static final DeferredItem<Item> MAGIC_WASTE_PAPER = ITEMS.register("magic_waste_paper",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> RANDOM_GEM = ITEMS.register("random_gem",
            () -> new RandomGemItem(new Item.Properties()));
    public static final DeferredItem<Item> RANDOM_START_ATTRIBUTES = ITEMS.register("random_start_attributes",
            () -> new RandomStartAttributesItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> CLAW_OF_CHAOS = registerSimpleItem("claw_of_chaos");
    public static final DeferredItem<Item> DRAGON_FANG = registerSimpleItem("dragon_fang");
    public static final DeferredItem<Item> DRAGONS_REVERSE_SCALE = registerSimpleItem("dragons_reverse_scale");
    public static final DeferredItem<Item> EVIL_BONE = registerSimpleItem("evil_bone");
    public static final DeferredItem<Item> SEA_BEAST_BONE = registerSummoningRelic("sea_beast_bone");
    public static final DeferredItem<Item> ODA_MATCHLOCK_CATALYST = ITEMS.register("oda_matchlock_catalyst",
            () -> new OdaMatchlockCatalystItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BROKEN_BOWSTRING = registerSummoningRelic("broken_bowstring");
    public static final DeferredItem<Item> CHALK = ITEMS.register("chalk",
            () -> new ChalkItem(new Item.Properties().stacksTo(64)));
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
    public static final DeferredItem<Item> SUPERVISOR_COMMAND_SPELL = ITEMS.register("supervisor_command_spell",
            () -> new CommandSpellItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> SINGLE_COMMAND_SPELL = ITEMS.register("single_command_spell",
            () -> new CommandSpellItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SERVANT_CARD_RELEASE = ITEMS.register("servant_card_release",
            () -> new ServantCardReleaseItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SERVANT_CARD_GENERIC = ITEMS.register("servant_card",
            () -> new ServantCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ""));
    public static final DeferredItem<Item> MASTER_CARD_GENERIC = ITEMS.register("master_card",
            () -> new MasterCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ""));
    public static final DeferredItem<Item> SERVANT_SUMMON_GENERIC = ITEMS.register("servant_summon",
            () -> new GenericServantSummonItem(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<Item> SERVANT_ARMOR_GENERIC_HEAD = registerServantArmor("", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_ARMOR_GENERIC_CHEST = registerServantArmor("", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_ARMOR_GENERIC_LEGS = registerServantArmor("", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_ARMOR_GENERIC_FEET = registerServantArmor("", net.minecraft.world.entity.EquipmentSlot.FEET);

    public static final DeferredItem<Item> MASTER_CARD_TOHSAKA_RIN = registerMasterCard("tohsaka_rin");
    public static final DeferredItem<Item> MASTER_CARD_EMIYA_KIRITSUGU = registerMasterCard("emiya_kiritsugu");
    public static final DeferredItem<Item> MASTER_CARD_EMIYA_SHIROU = registerMasterCard("emiya_shirou");
    public static final DeferredItem<Item> MASTER_CARD_KOTOMINE_KIREI = registerMasterCard("kotomine_kirei");
    public static final DeferredItem<Item> MASTER_CARD_LUVIA = registerMasterCard("luvia");
    public static final DeferredItem<Item> MASTER_CARD_ELSA_SAIJO = registerMasterCard("elsa_saijo");
    public static final DeferredItem<Item> MASTER_CARD_WAVER = registerMasterCard("waver");
    public static final DeferredItem<Item> MASTER_CARD_TOHSAKA_TOKIOMI = registerMasterCard("tohsaka_tokiomi");

    public static final DeferredItem<Item> RELIC_APOCALYPSE = registerSummoningRelic("relic_apocalypse");
    public static final DeferredItem<Item> RELIC_APOCALYPSE_PAGE = registerSummoningRelic("relic_apocalypse_page");
    public static final DeferredItem<Item> RELIC_FIRST_SNAKE_SKIN = registerSummoningRelic("relic_first_snake_skin");
    public static final DeferredItem<Item> RELIC_HAJIQUAN_MANUAL = ITEMS.register("relic_hajiquan_manual",
            () -> new BajiquanManualItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> GANRYU_MANUAL = ITEMS.register("ganryu_manual",
            () -> new GanryuManualItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> HOKUSHIN_MANUAL = ITEMS.register("hokushin_manual",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.KendoManualItem(new Item.Properties().rarity(Rarity.RARE),
                    net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool.HOKUSHIN));
    public static final DeferredItem<Item> TENNEN_MANUAL = ITEMS.register("tennen_manual",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.KendoManualItem(new Item.Properties().rarity(Rarity.RARE),
                    net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool.TENNEN));
    public static final DeferredItem<Item> SPARRING_INVITATION = ITEMS.register("sparring_invitation",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> RELIC_VALKYRIE_ARROWHEAD = registerSimpleItem("relic_valkyrie_arrowhead");
    public static final DeferredItem<Item> RELIC_BRONZE_MIRROR = registerSummoningRelic("relic_bronze_mirror");
    public static final DeferredItem<Item> RELIC_ROUND_TABLE_FRAGMENT = registerSummoningRelic("relic_round_table_fragment");
    public static final DeferredItem<Item> RELIC_TSUBURA_SHIP_PLANK = registerSummoningRelic("relic_tsubura_ship_plank");
    public static final DeferredItem<Item> RELIC_BIZEN_TSUBA = registerSummoningRelic("relic_bizen_tsuba");
    public static final DeferredItem<Item> RELIC_OLD_MAN_MASK = registerSummoningRelic("relic_old_man_mask");
    public static final DeferredItem<Item> RELIC_BANDAGE = registerSummoningRelic("relic_bandage");
    public static final DeferredItem<Item> RELIC_PHILOSOPHERS_STONE = registerSummoningRelic("relic_philosophers_stone");
    public static final DeferredItem<Item> RELIC_GOLDEN_FLEECE = registerSummoningRelic("relic_golden_fleece");
    public static final DeferredItem<Item> RELIC_ATO_CRADLE = registerSummoningRelic("relic_ato_cradle");
    public static final DeferredItem<Item> GEM_NECKLACE = registerSummoningRelic("gem_necklace");
    public static final DeferredItem<Item> MYSTIC_MERCURY = ITEMS.register("mystic_mercury",
            () -> new MysticMercuryItem(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> GILGAMESH_BAB_ILU = ITEMS.register("gilgamesh_bab_ilu",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "babili"));
    public static final DeferredItem<Item> GILGAMESH_EA = ITEMS.register("gilgamesh_ea",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "ea"));
    public static final DeferredItem<Item> GILGAMESH_DURANDAL = ITEMS.register("gilgamesh_durandal",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "durandal"));
    public static final DeferredItem<Item> GILGAMESH_GRAM = ITEMS.register("gilgamesh_gram",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "gram"));
    public static final DeferredItem<Item> GILGAMESH_HARPE = ITEMS.register("gilgamesh_harpe",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "harpe"));
    public static final DeferredItem<Item> GILGAMESH_VAJRA = ITEMS.register("gilgamesh_vajra",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1), "vajra"));
    public static final DeferredItem<Item> GILGAMESH_FANGTIAN_HUAJI = ITEMS.register("gilgamesh_fangtian_huaji",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.GilgameshNoblePhantasmItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gilgamesh_fangtian_huaji_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build()), "fangtian_huaji"));
    public static final DeferredItem<Item> GILGAMESH_SPIRAL_SWORD = ITEMS.register("gilgamesh_spiral_sword",
            () -> new EmiyaProjectionItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant(), "pseudo_spiral_sword"));
    public static final DeferredItem<Item> GILGAMESH_GAE_BULG = ITEMS.register("gilgamesh_gae_bulg",
            () -> new GilgameshHarmlessGaeBulgItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "gilgamesh_gae_bulg_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER = registerServantCard("emiya_archer");
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON = registerServantCard("artoria_pendragon");
    public static final DeferredItem<Item> SERVANT_CARD_SASAKI_KOJIRO = registerServantCard("sasaki_kojiro");
    public static final DeferredItem<Item> SERVANT_CARD_CU_CHULAINN = registerServantCard("cu_chulainn");
    public static final DeferredItem<Item> SERVANT_CARD_MEDEA = registerServantCard("medea");
    public static final DeferredItem<Item> SERVANT_CARD_MEDUSA = registerServantCard("medusa");
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN = registerServantCard("cursed_arm_hassan");
    public static final DeferredItem<Item> SERVANT_CARD_SHADOW_HASSAN = registerServantCard("shadow_hassan");
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES = registerServantCard("heracles");
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA = registerServantCard("oda_nobunaga");
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU = registerServantCard("enkidu");
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH = registerServantCard("gilgamesh");
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_CASTER = registerServantCard("gilgamesh_caster");
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN = registerServantCard("gawain");
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS = registerServantCard("paracelsus");
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN = registerServantCard("li_shuwen");
    public static final DeferredItem<Item> SERVANT_CARD_PALE_RIDER = registerServantCard("pale_rider");
    public static final DeferredItem<Item> SERVANT_CARD_HUNDRED_FACES_HASSAN = registerServantCard("hundred_faces_hassan");
    public static final DeferredItem<Item> SERVANT_CARD_USHIWAKAMARU_RIDER = registerServantCard("ushiwakamaru_rider");
    public static final DeferredItem<Item> SERVANT_CARD_FANATIC_ASSASSIN = registerServantCard("fanatic_assassin");
    public static final DeferredItem<Item> SERVANT_CARD_ARASH = registerServantCard("arash");
    public static final DeferredItem<Item> SERVANT_CARD_NIGHTINGALE = registerServantCard("nightingale");
    public static final DeferredItem<Item> SERVANT_CARD_ZHAO_YUN_RIDER = registerServantCard("zhao_yun_rider");
    public static final DeferredItem<Item> SERVANT_CARD_SENKO_MURAMASA = registerServantCard("senko_muramasa");

    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER_CHEST = registerServantArmor("emiya_archer", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_EMIYA_ARCHER_LEGS = registerServantArmor("emiya_archer", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON_HEAD = registerServantArmor("artoria_pendragon", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON_CHEST = registerServantArmor("artoria_pendragon", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ARTORIA_PENDRAGON_LEGS = registerServantArmor("artoria_pendragon", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_SASAKI_KOJIRO_HEAD = registerServantArmor("sasaki_kojiro", net.minecraft.world.entity.EquipmentSlot.HEAD);
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
    public static final DeferredItem<Item> SHADOW_HASSAN_MASK = registerServantArmor("shadow_hassan", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN_CHEST = registerServantArmor("cursed_arm_hassan", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_CURSED_ARM_HASSAN_LEGS = registerServantArmor("cursed_arm_hassan", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES_CHEST = registerServantArmor("heracles", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_HERACLES_LEGS = registerServantArmor("heracles", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA_CHEST = registerServantArmor("oda_nobunaga", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA_LEGS = registerServantArmor("oda_nobunaga", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ODA_NOBUNAGA_HEAD = registerServantArmor("oda_nobunaga", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU_HEAD = registerServantArmor("enkidu", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU_CHEST = registerServantArmor("enkidu", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ENKIDU_LEGS = registerServantArmor("enkidu", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_CHEST = registerServantArmor("gilgamesh", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_LEGS = registerServantArmor("gilgamesh", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_CASTER_HEAD = registerServantArmor("gilgamesh_caster", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_CASTER_CHEST = registerServantArmor("gilgamesh_caster", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_GILGAMESH_CASTER_LEGS = registerServantArmor("gilgamesh_caster", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN_CHEST = registerServantArmor("gawain", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_GAWAIN_LEGS = registerServantArmor("gawain", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS_HEAD = registerServantArmor("paracelsus", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS_CHEST = registerServantArmor("paracelsus", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_PARACELSUS_LEGS = registerServantArmor("paracelsus", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN_CHEST = registerServantArmor("li_shuwen", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN_LEGS = registerServantArmor("li_shuwen", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_LI_SHUWEN_HEAD = registerServantArmor("li_shuwen", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_USHIWAKAMARU_RIDER_HEAD = registerServantArmor("ushiwakamaru_rider", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_USHIWAKAMARU_RIDER_CHEST = registerServantArmor("ushiwakamaru_rider", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_USHIWAKAMARU_RIDER_LEGS = registerServantArmor("ushiwakamaru_rider", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_FANATIC_ASSASSIN_HEAD = registerServantArmor("fanatic_assassin", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_FANATIC_ASSASSIN_CHEST = registerServantArmor("fanatic_assassin", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_HUNDRED_FACES_HASSAN_HEAD = registerServantArmor("hundred_faces_hassan", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_HUNDRED_FACES_HASSAN_CHEST = registerServantArmor("hundred_faces_hassan", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_HUNDRED_FACES_HASSAN_LEGS = registerServantArmor("hundred_faces_hassan", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ARASH_CHEST = registerServantArmor("arash", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ARASH_LEGS = registerServantArmor("arash", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_NIGHTINGALE_CHEST = registerServantArmor("nightingale", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_NIGHTINGALE_LEGS = registerServantArmor("nightingale", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_ZHAO_YUN_RIDER_HEAD = registerServantArmor("zhao_yun_rider", net.minecraft.world.entity.EquipmentSlot.HEAD);
    public static final DeferredItem<Item> SERVANT_CARD_ZHAO_YUN_RIDER_CHEST = registerServantArmor("zhao_yun_rider", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_ZHAO_YUN_RIDER_LEGS = registerServantArmor("zhao_yun_rider", net.minecraft.world.entity.EquipmentSlot.LEGS);
    public static final DeferredItem<Item> SERVANT_CARD_SENKO_MURAMASA_CHEST = registerServantArmor("senko_muramasa", net.minecraft.world.entity.EquipmentSlot.CHEST);
    public static final DeferredItem<Item> SERVANT_CARD_SENKO_MURAMASA_LEGS = registerServantArmor("senko_muramasa", net.minecraft.world.entity.EquipmentSlot.LEGS);

    private static DeferredItem<Item> registerServantCard(String servantId) {
        return ITEMS.register("servant_card_" + servantId,
                () -> new ServantCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), servantId));
    }

    private static DeferredItem<Item> registerMasterCard(String masterId) {
        return ITEMS.register("master_card_" + masterId,
                () -> new MasterCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), masterId));
    }

    private static DeferredItem<Item> registerServantArmor(String servantId, net.minecraft.world.entity.EquipmentSlot slot) {
        String name = servantId.isBlank() ? "servant_armor_generic_" + slot.getName() : "servant_card_" + servantId + "_" + slot.getName();
        return ITEMS.register(name,
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
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null,
                    "jewel_magic_shoot", "jewel_random_shoot"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL = ITEMS.register("magic_scroll_advanced_jewel",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_magic_release"));

    public static final DeferredItem<Item> MAGIC_SCROLL_ADVANCED_JEWEL_BROKEN = ITEMS.register("magic_scroll_advanced_jewel_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.3, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_magic_release"));

    public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN = ITEMS.register("magic_scroll_machine_gun",
            () -> new MagicScrollItem(new Item.Properties().durability(1), 1.0, "jewel_magic_shoot", // Require Basic Jewel Magic
                    "jewel_machine_gun"));

    public static final DeferredItem<Item> MAGIC_SCROLL_MACHINE_GUN_BROKEN = ITEMS.register("magic_scroll_machine_gun_broken",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.3, "jewel_magic_shoot", // Require Basic Jewel Magic, Low Chance
                    "jewel_machine_gun"));

    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION = ITEMS.register("magic_scroll_projection",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.5, true, (String)null, 
                    "projection", "structural_analysis"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_PROJECTION_BROKEN = ITEMS.register("magic_scroll_projection_broken",
            () -> new MagicScrollItem(new Item.Properties().stacksTo(3), 0.2, true, (String)null,
                    "projection", "structural_analysis"));
    public static final DeferredItem<Item> MAGIC_BOOK_PROJECTION = ITEMS.register("magic_book_projection",
            () -> new MagicScrollItem(new Item.Properties().stacksTo(1), 1.0, false, (String)null, "projection"));
    public static final DeferredItem<Item> MAGIC_PAGE_PROJECTION = ITEMS.register("magic_page_projection",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "projection"));
    public static final DeferredItem<Item> MAGIC_BOOK_STRUCTURAL_ANALYSIS = ITEMS.register("magic_book_structural_analysis",
            () -> new MagicScrollItem(new Item.Properties().stacksTo(1), 1.0, false, (String)null, "structural_analysis"));
    public static final DeferredItem<Item> MAGIC_PAGE_STRUCTURAL_ANALYSIS = ITEMS.register("magic_page_structural_analysis",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "structural_analysis"));
    public static final DeferredItem<Item> MAGIC_BOOK_MAGIC_ANALYSIS = ITEMS.register("magic_book_magic_analysis",
            () -> new MagicScrollItem(new Item.Properties().stacksTo(1), 1.0, false, (String)null, "magic_analysis"));
    public static final DeferredItem<Item> MAGIC_PAGE_MAGIC_ANALYSIS = ITEMS.register("magic_page_magic_analysis",
            () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "magic_analysis"));

    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM = ITEMS.register("magic_scroll_broken_phantasm",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.5, false, "projection", // Require Projection
                    "broken_phantasm"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_BROKEN_PHANTASM_BROKEN = ITEMS.register("magic_scroll_broken_phantasm_broken",
             () -> new MagicScrollItem(new Item.Properties().stacksTo(3), 0.1, false, "projection", // Require Projection
                    "broken_phantasm"));
    
    public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY = ITEMS.register("magic_scroll_gravity",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 0.8, false, (String)null, 
                    "gravity_magic"));
                    
    public static final DeferredItem<Item> MAGIC_SCROLL_GRAVITY_BROKEN = ITEMS.register("magic_scroll_gravity_broken",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.3, (String)null,
                    "gravity_magic"));

    public static final DeferredItem<Item> MAGIC_SCROLL_GANDER = ITEMS.register("magic_scroll_gander",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null,
                    "gander"));

    public static final DeferredItem<Item> MAGIC_SCROLL_GANDER_BROKEN = ITEMS.register("magic_scroll_gander_broken",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null,
                    "gander"));

    public static final DeferredItem<Item> MAGIC_BOOK_REINFORCEMENT = ITEMS.register("magic_book_reinforcement",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, // learnAll=false (default), no req
                    (String)null, "reinforcement"));

    public static final DeferredItem<Item> MAGIC_BOOK_MANA_BURST = ITEMS.register("magic_book_mana_burst",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "mana_burst"));

    public static final DeferredItem<Item> MAGIC_PAGE_REINFORCEMENT = ITEMS.register("magic_page_reinforcement",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, // no req
                    (String)null, "reinforcement", "reinforcement_self", "reinforcement_other", "reinforcement_item"));

    public static final DeferredItem<Item> MAGIC_BOOK_HEALING = ITEMS.register("magic_book_healing",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "healing_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_HEALING = ITEMS.register("magic_page_healing",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "healing_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_MAGIC_BULLET = ITEMS.register("magic_book_magic_bullet",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "magic_bullet"));

    public static final DeferredItem<Item> MAGIC_PAGE_MAGIC_BULLET = ITEMS.register("magic_page_magic_bullet",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "magic_bullet"));

    public static final DeferredItem<Item> MAGIC_BOOK_SUGGESTION = ITEMS.register("magic_book_suggestion",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "suggestion_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_SUGGESTION = ITEMS.register("magic_page_suggestion",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "suggestion_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_BINDING = ITEMS.register("magic_book_binding",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "binding_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_BINDING = ITEMS.register("magic_page_binding",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "binding_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_FIRE = ITEMS.register("magic_book_fire",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "fire_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_FIRE = ITEMS.register("magic_page_fire",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "fire_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_WATER = ITEMS.register("magic_book_water",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "water_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_WATER = ITEMS.register("magic_page_water",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "water_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_WIND = ITEMS.register("magic_book_wind",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "wind_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_WIND = ITEMS.register("magic_page_wind",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "wind_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_EARTH = ITEMS.register("magic_book_earth",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "earth_magic"));

    public static final DeferredItem<Item> MAGIC_PAGE_EARTH = ITEMS.register("magic_page_earth",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "earth_magic"));

    public static final DeferredItem<Item> MAGIC_BOOK_TIME_ALTER = ITEMS.register("magic_book_time_alter",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "time_alter"));

    public static final DeferredItem<Item> MAGIC_PAGE_TIME_ALTER = ITEMS.register("magic_page_time_alter",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "time_alter"));

    public static final DeferredItem<Item> MAGIC_BOOK_SPIRITUAL_HEALING = ITEMS.register("magic_book_spiritual_healing",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "spiritual_healing"));

    public static final DeferredItem<Item> MAGIC_PAGE_SPIRITUAL_HEALING = ITEMS.register("magic_page_spiritual_healing",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "spiritual_healing"));

    public static final DeferredItem<Item> MAGIC_BOOK_BAPTISM_RITE = ITEMS.register("magic_book_baptism_rite",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "baptism_rite"));

    public static final DeferredItem<Item> MAGIC_PAGE_BAPTISM_RITE = ITEMS.register("magic_page_baptism_rite",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "baptism_rite"));
    public static final DeferredItem<Item> PARTITIONED_THOUGHT_FRAGMENT = ITEMS.register("partitioned_thought_fragment",
            () -> new PartitionedThoughtFragmentItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> MAGIC_BOOK_BLACK_KEY_FIRE_ENGRAVING = ITEMS.register("magic_book_black_key_fire_engraving",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "black_key_fire_engraving"));
    public static final DeferredItem<Item> MAGIC_PAGE_BLACK_KEY_FIRE_ENGRAVING = ITEMS.register("magic_page_black_key_fire_engraving",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "black_key_fire_engraving"));
    public static final DeferredItem<Item> MAGIC_BOOK_STIGMA = ITEMS.register("magic_book_stigma",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "stigma"));
    public static final DeferredItem<Item> MAGIC_PAGE_STIGMA = ITEMS.register("magic_page_stigma",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "stigma"));
    public static final DeferredItem<Item> MAGIC_BOOK_ABSORPTION = ITEMS.register("magic_book_absorption",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "absorption"));
    public static final DeferredItem<Item> MAGIC_PAGE_ABSORPTION = ITEMS.register("magic_page_absorption",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "absorption"));
    public static final DeferredItem<Item> MAGIC_BOOK_AIRFLOW_BLADE = ITEMS.register("magic_book_airflow_blade",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "airflow_blade"));
    public static final DeferredItem<Item> MAGIC_PAGE_AIRFLOW_BLADE = ITEMS.register("magic_page_airflow_blade",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "airflow_blade"));
    public static final DeferredItem<Item> MAGIC_BOOK_ANDRASIAS = ITEMS.register("magic_book_andrasias",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "andrasias"));
    public static final DeferredItem<Item> MAGIC_PAGE_ANDRASIAS = ITEMS.register("magic_page_andrasias",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "andrasias"));
    public static final DeferredItem<Item> MAGIC_BOOK_ANDREPHIUS = ITEMS.register("magic_book_andrephius",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "andrephius"));
    public static final DeferredItem<Item> MAGIC_PAGE_ANDREPHIUS = ITEMS.register("magic_page_andrephius",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "andrephius"));
    public static final DeferredItem<Item> MAGIC_BOOK_ANTORES = ITEMS.register("magic_book_antores",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "antores"));
    public static final DeferredItem<Item> MAGIC_PAGE_ANTORES = ITEMS.register("magic_page_antores",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "antores"));
    public static final DeferredItem<Item> MAGIC_BOOK_DEMON_GOD_GAZE = ITEMS.register("magic_book_demon_god_gaze",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "demon_god_gaze"));
    public static final DeferredItem<Item> MAGIC_PAGE_DEMON_GOD_GAZE = ITEMS.register("magic_page_demon_god_gaze",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "demon_god_gaze"));
    public static final DeferredItem<Item> MAGIC_BOOK_DETECTION = ITEMS.register("magic_book_detection",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "detection"));
    public static final DeferredItem<Item> MAGIC_PAGE_DETECTION = ITEMS.register("magic_page_detection",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "detection"));
    public static final DeferredItem<Item> MAGIC_BOOK_IMAGINARY_DISPLACEMENT = ITEMS.register("magic_book_imaginary_displacement",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "imaginary_displacement"));
    public static final DeferredItem<Item> MAGIC_PAGE_IMAGINARY_DISPLACEMENT = ITEMS.register("magic_page_imaginary_displacement",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "imaginary_displacement"));
    public static final DeferredItem<Item> MAGIC_BOOK_IMAGINARY_DIVE = ITEMS.register("magic_book_imaginary_dive",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "imaginary_dive"));
    public static final DeferredItem<Item> MAGIC_PAGE_IMAGINARY_DIVE = ITEMS.register("magic_page_imaginary_dive",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "imaginary_dive"));
    public static final DeferredItem<Item> MAGIC_BOOK_IMAGINARY_SPACE = ITEMS.register("magic_book_imaginary_space",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "imaginary_space"));
    public static final DeferredItem<Item> MAGIC_PAGE_IMAGINARY_SPACE = ITEMS.register("magic_page_imaginary_space",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "imaginary_space"));
    public static final DeferredItem<Item> MAGIC_BOOK_KIMARIS = ITEMS.register("magic_book_kimaris",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "kimaris"));
    public static final DeferredItem<Item> MAGIC_PAGE_KIMARIS = ITEMS.register("magic_page_kimaris",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "kimaris"));
    public static final DeferredItem<Item> MAGIC_BOOK_NEGA_SUMMON = ITEMS.register("magic_book_nega_summon",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "nega_summon"));
    public static final DeferredItem<Item> MAGIC_PAGE_NEGA_SUMMON = ITEMS.register("magic_page_nega_summon",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "nega_summon"));
    public static final DeferredItem<Item> MAGIC_BOOK_ORIAS = ITEMS.register("magic_book_orias",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "orias"));
    public static final DeferredItem<Item> MAGIC_PAGE_ORIAS = ITEMS.register("magic_page_orias",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "orias"));
    public static final DeferredItem<Item> MAGIC_BOOK_STORAGE = ITEMS.register("magic_book_storage",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "storage"));
    public static final DeferredItem<Item> MAGIC_PAGE_STORAGE = ITEMS.register("magic_page_storage",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "storage"));
    public static final DeferredItem<Item> MAGIC_BOOK_STORM = ITEMS.register("magic_book_storm",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "storm"));
    public static final DeferredItem<Item> MAGIC_PAGE_STORM = ITEMS.register("magic_page_storm",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "storm"));
    public static final DeferredItem<Item> MAGIC_BOOK_ZAGAN = ITEMS.register("magic_book_zagan",
            () -> new MagicScrollItem(new Item.Properties().durability(20), 1.0, (String)null, "zagan"));
    public static final DeferredItem<Item> MAGIC_PAGE_ZAGAN = ITEMS.register("magic_page_zagan",
             () -> new RandomMagicScrollItem(new Item.Properties().stacksTo(3), 0.5, (String)null, "zagan"));

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
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "hecates_staff_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> RUBY_STAFF = ITEMS.register("ruby_staff",
            () -> new RubyStaffItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ruby_staff_damage"),
                                            7.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ruby_staff_speed"),
                                            0.6, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "ruby_staff_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
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
    public static final DeferredItem<Item> ARASH_BOW = ITEMS.register("arash_bow",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ArashBowItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> GILGAMESH_SLATE = ITEMS.register("gilgamesh_slate",
            () -> new GilgameshSlateItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
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
    public static final DeferredItem<Item> YAJIAO_QIANG = ITEMS.register("yajiao_qiang",
            () -> new YajiaoQiangItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yajiao_qiang_damage"),
                                            12.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yajiao_qiang_speed"),
                                            1.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "yajiao_qiang_range"),
                                            2.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));

    public static final DeferredItem<Item> SPIDER_CUTTER = ITEMS.register("spider_cutter",
            () -> new SpiderCutterItem(new Item.Properties().durability(SpiderCutterItem.DURABILITY).fireResistant().rarity(Rarity.RARE)
                    .attributes(net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "spider_cutter_damage"),
                                            SpiderCutterItem.ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                                    new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "spider_cutter_speed"),
                                            SpiderCutterItem.ATTACK_SPEED, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                                    net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                            .build())));
    public static final DeferredItem<Item> THOMPSON_CONTENDER = ITEMS.register("thompson_contender",
            () -> new ThompsonContenderItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> NIGHTINGALE_GUN = ITEMS.register("nightingale_gun",
            () -> new NightingaleGunItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant()));
    public static final DeferredItem<Item> BLACK_KEY = ITEMS.register("black_key",
            () -> new BlackKeyItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
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
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_GRAND_SPAWN_EGG = ITEMS.register("mystic_magician_grand_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_GRAND, 0x24113D, 0xF2D27A, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_BRAND_SPAWN_EGG = ITEMS.register("mystic_magician_brand_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_BRAND, 0x8B1E3F, 0xF6C453, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_PRIDE_SPAWN_EGG = ITEMS.register("mystic_magician_pride_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_PRIDE, 0x1D3B6D, 0x9BC6E4, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_FES_SPAWN_EGG = ITEMS.register("mystic_magician_fes_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_FES, 0x5B2B73, 0xD8A6F0, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_ADEPT_SPAWN_EGG = ITEMS.register("mystic_magician_adept_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_ADEPT, 0x315B3A, 0xB6D39A, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_UMNOS_SPAWN_EGG = ITEMS.register("mystic_magician_umnos_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_UMNOS, 0x42515C, 0xD3B98A, new Item.Properties()));
    public static final DeferredItem<Item> MYSTIC_MAGICIAN_FRAME_SPAWN_EGG = ITEMS.register("mystic_magician_frame_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTIC_MAGICIAN_FRAME, 0x5A5A5A, 0xC9C9C9, new Item.Properties()));
    public static final DeferredItem<Item> THE_DEAD_SPAWN_EGG = ITEMS.register("the_dead_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.THE_DEAD, 0x5C5C5C, 0x262626, new Item.Properties()));
    public static final DeferredItem<Item> GHOUL_SPAWN_EGG = ITEMS.register("ghoul_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.GHOUL, 0x66705B, 0x2B3028, new Item.Properties()));
    public static final DeferredItem<Item> LIVING_DEAD_SPAWN_EGG = ITEMS.register("living_dead_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.LIVING_DEAD, 0x6E7375, 0xA5A09A, new Item.Properties()));
    public static final DeferredItem<Item> NIGHT_KIN_SPAWN_EGG = ITEMS.register("night_kin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NIGHT_KIN, 0x272733, 0x8C2636, new Item.Properties()));
    public static final DeferredItem<Item> NERO_CHAOS_SPAWN_EGG = ITEMS.register("nero_chaos_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NERO_CHAOS, 0x080808, 0x4A0A12, new Item.Properties()));
    public static final DeferredItem<Item> CHURCH_EXECUTOR_SPAWN_EGG = ITEMS.register("church_executor_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.CHURCH_EXECUTOR, 0x202020, 0xD0D0D0, new Item.Properties()));

    public static final DeferredItem<Item> BAJIQUAN_MASTER_SPAWN_EGG = ITEMS.register("bajiquan_master_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.BAJIQUAN_MASTER, 0x27331F, 0xD8C59A, new Item.Properties()));
    public static final DeferredItem<Item> BAJIQUAN_APPRENTICE_SPAWN_EGG = ITEMS.register("bajiquan_apprentice_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.BAJIQUAN_APPRENTICE, 0x3B5E39, 0xE2D2B4, new Item.Properties()));
    public static final DeferredItem<Item> MYSTERIOUS_SWORDSMAN_SPAWN_EGG = ITEMS.register("mysterious_swordsman_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.MYSTERIOUS_SWORDSMAN, 0x26365A, 0xD7C6A5, new Item.Properties()));
    public static final DeferredItem<Item> KENDO_MASTER_SPAWN_EGG = ITEMS.register("kendo_master_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.KendoMasterSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.KENDO_MASTER, 0x51362A, 0xD8C59A, new Item.Properties(), net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool.HOKUSHIN));
    public static final DeferredItem<Item> KENDO_MASTER_TENNEN_SPAWN_EGG = ITEMS.register("kendo_master_tennen_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.KendoMasterSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.KENDO_MASTER, 0x25466A, 0xD7C6A5, new Item.Properties(), net.xxxjk.TYPE_MOON_WORLD.martial.KendoSchool.TENNEN));
    public static final DeferredItem<Item> KENDO_APPRENTICE_SPAWN_EGG = ITEMS.register("kendo_apprentice_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.KENDO_APPRENTICE, 0x4B6A73, 0xE2D2B4, new Item.Properties()));
    public static final DeferredItem<Item> RONIN_SPAWN_EGG = ITEMS.register("ronin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.RONIN, 0x3A2D29, 0xB68D6F, new Item.Properties()));
    public static final DeferredItem<Item> SHINSENGUMI_SPAWN_EGG = ITEMS.register("shinsengumi_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.SHINSENGUMI, 0x25466A, 0xD7C6A5, new Item.Properties()));
    public static final DeferredItem<Item> TOHSAKA_RIN_SPAWN_EGG = ITEMS.register("tohsaka_rin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.TOHSAKA_RIN, 0x8B1126, 0xE8D8C5, new Item.Properties()));

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
    public static final DeferredItem<Item> USHIWAKAMARU_RIDER_SPAWN_EGG = ITEMS.register("ushiwakamaru_rider_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.USHIWAKAMARU_RIDER, 0x4A1E38, 0xE9C9D5));
    public static final DeferredItem<Item> ZHAO_YUN_RIDER_SPAWN_EGG = ITEMS.register("zhao_yun_rider_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ZHAO_YUN_RIDER, 0xDDEEFF, 0x33AA66));
    public static final DeferredItem<Item> ENKIDU_SPAWN_EGG = ITEMS.register("enkidu_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ENKIDU, 0xF4FFF7, 0x7FE7B2));
    public static final DeferredItem<Item> GILGAMESH_SPAWN_EGG = ITEMS.register("gilgamesh_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.GILGAMESH, 0xD4AF37, 0xB11226));
    public static final DeferredItem<Item> GILGAMESH_CASTER_SPAWN_EGG = ITEMS.register("gilgamesh_caster_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.GILGAMESH_CASTER, 0xE4B94E, 0x2D4A7A));
    public static final DeferredItem<Item> GAWAIN_SPAWN_EGG = ITEMS.register("gawain_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.GAWAIN, 0xC0C0C0, 0xFFD700));
    public static final DeferredItem<Item> SENKO_MURAMASA_SPAWN_EGG = ITEMS.register("senko_muramasa_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.SENKO_MURAMASA, 0xFF3300, 0x444444));
    public static final DeferredItem<Item> LI_SHUWEN_SPAWN_EGG = ITEMS.register("li_shuwen_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.LI_SHUWEN, 0x1A1A1A, 0xD8D0C8));
    public static final DeferredItem<Item> PARACELSUS_SPAWN_EGG = ITEMS.register("paracelsus_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.PARACELSUS, 0xA9E7FF, 0xFFD36A));
    public static final DeferredItem<Item> PALE_RIDER_SPAWN_EGG = ITEMS.register("pale_rider_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.PALE_RIDER, 0x17191C, 0xC7CBD1));
    public static final DeferredItem<Item> NIGHTINGALE_SPAWN_EGG = ITEMS.register("nightingale_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.NIGHTINGALE, 0xA73A45, 0xF3E7DC));
    public static final DeferredItem<Item> SHADOW_HASSAN_SPAWN_EGG = ITEMS.register("shadow_hassan_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.SHADOW_HASSAN, 0x080808, 0xE8E4DD));
    public static final DeferredItem<Item> FANATIC_ASSASSIN_SPAWN_EGG = ITEMS.register("fanatic_assassin_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.FANATIC_ASSASSIN, 0x15151D, 0xB9A7A0));
    public static final DeferredItem<Item> HUNDRED_FACES_HASSAN_SPAWN_EGG = ITEMS.register("hundred_faces_hassan_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.HUNDRED_FACES_HASSAN, 0x15151A, 0xD8D2C8));
    public static final DeferredItem<Item> ARASH_SPAWN_EGG = ITEMS.register("arash_spawn_egg",
            () -> new net.xxxjk.TYPE_MOON_WORLD.item.custom.ServantEntitySpawnEggItem(net.xxxjk.TYPE_MOON_WORLD.init.ModEntities.ARASH, 0x2F6F65, 0xC66B32));

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
        if (servantId != null && servantId.indexOf(':') >= 0) {
            return switch (slot) {
                case HEAD -> SERVANT_ARMOR_GENERIC_HEAD.get();
                case LEGS -> SERVANT_ARMOR_GENERIC_LEGS.get();
                case FEET -> SERVANT_ARMOR_GENERIC_FEET.get();
                default -> SERVANT_ARMOR_GENERIC_CHEST.get();
            };
        }
        boolean head = slot == net.minecraft.world.entity.EquipmentSlot.HEAD;
        boolean legs = slot == net.minecraft.world.entity.EquipmentSlot.LEGS;
        return switch (servantId == null ? "" : servantId) {
            case "pale_rider" -> Items.AIR;
            case "emiya_archer" -> (legs ? SERVANT_CARD_EMIYA_ARCHER_LEGS : SERVANT_CARD_EMIYA_ARCHER_CHEST).get();
            case "artoria_pendragon" -> (head ? SERVANT_CARD_ARTORIA_PENDRAGON_HEAD : legs ? SERVANT_CARD_ARTORIA_PENDRAGON_LEGS : SERVANT_CARD_ARTORIA_PENDRAGON_CHEST).get();
            case "sasaki_kojiro" -> (head ? SERVANT_CARD_SASAKI_KOJIRO_HEAD : legs ? SERVANT_CARD_SASAKI_KOJIRO_LEGS : SERVANT_CARD_SASAKI_KOJIRO_CHEST).get();
            case "cu_chulainn" -> (legs ? SERVANT_CARD_CU_CHULAINN_LEGS : SERVANT_CARD_CU_CHULAINN_CHEST).get();
            case "medea" -> (head ? SERVANT_CARD_MEDEA_HEAD : legs ? SERVANT_CARD_MEDEA_LEGS : SERVANT_CARD_MEDEA_CHEST).get();
            case "medusa" -> (head ? SERVANT_CARD_MEDUSA_HEAD : legs ? SERVANT_CARD_MEDUSA_LEGS : SERVANT_CARD_MEDUSA_CHEST).get();
            case "cursed_arm_hassan" -> head ? SERVANT_CARD_CURSED_ARM_HASSAN_HEAD.get()
                : slot == net.minecraft.world.entity.EquipmentSlot.CHEST ? SERVANT_CARD_CURSED_ARM_HASSAN_CHEST.get() : Items.AIR;
            case "shadow_hassan" -> head ? SHADOW_HASSAN_MASK.get() : net.minecraft.world.item.Items.AIR;
            case "heracles" -> (legs ? SERVANT_CARD_HERACLES_LEGS : SERVANT_CARD_HERACLES_CHEST).get();
            case "oda_nobunaga" -> (head ? SERVANT_CARD_ODA_NOBUNAGA_HEAD : legs ? SERVANT_CARD_ODA_NOBUNAGA_LEGS : SERVANT_CARD_ODA_NOBUNAGA_CHEST).get();
            case "enkidu" -> (head ? SERVANT_CARD_ENKIDU_HEAD : legs ? SERVANT_CARD_ENKIDU_LEGS : SERVANT_CARD_ENKIDU_CHEST).get();
            case "gilgamesh" -> (legs ? SERVANT_CARD_GILGAMESH_LEGS : SERVANT_CARD_GILGAMESH_CHEST).get();
            case "gilgamesh_caster" -> (head ? SERVANT_CARD_GILGAMESH_CASTER_HEAD : legs ? SERVANT_CARD_GILGAMESH_CASTER_LEGS : SERVANT_CARD_GILGAMESH_CASTER_CHEST).get();
            case "gawain" -> (legs ? SERVANT_CARD_GAWAIN_LEGS : SERVANT_CARD_GAWAIN_CHEST).get();
            case "paracelsus" -> (head ? SERVANT_CARD_PARACELSUS_HEAD : legs ? SERVANT_CARD_PARACELSUS_LEGS : SERVANT_CARD_PARACELSUS_CHEST).get();
            case "li_shuwen" -> (head ? SERVANT_CARD_LI_SHUWEN_HEAD : legs ? SERVANT_CARD_LI_SHUWEN_LEGS : SERVANT_CARD_LI_SHUWEN_CHEST).get();
            case "ushiwakamaru_rider" -> (head ? SERVANT_CARD_USHIWAKAMARU_RIDER_HEAD : legs ? SERVANT_CARD_USHIWAKAMARU_RIDER_LEGS : SERVANT_CARD_USHIWAKAMARU_RIDER_CHEST).get();
            case "fanatic_assassin" -> head ? SERVANT_CARD_FANATIC_ASSASSIN_HEAD.get()
                : slot == net.minecraft.world.entity.EquipmentSlot.CHEST ? SERVANT_CARD_FANATIC_ASSASSIN_CHEST.get() : Items.AIR;
            case "hundred_faces_hassan" -> head ? SERVANT_CARD_HUNDRED_FACES_HASSAN_HEAD.get()
                : slot == net.minecraft.world.entity.EquipmentSlot.CHEST ? SERVANT_CARD_HUNDRED_FACES_HASSAN_CHEST.get()
                : slot == net.minecraft.world.entity.EquipmentSlot.LEGS ? SERVANT_CARD_HUNDRED_FACES_HASSAN_LEGS.get() : Items.AIR;
            case "arash" -> (legs ? SERVANT_CARD_ARASH_LEGS : SERVANT_CARD_ARASH_CHEST).get();
            case "nightingale" -> (legs ? SERVANT_CARD_NIGHTINGALE_LEGS : SERVANT_CARD_NIGHTINGALE_CHEST).get();
            case "zhao_yun_rider" -> (head ? SERVANT_CARD_ZHAO_YUN_RIDER_HEAD : legs ? SERVANT_CARD_ZHAO_YUN_RIDER_LEGS : SERVANT_CARD_ZHAO_YUN_RIDER_CHEST).get();
            case "senko_muramasa" -> (legs ? SERVANT_CARD_SENKO_MURAMASA_LEGS : SERVANT_CARD_SENKO_MURAMASA_CHEST).get();
            default -> (legs ? SERVANT_CARD_EMIYA_ARCHER_LEGS : SERVANT_CARD_EMIYA_ARCHER_CHEST).get();
        };
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
