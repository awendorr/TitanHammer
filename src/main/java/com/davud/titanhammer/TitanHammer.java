package com.davud.titanhammer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class TitanHammer extends JavaPlugin {

    public static NamespacedKey TIER_KEY;
    public static NamespacedKey MAX_DURA_KEY;
    public static NamespacedKey CUR_DURA_KEY;

    @Override
    public void onEnable() {
        TIER_KEY     = new NamespacedKey(this, "titan_tier");
        MAX_DURA_KEY = new NamespacedKey(this, "titan_max_durability");
        CUR_DURA_KEY = new NamespacedKey(this, "titan_cur_durability");

        registerRecipes();
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);

        getLogger().info("TitanHammer enabled!");
    }

    private void registerRecipes() {
        // Demir Titan Çekici — max 750 (3 × 250)
        // Tarif: 3 Demir Külçe / 3 Demir Kazma / 3 Demir Külçe
        ItemStack ironHammer = createHammer(Material.IRON_PICKAXE, "Demir", ChatColor.WHITE, 750);
        ShapedRecipe ironRecipe = new ShapedRecipe(new NamespacedKey(this, "iron_hammer"), ironHammer);
        ironRecipe.shape("III", "PPP", "III");
        ironRecipe.setIngredient('I', Material.IRON_INGOT);
        ironRecipe.setIngredient('P', Material.IRON_PICKAXE);
        Bukkit.addRecipe(ironRecipe);

        // Elmas Titan Çekici — max 4683 (3 × 1561)
        ItemStack diamondHammer = createHammer(Material.DIAMOND_PICKAXE, "Elmas", ChatColor.AQUA, 4683);
        ShapedRecipe diamondRecipe = new ShapedRecipe(new NamespacedKey(this, "diamond_hammer"), diamondHammer);
        diamondRecipe.shape("DDD", "PPP", "DDD");
        diamondRecipe.setIngredient('D', Material.DIAMOND);
        diamondRecipe.setIngredient('P', Material.DIAMOND_PICKAXE);
        Bukkit.addRecipe(diamondRecipe);

        // Netherite Titan Çekici — max 6093 (3 × 2031)
        ItemStack netheriteHammer = createHammer(Material.NETHERITE_PICKAXE, "Netherite", ChatColor.DARK_GRAY, 6093);
        ShapedRecipe netheriteRecipe = new ShapedRecipe(new NamespacedKey(this, "netherite_hammer"), netheriteHammer);
        netheriteRecipe.shape("NNN", "PPP", "NNN");
        netheriteRecipe.setIngredient('N', Material.NETHERITE_INGOT);
        netheriteRecipe.setIngredient('P', Material.NETHERITE_PICKAXE);
        Bukkit.addRecipe(netheriteRecipe);
    }

    /**
     * Creates a fresh hammer with FULL durability (used as recipe result template).
     * The real durability is applied by CraftListener based on ingredient health.
     */
    public ItemStack createHammer(Material baseMaterial, String tierName, ChatColor color, int maxDurability) {
        ItemStack item = new ItemStack(baseMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "" + ChatColor.BOLD + tierName + " Titan Çekici");
            meta.getPersistentDataContainer().set(TIER_KEY,     PersistentDataType.STRING,  tierName);
            meta.getPersistentDataContainer().set(MAX_DURA_KEY, PersistentDataType.INTEGER, maxDurability);
            meta.getPersistentDataContainer().set(CUR_DURA_KEY, PersistentDataType.INTEGER, maxDurability);

            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Derin Alan Kazısı: 3x3x3",
                    ChatColor.GRAY + "Dayanıklılık: " + maxDurability + " / " + maxDurability
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Returns the remaining durability of an item (accounting for vanilla damage).
     * Returns -1 if the item is not Damageable or is null.
     */
    public static int getRemainingDurability(ItemStack item) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) return -1;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable dmg)) return -1;
        int max = item.getType().getMaxDurability();
        if (max <= 0) return -1;
        return max - dmg.getDamage();
    }

    /**
     * Returns the max vanilla durability for the material.
     */
    public static int getMaxVanillaDurability(Material mat) {
        return mat.getMaxDurability();
    }
}
