package net.xxxjk.TYPE_MOON_WORLD.servant.shadowhassan;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ShadowHassanDamageSource extends DamageSource {
   private static final List<String> DEATH_MESSAGES = List.of(
      "death.attack.fall", "death.attack.drown", "death.attack.inFire", "death.attack.cactus",
      "death.attack.inWall", "death.attack.starve", "death.attack.freeze");

   @Nullable
   private final UUID attackerUuid;

   private ShadowHassanDamageSource(DamageSource source, boolean hideAttacker) {
      super(source.typeHolder(), hideAttacker ? null : source.getDirectEntity(),
         hideAttacker ? null : source.getEntity(), source.getSourcePosition());
      Entity attacker = source.getEntity();
      if (attacker == null && source.getDirectEntity() instanceof Projectile projectile) {
         attacker = projectile.getOwner();
      }
      this.attackerUuid = attacker == null ? null : attacker.getUUID();
   }

   public static ShadowHassanDamageSource withoutAttribution(DamageSource source) {
      return new ShadowHassanDamageSource(source, true);
   }

   public static ShadowHassanDamageSource forTarget(DamageSource source, LivingEntity target) {
      return new ShadowHassanDamageSource(source, target instanceof NeutralMob);
   }

   @Nullable
   public LivingEntity resolveAttacker(ServerLevel level) {
      if (this.attackerUuid == null) return null;
      Entity loaded = level.getEntity(this.attackerUuid);
      if (loaded instanceof LivingEntity living) return living;
      return level.getServer().getPlayerList().getPlayer(this.attackerUuid);
   }

   @Override
   public Component getLocalizedDeathMessage(LivingEntity victim) {
      String key = DEATH_MESSAGES.get(victim.getRandom().nextInt(DEATH_MESSAGES.size()));
      return Component.translatable(key, victim.getDisplayName());
   }
}
