package net.xxxjk.typemoonworld.api.datagen;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
public final class AiDefinitionBuilder extends DefinitionDataBuilder {
   public AiDefinitionBuilder(ResourceLocation id) { super(id); }
   private final JsonObject combat = new JsonObject();
   private final JsonObject tactical = new JsonObject();
   public AiDefinitionBuilder preferredDistance(double value) { combat.addProperty("preferred_attack_distance", value); object("combat", combat); return this; }
   public AiDefinitionBuilder retreatRatio(double value) { combat.addProperty("retreat_health_ratio", value); object("combat", combat); return this; }
   public AiDefinitionBuilder tacticalStyle(String value) { tactical.addProperty("style", value); object("tactical", tactical); return this; }
   public AiDefinitionBuilder rangeBand(double minimum, double preferred, double maximum) {
      tactical.addProperty("minimum_range", minimum); tactical.addProperty("preferred_range", preferred);
      tactical.addProperty("maximum_range", maximum); object("tactical", tactical); return this;
   }
   public AiDefinitionBuilder pursuit(double aggression, double interceptBias) {
      tactical.addProperty("pursuit_aggression", aggression); tactical.addProperty("intercept_bias", interceptBias);
      object("tactical", tactical); return this;
   }
}
