package net.xxxjk.typemoonworld.api.datagen;
import net.minecraft.resources.ResourceLocation;
public final class ServantSkillBuilder extends DefinitionDataBuilder {
   public ServantSkillBuilder(ResourceLocation id) { super(id); }
   public ServantSkillBuilder type(String value) { string("type", value); return this; }
   public ServantSkillBuilder cost(int value) { integer("mp_cost", value); return this; }
}
