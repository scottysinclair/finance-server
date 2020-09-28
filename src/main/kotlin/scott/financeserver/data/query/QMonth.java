package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Month;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QMonth extends QueryObject<Month> {
  private static final long serialVersionUID = 1L;
  public QMonth() {
    super(Month.class);
  }

  public QMonth(QueryObject<?> parent) {
    super(Month.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<Date> starting() {
    return new QProperty<Date>(this, "starting");
  }

  public QProperty<BigDecimal> startingBalance() {
    return new QProperty<BigDecimal>(this, "startingBalance");
  }

  public QProperty<Boolean> finished() {
    return new QProperty<Boolean>(this, "finished");
  }
}