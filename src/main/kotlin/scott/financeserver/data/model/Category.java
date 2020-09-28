package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Category extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final ValueNode monthlyLimit;

  public Category(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    monthlyLimit = entity.getChild("monthlyLimit", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }

  public Integer getMonthlyLimit() {
    return monthlyLimit.getValue();
  }

  public void setMonthlyLimit(Integer monthlyLimit) {
    this.monthlyLimit.setValue(monthlyLimit);
  }
}
