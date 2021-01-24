package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class AccountDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private String name;

  public AccountDto() {
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
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
