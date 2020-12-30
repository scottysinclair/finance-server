package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.BalanceAt;
import java.util.UUID;
import scott.financeserver.data.query.QAccount;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QBalanceAt extends QueryObject<BalanceAt> {
  private static final long serialVersionUID = 1L;
  public QBalanceAt() {
    super(BalanceAt.class);
  }

  public QBalanceAt(QueryObject<?> parent) {
    super(BalanceAt.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<UUID> accountId() {
    return new QProperty<UUID>(this, "account");
  }

  public QAccount joinToAccount() {
    QAccount account = new QAccount();
    addLeftOuterJoin(account, "account");
    return account;
  }

  public QAccount joinToAccount(JoinType joinType) {
    QAccount account = new QAccount();
    addJoin(account, "account", joinType);
    return account;
  }

  public QAccount existsAccount() {
    QAccount account = new QAccount(this);
    addExists(account, "account");
    return account;
  }

  public QProperty<BigDecimal> amount() {
    return new QProperty<BigDecimal>(this, "amount");
  }

  public QProperty<Date> time() {
    return new QProperty<Date>(this, "time");
  }
}