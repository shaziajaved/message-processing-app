package com.jpm.product;

public class Product {
    private String type; //initially I thought of fields: name, type, status, category. It seemed like overengineering so I am keeping fields as were asked, mostly that is.
    private long inStock; //represents amount available in stock at any given time
    private long soldOut; //represents total sold out units at any given time (as is updated by incoming messages)
    private Double unitPrice; //could benefit from BigDecimal here

    public Product() {
    }

    public Product(String type, long inStock, long soldOut, Double unitPrice) {
        this.type = type;
        this.inStock = inStock;
        this.soldOut = soldOut;
        this.unitPrice = unitPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getInStock() {
        return inStock;
    }

    public void setInStock(long inStock) {
        this.inStock = inStock;
    }

    public long getSoldOut() {
        return soldOut;
    }

    public void setSoldOut(long soldOut) {
        this.soldOut = soldOut;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }
}
