package net.xxxjk.TYPE_MOON_WORLD;

import com.mojang.logging.LogUtils;
import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.AddonNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.block.entity.ModBlockEntities;
import net.xxxjk.TYPE_MOON_WORLD.command.TypeMoonCommands;
import net.xxxjk.TYPE_MOON_WORLD.init.ModBiomes;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.init.ModLootModifiers;
import net.xxxjk.TYPE_MOON_WORLD.init.ModMobEffects;
import net.xxxjk.TYPE_MOON_WORLD.init.ModParticles;
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.init.ModVillagers;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantDefinitionLoader;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.InternalApiProvider;
import net.xxxjk.TYPE_MOON_WORLD.api.ExtensionApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.EffectsApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.CardActionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicDefinitionRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.MagicPresetRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.ClientExtensionRegistryImpl;
import net.xxxjk.TYPE_MOON_WORLD.api.GemApiRegistry;
import net.xxxjk.TYPE_MOON_WORLD.api.MasterProfileApiRegistry;
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.BajiquanInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BajiquanPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GanryuInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GanryuPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HakuryuRideMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.KendoInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CircleRealmStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BodyTrainingPointMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_gui_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.CastMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ClairvoyanceStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CycleMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.DeleteProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.DeleteProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.DuelScreenFlashMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduDetectionHighlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduTransfigurationPointMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.EnkiduTransfigurationSetMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.FirearmPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenGilgameshVaultScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GilgameshVaultSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GemCarvingEngraveMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicResearchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCopyMessage;
import net.xxxjk.TYPE_MOON_WORLD.magic.MagicAnalysisService;
import net.xxxjk.TYPE_MOON_WORLD.network.GemGravitySelfCastMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ImplantMagicCrestMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCircuitSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ManaBurstInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Magical_attributes_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellPoseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterCommandSpellMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MasterVisualStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenServantCommandScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MedeaCraftSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MuramasaForgeSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenLeylineSurveyMapMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenMedeaCraftScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenMuramasaForgeScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenEnkiduTransfigurationScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenParacelsusCraftScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenParacelsusElementScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenProjectionGuiMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PageChangeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ParacelsusCraftSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ParacelsusElementSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SaveStructuralSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardBasicAttackMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardFlightMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SetTimeAlterMultiplierMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TimeAlterVisualStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardHoldActionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardJumpMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantCardReleaseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSelectMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderSpawnModeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderPossessionInputMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PaleRiderStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseRequestMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.BaobhanSithCurseTriggerMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesOpenScreenMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesSummonMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesCommandMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.HundredFacesStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ServantMasterContractMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.StartStructureProjectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicIndexMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ThompsonContenderUseMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TerrainDebrisMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CustomCommandSpellMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ConcealmentStateMessage;
import net.xxxjk.TYPE_MOON_WORLD.chain.network.ChainInputPayload;
import net.xxxjk.TYPE_MOON_WORLD.vfx.network.VFXSpawnEffectMessage;
import net.xxxjk.TYPE_MOON_WORLD.gametest.TypeMoonWorldGameTests;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.xxxjk.TYPE_MOON_WORLD.world.gem.GemRegion;
import net.xxxjk.TYPE_MOON_WORLD.world.city.CityRegion;
import org.slf4j.Logger;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.SurfaceRuleManager.RuleCategory;

@Mod("typemoonworld")
public class TYPE_MOON_WORLD {
   public static final String MOD_ID = "typemoonworld";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static boolean networkingRegistered = false;
   private static final Map<Type<?>, TYPE_MOON_WORLD.NetworkMessage<?>> MESSAGES = new HashMap<>();
   private static final Map<Long, Queue<Runnable>> scheduledWork = new ConcurrentHashMap<>();
   private static final int MAX_SCHEDULED_WORK_PER_TICK = 128;
   private static volatile long serverTickCounter = 0L;

