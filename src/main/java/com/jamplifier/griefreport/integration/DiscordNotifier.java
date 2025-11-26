package com.jamplifier.griefreport.integration;

import com.jamplifier.griefreport.model.GriefReport;

public interface DiscordNotifier {

    void sendNewReport(GriefReport report);

    void sendReportClosed(GriefReport report);
}
