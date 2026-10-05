package com.garment.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存工序请求DTO
 */
public class SaveProcessListDTO {

    private List<ProcessItem> processes;

    public List<ProcessItem> getProcesses() { return processes; }
    public void setProcesses(List<ProcessItem> processes) { this.processes = processes; }

    public static class ProcessItem {
        private String name;
        private Integer seq;
        private BigDecimal price;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getSeq() { return seq; }
        public void setSeq(Integer seq) { this.seq = seq; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }
}
