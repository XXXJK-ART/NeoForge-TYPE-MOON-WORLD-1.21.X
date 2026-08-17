package com.example.typemoonaddon.registry;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AddonFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TypeMoonAddon.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, TypeMoonAddon.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> BLACK_MUD_TYPE = FLUID_TYPES.register(
            "black_mud",
            () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.typemoonworld.black_mud")
                            .motionScale(0.0D)
                            .canPushEntity(false)
                            .canSwim(false)
                            .canDrown(false)
                            .fallDistanceModifier(0.0F)
                            .density(3000)
                            .viscosity(6000)
                            .rarity(Rarity.EPIC)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
            )
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> BLACK_MUD = FLUIDS.register(
            "black_mud",
            () -> new BaseFlowingFluid.Source(properties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_BLACK_MUD = FLUIDS.register(
            "flowing_black_mud",
            () -> new BaseFlowingFluid.Flowing(properties())
    );

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(BLACK_MUD_TYPE, BLACK_MUD, FLOWING_BLACK_MUD)
                .block(AddonBlocks.BLACK_MUD)
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(30)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    private AddonFluids() {
    }
}
