package net.xxxjk.typemoonworld.api.datagen;
import net.minecraft.resources.ResourceLocation;
public final class NoblePhantasmBuilder extends DefinitionDataBuilder {
   public NoblePhantasmBuilder(ResourceLocation id) { super(id); }
   public NoblePhantasmBuilder rank(String value) { string("rank", value); return this; }
   public NoblePhantasmBuilder cost(int value) { integer("mp_cost", value); return this; }
}
