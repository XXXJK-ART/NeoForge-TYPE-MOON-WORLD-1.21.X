package net.xxxjk.typemoonworld.api.datagen;
import com.google.gson.JsonArray;
import net.minecraft.resources.ResourceLocation;
public final class MagicDefinitionBuilder extends DefinitionDataBuilder {
   public MagicDefinitionBuilder(ResourceLocation id) { super(id); }
   public MagicDefinitionBuilder manaCost(double value) { number("mana_cost", value); return this; }
   public MagicDefinitionBuilder cooldown(int value) { integer("cooldown_ticks", value); return this; }
   public MagicDefinitionBuilder npcAllowed(boolean value) { bool("npc_allowed", value); return this; }
   public MagicDefinitionBuilder requireAttribute(ResourceLocation... attributes) {
      JsonArray values = new JsonArray();
      if (attributes != null) for (ResourceLocation attribute : attributes) if (attribute != null) values.add(attribute.toString());
      value("required_attributes", values);
      return this;
   }
}
