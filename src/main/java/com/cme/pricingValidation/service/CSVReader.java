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
            String line;
            logger.debug("Skipping the headers");
            boolean headers = true;
            logger.info("Parsing the file into tokens ");
            while ((line = br.readLine())!= null){
                if(headers){headers = false; continue;}
                if(line.trim().isEmpty()) continue;
                String[] tokens = line.split(",",-1);
                String guid = tokens.length >0 ? tokens[0].trim():"";
                String tradeDate = tokens.length > 0 ? tokens[1].trim():"";
                String price = tokens.length > 0 ? tokens[2].trim():"";
                String exchange = tokens.length > 0 ? tokens[3].trim():"";
                String productType = tokens.length > 0 ? tokens[4].trim():"";
                records.add(new PriceRecord(guid,tradeDate,price,exchange,productType));

            }
        }
        logger.info("Completed Parsing returning Records ");
        return records;

    }

}
