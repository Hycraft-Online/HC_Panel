package com.hcpanel;

import com.hcpanel.commands.MenuCommand;
import com.hcpanel.config.NewsConfig;
import com.hcpanel.database.DatabaseConfig;
import com.hcpanel.database.DatabaseManager;
import com.hcpanel.database.NewsRepository;
import com.hcpanel.gui.modules.CharacterModule;
import com.hcpanel.gui.modules.CharactersModule;
import com.hcpanel.gui.modules.FactionsModule;
import com.hcpanel.gui.modules.HonorModule;
import com.hcpanel.gui.modules.ModuleContentProvider;
import com.hcpanel.gui.modules.RecruitmentModule;
import com.hcpanel.gui.modules.SkillsModule;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.io.File;
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

    private static volatile HC_PanelPlugin instance;

    private final List<ModuleContentProvider> availableModules = new ArrayList<>();
    private NewsConfig newsConfig;
    private DatabaseManager databaseManager;

    public HC_PanelPlugin(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        super.setup();
        this.getLogger().at(Level.INFO).log("HC_Panel setting up...");

        // Load news from database, fall back to bundled JSON
        this.newsConfig = loadNewsFromDatabase();

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
        if (databaseManager != null) {
            databaseManager.close();
        }
        this.getLogger().at(Level.INFO).log("HC_Panel shutting down...");
        instance = null;
    }

    /**
     * Tries to load news from the database, falls back to bundled news.json.
     */
    private NewsConfig loadNewsFromDatabase() {
        try {
            File modFolder = new File("mods/.hc_config/HC_Panel");
            DatabaseConfig dbConfig = DatabaseConfig.load(modFolder);
            databaseManager = new DatabaseManager(
                    dbConfig.getUrl(), dbConfig.getUsername(),
                    dbConfig.getPassword(), dbConfig.getPoolSize());

            NewsRepository newsRepo = new NewsRepository(databaseManager);
            List<NewsConfig.NewsEntry> entries = newsRepo.loadPublishedNews(10);

            if (!entries.isEmpty()) {
                this.getLogger().at(Level.INFO).log("Loaded " + entries.size() + " news articles from database");
                return new NewsConfig(entries);
            }

            this.getLogger().at(Level.WARNING).log("No news found in database, falling back to news.json");
        } catch (Exception e) {
            this.getLogger().at(Level.WARNING).log("Database unavailable for news, falling back to news.json: " + e.getMessage());
        }
        return NewsConfig.load(this.getLogger());
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

        // Check for HC_Professions - Skills module
        if (isPluginAvailable("com.hcprofessions.HC_ProfessionsPlugin")) {
            availableModules.add(new SkillsModule());
            this.getLogger().at(Level.INFO).log("Detected HC_Professions - Skills module enabled");
        }

        // Check for HC_Recruitment
        if (isPluginAvailable("com.hcrecruitment.HC_RecruitmentPlugin")) {
            availableModules.add(new RecruitmentModule());
            this.getLogger().at(Level.INFO).log("Detected HC_Recruitment - Recruitment module enabled");
        }

        // Check for HC_MultiChar (character switching)
        if (isPluginAvailable("com.hcmultichar.HC_MultiCharPlugin")) {
            availableModules.add(new CharactersModule());
            this.getLogger().at(Level.INFO).log("Detected HC_MultiChar - Characters module enabled");
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
