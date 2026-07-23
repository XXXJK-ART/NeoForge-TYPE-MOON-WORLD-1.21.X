package net.xxxjk.typemoonworld.api.datagen;
import net.minecraft.resources.ResourceLocation;
public final class AiDefinitionBuilder extends DefinitionDataBuilder {
   public AiDefinitionBuilder(ResourceLocation id) { super(id); }
   public AiDefinitionBuilder preferredDistance(double value) { number("preferred_attack_distance", value); return this; }
   public AiDefinitionBuilder retreatRatio(double value) { number("retreat_health_ratio", value); return this; }
}
