package net.xxxjk.TYPE_MOON_WORLD.network.state;

import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Domain view for analyzed/projection selections. */
public final class ProjectionState {
   private final TypeMoonWorldModVariables.PlayerVariables vars;
   public ProjectionState(TypeMoonWorldModVariables.PlayerVariables vars) { this.vars = vars; }
   public ItemStack selectedItem() { return vars.projection_selected_item; }
   public String selectedStructureId() { return vars.projection_selected_structure_id; }
   public void setSelectedItem(ItemStack value) { vars.projection_selected_item = value == null ? ItemStack.EMPTY : value; vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.PROJECTION); }
   public void setSelectedStructureId(String value) { vars.projection_selected_structure_id = value == null ? "" : value; vars.markDirty(TypeMoonWorldModVariables.PlayerVariables.StateDomain.PROJECTION); }
}
