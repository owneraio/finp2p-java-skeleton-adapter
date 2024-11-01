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
import io.ownera.ledger.adapter.api.model.Destination;
import io.ownera.ledger.adapter.api.model.PayoutAsset;
import io.ownera.ledger.adapter.api.model.PayoutInstruction;
import io.ownera.ledger.adapter.api.model.Signature;
import io.ownera.ledger.adapter.api.model.Source;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * PayoutRequest
 */
@JsonPropertyOrder({
  PayoutRequest.JSON_PROPERTY_SOURCE,
  PayoutRequest.JSON_PROPERTY_DESTINATION,
  PayoutRequest.JSON_PROPERTY_QUANTITY,
  PayoutRequest.JSON_PROPERTY_PAYOUT_INSTRUCTION,
  PayoutRequest.JSON_PROPERTY_ASSET,
  PayoutRequest.JSON_PROPERTY_NONCE,
  PayoutRequest.JSON_PROPERTY_SIGNATURE
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class PayoutRequest {
  public static final String JSON_PROPERTY_SOURCE = "source";
  private Source source;

  public static final String JSON_PROPERTY_DESTINATION = "destination";
  private Destination destination;

  public static final String JSON_PROPERTY_QUANTITY = "quantity";
  private String quantity;

  public static final String JSON_PROPERTY_PAYOUT_INSTRUCTION = "payoutInstruction";
  private PayoutInstruction payoutInstruction;

  public static final String JSON_PROPERTY_ASSET = "asset";
  private PayoutAsset asset;

  public static final String JSON_PROPERTY_NONCE = "nonce";
  private String nonce;

  public static final String JSON_PROPERTY_SIGNATURE = "signature";
  private Signature signature;

  public PayoutRequest() { 
  }

  public PayoutRequest source(Source source) {
    this.source = source;
    return this;
  }

  /**
   * Get source
   * @return source
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_SOURCE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public Source getSource() {
    return source;
  }


  @JsonProperty(JSON_PROPERTY_SOURCE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setSource(Source source) {
    this.source = source;
  }


  public PayoutRequest destination(Destination destination) {
    this.destination = destination;
    return this;
  }

  /**
   * Get destination
   * @return destination
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_DESTINATION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public Destination getDestination() {
    return destination;
  }


  @JsonProperty(JSON_PROPERTY_DESTINATION)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setDestination(Destination destination) {
    this.destination = destination;
  }


  public PayoutRequest quantity(String quantity) {
    this.quantity = quantity;
    return this;
  }

  /**
   * How many units of the asset
   * @return quantity
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_QUANTITY)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public String getQuantity() {
    return quantity;
  }


  @JsonProperty(JSON_PROPERTY_QUANTITY)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setQuantity(String quantity) {
    this.quantity = quantity;
  }


  public PayoutRequest payoutInstruction(PayoutInstruction payoutInstruction) {
    this.payoutInstruction = payoutInstruction;
    return this;
  }

  /**
   * Get payoutInstruction
   * @return payoutInstruction
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PAYOUT_INSTRUCTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public PayoutInstruction getPayoutInstruction() {
    return payoutInstruction;
  }


  @JsonProperty(JSON_PROPERTY_PAYOUT_INSTRUCTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPayoutInstruction(PayoutInstruction payoutInstruction) {
    this.payoutInstruction = payoutInstruction;
  }


  public PayoutRequest asset(PayoutAsset asset) {
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
  public PayoutAsset getAsset() {
    return asset;
  }


  @JsonProperty(JSON_PROPERTY_ASSET)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setAsset(PayoutAsset asset) {
    this.asset = asset;
  }


  public PayoutRequest nonce(String nonce) {
    this.nonce = nonce;
    return this;
  }

  /**
   * 32 bytes buffer (24 randomly generated bytes by the client + 8 bytes epoch timestamp seconds) encoded to hex:    const nonce &#x3D; Buffer.alloc(32);   nonce.fill(crypto.randomBytes(24), 0, 24);    const nowEpochSeconds &#x3D; Math.floor(new Date().getTime() / 1000);   const t &#x3D; BigInt(nowEpochSeconds);   nonce.writeBigInt64BE(t, 24); 
   * @return nonce
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NONCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getNonce() {
    return nonce;
  }


  @JsonProperty(JSON_PROPERTY_NONCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNonce(String nonce) {
    this.nonce = nonce;
  }


  public PayoutRequest signature(Signature signature) {
    this.signature = signature;
    return this;
  }

  /**
   * Get signature
   * @return signature
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SIGNATURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Signature getSignature() {
    return signature;
  }


  @JsonProperty(JSON_PROPERTY_SIGNATURE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSignature(Signature signature) {
    this.signature = signature;
  }


  /**
   * Return true if this PayoutRequest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PayoutRequest payoutRequest = (PayoutRequest) o;
    return Objects.equals(this.source, payoutRequest.source) &&
        Objects.equals(this.destination, payoutRequest.destination) &&
        Objects.equals(this.quantity, payoutRequest.quantity) &&
        Objects.equals(this.payoutInstruction, payoutRequest.payoutInstruction) &&
        Objects.equals(this.asset, payoutRequest.asset) &&
        Objects.equals(this.nonce, payoutRequest.nonce) &&
        Objects.equals(this.signature, payoutRequest.signature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination, quantity, payoutInstruction, asset, nonce, signature);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PayoutRequest {\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    destination: ").append(toIndentedString(destination)).append("\n");
    sb.append("    quantity: ").append(toIndentedString(quantity)).append("\n");
    sb.append("    payoutInstruction: ").append(toIndentedString(payoutInstruction)).append("\n");
    sb.append("    asset: ").append(toIndentedString(asset)).append("\n");
    sb.append("    nonce: ").append(toIndentedString(nonce)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
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

    // add `source` to the URL query string
    if (getSource() != null) {
      joiner.add(getSource().toUrlQueryString(prefix + "source" + suffix));
    }

    // add `destination` to the URL query string
    if (getDestination() != null) {
      joiner.add(getDestination().toUrlQueryString(prefix + "destination" + suffix));
    }

    // add `quantity` to the URL query string
    if (getQuantity() != null) {
      joiner.add(String.format("%squantity%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getQuantity()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `payoutInstruction` to the URL query string
    if (getPayoutInstruction() != null) {
      joiner.add(getPayoutInstruction().toUrlQueryString(prefix + "payoutInstruction" + suffix));
    }

    // add `asset` to the URL query string
    if (getAsset() != null) {
      joiner.add(getAsset().toUrlQueryString(prefix + "asset" + suffix));
    }

    // add `nonce` to the URL query string
    if (getNonce() != null) {
      joiner.add(String.format("%snonce%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getNonce()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `signature` to the URL query string
    if (getSignature() != null) {
      joiner.add(getSignature().toUrlQueryString(prefix + "signature" + suffix));
    }

    return joiner.toString();
  }
}

