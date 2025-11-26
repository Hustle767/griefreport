package com.jamplifier.griefreport.manager;

import com.jamplifier.griefreport.model.GriefReport;
import com.jamplifier.griefreport.model.GriefReportStatus;
import com.jamplifier.griefreport.storage.ReportStorage;
import org.bukkit.Location;

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

    public GriefReport createReport(UUID reporter, String reporterName, Location loc, String message) {
        GriefReport report = new GriefReport(nextId++, reporter, reporterName, loc, message);
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

    public List<GriefReport> getReportsByReporter(UUID reporter) {
        return reports.values().stream()
                .filter(r -> r.getReporter().equals(reporter))
                .sorted(Comparator.comparingInt(GriefReport::getId))
                .collect(Collectors.toList());
    }

    public Optional<GriefReport> getLastReportFor(UUID reporter) {
        return getReportsByReporter(reporter).stream().reduce((first, second) -> second);
    }

    public void save(GriefReport report) {
        storage.save(report);
    }

    public void saveAll() {
        storage.saveAll(reports.values());
    }
}
