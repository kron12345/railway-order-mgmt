package com.ordermgmt.railway.domain.infrastructure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ordermgmt.railway.domain.infrastructure.model.PredefinedTag;
import com.ordermgmt.railway.domain.infrastructure.repository.PredefinedTagRepository;

@ExtendWith(MockitoExtension.class)
class PredefinedTagImportServiceTest {

    @Mock private PredefinedTagRepository tagRepository;

    @InjectMocks private PredefinedTagImportService importService;

    @Test
    void importsNewTagsAndUpdatesExistingOnes() throws Exception {
        PredefinedTag existing = new PredefinedTag();
        existing.setId(UUID.randomUUID());
        existing.setName("Express");
        existing.setCategory("ORDER");

        when(tagRepository.findByNameAndCategory("Express", "ORDER"))
                .thenReturn(Optional.of(existing));
        when(tagRepository.findByNameAndCategory("Fahrplan", "POSITION"))
                .thenReturn(Optional.empty());

        String csv =
                """
                name,category,color,sort_order,active
                Express,ORDER,#00E676,5,true
                Fahrplan,POSITION,#448AFF,1,false
                """;

        int imported =
                importService.importCsv(
                        new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(imported).isEqualTo(2);

        ArgumentCaptor<PredefinedTag> captor = ArgumentCaptor.forClass(PredefinedTag.class);
        verify(tagRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PredefinedTag::getName, PredefinedTag::getCategory)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Express", "ORDER"),
                        org.assertj.core.groups.Tuple.tuple("Fahrplan", "POSITION"));
        assertThat(captor.getAllValues())
                .extracting(PredefinedTag::isActive)
                .containsExactly(true, false);
    }

    @Test
    void rejectsUnsupportedCategories() {
        String csv =
                """
                name,category,color,sort_order,active
                Test,UNKNOWN,#000000,1,true
                """;

        assertThatThrownBy(
                        () ->
                                importService.importCsv(
                                        new ByteArrayInputStream(
                                                csv.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported tag category");
    }
}
