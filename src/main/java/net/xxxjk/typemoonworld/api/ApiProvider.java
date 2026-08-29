package net.xxxjk.typemoonworld.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

/** Runtime bridge implemented by Type Moon World itself. */
public interface ApiProvider {
   AddonRegistrar addon(String modId);
   BodyTrainingAccess bodyTraining(LivingEntity entity);
   MagicAttributeAccess magicAttributes(LivingEntity entity);
   boolean isMagicAvailable(LivingEntity entity, ResourceLocation magicId);
   ProjectionEffects projectionEffects();
   ServantFormAccess servantForm(ServerPlayer player);
   MasterAccess master(ServerPlayer player);
}
