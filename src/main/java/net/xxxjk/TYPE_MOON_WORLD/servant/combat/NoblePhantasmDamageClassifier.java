package net.xxxjk.TYPE_MOON_WORLD.servant.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.ArtoriaExcaliburBeamEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.BrokenPhantasmProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.CrimsonHoundProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.GaeBulgArmyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.PseudoSpiralSwordProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.SwordBarrelProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ParacelsusDamageTypes;
import net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan.ShadowHassanDamageTypes;

public final class NoblePhantasmDamageClassifier {
   public static final TagKey<DamageType> NOBLE_PHANTASM_DAMAGE = TagKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "noble_phantasm_damage")
   );

   private NoblePhantasmDamageClassifier() {}

   public static boolean isParacelsusElementalSwordDamage(DamageSource source) {
      return source != null && source.is(ParacelsusDamageTypes.ELEMENTAL_SWORD);
   }

   public static boolean isNoblePhantasmDamage(DamageSource source, float originalDamage) {
      if (source == null) return false;
      if (source.is(NOBLE_PHANTASM_DAMAGE) || source.is(ShadowHassanDamageTypes.MEDITATIVE_SENSITIVITY)) return true;
      Entity direct = source.getDirectEntity();
      if (direct instanceof ArtoriaExcaliburBeamEntity || direct instanceof GaeBulgArmyProjectileEntity) return true;
      if (!source.is(DamageTypeTags.IS_EXPLOSION) || originalDamage < 300.0F) return false;
      if (direct instanceof PseudoSpiralSwordProjectileEntity
         || direct instanceof CrimsonHoundProjectileEntity
         || direct instanceof BrokenPhantasmProjectileEntity) return true;
      if (direct instanceof SwordBarrelProjectileEntity swordBarrel) return swordBarrel.isBrokenPhantasm();
      return direct instanceof EmiyaArcherEntity;
   }
}
