package com.github.happyuky7.separeworlditems.listeners.Integration.EssentialsX;

import com.github.happyuky7.separeworlditems.SepareWorldItems;
import com.github.happyuky7.separeworlditems.filemanagers.FileManager2;

import net.ess3.api.events.UserTeleportHomeEvent;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;

/**
 * Event listener for handling player teleport events related to home teleports.
 * Integrates with EssentialsX plugin.
 */
public class HomeEvent implements Listener {

    private final SepareWorldItems plugin;

    /**
     * Constructor for UserTeleportHomeEvent.
     *
     * @param plugin The main plugin instance.
     */
    public HomeEvent(SepareWorldItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Event handler for PlayerTeleportEvent. Handles the specific teleportation
     * behavior when a player teleports to their home.
     *
     * @param event The PlayerTeleportEvent.
     */
    @EventHandler
    public void onUserTeleportHome(UserTeleportHomeEvent event) {
        Player player = event.getUser().getBase().getPlayer();
        String fromWorld = player.getWorld().getName(); // Current world
        String toWorld = event.getHomeLocation().getWorld().getName(); // Target world (home)

        // Debug message: inform about teleportation
        Bukkit.getServer().broadcastMessage(
                "[DEBUG] Player " + player.getName() + " is teleporting from " + fromWorld + " to " + toWorld);

        // Check if the target world is configured
        FileConfiguration config = plugin.getConfig();

        if (config.contains("worlds." + fromWorld) && config.contains("worlds." + toWorld)) {
            String fromGroup = config.getString("worlds." + fromWorld);
            String toGroup = config.getString("worlds." + toWorld);

            // Save current player data before proceeding
            savePlayerData(player, fromGroup);

            if (!fromGroup.equals(toGroup)) {
                Bukkit.getServer().broadcastMessage("[DEBUG] Saving and loading player data for " + player.getName());

                savePlayerData(player, fromGroup);
                loadPlayerData(player, toGroup);
            } else {
                Bukkit.getServer().broadcastMessage("[DEBUG] No data transfer needed for " + player.getName());

                // Call reloadExperienceAndLevel when no data transfer is needed
                reloadExperienceAndLevel(player, fromGroup);
            }
        }
    }

    /**
     * Saves the player's current data to a group-specific configuration file.
     *
     * @param player    The player whose data is being saved.
     * @param groupName The group name associated with the player's current world.
     */
    private void savePlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        saveInventory(player, config);
        savePlayerAttributes(player, config);
        savePotionEffects(player, config);
        saveOffHandItem(player, config);

        FileManager2.saveConfiguration(file, config);

        clearPlayerState(player);
    }

    /**
     * Loads the player's data from a group-specific configuration file.
     *
     * @param player    The player whose data is being loaded.
     * @param groupName The group name associated with the player's target world.
     */
    private void loadPlayerData(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        loadInventory(player, config);
        loadPlayerAttributes(player, config);
        loadPotionEffects(player, config);
        loadOffHandItem(player, config);
    }

    /**
     * Saves the player's inventory, ender chest, and armor contents to the
     * configuration file.
     *
     * @param player The player whose inventory is being saved.
     * @param config The configuration file where the data is saved.
     */
    private void saveInventory(Player player, FileConfiguration config) {
        // Save inventory contents
        int index = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            config.set("inventory." + index++, item);
        }

        // Save ender chest contents if enabled
        if (plugin.getConfig().getBoolean("Options.ender-chest", true)) {
            index = 0;
            for (ItemStack item : player.getEnderChest().getContents()) {
                config.set("ender_chest." + index++, item);
            }
        }

