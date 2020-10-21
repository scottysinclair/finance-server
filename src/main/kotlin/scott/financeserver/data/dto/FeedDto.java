package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.util.Date;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class FeedDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private Date dateImported;
  private AccountDto account;
  private String file;

  public FeedDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Date getDateImported() {
    return dateImported;
  }

  public void setDateImported(Date dateImported) {
    this.dateImported = dateImported;
  }

  public AccountDto getAccount() {
    return account;
  }

  public void setAccount(AccountDto account) {
    this.account = account;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
