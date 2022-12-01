package scott.financeserver.data.model;

import java.util.List;
import scott.barleydb.api.stream.ObjectInputStream;
import scott.barleydb.api.stream.QueryEntityInputStream;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.exception.BarleyDBRuntimeException;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.proxy.ToManyNodeProxyHelper;
import scott.financeserver.data.model.CategoryMatcher;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class Category extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode name;
  private final ToManyNodeProxyHelper matchers;

  public Category(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    name = entity.getChild("name", ValueNode.class, true);
    matchers = new ToManyNodeProxyHelper(entity.getChild("matchers", ToManyNode.class, true));
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

  public List<scott.financeserver.data.model.CategoryMatcher> getMatchers() {
    return super.getListProxy(matchers.toManyNode);
  }
  public ObjectInputStream<scott.financeserver.data.model.CategoryMatcher> streamMatchers() throws BarleyDBRuntimeException {
    try {final QueryEntityInputStream in = matchers.toManyNode.stream();
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }

  public ObjectInputStream<scott.financeserver.data.model.CategoryMatcher> streamMatchers(QueryObject<CategoryMatcher> query) throws BarleyDBRuntimeException  {
    try { final QueryEntityInputStream in = matchers.toManyNode.stream(query);
         return new ObjectInputStream<>(in);
    }catch(Exception x) {
      BarleyDBRuntimeException x2 = new BarleyDBRuntimeException(x.getMessage());
      x2.setStackTrace(x.getStackTrace()); 
      throw x2;
    }
  }
}
