package com.cme.pricingValidation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "price-records")
public class PriceRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String instrumentGuid;
    private String tradeDate;
    private String price;
    private String exchange;
    private String productType;
    private Integer rowNumber;

    private boolean valid;

    @Column(length = 500)
    private String errors;

    public PriceRecordEntity(){}
    public PriceRecordEntity(String instrumentGuid, String tradeDate, String price,
                             String exchange, String productType,Integer rowNumber, boolean valid, String errors) {
        this.instrumentGuid = instrumentGuid;
        this.tradeDate = tradeDate;
        this.price = price;
        this.exchange = exchange;
        this.productType = productType;
        this.rowNumber = rowNumber;
        this.valid = valid;
        this.errors = errors;
    }
    public Long getId() { return id; }

    public String getInstrumentGuid() { return instrumentGuid; }
    public void setInstrumentGuid(String instrumentGuid) { this.instrumentGuid = instrumentGuid; }

    public String getTradeDate() { return tradeDate; }
    public void setTradeDate(String tradeDate) { this.tradeDate = tradeDate; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public Integer getRowNumber(){return rowNumber;}
    public void setRowNumber(Integer rowNumber){this.rowNumber = rowNumber;}

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getErrors() { return errors; }
    public void setErrors(String errors) { this.errors = errors; }

}
