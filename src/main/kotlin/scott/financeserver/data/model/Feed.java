package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.proxy.RefNodeProxyHelper;
import java.util.UUID;
import java.util.Date;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Feed extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode dateImported;
  private final RefNodeProxyHelper account;
  private final ValueNode file;

  public Feed(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    dateImported = entity.getChild("dateImported", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    file = entity.getChild("file", ValueNode.class, true);
  }

  public UUID getId() {
    return id.getValue();
  }

  public Date getDateImported() {
    return dateImported.getValue();
  }

  public void setDateImported(Date dateImported) {
    this.dateImported.setValue(dateImported);
  }

  public Account getAccount() {
    return super.getFromRefNode(account.refNode);
  }

  public void setAccount(Account account) {
    setToRefNode(this.account.refNode, account);
  }

  public String getFile() {
    return file.getValue();
  }

  public void setFile(String file) {
    this.file.setValue(file);
  }
}
