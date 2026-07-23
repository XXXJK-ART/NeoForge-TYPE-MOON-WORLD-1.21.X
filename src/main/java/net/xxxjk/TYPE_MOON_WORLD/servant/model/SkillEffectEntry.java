package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import java.util.Map;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SkillEffectEntry(String effectType, Map<String, Dynamic<?>> params) {
   public static final Codec<SkillEffectEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.STRING.optionalFieldOf("effect_type", "").forGetter(SkillEffectEntry::effectType),
      Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH).optionalFieldOf("params", Map.of()).forGetter(SkillEffectEntry::params)
   ).apply(instance, SkillEffectEntry::new));
}
