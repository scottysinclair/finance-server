package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class EndOfMonthStatement extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper account;
  private final ValueNode date;
  private final ValueNode amount;

  public EndOfMonthStatement(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    date = entity.getChild("date", ValueNode.class, true);
    amount = entity.getChild("amount", ValueNode.class, true);
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

  public Date getDate() {
    return date.getValue();
  }

  public void setDate(Date date) {
    this.date.setValue(date);
  }

  public BigDecimal getAmount() {
    return amount.getValue();
  }

  public void setAmount(BigDecimal amount) {
    this.amount.setValue(amount);
  }
}
