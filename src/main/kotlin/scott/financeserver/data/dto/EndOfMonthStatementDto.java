package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;
import scott.financeserver.data.dto.AccountDto;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class EndOfMonthStatementDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private scott.financeserver.data.dto.AccountDto account;
  private Date date;
  private BigDecimal amount;

  public EndOfMonthStatementDto() {
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

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
