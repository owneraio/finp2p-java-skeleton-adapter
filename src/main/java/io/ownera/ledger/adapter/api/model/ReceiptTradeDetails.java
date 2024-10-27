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
import io.ownera.ledger.adapter.api.model.ReceiptExecutionContext;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * ReceiptTradeDetails
 */
@JsonPropertyOrder({
  ReceiptTradeDetails.JSON_PROPERTY_INTENT_ID,
  ReceiptTradeDetails.JSON_PROPERTY_INTENT_VERSION,
  ReceiptTradeDetails.JSON_PROPERTY_EXECUTION_CONTEXT
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class ReceiptTradeDetails {
  public static final String JSON_PROPERTY_INTENT_ID = "intentId";
  private String intentId;

  public static final String JSON_PROPERTY_INTENT_VERSION = "intentVersion";
  private String intentVersion;

  public static final String JSON_PROPERTY_EXECUTION_CONTEXT = "executionContext";
  private ReceiptExecutionContext executionContext;

  public ReceiptTradeDetails() { 
  }

  public ReceiptTradeDetails intentId(String intentId) {
    this.intentId = intentId;
    return this;
  }

  /**
   * Get intentId
   * @return intentId
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INTENT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIntentId() {
    return intentId;
  }


  @JsonProperty(JSON_PROPERTY_INTENT_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIntentId(String intentId) {
    this.intentId = intentId;
  }


  public ReceiptTradeDetails intentVersion(String intentVersion) {
    this.intentVersion = intentVersion;
    return this;
  }

  /**
   * Get intentVersion
   * @return intentVersion
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INTENT_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIntentVersion() {
    return intentVersion;
  }


  @JsonProperty(JSON_PROPERTY_INTENT_VERSION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setIntentVersion(String intentVersion) {
    this.intentVersion = intentVersion;
  }


  public ReceiptTradeDetails executionContext(ReceiptExecutionContext executionContext) {
    this.executionContext = executionContext;
    return this;
  }

  /**
   * Get executionContext
   * @return executionContext
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXECUTION_CONTEXT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public ReceiptExecutionContext getExecutionContext() {
    return executionContext;
  }


  @JsonProperty(JSON_PROPERTY_EXECUTION_CONTEXT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExecutionContext(ReceiptExecutionContext executionContext) {
    this.executionContext = executionContext;
  }


  /**
   * Return true if this receiptTradeDetails object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReceiptTradeDetails receiptTradeDetails = (ReceiptTradeDetails) o;
    return Objects.equals(this.intentId, receiptTradeDetails.intentId) &&
        Objects.equals(this.intentVersion, receiptTradeDetails.intentVersion) &&
        Objects.equals(this.executionContext, receiptTradeDetails.executionContext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(intentId, intentVersion, executionContext);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReceiptTradeDetails {\n");
    sb.append("    intentId: ").append(toIndentedString(intentId)).append("\n");
    sb.append("    intentVersion: ").append(toIndentedString(intentVersion)).append("\n");
    sb.append("    executionContext: ").append(toIndentedString(executionContext)).append("\n");
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

    // add `intentId` to the URL query string
    if (getIntentId() != null) {
      joiner.add(String.format("%sintentId%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getIntentId()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `intentVersion` to the URL query string
    if (getIntentVersion() != null) {
      joiner.add(String.format("%sintentVersion%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getIntentVersion()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `executionContext` to the URL query string
    if (getExecutionContext() != null) {
      joiner.add(getExecutionContext().toUrlQueryString(prefix + "executionContext" + suffix));
    }

    return joiner.toString();
  }
}
