package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import scott.barleydb.api.dto.DtoList;
import scott.financeserver.data.dto.CategoryMatcherDto;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CategoryDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private String name;
  private DtoList<scott.financeserver.data.dto.CategoryMatcherDto> matchers = new DtoList<>();

  public CategoryDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DtoList<CategoryMatcherDto> getMatchers() {
    return matchers;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
