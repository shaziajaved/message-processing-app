package com.jpm.sale;

import com.jpm.core.OperationType;
import com.jpm.message.AdjustmentMessage;
import com.jpm.message.DetailedMessage;
import com.jpm.message.Message;
import com.jpm.product.Product;

import java.util.*;

public class Register {
    Map<Product, List<Transaction>> records;

    public Register() {
        this.records = new HashMap<>();
    }

    public Register(Map<Product, List<Transaction>> records) {
        this.records = records;
    }

    public Map<Product, List<Transaction>> getRecords() {
        return records;
    }

    public boolean addProduct(Product product) {
        if(product == null || records.containsKey(product)) {
            return false; //can be revised to updating quantity instead, if product != null and functionality needed
        }

        records.put(product, new ArrayList<>());
        return true;
    }

    public boolean updateRecords(Message message) {
        if(message == null) {
            System.out.println("Invalid sales record. Please review incoming data. Inform developers.");
            return false;
        }

        Product product = findProduct(message.getType());
        if(product == null) {
            System.out.println("Invalid sales record. Sold product was not in stock to begin with.");
            return false;
        }

        List<Transaction> transactions = records.get(product);

        if(message instanceof AdjustmentMessage) {
            transactions = adjustTransactions(transactions, message);
        } else if(message instanceof DetailedMessage) {
            transactions = addNewTransactions(transactions, message);
        } else {
            examineNotification(message);
            transactions.add(new Transaction(message.getSellingPrice()));
        }

        if(transactions.size() == 0) {
            System.out.println("\nThere are no product (" + message.getType() +
                    ") related sales to adjust. Doing nothing.");
            return false; //in event of first unsuccessful adjustment (owing to missing sales) stopping prematurely (for now)
        }

        records.put(product, transactions);
        return true;
    }

    private Product findProduct(String productType) {
        Set<Product> products = records.keySet();

        for(Product product : products) {
            if(productType.equals(product.getType())) {
                return product;
            }
        }

        return null;
    }

    private List<Transaction> adjustTransactions(List<Transaction> transactions, Message message) {
        OperationType operationType = ((AdjustmentMessage) message).getOperationType();

        switch(operationType) {
            case ADD:
                for(Transaction transaction : transactions) {
                    transaction.setValue(transaction.getValue() + message.getSellingPrice());
                    transaction.setTransactionStatus(TransactionStatus.ADJUSTED);
                }
                break;
            case MULTIPLY:
                for(Transaction transaction : transactions) {
                    transaction.setValue(transaction.getValue() * message.getSellingPrice());
                    transaction.setTransactionStatus(TransactionStatus.ADJUSTED);
                }
                break;
            case SUBTRACT:
                for(Transaction transaction : transactions) {
                    if(transaction.getValue() < message.getSellingPrice()) {
                       System.out.println("Potential loss detected @ [Product type: " +
                               message.getType() + ", Existing value: " + transaction.getValue() +
                               ", Selling price: " + message.getSellingPrice() +
                               ", Adjustment operation: SUBTRACT] during processing.");
                    }

                    transaction.setValue(transaction.getValue() - message.getSellingPrice());
                    transaction.setTransactionStatus(TransactionStatus.ADJUSTED);
                }
                break;
            default:
                System.out.println("Unsupported operation encountered during processing.");
                break;
        }

        return transactions;
    }

    private List<Transaction> addNewTransactions(List<Transaction> transactions, Message message) {
        double price = message.getSellingPrice();
        long transactionsCount = ((DetailedMessage) message).getInstanceCount();

        examineNotification(message);

        if(transactionsCount <= 0) {
            System.out.println("Null or negative sales instance(s) found. Doing nothing.");
            return transactions;
        }

        for(long i=0; i<transactionsCount; i++) {
            transactions.add(new Transaction(price));
        }

        return transactions;
    }

    private void examineNotification(Message message) {
        if(message.getSellingPrice() <= 0) {
            System.out.println("\nLogging (on console) the free distribution of goods and money @ [Product type: " +
                    message.getType() + ", Selling price: " + message.getSellingPrice() +
                    "]. Bad sales notification(s), at least for the business.");
        }
    }

    public void printSalesReport() {
        for(Map.Entry<Product, List<Transaction>> record : records.entrySet()) {
            System.out.println("Product type: " + record.getKey().getType() +
                    ", Total units sold: " + record.getValue().size() +
                    ", Revenue generated: " + getRevenueForProduct(record.getValue())
            );
        }
    }

    private double getRevenueForProduct(List<Transaction> transactions) {
        double revenueGenerated = 0;

        for(Transaction transaction : transactions) {
            revenueGenerated += transaction.getValue();
        }

        return revenueGenerated;
    }
}
