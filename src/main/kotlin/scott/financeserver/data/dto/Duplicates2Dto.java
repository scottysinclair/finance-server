package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class Duplicates2Dto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private Date date;
  private BigDecimal amount;
  private String duplicateTransaction;
  private String sourceTransaction;

  public Duplicates2Dto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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

  public String getDuplicateTransaction() {
    return duplicateTransaction;
  }

  public void setDuplicateTransaction(String duplicateTransaction) {
    this.duplicateTransaction = duplicateTransaction;
  }

  public String getSourceTransaction() {
    return sourceTransaction;
  }

  public void setSourceTransaction(String sourceTransaction) {
    this.sourceTransaction = sourceTransaction;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
