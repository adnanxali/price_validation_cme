package com.cme.pricingValidation.service;

import com.cme.pricingValidation.model.PriceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVReader {
    private static final Logger logger = LoggerFactory.getLogger(CSVReader.class);
    public List<PriceRecord> read (MultipartFile file) throws IOException{
        logger.info("Starting to read CSV file...",file.getOriginalFilename());

        List<PriceRecord> records = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                logger.warn("CSV file {} is empty", file.getOriginalFilename());
                throw new IllegalArgumentException("CSV file is empty");
            }
            String[] headerTokens = headerLine.split(",", -1);
            for (int i = 0; i < headerTokens.length; i++) {
                headerTokens[i] = headerTokens[i].trim();
            }

            int guidIdx = findColumnIndex(headerTokens, "instrumentGuid", "instrument_guid");
            int tradeDateIdx = findColumnIndex(headerTokens, "tradeDate", "trade_date");
            int priceIdx = findColumnIndex(headerTokens, "price");
            int exchangeIdx = findColumnIndex(headerTokens, "exchange");
            int productTypeIdx = findColumnIndex(headerTokens, "productType", "product_type");

            if (guidIdx == -1 || tradeDateIdx == -1 || priceIdx == -1
                    || exchangeIdx == -1 || productTypeIdx == -1) {

                logger.error("CSV headers missing required columns. Found headers: {}", String.join(",", headerTokens));
                throw new IllegalArgumentException(
                        "CSV must contain headers: instrumentGuid, tradeDate, price, exchange, productType"
                );
            }
            String line;

            while ((line = br.readLine())!= null){
                if(line.trim().isEmpty()) continue;
                String[] tokens = line.split(",",-1);
                String guid = tokens.length >0 ? tokens[0].trim():"";
                String tradeDate = tokens.length > 1 ? tokens[1].trim():"";
                String price = tokens.length > 2 ? tokens[2].trim():"";
                String exchange = tokens.length > 3 ? tokens[3].trim():"";
                String productType = tokens.length > 4 ? tokens[4].trim():"";
                records.add(new PriceRecord(guid,tradeDate,price,exchange,productType));

            }
        }
        logger.info("Completed Parsing returning Records ");
        return records;

    }
    private int findColumnIndex(String[] headers, String... possibleNames) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            for (String name : possibleNames) {
                if (h.equals(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

}
