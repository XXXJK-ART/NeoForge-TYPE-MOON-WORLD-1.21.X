package net.xxxjk.TYPE_MOON_WORLD.servant.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CuChulainnEntity extends ServantEntity {
   public static final String SERVANT_KEY = "cu_chulainn";

   public CuChulainnEntity(EntityType<CuChulainnEntity> entityType, Level level) {
      super(entityType, level, SERVANT_KEY);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (amount <= 0.0F) {
         return false;
      }
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         if (CuChulainnCombatHelper.tryNegateMedeaSmallMagic(this, source, amount)) {
            return false;
         }
         amount = CuChulainnCombatHelper.applyMagicResistance(this, source, amount);
         if (amount <= 0.0F) {
            return false;
         }
         if (CuChulainnCombatHelper.tryBlock(this, source)) {
            return false;
         }
      }
      return super.hurt(source, amount);
   }
}
