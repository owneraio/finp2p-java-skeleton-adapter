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
import io.ownera.ledger.adapter.api.model.RegulationError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import io.ownera.ledger.adapter.api.ApiClient;
/**
 * RegulationFailure
 */
@JsonPropertyOrder({
  RegulationFailure.JSON_PROPERTY_FAILURE_TYPE,
  RegulationFailure.JSON_PROPERTY_ERRORS
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2024-10-23T11:29:49.092442+03:00[Asia/Jerusalem]", comments = "Generator version: 7.9.0")
public class RegulationFailure {
  /**
   * Gets or Sets failureType
   */
  public enum FailureTypeEnum {
    REGULATION_FAILURE("RegulationFailure");

    private String value;

    FailureTypeEnum(String value) {
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
    public static FailureTypeEnum fromValue(String value) {
      for (FailureTypeEnum b : FailureTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  public static final String JSON_PROPERTY_FAILURE_TYPE = "failureType";
  private FailureTypeEnum failureType;

  public static final String JSON_PROPERTY_ERRORS = "errors";
  private List<RegulationError> errors = new ArrayList<>();

  public RegulationFailure() { 
  }

  public RegulationFailure failureType(FailureTypeEnum failureType) {
    this.failureType = failureType;
    return this;
  }

  /**
   * Get failureType
   * @return failureType
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_FAILURE_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public FailureTypeEnum getFailureType() {
    return failureType;
  }


  @JsonProperty(JSON_PROPERTY_FAILURE_TYPE)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setFailureType(FailureTypeEnum failureType) {
    this.failureType = failureType;
  }


  public RegulationFailure errors(List<RegulationError> errors) {
    this.errors = errors;
    return this;
  }

  public RegulationFailure addErrorsItem(RegulationError errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * Get errors
   * @return errors
   */
  @javax.annotation.Nonnull
  @JsonProperty(JSON_PROPERTY_ERRORS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public List<RegulationError> getErrors() {
    return errors;
  }


  @JsonProperty(JSON_PROPERTY_ERRORS)
  @JsonInclude(value = JsonInclude.Include.ALWAYS)
  public void setErrors(List<RegulationError> errors) {
    this.errors = errors;
  }


  /**
   * Return true if this RegulationFailure object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegulationFailure regulationFailure = (RegulationFailure) o;
    return Objects.equals(this.failureType, regulationFailure.failureType) &&
        Objects.equals(this.errors, regulationFailure.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(failureType, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegulationFailure {\n");
    sb.append("    failureType: ").append(toIndentedString(failureType)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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

    // add `failureType` to the URL query string
    if (getFailureType() != null) {
      joiner.add(String.format("%sfailureType%s=%s", prefix, suffix, URLEncoder.encode(ApiClient.valueToString(getFailureType()), StandardCharsets.UTF_8).replaceAll("\\+", "%20")));
    }

    // add `errors` to the URL query string
    if (getErrors() != null) {
      for (int i = 0; i < getErrors().size(); i++) {
        if (getErrors().get(i) != null) {
          joiner.add(getErrors().get(i).toUrlQueryString(String.format("%serrors%s%s", prefix, suffix,
          "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix))));
        }
      }
    }

    return joiner.toString();
  }
}
