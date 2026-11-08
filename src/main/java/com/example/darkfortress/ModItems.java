package com.example.darkfortress;

import com.example.darkfortress.item.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
    public static Item ESTUS_FLASK;
    public static Item SOUL_LOST_UNDEAD;
    public static Item SOUL_GREAT_DEMON;
    public static Item FORTRESS_KEY;
    public static Item BROKEN_SWORD;
    public static Item BLACK_KNIGHT_GREATSWORD;
    public static Item MOONLIGHT_GREATSWORD;
    public static Item LIGHTNING_SPEAR;
    public static Item PYROMANCY_FLAME;
    public static Item DIVINE_BLESSING;
    public static Item RING_OF_FAVOR;
    public static Item BLACK_KNIGHT_HELM;
    public static Item BLACK_KNIGHT_ARMOR;
    public static Item BLACK_KNIGHT_LEGGINGS;
    public static Item BLACK_KNIGHT_BOOTS;

    private ModItems() {}

    private static Item reg(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(DarkFortressMod.MOD_ID, id), item);
    }

    private static Item.Settings gear() {
        return new Item.Settings().maxCount(1).rarity(Rarity.RARE).fireproof();
    }

    public static void init() {
        ESTUS_FLASK = reg("estus_flask", new EstusFlaskItem(new Item.Settings().maxDamage(5).rarity(Rarity.UNCOMMON)));

        SOUL_LOST_UNDEAD = reg("soul_of_a_lost_undead", new SoulItem(new Item.Settings().maxCount(64), 30, false));
        SOUL_GREAT_DEMON = reg("soul_of_a_great_demon", new SoulItem(new Item.Settings().maxCount(16).rarity(Rarity.EPIC), 1500, true));

        FORTRESS_KEY = reg("fortress_key", new Item(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

        BROKEN_SWORD = reg("broken_straight_sword", new SwordItem(ToolMaterials.STONE, new Item.Settings()
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.STONE, 3, -2.4f))));

        BLACK_KNIGHT_GREATSWORD = reg("black_knight_greatsword", new HeavySwordItem(ToolMaterials.NETHERITE, gear()
                .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 9, -3.0f))));

        MOONLIGHT_GREATSWORD = reg("moonlight_greatsword", new MoonlightGreatswordItem(ToolMaterials.NETHERITE,
                new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof()
                        .attributeModifiers(SwordItem.createAttributeModifiers(ToolMaterials.NETHERITE, 7, -2.8f))));

        LIGHTNING_SPEAR = reg("lightning_spear", new AbilityItem(
                new Item.Settings().maxCount(1).rarity(Rarity.EPIC).fireproof(), 100, false, Abilities::lightning,
                "Right-click: call lightning on your target", "Range: 48 blocks"));

        PYROMANCY_FLAME = reg("pyromancy_flame", new AbilityItem(
                new Item.Settings().maxCount(1).rarity(Rarity.RARE).fireproof(), 30, false, Abilities::pyromancy,
                "Right-click: cone of fire"));

        DIVINE_BLESSING = reg("divine_blessing", new AbilityItem(
                new Item.Settings().maxCount(4).rarity(Rarity.EPIC), 0, true, Abilities::blessing,
                "Right-click: fully heal and cleanse", "Consumed on use"));

        RING_OF_FAVOR = reg("ring_of_favor", new RingOfFavorItem(
                new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

        BLACK_KNIGHT_HELM = reg("black_knight_helm", new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.HELMET,
                new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(37)).rarity(Rarity.RARE).fireproof()));
        BLACK_KNIGHT_ARMOR = reg("black_knight_armor", new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.CHESTPLATE,
                new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(37)).rarity(Rarity.RARE).fireproof()));
        BLACK_KNIGHT_LEGGINGS = reg("black_knight_leggings", new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.LEGGINGS,
                new Item.Settings().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(37)).rarity(Rarity.RARE).fireproof()));
        BLACK_KNIGHT_BOOTS = reg("black_knight_boots", new ArmorItem(ArmorMaterials.NETHERITE, ArmorItem.Type.BOOTS,
                new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(37)).rarity(Rarity.RARE).fireproof()));
    }
}
