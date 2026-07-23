package net.xxxjk.typemoonworld.api.datagen;
import net.minecraft.resources.ResourceLocation;
public final class ServantCardBuilder extends DefinitionDataBuilder {
   public ServantCardBuilder(ResourceLocation id) { super(id); }
   public ServantCardBuilder servant(ResourceLocation value) { if (value != null) string("servant_id", value.toString()); return this; }
}
