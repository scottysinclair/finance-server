package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Account extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;

  public Account(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
  }

  public UUID getId() {
    return id.getValue();
  }

  public String getName() {
    return name.getValue();
  }

  public void setName(String name) {
    this.name.setValue(name);
  }
}
