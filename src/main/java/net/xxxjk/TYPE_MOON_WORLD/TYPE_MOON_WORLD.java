package net.xxxjk.TYPE_MOON_WORLD;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file.
//此处的值应与 META-INF/neoforge.mods.toml 文件中的条目匹配。
@Mod(TYPE_MOON_WORLD.MOD_ID)
public class TYPE_MOON_WORLD {
    public static final String MOD_ID = "typemoonworld";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TYPE_MOON_WORLD(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for mod loading
        //注册 commonSetup 方法用于 mod 加载。
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        //注册我们感兴趣的服务器和其他游戏活动。

        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        //请注意，当且仅当我们希望*此*类（ExampleMod）直接响应事件时，这才是必要的。

        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        //如果此类中没有 @SubscribeEvent 注释的函数（如下面的 onServerStarting()），请不要添加此行。
        NeoForge.EVENT_BUS.register(this);

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
