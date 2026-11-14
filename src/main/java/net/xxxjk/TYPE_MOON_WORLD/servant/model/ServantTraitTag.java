package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum ServantTraitTag {
   DIVINE("divine"),
   DEMONIC("demonic"),
   DRAGON("dragon"),
   BEAST("beast"),
   UNDEAD("undead"),
   GIANT("giant"),
   MECHANICAL("mechanical"),
   FAIRY_TALE("fairy_tale"),
   FAIRY("fairy"),
   MOUNTED("mounted"),
   CHALDEAN("chaldean"),
   EARTH("earth"),
   HEAVEN_OR_EARTH_SERVANT("heaven_or_earth_servant"),
   GREEK_MYTHOLOGY_MALE("greek_mythology_male"),
   ARGONAUT("argonaut"),
   RIDING("riding"),
   SUN_KNIGHT("sun_knight"),

   HUMAN("human"),
   HUMANOID("humanoid"),
   LIVING_HUMAN("living_human"),
   HUMAN_KIND("human_kind"),
   HUMAN_THREAT("human_threat"),
   SAPIENT("sapient"),

   MALE("male"),
   FEMALE("female"),
   CHILD("child"),

   SUMMER("summer"),
   COSTUME("costume"),

   SABER("saber"),
   LANCER("lancer"),
   ARCHER("archer"),
   RIDER("rider"),
   CASTER("caster"),
   ASSASSIN("assassin"),
   BERSERKER("berserker"),
   RULER("ruler"),
   AVENGER("avenger"),
   MOON_CANCER("moon_cancer"),
   ALTER_EGO("alter_ego"),
   FOREIGNER("foreigner"),
   PRETENDER("pretender"),
   SHIELDER("shielder"),

   SERVANT("servant"),
   SEVEN_KNIGHTS("seven_knights"),
   BELOVED("beloved"),
   FIANNA_KNIGHTS("fianna_knights"),
   KNIGHT("knight"),
   SHADOW_SERVANT("shadow_servant"),

   CHAOTIC("chaotic"),
   LAWFUL("lawful"),
   NEUTRAL("neutral_alignment"),
   GOOD("good"),
   EVIL("evil"),
   BALANCED("balanced"),
   MAD("mad"),

   KING("king"),
   ARTHUR("arthur"),
   NOBUNAGA("nobunaga"),
   ROMAN("roman"),
   GENJI("genji"),
   ROUND_TABLE("round_table"),
   KNIGHT_OF_THE_LAKE("knight_of_the_lake"),
   STAR_BORN("star_born"),
   CELESTIAL("celestial"),
   WITCH("witch"),
   NURSE("nurse"),
   LAMP_ANGEL("lamp_angel"),
   CRIMEAN_ANGEL("crimean_angel"),
   EA_SPECIAL("ea_special"),
   GENDERLESS("genderless"),
   CONCEPTUAL_EXISTENCE("conceptual_existence"),
   APOCALYPSE_HORSEMAN("apocalypse_horseman"),
   PLAGUE("plague"),
   FALSE_RIDER("false_rider"),
   SYSTEM_SERVANT("system_servant"),
   HASSAN_ORDER("hassan_order"),
   SHINSENGUMI("shinsengumi"),
   FANATIC("fanatic"),
   DIVINE_TECHNIQUE_MIMIC("divine_technique_mimic");

   private final String key;

   ServantTraitTag(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   public static ServantTraitTag fromKey(String key) {
      return findByKey(key).orElse(SERVANT);
   }

   public static java.util.Optional<ServantTraitTag> findByKey(String key) {
      if (key == null) {
         return java.util.Optional.empty();
      }

      for (ServantTraitTag tag : values()) {
         if (tag.key.equals(key.toLowerCase(java.util.Locale.ROOT))) {
            return java.util.Optional.of(tag);
         }
      }

      return java.util.Optional.empty();
   }
}
