package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import scott.financeserver.data.model.Category;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class CategoryMatcher extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper category;
  private final ValueNode pattern;

  public CategoryMatcher(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    category = new RefNodeProxyHelper(entity.getChild("category", RefNode.class, true));
    pattern = entity.getChild("pattern", ValueNode.class, true);
  }

  public UUID getId() {
    return id.getValue();
  }

  public scott.financeserver.data.model.Category getCategory() {
    return super.getFromRefNode(category.refNode);
  }

  public void setCategory(Category category) {
    setToRefNode(this.category.refNode, category);
  }

  public String getPattern() {
    return pattern.getValue();
  }

  public void setPattern(String pattern) {
    this.pattern.setValue(pattern);
  }
}
