package com.jamplifier.griefreport;

import com.jamplifier.griefreport.command.GriefCommand;
import com.jamplifier.griefreport.integration.DiscordNotifier;
import com.jamplifier.griefreport.integration.WebhookDiscordNotifier;
import com.jamplifier.griefreport.manager.GriefReportManager;
import com.jamplifier.griefreport.storage.ReportStorage;
import com.jamplifier.griefreport.storage.YamlReportStorage;
import org.bukkit.plugin.java.JavaPlugin;

public final class GriefReportPlugin extends JavaPlugin {

    private static GriefReportPlugin instance;

    private GriefReportManager reportManager;
    private DiscordNotifier discordNotifier;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        ReportStorage storage = new YamlReportStorage(this);
        this.reportManager = new GriefReportManager(storage);

        this.discordNotifier = new WebhookDiscordNotifier(this);

        GriefCommand griefCommand = new GriefCommand(this);
        getCommand("grief").setExecutor(griefCommand);
        getCommand("grief").setTabCompleter(griefCommand);
    }


    @Override
    public void onDisable() {
        if (reportManager != null) {
            reportManager.saveAll();
        }
    }
    

    public static GriefReportPlugin getInstance() {
        return instance;
    }

    public GriefReportManager getReportManager() {
        return reportManager;
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }
    
    public void reloadPluginConfig() {
        reloadConfig();
        if (this.discordNotifier instanceof WebhookDiscordNotifier notifier) {
            notifier.reload();
        } else {
            this.discordNotifier = new WebhookDiscordNotifier(this);
        }
    }


}
