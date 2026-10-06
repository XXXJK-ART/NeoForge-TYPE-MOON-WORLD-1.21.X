package net.xxxjk.typemoonworld.api.datagen;
import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;
public final class MagicDefinitionBuilder extends DefinitionDataBuilder {
   public MagicDefinitionBuilder(ResourceLocation id) { super(id); }
   public MagicDefinitionBuilder manaCost(double value) { number("mana_cost", value); return this; }
   public MagicDefinitionBuilder cooldown(int value) { integer("cooldown_ticks", value); return this; }
   public MagicDefinitionBuilder npcAllowed(boolean value) { bool("npc_allowed", value); return this; }
   public MagicDefinitionBuilder playerAllowed(boolean value) { bool("player_allowed", value); return this; }
   public MagicDefinitionBuilder canonStatus(String value) { string("canon_status", value); return this; }
   public MagicDefinitionBuilder sourceKey(String value) { string("source_key", value); return this; }
   public MagicDefinitionBuilder magicType(String value) { string("magic_type", value); return this; }
   public MagicDefinitionBuilder requiredOrigin(String value) { string("required_origin", value); return this; }
   public MagicDefinitionBuilder requiredFoundation(String value) { string("required_foundation", value); return this; }
   public MagicDefinitionBuilder requiredCatalyst(String value) { string("required_catalyst", value); return this; }
   public MagicDefinitionBuilder castMode(String value) { string("cast_mode", value); return this; }
   public MagicDefinitionBuilder resourceModel(String value) { string("resource_model", value); return this; }
   public MagicDefinitionBuilder copyPolicy(String value) { string("copy_policy", value); return this; }
   public MagicDefinitionBuilder requireAttribute(ResourceLocation... attributes) {
      JsonArray values = new JsonArray();
      if (attributes != null) for (ResourceLocation attribute : attributes) if (attribute != null) values.add(attribute.toString());
      value("required_attributes", values);
      return this;
   }
}
