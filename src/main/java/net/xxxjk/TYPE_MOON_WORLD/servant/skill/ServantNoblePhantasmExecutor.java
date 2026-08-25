package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ZhaoYunRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.registry.ServantAddonRegistry;

public final class ServantNoblePhantasmExecutor {
   private ServantNoblePhantasmExecutor() {
   }

   public static ServantExecutionResult activateNp(
      ServantEntity caster,
      LivingEntity target,
      ServantNoblePhantasmDefinition npDef,
      int overChargeLevel
   ) {
      if (npDef == null) {
         return ServantExecutionResult.FAILED;
      }

      ServantNoblePhantasmResourceService.CastDecision decision =
         ServantNoblePhantasmResourceService.evaluateNpcCast(caster, npDef.mpCost());
      if (!decision.allowed() || ServantNoblePhantasmResourceService.isOverdraftWeak(caster)) {
         return ServantExecutionResult.FAILED;
      }
      double currentMp = caster.getCurrentMp();

      // Qinggang Sword is a servant-specific timed state rather than a
      // generic one-shot NP. Let Zhao Yun own its cooldown, MP deduction and
      // delayed-hit queue while still exposing it through the common executor.
      if (caster instanceof ZhaoYunRiderEntity zhaoYun && "qinggang_sword".equals(npDef.id())) {
         if (!zhaoYun.startAokoSword(decision)) {
            return ServantExecutionResult.FAILED;
         }
         ServantNoblePhantasmResourceService.commitNpcCast(caster, decision);
         return ServantExecutionResult.SUCCESS.withMpCost(decision.actualMpCost());
      }

      ServantExecutionResult addonResult = ServantAddonRegistry.executeNoblePhantasm(
         new ServantNoblePhantasmContext(
            caster, target, caster.getDefinition(), npDef, overChargeLevel, currentMp,
            decision.powerScale(), decision.overdraft()
         )
      );
      if (addonResult.handled()) {
         if (addonResult.success()) {
            ServantNoblePhantasmResourceService.commitNpcCast(caster, decision);
         }
         return addonResult;
      }

      ServantNoblePhantasmResourceService.commitNpcCast(caster, decision);

      return ServantExecutionResult.SUCCESS.withMpCost(decision.actualMpCost());
   }
}
