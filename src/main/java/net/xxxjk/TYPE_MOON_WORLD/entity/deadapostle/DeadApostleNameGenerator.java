package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import net.minecraft.util.RandomSource;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

final class DeadApostleNameGenerator {
   private DeadApostleNameGenerator() {}

   static String generate(RandomSource random, int culture, boolean female) {
      return MysticMagicianEntity.generateCulturalNameChinese(random, culture, female);
   }
}
