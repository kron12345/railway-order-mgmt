package com.ordermgmt.railway.domain.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.ordermgmt.railway.domain.infrastructure.model.ImportLog;
import com.ordermgmt.railway.domain.infrastructure.model.OperationalPoint;
import com.ordermgmt.railway.domain.infrastructure.repository.ImportLogRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.OperationalPointRepository;
import com.ordermgmt.railway.domain.infrastructure.repository.SectionOfLineRepository;

@ExtendWith(MockitoExtension.class)
class RinfImportServiceTest {

    @Mock
    private OperationalPointRepository opRepo;

    @Mock
    private SectionOfLineRepository solRepo;

    @Mock
    private ImportLogRepository logRepo;

    @Mock
    private TransactionOperations transactionOperations;

    @InjectMocks
    private RinfImportService importService;

    @Test
    void deduplicatesOperationalPointsByUopidBeforePersisting() {
        when(transactionOperations.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<Integer> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
        when(logRepo.save(any(ImportLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv =
                """
                "uopid","name","wkt","typeCode","tafTap"
                "DE00KWR","Wuppertal Rauenthal","POINT(7.237149 51.268574)","10","DE21373"
                "DE00KWR","Wuppertal Rauenthal","POINT(7.237149 51.268574)","10","DE21373"
                "DE0TRXS","Renningen Süd","POINT(8.932076 48.762682)","70","DE18798"
                """;

        ImportLog result =
                importService.importOperationalPoints(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "DEU");

        ArgumentCaptor<List<OperationalPoint>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(opRepo).deleteByCountry("DEU");
        verify(opRepo).saveAll(batchCaptor.capture());

        assertThat(batchCaptor.getValue())
                .extracting(OperationalPoint::getUopid)
                .containsExactly("DE00KWR", "DE0TRXS");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getRecordCount()).isEqualTo(2);
    }

    @Test
    void recordsErrorLogWhenTransactionalImportFails() {
        when(transactionOperations.execute(any()))
                .thenThrow(new IllegalStateException("duplicate key value violates unique constraint"));
        when(logRepo.save(any(ImportLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String csv =
                """
                "uopid","name","wkt","typeCode","tafTap"
                "DE00KWR","Wuppertal Rauenthal","POINT(7.237149 51.268574)","10","DE21373"
                """;

        ImportLog result =
                importService.importOperationalPoints(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), "DEU");

        assertThat(result.getStatus()).isEqualTo("ERROR");
        assertThat(result.getRecordCount()).isZero();
        assertThat(result.getMessage()).contains("duplicate key");
    }
}
