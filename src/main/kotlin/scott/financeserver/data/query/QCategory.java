package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.Category;
import java.util.UUID;
import scott.financeserver.data.query.QCategoryMatcher;

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


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QCategoryMatcher joinToMatchers() {
    QCategoryMatcher matchers = new QCategoryMatcher();
    addLeftOuterJoin(matchers, "matchers");
    return matchers;
  }

  public QCategoryMatcher joinToMatchers(JoinType joinType) {
    QCategoryMatcher matchers = new QCategoryMatcher();
    addJoin(matchers, "matchers", joinType);
    return matchers;
  }

  public QCategoryMatcher existsMatchers() {
    QCategoryMatcher matchers = new QCategoryMatcher(this);
    addExists(matchers, "matchers");
    return matchers;
  }
}