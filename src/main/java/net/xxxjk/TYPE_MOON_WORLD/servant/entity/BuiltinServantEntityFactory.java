package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;

public final class BuiltinServantEntityFactory {
   private BuiltinServantEntityFactory() {
   }

   public static ServantEntity create(ServerLevel level, ResourceLocation servantId) {
      if (level == null || servantId == null) {
         return null;
      }
      if (TYPE_MOON_WORLD.MOD_ID.equals(servantId.getNamespace())) {
         // Every built-in servant with a registered entity must keep that entity here.
         // Falling through to GenericServantEntity silently changes the renderer to the
         // Steve-style fallback and also drops servant-specific behavior.
         ServantEntity builtin = switch (servantId.getPath()) {
            case "arash" -> ModEntities.ARASH.get().create(level);
            case "artoria_pendragon" -> ModEntities.ARTORIA_PENDRAGON.get().create(level);
            case "cu_chulainn" -> ModEntities.CU_CHULAINN.get().create(level);
            case "cursed_arm_hassan" -> ModEntities.CURSED_ARM_HASSAN.get().create(level);
            case "diarmuid_ua_duibhne" -> ModEntities.DIARMUID_UA_DUIBHNE.get().create(level);
            case "emiya_archer" -> ModEntities.EMIYA_ARCHER.get().create(level);
            case "enkidu" -> ModEntities.ENKIDU.get().create(level);
            case "fanatic_assassin" -> ModEntities.FANATIC_ASSASSIN.get().create(level);
            case "gawain" -> ModEntities.GAWAIN.get().create(level);
            case "gilgamesh" -> ModEntities.GILGAMESH.get().create(level);
            case "gilgamesh_caster" -> ModEntities.GILGAMESH_CASTER.get().create(level);
            case "heracles" -> ModEntities.HERACLES.get().create(level);
            case "hundred_faces_hassan" -> ModEntities.HUNDRED_FACES_HASSAN.get().create(level);
            case "iskandar" -> ModEntities.ISKANDAR.get().create(level);
            case "lancelot_berserker" -> ModEntities.LANCELOT_BERSERKER.get().create(level);
            case "li_shuwen" -> ModEntities.LI_SHUWEN.get().create(level);
            case "medea" -> ModEntities.MEDEA.get().create(level);
            case "medusa" -> ModEntities.MEDUSA.get().create(level);
            case "nightingale" -> ModEntities.NIGHTINGALE.get().create(level);
            case "oda_nobunaga" -> ModEntities.ODA_NOBUNAGA.get().create(level);
            case "okita_souji_saber" -> ModEntities.OKITA_SOUJI_SABER.get().create(level);
            case "pale_rider" -> ModEntities.PALE_RIDER.get().create(level);
            case "paracelsus" -> ModEntities.PARACELSUS.get().create(level);
            case "sasaki_kojiro" -> ModEntities.SASAKI_KOJIRO.get().create(level);
            case "senko_muramasa" -> ModEntities.SENKO_MURAMASA.get().create(level);
            case "shadow_hassan" -> ModEntities.SHADOW_HASSAN.get().create(level);
            case "ushiwakamaru_rider" -> ModEntities.USHIWAKAMARU_RIDER.get().create(level);
            case "zhao_yun_rider" -> ModEntities.ZHAO_YUN_RIDER.get().create(level);
            default -> null;
         };
         if (builtin != null) {
            return builtin;
         }
      }
      ServantEntity external = ServantAddonRegistry.createExternalEntity(level, servantId);
      if (external != null) {
         return external;
      }
      GenericServantEntity generic = ModEntities.GENERIC_SERVANT.get().create(level);
      if (generic != null) {
         generic.setServantId(servantId.toString());
      }
      return generic;
   }
}
