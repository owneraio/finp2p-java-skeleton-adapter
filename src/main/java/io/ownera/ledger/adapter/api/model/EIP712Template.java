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
import io.ownera.ledger.adapter.api.model.EIP712Domain;
import io.ownera.ledger.adapter.api.model.EIP712TypedValue;
import io.ownera.ledger.adapter.api.model.EIP712Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * EIP712Template
 */
@JsonPropertyOrder({
  EIP712Template.JSON_PROPERTY_TYPE,
  EIP712Template.JSON_PROPERTY_DOMAIN,
  EIP712Template.JSON_PROPERTY_MESSAGE,
  EIP712Template.JSON_PROPERTY_TYPES,
  EIP712Template.JSON_PROPERTY_PRIMARY_TYPE,
  EIP712Template.JSON_PROPERTY_HASH
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class EIP712Template {
  /**
   * Gets or Sets type
   */
  public enum TypeEnum {
    EIP712("EIP712");

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

  public static final String JSON_PROPERTY_DOMAIN = "domain";
  private EIP712Domain domain;

  public static final String JSON_PROPERTY_MESSAGE = "message";
  private Map<String, EIP712TypedValue> message = new HashMap<>();

  public static final String JSON_PROPERTY_TYPES = "types";
  private EIP712Types types;

  public static final String JSON_PROPERTY_PRIMARY_TYPE = "primaryType";
  private String primaryType;

  public static final String JSON_PROPERTY_HASH = "hash";
  private String hash;

  public EIP712Template() { 
  }

  public EIP712Template type(TypeEnum type) {
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


  public EIP712Template domain(EIP712Domain domain) {
    this.domain = domain;
    return this;
  }

  /**
   * Get domain
   * @return domain
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_DOMAIN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public EIP712Domain getDomain() {
    return domain;
  }


  @JsonProperty(JSON_PROPERTY_DOMAIN)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setDomain(EIP712Domain domain) {
    this.domain = domain;
  }


  public EIP712Template message(Map<String, EIP712TypedValue> message) {
    this.message = message;
    return this;
  }

  public EIP712Template putMessageItem(String key, EIP712TypedValue messageItem) {
    if (this.message == null) {
      this.message = new HashMap<>();
    }
    this.message.put(key, messageItem);
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_MESSAGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public Map<String, EIP712TypedValue> getMessage() {
    return message;
  }


  @JsonProperty(JSON_PROPERTY_MESSAGE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setMessage(Map<String, EIP712TypedValue> message) {
    this.message = message;
  }


  public EIP712Template types(EIP712Types types) {
    this.types = types;
    return this;
  }

  /**
   * Get types
   * @return types
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_TYPES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public EIP712Types getTypes() {
    return types;
  }


  @JsonProperty(JSON_PROPERTY_TYPES)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setTypes(EIP712Types types) {
    this.types = types;
  }


  public EIP712Template primaryType(String primaryType) {
    this.primaryType = primaryType;
    return this;
  }

  /**
   * Get primaryType
   * @return primaryType
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_PRIMARY_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public String getPrimaryType() {
    return primaryType;
  }


  @JsonProperty(JSON_PROPERTY_PRIMARY_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setPrimaryType(String primaryType) {
    this.primaryType = primaryType;
  }


  public EIP712Template hash(String hash) {
    this.hash = hash;
    return this;
  }

  /**
   * hex representation of template hash
   * @return hash
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_HASH)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public String getHash() {
    return hash;
  }


  @JsonProperty(JSON_PROPERTY_HASH)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setHash(String hash) {
    this.hash = hash;
  }


  /**
   * Return true if this EIP712Template object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EIP712Template eiP712Template = (EIP712Template) o;
    return Objects.equals(this.type, eiP712Template.type) &&
        Objects.equals(this.domain, eiP712Template.domain) &&
        Objects.equals(this.message, eiP712Template.message) &&
        Objects.equals(this.types, eiP712Template.types) &&
        Objects.equals(this.primaryType, eiP712Template.primaryType) &&
        Objects.equals(this.hash, eiP712Template.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, domain, message, types, primaryType, hash);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EIP712Template {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    domain: ").append(toIndentedString(domain)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
    sb.append("    primaryType: ").append(toIndentedString(primaryType)).append("\n");
    sb.append("    hash: ").append(toIndentedString(hash)).append("\n");
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

    // add `domain` to the URL query string
    if (getDomain() != null) {
      joiner.add(getDomain().toUrlQueryString(prefix + "domain" + suffix));
    }

    // add `message` to the URL query string
    if (getMessage() != null) {
      for (String _key : getMessage().keySet()) {
        if (getMessage().get(_key) != null) {
          joiner.add(getMessage().get(_key).toUrlQueryString(String.format("%smessage%s%s", prefix, suffix,
              "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, _key, containerSuffix))));
        }
      }
    }

    // add `types` to the URL query string
    if (getTypes() != null) {
      joiner.add(getTypes().toUrlQueryString(prefix + "types" + suffix));
    }

    // add `primaryType` to the URL query string
    if (getPrimaryType() != null) {
      joiner.add(String.format("%sprimaryType%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getPrimaryType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `hash` to the URL query string
    if (getHash() != null) {
      joiner.add(String.format("%shash%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getHash()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    return joiner.toString();
  }
}

