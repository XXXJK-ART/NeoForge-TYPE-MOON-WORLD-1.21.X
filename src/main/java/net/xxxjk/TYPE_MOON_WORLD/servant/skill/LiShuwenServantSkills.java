package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.skill.ServantSkillRegistry;

public final class LiShuwenServantSkills {
   public static final String ACTION_PURSUIT = "pursuit";
   public static final String ACTION_CHARGE = "charge";
   public static final String ACTION_INTERRUPT = "interrupt";
   public static final String SKILL_PURSUIT = "li_shuwen_pursuit";
   public static final String SKILL_CHARGE = "li_shuwen_charge";
   public static final String SKILL_INTERRUPT = "li_shuwen_interrupt";

   private LiShuwenServantSkills() {
   }

   public static void registerBuiltin(ServantSkillRegistry registry) {
      registry.register("presence_concealment_circle_realm", LiShuwenServantSkills::markPresenceConcealment, "typemoonworld_core");
      registry.register("seasoned_a_plus", LiShuwenServantSkills::markSeasoned, "typemoonworld_core");
      registry.register("chinese_martial_arts_a_triple_plus", LiShuwenServantSkills::markChineseMartialArts, "typemoonworld_core");
      registry.register("sphere_boundary_extreme_a_minus", LiShuwenServantSkills::markSphereBoundary, "typemoonworld_core");
      registry.register("yin_yang_crossing_b", LiShuwenServantSkills::markYinYangCrossing, "typemoonworld_core");
      registry.register(SKILL_PURSUIT, LiShuwenServantSkills::markPursuit, "typemoonworld_core");
      registry.register(SKILL_CHARGE, LiShuwenServantSkills::markCharge, "typemoonworld_core");
      registry.register(SKILL_INTERRUPT, LiShuwenServantSkills::markInterrupt, "typemoonworld_core");
   }

   public static void registerCombatActions(IServantAddonRegistry registry) {
      registry.registerCombatAction(ACTION_PURSUIT, LiShuwenServantSkills::performPursuit, "typemoonworld_core");
      registry.registerCombatAction(ACTION_CHARGE, LiShuwenServantSkills::performCharge, "typemoonworld_core");
      registry.registerCombatAction(ACTION_INTERRUPT, LiShuwenServantSkills::performInterrupt, "typemoonworld_core");
   }

   private static ServantExecutionResult markPresenceConcealment(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenPresenceConcealmentActive");
   }

   private static ServantExecutionResult markSeasoned(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenSeasonedActive");
   }

   private static ServantExecutionResult markChineseMartialArts(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenChineseMartialArtsActive");
   }

   private static ServantExecutionResult markSphereBoundary(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenSphereBoundaryAvailable");
   }

   private static ServantExecutionResult markYinYangCrossing(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenYinYangCrossingAvailable");
   }

   private static ServantExecutionResult markPursuit(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenPursuitAvailable");
   }

   private static ServantExecutionResult markCharge(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenChargeAvailable");
   }

   private static ServantExecutionResult markInterrupt(ServantExecutionContext context) {
      return mark(context.caster(), "LiShuwenInterruptAvailable");
   }

   private static ServantExecutionResult performPursuit(ServantCombatActionContext context) {
      if (!(context.caster() instanceof LiShuwenEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (entity == null || target == null || !target.isAlive() || context.distance() > 8.0 || !context.hasLineOfSight()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      if (entity.getCurrentMp() < 4.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 4.0);
      entity.faceToward(target.position());
      entity.triggerPursuitAnimation();
      LiShuwenCombatHelper.performPursuitGapClose(entity, target, 1.30, 0.18, 0.95F);
      return ServantExecutionResult.SUCCESS.withMpCost(4.0);
   }

   private static ServantExecutionResult performCharge(ServantCombatActionContext context) {
      if (!(context.caster() instanceof LiShuwenEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (entity == null || target == null || !target.isAlive() || context.distance() < 3.0 || context.distance() > 12.0 || !context.hasLineOfSight()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      if (entity.getCurrentMp() < 6.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 6.0);
      entity.faceToward(target.position());
      entity.triggerChargeAnimation();
      LiShuwenCombatHelper.performPursuitGapClose(entity, target, 1.65, 0.22, 1.10F);
      return ServantExecutionResult.SUCCESS.withMpCost(6.0);
   }

   private static ServantExecutionResult performInterrupt(ServantCombatActionContext context) {
      if (!(context.caster() instanceof LiShuwenEntity entity)) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      LivingEntity target = context.target();
      if (entity == null || target == null || !target.isAlive() || context.distance() > 5.0 || !context.hasLineOfSight()) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      if (entity.getCurrentMp() < 5.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }
      entity.setCurrentMp(entity.getCurrentMp() - 5.0);
      entity.faceToward(target.position());
      entity.triggerInterruptAnimation();
      LiShuwenCombatHelper.performPursuitGapClose(entity, target, 0.95, 0.12, 1.25F);
      return ServantExecutionResult.SUCCESS.withMpCost(5.0);
   }

   private static ServantExecutionResult mark(LivingEntity entity, String tag) {
      if (!(entity instanceof LiShuwenEntity)) {
         return ServantExecutionResult.FAILED;
      }
      entity.getPersistentData().putBoolean(tag, true);
      return ServantExecutionResult.SUCCESS;
   }
}
