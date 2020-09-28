package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class MonthDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private Date starting;
  private BigDecimal startingBalance;
  private Boolean finished;

  public MonthDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Date getStarting() {
    return starting;
  }

  public void setStarting(Date starting) {
    this.starting = starting;
  }

  public BigDecimal getStartingBalance() {
    return startingBalance;
  }

  public void setStartingBalance(BigDecimal startingBalance) {
    this.startingBalance = startingBalance;
  }

  public Boolean getFinished() {
    return finished;
  }

  public void setFinished(Boolean finished) {
    this.finished = finished;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
