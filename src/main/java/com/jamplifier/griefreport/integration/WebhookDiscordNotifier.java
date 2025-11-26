package com.jamplifier.griefreport.integration;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.model.GriefReport;

public class WebhookDiscordNotifier implements DiscordNotifier {

    private final GriefReportPlugin plugin;

    public WebhookDiscordNotifier(GriefReportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNewReport(GriefReport report) {
        // TODO: Implement Discord webhook sending using config value
    }

    @Override
    public void sendReportClosed(GriefReport report) {
        // TODO: Implement Discord webhook sending
    }
}
