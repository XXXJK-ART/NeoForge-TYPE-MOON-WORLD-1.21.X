package net.xxxjk.TYPE_MOON_WORLD.servant.lishuwen;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.LiShuwenEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.fanatic.FanaticDamageTypes;

/** Shared, non-stacking Seasoned A+ passive for the NPC and player card. */
public final class LiShuwenSeasonedService {
   private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "li_shuwen_seasoned");
   private static final List<Holder<MobEffect>> MENTAL_EFFECTS = List.of(MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.DARKNESS);

   private LiShuwenSeasonedService() { }

   public static boolean isActive(LivingEntity entity) {
      if (entity instanceof LiShuwenEntity) return entity.getPersistentData().getBoolean("LiShuwenSeasonedActive");
      if (entity instanceof ServerPlayer player) {
         var vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         return vars.servant_card_transformed && "li_shuwen".equals(vars.servant_card_id);
      }
      return false;
   }

   public static void tick(LivingEntity entity) {
      if (!(entity.level() instanceof ServerLevel level)) return;
      if (!isActive(entity)) {
         clear(entity);
         return;
      }
      applyModifier(entity, Attributes.KNOCKBACK_RESISTANCE, 0.20, AttributeModifier.Operation.ADD_VALUE);
      applyModifier(entity, Attributes.MOVEMENT_SPEED, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      clearMentalEffects(entity);
      if (entity.tickCount % 20 == 0) {
         for (ServantEntity ally : level.getEntitiesOfClass(ServantEntity.class, entity.getBoundingBox().inflate(20.0),
            other -> other.isAlive() && other.isAlliedTo(entity))) clearMentalEffects(ally);
      }
   }

   public static float incomingDamage(LivingEntity entity, DamageSource source, float amount) {
      if (amount <= 0 || !isActive(entity) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(DamageTypeTags.BYPASSES_RESISTANCE) || source.is(FanaticDamageTypes.BYPASSES_DEFENSES)) return amount;
      return amount * 0.95F;
   }

   public static void clear(LivingEntity entity) {
      for (var attribute : List.of(Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED)) {
         var instance = entity.getAttribute(attribute);
         if (instance != null) instance.removeModifier(MODIFIER_ID);
      }
   }

   private static void applyModifier(LivingEntity entity, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
      var instance = entity.getAttribute(attribute);
      if (instance == null) return;
      var existing = instance.getModifier(MODIFIER_ID);
      if (existing != null && existing.amount() == amount && existing.operation() == operation) return;
      instance.removeModifier(MODIFIER_ID);
      instance.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, operation));
   }

   private static void clearMentalEffects(LivingEntity entity) {
      for (var effect : MENTAL_EFFECTS) entity.removeEffect(effect);
   }
}
