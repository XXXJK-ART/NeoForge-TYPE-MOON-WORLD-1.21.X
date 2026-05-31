package net.xxxjk.TYPE_MOON_WORLD.servant.ai.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.ServantAiModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public final class MovementModule implements ServantAiModule {
   private static final int WANDER_INTERVAL = 120;
   private static final int WANDER_RANGE = 8;

   @Override
   public void tick(ServantEntity entity, ServantAiContext context) {
      // 有目标时不控制移动，由 CombatModule 处理
      LivingEntity target = context.target();
      if (target != null) {
         return;
      }

      // 闲逛逻辑
      if (entity.getNavigation().isDone() && entity.getRandom().nextInt(WANDER_INTERVAL) == 0) {
         BlockPos current = entity.blockPosition();
         int dx = entity.getRandom().nextIntBetweenInclusive(-WANDER_RANGE, WANDER_RANGE);
         int dz = entity.getRandom().nextIntBetweenInclusive(-WANDER_RANGE, WANDER_RANGE);
         BlockPos wanderTarget = current.offset(dx, 0, dz);
         entity.getNavigation().moveTo(
            wanderTarget.getX() + 0.5,
            wanderTarget.getY(),
            wanderTarget.getZ() + 0.5,
            0.6
         );
      }
   }
}
