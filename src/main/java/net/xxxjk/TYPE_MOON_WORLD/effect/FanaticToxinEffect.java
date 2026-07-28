package net.xxxjk.TYPE_MOON_WORLD.effect;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticAssassinRules;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;

public final class FanaticToxinEffect extends UncurableEffect {
   public static final String TAG_OWNER = "FanaticToxinOwner";

   public FanaticToxinEffect() {
      super(MobEffectCategory.HARMFUL, 0x3B8F35);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 20 == 0;
   }

   @Override
   public boolean applyEffectTick(LivingEntity entity, int amplifier) {
      LivingEntity owner = toxinOwner(entity);
      if (owner != null) {
         entity.hurt(owner.damageSources().source(FanaticDamageTypes.TOXIN, owner), FanaticAssassinRules.TOXIN_DAMAGE_PER_SECOND);
      } else {
         entity.hurt(entity.damageSources().source(FanaticDamageTypes.TOXIN), FanaticAssassinRules.TOXIN_DAMAGE_PER_SECOND);
      }
      return true;
   }

   private static LivingEntity toxinOwner(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level) || !entity.getPersistentData().hasUUID(TAG_OWNER)) return null;
      UUID id = entity.getPersistentData().getUUID(TAG_OWNER);
      Entity owner = level.getEntity(id);
      return owner instanceof LivingEntity living && living.isAlive() ? living : null;
   }
}
