package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Month extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode starting;
  private final ValueNode startingBalance;
  private final ValueNode finished;

  public Month(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    starting = entity.getChild("starting", ValueNode.class, true);
    startingBalance = entity.getChild("startingBalance", ValueNode.class, true);
    finished = entity.getChild("finished", ValueNode.class, true);
  }

  public Long getId() {
    return id.getValue();
  }

  public Date getStarting() {
    return starting.getValue();
  }

  public void setStarting(Date starting) {
    this.starting.setValue(starting);
  }

  public BigDecimal getStartingBalance() {
    return startingBalance.getValue();
  }

  public void setStartingBalance(BigDecimal startingBalance) {
    this.startingBalance.setValue(startingBalance);
  }

  public Boolean getFinished() {
    return finished.getValue();
  }

  public void setFinished(Boolean finished) {
    this.finished.setValue(finished);
  }
}
