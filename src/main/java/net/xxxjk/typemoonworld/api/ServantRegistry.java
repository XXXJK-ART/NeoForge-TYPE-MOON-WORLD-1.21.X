package net.xxxjk.typemoonworld.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

public interface ServantRegistry {
   boolean registerAction(ResourceLocation id, ServantActionExecutor executor);
   /** Declares a data-driven servant definition and enables generated content for it. */
   boolean registerDefinition(ResourceLocation id);
   ItemStack createCard(ResourceLocation servantId);
   /** Creates the universal data-backed summon item for a servant definition. */
   ItemStack createSummonItem(ResourceLocation servantId);
   LivingEntity summon(ServerLevel level, ResourceLocation servantId, BlockPos pos);
   boolean hasDefinition(ResourceLocation servantId);
   Set<ResourceLocation> definitions();
}
