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

   private static final class TypedSource extends DamageSource {
      final String npId;
      TypedSource(DamageSource base, String npId) {
         super(base.typeHolder(), base.getDirectEntity(), base.getEntity(), base.getSourcePosition());
         this.npId = npId;
      }
   }
   /** Attach an existing NP record without changing its base damage type or other servants' damage rules. */
   public static DamageSource withNoblePhantasm(DamageSource base, String npId) { return new TypedSource(base, npId); }

   /** Known NP provenance only; ordinary area attacks must not pass this classifier. */
   public static net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType typeOf(DamageSource source) {
      if (source instanceof net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource oda) return oda.npType();
      if (source == null) return null;
      if (source instanceof TypedSource typed) {
         var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry.get(typed.npId);
         return definition == null ? null : definition.type();
      }
      if (isParacelsusElementalSwordDamage(source) || source.is(net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderDamageTypes.CONCEPT_SWORD))
         return net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType.ARMY;
      Entity direct = source.getDirectEntity();
      if (direct instanceof ArtoriaExcaliburBeamEntity) return net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType.CASTLE;
      if (direct instanceof GaeBulgArmyProjectileEntity
         || direct instanceof net.xxxjk.TYPE_MOON_WORLD.entity.ArashStellaControllerEntity) return net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition.NpType.ARMY;
      if (direct != null) {
         String id = direct.getPersistentData().getString("NoblePhantasmId");
         var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry.get(id);
         if (definition != null) return definition.type();
      }
      if (source.is(NOBLE_PHANTASM_DAMAGE)) {
         String id = source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("");
         var definition = net.xxxjk.TYPE_MOON_WORLD.servant.data.ServantNoblePhantasmDataRegistry.get(id);
         if (definition != null) return definition.type();
      }
      return null;
   }

   public static boolean isParacelsusElementalSwordDamage(DamageSource source) {
      return source != null && source.is(ParacelsusDamageTypes.ELEMENTAL_SWORD);
   }

   public static boolean isNoblePhantasmDamage(DamageSource source, float originalDamage) {
      if (source == null) return false;
      if (source instanceof net.xxxjk.TYPE_MOON_WORLD.servant.oda.OdaDamageSource oda) return oda.npType() != null;
      if (source.is(NOBLE_PHANTASM_DAMAGE) || source.is(ShadowHassanDamageTypes.MEDITATIVE_SENSITIVITY)) return true;
      Entity direct = source.getDirectEntity();
      if (direct instanceof ArtoriaExcaliburBeamEntity
         || direct instanceof GaeBulgArmyProjectileEntity
         || direct instanceof PseudoSpiralSwordProjectileEntity) return true;
      if (!source.is(DamageTypeTags.IS_EXPLOSION) || originalDamage < 300.0F) return false;
      if (direct instanceof PseudoSpiralSwordProjectileEntity
         || direct instanceof CrimsonHoundProjectileEntity
         || direct instanceof BrokenPhantasmProjectileEntity) return true;
      if (direct instanceof SwordBarrelProjectileEntity swordBarrel) return swordBarrel.isBrokenPhantasm();
      return direct instanceof EmiyaArcherEntity;
   }
}
