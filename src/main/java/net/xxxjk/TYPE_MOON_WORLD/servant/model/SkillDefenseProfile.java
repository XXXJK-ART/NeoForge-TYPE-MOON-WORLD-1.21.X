package net.xxxjk.TYPE_MOON_WORLD.servant.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record SkillDefenseProfile(boolean evasion, boolean guard, boolean durability, List<String> activationActions) {
   public static final SkillDefenseProfile NONE = new SkillDefenseProfile(false, false, false, List.of());
   public static final Codec<SkillDefenseProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.BOOL.optionalFieldOf("evasion", false).forGetter(SkillDefenseProfile::evasion),
      Codec.BOOL.optionalFieldOf("guard", false).forGetter(SkillDefenseProfile::guard),
      Codec.BOOL.optionalFieldOf("durability", false).forGetter(SkillDefenseProfile::durability),
      Codec.STRING.listOf().optionalFieldOf("activation_actions", List.of()).forGetter(SkillDefenseProfile::activationActions)
   ).apply(instance, SkillDefenseProfile::new));
   public SkillDefenseProfile { activationActions = activationActions == null ? List.of() : List.copyOf(activationActions); }
}