   public TYPE_MOON_WORLD(IEventBus modEventBus, ModContainer modContainer) {
      TypeMoonWorldApi.install(new InternalApiProvider());
      modEventBus.addListener(this::registerNetworking);
      TypeMoonWorldModVariables.ATTACHMENT_TYPES.register(modEventBus);
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.register(MagicAnalysisService.class);
      NeoForge.EVENT_BUS.addListener(this::registerCommands);
      ModCreativeModeTabs.register(modEventBus);
      ModItems.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModMobEffects.register(modEventBus);
      ModParticles.register(modEventBus);
      ModSounds.register(modEventBus);
      ModVillagers.register(modEventBus);
      new TypeMoonAddon(modEventBus, modContainer);
      ModLootModifiers.register(modEventBus);
      ModBiomes.register(modEventBus);
      TypeMoonWorldModMenus.REGISTRY.register(modEventBus);
      modEventBus.addListener(this::addCreative);
      modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);
      modEventBus.addListener(this::commonSetup);
      modEventBus.addListener((RegisterGameTestsEvent event) -> event.register(TypeMoonWorldGameTests.class));
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(
         () -> {
            MagicModularRegistry.ensureInitialized();
            MagicDefinitionRegistry.bootstrapBuiltins();
            ServantSkillRegistry.ensureInitialized();
            ServantAddonRegistry.ensureInitialized();
            MagicModularRegistry.freeze();
            ServantSkillRegistry.freeze();
            ServantAddonRegistry.freeze();
            CardActionRegistry.freeze();
            MagicDefinitionRegistry.freeze();
            MagicPresetRegistry.freeze();
            GemApiRegistry.freeze();
            MasterProfileApiRegistry.freeze();
            ExtensionApiRegistry.freeze();
            EffectsApiRegistry.freeze();
            Regions.register(new GemRegion(ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_region"), 2));
            Regions.register(new CityRegion(ResourceLocation.fromNamespaceAndPath("typemoonworld", "city_region"), 1));
            ResourceKey<Biome> gemBiome = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_biome"));
            SurfaceRuleManager.addSurfaceRules(
               RuleCategory.OVERWORLD,
               "typemoonworld",
               SurfaceRules.ifTrue(
                  SurfaceRules.isBiome(new ResourceKey[]{gemBiome}),
                  SurfaceRules.sequence(
                     new RuleSource[]{
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(Blocks.STONE.defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.STONE.defaultBlockState()))
                     }
                  )
               )
            );
         }
      );
   }

   private void registerCommands(RegisterCommandsEvent event) {
      TypeMoonCommands.register(event.getDispatcher());
   }

   private void addCreative(BuildCreativeModeTabContentsEvent event) {
      if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
         event.remove(ModItems.GILGAMESH_SPAWN_EGG.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
         event.remove(ModItems.GILGAMESH_CASTER_SPAWN_EGG.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
      }
   }

   public static <T extends CustomPacketPayload> void addNetworkMessage(
      Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler
   ) {
      if (networkingRegistered) {
         throw new IllegalStateException("Cannot register new network messages after networking has been registered");
      } else {
         MESSAGES.put(id, new TYPE_MOON_WORLD.NetworkMessage<>(reader, handler));
      }
   }

   public static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
   }

