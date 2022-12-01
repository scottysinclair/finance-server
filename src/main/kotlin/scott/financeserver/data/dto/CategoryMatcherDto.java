package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;
import scott.financeserver.data.dto.CategoryDto;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CategoryMatcherDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private scott.financeserver.data.dto.CategoryDto category;
  private String pattern;

  public CategoryMatcherDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public scott.financeserver.data.dto.CategoryDto getCategory() {
    return category;
  }

  public void setCategory(CategoryDto category) {
    this.category = category;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
