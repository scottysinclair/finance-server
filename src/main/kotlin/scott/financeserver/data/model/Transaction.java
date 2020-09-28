package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Transaction extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode date;
  private final ValueNode amount;
  private final ValueNode comment;
  private final ValueNode important;
  private final RefNodeProxyHelper account;
  private final RefNodeProxyHelper category;

  public Transaction(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    date = entity.getChild("date", ValueNode.class, true);
    amount = entity.getChild("amount", ValueNode.class, true);
    comment = entity.getChild("comment", ValueNode.class, true);
    important = entity.getChild("important", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    category = new RefNodeProxyHelper(entity.getChild("category", RefNode.class, true));
  }

  public Long getId() {
    return id.getValue();
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

  public String getComment() {
    return comment.getValue();
  }

  public void setComment(String comment) {
    this.comment.setValue(comment);
  }

  public Boolean getImportant() {
    return important.getValue();
  }

  public void setImportant(Boolean important) {
    this.important.setValue(important);
  }

  public Account getAccount() {
    return super.getFromRefNode(account.refNode);
  }

  public void setAccount(Account account) {
    setToRefNode(this.account.refNode, account);
  }

  public Category getCategory() {
    return super.getFromRefNode(category.refNode);
  }

  public void setCategory(Category category) {
    setToRefNode(this.category.refNode, category);
  }
}
