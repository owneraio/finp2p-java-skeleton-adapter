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
import io.ownera.ledger.adapter.api.model.ApproveExecutionPlanRequestExecutionPlan;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * ApproveExecutionPlanRequest
 */
@JsonPropertyOrder({
  ApproveExecutionPlanRequest.JSON_PROPERTY_EXECUTION_PLAN
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class ApproveExecutionPlanRequest {
  public static final String JSON_PROPERTY_EXECUTION_PLAN = "executionPlan";
  private ApproveExecutionPlanRequestExecutionPlan executionPlan;

  public ApproveExecutionPlanRequest() { 
  }

  public ApproveExecutionPlanRequest executionPlan(ApproveExecutionPlanRequestExecutionPlan executionPlan) {
    this.executionPlan = executionPlan;
    return this;
  }

  /**
   * Get executionPlan
   * @return executionPlan
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_EXECUTION_PLAN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public ApproveExecutionPlanRequestExecutionPlan getExecutionPlan() {
    return executionPlan;
  }


  @JsonProperty(JSON_PROPERTY_EXECUTION_PLAN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setExecutionPlan(ApproveExecutionPlanRequestExecutionPlan executionPlan) {
    this.executionPlan = executionPlan;
  }


  /**
   * Return true if this ApproveExecutionPlanRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApproveExecutionPlanRequest approveExecutionPlanRequest = (ApproveExecutionPlanRequest) o;
    return Objects.equals(this.executionPlan, approveExecutionPlanRequest.executionPlan);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionPlan);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApproveExecutionPlanRequest {\n");
    sb.append("    executionPlan: ").append(toIndentedString(executionPlan)).append("\n");
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

    // add `executionPlan` to the URL query string
    if (getExecutionPlan() != null) {
      joiner.add(getExecutionPlan().toUrlQueryString(prefix + "executionPlan" + suffix));
    }

    return joiner.toString();
  }
}

