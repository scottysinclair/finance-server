package scott.financeserver.data.dto;

import scott.barleydb.api.dto.BaseDto;

import java.util.UUID;

/**
 * Generated from Entity Specification
 *
 * @author exssinclair
 */
public class DuplicatesDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private UUID id;
  private String feedHash;
  private Integer feedRecordNumber;
  private String contentHash;
  private String content;
  private Boolean duplicate;

  public DuplicatesDto() {
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getFeedHash() {
    return feedHash;
  }

  public void setFeedHash(String feedHash) {
    this.feedHash = feedHash;
  }

  public Integer getFeedRecordNumber() {
    return feedRecordNumber;
  }

  public void setFeedRecordNumber(Integer feedRecordNumber) {
    this.feedRecordNumber = feedRecordNumber;
  }

  public String getContentHash() {
    return contentHash;
  }

  public void setContentHash(String contentHash) {
    this.contentHash = contentHash;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
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
