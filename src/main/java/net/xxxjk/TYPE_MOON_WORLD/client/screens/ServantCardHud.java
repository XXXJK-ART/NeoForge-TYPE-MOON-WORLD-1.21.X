package net.xxxjk.TYPE_MOON_WORLD.client.screens;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.xxxjk.TYPE_MOON_WORLD.client.ReplayUiSuppressor;
import net.xxxjk.TYPE_MOON_WORLD.client.PaleRiderClientState;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardCasterGilgameshSkills;
import net.xxxjk.TYPE_MOON_WORLD.servant.card.ServantCardTransformManager;

@EventBusSubscriber({Dist.CLIENT})
public class ServantCardHud {
   private static final int DISPLAY_READY_THRESHOLD_TICKS = 10;
   private static String cachedCooldownRaw = null;
   private static int[] cachedCooldowns = new int[10];
   private static String cachedCooldownEndsRaw = null;
   private static long[] cachedCooldownEnds = new long[10];
   private static final int[] effectiveCooldowns = new int[10];

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = minecraft.player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed && !vars.master_active) {
         return;
      }
      ResourceLocation layer = event.getName();
      if (VanillaGuiLayers.PLAYER_HEALTH.equals(layer) || VanillaGuiLayers.ARMOR_LEVEL.equals(layer) || VanillaGuiLayers.FOOD_LEVEL.equals(layer)) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.NORMAL)
   public static void onRenderGui(RenderGuiEvent.Pre event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.hideGui || ReplayUiSuppressor.shouldHideTypeMoonHud()) {
         return;
      }
      Player player = minecraft.player;
      if (player == null) {
         return;
      }
      TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
      if (!vars.servant_card_transformed && !vars.master_active) {
         return;
      }

      GuiGraphics gui = event.getGuiGraphics();
      int guiWidth = gui.guiWidth();
      int guiHeight = gui.guiHeight();
      if (isSurvivalLike(minecraft)) {
         int statusWidth = 81;
         int leftStatusX = guiWidth / 2 - 91;
         int baseStatusY = guiHeight - 39;
         drawBar(gui, minecraft, leftStatusX, baseStatusY, statusWidth, "HP", player.getHealth(), player.getMaxHealth(), 0xFFB71C1C, 0xFFE53935);
         drawBar(gui, minecraft, leftStatusX, baseStatusY - 10, statusWidth, "DEF", player.getArmorValue(), 20.0, 0xFF607D8B, 0xFFECEFF1);
      }

      int manaX = 10;
      int manaY = guiHeight - 20;
      if (vars.master_active) {
         if (vars.master_servant_link_partner_max_mana > 0.0) {
            drawBar(gui, minecraft, manaX, manaY, 120, linkLabel("Servant", vars), vars.master_servant_link_partner_mana, vars.master_servant_link_partner_max_mana, 0xFF00838F, 0xFF00E5FF);
         }
      } else {
         drawBar(gui, minecraft, manaX, manaY, 120, "MP", vars.servant_card_mana, vars.servant_card_max_mana, 0xFF00838F, 0xFF00E5FF);
         if (vars.master_servant_link_partner_max_mana > 0.0) {
            drawBar(gui, minecraft, manaX, manaY - 11, 120, linkLabel("Master", vars), vars.master_servant_link_partner_mana, vars.master_servant_link_partner_max_mana, 0xFF6A1B9A, 0xFFCE93D8);
         }
      }

      int x = 6;
      int y = 10;
      if (vars.master_active) {
         drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.master.status"), x, y + 38, 0xFFFFFFFF, 0.72F);
         drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.master.command_spells", vars.master_command_spells), x, y + 48, 0xFFE0E0E0, 0.62F);
         drawServantPosition(gui, minecraft, vars, x, y + 58);
         return;
      }
      String servant = vars.servant_card_id == null || vars.servant_card_id.isBlank() ? "servant" : vars.servant_card_id;
      Component servantName = "servant".equals(servant) ? Component.literal(servant) : Component.translatable("item.typemoonworld.servant_card_" + servant);
      drawScaledString(gui, minecraft, servantName, x, y + 38, 0xFFFFFFFF, 0.72F);
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.jump_np", effectiveJumpCharges(minecraft, vars), ticksToSeconds(effectiveNpCooldown(minecraft, vars))),
         x,
         y + 48,
         0xFFE0E0E0,
         0.62F
      );
      drawFlightStatus(gui, minecraft, vars, x, y + 58);
      drawPaleRiderStatus(gui, minecraft, vars, x, y + 58);
      drawMasterPosition(gui, minecraft, vars, x, y + 68);
      drawMasterLossStatus(gui, minecraft, vars, x, y + 78);
      drawCooldownGrid(gui, minecraft, vars, 5, 98);
      drawMedeaStocks(gui, minecraft, vars, guiWidth, 36);
      drawParacelsusStocks(gui, minecraft, vars, guiWidth, 36);
      drawArashArrows(gui, minecraft, vars, guiWidth, 36);
      drawCasterGilgameshRoyalCannon(gui, minecraft, vars, guiWidth, 36);
      drawGilgameshOmniscience(gui, minecraft, vars);
   }

   private static void drawGilgameshOmniscience(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (!("gilgamesh".equals(vars.servant_card_id) || "gilgamesh_caster".equals(vars.servant_card_id))
         || minecraft.level == null || minecraft.player == null) return;
      HitResult hit = minecraft.hitResult;
      ItemEntity lookedAtItem = findLookedAtItem(minecraft, hit);
      if ((hit == null || hit.getType() == HitResult.Type.MISS) && lookedAtItem == null) return;
      List<String> lines = new java.util.ArrayList<>();
      ItemStack icon = ItemStack.EMPTY;
      if (lookedAtItem != null) {
         ItemStack stack = lookedAtItem.getItem();
         icon = stack;
         lines.add(stack.getHoverName().getString());
         lines.add(Component.translatable("hud.typemoonworld.gilgamesh.item_count", stack.getCount()).getString());
         lines.add(Component.translatable("hud.typemoonworld.gilgamesh.item_id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).getString());
         if (stack.isDamageableItem()) lines.add(Component.translatable("hud.typemoonworld.gilgamesh.durability", stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()).getString());
      } else if (hit instanceof BlockHitResult block) {
         lines.add(minecraft.level.getBlockState(block.getBlockPos()).getBlock().getName().getString());
         lines.add(Component.translatable("hud.typemoonworld.gilgamesh.block_id", BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(block.getBlockPos()).getBlock()).toString()).getString());
      } else if (hit.getType() == HitResult.Type.ENTITY) {
         net.minecraft.world.entity.Entity entity = ((net.minecraft.world.phys.EntityHitResult)hit).getEntity();
         if (entity instanceof LivingEntity living) {
            lines.add(living.getName().getString());
            lines.add(Component.translatable("hud.typemoonworld.gilgamesh.entity_type", living.getType().getDescription()).getString());
            lines.add(Component.translatable("hud.typemoonworld.gilgamesh.distance", minecraft.player.distanceTo(living)).getString());
            lines.add(String.format(java.util.Locale.ROOT, "HP %.1f / %.1f", living.getHealth(), living.getMaxHealth()));
            lines.add("DEF " + living.getArmorValue());
            lines.add(Component.translatable("hud.typemoonworld.gilgamesh.toughness", living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)).getString());
            if (!living.getMainHandItem().isEmpty()) lines.add(Component.translatable("hud.typemoonworld.gilgamesh.mainhand", living.getMainHandItem().getHoverName()).getString());
            if (!living.getOffhandItem().isEmpty()) lines.add(Component.translatable("hud.typemoonworld.gilgamesh.offhand", living.getOffhandItem().getHoverName()).getString());
            if (!living.getMainHandItem().isEmpty()) icon = living.getMainHandItem();
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
               ItemStack equipped = living.getItemBySlot(slot);
               if (!equipped.isEmpty()) lines.add(Component.translatable("hud.typemoonworld.gilgamesh.equipment." + slot.getName(), equipped.getHoverName()).getString());
            }
            if (!living.getActiveEffects().isEmpty()) {
               lines.add(Component.translatable("hud.typemoonworld.gilgamesh.effects").getString());
               for (net.minecraft.world.effect.MobEffectInstance effect : living.getActiveEffects()) {
                  lines.add(Component.translatable("hud.typemoonworld.gilgamesh.effect", effect.getEffect().value().getDisplayName(), effect.getAmplifier() + 1, String.format(java.util.Locale.ROOT, "%.1f", effect.getDuration() / 20.0F)).getString());
               }
            }
         } else if (entity instanceof ItemEntity item) {
            icon = item.getItem();
            lines.add(item.getItem().getHoverName().getString());
            lines.add(Component.translatable("hud.typemoonworld.gilgamesh.item_count", item.getItem().getCount()).getString());
         }
      }
      if (lines.isEmpty()) return;
      int width = 0; for (String line : lines) width = Math.max(width, minecraft.font.width(line));
      if (!icon.isEmpty()) width += 22;
      int height = lines.size() * 10 + 7;
      int x = Math.max(4, Math.min(gui.guiWidth() / 2 + 14, gui.guiWidth() - width - 8));
      int y = Math.max(6, Math.min(gui.guiHeight() / 2 + 10, gui.guiHeight() - height - 6));
      gui.fill(x - 4, y - 4, x + width + 5, y + lines.size() * 10 + 3, 0xB0181820);
      gui.renderOutline(x - 4, y - 4, width + 9, lines.size() * 10 + 7, 0xFFD4AF37);
      for (int i = 0; i < lines.size(); i++) gui.drawString(minecraft.font, lines.get(i), x, y + i * 10, i == 0 ? 0xFFFFD54F : 0xFFE8E8E8, true);
      if (!icon.isEmpty()) gui.renderItem(icon, x + width - 18, y);
   }

   private static ItemEntity findLookedAtItem(Minecraft minecraft, HitResult hit) {
      if (minecraft.player == null || minecraft.level == null) return null;
      Vec3 eye = minecraft.player.getEyePosition();
      Vec3 look = minecraft.player.getLookAngle().normalize();
      Vec3 end = eye.add(look.scale(16.0));
      double hitDistance = hit == null || hit.getType() == HitResult.Type.MISS ? 16.0 : Math.sqrt(hit.distanceTo(minecraft.player));
      ItemEntity best = null;
      double bestDistance = hitDistance + .5;
      for (ItemEntity item : minecraft.level.getEntitiesOfClass(ItemEntity.class, new AABB(eye, end).inflate(1.0), ItemEntity::isAlive)) {
         Vec3 delta = item.position().add(0.0, item.getBbHeight() * .5, 0.0).subtract(eye);
         double along = delta.dot(look);
         if (along < 0.0 || along > bestDistance) continue;
         if (delta.subtract(look.scale(along)).lengthSqr() > .8 * .8) continue;
         best = item;
         bestDistance = along;
      }
      return best;
   }

   private static void drawFlightStatus(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      if (!"medea".equals(vars.servant_card_id) && !"enkidu".equals(vars.servant_card_id) && !"oda_nobunaga".equals(vars.servant_card_id) && !"gilgamesh".equals(vars.servant_card_id) && !"gilgamesh_caster".equals(vars.servant_card_id)) return;
      long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
      long high = vars.servant_card_flight_mode == 2
         ? Math.max(0L, vars.servant_card_high_flight_until - now)
         : vars.servant_card_oda_flight_ticks;
      long cd = Math.max(0L, vars.servant_card_oda_flight_cooldown_until - now);
      Component text = Component.translatable("hud.typemoonworld.servant_card.flight", vars.servant_card_flight_mode, ticksToSeconds((int)Math.min(Integer.MAX_VALUE, high)), ticksToSeconds((int)Math.min(Integer.MAX_VALUE, cd)));
      drawScaledString(gui, minecraft, text, x, y, 0xFFBFE8FF, 0.54F);
   }

   private static void drawPaleRiderStatus(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      if (!"pale_rider".equals(vars.servant_card_id)) return;
      Component text = Component.translatable("hud.typemoonworld.servant_card.pale_rider_status",
         PaleRiderClientState.controlledCount,
         PaleRiderClientState.possessing ? "P" : "-",
         PaleRiderClientState.underworld ? "U" : "-",
         PaleRiderClientState.calamity ? "C" : "-");
      drawScaledString(gui, minecraft, text, x, y, 0xFFC8C8D0, 0.54F);
   }

   private static boolean isSurvivalLike(Minecraft minecraft) {
      if (minecraft.gameMode == null) {
         return true;
      }
      GameType mode = minecraft.gameMode.getPlayerMode();
      return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
   }

   private static String linkLabel(String base, TypeMoonWorldModVariables.PlayerVariables vars) {
      return switch (vars.master_servant_link_state == null ? "" : vars.master_servant_link_state) {
         case "normal" -> base + " [G]";
         case "unstable" -> base + " [Y]";
         case "independent_action" -> base + " [Y]";
         case "broken", "forced_death", "decaying" -> base + " [R]";
         default -> base;
      };
   }

   private static void drawMasterLossStatus(GuiGraphics gui, Minecraft minecraft,
                                            TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      String state = vars.master_servant_survival_state == null ? "none" : vars.master_servant_survival_state;
      if ("independent_action".equals(state)) {
         drawScaledString(gui, minecraft,
            Component.translatable("hud.typemoonworld.servant_card.independent_action", ticksToDuration(vars.master_servant_survival_ticks)),
            x, y, 0xFFFFD54F, 0.58F);
      } else if ("forced_death".equals(state)) {
         drawScaledString(gui, minecraft,
            Component.translatable("hud.typemoonworld.servant_card.forced_death", ticksToSeconds(vars.master_servant_survival_ticks)),
            x, y, 0xFFFF5252, 0.58F);
      } else if ("decaying".equals(state)) {
         drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.decaying"),
            x, y, 0xFFFF1744, 0.58F);
      }
   }

   private static void drawMasterPosition(GuiGraphics gui, Minecraft minecraft,
                                          TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      if (!vars.master_servant_master_position_valid || minecraft.player == null) return;
      int masterX = (int)Math.floor(vars.master_servant_master_x);
      int masterY = (int)Math.floor(vars.master_servant_master_y);
      int masterZ = (int)Math.floor(vars.master_servant_master_z);
      String currentDimension = minecraft.player.level().dimension().location().toString();
      Component freshness = Component.translatable(vars.master_servant_master_position_online
         ? "hud.typemoonworld.servant_card.master_position_live"
         : "hud.typemoonworld.servant_card.master_position_last_known");
      Component text;
      if (currentDimension.equals(vars.master_servant_master_dimension)) {
         double dx = vars.master_servant_master_x - minecraft.player.getX();
         double dz = vars.master_servant_master_z - minecraft.player.getZ();
         int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
         text = Component.translatable("hud.typemoonworld.servant_card.master_position",
            compassDirection(dx, dz), distance, masterX, masterY, masterZ, freshness);
      } else {
         text = Component.translatable("hud.typemoonworld.servant_card.master_position_dimension",
            vars.master_servant_master_dimension, masterX, masterY, masterZ, freshness);
      }
      drawScaledString(gui, minecraft, text, x, y,
         vars.master_servant_master_position_online ? 0xFF80CBC4 : 0xFFB0BEC5, 0.54F);
   }

   private static void drawServantPosition(GuiGraphics gui, Minecraft minecraft,
                                           TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      if (!vars.master_servant_servant_position_valid || minecraft.player == null) return;
      int servantX = (int)Math.floor(vars.master_servant_servant_x);
      int servantY = (int)Math.floor(vars.master_servant_servant_y);
      int servantZ = (int)Math.floor(vars.master_servant_servant_z);
      String currentDimension = minecraft.player.level().dimension().location().toString();
      Component freshness = Component.translatable(vars.master_servant_servant_position_online
         ? "hud.typemoonworld.servant_card.servant_position_live"
         : "hud.typemoonworld.servant_card.servant_position_last_known");
      Component text;
      if (currentDimension.equals(vars.master_servant_servant_dimension)) {
         double dx = vars.master_servant_servant_x - minecraft.player.getX();
         double dz = vars.master_servant_servant_z - minecraft.player.getZ();
         int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
         text = Component.translatable("hud.typemoonworld.servant_card.servant_position",
            compassDirection(dx, dz), distance, servantX, servantY, servantZ, freshness);
      } else {
         text = Component.translatable("hud.typemoonworld.servant_card.servant_position_dimension",
            vars.master_servant_servant_dimension, servantX, servantY, servantZ, freshness);
      }
      drawScaledString(gui, minecraft, text, x, y,
         vars.master_servant_servant_position_online ? 0xFF80CBC4 : 0xFFB0BEC5, 0.54F);
   }

   private static String compassDirection(double dx, double dz) {
      if (dx * dx + dz * dz < 1.0) return "HERE";
      String[] directions = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
      int index = Math.floorMod((int)Math.round(Math.atan2(dz, dx) / (Math.PI / 4.0)), directions.length);
      return directions[index];
   }

   private static String ticksToDuration(int ticks) {
      // Survival timers use Minecraft time: 24,000 ticks per in-game day and
      // 1,000 ticks per displayed in-game hour.  Do not convert this value to
      // wall-clock seconds (that would make a seven-day timer look like hours).
      long remainingTicks = Math.max(0L, ticks);
      long days = remainingTicks / 24000L;
      long hours = remainingTicks % 24000L / 1000L;
      return days > 0 ? days + "d " + hours + "h" : hours + "h";
   }

   private static int effectiveNpCooldown(Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars) {
      if ("ushiwakamaru_rider".equals(vars.servant_card_id)) {
         return effectiveSkillCooldowns(minecraft, vars)[9];
      }
      int syncedCooldown = effectiveRemainingTicks(minecraft, vars.servant_card_np_cooldown, vars.servant_card_np_cooldown_end);
      if (minecraft.player == null) {
         return displayRemainingTicks(syncedCooldown);
      }
      int itemCooldown = 0;
      if (!minecraft.player.getMainHandItem().isEmpty()) {
         itemCooldown = Math.max(itemCooldown, Math.round(minecraft.player.getCooldowns().getCooldownPercent(minecraft.player.getMainHandItem().getItem(), 0.0F) * 3600.0F));
      }
      if (!minecraft.player.getOffhandItem().isEmpty()) {
         itemCooldown = Math.max(itemCooldown, Math.round(minecraft.player.getCooldowns().getCooldownPercent(minecraft.player.getOffhandItem().getItem(), 0.0F) * 3600.0F));
      }
      return displayRemainingTicks(Math.max(syncedCooldown, itemCooldown));
   }

   private static void drawBar(GuiGraphics gui, Minecraft minecraft, int x, int y, int width, String label, double value, double max, int startColor, int endColor) {
      int height = 8;
      gui.fill(x, y, x + width, y + height, 0x90000000);
      if (max > 0.0) {
         int fill = (int)(width * Math.max(0.0, Math.min(1.0, value / max)));
         gui.fillGradient(x, y, x + fill, y + height, startColor, endColor);
      }
      gui.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xCCFFFFFF);
      String text = label + " " + (int)value + "/" + (int)Math.max(0.0, max);
      float scale = 0.62F;
      int textX = x + (int)((width - minecraft.font.width(text) * scale) / 2.0F);
      drawScaledString(gui, minecraft, Component.literal(text), textX, y + 1, 0xFFFFFFFF, scale);
   }

   private static String ticksToSeconds(int ticks) {
      if (ticks <= 0) {
         return Component.translatable("hud.typemoonworld.servant_card.ready").getString();
      }
      return String.format(java.util.Locale.ROOT, "%.1fs", ticks / 20.0F);
   }

   private static void drawCooldownGrid(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int x, int y) {
      int[] cooldowns = effectiveSkillCooldowns(minecraft, vars);
      for (int i = 0; i < 10; i++) {
         int drawX = x;
         int drawY = y + i * 8;
         int ticks = i == 9 && !"gilgamesh".equals(vars.servant_card_id)
            && !"gilgamesh_caster".equals(vars.servant_card_id)
            && !"ushiwakamaru_rider".equals(vars.servant_card_id) ? effectiveNpCooldown(minecraft, vars) : cooldowns[i];
         String skillKey = ServantCardTransformManager.skillTranslationKey(vars.servant_card_id, i, false);
         boolean empty = skillKey.isBlank();
         Component label = empty
            ? Component.translatable("hud.typemoonworld.servant_card.none")
            : (ticks <= 0 ? Component.translatable(skillKey) : Component.literal(ticksToSeconds(ticks)));
         Component text = TypeMoonWorldModKeyMappings.SERVANT_CARD_SKILL_KEYS[i]
            .getTranslatedKeyMessage()
            .copy()
            .append(":")
            .append(label);
         int color = empty ? 0xFF888888 : ticks <= 0 ? 0xFFD8F8D8 : 0xFFFFD180;
         float scale = 0.54F;
         int width = Math.min(142, Math.max(38, (int)(minecraft.font.width(text) * scale) + 5));
         gui.fill(drawX - 1, drawY - 1, drawX + width, drawY + 6, 0x44000000);
         drawScaledString(gui, minecraft, text, drawX + 1, drawY - 1, color, scale);
      }
   }

   private static void drawMedeaStocks(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int guiWidth, int y) {
      if (!"medea".equals(vars.servant_card_id)) {
         return;
      }
      int x = guiWidth - 66;
      gui.fill(x - 3, y - 4, guiWidth - 5, y + 31, 0x44000000);
      drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.medea_items"), x, y - 2, 0xFFE6D8FF, 0.62F);
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_dragonfang_count", vars.servant_card_medea_dragonfang_stock),
         x,
         y + 8,
         0xFFD8F8FF,
         0.58F
      );
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_mana_charm_count", vars.servant_card_medea_mana_charm_stock),
         x,
         y + 17,
         0xFFBFEFFF,
         0.58F
      );
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.medea_heal_charm_count", vars.servant_card_medea_heal_charm_stock),
         x,
         y + 26,
         0xFFC8FFC8,
         0.58F
      );
   }

   private static void drawParacelsusStocks(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int guiWidth, int y) {
      if (!"paracelsus".equals(vars.servant_card_id)) {
         return;
      }
      int x = guiWidth - 78;
      gui.fill(x - 3, y - 4, guiWidth - 5, y + 22, 0x44000000);
      drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.paracelsus_items"), x, y - 2, 0xFFE6D8FF, 0.62F);
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.paracelsus_stone_count", vars.servant_card_paracelsus_stone_stock),
         x,
         y + 8,
         0xFFFFE0A8,
         0.58F
      );
      drawScaledString(
         gui,
         minecraft,
         Component.translatable("hud.typemoonworld.servant_card.paracelsus_diamond_shield_count", vars.servant_card_paracelsus_diamond_shield_stock),
         x,
         y + 17,
         0xFFBFEFFF,
         0.58F
      );
   }

   private static void drawArashArrows(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int guiWidth, int y) {
      if (!"arash".equals(vars.servant_card_id)) return;
      int arrows = Math.max(0, Math.min(5000, vars.servant_card_arash_arrow_stock));
      int panelWidth = 96;
      int x = guiWidth - panelWidth - 6;
      gui.fill(x - 3, y - 4, guiWidth - 5, y + 20, 0x66000000);
      gui.renderOutline(x - 3, y - 4, panelWidth + 1, 24, arrows == 0 ? 0xFFFF4040 : 0xFFE0C080);
      drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.arash_arrows"),
         x, y - 2, 0xFFFFE0A8, 0.62F);
      int color = arrows == 0 ? 0xFFFF5555 : arrows < 50 ? 0xFFFFAA55 : 0xFFFFFFFF;
      drawScaledString(gui, minecraft,
         Component.translatable("hud.typemoonworld.servant_card.arash_arrow_count", arrows, 5000),
         x, y + 8, color, 0.62F);
      if (arrows == 0) {
         drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.arash_empty"),
            x, y + 16, 0xFFFF7777, 0.50F);
      }
   }

   private static void drawCasterGilgameshRoyalCannon(GuiGraphics gui, Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars, int guiWidth, int y) {
      if (!"gilgamesh_caster".equals(vars.servant_card_id)) return;
      int maxAmmo = ServantCardCasterGilgameshSkills.MAX_AMMO;
      int ammo = Math.max(0, Math.min(maxAmmo, vars.servant_card_royal_cannon_ammo));
      int panelWidth = 112;
      int x = guiWidth - panelWidth - 6;
      int outline = ammo < ServantCardCasterGilgameshSkills.CANNON_SHOTS_PER_ROUND ? 0xFFFF4040 : 0xFFECC84A;
      gui.fill(x - 3, y - 4, guiWidth - 5, y + 25, 0x66000000);
      gui.renderOutline(x - 3, y - 4, panelWidth + 1, 29, outline);
      drawScaledString(gui, minecraft, Component.translatable("hud.typemoonworld.servant_card.royal_cannon"),
         x, y - 2, 0xFFFFD85C, 0.62F);
      int color = ammo < ServantCardCasterGilgameshSkills.CANNON_SHOTS_PER_ROUND ? 0xFFFF5555 : ammo < 100 ? 0xFFFFAA55 : 0xFFFFFFFF;
      drawScaledString(gui, minecraft,
         Component.translatable("hud.typemoonworld.servant_card.royal_cannon_ammo", ammo, maxAmmo),
         x, y + 8, color, 0.62F);
      int barWidth = panelWidth - 8;
      int barY = y + 19;
      gui.fill(x, barY, x + barWidth, barY + 3, 0xAA2A2414);
      int fill = (int)(barWidth * Math.max(0.0F, Math.min(1.0F, ammo / (float)maxAmmo)));
      gui.fill(x, barY, x + fill, barY + 3, ammo < ServantCardCasterGilgameshSkills.CANNON_SHOTS_PER_ROUND ? 0xFFFF5555 : 0xFFFFC928);
   }

   private static void drawScaledString(GuiGraphics gui, Minecraft minecraft, Component text, int x, int y, int color, float scale) {
      gui.pose().pushPose();
      gui.pose().scale(scale, scale, 1.0F);
      gui.drawString(minecraft.font, text, (int)(x / scale), (int)(y / scale), color, true);
      gui.pose().popPose();
   }

   private static int[] parseCooldowns(String raw) {
      if (raw == null || raw.isBlank()) {
         cachedCooldownRaw = raw;
         cachedCooldowns = new int[10];
         return cachedCooldowns;
      }
      if (raw.equals(cachedCooldownRaw)) {
         return cachedCooldowns;
      }
      int[] result = new int[10];
      String[] parts = raw.split(",");
      for (int i = 0; i < result.length && i < parts.length; i++) {
         try {
            result[i] = Math.max(0, Integer.parseInt(parts[i]));
         } catch (NumberFormatException ignored) {
            result[i] = 0;
         }
      }
      cachedCooldownRaw = raw;
      cachedCooldowns = result;
      return result;
   }

   private static int[] effectiveSkillCooldowns(Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars) {
      int[] synced = parseCooldowns(vars.servant_card_skill_cooldowns);
      long[] ends = parseCooldownEnds(vars.servant_card_skill_cooldown_ends);
      for (int index = 0; index < effectiveCooldowns.length; index++) {
         effectiveCooldowns[index] = displayRemainingTicks(effectiveRemainingTicks(minecraft, synced[index], ends[index]));
      }
      return effectiveCooldowns;
   }

   private static int effectiveJumpCharges(Minecraft minecraft, TypeMoonWorldModVariables.PlayerVariables vars) {
      if (vars.servant_card_jump_recovery_end <= 0L || minecraft.level == null) return vars.servant_card_jump_charges;
      long remaining = vars.servant_card_jump_recovery_end - minecraft.level.getGameTime();
      return remaining <= DISPLAY_READY_THRESHOLD_TICKS ? vars.servant_card_jump_charges + 1 : vars.servant_card_jump_charges;
   }

   private static int effectiveRemainingTicks(Minecraft minecraft, int syncedTicks, long endTick) {
      if (endTick <= 0L || minecraft.level == null) return Math.max(0, syncedTicks);
      long remaining = endTick - minecraft.level.getGameTime();
      return remaining <= 0L ? 0 : (int)Math.min(Integer.MAX_VALUE, remaining);
   }

   private static int displayRemainingTicks(int ticks) {
      return ticks <= DISPLAY_READY_THRESHOLD_TICKS ? 0 : ticks;
   }

   private static long[] parseCooldownEnds(String raw) {
      if (raw == null || raw.isBlank()) {
         cachedCooldownEndsRaw = raw;
         cachedCooldownEnds = new long[10];
         return cachedCooldownEnds;
      }
      if (raw.equals(cachedCooldownEndsRaw)) return cachedCooldownEnds;
      long[] result = new long[10];
      String[] parts = raw.split(",");
      for (int index = 0; index < result.length && index < parts.length; index++) {
         try {
            result[index] = Math.max(0L, Long.parseLong(parts[index]));
         } catch (NumberFormatException ignored) {
            result[index] = 0L;
         }
      }
      cachedCooldownEndsRaw = raw;
      cachedCooldownEnds = result;
      return result;
   }
}
