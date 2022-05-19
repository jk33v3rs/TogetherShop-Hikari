package com.ghostchu.quickshop.util;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.shop.permission.BuiltInShopPermissionGroup;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.Reloadable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ItemMarker implements Reloadable {
    private final QuickShop plugin;
    private final Map<String, ItemStack> stacks = new HashMap<>();
    private YamlConfiguration configuration;
    private final File file;

    public ItemMarker(@NotNull QuickShop plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "items.yml");
        init();
        plugin.getReloadManager().register(this);
    }

    public void init() {
        stacks.clear();

        if (!file.exists()) {
            initDefaultConfiguration(file);
        }
        configuration = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = configuration.getConfigurationSection("item-template");
        if (section == null)
            section = configuration.createSection("item-template");
        for (String key : section.getKeys(false)) {
            if (section.isItemStack(key)) {
                try {
                    stacks.put(key, Util.deserialize(key));
                } catch (InvalidConfigurationException e) {
                    Log.debug(Level.WARNING, "Failed to load item " + key + " from items.yml");
                }
            }
        }
    }

    public boolean save(@NotNull String itemName, @NotNull ItemStack itemStack) {
        if (stacks.containsKey(itemName)) {
            return false;
        }
        if (itemName.contains(".")) {
            return false;
        }
        stacks.put(itemName, itemStack);
        ConfigurationSection section = configuration.getConfigurationSection("item-template");
        if (section == null)
            section = configuration.createSection("item-template");
        section.set(itemName, Util.serialize(itemStack));
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save items.yml", e);
            return false;
        }
        Log.debug("Saved item " + itemName + " !");
        return true;
    }

    @Nullable
    public ItemStack get(@NotNull String itemName) {
        return stacks.get(itemName);
    }

    private void initDefaultConfiguration(@NotNull File file) {
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("version", 1);
        for (BuiltInShopPermissionGroup group : BuiltInShopPermissionGroup.values()) {
            yamlConfiguration.set(group.getNamespacedNode(), group.getPermissions().stream().map(BuiltInShopPermission::getNamespacedNode).collect(Collectors.toList()));
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            yamlConfiguration.save(file);
        } catch (Exception e) {
            Log.permission(Level.SEVERE, "Failed to create default items configuration file");
            plugin.getLogger().log(Level.SEVERE, "Failed to create default items configuration", e);
        }
    }

    @Override
    public ReloadResult reloadModule() throws Exception {
        init();
        return Reloadable.super.reloadModule();
    }
}
