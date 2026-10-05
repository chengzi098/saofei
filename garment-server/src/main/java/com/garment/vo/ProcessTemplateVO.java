package com.garment.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序模板VO
 */
public class ProcessTemplateVO {

    private Long id;
    private String name;
    private String remark;
    private Integer itemCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<TemplateItem> items;

    public static class TemplateItem {
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public List<TemplateItem> getItems() { return items; }
    public void setItems(List<TemplateItem> items) { this.items = items; }
}