        // Save armor contents
        config.set("armor_contents.helmet", player.getInventory().getHelmet());
        config.set("armor_contents.chestplate", player.getInventory().getChestplate());
        config.set("armor_contents.leggings", player.getInventory().getLeggings());
        config.set("armor_contents.boots", player.getInventory().getBoots());
    }

    /**
     * Saves the player's attributes such as gamemode, flying state, health, hunger,
     * and experience.
     *
     * @param player The player whose attributes are being saved.
     * @param config The configuration file where the data is saved.
     */
    private void savePlayerAttributes(Player player, FileConfiguration config) {
        // Save gamemode
        if (plugin.getConfig().getBoolean("Options.gamemode", true)) {
            config.set("gamemode", player.getGameMode().toString());
        }

        // Save flying state
        if (plugin.getConfig().getBoolean("Options.flying", true)) {
            config.set("flying", player.isFlying());
        }

        // Save health and hunger
        config.set("health", plugin.getConfig().getBoolean("Options.health-options.health-default-save", true)
                ? player.getHealth()
                : 20.0D);
        config.set("hunger", player.getFoodLevel());

        // Save experience
        config.set("exp", player.getExp());
        config.set("exp-level", player.getLevel());
    }

    /**
     * Saves the player's active potion effects to the configuration file.
     *
     * @param player The player whose potion effects are being saved.
     * @param config The configuration file where the data is saved.
     */
    @SuppressWarnings("deprecation")
    private void savePotionEffects(Player player, FileConfiguration config) {
        int index = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            config.set("potion_effect." + index + ".type", effect.getType().getName());
            config.set("potion_effect." + index + ".level", effect.getAmplifier());
            config.set("potion_effect." + index + ".duration", effect.getDuration());
            index++;
        }
    }

    /**
     * Saves the player's off-hand item to the configuration file.
     *
     * @param player The player whose off-hand item is being saved.
     * @param config The configuration file where the data is saved.
     */
    private void saveOffHandItem(Player player, FileConfiguration config) {
        config.set("off_hand_item", player.getInventory().getItemInOffHand());
    }

    /**
     * Loads the player's off-hand item from the configuration file.
     *
     * @param player The player whose off-hand item is being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    private void loadOffHandItem(Player player, FileConfiguration config) {
        if (config.contains("off_hand_item")) {
            player.getInventory().setItemInOffHand(config.getItemStack("off_hand_item"));
        }
    }

    /**
     * Clears the player's state by resetting their inventory, ender chest, flying
     * state, gamemode, health, hunger, experience, and level.
     *
     * @param player The player whose state is being cleared.
     */
    private void clearPlayerState(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setFlying(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0D);
        player.setFoodLevel(20);
        player.setExp(0.0F);
        player.setLevel(0);
    }

    /**
     * Loads the player's inventory, ender chest, and armor contents from the
     * configuration file.
     *
     * @param player The player whose inventory is being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    private void loadInventory(Player player, FileConfiguration config) {
        if (config.contains("inventory")) {
            for (String key : config.getConfigurationSection("inventory").getKeys(false)) {
                player.getInventory().setItem(Integer.parseInt(key), config.getItemStack("inventory." + key));
            }
        }

        if (plugin.getConfig().getBoolean("Options.ender-chest", true) && config.contains("ender_chest")) {
            for (String key : config.getConfigurationSection("ender_chest").getKeys(false)) {
                player.getEnderChest().setItem(Integer.parseInt(key), config.getItemStack("ender_chest." + key));
            }
        }
    }

    /**
     * Loads the player's attributes such as gamemode, flying state, health, hunger,
     * and experience from the configuration file.
     *
     * @param player The player whose attributes are being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    private void loadPlayerAttributes(Player player, FileConfiguration config) {
        player.setGameMode(GameMode.valueOf(config.getString("gamemode", "SURVIVAL")));
        player.setFlying(config.getBoolean("flying", false));
        player.setHealth(config.getDouble("health", 20.0D));
        player.setFoodLevel(config.getInt("hunger", 20));
        player.setExp((float) config.getDouble("exp", 0.0F));
        player.setLevel(config.getInt("exp-level", 0));
    }

    /**
     * Loads the player's active potion effects from the configuration file.
     *
     * @param player The player whose potion effects are being loaded.
     * @param config The configuration file where the data is loaded from.
     */
    private void loadPotionEffects(Player player, FileConfiguration config) {
        if (config.contains("potion_effect")) {
            for (String key : config.getConfigurationSection("potion_effect").getKeys(false)) {
                @SuppressWarnings("deprecation")
                PotionEffect effect = new PotionEffect(
                        PotionEffectType.getByName(config.getString("potion_effect." + key + ".type")),
                        config.getInt("potion_effect." + key + ".duration"),
                        config.getInt("potion_effect." + key + ".level"));
                player.addPotionEffect(effect);
            }
        }
    }

    /**
     * Reloads the player's experience and level from a group-specific configuration
     * file.
     *
     * @param player    The player whose experience and level are being reloaded.
     * @param groupName The group name associated with the player's data.
     */
    private void reloadExperienceAndLevel(Player player, String groupName) {
        File file = new File(plugin.getDataFolder() + File.separator + "groups"
                + File.separator + groupName + File.separator + player.getName() + "-" + player.getUniqueId() + ".yml");
        FileConfiguration config = FileManager2.getYaml(file);

        // Check if experience and level data are present
        if (config.contains("exp") && config.contains("exp-level")) {
            float experience = (float) config.getDouble("exp", 0.0F);
            int level = config.getInt("exp-level", 0);

            // Reload experience and level
            player.setExp(experience);
            player.setLevel(level);

            // Debug message for successful reload
            Bukkit.getServer().broadcastMessage("[DEBUG] Reloaded experience and level for " + player.getName() +
                    " (Exp: " + experience + ", Level: " + level + ")");
        } else {
            // Debug message if data is not found
            Bukkit.getServer().broadcastMessage("[DEBUG] No experience or level data found for " + player.getName());
        }
    }
}