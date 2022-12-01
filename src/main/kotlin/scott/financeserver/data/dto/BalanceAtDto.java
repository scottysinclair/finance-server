package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;
import scott.financeserver.data.dto.AccountDto;

import java.util.UUID;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class BalanceAtDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private scott.financeserver.data.dto.AccountDto account;
  private BigDecimal amount;
  private Date time;

  public BalanceAtDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public scott.financeserver.data.dto.AccountDto getAccount() {
    return account;
  }

  public void setAccount(AccountDto account) {
    this.account = account;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
