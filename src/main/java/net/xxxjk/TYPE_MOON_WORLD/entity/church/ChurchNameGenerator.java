package net.xxxjk.TYPE_MOON_WORLD.entity.church;

import net.minecraft.util.RandomSource;
import net.xxxjk.TYPE_MOON_WORLD.entity.MysticMagicianEntity;

final class ChurchNameGenerator {
   private ChurchNameGenerator() {}

   static String generate(RandomSource random, boolean female) {
      return MysticMagicianEntity.generateChurchNameChinese(random, female);
   }
}
