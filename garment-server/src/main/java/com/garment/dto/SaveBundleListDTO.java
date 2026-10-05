package com.garment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量保存扎货DTO
 */
public class SaveBundleListDTO {

    @NotNull(message = "床号ID不能为空")
    private Long bedId;

    @NotEmpty(message = "至少需要一扎")
    @Valid
    private List<SaveBundleItemDTO> bundles;

    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public List<SaveBundleItemDTO> getBundles() { return bundles; }
    public void setBundles(List<SaveBundleItemDTO> bundles) { this.bundles = bundles; }
}
