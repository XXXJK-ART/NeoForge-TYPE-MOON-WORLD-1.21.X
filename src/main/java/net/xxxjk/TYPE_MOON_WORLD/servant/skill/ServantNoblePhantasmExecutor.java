package net.xxxjk.TYPE_MOON_WORLD.servant.skill;

import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantNoblePhantasmDefinition;

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

      double currentMp = caster.getCurrentMp();
      if (currentMp < npDef.mpCost()) {
         return ServantExecutionResult.FAILED;
      }

      ServantExecutionContext context = new ServantExecutionContext(
         caster, target, currentMp, overChargeLevel
      );

      caster.setCurrentMp(currentMp - npDef.mpCost());

      return ServantExecutionResult.SUCCESS.withMpCost(npDef.mpCost());
   }
}
