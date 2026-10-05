package com.garment.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/编辑工序模板请求DTO
 */
public class SaveProcessTemplateDTO {

    private String name;
    private String remark;
    private List<TemplateItem> items;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<TemplateItem> getItems() { return items; }
    public void setItems(List<TemplateItem> items) { this.items = items; }

    /**
     * 模板工序项
     */
    public static class TemplateItem {
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
