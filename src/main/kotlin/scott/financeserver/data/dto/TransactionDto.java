package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class TransactionDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private AccountDto account;
  private FeedDto feed;
  private Integer feedRecordNumber;
  private String content;
  private String contentHash;
  private String description;
  private Date date;
  private CategoryDto category;
  private Boolean userCategorized;
  private BigDecimal amount;
  private Boolean duplicate;

  public TransactionDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public AccountDto getAccount() {
    return account;
  }

  public void setAccount(AccountDto account) {
    this.account = account;
  }

  public FeedDto getFeed() {
    return feed;
  }

  public void setFeed(FeedDto feed) {
    this.feed = feed;
  }

  public Integer getFeedRecordNumber() {
    return feedRecordNumber;
  }

  public void setFeedRecordNumber(Integer feedRecordNumber) {
    this.feedRecordNumber = feedRecordNumber;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(String contentHash) {
    this.contentHash = contentHash;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public CategoryDto getCategory() {
    return category;
  }

  public void setCategory(CategoryDto category) {
    this.category = category;
  }

  public Boolean getUserCategorized() {
    return userCategorized;
  }

  public void setUserCategorized(Boolean userCategorized) {
    this.userCategorized = userCategorized;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Boolean getDuplicate() {
    return duplicate;
  }

  public void setDuplicate(Boolean duplicate) {
    this.duplicate = duplicate;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
