package scott.financeserver.data;

import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.Environment;

public class DataEntityContext extends EntityContext {

  private static final long serialVersionUID = 1L;

  public DataEntityContext(Environment env) {
    super(env, "scott.financeserver.data");
  }
}