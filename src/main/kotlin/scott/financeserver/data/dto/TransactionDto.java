package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class TransactionDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private AccountDto account;
  private FeedDto feed;
  private String content;
  private String contentHash;
  private Date date;
  private CategoryDto category;
  private Boolean userCategorized;
  private BigDecimal amount;
  private String comment;
  private Boolean important;

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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Boolean getImportant() {
    return important;
  }

  public void setImportant(Boolean important) {
    this.important = important;
  }
  public String toString() {
    return getClass().getSimpleName() + "[id = " + getId() + "]";
  }
}
