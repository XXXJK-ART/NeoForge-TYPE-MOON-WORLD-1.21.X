package net.xxxjk.TYPE_MOON_WORLD.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.xxxjk.TYPE_MOON_WORLD.init.ModEntities;
import net.xxxjk.TYPE_MOON_WORLD.item.ModItems;
import net.minecraft.world.item.ItemStack;

public class GaeBulgArmyProjectileEntity extends GaeBulgProjectileEntity {
   public GaeBulgArmyProjectileEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
      super(type, level);
      this.setMode(Mode.ARMY);
   }

   public GaeBulgArmyProjectileEntity(Level level, LivingEntity shooter) {
      super(ModEntities.GAE_BULG_ARMY_PROJECTILE.get(), shooter, level);
      this.setItem(new ItemStack(ModItems.GAE_BULG.get()));
      this.setMode(Mode.ARMY);
   }
}
