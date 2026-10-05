package com.garment.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/编辑床号请求DTO
 */
public class SaveBedDTO {

    private String bedNo;
    private String name;
    private String colors;
    private Integer quantity;
    private String remark;
    private List<ProcessItem> processes;

    public String getBedNo() { return bedNo; }
    public void setBedNo(String bedNo) { this.bedNo = bedNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColors() { return colors; }
    public void setColors(String colors) { this.colors = colors; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<ProcessItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessItem> processes) { this.processes = processes; }

    /**
     * 工序项
     */
    public static class ProcessItem {
        private Long id;
        private String name;
        private Integer seq;
        private BigDecimal price;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getSeq() { return seq; }
        public void setSeq(Integer seq) { this.seq = seq; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}
