package com.hcpanel.gui;

import com.hcpanel.gui.modules.ModuleContentProvider;

/**
 * Tracks navigation state for the unified panel.
 * Used for BACK button functionality.
 */
public class NavigationContext {

    private final ModuleContentProvider currentModule;
    private final String currentView;
    private final NavigationContext previousContext;

    /**
     * Creates a root context (main menu).
     */
    public NavigationContext() {
        this.currentModule = null;
        this.currentView = null;
        this.previousContext = null;
    }

    /**
     * Creates a module context (viewing a module's content).
     */
    public NavigationContext(ModuleContentProvider module, String view, NavigationContext previous) {
        this.currentModule = module;
        this.currentView = view;
        this.previousContext = previous;
    }

    /**
     * Returns true if this is the main menu (root level).
     */
    public boolean isMainMenu() {
        return currentModule == null;
    }

    /**
     * Returns true if there's a previous context to go back to.
     */
    public boolean canGoBack() {
        return previousContext != null;
    }

    public ModuleContentProvider getCurrentModule() {
        return currentModule;
    }

    public String getCurrentView() {
        return currentView;
    }

    public NavigationContext getPreviousContext() {
        return previousContext;
    }

    /**
     * Creates a new context for navigating into a module.
     */
    public NavigationContext navigateToModule(ModuleContentProvider module, String view) {
        return new NavigationContext(module, view, this);
    }

    /**
     * Creates a new context for navigating within the same module.
     */
    public NavigationContext navigateToView(String view) {
        if (currentModule == null) {
            throw new IllegalStateException("Cannot navigate to view without a module");
        }
        return new NavigationContext(currentModule, view, this);
    }
}
