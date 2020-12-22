package scott.financeserver.data.model;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.core.proxy.AbstractCustomEntityProxy;
import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class Duplicates extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final ValueNode feedHash;
  private final ValueNode feedRecordNumber;
  private final ValueNode contentHash;
  private final ValueNode content;

  public Duplicates(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    feedHash = entity.getChild("feedHash", ValueNode.class, true);
    feedRecordNumber = entity.getChild("feedRecordNumber", ValueNode.class, true);
    contentHash = entity.getChild("contentHash", ValueNode.class, true);
    content = entity.getChild("content", ValueNode.class, true);
  }

  public UUID getId() {
    return id.getValue();
  }

  public String getFeedHash() {
    return feedHash.getValue();
  }

  public void setFeedHash(String feedHash) {
    this.feedHash.setValue(feedHash);
  }

  public Integer getFeedRecordNumber() {
    return feedRecordNumber.getValue();
  }

  public void setFeedRecordNumber(Integer feedRecordNumber) {
    this.feedRecordNumber.setValue(feedRecordNumber);
  }

  public String getContentHash() {
    return contentHash.getValue();
  }

  public void setContentHash(String contentHash) {
    this.contentHash.setValue(contentHash);
  }

  public String getContent() {
    return content.getValue();
  }

  public void setContent(String content) {
    this.content.setValue(content);
  }
}
