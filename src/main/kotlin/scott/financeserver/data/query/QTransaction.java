package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Transaction;
import java.util.UUID;
import scott.financeserver.data.query.QAccount;
import java.util.Date;
import scott.financeserver.data.query.QCategory;
import java.math.BigDecimal;

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


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<String> content() {
    return new QProperty<String>(this, "content");
  }

  public QProperty<String> contentHash() {
    return new QProperty<String>(this, "contentHash");
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

  public QProperty<Date> date() {
    return new QProperty<Date>(this, "date");
  }

  public QProperty<UUID> categoryId() {
    return new QProperty<UUID>(this, "category");
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

  public QProperty<Boolean> userCategorized() {
    return new QProperty<Boolean>(this, "userCategorized");
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
}