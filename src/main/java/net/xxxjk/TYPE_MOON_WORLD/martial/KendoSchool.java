package net.xxxjk.TYPE_MOON_WORLD.martial;

import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** The two schools taught by the generated sword dojos. */
public enum KendoSchool {
   HOKUSHIN("hokushin_ittoryu", "北辰一刀流", 50.0),
   TENNEN("tennen_rishin_ryu", "天然理心流", 80.0);

   private final String id;
   private final String displayName;
   private final double preMasterCap;

   KendoSchool(String id, String displayName, double preMasterCap) {
      this.id = id;
      this.displayName = displayName;
      this.preMasterCap = preMasterCap;
   }

   public String id() { return id; }
   public String displayName() { return displayName; }
   public double preMasterCap() { return preMasterCap; }

   public boolean learned(TypeMoonWorldModVariables.PlayerVariables vars) {
      return this == HOKUSHIN ? vars.hokushin_learned : vars.tennen_learned;
   }

   public double proficiency(TypeMoonWorldModVariables.PlayerVariables vars) {
      return this == HOKUSHIN ? vars.hokushin_proficiency : vars.tennen_proficiency;
   }

   public boolean masterDefeated(TypeMoonWorldModVariables.PlayerVariables vars) {
      return this == HOKUSHIN ? vars.hokushin_master_defeated : vars.tennen_master_defeated;
   }
}
