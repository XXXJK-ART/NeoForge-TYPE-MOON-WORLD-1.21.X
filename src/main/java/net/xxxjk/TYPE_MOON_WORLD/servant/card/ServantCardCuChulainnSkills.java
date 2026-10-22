package net.xxxjk.TYPE_MOON_WORLD.servant.card;

import net.minecraft.server.level.ServerPlayer;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.cu.CuChulainnAbilityService;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.CuChulainnCombatHelper;

/** Compatibility facade. The character executor owns acceptance, payment and timing. */
public final class ServantCardCuChulainnSkills {
   public static final String CU_RUNE_ALGIZ_SHIELD_TAG=CuChulainnCombatHelper.ALGIZ_SHIELD_TAG;
   public static final String CU_RUNE_BARRIER_HP_TAG=CuChulainnCombatHelper.RUNE_BARRIER_HP_TAG;
   public static final String CU_RUNE_BARRIER_UNTIL_TAG=CuChulainnCombatHelper.RUNE_BARRIER_UNTIL_TAG;
   public static final String CU_RUNE_BARRIER_X_TAG=CuChulainnCombatHelper.RUNE_BARRIER_X_TAG;
   public static final String CU_RUNE_BARRIER_Y_TAG=CuChulainnCombatHelper.RUNE_BARRIER_Y_TAG;
   public static final String CU_RUNE_BARRIER_Z_TAG=CuChulainnCombatHelper.RUNE_BARRIER_Z_TAG;
   private ServantCardCuChulainnSkills() { }
   public static void tick(ServerPlayer player,TypeMoonWorldModVariables.PlayerVariables vars) { }
   public static void clear(ServerPlayer player) { CuChulainnAbilityService.clear(player); }
   public static void markCombat(ServerPlayer player) { CuChulainnAbilityService.actualDamage(player); }
   public static void clearRuneBarrier(ServerPlayer player) { CuChulainnAbilityService.clearBarrier(player); }
   public static void performAnsuzRune(ServerPlayer p) { CuChulainnAbilityService.start(p,0); }
   public static void performLaguzRune(ServerPlayer p) { CuChulainnAbilityService.start(p,1); }
   public static void performTiwazRune(ServerPlayer p) { CuChulainnAbilityService.start(p,2); }
   public static void performAlgizRune(ServerPlayer p) { CuChulainnAbilityService.start(p,3); }
   public static void performBerkanaRune(ServerPlayer p) { CuChulainnAbilityService.start(p,4); }
   public static void performDisengage(ServerPlayer p) { CuChulainnAbilityService.start(p,5); }
   public static void performRuneBarrier(ServerPlayer p) { CuChulainnAbilityService.start(p,6); }
   public static void performCrouchThrust(ServerPlayer p) { CuChulainnAbilityService.start(p,-1); }
}
