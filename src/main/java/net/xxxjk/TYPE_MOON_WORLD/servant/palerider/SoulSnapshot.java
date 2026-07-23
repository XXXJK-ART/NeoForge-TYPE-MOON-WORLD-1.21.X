package net.xxxjk.TYPE_MOON_WORLD.servant.palerider;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;
import net.xxxjk.TYPE_MOON_WORLD.servant.entity.ServantEntity;

public record SoulSnapshot(
   UUID id,
   String entityType,
   String displayName,
   SoulKind kind,
   String playerProfile,
   String playerUuid,
   float width,
   float height,
   double maxHealth,
   double attackDamage,
   double movementSpeed,
   double armor,
   double armorToughness,
   double knockbackResistance
) {
   public static SoulSnapshot capture(LivingEntity entity) {
      ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
      SoulKind kind = SoulKind.CREATURE;
      String profile = "";
      String playerUuid = "";
      if (entity instanceof Player player) {
         TypeMoonWorldModVariables.PlayerVariables vars = player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
         kind = vars.servant_card_transformed ? SoulKind.SERVANT : vars.master_card_active || vars.is_magic_circuit_open ? SoulKind.MAGICIAN : SoulKind.PLAYER;
         profile = player.getGameProfile().getName();
         playerUuid = player.getUUID().toString();
      } else if (entity instanceof ServantEntity) {
         kind = SoulKind.SERVANT;
      }
      return new SoulSnapshot(
         UUID.randomUUID(),
         type == null ? "minecraft:zombie" : type.toString(),
         entity.getDisplayName().getString(),
         kind,
         profile,
         playerUuid,
         entity.getBbWidth(),
         entity.getBbHeight(),
         Math.max(1.0, entity.getMaxHealth()),
         attribute(entity, Attributes.ATTACK_DAMAGE, 2.0),
         attribute(entity, Attributes.MOVEMENT_SPEED, 0.23),
         attribute(entity, Attributes.ARMOR, 0.0),
         attribute(entity, Attributes.ARMOR_TOUGHNESS, 0.0),
         attribute(entity, Attributes.KNOCKBACK_RESISTANCE, 0.0)
      );
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putUUID("Id", this.id);
      tag.putString("EntityType", this.entityType);
      tag.putString("DisplayName", this.displayName);
      tag.putString("Kind", this.kind.name());
      tag.putString("PlayerProfile", this.playerProfile);
      tag.putString("PlayerUuid", this.playerUuid);
      tag.putFloat("Width", this.width);
      tag.putFloat("Height", this.height);
      tag.putDouble("MaxHealth", this.maxHealth);
      tag.putDouble("AttackDamage", this.attackDamage);
      tag.putDouble("MovementSpeed", this.movementSpeed);
      tag.putDouble("Armor", this.armor);
      tag.putDouble("ArmorToughness", this.armorToughness);
      tag.putDouble("KnockbackResistance", this.knockbackResistance);
      return tag;
   }

   public static SoulSnapshot load(CompoundTag tag) {
      SoulKind kind;
      try {
         kind = SoulKind.valueOf(tag.getString("Kind"));
      } catch (IllegalArgumentException ignored) {
         kind = SoulKind.CREATURE;
      }
      return new SoulSnapshot(
         tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
         tag.getString("EntityType"),
         tag.getString("DisplayName"),
         kind,
         tag.getString("PlayerProfile"),
         tag.getString("PlayerUuid"),
         tag.getFloat("Width"),
         tag.getFloat("Height"),
         tag.getDouble("MaxHealth"),
         tag.getDouble("AttackDamage"),
         tag.getDouble("MovementSpeed"),
         tag.getDouble("Armor"),
         tag.getDouble("ArmorToughness"),
         tag.getDouble("KnockbackResistance")
      );
   }

   @SuppressWarnings("unchecked")
   public static SoulSnapshot fromEntityType(String entityType, SoulKind kind) {
      ResourceLocation id = ResourceLocation.tryParse(entityType);
      EntityType<?> type = id == null ? EntityType.ZOMBIE : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(EntityType.ZOMBIE);
      EntityDimensions dimensions = type.getDimensions();
      AttributeSupplier attributes = DefaultAttributes.hasSupplier(type)
         ? DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>)type) : null;
      return new SoulSnapshot(UUID.randomUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
         type.getDescription().getString(), kind, "", "", dimensions.width(), dimensions.height(),
         defaultAttribute(attributes, Attributes.MAX_HEALTH, 20.0),
         defaultAttribute(attributes, Attributes.ATTACK_DAMAGE, 2.0),
         defaultAttribute(attributes, Attributes.MOVEMENT_SPEED, 0.23),
         defaultAttribute(attributes, Attributes.ARMOR, 0.0),
         defaultAttribute(attributes, Attributes.ARMOR_TOUGHNESS, 0.0),
         defaultAttribute(attributes, Attributes.KNOCKBACK_RESISTANCE, 0.0));
   }

   public double threat() {
      return this.maxHealth * 0.8 + this.attackDamage * 6.0 + this.armor * 2.0 + this.armorToughness * 2.5;
   }

   private static double attribute(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double fallback) {
      return entity.getAttribute(attribute) == null ? fallback : entity.getAttributeValue(attribute);
   }

   private static double defaultAttribute(AttributeSupplier attributes,
      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double fallback) {
      return attributes != null && attributes.hasAttribute(attribute) ? attributes.getBaseValue(attribute) : fallback;
   }

   public enum SoulKind {
      CREATURE,
      PLAYER,
      MAGICIAN,
      SERVANT
   }
}
