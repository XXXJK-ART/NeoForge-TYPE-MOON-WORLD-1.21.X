package net.xxxjk.TYPE_MOON_WORLD;

import com.mojang.logging.LogUtils;
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
import net.xxxjk.TYPE_MOON_WORLD.init.ModSounds;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModMenus;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.magic.registry.MagicModularRegistry;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.Basic_information_gui_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.CastMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.CycleMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.DeleteProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GemCarvingEngraveMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.GemGravitySelfCastMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.ImplantMagicCrestMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Lose_health_regain_mana_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicCircuitSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicModeSwitchMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.MagicWheelSlotEditMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.Magical_attributes_Button_Message;
import net.xxxjk.TYPE_MOON_WORLD.network.MysticEyesToggleMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenLeylineSurveyMapMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.OpenProjectionGuiMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.PageChangeMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SaveStructuralSelectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionItemMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SelectProjectionStructureMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.StartStructureProjectionMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicIndexMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.SwitchMagicWheelMessage;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.world.gem.GemRegion;
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
   private static volatile long serverTickCounter = 0L;

   public TYPE_MOON_WORLD(IEventBus modEventBus, ModContainer modContainer) {
      modEventBus.addListener(this::registerNetworking);
      TypeMoonWorldModVariables.ATTACHMENT_TYPES.register(modEventBus);
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.addListener(this::registerCommands);
      ModCreativeModeTabs.register(modEventBus);
      ModItems.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModMobEffects.register(modEventBus);
      ModSounds.register(modEventBus);
      ModLootModifiers.register(modEventBus);
      ModBiomes.register(modEventBus);
      TypeMoonWorldModMenus.REGISTRY.register(modEventBus);
      modEventBus.addListener(this::addCreative);
      modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, Config.SPEC);
      modEventBus.addListener(this::commonSetup);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(
         () -> {
            MagicModularRegistry.ensureInitialized();
            Regions.register(new GemRegion(ResourceLocation.fromNamespaceAndPath("typemoonworld", "gem_region"), 2));
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
   }

   public static <T extends CustomPacketPayload> void addNetworkMessage(
      Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler
   ) {
      if (networkingRegistered) {
         throw new IllegalStateException("Cannot register new network messages after networking has been registered");
      } else {
         MESSAGES.put(id, new TYPE_MOON_WORLD.NetworkMessage(reader, handler));
      }
   }

   private void registerNetworking(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("typemoonworld");
      registrar.playToServer(Basic_information_Button_Message.TYPE, Basic_information_Button_Message.STREAM_CODEC, Basic_information_Button_Message::handleData);
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
      registrar.playToServer(MagicModeSwitchMessage.TYPE, MagicModeSwitchMessage.STREAM_CODEC, MagicModeSwitchMessage::handleData);
      registrar.playToServer(SwitchMagicMessage.TYPE, SwitchMagicMessage.STREAM_CODEC, SwitchMagicMessage::handleData);
      registrar.playToServer(SwitchMagicIndexMessage.TYPE, SwitchMagicIndexMessage.STREAM_CODEC, SwitchMagicIndexMessage::handleData);
      registrar.playToServer(SwitchMagicWheelMessage.TYPE, SwitchMagicWheelMessage.STREAM_CODEC, SwitchMagicWheelMessage::handleData);
      registrar.playToServer(MagicWheelSlotEditMessage.TYPE, MagicWheelSlotEditMessage.STREAM_CODEC, MagicWheelSlotEditMessage::handleData);
      registrar.playToServer(ImplantMagicCrestMessage.TYPE, ImplantMagicCrestMessage.STREAM_CODEC, ImplantMagicCrestMessage::handleData);
      registrar.playToServer(SaveStructuralSelectionMessage.TYPE, SaveStructuralSelectionMessage.STREAM_CODEC, SaveStructuralSelectionMessage::handleData);
      registrar.playToServer(SelectProjectionStructureMessage.TYPE, SelectProjectionStructureMessage.STREAM_CODEC, SelectProjectionStructureMessage::handleData);
      registrar.playToServer(DeleteProjectionStructureMessage.TYPE, DeleteProjectionStructureMessage.STREAM_CODEC, DeleteProjectionStructureMessage::handleData);
      registrar.playToServer(StartStructureProjectionMessage.TYPE, StartStructureProjectionMessage.STREAM_CODEC, StartStructureProjectionMessage::handleData);
      registrar.playToServer(GemCarvingEngraveMessage.TYPE, GemCarvingEngraveMessage.STREAM_CODEC, GemCarvingEngraveMessage::handleData);
      registrar.playToServer(GemGravitySelfCastMessage.TYPE, GemGravitySelfCastMessage.STREAM_CODEC, GemGravitySelfCastMessage::handleData);
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
         while ((action = due.poll()) != null) {
            try {
               action.run();
            } catch (Exception var5) {
               LOGGER.error("Error while running scheduled server work", var5);
            }
         }
      }
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      scheduledWork.clear();
      serverTickCounter = 0L;
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
      scheduledWork.clear();
      serverTickCounter = 0L;
   }

   private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
   }
}
