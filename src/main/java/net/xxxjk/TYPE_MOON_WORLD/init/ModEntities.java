package net.xxxjk.TYPE_MOON_WORLD.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MuramasaSlashProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariLightColumnEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariWaveProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwChantRippleEntity;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "typemoonworld");
   public static final DeferredHolder<EntityType<?>, EntityType<MuramasaSlashProjectileEntity>> MURAMASA_SLASH = ENTITY_TYPES.register(
      "muramasa_slash",
      () -> Builder.<MuramasaSlashProjectileEntity>of(
            (entityType, level) -> new MuramasaSlashProjectileEntity((EntityType<? extends Projectile>)entityType, level), MobCategory.MISC
         )
         .sized(1.0F, 1.0F)
         .clientTrackingRange(10)
         .updateInterval(2)
         .build("muramasa_slash")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RubyProjectileEntity>> RUBY_PROJECTILE = ENTITY_TYPES.register(
      "ruby_projectile",
      () -> Builder.<RubyProjectileEntity>of(
            (entityType, level) -> new RubyProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .build("ruby_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SapphireProjectileEntity>> SAPPHIRE_PROJECTILE = ENTITY_TYPES.register(
      "sapphire_projectile",
      () -> Builder.<SapphireProjectileEntity>of(
            (entityType, level) -> new SapphireProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .build("sapphire_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TopazProjectileEntity>> TOPAZ_PROJECTILE = ENTITY_TYPES.register(
      "topaz_projectile",
      () -> Builder.<TopazProjectileEntity>of(
            (entityType, level) -> new TopazProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .build("topaz_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BrokenPhantasmProjectileEntity>> BROKEN_PHANTASM_PROJECTILE = ENTITY_TYPES.register(
      "broken_phantasm_projectile",
      () -> Builder.<BrokenPhantasmProjectileEntity>of(
            (entityType, level) -> new BrokenPhantasmProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(4)
         .updateInterval(10)
         .build("broken_phantasm_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<UBWProjectileEntity>> UBW_PROJECTILE = ENTITY_TYPES.register(
      "ubw_projectile",
      () -> Builder.<UBWProjectileEntity>of(
            (entityType, level) -> new UBWProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(4)
         .updateInterval(10)
         .build("ubw_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GanderProjectileEntity>> GANDER_PROJECTILE = ENTITY_TYPES.register(
      "gander_projectile",
      () -> Builder.<GanderProjectileEntity>of(
            (entityType, level) -> new GanderProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(8)
         .updateInterval(1)
         .build("gander_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RyougiShikiEntity>> RYOUGI_SHIKI = ENTITY_TYPES.register(
      "ryougi_shiki", () -> Builder.of(RyougiShikiEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("ryougi_shiki")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MerlinEntity>> MERLIN = ENTITY_TYPES.register(
      "merlin", () -> Builder.of(MerlinEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("merlin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<StoneManEntity>> STONE_MAN = ENTITY_TYPES.register(
      "stone_man", () -> Builder.of(StoneManEntity::new, MobCategory.CREATURE).sized(1.0F, 2.5F).build("stone_man")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MysticMagicianEntity>> MYSTIC_MAGICIAN = ENTITY_TYPES.register(
      "mystic_magician", () -> Builder.of(MysticMagicianEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("mystic_magician")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SwordBarrelProjectileEntity>> SWORD_BARREL_PROJECTILE = ENTITY_TYPES.register(
      "sword_barrel_projectile",
      () -> Builder.<SwordBarrelProjectileEntity>of(
            (entityType, level) -> new SwordBarrelProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(4)
         .updateInterval(10)
         .build("sword_barrel_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CyanWindFieldEntity>> CYAN_WIND_FIELD = ENTITY_TYPES.register(
      "cyan_wind_field",
      () -> Builder.<CyanWindFieldEntity>of((entityType, level) -> new CyanWindFieldEntity(entityType, level), MobCategory.MISC)
         .sized(4.0F, 2.0F)
         .clientTrackingRange(10)
         .updateInterval(2)
         .build("cyan_wind_field")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GravityShellEffectEntity>> GRAVITY_SHELL_EFFECT = ENTITY_TYPES.register(
      "gravity_shell_effect",
      () -> Builder.<GravityShellEffectEntity>of((entityType, level) -> new GravityShellEffectEntity(entityType, level), MobCategory.MISC)
         .sized(4.0F, 4.0F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("gravity_shell_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GravityFieldShellEffectEntity>> GRAVITY_FIELD_SHELL_EFFECT = ENTITY_TYPES.register(
      "gravity_field_shell_effect",
      () -> Builder.<GravityFieldShellEffectEntity>of((entityType, level) -> new GravityFieldShellEffectEntity(entityType, level), MobCategory.MISC)
         .sized(8.0F, 8.0F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("gravity_field_shell_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ExpandingRingEffectEntity>> EXPANDING_RING_EFFECT = ENTITY_TYPES.register(
      "expanding_ring_effect",
      () -> Builder.<ExpandingRingEffectEntity>of((entityType, level) -> new ExpandingRingEffectEntity(entityType, level), MobCategory.MISC)
         .sized(16.0F, 1.0F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("expanding_ring_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ProjectionCircuitEffectEntity>> PROJECTION_CIRCUIT_EFFECT = ENTITY_TYPES.register(
      "projection_circuit_effect",
      () -> Builder.<ProjectionCircuitEffectEntity>of((entityType, level) -> new ProjectionCircuitEffectEntity(entityType, level), MobCategory.MISC)
         .sized(8.0F, 1.0F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("projection_circuit_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<UbwChantRippleEntity>> UBW_CHANT_RIPPLE = ENTITY_TYPES.register(
      "ubw_chant_ripple",
      () -> Builder.<UbwChantRippleEntity>of((entityType, level) -> new UbwChantRippleEntity(entityType, level), MobCategory.MISC)
         .sized(48.0F, 1.0F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .build("ubw_chant_ripple")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TsumukariWaveProjectileEntity>> TSUMUKARI_WAVE_PROJECTILE = ENTITY_TYPES.register(
      "tsumukari_wave_projectile",
      () -> Builder.<TsumukariWaveProjectileEntity>of(
            (entityType, level) -> new TsumukariWaveProjectileEntity((EntityType<? extends Projectile>)entityType, level), MobCategory.MISC
         )
         .sized(2.5F, 2.5F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("tsumukari_wave_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TsumukariLightColumnEffectEntity>> TSUMUKARI_LIGHT_COLUMN_EFFECT = ENTITY_TYPES.register(
      "tsumukari_light_column_effect",
      () -> Builder.<TsumukariLightColumnEffectEntity>of((entityType, level) -> new TsumukariLightColumnEffectEntity(entityType, level), MobCategory.MISC)
         .sized(2.0F, 8.0F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("tsumukari_light_column_effect")
   );

   public static void register(IEventBus eventBus) {
      ENTITY_TYPES.register(eventBus);
   }
}
