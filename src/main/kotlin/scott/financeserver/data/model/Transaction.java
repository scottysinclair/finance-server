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
 * @author exssinclair
 */
public class Transaction extends AbstractCustomEntityProxy {
  private static final long serialVersionUID = 1L;

  private final ValueNode id;
  private final RefNodeProxyHelper account;
  private final RefNodeProxyHelper feed;
  private final ValueNode feedRecordNumber;
  private final ValueNode content;
  private final ValueNode contentHash;
  private final ValueNode description;
  private final ValueNode date;
  private final RefNodeProxyHelper category;
  private final ValueNode userCategorized;
  private final ValueNode amount;
  private final ValueNode duplicate;

  public Transaction(Entity entity) {
    super(entity);
    id = entity.getChild("id", ValueNode.class, true);
    account = new RefNodeProxyHelper(entity.getChild("account", RefNode.class, true));
    feed = new RefNodeProxyHelper(entity.getChild("feed", RefNode.class, true));
    feedRecordNumber = entity.getChild("feedRecordNumber", ValueNode.class, true);
    content = entity.getChild("content", ValueNode.class, true);
    contentHash = entity.getChild("contentHash", ValueNode.class, true);
    description = entity.getChild("description", ValueNode.class, true);
    date = entity.getChild("date", ValueNode.class, true);
    category = new RefNodeProxyHelper(entity.getChild("category", RefNode.class, true));
    userCategorized = entity.getChild("userCategorized", ValueNode.class, true);
    amount = entity.getChild("amount", ValueNode.class, true);
    duplicate = entity.getChild("duplicate", ValueNode.class, true);
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

  public Feed getFeed() {
    return super.getFromRefNode(feed.refNode);
  }

  public void setFeed(Feed feed) {
    setToRefNode(this.feed.refNode, feed);
  }

  public Integer getFeedRecordNumber() {
    return feedRecordNumber.getValue();
  }

  public void setFeedRecordNumber(Integer feedRecordNumber) {
    this.feedRecordNumber.setValue(feedRecordNumber);
  }

  public String getContent() {
    return content.getValue();
  }

  public void setContent(String content) {
    this.content.setValue(content);
  }

  public String getContentHash() {
    return contentHash.getValue();
  }

  public void setContentHash(String contentHash) {
    this.contentHash.setValue(contentHash);
  }

  public String getDescription() {
    return description.getValue();
  }

  public void setDescription(String description) {
    this.description.setValue(description);
  }

  public Date getDate() {
    return date.getValue();
  }

  public void setDate(Date date) {
    this.date.setValue(date);
  }

  public Category getCategory() {
    return super.getFromRefNode(category.refNode);
  }

  public void setCategory(Category category) {
    setToRefNode(this.category.refNode, category);
  }

  public Boolean getUserCategorized() {
    return userCategorized.getValue();
  }

  public void setUserCategorized(Boolean userCategorized) {
    this.userCategorized.setValue(userCategorized);
  }

  public BigDecimal getAmount() {
    return amount.getValue();
  }

  public void setAmount(BigDecimal amount) {
    this.amount.setValue(amount);
  }

  public Boolean getDuplicate() {
    return duplicate.getValue();
  }

  public void setDuplicate(Boolean duplicate) {
    this.duplicate.setValue(duplicate);
  }
}
