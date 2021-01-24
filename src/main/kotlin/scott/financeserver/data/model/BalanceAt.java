package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class BalanceAt extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper account;
  private final ValueNode amount;
  private final ValueNode time;

  public BalanceAt(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    amount = entity.getChild("amount", ValueNode.class, true);
    time = entity.getChild("time", ValueNode.class, true);
  }

  public UUID getId() {
    return id.getValue();
  }

  public Account getAccount() {
    return super.getFromRefNode(account.refNode);
  }

  public void setAccount(Account account) {
    setToRefNode(this.account.refNode, account);
  }

  public BigDecimal getAmount() {
    return amount.getValue();
  }

  public void setAmount(BigDecimal amount) {
    this.amount.setValue(amount);
  }

  public Date getTime() {
    return time.getValue();
  }

  public void setTime(Date time) {
    this.time.setValue(time);
  }
}
