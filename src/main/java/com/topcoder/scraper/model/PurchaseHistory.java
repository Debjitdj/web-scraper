package com.topcoder.scraper.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;

/**
 * Purchase History model
 */
public class PurchaseHistory {

  /**
   * Represents user id (email / telephone)
   */
  @JsonIgnore
  private String userId;

  /**
   * Represents order number
   */
  @JsonProperty("order_no")
  private String orderNumber;

  /**
   * Represents order date
   */
  @JsonProperty("order_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private Date orderDate;

  /**
   * Represents order total amount
   */
  @JsonProperty("total_amount")
  private String totalAmount;

  /**
   * Represents list of ProductInfo
   */
  private List<ProductInfo> products;

  /**
   * Represents order delivery status
   * not implemented currently
   */
  @JsonProperty("delivery_status")
  private String deliveryStatus;

  public PurchaseHistory() {
  }

  public PurchaseHistory(String userId, String orderNumber, Date orderDate, String totalAmount, List<ProductInfo> products, String deliveryStatus) {
    this.userId = userId;
    this.orderNumber = orderNumber;
    this.orderDate = orderDate;
    this.totalAmount = totalAmount;
    this.products = products;
    this.deliveryStatus = deliveryStatus;
  }

  public String getUserId() {
    return userId;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public Date getOrderDate() {
    return orderDate;
  }

  public String getTotalAmount() {
    return totalAmount;
  }

  public List<ProductInfo> getProducts() {
    return products;
  }

  public String getDeliveryStatus() {
    return deliveryStatus;
  }
}

