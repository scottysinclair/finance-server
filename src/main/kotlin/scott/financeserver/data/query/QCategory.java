package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Category;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QCategory extends QueryObject<Category> {
  private static final long serialVersionUID = 1L;
  public QCategory() {
    super(Category.class);
  }

  public QCategory(QueryObject<?> parent) {
    super(Category.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QProperty<Integer> monthlyLimit() {
    return new QProperty<Integer>(this, "monthlyLimit");
  }
}