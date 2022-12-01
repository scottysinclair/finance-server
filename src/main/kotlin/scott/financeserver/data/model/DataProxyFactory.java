package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.proxy.ProxyFactory;
import scott.barleydb.api.exception.model.ProxyCreationException;
import scott.financeserver.data.model.Account;
import scott.financeserver.data.model.BalanceAt;
import scott.financeserver.data.model.Category;
import scott.financeserver.data.model.CategoryMatcher;
import scott.financeserver.data.model.Duplicates;
import scott.financeserver.data.model.EndOfMonthStatement;
import scott.financeserver.data.model.Feed;
import scott.financeserver.data.model.Transaction;

public class DataProxyFactory implements ProxyFactory {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public <T> T newProxy(Entity entity) throws ProxyCreationException {
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.EndOfMonthStatement.class.getName())) {
      return (T) new EndOfMonthStatement(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.CategoryMatcher.class.getName())) {
      return (T) new CategoryMatcher(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.Category.class.getName())) {
      return (T) new Category(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(Duplicates2.class.getName())) {
      return (T) new Duplicates2(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.Duplicates.class.getName())) {
      return (T) new Duplicates(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.Transaction.class.getName())) {
      return (T) new Transaction(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.Feed.class.getName())) {
      return (T) new Feed(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.BalanceAt.class.getName())) {
      return (T) new BalanceAt(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(scott.financeserver.data.model.Account.class.getName())) {
      return (T) new Account(entity);
    }
    return null;
  }
}
