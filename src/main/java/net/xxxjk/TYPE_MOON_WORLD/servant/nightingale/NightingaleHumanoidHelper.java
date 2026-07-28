package net.xxxjk.TYPE_MOON_WORLD.servant.nightingale;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.HumanNpcEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.model.ServantTraitTag;

public final class NightingaleHumanoidHelper {
   public static final TagKey<net.minecraft.world.entity.EntityType<?>> HUMANOIDS = TagKey.create(
      Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, "nightingale_humanoids")
   );

   private NightingaleHumanoidHelper() {}

   public static boolean isHumanoid(LivingEntity entity) {
      if (entity instanceof Player || entity instanceof Villager || entity instanceof HumanNpcEntity) return true;
      if (entity.getType().is(HUMANOIDS)) return true;
      if (entity instanceof ServantEntity servant && servant.getDefinition() != null) {
         var traits = servant.getDefinition().traits();
         return traits.contains(ServantTraitTag.HUMAN)
            || traits.contains(ServantTraitTag.HUMANOID)
            || traits.contains(ServantTraitTag.LIVING_HUMAN);
      }
      return false;
   }
}
