package net.xxxjk.typemoonworld.api.datagen;
import net.minecraft.resources.ResourceLocation;
public final class ServantDefinitionBuilder extends DefinitionDataBuilder {
   public ServantDefinitionBuilder(ResourceLocation id) { super(id); }
   public ServantDefinitionBuilder classType(String value) { string("class_type", value); return this; }
   public ServantDefinitionBuilder faction(String value) { string("faction", value); return this; }
}
