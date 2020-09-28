package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.exception.model.ProxyCreationException;


public class DataProxyFactory implements ProxyFactory {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public <T> T newProxy(Entity entity) throws ProxyCreationException {
    if (entity.getEntityType().getInterfaceName().equals(Month.class.getName())) {
      return (T) new Month(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(Category.class.getName())) {
      return (T) new Category(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(Transaction.class.getName())) {
      return (T) new Transaction(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(Account.class.getName())) {
      return (T) new Account(entity);
    }
    return null;
  }
}
