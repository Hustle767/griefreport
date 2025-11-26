package com.jamplifier.griefreport.integration;

import com.jamplifier.griefreport.GriefReportPlugin;
import com.jamplifier.griefreport.model.GriefReport;
import org.bukkit.Bukkit;

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

    public WebhookDiscordNotifier(GriefReportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendNewReport(GriefReport report) {
        String url = getWebhookUrl();
        if (url == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String reporterName = Bukkit.getOfflinePlayer(report.getReporter()).getName();
                if (reporterName == null) reporterName = "Unknown";

                String locationLine = report.getWorldName() + " "
                        + (int) report.getX() + ", "
                        + (int) report.getY() + ", "
                        + (int) report.getZ();

                String message = report.getMessage().isEmpty()
                        ? "No message provided."
                        : report.getMessage();

                String createdAt = dateFormatter.format(report.getCreatedAt());

                String json = """
                        {
                          "username": "GriefReport",
                          "embeds": [{
                            "title": "New Grief Report #%d",
                            "description": "%s",
                            "color": 16711680,
                            "fields": [
                              { "name": "Reporter", "value": "%s", "inline": true },
                              { "name": "Location", "value": "%s", "inline": true },
                              { "name": "Created At", "value": "%s", "inline": false }
                            ]
                          }]
                        }
                        """.formatted(
                        report.getId(),
                        escapeJson(message),
                        escapeJson(reporterName),
                        escapeJson(locationLine),
                        escapeJson(createdAt)
                );

                postJson(url, json);
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord webhook (new report): " + e.getMessage());
            }
        });
    }

    @Override
    public void sendReportClosed(GriefReport report) {
        String url = getWebhookUrl();
        if (url == null) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String closedByName = report.getClosedBy() != null
                        ? Bukkit.getOfflinePlayer(report.getClosedBy()).getName()
                        : "Unknown";

                String locationLine = report.getWorldName() + " "
                        + (int) report.getX() + ", "
                        + (int) report.getY() + ", "
                        + (int) report.getZ();

                String closedAt = report.getClosedAt() != null
                        ? dateFormatter.format(report.getClosedAt())
                        : "Unknown";

                String json = """
                        {
                          "username": "GriefReport",
                          "embeds": [{
                            "title": "Grief Report #%d Closed",
                            "color": 65280,
                            "fields": [
                              { "name": "Closed By", "value": "%s", "inline": true },
                              { "name": "Location", "value": "%s", "inline": true },
                              { "name": "Closed At", "value": "%s", "inline": false }
                            ]
                          }]
                        }
                        """.formatted(
                        report.getId(),
                        escapeJson(closedByName),
                        escapeJson(locationLine),
                        escapeJson(closedAt)
                );

                postJson(url, json);
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending Discord webhook (closed report): " + e.getMessage());
            }
        });
    }

    private String getWebhookUrl() {
        String url = plugin.getConfig().getString("discord-webhook-url", "").trim();
        if (url.isEmpty()) return null;
        return url;
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
}
