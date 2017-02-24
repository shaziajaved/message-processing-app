package com.jpm.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.jpm.message.AdjustmentMessage;
import com.jpm.message.Message;
import com.jpm.product.Product;
import com.jpm.sale.Register;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class SalesEngine {
    private static SalesEngine salesEngine = new SalesEngine();
    private Register register; //refactor later for auditing and stuff as application grows

    private static final long MESSAGE_PROCESSING_CAPACITY = 50;

    private SalesEngine() {
        this.register = new Register();
    }

    public static SalesEngine getSalesEngine() {
        return salesEngine;
    }

    public boolean initialize(String stockFile) {
        BufferedReader stockBuffer = null;

        try {
            String stockEntry;
            stockBuffer = new BufferedReader(new FileReader(stockFile));

            while((stockEntry = stockBuffer.readLine()) != null) {
                boolean productAdded = register.addProduct(parseStockEntry(stockEntry));

                if(!productAdded) {
                    System.out.println("Stock update failed. Please confirm stock data.");
                }
            }
        } catch(IOException exception) {
            exception.printStackTrace();
        } finally {
            if(stockBuffer != null) {
                try {
                    stockBuffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    private Product parseStockEntry(String stockEntry) {
        if(stockEntry == null) {
            return null;
        }

        String[] productData = stockEntry.split("\\s*,\\s*");

        if(productData.length != 4) {
            System.out.println("Too much or too less product data (entry). Please confirm stock data.");
            return null;
        }

        Product product = null;

        try {
            product = new Product(productData[0], //product type, string value
                    Long.valueOf(productData[1]), //in stock units, long value
                    Long.valueOf(productData[2]), //sold out units, long value
                    Double.valueOf(productData[3])); //unit price, double value
        } catch (NumberFormatException exception) {
            System.out.println("Product count and/or pricing is incorrect. Please revise product data (entry).");
        }

        return product;
    }

    public List<Message> parse(String notificationsFile) {
        List<Message> messages = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            messages = mapper.readValue(new File(notificationsFile), new TypeReference<List<Message>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        return messages;
    }

    public boolean process(List<Message> messages) {
        int processedMessages = 0;
        StringBuilder adjustmentsLog = new StringBuilder();

        for(Message message : messages) {
            boolean recordsUpdated = register.updateRecords(message);
            if(!recordsUpdated) {
                return false;
            }

            processedMessages++;

            if(message instanceof AdjustmentMessage) {
                adjustmentsLog.append("Product (");
                adjustmentsLog.append(message.getType());
                adjustmentsLog.append(") was adjusted (operation: ");
                adjustmentsLog.append(((AdjustmentMessage) message).getOperationType());
                adjustmentsLog.append(") by a value of ");
                adjustmentsLog.append(message.getSellingPrice());
                adjustmentsLog.append(" at approximately ");
                adjustmentsLog.append(new Date());
                adjustmentsLog.append(".\n");
            }

            if(processedMessages % 10 == 0) {
                System.out.println("\n*** Intermediate Processed Sales' Record ***");
                register.printSalesReport();
            }

            if(processedMessages == MESSAGE_PROCESSING_CAPACITY) {
                System.out.println("\nOwing to limited processing capacity, only a total of "
                        + MESSAGE_PROCESSING_CAPACITY + " messages can and were processed. Processing stopped.");
                break;
            }
        }

        /* printing one last time because any processed and recorded "reminder"
            (14 notifications % 10 = 4 not reflected otherwise)
           sales won't get displayed on console otherwise.
         */
        System.out.println("\n*** Final Processed Sales' Record ***");
        register.printSalesReport();

        if(adjustmentsLog.length() != 0) {
            System.out.println("\n*** Adjustment Log ***");
            System.out.println(adjustmentsLog.toString());
        }

        return true;
    }
}