package net.xxxjk.TYPE_MOON_WORLD.servant.summon;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Server-side catalyst mapping used by the summoning circle ritual. */
public final class SummoningRelicRegistry {
   private static final Map<ResourceLocation, List<String>> MAPPINGS = new ConcurrentHashMap<>();

   static {
      map("avalon", "artoria_pendragon");
      map("relic_round_table_fragment", "gawain");
      map("relic_bronze_mirror", "medusa");
      map("relic_old_man_mask", "cursed_arm_hassan", "shadow_hassan");
      map("relic_hajiquan_manual", "li_shuwen");
      map("relic_first_snake_skin", "gilgamesh");
      map("relic_tsubura_ship_plank", "ushiwakamaru_rider");
      map("relic_bizen_tsuba", "sasaki_kojiro");
      map("relic_bandage", "nightingale");
      map("relic_philosophers_stone", "paracelsus");
      map("relic_golden_fleece", "medea");
      map("relic_apocalypse", "pale_rider");
      map("relic_apocalypse_page", "pale_rider");
      map("gem_necklace", "emiya_archer");
      map("sea_beast_bone", "cu_chulainn");
      map("oda_matchlock_catalyst", "oda_nobunaga");
      map("broken_bowstring", "arash");
      map("ancient_temple_stone", "heracles");
      map("age_of_gods_dirt", "enkidu");
   }

   private SummoningRelicRegistry() {
   }

   private static void map(String itemId, String... servantIds) {
      MAPPINGS.put(ResourceLocation.fromNamespaceAndPath(TYPE_MOON_WORLD.MOD_ID, itemId), List.of(servantIds));
   }

   public static List<String> candidates(ItemStack stack) {
      if (stack == null || stack.isEmpty()) return List.of();
      ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? List.of() : MAPPINGS.getOrDefault(id, List.of());
   }

   public static Set<ResourceLocation> mappedItems() {
      return Set.copyOf(MAPPINGS.keySet());
   }

   public static boolean isMatchingPlayer(ServerPlayer player, String servantId) {
      if (player == null || servantId == null) return false;
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      return player.isAlive() && vars.servant_card_transformed
         && servantId.equals(normalizeServantId(vars.servant_card_id))
         && (vars.servant_card_master_uuid == null || vars.servant_card_master_uuid.isBlank());
   }

   public static String normalizeServantId(String id) {
      if (id == null || id.isBlank()) return "";
      int separator = id.indexOf(':');
      return separator >= 0 ? id.substring(separator + 1) : id;
   }
}
