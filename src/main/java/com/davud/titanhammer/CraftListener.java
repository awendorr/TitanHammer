package com.davud.titanhammer;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CraftListener implements Listener {

    private final TitanHammer plugin;

    public CraftListener(TitanHammer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result == null || !result.hasItemMeta()) return;

        ItemMeta resultMeta = result.getItemMeta();
        if (!resultMeta.getPersistentDataContainer().has(TitanHammer.TIER_KEY, PersistentDataType.STRING)) return;

        // ── This is a TitanHammer craft ──────────────────────────────────────────
        Integer maxDura = resultMeta.getPersistentDataContainer().get(TitanHammer.MAX_DURA_KEY, PersistentDataType.INTEGER);
        if (maxDura == null) return;

        // Read the crafting matrix (slots 1-9 for a crafting table)
        CraftingInventory craftInv = event.getInventory();
        ItemStack[] matrix = craftInv.getMatrix();

        // Collect the 3 pickaxes from the middle row (slots 3, 4, 5 in the 3x3 matrix)
        List<Double> ratios = new ArrayList<>();
        for (ItemStack ingredient : matrix) {
            if (ingredient == null || ingredient.getType() == Material.AIR) continue;

            int vanillaMax = TitanHammer.getMaxVanillaDurability(ingredient.getType());
            if (vanillaMax <= 0) continue; // not a damageable tool ingredient

            int remaining = TitanHammer.getRemainingDurability(ingredient);
            if (remaining < 0) continue;

            // Durability ratio: 1.0 = brand new, 0.0 = broken
            ratios.add((double) remaining / vanillaMax);
        }

        if (ratios.isEmpty()) return; // no damageable tools found (e.g. only ingots)

        // Average durability ratio of all 3 pickaxes
        double avgRatio = ratios.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        int newCurDura  = (int) Math.max(1, Math.round(maxDura * avgRatio));

        // Apply to the result item (we must clone & modify since it's the recipe template)
        ItemStack finalResult = result.clone();
        ItemMeta finalMeta = finalResult.getItemMeta();
        finalMeta.getPersistentDataContainer().set(TitanHammer.CUR_DURA_KEY, PersistentDataType.INTEGER, newCurDura);

        // Update lore to reflect starting durability
        String pct = String.format("%.0f%%", avgRatio * 100);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Derin Alan Kazısı: 3x3x3");
        lore.add(ChatColor.GRAY + "Dayanıklılık: " + newCurDura + " / " + maxDura
                + ChatColor.DARK_GRAY + "  (" + pct + ")");
        finalMeta.setLore(lore);

        // Sync vanilla damage bar proportionally
        if (finalMeta instanceof Damageable dmg) {
            int vanillaMax = finalResult.getType().getMaxDurability();
            dmg.setDamage((int) ((1.0 - avgRatio) * vanillaMax));
        }

        finalResult.setItemMeta(finalMeta);

        // Override the craft result
        event.setCurrentItem(finalResult);

        // Inform the player
        if (avgRatio < 1.0) {
            event.getWhoClicked().sendMessage(
                ChatColor.YELLOW + "⚠ " + ChatColor.GOLD + "Titan Çekici " +
                ChatColor.WHITE + pct + ChatColor.GOLD + " dayanıklılıkla üretildi! " +
                ChatColor.GRAY + "(Kazmalar hasar görmüştü)"
            );
        }
    }
}
