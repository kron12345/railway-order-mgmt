package com.ordermgmt.railway.domain.infrastructure.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
            List<String[]> rows = parseCsv(csvStream);
            opRepo.deleteByCountry(country);

            List<OperationalPoint> batch = new ArrayList<>(BATCH_SIZE);
            int count = 0;
            for (String[] row : rows) {
                if (row.length < 2) continue;
                OperationalPoint op = new OperationalPoint();
                op.setUopid(clean(row[0]));
                op.setName(clean(row[1]));
                op.setCountry(country);

                if (row.length > 2 && !row[2].isBlank()) {
                    parseWkt(clean(row[2]), op);
                }
                if (row.length > 3 && !row[3].isBlank()) {
                    try {
                        op.setOpType(Integer.parseInt(clean(row[3])));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (row.length > 4 && !row[4].isBlank()) {
                    op.setTafTapCode(clean(row[4]));
                }

                batch.add(op);
                if (batch.size() >= BATCH_SIZE) {
                    opRepo.saveAll(batch);
                    batch.clear();
                }
                count++;
            }
            if (!batch.isEmpty()) opRepo.saveAll(batch);

            entry.setRecordCount(count);
            entry.setStatus("SUCCESS");
            entry.setMessage(count + " operational points imported for " + country);
            log.info("Imported {} OPs for {}", count, country);
        } catch (Exception e) {
            entry.setStatus("ERROR");
            entry.setMessage(e.getMessage());
            log.error("OP import failed for {}", country, e);
        }
        entry.setFinishedAt(Instant.now());
        return logRepo.save(entry);
    }

    @Transactional
    public ImportLog importSectionsOfLine(InputStream csvStream, String country) {
        ImportLog entry = newLog("RINF_SOL", country);
        try {
            List<String[]> rows = parseCsv(csvStream);
            solRepo.deleteByCountry(country);

            List<SectionOfLine> batch = new ArrayList<>(BATCH_SIZE);
            int count = 0;
            for (String[] row : rows) {
                if (row.length < 3) continue;
                SectionOfLine sol = new SectionOfLine();
                sol.setSolId(clean(row[0]));
                sol.setStartOpUopid(clean(row[1]));
                sol.setEndOpUopid(clean(row[2]));
                sol.setCountry(country);

                if (row.length > 3 && !row[3].isBlank()) {
                    try {
                        sol.setLengthMeters(Double.parseDouble(clean(row[3])));
                    } catch (NumberFormatException ignored) {
                    }
                }

                batch.add(sol);
                if (batch.size() >= BATCH_SIZE) {
                    solRepo.saveAll(batch);
                    batch.clear();
                }
                count++;
            }
            if (!batch.isEmpty()) solRepo.saveAll(batch);

            entry.setRecordCount(count);
            entry.setStatus("SUCCESS");
            entry.setMessage(count + " sections of line imported for " + country);
            log.info("Imported {} SoLs for {}", count, country);
        } catch (Exception e) {
            entry.setStatus("ERROR");
            entry.setMessage(e.getMessage());
            log.error("SoL import failed for {}", country, e);
        }
        entry.setFinishedAt(Instant.now());
        return logRepo.save(entry);
    }

    private void parseWkt(String wkt, OperationalPoint op) {
        Matcher m = WKT_POINT.matcher(wkt);
        if (m.find()) {
            op.setLongitude(Double.parseDouble(m.group(1)));
            op.setLatitude(Double.parseDouble(m.group(2)));
        }
    }

    private List<String[]> parseCsv(InputStream is) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String header = reader.readLine(); // skip header
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

    private ImportLog newLog(String source, String country) {
        ImportLog entry = new ImportLog();
        entry.setSource(source);
        entry.setCountry(country);
        entry.setStatus("RUNNING");
        return entry;
    }
}
