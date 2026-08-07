package net.xxxjk.TYPE_MOON_WORLD.magic.church;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.BasicMagecraftHelper;
import net.xxxjk.TYPE_MOON_WORLD.magic.basic.MagicSpiritualHealing;
import net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle.DeadApostleEntity;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;
import net.xxxjk.TYPE_MOON_WORLD.utils.ManaHelper;

public final class MagicBaptismRite {
   public static final String MAGIC_ID = "baptism_rite";

   private MagicBaptismRite() {
   }

   public static boolean execute(Entity entity) {
      if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
         return false;
      }
      if (BaptismRiteEventHandler.isChanting(player)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.already_chanting"), true);
         return false;
      }

      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      double proficiency = vars.proficiency_baptism_rite;
      LivingEntity target = EntityUtils.findAutoAimTarget(player, targetRange(proficiency), 55.0);
      if (target == null || EntityUtils.isImmunePlayerTarget(target) || !isValidRiteTarget(target)) {
         player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.no_target"), true);
         return false;
      }

      double cost = 20.0 + BasicMagecraftHelper.clampProficiency(proficiency) * 0.08;
      if (!ManaHelper.consumeManaOrHealth(player, cost)) {
         return false;
      }

      UUID targetId = target.getUUID();
      int chantTicks = chantTicks(proficiency);
      BaptismRiteEventHandler.start(player, targetId, proficiency, chantTicks);
      player.displayClientMessage(Component.translatable("message.typemoonworld.magic.baptism_rite.start", target.getDisplayName()), true);
      net.xxxjk.TYPE_MOON_WORLD.magic.MagicProficiencyService.add(vars, "baptism_rite", 0.16);
      vars.syncProficiency(player);
      return true;
   }

   public static boolean isValidRiteTarget(LivingEntity target) {
      return target instanceof ServantEntity || isUndeadLike(target) || MagicSpiritualHealing.canAffect(target);
   }

   public static double targetRange(double proficiency) {
      return BasicMagecraftHelper.clampProficiency(proficiency) >= 50.0 ? 28.0 : 20.0;
   }

   public static int chantTicks(double proficiency) {
      return BasicMagecraftHelper.clampProficiency(proficiency) >= 50.0 ? 60 : 40;
   }

   private static boolean isUndeadLike(LivingEntity target) {
      return DeadApostleEntity.isDeadApostle(target)
         || target.getType().is(EntityTypeTags.UNDEAD)
         || target instanceof Zombie
         || target instanceof Skeleton
         || target instanceof Stray
         || target instanceof Husk
         || target instanceof Drowned
         || target instanceof WitherSkeleton
         || target instanceof WitherBoss
         || target instanceof ZombifiedPiglin;
   }
}