   private void registerNetworking(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("typemoonworld");
      // Integrated addon payloads must share this registrar. A second registrar
      // creates a separate protocol channel and leaves mock/server connections
      // unable to send the payload despite successful class loading.
      AddonNetwork.registerPayloads(registrar);
      registrar.playToServer(Basic_information_Button_Message.TYPE, Basic_information_Button_Message.STREAM_CODEC, Basic_information_Button_Message::handleData);
      registrar.playToServer(BajiquanInputMessage.TYPE, BajiquanInputMessage.STREAM_CODEC, BajiquanInputMessage::handleData);
      registrar.playToServer(GanryuInputMessage.TYPE, GanryuInputMessage.STREAM_CODEC, GanryuInputMessage::handleData);
      registrar.playToServer(HakuryuRideMessage.TYPE, HakuryuRideMessage.STREAM_CODEC, HakuryuRideMessage::handleData);
      registrar.playToServer(KendoInputMessage.TYPE, KendoInputMessage.STREAM_CODEC, KendoInputMessage::handleData);
      registrar.playToServer(BodyTrainingPointMessage.TYPE, BodyTrainingPointMessage.STREAM_CODEC, BodyTrainingPointMessage::handleData);
      registrar.playToServer(Basic_information_gui_Message.TYPE, Basic_information_gui_Message.STREAM_CODEC, Basic_information_gui_Message::handleData);
      registrar.playToServer(Lose_health_regain_mana_Message.TYPE, Lose_health_regain_mana_Message.STREAM_CODEC, Lose_health_regain_mana_Message::handleData);
      registrar.playToServer(
         Magical_attributes_Button_Message.TYPE, Magical_attributes_Button_Message.STREAM_CODEC, Magical_attributes_Button_Message::handleData
      );
      registrar.playToServer(MagicCircuitSwitchMessage.TYPE, MagicCircuitSwitchMessage.STREAM_CODEC, MagicCircuitSwitchMessage::handleData);
      registrar.playToServer(CastMagicMessage.TYPE, CastMagicMessage.STREAM_CODEC, CastMagicMessage::handleData);
      registrar.playToServer(SelectMagicMessage.TYPE, SelectMagicMessage.STREAM_CODEC, SelectMagicMessage::handleData);
      registrar.playToServer(CycleMagicMessage.TYPE, CycleMagicMessage.STREAM_CODEC, CycleMagicMessage::handleData);
      registrar.playToServer(SelectProjectionItemMessage.TYPE, SelectProjectionItemMessage.STREAM_CODEC, SelectProjectionItemMessage::handleData);
      registrar.playToServer(OpenProjectionGuiMessage.TYPE, OpenProjectionGuiMessage.STREAM_CODEC, OpenProjectionGuiMessage::handleData);
      registrar.playToServer(MysticEyesToggleMessage.TYPE, MysticEyesToggleMessage.STREAM_CODEC, MysticEyesToggleMessage::handleData);
      registrar.playToServer(PageChangeMessage.TYPE, PageChangeMessage.STREAM_CODEC, PageChangeMessage::handleData);
      registrar.playToServer(ManaBurstInputMessage.TYPE, ManaBurstInputMessage.STREAM_CODEC, ManaBurstInputMessage::handleData);
      registrar.playToServer(MagicModeSwitchMessage.TYPE, MagicModeSwitchMessage.STREAM_CODEC, MagicModeSwitchMessage::handleData);
      registrar.playToServer(SwitchMagicMessage.TYPE, SwitchMagicMessage.STREAM_CODEC, SwitchMagicMessage::handleData);
      registrar.playToServer(SwitchMagicIndexMessage.TYPE, SwitchMagicIndexMessage.STREAM_CODEC, SwitchMagicIndexMessage::handleData);
      registrar.playToServer(SwitchMagicWheelMessage.TYPE, SwitchMagicWheelMessage.STREAM_CODEC, SwitchMagicWheelMessage::handleData);
      registrar.playToServer(MagicWheelSlotEditMessage.TYPE, MagicWheelSlotEditMessage.STREAM_CODEC, MagicWheelSlotEditMessage::handleData);
      registrar.playToServer(ImplantMagicCrestMessage.TYPE, ImplantMagicCrestMessage.STREAM_CODEC, ImplantMagicCrestMessage::handleData);
      registrar.playToServer(SaveStructuralSelectionMessage.TYPE, SaveStructuralSelectionMessage.STREAM_CODEC, SaveStructuralSelectionMessage::handleData);
      registrar.playToServer(SelectProjectionStructureMessage.TYPE, SelectProjectionStructureMessage.STREAM_CODEC, SelectProjectionStructureMessage::handleData);
      registrar.playToServer(DeleteProjectionStructureMessage.TYPE, DeleteProjectionStructureMessage.STREAM_CODEC, DeleteProjectionStructureMessage::handleData);
      registrar.playToServer(DeleteProjectionItemMessage.TYPE, DeleteProjectionItemMessage.STREAM_CODEC, DeleteProjectionItemMessage::handleData);
      registrar.playToServer(StartStructureProjectionMessage.TYPE, StartStructureProjectionMessage.STREAM_CODEC, StartStructureProjectionMessage::handleData);
      registrar.playToServer(GemCarvingEngraveMessage.TYPE, GemCarvingEngraveMessage.STREAM_CODEC, GemCarvingEngraveMessage::handleData);
      registrar.playToServer(MagicResearchMessage.TYPE, MagicResearchMessage.STREAM_CODEC, MagicResearchMessage::handleData);
      registrar.playToServer(MagicCopyMessage.TYPE, MagicCopyMessage.STREAM_CODEC, MagicCopyMessage::handleData);
      registrar.playToServer(ChainInputPayload.TYPE, ChainInputPayload.STREAM_CODEC, ChainInputPayload::handleData);
      registrar.playToServer(GemGravitySelfCastMessage.TYPE, GemGravitySelfCastMessage.STREAM_CODEC, GemGravitySelfCastMessage::handleData);
      registrar.playToServer(GilgameshVaultSelectionMessage.TYPE, GilgameshVaultSelectionMessage.STREAM_CODEC, GilgameshVaultSelectionMessage::handleData);
      registrar.playToServer(ServantCardActionMessage.TYPE, ServantCardActionMessage.STREAM_CODEC, ServantCardActionMessage::handleData);
      registrar.playToServer(ServantCardBasicAttackMessage.TYPE, ServantCardBasicAttackMessage.STREAM_CODEC, ServantCardBasicAttackMessage::handleData);
      registrar.playToServer(ServantCardFlightMessage.TYPE, ServantCardFlightMessage.STREAM_CODEC, ServantCardFlightMessage::handleData);
      registrar.playToServer(SetTimeAlterMultiplierMessage.TYPE, SetTimeAlterMultiplierMessage.STREAM_CODEC, SetTimeAlterMultiplierMessage::handleData);
      registrar.playToClient(TimeAlterVisualStateMessage.TYPE, TimeAlterVisualStateMessage.STREAM_CODEC, TimeAlterVisualStateMessage::handleData);
      registrar.playToClient(ClairvoyanceStateMessage.TYPE, ClairvoyanceStateMessage.STREAM_CODEC, ClairvoyanceStateMessage::handleData);
      registrar.playToServer(ServantCardHoldActionMessage.TYPE, ServantCardHoldActionMessage.STREAM_CODEC, ServantCardHoldActionMessage::handleData);
      registrar.playToServer(ServantCardJumpMessage.TYPE, ServantCardJumpMessage.STREAM_CODEC, ServantCardJumpMessage::handleData);
      registrar.playToServer(ServantCardReleaseMessage.TYPE, ServantCardReleaseMessage.STREAM_CODEC, ServantCardReleaseMessage::handleData);
      registrar.playToServer(PaleRiderSelectMessage.TYPE, PaleRiderSelectMessage.STREAM_CODEC, PaleRiderSelectMessage::handleData);
      registrar.playToServer(PaleRiderCommandMessage.TYPE, PaleRiderCommandMessage.STREAM_CODEC, PaleRiderCommandMessage::handleData);
      registrar.playToServer(PaleRiderSpawnModeMessage.TYPE, PaleRiderSpawnModeMessage.STREAM_CODEC, PaleRiderSpawnModeMessage::handleData);
      registrar.playToClient(PaleRiderOpenScreenMessage.TYPE, PaleRiderOpenScreenMessage.STREAM_CODEC, PaleRiderOpenScreenMessage::handleData);
      registrar.playToServer(PaleRiderPossessionInputMessage.TYPE, PaleRiderPossessionInputMessage.STREAM_CODEC, PaleRiderPossessionInputMessage::handleData);
      registrar.playToClient(PaleRiderStateMessage.TYPE, PaleRiderStateMessage.STREAM_CODEC, PaleRiderStateMessage::handleData);
      registrar.playToClient(HundredFacesOpenScreenMessage.TYPE, HundredFacesOpenScreenMessage.STREAM_CODEC, HundredFacesOpenScreenMessage::handleData);
      registrar.playToServer(HundredFacesSummonMessage.TYPE, HundredFacesSummonMessage.STREAM_CODEC, HundredFacesSummonMessage::handleData);
      registrar.playToServer(HundredFacesCommandMessage.TYPE, HundredFacesCommandMessage.STREAM_CODEC, HundredFacesCommandMessage::handleData);
      registrar.playToServer(HundredFacesSwitchMessage.TYPE, HundredFacesSwitchMessage.STREAM_CODEC, HundredFacesSwitchMessage::handleData);
      registrar.playToClient(HundredFacesStateMessage.TYPE, HundredFacesStateMessage.STREAM_CODEC, HundredFacesStateMessage::handleData);
      registrar.playToClient(BaobhanSithCurseOpenScreenMessage.TYPE, BaobhanSithCurseOpenScreenMessage.STREAM_CODEC, BaobhanSithCurseOpenScreenMessage::handleData);
      registrar.playToServer(BaobhanSithCurseRequestMessage.TYPE, BaobhanSithCurseRequestMessage.STREAM_CODEC, BaobhanSithCurseRequestMessage::handleData);
      registrar.playToServer(BaobhanSithCurseTriggerMessage.TYPE, BaobhanSithCurseTriggerMessage.STREAM_CODEC, BaobhanSithCurseTriggerMessage::handleData);
      registrar.playToServer(ServantMasterContractMessage.TYPE, ServantMasterContractMessage.STREAM_CODEC, ServantMasterContractMessage::handleData);
      registrar.playToServer(MasterCommandSpellMessage.TYPE, MasterCommandSpellMessage.STREAM_CODEC, MasterCommandSpellMessage::handleData);
      registrar.playToServer(MasterCommandSpellPoseMessage.TYPE, MasterCommandSpellPoseMessage.STREAM_CODEC, MasterCommandSpellPoseMessage::handleData);
      registrar.playToServer(ServantCommandMessage.TYPE, ServantCommandMessage.STREAM_CODEC, ServantCommandMessage::handleData);
      registrar.playToClient(OpenServantCommandScreenMessage.TYPE, OpenServantCommandScreenMessage.STREAM_CODEC, OpenServantCommandScreenMessage::handleData);
      registrar.playToServer(CustomCommandSpellMessage.TYPE, CustomCommandSpellMessage.STREAM_CODEC, CustomCommandSpellMessage::handleData);
      registrar.playToServer(EnkiduTransfigurationPointMessage.TYPE, EnkiduTransfigurationPointMessage.STREAM_CODEC, EnkiduTransfigurationPointMessage::handleData);
      registrar.playToServer(EnkiduTransfigurationSetMessage.TYPE, EnkiduTransfigurationSetMessage.STREAM_CODEC, EnkiduTransfigurationSetMessage::handleData);
      registrar.playToServer(MedeaCraftSelectionMessage.TYPE, MedeaCraftSelectionMessage.STREAM_CODEC, MedeaCraftSelectionMessage::handleData);
      registrar.playToServer(MuramasaForgeSelectionMessage.TYPE, MuramasaForgeSelectionMessage.STREAM_CODEC, MuramasaForgeSelectionMessage::handleData);
      registrar.playToServer(ParacelsusCraftSelectionMessage.TYPE, ParacelsusCraftSelectionMessage.STREAM_CODEC, ParacelsusCraftSelectionMessage::handleData);
      registrar.playToServer(ParacelsusElementSelectionMessage.TYPE, ParacelsusElementSelectionMessage.STREAM_CODEC, ParacelsusElementSelectionMessage::handleData);
      registrar.playToServer(ThompsonContenderUseMessage.TYPE, ThompsonContenderUseMessage.STREAM_CODEC, ThompsonContenderUseMessage::handleData);
      registrar.playToClient(OpenMedeaCraftScreenMessage.TYPE, OpenMedeaCraftScreenMessage.STREAM_CODEC, OpenMedeaCraftScreenMessage::handleData);
      registrar.playToClient(OpenMuramasaForgeScreenMessage.TYPE, OpenMuramasaForgeScreenMessage.STREAM_CODEC, OpenMuramasaForgeScreenMessage::handleData);
      registrar.playToClient(OpenEnkiduTransfigurationScreenMessage.TYPE, OpenEnkiduTransfigurationScreenMessage.STREAM_CODEC, OpenEnkiduTransfigurationScreenMessage::handleData);
      registrar.playToClient(OpenGilgameshVaultScreenMessage.TYPE, OpenGilgameshVaultScreenMessage.STREAM_CODEC, OpenGilgameshVaultScreenMessage::handleData);
      registrar.playToClient(OpenParacelsusCraftScreenMessage.TYPE, OpenParacelsusCraftScreenMessage.STREAM_CODEC, OpenParacelsusCraftScreenMessage::handleData);
      registrar.playToClient(OpenParacelsusElementScreenMessage.TYPE, OpenParacelsusElementScreenMessage.STREAM_CODEC, OpenParacelsusElementScreenMessage::handleData);
      registrar.playToClient(MasterVisualStateMessage.TYPE, MasterVisualStateMessage.STREAM_CODEC, MasterVisualStateMessage::handleData);
      registrar.playToClient(ConcealmentStateMessage.TYPE, ConcealmentStateMessage.STREAM_CODEC, ConcealmentStateMessage::handleData);
      registrar.playToClient(FirearmPoseMessage.TYPE, FirearmPoseMessage.STREAM_CODEC, FirearmPoseMessage::handleData);
      registrar.playToClient(BajiquanPoseMessage.TYPE, BajiquanPoseMessage.STREAM_CODEC, BajiquanPoseMessage::handleData);
      registrar.playToClient(GanryuPoseMessage.TYPE, GanryuPoseMessage.STREAM_CODEC, GanryuPoseMessage::handleData);
      registrar.playToClient(CircleRealmStateMessage.TYPE, CircleRealmStateMessage.STREAM_CODEC, CircleRealmStateMessage::handleData);
      registrar.playToClient(EnkiduDetectionHighlightMessage.TYPE, EnkiduDetectionHighlightMessage.STREAM_CODEC, EnkiduDetectionHighlightMessage::handleData);
      registrar.playToClient(DuelScreenFlashMessage.TYPE, DuelScreenFlashMessage.STREAM_CODEC, DuelScreenFlashMessage::handleData);
      registrar.playToClient(
         TypeMoonWorldModVariables.PlayerVariablesSyncMessage.TYPE,
         TypeMoonWorldModVariables.PlayerVariablesSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.PlayerVariablesSyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.RuntimeSelectionSyncMessage.TYPE,
         TypeMoonWorldModVariables.RuntimeSelectionSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.RuntimeSelectionSyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.ModeStateSyncMessage.TYPE,
         TypeMoonWorldModVariables.ModeStateSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.ModeStateSyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.ManaSyncMessage.TYPE,
         TypeMoonWorldModVariables.ManaSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.ManaSyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.ServantCardRuntimeSyncMessage.TYPE,
         TypeMoonWorldModVariables.ServantCardRuntimeSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.ServantCardRuntimeSyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.ProficiencySyncMessage.TYPE,
         TypeMoonWorldModVariables.ProficiencySyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.ProficiencySyncMessage::handleData
      );
      registrar.playToClient(
         TypeMoonWorldModVariables.ProjectionDeltaSyncMessage.TYPE,
         TypeMoonWorldModVariables.ProjectionDeltaSyncMessage.STREAM_CODEC,
         TypeMoonWorldModVariables.ProjectionDeltaSyncMessage::handleData
      );
      registrar.playToClient(OpenLeylineSurveyMapMessage.TYPE, OpenLeylineSurveyMapMessage.STREAM_CODEC, OpenLeylineSurveyMapMessage::handleData);
      registrar.playToClient(VFXSpawnEffectMessage.TYPE, VFXSpawnEffectMessage.STREAM_CODEC, VFXSpawnEffectMessage::handleData);
      registrar.playToClient(TerrainDebrisMessage.TYPE, TerrainDebrisMessage.STREAM_CODEC, TerrainDebrisMessage::handleData);
      registrar.playToClient(DefinitionSnapshotMessage.TYPE, DefinitionSnapshotMessage.STREAM_CODEC, DefinitionSnapshotMessage::handleData);
      networkingRegistered = true;
   }

