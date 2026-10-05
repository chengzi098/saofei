package com.garment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量保存床号DTO
 */
public class SaveBedListDTO {

    @NotEmpty(message = "至少需要一个床号")
    @Valid
    private List<SaveBedDTO> beds;

    public List<SaveBedDTO> getBeds() { return beds; }
    public void setBeds(List<SaveBedDTO> beds) { this.beds = beds; }
}
