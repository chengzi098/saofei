package com.garment.service;

import com.garment.common.TenantContext;
import com.garment.dto.SaveBedDTO;
import com.garment.entity.Bed;
import com.garment.entity.Bundle;
import com.garment.entity.BundleProcess;
import com.garment.entity.Process;
import com.garment.entity.Style;
import com.garment.mapper.BedMapper;
import com.garment.mapper.BundleMapper;
import com.garment.mapper.BundleProcessMapper;
import com.garment.mapper.ProcessMapper;
import com.garment.mapper.StyleMapper;
import com.garment.mapper.WorkRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BedServiceTest {

    private final BedMapper bedMapper = mock(BedMapper.class);
    private final StyleMapper styleMapper = mock(StyleMapper.class);
    private final BundleMapper bundleMapper = mock(BundleMapper.class);
    private final ProcessMapper processMapper = mock(ProcessMapper.class);
    private final BundleProcessMapper bundleProcessMapper = mock(BundleProcessMapper.class);
    private final WorkRecordMapper workRecordMapper = mock(WorkRecordMapper.class);
    private final BedService bedService = new BedService(
            bedMapper, styleMapper, bundleMapper, processMapper, bundleProcessMapper, workRecordMapper);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void addBedRequiresName() {
        mockStyle();
        SaveBedDTO dto = validBed();
        dto.setName(" ");

        RuntimeException error = assertThrows(RuntimeException.class, () -> bedService.addBed(10L, dto));

        assertTrue(error.getMessage().contains("名称"));
    }

    @Test
    void addBedRejectsDuplicateBedNoAndName() {
        mockStyle();
        when(bedMapper.selectCount(any())).thenReturn(1L);
        SaveBedDTO dto = validBed();

        RuntimeException error = assertThrows(RuntimeException.class, () -> bedService.addBed(10L, dto));

        assertTrue(error.getMessage().contains("床号和名称"), error.getMessage());
    }

    @Test
    void updateBedRejectsDeletingReportedZeroPriceProcess() {
        TenantContext.setCompanyId(1L);

        Bed bed = new Bed();
        bed.setId(20L);
        bed.setStyleId(10L);
        bed.setCompanyId(1L);
        bed.setDeleted(0);
        when(bedMapper.selectById(20L)).thenReturn(bed);

        Process process = new Process();
        process.setId(30L);
        process.setBedId(20L);
        process.setCompanyId(1L);
        process.setName("车缝");
        process.setPrice(BigDecimal.ZERO);
        when(processMapper.selectList(any())).thenReturn(List.of(process));

        Bundle bundle = new Bundle();
        bundle.setId(40L);
        when(bundleMapper.selectList(any())).thenReturn(List.of(bundle));

        BundleProcess bundleProcess = new BundleProcess();
        bundleProcess.setId(50L);
        when(bundleProcessMapper.selectList(any())).thenReturn(List.of(bundleProcess));
        when(workRecordMapper.selectCount(any())).thenReturn(1L);

        SaveBedDTO dto = new SaveBedDTO();
        dto.setProcesses(List.of());

        RuntimeException error = assertThrows(RuntimeException.class, () -> bedService.updateBed(20L, dto));

        assertTrue(error.getMessage().contains("已有员工报工"), error.getMessage());
    }

    private void mockStyle() {
        TenantContext.setCompanyId(1L);
        Style style = new Style();
        style.setId(10L);
        style.setCompanyId(1L);
        when(styleMapper.selectById(10L)).thenReturn(style);
    }

    private SaveBedDTO validBed() {
        SaveBedDTO dto = new SaveBedDTO();
        dto.setBedNo("1号床");
        dto.setName("大货");
        dto.setQuantity(100);
        return dto;
    }
}
