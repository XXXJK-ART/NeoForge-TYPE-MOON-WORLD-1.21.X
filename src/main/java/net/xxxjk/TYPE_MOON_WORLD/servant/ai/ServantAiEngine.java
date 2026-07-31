package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.CombatModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.EnvironmentModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.HostileTargetingModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.MovementModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.SocialModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.CommandModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.BehaviorProfileMatrix;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ServantBehaviorProfile;

public final class ServantAiEngine {
   private final List<ServantAiModule> modules = new ArrayList<>();

   public ServantAiEngine() {
      this.modules.add(new EnvironmentModule());
      this.modules.add(new HostileTargetingModule());
      this.modules.add(new CombatModule());
      this.modules.add(new CommandModule());
      this.modules.add(new SocialModule());
      this.modules.add(new MovementModule());
   }

   public void tick(ServantEntity entity) {
      net.xxxjk.TYPE_MOON_WORLD.combat.ai.EvasionMovementService.tickAirState(entity);
      ServantDefinition definition = entity.getDefinition();
      if (definition == null) {
         return;
      }

      ServantBehaviorProfile profile = BehaviorProfileMatrix.lookup(
         entity.getObedienceAxis(), entity.getPrincipleAxis()
      );
      long gameTick = entity.level().getGameTime();
      ServantAiContext ctx = new ServantAiContext(
         entity, entity.getTarget(), definition, profile, gameTick
      );

      for (ServantAiModule module : this.modules) {
         try {
            LivingEntity currentTarget = entity.getTarget();
            if (currentTarget != ctx.target()) {
               ctx = new ServantAiContext(entity, currentTarget, definition, profile, gameTick);
            }
            module.tick(entity, ctx);
         } catch (Exception e) {
            TYPE_MOON_WORLD.LOGGER.error("Servant AI module {} failed", module.getClass().getSimpleName(), e);
         }
      }
      // Legacy helpers have many early returns.  Run the shared tempo guard after
      // them so cooldowns and failed casts can never leave a servant idle forever.
      ServantCombatTempoService.enforceLegacy(entity, entity.getTarget(), gameTick);
      // Keep orientation deterministic at the end of the tick.  Individual skills
      // may return early, but a valid target should still be the facing authority.
      LivingEntity finalTarget = entity.getTarget();
      if (finalTarget != null && finalTarget.isAlive()
         && !net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantMasterTargeting.isContractMaster(entity, finalTarget)) {
         entity.getLookControl().setLookAt(finalTarget, 45.0F, 45.0F);
      }
   }
}
