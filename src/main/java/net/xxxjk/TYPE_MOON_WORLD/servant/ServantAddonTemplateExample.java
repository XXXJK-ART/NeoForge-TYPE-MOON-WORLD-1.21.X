package net.xxxjk.TYPE_MOON_WORLD.servant;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantLifecycleContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantNoblePhantasmContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

/**
 * Reference template for addon servants under the current servant extension API.
 *
 * <p>Addon loading:
 * <ol>
 * <li>Implement {@link IServantAddonEntrypoint} and register combat actions / noble phantasms.</li>
 * <li>Create {@code META-INF/services/net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint}
 *     in your addon mod and write your entrypoint class name into it.</li>
 * <li>Put servant data JSON under {@code data/<your_modid>/servant/definitions/<servant_id>.json}.
 *     The core reload listener reads the same directory from every namespace.</li>
 * <li>Register your own {@code EntityType} that extends {@link ServantEntity}; pass the same servant id
 *     to the superclass constructor.</li>
 * <li>Register your own renderer/model on the client, pointing to your addon assets.</li>
 * </ol>
 *
 * <p>The JSON {@code specialization.combat_actions} list controls which registered action executors
 * are considered by the core combat AI. If your executor is registered as {@code example_flash_cut},
 * the servant definition must include {@code "example_flash_cut"} in {@code combat_actions}.
 */
public final class ServantAddonTemplateExample {
   public static final String PROVIDER_ID = "example_servant_addon";
   public static final String ACTION_FLASH_CUT = "example_flash_cut";
   public static final String NP_EXAMPLE_SWORD = "example_sword_np";
   public static final String LIFECYCLE_HANDLER = "example_servant_lifecycle";
   private static final String LAST_FLASH_CUT_TICK = "ExampleAddonLastFlashCutTick";
   private static final int FLASH_CUT_COOLDOWN = 70;

   private ServantAddonTemplateExample() {
   }

   public static final class ExampleEntrypoint implements IServantAddonEntrypoint {
      @Override
      public String providerId() {
         return PROVIDER_ID;
      }

      @Override
      public void registerServants(IServantAddonRegistry registry) {
         registry.registerCombatAction(ACTION_FLASH_CUT, ServantAddonTemplateExample::castFlashCut, this.providerId());
         registry.registerNoblePhantasm(NP_EXAMPLE_SWORD, ServantAddonTemplateExample::castExampleSwordNp, this.providerId());
         registry.registerLifecycleHandler(LIFECYCLE_HANDLER, ServantAddonTemplateExample::tickExampleServants, this.providerId());
      }
   }

   private static ServantExecutionResult tickExampleServants(ServantLifecycleContext context) {
      ServantEntity servant = context.entity();
      if (servant == null || context.definition() == null || !"example_saber".equals(context.definition().id())) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      LivingEntity target = context.target();
      if (target != null && target.isAlive() && servant.distanceTo(target) > 10.0 && servant.getNavigation().isDone()) {
         servant.getNavigation().moveTo(target, 1.25);
      }
      return ServantExecutionResult.NOT_HANDLED;
   }

   private static ServantExecutionResult castFlashCut(ServantCombatActionContext context) {
      ServantEntity caster = context.caster();
      LivingEntity target = context.target();
      if (caster == null || target == null || !target.isAlive() || context.distance() > 5.0 || !context.hasLineOfSight()) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      CompoundTag data = caster.getPersistentData();
      if (context.gameTick() - data.getLong(LAST_FLASH_CUT_TICK) < FLASH_CUT_COOLDOWN || caster.getCurrentMp() < 6.0) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      data.putLong(LAST_FLASH_CUT_TICK, context.gameTick());
      caster.setCurrentMp(caster.getCurrentMp() - 6.0);
      caster.faceToward(target.position());
      caster.triggerSlashAnimation();

      float damage = (float)(caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.2);
      target.invulnerableTime = 0;
      target.hurt(caster.damageSources().mobAttack(caster), damage);
      pushTargetAway(caster, target, 0.75, 0.25);

      if (caster.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
         level.playSound(null, caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.25F);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(6.0);
   }

   private static ServantExecutionResult castExampleSwordNp(ServantNoblePhantasmContext context) {
      ServantEntity caster = context.caster();
      LivingEntity target = context.target();
      if (caster == null || target == null || context.noblePhantasmDefinition() == null) {
         return ServantExecutionResult.NOT_HANDLED;
      }

      double cost = context.noblePhantasmDefinition().mpCost();
      if (context.currentMp() < cost) {
         return ServantExecutionResult.FAILED;
      }

      caster.faceToward(target.position());
      caster.triggerHorizontalSwingAnimation();
      float damage = (float)(18.0 + context.overChargeLevel() * 6.0);
      target.invulnerableTime = 0;
      target.hurt(caster.damageSources().magic(), damage);
      pushTargetAway(caster, target, 1.4, 0.45);

      if (caster.level() instanceof ServerLevel level) {
         Vec3 center = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
         level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 36, 1.4, 0.5, 1.4, 0.08);
         level.playSound(null, target.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.4F);
      }
      return ServantExecutionResult.SUCCESS.withMpCost(cost);
   }

   private static void pushTargetAway(ServantEntity caster, LivingEntity target, double horizontalStrength, double verticalStrength) {
      Vec3 dir = target.position().subtract(caster.position());
      Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z);
      if (horizontal.lengthSqr() > 1.0E-4) {
         horizontal = horizontal.normalize();
         target.push(horizontal.x * horizontalStrength, verticalStrength, horizontal.z * horizontalStrength);
         target.hurtMarked = true;
      }
   }
}
