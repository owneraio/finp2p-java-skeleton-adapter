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
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * Finp2pAsset
 */
@JsonPropertyOrder({
  Finp2pAsset.JSON_PROPERTY_TYPE,
  Finp2pAsset.JSON_PROPERTY_RESOURCE_ID
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class Finp2pAsset {
  /**
   * Gets or Sets type
   */
  public enum TypeEnum {
    FINP2P("finp2p");

    private String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_TYPE = "type";
  private TypeEnum type;

  public static final String JSON_PROPERTY_RESOURCE_ID = "resourceId";
  private String resourceId;

  public Finp2pAsset() { 
  }

  public Finp2pAsset type(TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public TypeEnum getType() {
    return type;
  }


  @JsonProperty(JSON_PROPERTY_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setType(TypeEnum type) {
    this.type = type;
  }


  public Finp2pAsset resourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  /**
   * Unique resource ID of the FinP2P asset [format](&#39;https://finp2p.atlassian.net/wiki/spaces/FINP2P/pages/67764240/FinP2P+Network+Interface+Specification#ResourceID-format&#39;) 
   * @return resourceId
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_RESOURCE_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public String getResourceId() {
    return resourceId;
  }


  @JsonProperty(JSON_PROPERTY_RESOURCE_ID)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }


  /**
   * Return true if this finp2pAsset object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Finp2pAsset finp2pAsset = (Finp2pAsset) o;
    return Objects.equals(this.type, finp2pAsset.type) &&
        Objects.equals(this.resourceId, finp2pAsset.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, resourceId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Finp2pAsset {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    resourceId: ").append(toIndentedString(resourceId)).append("\n");
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

    // add `type` to the URL query string
    if (getType() != null) {
      joiner.add(String.format("%stype%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `resourceId` to the URL query string
    if (getResourceId() != null) {
      joiner.add(String.format("%sresourceId%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getResourceId()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

