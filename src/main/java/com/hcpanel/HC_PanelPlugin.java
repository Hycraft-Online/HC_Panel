package com.hcpanel;

import com.hcpanel.commands.MenuCommand;
import com.hcpanel.config.NewsConfig;
import com.hcpanel.gui.modules.CharacterModule;
import com.hcpanel.gui.modules.FactionsModule;
import com.hcpanel.gui.modules.HonorModule;
import com.hcpanel.gui.modules.ModuleContentProvider;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * HC_Panel - Unified Character Panel
 *
 * Combines HC_Factions, HC_Honor, and HC_Attributes into a single
 * unified UI with hierarchical navigation.
 */
public class HC_PanelPlugin extends JavaPlugin {

    private static HC_PanelPlugin instance;

    private final List<ModuleContentProvider> availableModules = new ArrayList<>();
    private NewsConfig newsConfig;

    public HC_PanelPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        this.getLogger().at(Level.INFO).log("HC_Panel setting up...");

        // Load news configuration
        this.newsConfig = NewsConfig.load(this.getLogger());

        // Detect available modules
        detectAvailableModules();

        // Register /menu command
        this.getCommandRegistry().registerCommand(new MenuCommand(this));
    }

    @Override
    protected void start() {
        super.start();
        this.getLogger().at(Level.INFO).log("HC_Panel started with " + availableModules.size() + " modules available:");
        for (ModuleContentProvider module : availableModules) {
            this.getLogger().at(Level.INFO).log("  - " + module.getModuleName());
        }
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        this.getLogger().at(Level.INFO).log("HC_Panel shutting down...");
        instance = null;
    }

    /**
     * Detects which HC plugins are available and registers their modules.
     * Called during setup and can be called again to refresh module availability.
     */
    public void detectAvailableModules() {
        availableModules.clear();

        // Check for HC_Factions
        if (isPluginAvailable("com.hcfactions.HC_FactionsPlugin")) {
            availableModules.add(new FactionsModule());
            this.getLogger().at(Level.INFO).log("Detected HC_Factions - Factions module enabled");
        }

        // Check for HC_Honor
        if (isPluginAvailable("com.hchonor.HC_HonorPlugin")) {
            availableModules.add(new HonorModule());
            this.getLogger().at(Level.INFO).log("Detected HC_Honor - Honor module enabled");
        }

        // Check for HC_Attributes OR HC_Classes - if either is available, show Character module
        boolean hasAttributes = isPluginAvailable("com.hcattributes.HC_AttributesPlugin");
        boolean hasClasses = isPluginAvailable("com.hcclasses.HC_ClassesPlugin");
        if (hasAttributes || hasClasses) {
            availableModules.add(new CharacterModule());
            this.getLogger().at(Level.INFO).log("Detected HC_Attributes/HC_Classes - Character module enabled");
        }

        if (availableModules.isEmpty()) {
            this.getLogger().at(Level.WARNING).log("No HC plugins detected! The panel will be empty.");
        }
    }

    /**
     * Checks if a plugin class is available and has a valid instance.
     */
    private boolean isPluginAvailable(String className) {
        try {
            Class<?> pluginClass = Class.forName(className);
            var getInstanceMethod = pluginClass.getMethod("getInstance");
            Object instance = getInstanceMethod.invoke(null);
            return instance != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the list of currently available modules.
     * This is refreshed each time /menu is opened to handle hot-reload scenarios.
     */
    public List<ModuleContentProvider> getAvailableModules() {
        // Re-detect on each access to handle plugin hot-reload
        detectAvailableModules();
        return new ArrayList<>(availableModules);
    }

    public static HC_PanelPlugin getInstance() {
        return instance;
    }

    public NewsConfig getNewsConfig() {
        return newsConfig;
    }
}
