package net.xxxjk.typemoonworld.api.datagen;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;

/** Small Codec-friendly builder used by addon data providers. */
public class DefinitionDataBuilder {
   protected final ResourceLocation id;
   protected final JsonObject json = new JsonObject();
   public DefinitionDataBuilder(ResourceLocation id) { if (id == null) throw new IllegalArgumentException("id"); this.id = id; json.addProperty("id", id.toString()); }
   public DefinitionDataBuilder string(String key, String value) { if (key != null && value != null) json.addProperty(key, value); return this; }
   public DefinitionDataBuilder bool(String key, boolean value) { if (key != null) json.addProperty(key, value); return this; }
   public DefinitionDataBuilder integer(String key, int value) { if (key != null) json.addProperty(key, value); return this; }
   public DefinitionDataBuilder number(String key, double value) { if (key != null) json.addProperty(key, value); return this; }
   public DefinitionDataBuilder object(String key, JsonObject value) { if (key != null && value != null) json.add(key, value); return this; }
   public DefinitionDataBuilder value(String key, JsonElement value) { if (key != null && value != null) json.add(key, value); return this; }
   public ResourceLocation id() { return id; }
   public JsonObject json() { return json.deepCopy(); }
   public CompletableFuture<?> save(CachedOutput output, Path root, String directory) { return DataProvider.saveStable(output, json, root.resolve(directory).resolve(id.getNamespace()).resolve(id.getPath() + ".json")); }
}
