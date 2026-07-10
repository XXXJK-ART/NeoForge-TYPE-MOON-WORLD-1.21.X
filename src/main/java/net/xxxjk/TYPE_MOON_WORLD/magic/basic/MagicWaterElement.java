package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class MagicWaterElement {
   private MagicWaterElement() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double p = ElementalMagicHelper.proficiency(vars, "water_magic");
      boolean utility = vars.water_magic_mode == 1;
      if (utility && !ElementalMagicHelper.hasUtilityUnlocked("water_magic", p)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.element.utility_locked"), true);
         return false;
      }
      if (!ManaHelper.consumeOneTimeMagicCost(player, ElementalMagicHelper.applyManaAffinity(vars, "water_magic", 9.0 + p * 0.1))) {
         return false;
      }
      boolean ok = utility ? castUtility(player, vars, p) : castAttack(player, null, vars, p);
      if (ok) {
         ElementalMagicHelper.addPractice(vars, "water_magic", utility ? 0.22 : 0.18);
      }
      return ok;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, boolean utility) {
      if (utility && ElementalMagicHelper.hasUtilityUnlocked("water_magic", proficiency)) {
         return castUtility(caster, vars, proficiency);
      }
      return castAttack(caster, target, vars, proficiency);
   }

   private static boolean castAttack(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double p) {
      int tier = ElementalMagicHelper.tier(p);
      if (tier >= 3) {
         return ElementalMagicHelper.spawnProjectile(vars, "water_magic", caster, ElementalMagicHelper.aimDirection(caster, target, 4.4), ElementalMagicProjectileEntity.ELEMENT_WATER, ElementalMagicProjectileEntity.FORM_ULTIMATE, ElementalMagicHelper.lerpDamage(p, 30.0F, 40.0F), 0.0F, 5.0F, 0, 0, 0.0F, 40.0, 4.4F, false, true, 0.65F) != null;
      } else if (tier >= 1) {
         return ElementalMagicHelper.spawnProjectile(vars, "water_magic", caster, ElementalMagicHelper.aimDirection(caster, target, 2.6), ElementalMagicProjectileEntity.ELEMENT_WATER, ElementalMagicProjectileEntity.FORM_HIGH, ElementalMagicHelper.lerpDamage(p, 8.0F, 12.0F), 0.0F, 2.0F, 0, 0, 0.0F, 20.0, 2.6F, false, false, 0.65F) != null;
      }
      return ElementalMagicHelper.spawnProjectile(vars, "water_magic", caster, ElementalMagicHelper.aimDirection(caster, target, 1.4), ElementalMagicProjectileEntity.ELEMENT_WATER, ElementalMagicProjectileEntity.FORM_BASIC, ElementalMagicHelper.lerpDamage(p, 3.0F, 5.0F), 0.0F, 0.2F, 0, 0, 0.0F, 10.0, 1.4F, false, false, 0.5F) != null;
   }

   private static boolean castUtility(LivingEntity caster, TypeMoonWorldModVariables.PlayerVariables vars, double p) {
      if (!ElementalMagicHelper.hasUtilityUnlocked("water_magic", p)) {
         if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.element.utility_locked"), true);
         }
         return false;
      }
      if (ElementalMagicHelper.tier(p) < 2) {
         ElementalMagicHelper.changeGroundToMudOrFarmland(caster, 8.0);
         return true;
      }
      BlockPos pos = ElementalMagicHelper.targetBlock(caster, 12.0);
      return ElementalMagicHelper.spawnField(vars, "water_magic", caster, pos, ElementalMagicFieldEntity.ELEMENT_WATER, ElementalMagicFieldEntity.FORM_WATER_PRISON, 2.0F, 3.0F, 40, 8.0F) != null;
   }
}
