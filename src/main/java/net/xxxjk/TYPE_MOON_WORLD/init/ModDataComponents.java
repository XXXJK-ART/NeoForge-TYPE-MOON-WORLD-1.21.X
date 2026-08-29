package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
   public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "typemoonworld");
   public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> RUNE_INSCRIPTION = COMPONENTS.register(
      "rune_inscription", () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC).build()
   );
   private ModDataComponents() { }
   public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
