package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;
import java.util.Set;

public interface MagicRegistry {
   boolean registerExecutor(ResourceLocation id, MagicExecutor executor);
   boolean registerDefinition(MagicDefinitionData definition);
   boolean registerPreset(ResourceLocation id, MagicPresetHandler handler);
   MagicKnowledge knowledge(LivingEntity entity);
   ManaAccess mana(LivingEntity entity);
   Optional<MagicDefinitionData> definition(ResourceLocation id);
   Set<ResourceLocation> definitions();
}
