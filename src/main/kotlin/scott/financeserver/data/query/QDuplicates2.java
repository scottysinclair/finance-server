package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Duplicates2;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QDuplicates2 extends QueryObject<Duplicates2> {
  private static final long serialVersionUID = 1L;
  public QDuplicates2() {
    super(Duplicates2.class);
  }

  public QDuplicates2(QueryObject<?> parent) {
    super(Duplicates2.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<Date> date() {
    return new QProperty<Date>(this, "date");
  }

  public QProperty<BigDecimal> amount() {
    return new QProperty<BigDecimal>(this, "amount");
  }

  public QProperty<String> duplicateTransaction() {
    return new QProperty<String>(this, "duplicateTransaction");
  }

  public QProperty<String> sourceTransaction() {
    return new QProperty<String>(this, "sourceTransaction");
  }
}