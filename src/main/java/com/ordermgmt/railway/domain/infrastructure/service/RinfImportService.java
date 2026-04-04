package com.ordermgmt.railway.domain.infrastructure.service;

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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private static final CSVFormat CSV_FORMAT =
            CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

    // ── CSV column indices for Operational Points ─────────────────────
    private static final int OP_COL_UOPID = 0;
    private static final int OP_COL_NAME = 1;
    private static final int OP_COL_WKT = 2;
    private static final int OP_COL_TYPE = 3;
    private static final int OP_COL_TAF_TAP = 4;

    // ── CSV column indices for Sections of Line ──────────────────────
    private static final int SOL_COL_ID = 0;
    private static final int SOL_COL_START_UOPID = 1;
    private static final int SOL_COL_END_UOPID = 2;
    private static final int SOL_COL_LENGTH = 3;

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
     * Import OPs: parse first, then delete+insert atomically. On any error the transaction rolls
     * back — no partial state.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ImportLog importOperationalPoints(InputStream csvStream, String country) {
        try {
            List<CSVRecord> records = parseCsv(csvStream);
            List<OperationalPoint> items = toOperationalPoints(records, country);
            int count = replaceOperationalPoints(items, country);
            return saveLog(
                    "RINF_OP",
                    country,
                    "SUCCESS",
                    count,
                    count + " operational points imported for " + country);
        } catch (Exception e) {
            log.error("OP import failed for {}", country, e);
            return saveLog("RINF_OP", country, "ERROR", 0, sanitizeError(e));
        }
    }

    /** Import SoLs: parse first, then delete+insert atomically. */
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ImportLog importSectionsOfLine(InputStream csvStream, String country) {
        try {
            List<CSVRecord> records = parseCsv(csvStream);
            List<SectionOfLine> items = toSectionsOfLine(records, country);
            int count = replaceSectionsOfLine(items, country);
            return saveLog(
                    "RINF_SOL",
                    country,
                    "SUCCESS",
                    count,
                    count + " sections of line imported for " + country);
        } catch (Exception e) {
            log.error("SoL import failed for {}", country, e);
            return saveLog("RINF_SOL", country, "ERROR", 0, sanitizeError(e));
        }
    }

    /** Atomic delete+insert in a single transaction. Rolls back on any error. */
    protected int replaceOperationalPoints(List<OperationalPoint> items, String country) {
        Integer count =
                transactionOperations.execute(
                        status -> {
                            opRepo.deleteByCountry(country);
                            saveInBatches(items, opRepo::saveAll);
                            log.info("Imported {} OPs for {}", items.size(), country);
                            return items.size();
                        });
        return count != null ? count : 0;
    }

    protected int replaceSectionsOfLine(List<SectionOfLine> items, String country) {
        Integer count =
                transactionOperations.execute(
                        status -> {
                            solRepo.deleteByCountry(country);
                            saveInBatches(items, solRepo::saveAll);
                            log.info("Imported {} SoLs for {}", items.size(), country);
                            return items.size();
                        });
        return count != null ? count : 0;
    }

    private ImportLog saveLog(
            String source, String country, String status, int count, String message) {
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

    private List<OperationalPoint> toOperationalPoints(List<CSVRecord> records, String country) {
        Map<String, OperationalPoint> itemsByUopid = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (CSVRecord record : records) {
            if (record.size() < 2) continue;
            OperationalPoint item = toOperationalPoint(record, country);
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

    private OperationalPoint toOperationalPoint(CSVRecord record, String country) {
        OperationalPoint operationalPoint = new OperationalPoint();
        operationalPoint.setUopid(valueAt(record, OP_COL_UOPID));
        operationalPoint.setName(valueAt(record, OP_COL_NAME));
        operationalPoint.setCountry(country);
        String wktGeometry = valueAt(record, OP_COL_WKT);
        if (!wktGeometry.isBlank()) parseWkt(wktGeometry, operationalPoint);
        Integer opType = parseInteger(record, OP_COL_TYPE);
        if (opType != null) operationalPoint.setOpType(opType);
        String tafTapCode = valueAt(record, OP_COL_TAF_TAP);
        if (!tafTapCode.isBlank()) operationalPoint.setTafTapCode(tafTapCode);
        return operationalPoint;
    }

    private List<SectionOfLine> toSectionsOfLine(List<CSVRecord> records, String country) {
        Map<String, SectionOfLine> itemsBySolId = new LinkedHashMap<>();
        int duplicateCount = 0;
        for (CSVRecord record : records) {
            if (record.size() < 3) continue;
            SectionOfLine item = toSectionOfLine(record, country);
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

    private SectionOfLine toSectionOfLine(CSVRecord record, String country) {
        SectionOfLine sectionOfLine = new SectionOfLine();
        sectionOfLine.setSolId(valueAt(record, SOL_COL_ID));
        sectionOfLine.setStartOpUopid(valueAt(record, SOL_COL_START_UOPID));
        sectionOfLine.setEndOpUopid(valueAt(record, SOL_COL_END_UOPID));
        sectionOfLine.setCountry(country);
        Double lengthMeters = parseDouble(record, SOL_COL_LENGTH);
        if (lengthMeters != null) sectionOfLine.setLengthMeters(lengthMeters);
        return sectionOfLine;
    }

    private void parseWkt(String wktGeometry, OperationalPoint operationalPoint) {
        Matcher matcher = WKT_POINT.matcher(wktGeometry);
        if (matcher.find()) {
            operationalPoint.setLongitude(Double.parseDouble(matcher.group(1)));
            operationalPoint.setLatitude(Double.parseDouble(matcher.group(2)));
        }
    }

    private List<CSVRecord> parseCsv(InputStream is) throws IOException {
        try (CSVParser parser =
                CSV_FORMAT.parse(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return parser.getRecords();
        }
    }

    private String valueAt(CSVRecord record, int index) {
        return record.size() > index ? record.get(index).trim() : "";
    }

    private Integer parseInteger(CSVRecord record, int index) {
        String value = valueAt(record, index);
        if (value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parseDouble(CSVRecord record, int index) {
        String value = valueAt(record, index);
        if (value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private <T> void saveInBatches(List<T> items, Consumer<List<T>> saver) {
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            saver.accept(items.subList(start, Math.min(start + BATCH_SIZE, items.size())));
        }
    }
}
