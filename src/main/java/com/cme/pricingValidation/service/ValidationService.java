package com.cme.pricingValidation.service;

import com.cme.pricingValidation.entity.PriceRecordEntity;
import com.cme.pricingValidation.model.PriceRecord;
import com.cme.pricingValidation.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ValidationService {
    public static final Logger logger =LoggerFactory.getLogger(ValidationService.class);
    private boolean isInteger(String value){
        try{
            Integer.parseInt(value);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
    private boolean isDouble(String value){
        try{
            Double.parseDouble(value);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
    private boolean isValidDate(String dateStr, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDate.parse(dateStr, formatter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public List<ValidationResult> validateAll(List<PriceRecord> records) {
        logger.info("Starting to validate the Records ");
        Map<String,Integer> seen = new HashMap<>();
        List<ValidationResult> result = new ArrayList<>();

        logger.info("Checking for missing Values");
        for (int i = 0; i < records.size(); i++) {
            List<String> errors = new ArrayList<>();
            PriceRecord r = records.get(i);
            if (r.getInstrumentGuid() == null || r.getInstrumentGuid().isBlank()) {
                errors.add("Missing Guid at row " + i);
            }
            if (r.getPrice() == null || r.getPrice().isBlank()) {
                errors.add("Missing price at row " + i);
            }
            if (r.getExchange() == null || r.getExchange().isBlank()) {
                errors.add("Missing exchange at row " + i);
            }
            if (r.getTradeDate() == null || r.getTradeDate().isBlank()) {
                errors.add("Missing trade_date at row " + i);
            }
            if (r.getProductType() == null || r.getProductType().isBlank()) {
                errors.add("Missing product_type at row " + i);
            }

            //Checking for Invalid Data



            logger.info("Checking for Duplicate Values ");
            // duplicate check
            String key = r.getInstrumentGuid() + "|" + r.getTradeDate();
            if (seen.containsKey(key)) {
                errors.add("Duplicate record at row " + seen.get(key) + " and " + i);
            } else {
                seen.put(key, i);
            }
            boolean valid = errors.isEmpty();
            logger.debug("Adding result to result array ",result);
            result.add(new ValidationResult(r, valid, errors));
        }

        logger.info("Returning Results of Validation");
        return result;
    }
    public List<String> validateRecord(PriceRecordEntity record) {
        List<String> errors = new ArrayList<>();


        if (record.getPrice() == null || record.getPrice().isBlank())
            errors.add("Missing price at Row"+ record.getRowNumber());
        else {
            try { Double.parseDouble(record.getPrice()); }
            catch (NumberFormatException e) { errors.add("Invalid price format at Row " +record.getRowNumber()); }
        }

        if (record.getExchange() == null || record.getExchange().isBlank())
            errors.add("Missing exchange at Row "+record.getRowNumber());

        if (record.getProductType() == null || record.getProductType().isBlank())
            errors.add("Missing product type at Row "+record.getRowNumber());

        if (record.getTradeDate() == null || record.getTradeDate().isBlank())
            errors.add("Missing Trade Date at Row "+record.getTradeDate());

        return errors;
    }
    public Map<String,Object> summary(List<ValidationResult> result){
        long valid  = result.stream().filter(ValidationResult::getIfValid).count();
        long invalid = result.size()-valid;
        List<String> totalErrors = new ArrayList<>();
        for(ValidationResult r : result){
            totalErrors.addAll(r.getErrors());
        }
        Map<String,Object> sum = new HashMap<>();
        sum.put("total",result.size());
        sum.put("valid",valid);
        sum.put("invalid",invalid);
        sum.put("Errors",totalErrors);

        return sum;
    }

}
