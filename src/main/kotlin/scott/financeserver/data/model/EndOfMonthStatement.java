package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class EndOfMonthStatement extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper account;
  private final ValueNode year;
  private final ValueNode month;
  private final ValueNode amount;

  public EndOfMonthStatement(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    year = entity.getChild("year", ValueNode.class, true);
    month = entity.getChild("month", ValueNode.class, true);
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

  public Integer getYear() {
    return year.getValue();
  }

  public void setYear(Integer year) {
    this.year.setValue(year);
  }

  public Integer getMonth() {
    return month.getValue();
  }

  public void setMonth(Integer month) {
    this.month.setValue(month);
  }

  public BigDecimal getAmount() {
    return amount.getValue();
  }

  public void setAmount(BigDecimal amount) {
    this.amount.setValue(amount);
  }
}
