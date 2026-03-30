package com.ordermgmt.railway.domain.infrastructure.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ImportLog> getImportHistory() {
        return logRepo.findAllByOrderByStartedAtDesc();
    }

    public long countOps(String country) {
        return country != null ? opRepo.countByCountry(country) : opRepo.count();
    }

    public long countSols(String country) {
        return country != null ? solRepo.countByCountry(country) : solRepo.count();
    }

    @Transactional
    public ImportLog importOperationalPoints(InputStream csvStream, String country) {
        ImportLog entry = newLog("RINF_OP", country);
        try {
            List<OperationalPoint> operationalPoints =
                    toOperationalPoints(parseCsv(csvStream), country);
            opRepo.deleteByCountry(country);
            saveInBatches(operationalPoints, opRepo::saveAll);
            markSuccess(
                    entry,
                    operationalPoints.size(),
                    operationalPoints.size() + " operational points imported for " + country);
            log.info("Imported {} OPs for {}", operationalPoints.size(), country);
        } catch (Exception e) {
            markFailure(entry, e);
            log.error("OP import failed for {}", country, e);
        }
        return finish(entry);
    }

    @Transactional
    public ImportLog importSectionsOfLine(InputStream csvStream, String country) {
        ImportLog entry = newLog("RINF_SOL", country);
        try {
            List<SectionOfLine> sectionsOfLine = toSectionsOfLine(parseCsv(csvStream), country);
            solRepo.deleteByCountry(country);
            saveInBatches(sectionsOfLine, solRepo::saveAll);
            markSuccess(
                    entry,
                    sectionsOfLine.size(),
                    sectionsOfLine.size() + " sections of line imported for " + country);
            log.info("Imported {} SoLs for {}", sectionsOfLine.size(), country);
        } catch (Exception e) {
            markFailure(entry, e);
            log.error("SoL import failed for {}", country, e);
        }
        return finish(entry);
    }

    private List<OperationalPoint> toOperationalPoints(List<String[]> rows, String country) {
        List<OperationalPoint> operationalPoints = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 2) {
                continue;
            }
            operationalPoints.add(toOperationalPoint(row, country));
        }
        return operationalPoints;
    }

    private OperationalPoint toOperationalPoint(String[] row, String country) {
        OperationalPoint operationalPoint = new OperationalPoint();
        operationalPoint.setUopid(valueAt(row, 0));
        operationalPoint.setName(valueAt(row, 1));
        operationalPoint.setCountry(country);

        String wkt = valueAt(row, 2);
        if (!wkt.isBlank()) {
            parseWkt(wkt, operationalPoint);
        }

        Integer opType = parseInteger(row, 3);
        if (opType != null) {
            operationalPoint.setOpType(opType);
        }

        String tafTapCode = valueAt(row, 4);
        if (!tafTapCode.isBlank()) {
            operationalPoint.setTafTapCode(tafTapCode);
        }
        return operationalPoint;
    }

    private List<SectionOfLine> toSectionsOfLine(List<String[]> rows, String country) {
        List<SectionOfLine> sectionsOfLine = new ArrayList<>();
        for (String[] row : rows) {
            if (row.length < 3) {
                continue;
            }
            sectionsOfLine.add(toSectionOfLine(row, country));
        }
        return sectionsOfLine;
    }

    private SectionOfLine toSectionOfLine(String[] row, String country) {
        SectionOfLine sectionOfLine = new SectionOfLine();
        sectionOfLine.setSolId(valueAt(row, 0));
        sectionOfLine.setStartOpUopid(valueAt(row, 1));
        sectionOfLine.setEndOpUopid(valueAt(row, 2));
        sectionOfLine.setCountry(country);

        Double lengthMeters = parseDouble(row, 3);
        if (lengthMeters != null) {
            sectionOfLine.setLengthMeters(lengthMeters);
        }
        return sectionOfLine;
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
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(splitCsvLine(line));
                }
            }
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private String clean(String s) {
        return s != null ? s.trim().replace("\"", "") : "";
    }

    private String valueAt(String[] row, int index) {
        return row.length > index ? clean(row[index]) : "";
    }

    private Integer parseInteger(String[] row, int index) {
        String value = valueAt(row, index);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parseDouble(String[] row, int index) {
        String value = valueAt(row, index);
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private <T> void saveInBatches(List<T> items, Consumer<List<T>> saver) {
        for (int start = 0; start < items.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, items.size());
            saver.accept(items.subList(start, end));
        }
    }

    private void markSuccess(ImportLog entry, int recordCount, String message) {
        entry.setRecordCount(recordCount);
        entry.setStatus("SUCCESS");
        entry.setMessage(message);
    }

    private void markFailure(ImportLog entry, Exception exception) {
        entry.setStatus("ERROR");
        entry.setMessage(exception.getMessage());
    }

    private ImportLog finish(ImportLog entry) {
        entry.setFinishedAt(Instant.now());
        return logRepo.save(entry);
    }

    private ImportLog newLog(String source, String country) {
        ImportLog entry = new ImportLog();
        entry.setSource(source);
        entry.setCountry(country);
        entry.setStatus("RUNNING");
        return entry;
    }
}
