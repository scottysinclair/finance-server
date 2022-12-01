package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class Duplicates2 extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode date;
  private final ValueNode amount;
  private final ValueNode duplicateTransaction;
  private final ValueNode sourceTransaction;

  public Duplicates2(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    date = entity.getChild("date", ValueNode.class, true);
    amount = entity.getChild("amount", ValueNode.class, true);
    duplicateTransaction = entity.getChild("duplicateTransaction", ValueNode.class, true);
    sourceTransaction = entity.getChild("sourceTransaction", ValueNode.class, true);
  }

  public UUID getId() {
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

  public String getDuplicateTransaction() {
    return duplicateTransaction.getValue();
  }

  public void setDuplicateTransaction(String duplicateTransaction) {
    this.duplicateTransaction.setValue(duplicateTransaction);
  }

  public String getSourceTransaction() {
    return sourceTransaction.getValue();
  }

  public void setSourceTransaction(String sourceTransaction) {
    this.sourceTransaction.setValue(sourceTransaction);
  }
}
