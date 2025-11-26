package com.jamplifier.griefreport.storage;

import com.jamplifier.griefreport.model.GriefReport;

import java.util.Collection;
import java.util.Map;

public interface ReportStorage {

    void loadAll(Map<Integer, GriefReport> target);

    void save(GriefReport report);

    void saveAll(Collection<GriefReport> reports);
}
