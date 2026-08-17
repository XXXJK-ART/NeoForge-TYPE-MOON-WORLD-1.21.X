package net.xxxjk.TYPE_MOON_WORLD.servant.ai;

import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ArtoriaPendragonEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CasterGilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CursedArmHassanEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EmiyaArcherEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.EnkiduEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.GilgameshEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.MedusaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OdaNobunagaEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberCombatHelper;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.OkitaSoujiSaberEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.PaleRiderEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;
import net.xxxjk.TYPE_MOON_WORLD.servant.palerider.PaleRiderCombatHelper;

/** Advances servant-specific passive and already-started states before AI arbitration. */
public final class ServantSpecialStateService {
   private ServantSpecialStateService() { }

   public static void tickBeforeAi(ServantEntity entity) {
      if (entity == null || entity.level().isClientSide() || !entity.isAlive()
         || entity.isSpiritualDissolving()) return;
      if (entity instanceof ArtoriaPendragonEntity artoria) {
         ArtoriaPendragonCombatHelper.tickPersistentState(artoria);
      } else if (entity instanceof CasterGilgameshEntity casterGilgamesh) {
         CasterGilgameshCombatHelper.tickPersistentState(casterGilgamesh);
      } else if (entity instanceof GilgameshEntity gilgamesh) {
         GilgameshCombatHelper.tickPersistentState(gilgamesh);
      } else if (entity instanceof EnkiduEntity enkidu) {
         EnkiduCombatHelper.tickPersistentState(enkidu);
      } else if (entity instanceof MedusaEntity medusa) {
         MedusaCombatHelper.tickPersistentState(medusa);
      } else if (entity instanceof CursedArmHassanEntity cursedArm) {
         CursedArmHassanCombatHelper.tickPersistentState(cursedArm);
      } else if (entity instanceof EmiyaArcherEntity emiya) {
         EmiyaArcherCombatHelper.tickPersistentState(emiya);
      } else if (entity instanceof OdaNobunagaEntity oda) {
         OdaNobunagaCombatHelper.tickPersistentState(oda);
      } else if (entity instanceof OkitaSoujiSaberEntity okita) {
         OkitaSoujiSaberCombatHelper.tickPersistentState(okita);
      } else if (entity instanceof PaleRiderEntity paleRider) {
         PaleRiderCombatHelper.tickPersistentState(paleRider);
      }
   }
}
