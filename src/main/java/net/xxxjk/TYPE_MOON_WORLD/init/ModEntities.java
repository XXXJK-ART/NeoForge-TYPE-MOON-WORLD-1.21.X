package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;

@SuppressWarnings("null")
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RubyProjectileEntity>> RUBY_PROJECTILE =
            ENTITY_TYPES.register("ruby_projectile", () -> EntityType.Builder.<RubyProjectileEntity>of(RubyProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("ruby_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SapphireProjectileEntity>> SAPPHIRE_PROJECTILE =
            ENTITY_TYPES.register("sapphire_projectile", () -> EntityType.Builder.<SapphireProjectileEntity>of(SapphireProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("sapphire_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TopazProjectileEntity>> TOPAZ_PROJECTILE =
            ENTITY_TYPES.register("topaz_projectile", () -> EntityType.Builder.<TopazProjectileEntity>of(TopazProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("topaz_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
