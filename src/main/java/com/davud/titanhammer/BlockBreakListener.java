package com.davud.titanhammer;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BlockBreakListener implements Listener {

    private final TitanHammer plugin;

    public BlockBreakListener(TitanHammer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(TitanHammer.TIER_KEY, PersistentDataType.STRING)) return;

        // Determine face hit
        BlockFace face = player.getTargetBlockFace(6);
        if (face == null) face = BlockFace.UP;

        Block center = event.getBlock();
        List<Block> toBreak = get3x3x3Blocks(center, face);

        int blocksBroken = 0;

        for (Block b : toBreak) {
            // Skip the center block since it's already being broken by the event
            if (b.equals(center)) {
                blocksBroken++;
                continue;
            }

            // Only break solid blocks that aren't unbreakable (like Bedrock)
            if (b.getType().isSolid() && b.getType().getHardness() >= 0 && b.getType() != Material.OBSIDIAN && b.getType() != Material.CRYING_OBSIDIAN) {
                b.breakNaturally(item);
                blocksBroken++;
            }
        }

        // Handle custom durability if not in Creative mode
        if (player.getGameMode() != GameMode.CREATIVE) {
            Integer curDura = meta.getPersistentDataContainer().get(TitanHammer.CUR_DURA_KEY, PersistentDataType.INTEGER);
            Integer maxDura = meta.getPersistentDataContainer().get(TitanHammer.MAX_DURA_KEY, PersistentDataType.INTEGER);

            if (curDura != null && maxDura != null) {
                int newDura = curDura - blocksBroken;
                if (newDura <= 0) {
                    player.getInventory().setItemInMainHand(null);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    return;
                }

                meta.getPersistentDataContainer().set(TitanHammer.CUR_DURA_KEY, PersistentDataType.INTEGER, newDura);
                
                // Update Lore
                List<String> lore = meta.getLore();
                if (lore != null && lore.size() >= 2) {
                    lore.set(1, org.bukkit.ChatColor.GRAY + "Dayanıklılık: " + newDura + " / " + maxDura);
                    meta.setLore(lore);
                }

                // Update visual durability bar
                if (meta instanceof Damageable damageable) {
                    // MACE vanilla max durability is 250
                    // Calculate proportional damage
                    double ratio = 1.0 - ((double) newDura / maxDura);
                    int visualDamage = (int) (250 * ratio);
                    damageable.setDamage(visualDamage);
                }

                item.setItemMeta(meta);
            }
        }
    }

    private List<Block> get3x3x3Blocks(Block center, BlockFace face) {
        List<Block> blocks = new ArrayList<>();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    blocks.add(center.getWorld().getBlockAt(cx + x, cy + y, cz + z));
                }
            }
        }

        return blocks;
    }
}
