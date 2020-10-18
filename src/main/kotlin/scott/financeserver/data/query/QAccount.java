package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Account;
import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QAccount extends QueryObject<Account> {
  private static final long serialVersionUID = 1L;
  public QAccount() {
    super(Account.class);
  }

  public QAccount(QueryObject<?> parent) {
    super(Account.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }
}