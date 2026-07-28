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
import net.xxxjk.TYPE_MOON_WORLD.entity.BlackKeyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ChainsOfHeavenBindingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ContenderBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.NightingaleBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CyanWindFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DirkProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.DragonfangSoldierEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaArrowOrbProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashParticleArrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EmiyaThrownWeaponEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.EnkiduEarthWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ExpandingRingEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshCrossSlashEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshEaBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GilgameshGateWeaponProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityFieldShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GravityShellEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MagicBulletProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedusaPegasusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.HeraclesEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MerlinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaBeamEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MedeaMagicBoltEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MuramasaSlashProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BajiquanApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysteriousSwordsmanEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoApprenticeEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.KendoMasterEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RoninEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ShinsengumiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TohsakaRinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockBulletEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.OdaMatchlockGunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ProjectionCircuitEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RedSkeletonHajunEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RhoAiasEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.RyougiShikiEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SapphireProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.StoneManEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwSkyGearEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusSpiritCannonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArashEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GawainEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedeaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.UshiwakamaruRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SasakiKojiroEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.NightingaleEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ShadowHassanDeathShadowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.FanaticAssassinJinnEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GenericServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.RatSwarmEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderCrowEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.SoulEchoEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorsemanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ApocalypseHorseEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ConceptSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariLightColumnEffectEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TsumukariWaveProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.TopazProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWInterceptorSwordEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UBWProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.UbwChantRippleEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.VFXTriggerEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.GhoulEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.LivingDeadEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.NightKinEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.TheDeadEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.church.ChurchExecutorEntity;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "typemoonworld");
   public static final DeferredHolder<EntityType<?>, EntityType<VFXTriggerEntity>> VFX_TRIGGER = ENTITY_TYPES.register(
      "vfx_trigger",
      () -> Builder.<VFXTriggerEntity>of(VFXTriggerEntity::new, MobCategory.MISC)
         .sized(0.1F, 0.1F)
         .clientTrackingRange(192)
         .updateInterval(20)
         .build("vfx_trigger")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArashParticleArrowEntity>> ARASH_PARTICLE_ARROW = ENTITY_TYPES.register(
      "arash_particle_arrow",
      () -> Builder.<ArashParticleArrowEntity>of(
            (entityType, level) -> new ArashParticleArrowEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC)
         .sized(0.25F, 0.25F).clientTrackingRange(192).updateInterval(1).build("arash_particle_arrow")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArashStellaControllerEntity>> ARASH_STELLA_CONTROLLER = ENTITY_TYPES.register(
      "arash_stella_controller",
      () -> Builder.<ArashStellaControllerEntity>of(ArashStellaControllerEntity::new, MobCategory.MISC)
         .sized(1.0F, 1.0F).clientTrackingRange(256).updateInterval(1).build("arash_stella_controller")
   );
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
   public static final DeferredHolder<EntityType<?>, EntityType<UBWInterceptorSwordEntity>> UBW_INTERCEPTOR_SWORD = ENTITY_TYPES.register(
      "ubw_interceptor_sword",
      () -> Builder.<UBWInterceptorSwordEntity>of(
            (entityType, level) -> new UBWInterceptorSwordEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.3F, 0.3F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("ubw_interceptor_sword")
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
   public static final DeferredHolder<EntityType<?>, EntityType<BlackKeyProjectileEntity>> BLACK_KEY_PROJECTILE = ENTITY_TYPES.register(
      "black_key_projectile",
      () -> Builder.<BlackKeyProjectileEntity>of(BlackKeyProjectileEntity::new, MobCategory.MISC)
         .sized(0.22F, 0.22F).clientTrackingRange(12).updateInterval(1).build("black_key_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MagicBulletProjectileEntity>> MAGIC_BULLET_PROJECTILE = ENTITY_TYPES.register(
      "magic_bullet_projectile",
      () -> Builder.<MagicBulletProjectileEntity>of(
            (entityType, level) -> new MagicBulletProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.18F, 0.18F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("magic_bullet_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ElementalMagicProjectileEntity>> ELEMENTAL_MAGIC_PROJECTILE = ENTITY_TYPES.register(
      "elemental_magic_projectile",
      () -> Builder.<ElementalMagicProjectileEntity>of(
            (entityType, level) -> new ElementalMagicProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.22F, 0.22F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("elemental_magic_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ElementalMagicFieldEntity>> ELEMENTAL_MAGIC_FIELD = ENTITY_TYPES.register(
      "elemental_magic_field",
      () -> Builder.<ElementalMagicFieldEntity>of(ElementalMagicFieldEntity::new, MobCategory.MISC)
         .sized(0.2F, 0.2F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("elemental_magic_field")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MedeaMagicBoltEntity>> MEDEA_MAGIC_BOLT = ENTITY_TYPES.register(
      "medea_magic_bolt",
      () -> Builder.<MedeaMagicBoltEntity>of(
            (entityType, level) -> new MedeaMagicBoltEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("medea_magic_bolt")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MedeaBeamEffectEntity>> MEDEA_BEAM_EFFECT = ENTITY_TYPES.register(
      "medea_beam_effect",
      () -> Builder.<MedeaBeamEffectEntity>of((entityType, level) -> new MedeaBeamEffectEntity(entityType, level), MobCategory.MISC)
         .sized(8.0F, 2.0F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("medea_beam_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArtoriaExcaliburBeamEntity>> ARTORIA_EXCALIBUR_BEAM = ENTITY_TYPES.register(
      "artoria_excalibur_beam",
      () -> Builder.<ArtoriaExcaliburBeamEntity>of((entityType, level) -> new ArtoriaExcaliburBeamEntity(entityType, level), MobCategory.MISC)
         .sized(15.0F, 5.0F)
         .clientTrackingRange(64)
         .updateInterval(1)
         .build("artoria_excalibur_beam")
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
      "mystic_magician", () -> Builder.of(MysticMagicianEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("mystic_magician")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TheDeadEntity>> THE_DEAD = ENTITY_TYPES.register(
      "the_dead", () -> Builder.of(TheDeadEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("the_dead")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GhoulEntity>> GHOUL = ENTITY_TYPES.register(
      "ghoul", () -> Builder.of(GhoulEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("ghoul")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LivingDeadEntity>> LIVING_DEAD = ENTITY_TYPES.register(
      "living_dead", () -> Builder.of(LivingDeadEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("living_dead")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<NightKinEntity>> NIGHT_KIN = ENTITY_TYPES.register(
      "night_kin", () -> Builder.of(NightKinEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("night_kin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ChurchExecutorEntity>> CHURCH_EXECUTOR = ENTITY_TYPES.register(
      "church_executor", () -> Builder.of(ChurchExecutorEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("church_executor")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BajiquanMasterEntity>> BAJIQUAN_MASTER = ENTITY_TYPES.register(
      "bajiquan_master", () -> Builder.of(BajiquanMasterEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("bajiquan_master")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<BajiquanApprenticeEntity>> BAJIQUAN_APPRENTICE = ENTITY_TYPES.register(
      "bajiquan_apprentice", () -> Builder.of(BajiquanApprenticeEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("bajiquan_apprentice")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MysteriousSwordsmanEntity>> MYSTERIOUS_SWORDSMAN = ENTITY_TYPES.register(
      "mysterious_swordsman", () -> Builder.of(MysteriousSwordsmanEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("mysterious_swordsman")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<KendoMasterEntity>> KENDO_MASTER = ENTITY_TYPES.register(
      "kendo_master", () -> Builder.of(KendoMasterEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("kendo_master")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<KendoApprenticeEntity>> KENDO_APPRENTICE = ENTITY_TYPES.register(
      "kendo_apprentice", () -> Builder.of(KendoApprenticeEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("kendo_apprentice")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RoninEntity>> RONIN = ENTITY_TYPES.register(
      "ronin", () -> Builder.of(RoninEntity::new, MobCategory.MONSTER).sized(0.6F, 1.8F).build("ronin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ShinsengumiEntity>> SHINSENGUMI = ENTITY_TYPES.register(
      "shinsengumi", () -> Builder.of(ShinsengumiEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("shinsengumi")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<TohsakaRinEntity>> TOHSAKA_RIN = ENTITY_TYPES.register(
      "tohsaka_rin", () -> Builder.of(TohsakaRinEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("tohsaka_rin")
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
         .updateInterval(20)
         .build("expanding_ring_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ProjectionCircuitEffectEntity>> PROJECTION_CIRCUIT_EFFECT = ENTITY_TYPES.register(
      "projection_circuit_effect",
      () -> Builder.<ProjectionCircuitEffectEntity>of((entityType, level) -> new ProjectionCircuitEffectEntity(entityType, level), MobCategory.MISC)
         .sized(8.0F, 1.0F)
         .clientTrackingRange(12)
         .updateInterval(20)
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
         .sized(2.5F, 8.0F)
         .clientTrackingRange(16)
         .updateInterval(20)
         .build("tsumukari_light_column_effect")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<HeraclesEntity>> HERACLES = ENTITY_TYPES.register(
      "heracles", () -> Builder.of(HeraclesEntity::new, MobCategory.CREATURE).sized(1.0F, 3.0F).build("heracles")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GenericServantEntity>> GENERIC_SERVANT = ENTITY_TYPES.register(
      "generic_servant", () -> Builder.of(GenericServantEntity::new, MobCategory.CREATURE)
         .sized(0.6F, 1.8F).clientTrackingRange(64).build("generic_servant")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SasakiKojiroEntity>> SASAKI_KOJIRO = ENTITY_TYPES.register(
      "sasaki_kojiro", () -> Builder.of(SasakiKojiroEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("sasaki_kojiro")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CuChulainnEntity>> CU_CHULAINN = ENTITY_TYPES.register(
      "cu_chulainn", () -> Builder.of(CuChulainnEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("cu_chulainn")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MedeaEntity>> MEDEA = ENTITY_TYPES.register(
      "medea", () -> Builder.of(MedeaEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("medea")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MedusaEntity>> MEDUSA = ENTITY_TYPES.register(
      "medusa", () -> Builder.of(MedusaEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("medusa")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CursedArmHassanEntity>> CURSED_ARM_HASSAN = ENTITY_TYPES.register(
      "cursed_arm_hassan", () -> Builder.of(CursedArmHassanEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("cursed_arm_hassan")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<MedusaPegasusEntity>> MEDUSA_PEGASUS = ENTITY_TYPES.register(
      "medusa_pegasus", () -> Builder.of(MedusaPegasusEntity::new, MobCategory.CREATURE).sized(2.4F, 2.4F).build("medusa_pegasus")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DragonfangSoldierEntity>> DRAGONFANG_SOLDIER = ENTITY_TYPES.register(
      "dragonfang_soldier", () -> Builder.of(DragonfangSoldierEntity::new, MobCategory.CREATURE).sized(0.6F, 1.9F).build("dragonfang_soldier")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GaeBulgProjectileEntity>> GAE_BULG_PROJECTILE = ENTITY_TYPES.register(
      "gae_bulg_projectile",
      () -> Builder.<GaeBulgProjectileEntity>of(
            (entityType, level) -> new GaeBulgProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.5F, 0.5F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("gae_bulg_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GaeBulgArmyProjectileEntity>> GAE_BULG_ARMY_PROJECTILE = ENTITY_TYPES.register(
      "gae_bulg_army_projectile",
      () -> Builder.<GaeBulgArmyProjectileEntity>of(
            (entityType, level) -> new GaeBulgArmyProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.5F, 0.5F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("gae_bulg_army_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<DirkProjectileEntity>> DIRK_PROJECTILE = ENTITY_TYPES.register(
      "dirk_projectile",
      () -> Builder.<DirkProjectileEntity>of(
            (entityType, level) -> new DirkProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("dirk_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EmiyaArcherEntity>> EMIYA_ARCHER = ENTITY_TYPES.register(
      "emiya_archer", () -> Builder.of(EmiyaArcherEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("emiya_archer")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArashEntity>> ARASH = ENTITY_TYPES.register(
      "arash", () -> Builder.of(ArashEntity::new, MobCategory.CREATURE)
         .sized(0.6F, 1.85F).clientTrackingRange(256).updateInterval(1).build("arash")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ArtoriaPendragonEntity>> ARTORIA_PENDRAGON = ENTITY_TYPES.register(
      "artoria_pendragon", () -> Builder.of(ArtoriaPendragonEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("artoria_pendragon")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<OdaNobunagaEntity>> ODA_NOBUNAGA = ENTITY_TYPES.register(
      "oda_nobunaga", () -> Builder.of(OdaNobunagaEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("oda_nobunaga")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<UshiwakamaruRiderEntity>> USHIWAKAMARU_RIDER = ENTITY_TYPES.register(
      "ushiwakamaru_rider", () -> Builder.of(UshiwakamaruRiderEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("ushiwakamaru_rider")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EnkiduEntity>> ENKIDU = ENTITY_TYPES.register(
      "enkidu", () -> Builder.of(EnkiduEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("enkidu")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GilgameshEntity>> GILGAMESH = ENTITY_TYPES.register(
      "gilgamesh", () -> Builder.of(GilgameshEntity::new, MobCategory.CREATURE).sized(0.6F, 1.82F).clientTrackingRange(64).build("gilgamesh")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GawainEntity>> GAWAIN = ENTITY_TYPES.register(
      "gawain", () -> Builder.of(GawainEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("gawain")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<LiShuwenEntity>> LI_SHUWEN = ENTITY_TYPES.register(
      "li_shuwen", () -> Builder.of(LiShuwenEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("li_shuwen")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ParacelsusEntity>> PARACELSUS = ENTITY_TYPES.register(
      "paracelsus", () -> Builder.of(ParacelsusEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).build("paracelsus")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PaleRiderEntity>> PALE_RIDER = ENTITY_TYPES.register(
      "pale_rider", () -> Builder.of(PaleRiderEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(64).build("pale_rider")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<NightingaleEntity>> NIGHTINGALE = ENTITY_TYPES.register(
      "nightingale", () -> Builder.of(NightingaleEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(64).build("nightingale")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ShadowHassanEntity>> SHADOW_HASSAN = ENTITY_TYPES.register(
      "shadow_hassan", () -> Builder.of(ShadowHassanEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(64).build("shadow_hassan")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ShadowHassanDeathShadowEntity>> SHADOW_HASSAN_DEATH_SHADOW = ENTITY_TYPES.register(
      "shadow_hassan_death_shadow", () -> Builder.<ShadowHassanDeathShadowEntity>of(ShadowHassanDeathShadowEntity::new, MobCategory.MISC)
         .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(1).build("shadow_hassan_death_shadow")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FanaticAssassinEntity>> FANATIC_ASSASSIN = ENTITY_TYPES.register(
      "fanatic_assassin", () -> Builder.of(FanaticAssassinEntity::new, MobCategory.CREATURE)
         .sized(0.6F, 1.8F).clientTrackingRange(64).updateInterval(1).build("fanatic_assassin")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<FanaticAssassinJinnEntity>> FANATIC_ASSASSIN_JINN = ENTITY_TYPES.register(
      "fanatic_assassin_jinn", () -> Builder.of(FanaticAssassinJinnEntity::new, MobCategory.CREATURE)
         .sized(1.1F, 1.5F).clientTrackingRange(64).updateInterval(1).build("fanatic_assassin_jinn")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RatSwarmEntity>> RAT_SWARM = ENTITY_TYPES.register(
      "rat_swarm", () -> Builder.of(RatSwarmEntity::new, MobCategory.CREATURE).sized(1.6F, 0.65F).clientTrackingRange(48).updateInterval(2).build("rat_swarm")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PaleRiderCrowEntity>> PALE_RIDER_CROW = ENTITY_TYPES.register(
      "pale_rider_crow", () -> Builder.of(PaleRiderCrowEntity::new, MobCategory.CREATURE).sized(0.5F, 0.9F).clientTrackingRange(48).updateInterval(2).build("pale_rider_crow")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<SoulEchoEntity>> SOUL_ECHO = ENTITY_TYPES.register(
      "soul_echo", () -> Builder.of(SoulEchoEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(64).build("soul_echo")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ApocalypseHorsemanEntity>> APOCALYPSE_HORSEMAN = ENTITY_TYPES.register(
      "apocalypse_horseman", () -> Builder.of(ApocalypseHorsemanEntity::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(64).build("apocalypse_horseman")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ApocalypseHorseEntity>> APOCALYPSE_HORSE = ENTITY_TYPES.register(
      "apocalypse_horse", () -> Builder.of(ApocalypseHorseEntity::new, MobCategory.CREATURE).sized(1.4F, 1.6F).clientTrackingRange(64).build("apocalypse_horse")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ConceptSwordEntity>> CONCEPT_SWORD = ENTITY_TYPES.register(
      "concept_sword",
      () -> Builder.<ConceptSwordEntity>of((entityType, level) -> new ConceptSwordEntity(entityType, level), MobCategory.MISC)
         .sized(0.35F, 0.35F).clientTrackingRange(64).updateInterval(1).build("concept_sword")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ParacelsusSpiritCannonEntity>> PARACELSUS_SPIRIT_CANNON = ENTITY_TYPES.register(
      "paracelsus_spirit_cannon",
      () -> Builder.<ParacelsusSpiritCannonEntity>of((entityType, level) -> new ParacelsusSpiritCannonEntity(entityType, level), MobCategory.MISC)
         .sized(0.7F, 0.7F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .build("paracelsus_spirit_cannon")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<OdaMatchlockGunEntity>> ODA_MATCHLOCK_GUN = ENTITY_TYPES.register(
      "oda_matchlock_gun",
      () -> Builder.<OdaMatchlockGunEntity>of((entityType, level) -> new OdaMatchlockGunEntity(entityType, level), MobCategory.MISC)
         .sized(1.8F, 0.8F)
         .clientTrackingRange(64)
         .updateInterval(1)
         .build("oda_matchlock_gun")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<OdaMatchlockBulletEntity>> ODA_MATCHLOCK_BULLET = ENTITY_TYPES.register(
      "oda_matchlock_bullet",
      () -> Builder.<OdaMatchlockBulletEntity>of(
            (entityType, level) -> new OdaMatchlockBulletEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.22F, 0.22F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .build("oda_matchlock_bullet")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ContenderBulletEntity>> CONTENDER_BULLET = ENTITY_TYPES.register(
      "contender_bullet",
      () -> Builder.<ContenderBulletEntity>of(
            (entityType, level) -> new ContenderBulletEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.16F, 0.16F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .build("contender_bullet")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<NightingaleBulletEntity>> NIGHTINGALE_BULLET = ENTITY_TYPES.register(
      "nightingale_bullet",
      () -> Builder.<NightingaleBulletEntity>of(
            (entityType, level) -> new NightingaleBulletEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.12F, 0.12F)
         .clientTrackingRange(32)
         .updateInterval(1)
         .build("nightingale_bullet")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RedSkeletonHajunEntity>> RED_SKELETON_HAJUN = ENTITY_TYPES.register(
      "red_skeleton_hajun",
      () -> Builder.<RedSkeletonHajunEntity>of((entityType, level) -> new RedSkeletonHajunEntity(entityType, level), MobCategory.MISC)
         .sized(6.0F, 8.0F)
         .clientTrackingRange(96)
         .updateInterval(2)
         .build("red_skeleton_hajun")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<ChainsOfHeavenBindingEntity>> CHAINS_OF_HEAVEN_BINDING = ENTITY_TYPES.register(
      "chains_of_heaven_binding",
      () -> Builder.<ChainsOfHeavenBindingEntity>of((entityType, level) -> new ChainsOfHeavenBindingEntity(entityType, level), MobCategory.MISC)
         .sized(2.2F, 2.6F)
         .clientTrackingRange(64)
         .updateInterval(1)
         .build("chains_of_heaven_binding")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GilgameshGateWeaponProjectileEntity>> GILGAMESH_GATE_PROJECTILE = ENTITY_TYPES.register(
      "gilgamesh_gate_projectile",
      () -> Builder.<GilgameshGateWeaponProjectileEntity>of(GilgameshGateWeaponProjectileEntity::new, MobCategory.MISC)
         .sized(0.5F, 0.5F).clientTrackingRange(96).updateInterval(1).build("gilgamesh_gate_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GilgameshCrossSlashEntity>> GILGAMESH_CROSS_SLASH = ENTITY_TYPES.register(
      "gilgamesh_cross_slash",
      () -> Builder.<GilgameshCrossSlashEntity>of(GilgameshCrossSlashEntity::new, MobCategory.MISC)
         .sized(2.0F, 2.0F).clientTrackingRange(256).updateInterval(1).build("gilgamesh_cross_slash")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<GilgameshEaBeamEntity>> GILGAMESH_EA_BEAM = ENTITY_TYPES.register(
      "gilgamesh_ea_beam",
      () -> Builder.<GilgameshEaBeamEntity>of(GilgameshEaBeamEntity::new, MobCategory.MISC)
         .sized(15.0F, 8.0F).clientTrackingRange(256).updateInterval(1).build("gilgamesh_ea_beam")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EmiyaThrownWeaponEntity>> EMIYA_THROWN_WEAPON = ENTITY_TYPES.register(
      "emiya_thrown_weapon",
      () -> Builder.<EmiyaThrownWeaponEntity>of(
            (entityType, level) -> new EmiyaThrownWeaponEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.25F, 0.25F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("emiya_thrown_weapon")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EnkiduEarthWeaponProjectileEntity>> ENKIDU_EARTH_WEAPON = ENTITY_TYPES.register(
      "enkidu_earth_weapon",
      () -> Builder.<EnkiduEarthWeaponProjectileEntity>of(
            (entityType, level) -> new EnkiduEarthWeaponProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.32F, 0.32F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("enkidu_earth_weapon")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<EmiyaArrowOrbProjectileEntity>> EMIYA_ARROW_ORB = ENTITY_TYPES.register(
      "emiya_arrow_orb",
      () -> Builder.<EmiyaArrowOrbProjectileEntity>of(
            (entityType, level) -> new EmiyaArrowOrbProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.32F, 0.32F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("emiya_arrow_orb")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<CrimsonHoundProjectileEntity>> CRIMSON_HOUND_PROJECTILE = ENTITY_TYPES.register(
      "crimson_hound_projectile",
      () -> Builder.<CrimsonHoundProjectileEntity>of(
            (entityType, level) -> new CrimsonHoundProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.45F, 0.45F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("crimson_hound_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<PseudoSpiralSwordProjectileEntity>> PSEUDO_SPIRAL_SWORD_PROJECTILE = ENTITY_TYPES.register(
      "pseudo_spiral_sword_projectile",
      () -> Builder.<PseudoSpiralSwordProjectileEntity>of(
            (entityType, level) -> new PseudoSpiralSwordProjectileEntity((EntityType<? extends ThrowableItemProjectile>)entityType, level), MobCategory.MISC
         )
         .sized(0.45F, 0.45F)
         .clientTrackingRange(12)
         .updateInterval(1)
         .build("pseudo_spiral_sword_projectile")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<RhoAiasEntity>> RHO_AIAS_SHIELD = ENTITY_TYPES.register(
      "rho_aias_shield",
      () -> Builder.<RhoAiasEntity>of((entityType, level) -> new RhoAiasEntity(entityType, level), MobCategory.MISC)
         .sized(3.5F, 3.5F)
         .clientTrackingRange(16)
         .updateInterval(1)
         .build("rho_aias_shield")
   );
   public static final DeferredHolder<EntityType<?>, EntityType<UbwSkyGearEntity>> UBW_SKY_GEAR = ENTITY_TYPES.register(
      "ubw_sky_gear",
      () -> Builder.<UbwSkyGearEntity>of((entityType, level) -> new UbwSkyGearEntity(entityType, level), MobCategory.MISC)
         .sized(16.0F, 16.0F)
         .clientTrackingRange(128)
         .updateInterval(2)
         .build("ubw_sky_gear")
   );

   public static void register(IEventBus eventBus) {
      ENTITY_TYPES.register(eventBus);
   }
}
