package scott.financeserver.data.query;

import scott.barleydb.api.query.JoinType;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QueryObject;
import scott.financeserver.data.model.CategoryMatcher;
import java.util.UUID;
import scott.financeserver.data.query.QCategory;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class QCategoryMatcher extends QueryObject<CategoryMatcher> {
  private static final long serialVersionUID = 1L;
  public QCategoryMatcher() {
    super(CategoryMatcher.class);
  }

  public QCategoryMatcher(QueryObject<?> parent) {
    super(CategoryMatcher.class, parent);
  }


  public QProperty<UUID> id() {
    return new QProperty<UUID>(this, "id");
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

  public QProperty<String> pattern() {
    return new QProperty<String>(this, "pattern");
  }
}