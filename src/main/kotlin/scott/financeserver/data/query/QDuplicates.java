package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Duplicates;
import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QDuplicates extends QueryObject<Duplicates> {
  private static final long serialVersionUID = 1L;
  public QDuplicates() {
    super(Duplicates.class);
  }

  public QDuplicates(QueryObject<?> parent) {
    super(Duplicates.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<String> feedHash() {
    return new QProperty<String>(this, "feedHash");
  }

  public QProperty<Integer> feedRecordNumber() {
    return new QProperty<Integer>(this, "feedRecordNumber");
  }

  public QProperty<String> contentHash() {
    return new QProperty<String>(this, "contentHash");
  }

  public QProperty<String> content() {
    return new QProperty<String>(this, "content");
  }
}