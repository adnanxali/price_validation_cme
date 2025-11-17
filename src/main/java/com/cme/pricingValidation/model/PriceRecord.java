package com.cme.pricingValidation.model;

public class PriceRecord {
    private String instrumentGuid;
    private String tradeDate;
    private String price;
    private String exchange;
    private String productType;

    PriceRecord(){}
    public PriceRecord(String instrumentGuid,String tradeDate, String price, String exchange, String productType){
        this.instrumentGuid = instrumentGuid;
        this.tradeDate = tradeDate;
        this.price = price;
        this.exchange = exchange;
        this.productType = productType;
    }
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


}
