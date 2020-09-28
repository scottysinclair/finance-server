package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Transaction;
import java.util.Date;
import java.math.BigDecimal;
import scott.financeserver.data.query.QAccount;
import scott.financeserver.data.query.QCategory;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QTransaction extends QueryObject<Transaction> {
  private static final long serialVersionUID = 1L;
  public QTransaction() {
    super(Transaction.class);
  }

  public QTransaction(QueryObject<?> parent) {
    super(Transaction.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<Date> date() {
    return new QProperty<Date>(this, "date");
  }

  public QProperty<BigDecimal> amount() {
    return new QProperty<BigDecimal>(this, "amount");
  }

  public QProperty<String> comment() {
    return new QProperty<String>(this, "comment");
  }

  public QProperty<Boolean> important() {
    return new QProperty<Boolean>(this, "important");
  }

  public QProperty<Long> accountId() {
    return new QProperty<Long>(this, "account");
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

  public QProperty<Long> categoryId() {
    return new QProperty<Long>(this, "category");
  }

  public QCategory joinToCategory() {
    QCategory category = new QCategory();
    addLeftOuterJoin(category, "category");
    return category;
  }

  public QCategory joinToCategory(JoinType joinType) {
    QCategory category = new QCategory();
    addJoin(category, "category", joinType);
    return category;
  }

  public QCategory existsCategory() {
    QCategory category = new QCategory(this);
    addExists(category, "category");
    return category;
  }
}