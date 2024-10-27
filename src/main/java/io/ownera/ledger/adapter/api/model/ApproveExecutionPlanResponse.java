/*
 * Ledger Adapter Specification
 * This is the API specification for the Ledger Adapter with whom the FinP2P node will interact in order to execute and query the underlying implementation.
 *
 * The version of the OpenAPI document: x.x.x
 * Contact: support@ownera.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.ownera.ledger.adapter.api.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.ownera.ledger.adapter.api.model.ExecutionPlanApprovalOperationAllOfApproval;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * ApproveExecutionPlanResponse
 */
@JsonPropertyOrder({
  ApproveExecutionPlanResponse.JSON_PROPERTY_CID,
  ApproveExecutionPlanResponse.JSON_PROPERTY_IS_COMPLETED,
  ApproveExecutionPlanResponse.JSON_PROPERTY_APPROVAL
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class ApproveExecutionPlanResponse {
  public static final String JSON_PROPERTY_CID = "cid";
  private String cid;

  public static final String JSON_PROPERTY_IS_COMPLETED = "isCompleted";
  private Boolean isCompleted;

  public static final String JSON_PROPERTY_APPROVAL = "approval";
  private ExecutionPlanApprovalOperationAllOfApproval approval;

  public ApproveExecutionPlanResponse() { 
  }

  public ApproveExecutionPlanResponse cid(String cid) {
    this.cid = cid;
    return this;
  }

  /**
   * unique correlation id which identify the operation
   * @return cid
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_CID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public String getCid() {
    return cid;
  }


  @JsonProperty(JSON_PROPERTY_CID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setCid(String cid) {
    this.cid = cid;
  }


  public ApproveExecutionPlanResponse isCompleted(Boolean isCompleted) {
    this.isCompleted = isCompleted;
    return this;
  }

  /**
   * flag indicating if the operation completed, if true then error or response must be present (but not both)
   * @return isCompleted
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_IS_COMPLETED)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public Boolean getIsCompleted() {
    return isCompleted;
  }


  @JsonProperty(JSON_PROPERTY_IS_COMPLETED)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setIsCompleted(Boolean isCompleted) {
    this.isCompleted = isCompleted;
  }


  public ApproveExecutionPlanResponse approval(ExecutionPlanApprovalOperationAllOfApproval approval) {
    this.approval = approval;
    return this;
  }

  /**
   * Get approval
   * @return approval
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_APPROVAL)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public ExecutionPlanApprovalOperationAllOfApproval getApproval() {
    return approval;
  }


  @JsonProperty(JSON_PROPERTY_APPROVAL)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setApproval(ExecutionPlanApprovalOperationAllOfApproval approval) {
    this.approval = approval;
  }


  /**
   * Return true if this ApproveExecutionPlanResponse object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApproveExecutionPlanResponse approveExecutionPlanResponse = (ApproveExecutionPlanResponse) o;
    return Objects.equals(this.cid, approveExecutionPlanResponse.cid) &&
        Objects.equals(this.isCompleted, approveExecutionPlanResponse.isCompleted) &&
        Objects.equals(this.approval, approveExecutionPlanResponse.approval);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cid, isCompleted, approval);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApproveExecutionPlanResponse {\n");
    sb.append("    cid: ").append(toIndentedString(cid)).append("\n");
    sb.append("    isCompleted: ").append(toIndentedString(isCompleted)).append("\n");
    sb.append("    approval: ").append(toIndentedString(approval)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `cid` to the URL query string
    if (getCid() != null) {
      joiner.add(String.format("%scid%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getCid()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `isCompleted` to the URL query string
    if (getIsCompleted() != null) {
      joiner.add(String.format("%sisCompleted%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getIsCompleted()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `approval` to the URL query string
    if (getApproval() != null) {
      joiner.add(getApproval().toUrlQueryString(prefix + "approval" + suffix));
    }

    return joiner.toString();
  }
}

