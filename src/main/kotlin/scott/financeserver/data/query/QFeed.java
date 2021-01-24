package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Feed;
import java.util.UUID;
import java.util.Date;
import scott.financeserver.data.query.QAccount;
import scott.financeserver.data.model.FeedState;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QFeed extends QueryObject<Feed> {
  private static final long serialVersionUID = 1L;
  public QFeed() {
    super(Feed.class);
  }

  public QFeed(QueryObject<?> parent) {
    super(Feed.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<String> contentHash() {
    return new QProperty<String>(this, "contentHash");
  }

  public QProperty<Date> dateImported() {
    return new QProperty<Date>(this, "dateImported");
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

  public QProperty<String> file() {
    return new QProperty<String>(this, "file");
  }

  public QProperty<scott.financeserver.data.model.FeedState> state() {
    return new QProperty<scott.financeserver.data.model.FeedState>(this, "state");
  }
}