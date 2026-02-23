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
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MuramasaSlashProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;

@SuppressWarnings("null")
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TYPE_MOON_WORLD.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MuramasaSlashProjectileEntity>> MURAMASA_SLASH =
            ENTITY_TYPES.register("muramasa_slash", () -> EntityType.Builder.<MuramasaSlashProjectileEntity>of(MuramasaSlashProjectileEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(10).updateInterval(2).build("muramasa_slash"));

    public static final DeferredHolder<EntityType<?>, EntityType<RubyProjectileEntity>> RUBY_PROJECTILE =
            ENTITY_TYPES.register("ruby_projectile", () -> EntityType.Builder.<RubyProjectileEntity>of(RubyProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("ruby_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<SapphireProjectileEntity>> SAPPHIRE_PROJECTILE =
            ENTITY_TYPES.register("sapphire_projectile", () -> EntityType.Builder.<SapphireProjectileEntity>of(SapphireProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("sapphire_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<TopazProjectileEntity>> TOPAZ_PROJECTILE =
            ENTITY_TYPES.register("topaz_projectile", () -> EntityType.Builder.<TopazProjectileEntity>of(TopazProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).build("topaz_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<BrokenPhantasmProjectileEntity>> BROKEN_PHANTASM_PROJECTILE =
            ENTITY_TYPES.register("broken_phantasm_projectile", () -> EntityType.Builder.<BrokenPhantasmProjectileEntity>of(BrokenPhantasmProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("broken_phantasm_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<UBWProjectileEntity>> UBW_PROJECTILE =
            ENTITY_TYPES.register("ubw_projectile", () -> EntityType.Builder.<UBWProjectileEntity>of(UBWProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("ubw_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity>> RYOUGI_SHIKI =
            ENTITY_TYPES.register("ryougi_shiki", () -> EntityType.Builder.<net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity>of(net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f).build("ryougi_shiki"));

    public static final DeferredHolder<EntityType<?>, EntityType<MerlinEntity>> MERLIN =
            ENTITY_TYPES.register("merlin", () -> EntityType.Builder.<MerlinEntity>of(MerlinEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f).build("merlin"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity>> SWORD_BARREL_PROJECTILE =
            ENTITY_TYPES.register("sword_barrel_projectile", () -> EntityType.Builder.<net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity>of(net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("sword_barrel_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity>> CYAN_WIND_FIELD =
            ENTITY_TYPES.register("cyan_wind_field", () -> EntityType.Builder.<net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity>of(net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity::new, MobCategory.MISC)
                    .sized(4.0f, 2.0f).clientTrackingRange(10).updateInterval(2).build("cyan_wind_field"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
