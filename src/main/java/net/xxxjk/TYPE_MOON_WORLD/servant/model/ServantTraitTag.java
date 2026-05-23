package net.xxxjk.TYPE_MOON_WORLD.servant.model;

public enum ServantTraitTag {
   // 能力/血统
   DIVINE("divine"),
   DEMONIC("demonic"),
   DRAGON("dragon"),
   BEAST("beast"),
   UNDEAD("undead"),
   GIANT("giant"),
   MECHANICAL("mechanical"),
   FAIRY_TALE("fairy_tale"),
   MOUNTED("mounted"),
   CHALDEAN("chaldean"),

   // 人类相关
   HUMAN("human"),
   HUMANOID("humanoid"),
   LIVING_HUMAN("living_human"),
   HUMAN_THREAT("human_threat"),
   SAPIENT("sapient"),

   // 性别
   MALE("male"),
   FEMALE("female"),
   CHILD("child"),

   // 季节/外观
   SUMMER("summer"),
   COSTUME("costume"),

   // 职阶
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

   // 从者通用
   SERVANT("servant"),
   SEVEN_KNIGHTS("seven_knights"),
   SHADOW_SERVANT("shadow_servant"),

   // 阵营/属性
   CHAOTIC("chaotic"),
   LAWFUL("lawful"),
   NEUTRAL("neutral_alignment"),
   GOOD("good"),
   EVIL("evil"),
   BALANCED("balanced"),

   // 特殊/文化
   ROMAN("roman"),
   ROUND_TABLE("round_table"),
   STAR_BORN("star_born"),
   CELESTIAL("celestial"),
   WITCH("witch"),
   EA_SPECIAL("ea_special");

   private final String key;

   ServantTraitTag(String key) {
      this.key = key;
   }

   public String key() {
      return this.key;
   }

   public static ServantTraitTag fromKey(String key) {
      if (key == null) {
         return SERVANT;
      }

      for (ServantTraitTag tag : values()) {
         if (tag.key.equals(key.toLowerCase())) {
            return tag;
         }
      }

      return SERVANT;
   }
}
