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
import io.ownera.ledger.adapter.api.model.Asset;
import io.ownera.ledger.adapter.api.model.LedgerAssetBinding;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * CreateAssetRequest
 */
@JsonPropertyOrder({
  CreateAssetRequest.JSON_PROPERTY_ASSET,
  CreateAssetRequest.JSON_PROPERTY_LEDGER_ASSET_BINDING
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class CreateAssetRequest {
  public static final String JSON_PROPERTY_ASSET = "asset";
  private Asset asset;

  public static final String JSON_PROPERTY_LEDGER_ASSET_BINDING = "ledgerAssetBinding";
  private LedgerAssetBinding ledgerAssetBinding;

  public CreateAssetRequest() { 
  }

  public CreateAssetRequest asset(Asset asset) {
    this.asset = asset;
    return this;
  }

  /**
   * Get asset
   * @return asset
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ASSET)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public Asset getAsset() {
    return asset;
  }


  @JsonProperty(JSON_PROPERTY_ASSET)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAsset(Asset asset) {
    this.asset = asset;
  }


  public CreateAssetRequest ledgerAssetBinding(LedgerAssetBinding ledgerAssetBinding) {
    this.ledgerAssetBinding = ledgerAssetBinding;
    return this;
  }

  /**
   * Get ledgerAssetBinding
   * @return ledgerAssetBinding
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LEDGER_ASSET_BINDING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public LedgerAssetBinding getLedgerAssetBinding() {
    return ledgerAssetBinding;
  }


  @JsonProperty(JSON_PROPERTY_LEDGER_ASSET_BINDING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLedgerAssetBinding(LedgerAssetBinding ledgerAssetBinding) {
    this.ledgerAssetBinding = ledgerAssetBinding;
  }


  /**
   * Return true if this CreateAssetRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateAssetRequest createAssetRequest = (CreateAssetRequest) o;
    return Objects.equals(this.asset, createAssetRequest.asset) &&
        Objects.equals(this.ledgerAssetBinding, createAssetRequest.ledgerAssetBinding);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asset, ledgerAssetBinding);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateAssetRequest {\n");
    sb.append("    asset: ").append(toIndentedString(asset)).append("\n");
    sb.append("    ledgerAssetBinding: ").append(toIndentedString(ledgerAssetBinding)).append("\n");
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

    // add `asset` to the URL query string
    if (getAsset() != null) {
      joiner.add(getAsset().toUrlQueryString(prefix + "asset" + suffix));
    }

    // add `ledgerAssetBinding` to the URL query string
    if (getLedgerAssetBinding() != null) {
      joiner.add(getLedgerAssetBinding().toUrlQueryString(prefix + "ledgerAssetBinding" + suffix));
    }

    return joiner.toString();
  }
}

