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

public final class MagicEarthElement {
   private MagicEarthElement() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double p = ElementalMagicHelper.proficiency(vars, "earth_magic");
      boolean utility = vars.earth_magic_mode == 1;
      if (!ManaHelper.consumeOneTimeMagicCost(player, 9.0 + p * 0.1)) {
         return false;
      }
      boolean ok = utility ? castUtility(player, p) : castAttack(player, null, p);
      if (ok) {
         ElementalMagicHelper.addPractice(vars, "earth_magic", utility ? 0.22 : 0.18);
      }
      return ok;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, boolean utility) {
      if (utility && ElementalMagicHelper.hasUtilityUnlocked("earth_magic", proficiency)) {
         return castUtility(caster, proficiency);
      }
      return castAttack(caster, target, proficiency);
   }

   private static boolean castAttack(LivingEntity caster, LivingEntity target, double p) {
      int tier = ElementalMagicHelper.tier(p);
      if (tier >= 3) {
         BlockPos pos = target == null ? ElementalMagicHelper.targetBlock(caster, 16.0) : target.blockPosition();
         return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_EARTH, ElementalMagicFieldEntity.FORM_EARTH_PRISON, 3.0F, 3.0F, 100, 8.0F) != null;
      } else if (tier >= 2) {
         if (target != null) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.8, 0.0));
            target.hurtMarked = true;
            target.hurt(caster.damageSources().magic(), ElementalMagicHelper.lerpDamage(p, 15.0F, 20.0F));
            return true;
         }
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, null, 2.5), ElementalMagicProjectileEntity.ELEMENT_EARTH, ElementalMagicProjectileEntity.FORM_HIGH, ElementalMagicHelper.lerpDamage(p, 15.0F, 20.0F), 0.0F, 1.0F, 0, 20, 0.2F, 30.0, 2.5F, false, false, 0.7F) != null;
      } else if (tier >= 1) {
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 2.0), ElementalMagicProjectileEntity.ELEMENT_EARTH, ElementalMagicProjectileEntity.FORM_HIGH, ElementalMagicHelper.lerpDamage(p, 8.0F, 12.0F), 0.0F, 0.3F, 0, 60, 0.2F, 20.0, 2.0F, false, false, 0.6F) != null;
      }
      return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 1.4), ElementalMagicProjectileEntity.ELEMENT_EARTH, ElementalMagicProjectileEntity.FORM_BASIC, ElementalMagicHelper.lerpDamage(p, 3.0F, 5.0F), 0.0F, 0.2F, 0, 0, 0.0F, 10.0, 1.4F, false, false, 0.5F) != null;
   }

   private static boolean castUtility(LivingEntity caster, double p) {
      if (!ElementalMagicHelper.hasUtilityUnlocked("earth_magic", p)) {
         if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.element.utility_locked"), true);
         }
         return false;
      }
      BlockPos pos = ElementalMagicHelper.targetBlock(caster, 10.0);
      if (ElementalMagicHelper.tier(p) >= 3) {
         return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_EARTH, ElementalMagicFieldEntity.FORM_EARTH_QUAKE, 6.0F, 6.0F, 60, 0.0F) != null;
      } else if (ElementalMagicHelper.tier(p) >= 2) {
         return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_EARTH, ElementalMagicFieldEntity.FORM_EARTH_WALL, 2.0F, 3.0F, 100, 0.0F) != null;
      }
      return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_EARTH, ElementalMagicFieldEntity.FORM_EARTH_WALL, 2.0F, 2.0F, 200, 0.0F) != null;
   }
}
