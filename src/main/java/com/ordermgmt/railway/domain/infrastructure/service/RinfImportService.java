package com.ordermgmt.railway.domain.infrastructure.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.ordermgmt.railway.domain.infrastructure.model.ImportLog;
import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.model.SectionOfLine;
import com.ordermgmt.railway.domain.infrastructure.repository.ImportLogRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;

import lombok.RequiredArgsConstructor;

/** Imports operational points and sections of line from RINF CSV exports. */
@Service
@RequiredArgsConstructor
public class RinfImportService {

    private static final Logger log = LoggerFactory.getLogger(RinfImportService.class);
    private static final Pattern WKT_POINT =
            Pattern.compile("POINT\\(([\\d.\\-]+)\\s+([\\d.\\-]+)\\)");
    private static final int BATCH_SIZE = 500;

    private final OperationalPointRepository opRepo;
    private final SectionOfLineRepository solRepo;
    private final ImportLogRepository logRepo;
    private final TransactionOperations transactionOperations;

    public List<ImportLog> getImportHistory() {
        return logRepo.findAllByOrderByStartedAtDesc();
    }

    public long countOps(String country) {
        return country != null ? opRepo.countByCountry(country) : opRepo.count();
    }

    public long countSols(String country) {
        return country != null ? solRepo.countByCountry(country) : solRepo.count();
    }

    /**
     * Import OPs: parse first, then delete+insert atomically.
     * On any error the transaction rolls back — no partial state.
     */
    public ImportLog importOperationalPoints(InputStream csvStream, String country) {
        try {
            List<String[]> rows = parseCsv(csvStream);
            List<OperationalPoint> items = toOperationalPoints(rows, country);
            int count = replaceOperationalPoints(items, country);
            return saveLog("RINF_OP", country, "SUCCESS", count,
                    count + " operational points imported for " + country);
        } catch (Exception e) {
            log.error("OP import failed for {}", country, e);
            return saveLog("RINF_OP", country, "ERROR", 0, sanitizeError(e));
        }
    }

    /**
     * Import SoLs: parse first, then delete+insert atomically.
     */
    public ImportLog importSectionsOfLine(InputStream csvStream, String country) {
        try {
            List<String[]> rows = parseCsv(csvStream);
            List<SectionOfLine> items = toSectionsOfLine(rows, country);
            int count = replaceSectionsOfLine(items, country);
            return saveLog("RINF_SOL", country, "SUCCESS", count,
                    count + " sections of line imported for " + country);
        } catch (Exception e) {
            log.error("SoL import failed for {}", country, e);
            return saveLog("RINF_SOL", country, "ERROR", 0, sanitizeError(e));
        }
    }

    /** Atomic delete+insert in a single transaction. Rolls back on any error. */
    protected int replaceOperationalPoints(List<OperationalPoint> items, String country) {
        Integer count = transactionOperations.execute(status -> {
            opRepo.deleteByCountry(country);
            saveInBatches(items, opRepo::saveAll);
            log.info("Imported {} OPs for {}", items.size(), country);
            return items.size();
        });
        return count != null ? count : 0;
    }

    protected int replaceSectionsOfLine(List<SectionOfLine> items, String country) {
        Integer count = transactionOperations.execute(status -> {
            solRepo.deleteByCountry(country);
            saveInBatches(items, solRepo::saveAll);
            log.info("Imported {} SoLs for {}", items.size(), country);
            return items.size();
        });
        return count != null ? count : 0;
    }

    private ImportLog saveLog(String source, String country, String status,
                              int count, String message) {
        ImportLog entry = new ImportLog();
        entry.setSource(source);
        entry.setCountry(country);
        entry.setStatus(status);
        entry.setRecordCount(count);
        entry.setMessage(message);
        entry.setFinishedAt(Instant.now());
        return logRepo.save(entry);
    }

    /** Sanitize error message — never expose internal details to user. */
    private String sanitizeError(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.length() > 200) {
            return "Import fehlgeschlagen — siehe Server-Log";
        }
        return msg.replaceAll("(?i)(password|secret|token|credential)", "***");
    }

    // --- CSV parsing and mapping (no DB access, no transaction needed) ---

    private List<OperationalPoint> toOperationalPoints(List<String[]> rows, String country) {
        Map<String, OperationalPoint> itemsByUopid = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (String[] row : rows) {
            if (row.length < 2) continue;
            OperationalPoint item = toOperationalPoint(row, country);
            OperationalPoint existing = itemsByUopid.putIfAbsent(item.getUopid(), item);
            if (existing != null) {
                duplicateCount++;
            }
        }
        if (duplicateCount > 0) {
            log.warn("Skipped {} duplicate operational point rows for {}", duplicateCount, country);
        }
        return new ArrayList<>(itemsByUopid.values());
    }

    private OperationalPoint toOperationalPoint(String[] row, String country) {
        OperationalPoint op = new OperationalPoint();
        op.setUopid(valueAt(row, 0));
        op.setName(valueAt(row, 1));
        op.setCountry(country);
        String wkt = valueAt(row, 2);
        if (!wkt.isBlank()) parseWkt(wkt, op);
        Integer opType = parseInteger(row, 3);
        if (opType != null) op.setOpType(opType);
        String tafTap = valueAt(row, 4);
        if (!tafTap.isBlank()) op.setTafTapCode(tafTap);
        return op;
    }

    private List<SectionOfLine> toSectionsOfLine(List<String[]> rows, String country) {
        Map<String, SectionOfLine> itemsBySolId = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (String[] row : rows) {
            if (row.length < 3) continue;
            SectionOfLine item = toSectionOfLine(row, country);
            SectionOfLine existing = itemsBySolId.putIfAbsent(item.getSolId(), item);
            if (existing != null) {
                duplicateCount++;
            }
        }
        if (duplicateCount > 0) {
            log.warn("Skipped {} duplicate section-of-line rows for {}", duplicateCount, country);
        }
        return new ArrayList<>(itemsBySolId.values());
    }

    private SectionOfLine toSectionOfLine(String[] row, String country) {
        SectionOfLine sol = new SectionOfLine();
        sol.setSolId(valueAt(row, 0));
        sol.setStartOpUopid(valueAt(row, 1));
        sol.setEndOpUopid(valueAt(row, 2));
        sol.setCountry(country);
        Double len = parseDouble(row, 3);
        if (len != null) sol.setLengthMeters(len);
        return sol;
    }

    private void parseWkt(String wkt, OperationalPoint op) {
        Matcher m = WKT_POINT.matcher(wkt);
        if (m.find()) {
            op.setLongitude(Double.parseDouble(m.group(1)));
            op.setLatitude(Double.parseDouble(m.group(2)));
        }
    }

    private List<String[]> parseCsv(InputStream is) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) rows.add(splitCsvLine(line));
            }
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            else if (c == ',' && !inQuotes) { fields.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private String valueAt(String[] row, int index) {
        return row.length > index ? row[index].trim().replace("\"", "") : "";
    }

    private Integer parseInteger(String[] row, int index) {
        String v = valueAt(row, index);
        if (v.isBlank()) return null;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
    }

    private Double parseDouble(String[] row, int index) {
        String v = valueAt(row, index);
        if (v.isBlank()) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return null; }
    }

    private <T> void saveInBatches(List<T> items, Consumer<List<T>> saver) {
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            saver.accept(items.subList(start, Math.min(start + BATCH_SIZE, items.size())));
        }
    }
}
