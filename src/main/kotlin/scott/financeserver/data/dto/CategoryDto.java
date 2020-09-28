package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class CategoryDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String name;
  private Integer monthlyLimit;

  public CategoryDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getMonthlyLimit() {
    return monthlyLimit;
  }

  public void setMonthlyLimit(Integer monthlyLimit) {
    this.monthlyLimit = monthlyLimit;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
