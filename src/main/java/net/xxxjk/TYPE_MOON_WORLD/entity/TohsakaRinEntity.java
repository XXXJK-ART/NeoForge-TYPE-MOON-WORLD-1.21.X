package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatPersonality;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatStyle;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcCombatTemperament;
import net.xxxjk.TYPE_MOON_WORLD.magic.npc.NpcMagicCastBridge;
import org.jetbrains.annotations.Nullable;

public class TohsakaRinEntity extends MysticMagicianEntity {
   private static final String TAG_HEALTH_MIGRATED = "TypeMoonTohsakaRinHealthV4";
   public static final double MAX_HEALTH = 150.0;
   private static final double FIXED_SCALE = 0.8;

   public TohsakaRinEntity(EntityType<? extends net.minecraft.world.entity.PathfinderMob> type, Level level) { super(type, level); }

   public static AttributeSupplier.Builder createAttributes() {
      return MysticMagicianEntity.createAttributes()
         .add(Attributes.MAX_HEALTH, MAX_HEALTH)
         .add(Attributes.MOVEMENT_SPEED, 0.29)
         .add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.ARMOR, 6.0)
         .add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
         .add(Attributes.SCALE, FIXED_SCALE);
   }

   @Override public void tick() {
      super.tick();
      if (!this.level().isClientSide()) {
         NpcScaleHelper.ensureFixedScale(this, FIXED_SCALE);
         ensureFixedHealth();
         if (this.tickCount % 40 == 0) NpcMagicCastBridge.ensureTohsakaRinBajiquan(this);
      }
   }

   private void ensureFixedHealth() {
      var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
      if (maxHealth == null) return;
      boolean migrated = this.getPersistentData().getBoolean(TAG_HEALTH_MIGRATED);
      if (Double.compare(maxHealth.getBaseValue(), MAX_HEALTH) != 0) maxHealth.setBaseValue(MAX_HEALTH);
      if (!migrated) {
         this.setHealth(this.getMaxHealth());
         this.getPersistentData().putBoolean(TAG_HEALTH_MIGRATED, true);
      }
   }

   @Nullable @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, @Nullable SpawnGroupData data) {
      SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
      NpcScaleHelper.ensureFixedScale(this, FIXED_SCALE);
      this.setCustomName(Component.translatable("entity.typemoonworld.tohsaka_rin"));
      this.setCustomNameVisible(true);
      this.setCombatPersonality(NpcCombatPersonality.GOOD);
      this.setCombatTemperament(NpcCombatTemperament.BOLD);
      this.setCombatStyle(NpcCombatStyle.RANGED_BURST);
      NpcMagicCastBridge.configureTohsakaRin(this);
      ensureFixedHealth();
      this.setHealth(this.getMaxHealth());
      this.setPersistenceRequired();
      return result;
   }
}
