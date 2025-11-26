package com.jamplifier.griefreport.integration;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.model.GriefReport;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WebhookDiscordNotifier implements DiscordNotifier {

    private final GriefReportPlugin plugin;
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault());

    // Config cache
    private boolean enabled;
    private String webhookUrl;
    private String username;
    private String avatarUrl;
    private boolean mentionEnabled;
    private String mentionRoleId;
    private int colorNew;
    private int colorClosed;

    public WebhookDiscordNotifier(GriefReportPlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    private void reloadFromConfig() {
        var cfg = plugin.getConfig();

        this.enabled = cfg.getBoolean("discord.enabled", true);
        this.webhookUrl = cfg.getString("discord.webhook-url", "").trim();
        this.username = cfg.getString("discord.username", "GriefReport");
        this.avatarUrl = cfg.getString("discord.avatar-url", "").trim();
        this.mentionEnabled = cfg.getBoolean("discord.mention-enabled", false);
        this.mentionRoleId = cfg.getString("discord.mention-role-id", "").trim();
        this.colorNew = cfg.getInt("discord.color-new", 16711680);       // red
        this.colorClosed = cfg.getInt("discord.color-closed", 65280);    // green
    }

    @Override
    public void sendNewReport(GriefReport report) {
        if (!enabled || webhookUrl.isEmpty()) return;

        // Use cached config values & stored names; no Bukkit calls here.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String reporterName = report.getReporterName();
                String locationLine = report.getWorldName() + " "
                        + (int) report.getX() + ", "
                        + (int) report.getY() + ", "
                        + (int) report.getZ();

                String message = report.getMessage().isEmpty()
                        ? "No message provided."
                        : report.getMessage();

                String createdAt = dateFormatter.format(report.getCreatedAt());

                String mentionContent = "";
                if (mentionEnabled && !mentionRoleId.isEmpty()) {
                    mentionContent = "<@&" + mentionRoleId + ">";
                }

                String json = """
                        {
                          "username": "%s",
                          "avatar_url": "%s",
                          "content": "%s",
                          "embeds": [{
                            "title": "New Grief Report #%d",
                            "description": "%s",
                            "color": %d,
                            "fields": [
                              { "name": "Reporter", "value": "%s", "inline": true },
                              { "name": "Location", "value": "%s", "inline": true },
                              { "name": "Created At", "value": "%s", "inline": false }
                            ]
                          }]
                        }
                        """.formatted(
                        escapeJson(username),
                        escapeJson(avatarUrl),
                        escapeJson(mentionContent),
                        report.getId(),
                        escapeJson(message),
                        colorNew,
                        escapeJson(reporterName),
                        escapeJson(locationLine),
                        escapeJson(createdAt)
                );

                postJson(webhookUrl, json);
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord webhook (new report): " + e.getMessage());
            }
        });
    }

    @Override
    public void sendReportClosed(GriefReport report) {
        if (!enabled || webhookUrl.isEmpty()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String closedByName = report.getClosedByName() == null
                        ? "Unknown"
                        : report.getClosedByName();

                String locationLine = report.getWorldName() + " "
                        + (int) report.getX() + ", "
                        + (int) report.getY() + ", "
                        + (int) report.getZ();

                String closedAt = report.getClosedAt() != null
                        ? dateFormatter.format(report.getClosedAt())
                        : "Unknown";

                String mentionContent = "";
                if (mentionEnabled && !mentionRoleId.isEmpty()) {
                    mentionContent = "<@&" + mentionRoleId + ">";
                }

                String json = """
                        {
                          "username": "%s",
                          "avatar_url": "%s",
                          "content": "%s",
                          "embeds": [{
                            "title": "Grief Report #%d Closed",
                            "color": %d,
                            "fields": [
                              { "name": "Closed By", "value": "%s", "inline": true },
                              { "name": "Location", "value": "%s", "inline": true },
                              { "name": "Closed At", "value": "%s", "inline": false }
                            ]
                          }]
                        }
                        """.formatted(
                        escapeJson(username),
                        escapeJson(avatarUrl),
                        escapeJson(mentionContent),
                        report.getId(),
                        colorClosed,
                        escapeJson(closedByName),
                        escapeJson(locationLine),
                        escapeJson(closedAt)
                );

                postJson(webhookUrl, json);
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord webhook (closed report): " + e.getMessage());
            }
        });
    }

    private void postJson(String urlString, String json) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(7000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 204 && code != 200) {
            plugin.getLogger().warning("Discord webhook responded with code " + code);
        }
        conn.disconnect();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    // Called when /grief reload runs (via plugin.reloadPluginConfig())
    public void reload() {
        reloadFromConfig();
    }
}
