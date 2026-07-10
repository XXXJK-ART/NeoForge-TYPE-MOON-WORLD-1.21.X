package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class MagicWindElement {
   private MagicWindElement() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double p = ElementalMagicHelper.proficiency(vars, "wind_magic");
      boolean utility = vars.wind_magic_mode == 1;
      if (!ManaHelper.consumeOneTimeMagicCost(player, 8.0 + p * 0.09)) {
         return false;
      }
      boolean ok = utility ? castUtility(player, p) : castAttack(player, null, p);
      if (ok) {
         ElementalMagicHelper.addPractice(vars, "wind_magic", utility ? 0.22 : 0.18);
      }
      return ok;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, boolean utility) {
      if (utility && ElementalMagicHelper.hasUtilityUnlocked("wind_magic", proficiency)) {
         return castUtility(caster, proficiency);
      }
      return castAttack(caster, target, proficiency);
   }

   private static boolean castAttack(LivingEntity caster, LivingEntity target, double p) {
      int tier = ElementalMagicHelper.tier(p);
      if (tier >= 3) {
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 4.8), ElementalMagicProjectileEntity.ELEMENT_WIND, ElementalMagicProjectileEntity.FORM_ULTIMATE, ElementalMagicHelper.lerpDamage(p, 30.0F, 40.0F), 0.0F, 5.0F, 0, 0, 0.0F, 40.0, 4.8F, true, false, 0.35F) != null;
      } else if (tier >= 2) {
         BlockPos pos = target == null ? ElementalMagicHelper.targetBlock(caster, 16.0) : target.blockPosition();
         return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_WIND, ElementalMagicFieldEntity.FORM_WIND_TORNADO, 3.0F, 3.0F, 100, ElementalMagicHelper.lerpDamage(p, 15.0F, 20.0F)) != null;
      } else if (tier >= 1) {
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 3.0), ElementalMagicProjectileEntity.ELEMENT_WIND, ElementalMagicProjectileEntity.FORM_HIGH, ElementalMagicHelper.lerpDamage(p, 8.0F, 12.0F), 0.0F, 2.0F, 0, 0, 0.0F, 20.0, 3.0F, false, false, 0.45F) != null;
      }
      return ElementalMagicHelper.spawnField(caster, caster.blockPosition().relative(caster.getDirection(), 3), ElementalMagicFieldEntity.ELEMENT_WIND, ElementalMagicFieldEntity.FORM_WIND_TORNADO, 5.0F, 3.0F, 40, 0.0F) != null;
   }

   private static boolean castUtility(LivingEntity caster, double p) {
      if (!ElementalMagicHelper.hasUtilityUnlocked("wind_magic", p)) {
         if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.element.utility_locked"), true);
         }
         return false;
      }
      if (ElementalMagicHelper.tier(p) < 2) {
         caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, true, true));
         return true;
      }
      BlockPos pos = ElementalMagicHelper.targetBlock(caster, 10.0);
      return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_WIND, ElementalMagicFieldEntity.FORM_WIND_WALL, 3.0F, 5.0F, 100, 0.0F) != null;
   }
}
