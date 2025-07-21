package net.xxxjk.TYPE_MOON_WORLD;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.xxxjk.TYPE_MOON_WORLD.block.ModBlocks;
import net.xxxjk.TYPE_MOON_WORLD.init.ModCreativeModeTabs;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// The value here should match an entry in the META-INF/neoforge.mods.toml file.
//此处的值应与 META-INF/neoforge.mods.toml 文件中的条目匹配。
@Mod(TYPE_MOON_WORLD.MOD_ID)
public class TYPE_MOON_WORLD {
    public static final String MOD_ID = "typemoonworld";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TYPE_MOON_WORLD(IEventBus modEventBus, ModContainer modContainer) {
        // Register the registerNetworking method for mod loading
        //注册 registerNetworking 方法用于 mod 加载。
        modEventBus.addListener(this::registerNetworking);
        TypeMoonWorldModVariables.ATTACHMENT_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        //注册我们感兴趣的服务器和其他游戏活动。

        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        //请注意，当且仅当我们希望*此*类（ExampleMod）直接响应事件时，这才是必要的。

        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //如果此类中没有 @SubscribeEvent 注释的函数（如下面的 onServerStarting()），请不要添加此行。
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);//添加创造页

        ModItems.register(modEventBus);//添加物品
        ModBlocks.register(modEventBus);//添加方块

        // Register the item to a creative tab
        //将物品注册到创意标签。
        modEventBus.addListener(this::addCreative);
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        //注册我们的 mod 的 ModConfigSpec，以便 FML 可以为我们创建并加载配置文件。
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

        private void commonSetup(final FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    //将示例块项目添加到构建块选项卡。
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // 用户代码块修改方法开始(以下都是mcr代码模块)
    // 用户代码块修改方法结束
    private static boolean networkingRegistered = false;
    private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

    private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
    }

    public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
        if (networkingRegistered)
            throw new IllegalStateException("Cannot register new network messages after networking has been registered");
        MESSAGES.put(id, new NetworkMessage<>(reader, handler));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MOD_ID);
        MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(),
                ((NetworkMessage) networkMessage).handler()));
        networkingRegistered = true;
    }

    private static final Collection<Tuple<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(new Tuple<>(action, tick));
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        List<Tuple<Runnable, Integer>> actions = new ArrayList<>();
        workQueue.forEach(work -> {
            work.setB(work.getB() - 1);
            if (work.getB() == 0)
                actions.add(work);
        });
        actions.forEach(e -> e.getA().run());
        workQueue.removeAll(actions);
    }
    //以上都是mcr代码模块存放区域

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    //您可以使用 SubscribeEvent 并让事件总线发现要调用的方法。
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    //您可以使用 EventBusSubscriber 自动注册带有 @SubscribeEvent 注释的类中的所有静态方法。
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
