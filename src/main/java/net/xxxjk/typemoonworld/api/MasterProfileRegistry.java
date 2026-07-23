package net.xxxjk.typemoonworld.api;

import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface MasterProfileRegistry {
   boolean register(MasterProfileData profile, MasterProfileInitializer initializer);
   Optional<MasterProfileData> profile(ResourceLocation id);
   Set<ResourceLocation> profiles();
   ItemStack createCard(ResourceLocation id);
}
