package net.xxxjk.TYPE_MOON_WORLD.magic.basic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicFieldEntity;
import net.xxxjk.TYPE_MOON_WORLD.entity.ElementalMagicProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class MagicFireElement {
   private MagicFireElement() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player)) {
         return false;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double p = ElementalMagicHelper.proficiency(vars, "fire_magic");
      boolean utility = vars.fire_magic_mode == 1;
      if (!ManaHelper.consumeOneTimeMagicCost(player, cost(p, utility))) {
         return false;
      }
      boolean ok = utility ? castUtility(player, p) : castAttack(player, null, p);
      if (ok) {
         ElementalMagicHelper.addPractice(vars, "fire_magic", utility ? 0.22 : 0.18);
      }
      return ok;
   }

   public static boolean castDirect(LivingEntity caster, LivingEntity target, TypeMoonWorldModVariables.PlayerVariables vars, double proficiency, boolean utility) {
      if (utility && ElementalMagicHelper.hasUtilityUnlocked("fire_magic", proficiency)) {
         return castUtility(caster, proficiency);
      }
      return castAttack(caster, target, proficiency);
   }

   public static boolean castAttack(LivingEntity caster, LivingEntity target, double p) {
      int tier = ElementalMagicHelper.tier(p);
      if (tier >= 3) {
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 4.2), ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_ULTIMATE, ElementalMagicHelper.lerpDamage(p, 50.0F, 70.0F), 5.0F, 1.0F, 8, 0, 0.0F, 40.0, 4.2F, false, false, 1.1F) != null;
      }
      if (tier >= 2) {
         Vec3 dir = ElementalMagicHelper.aimDirection(caster, target, 3.2);
         Vec3 right = new Vec3(-dir.z, 0.0, dir.x).normalize();
         float damage = ElementalMagicHelper.lerpDamage(p, 18.0F, 25.0F);
         ElementalMagicHelper.spawnProjectile(caster, dir.add(right.scale(-0.1)), ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_HIGH, damage, 0.0F, 0.4F, 6, 0, 0.0F, 30.0, 3.2F, false, false, 0.75F);
         ElementalMagicHelper.spawnProjectile(caster, dir, ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_HIGH, damage, 0.0F, 0.4F, 6, 0, 0.0F, 30.0, 3.2F, false, false, 0.75F);
         ElementalMagicHelper.spawnProjectile(caster, dir.add(right.scale(0.1)), ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_HIGH, damage, 0.0F, 0.4F, 6, 0, 0.0F, 30.0, 3.2F, false, false, 0.75F);
         return true;
      }
      if (tier >= 1) {
         return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 2.5), ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_HIGH, ElementalMagicHelper.lerpDamage(p, 10.0F, 15.0F), 1.6F, 0.25F, 4, 0, 0.0F, 20.0, 2.5F, false, false, 0.75F) != null;
      }
      return ElementalMagicHelper.spawnProjectile(caster, ElementalMagicHelper.aimDirection(caster, target, 1.5), ElementalMagicProjectileEntity.ELEMENT_FIRE, ElementalMagicProjectileEntity.FORM_BASIC, ElementalMagicHelper.lerpDamage(p, 3.0F, 6.0F), 0.0F, 0.1F, 2, 0, 0.0F, 10.0, 1.5F, false, false, 0.5F) != null;
   }

   private static boolean castUtility(LivingEntity caster, double p) {
      if (!ElementalMagicHelper.hasUtilityUnlocked("fire_magic", p)) {
         if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.typemoonworld.magic.element.utility_locked"), true);
         }
         return false;
      }
      BlockPos pos = ElementalMagicHelper.targetBlock(caster, 10.0);
      return ElementalMagicHelper.spawnField(caster, pos, ElementalMagicFieldEntity.ELEMENT_FIRE, ElementalMagicFieldEntity.FORM_FIRE_WALL, 2.5F, 5.0F, 100, 4.0F) != null;
   }

   private static double cost(double p, boolean utility) {
      return utility ? 18.0 + p * 0.08 : 10.0 + p * 0.12;
   }
}
