package com.jamplifier.griefreport.manager;

import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.model.GriefReportStatus;
import com.jamplifier.griefreport.storage.ReportStorage;

import java.util.*;
import java.util.stream.Collectors;

public class GriefReportManager {

    private final ReportStorage storage;
    private final Map<Integer, GriefReport> reports = new HashMap<>();
    private int nextId = 1;

    public GriefReportManager(ReportStorage storage) {
        this.storage = storage;
        storage.loadAll(reports);
        recalcNextId();
    }

    private void recalcNextId() {
        if (!reports.isEmpty()) {
            nextId = Collections.max(reports.keySet()) + 1;
        }
    }

    public GriefReport createReport(UUID reporter, org.bukkit.Location loc, String message) {
        GriefReport report = new GriefReport(nextId++, reporter, loc, message);
        reports.put(report.getId(), report);
        storage.save(report);
        return report;
    }

    public GriefReport getReport(int id) {
        return reports.get(id);
    }

    public List<GriefReport> getOpenReports() {
        return reports.values().stream()
                .filter(r -> r.getStatus() != GriefReportStatus.CLOSED)
                .sorted(Comparator.comparingInt(GriefReport::getId))
                .collect(Collectors.toList());
    }

    public void saveAll() {
        storage.saveAll(reports.values());
    }
}