   public static void queueServerWork(int tick, Runnable action) {
      if (action != null) {
         int delay = Math.max(1, tick);
         long executeAt = serverTickCounter + delay;
         scheduledWork.computeIfAbsent(executeAt, k -> new ConcurrentLinkedQueue<>()).add(action);
      }
   }

   @SubscribeEvent
   public void tick(Post event) {
      serverTickCounter++;
      Queue<Runnable> due = scheduledWork.remove(serverTickCounter);
      if (due != null) {
         Runnable action;
         int processed = 0;
         while (processed < MAX_SCHEDULED_WORK_PER_TICK && (action = due.poll()) != null) {
            processed++;
            try {
               action.run();
            } catch (Exception var5) {
               LOGGER.error("Error while running scheduled server work", var5);
            }
         }
         if (!due.isEmpty()) {
            scheduledWork.computeIfAbsent(serverTickCounter + 1L, k -> new ConcurrentLinkedQueue<>()).addAll(due);
         }
      }
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      scheduledWork.clear();
      serverTickCounter = 0L;
      net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotService.invalidate();
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
      scheduledWork.clear();
      serverTickCounter = 0L;
      net.xxxjk.TYPE_MOON_WORLD.network.DefinitionSnapshotService.invalidate();
   }

   private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
   }
}
