package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class EndOfMonthStatementDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private AccountDto account;
  private Integer year;
  private Integer month;
  private BigDecimal amount;

  public EndOfMonthStatementDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public AccountDto getAccount() {
    return account;
  }

  public void setAccount(AccountDto account) {
    this.account = account;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public Integer getMonth() {
    return month;
  }

  public void setMonth(Integer month) {
    this.month = month;
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
