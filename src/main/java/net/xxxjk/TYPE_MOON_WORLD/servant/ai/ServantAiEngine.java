package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.CombatModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.CommandModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.EnvironmentModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.HostileTargetingModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.MovementModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.ai.module.SocialModule;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantDefinition;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.BehaviorProfileMatrix;
import net.xxxjk.TYPE_MOON_WORLD.servant.personality.ServantBehaviorProfile;

public final class ServantAiEngine {
   private final List<ServantAiModule> modules = new ArrayList<>();

   public ServantAiEngine() {
      this.modules.add(new EnvironmentModule());
      this.modules.add(new CommandModule());
      this.modules.add(new HostileTargetingModule());
      this.modules.add(new CombatModule());
      this.modules.add(new MovementModule());
      this.modules.add(new SocialModule());
   }

   public void tick(ServantEntity entity) {
      ServantDefinition definition = entity.getDefinition();
      if (definition == null) {
         return;
      }

      ServantBehaviorProfile profile = BehaviorProfileMatrix.lookup(
         entity.getObedienceAxis(), entity.getPrincipleAxis()
      );

      for (ServantAiModule module : this.modules) {
         try {
            ServantAiContext ctx = new ServantAiContext(
               entity, entity.getTarget(), definition, profile, entity.level().getGameTime()
            );
            module.tick(entity, ctx);
         } catch (Exception e) {
            TYPE_MOON_WORLD.LOGGER.error("Servant AI module {} failed", module.getClass().getSimpleName(), e);
         }
      }
   }
}
