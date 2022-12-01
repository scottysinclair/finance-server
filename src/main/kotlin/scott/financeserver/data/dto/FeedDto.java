package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.util.Date;

import scott.financeserver.data.dto.AccountDto;
import scott.financeserver.data.model.FeedState;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class FeedDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private String contentHash;
  private Date dateImported;
  private scott.financeserver.data.dto.AccountDto account;
  private String file;
  private FeedState state;

  public FeedDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(String contentHash) {
    this.contentHash = contentHash;
  }

  public Date getDateImported() {
    return dateImported;
  }

  public void setDateImported(Date dateImported) {
    this.dateImported = dateImported;
  }

  public scott.financeserver.data.dto.AccountDto getAccount() {
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

  public scott.financeserver.data.model.FeedState getState() {
    return state;
  }

  public void setState(scott.financeserver.data.model.FeedState state) {
    this.state = state;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
