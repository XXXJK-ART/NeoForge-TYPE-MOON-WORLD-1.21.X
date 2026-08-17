package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.registry.AddonEntities;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;

public final class BuiltinServantEntityFactory {
   private BuiltinServantEntityFactory() {
   }

   public static ServantEntity create(ServerLevel level, ResourceLocation servantId) {
      if (level == null || servantId == null) {
         return null;
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && UshiwakamaruRiderEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.USHIWAKAMARU_RIDER.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && ZhaoYunRiderEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.ZHAO_YUN_RIDER.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && IskandarEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.ISKANDAR.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && ShadowHassanEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.SHADOW_HASSAN.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && FanaticAssassinEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.FANATIC_ASSASSIN.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && HundredFacesHassanEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.HUNDRED_FACES_HASSAN.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && DiarmuidUaDuibhneEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.DIARMUID_UA_DUIBHNE.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && LancelotBerserkerEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.LANCELOT_BERSERKER.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && ArashEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.ARASH.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && NightingaleEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.NIGHTINGALE.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && PaleRiderEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.PALE_RIDER.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && MedusaEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.MEDUSA.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && OkitaSoujiSaberEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return ModEntities.OKITA_SOUJI_SABER.get().create(level);
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())
         && GillesDeRaisEntity.SERVANT_KEY.equals(servantId.getPath())) {
         return AddonEntities.GILLES_DE_RAIS_CASTER.get().create(level);
      }
      GenericServantEntity generic = ModEntities.GENERIC_SERVANT.get().create(level);
      if (generic != null) {
         generic.setServantId(servantId.toString());
      }
      return generic;
   }
}
