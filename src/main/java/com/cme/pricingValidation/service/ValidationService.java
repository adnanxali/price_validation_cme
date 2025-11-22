package com.cme.pricingValidation.service;

import com.cme.pricingValidation.entity.PriceRecordEntity;
import com.cme.pricingValidation.model.PriceRecord;
import com.cme.pricingValidation.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ValidationService {
    public static final Logger logger =LoggerFactory.getLogger(ValidationService.class);
    private static final Set<String> ValidExchanges = Set.of("CME", "NYMEX", "CBOT", "COMEX");
    private static final Set<String> ValidProductTypes = Set.of("FUT","OPT");
    private static final DateTimeFormatter validDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ValidationResult> validateAll(List<PriceRecord> records) {
        logger.info("Starting to validate the Records ",records.size());
        List<ValidationResult> result = new ArrayList<>();

        for(int i=0;i<records.size();i++){
            PriceRecord rec = records.get(i);
            List<String> errors = validateSingleRecord(rec,i+1);
            boolean valid = errors.isEmpty();
            result.add(new ValidationResult(rec,valid,errors));
        }
        detectDuplicates(result);

        return result;

    }
    public List<String> validateSingleRecord(PriceRecord record, int rowNumber){
        List<String> errors = new ArrayList<>();

        if (record.getInstrumentGuid() == null || record.getInstrumentGuid().isBlank()) {
            errors.add("Missing instrument_guid at row " + rowNumber);
        }

        if (record.getTradeDate() == null || record.getTradeDate().isBlank()) {
            errors.add("Missing trade_date at row " + rowNumber);
        }

        if (record.getExchange() == null || record.getExchange().isBlank()) {

            errors.add("Missing exchange at row " + rowNumber);
        }

        if (record.getProductType() == null || record.getProductType().isBlank()) {
            errors.add("Missing product_type at row " + rowNumber);
        }


        if (record.getPrice() == null || record.getPrice().isBlank()) {
            errors.add("Missing price at row " + rowNumber);
        } else {

            if (!isValidPrice(record.getPrice())) {
                errors.add("Invalid price format at row " + rowNumber + ": '" + record.getPrice() + "'");
            } else {

                try {
                    BigDecimal priceValue = new BigDecimal(record.getPrice());
                    if (priceValue.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add("Price must be positive at row " + rowNumber + ": " + record.getPrice());
                    }
                    if (priceValue.scale()>=3){
                        errors.add("Price must be rounded to 3 decimal places eg: 123.333 : "+record.getPrice());
                    }
                } catch (NumberFormatException e) {
                    errors.add("Invalid price format at row " + rowNumber + ": '" + record.getPrice() + "'");
                }
            }
        }


        if (record.getInstrumentGuid() != null && !record.getInstrumentGuid().isBlank()) {
            if (!isInteger(record.getInstrumentGuid())) {
                errors.add("Invalid instrument_guid format at row " + rowNumber +
                        " (must be numeric): '" + record.getInstrumentGuid() + "'");
            }
        }


        if (record.getTradeDate() != null && !record.getTradeDate().isBlank()) {

            if (!isValidDate(record.getTradeDate())) {
                errors.add("Invalid trade_date format at row " + rowNumber +
                        " (expected yyyy-MM-dd): '" + record.getTradeDate() + "'");
            } else {

                try {
                    LocalDate tradeDate = LocalDate.parse(record.getTradeDate(), validDateFormat);
                    if (tradeDate.isAfter(LocalDate.now())) {
                        errors.add("Trade date cannot be in future at row " + rowNumber + ": " + record.getTradeDate());
                    }
                } catch (DateTimeException e) {

                    errors.add("Invalid trade_date at row " + rowNumber+e.getMessage());
                }
            }
        }

        if (record.getExchange() != null && !record.getExchange().isBlank()) {
            String exchange = record.getExchange().toUpperCase().trim();
            if (!ValidExchanges.contains(exchange)) {
                errors.add("Invalid exchange at row " + rowNumber +
                        ": '" + record.getExchange() + "'. Must be one of: " + ValidExchanges);
            }
        }

        if (record.getProductType() != null && !record.getProductType().isBlank()) {
            String productType = record.getProductType().toUpperCase().trim();
            if (!ValidProductTypes.contains(productType)) {
                errors.add("Invalid product_type at row " + rowNumber +
                        ": '" + record.getProductType() + "'. Must be one of: " + ValidProductTypes);
            }
        }
        if (!errors.isEmpty()) {
            logger.debug("Row {} is invalid with errors: {}", rowNumber, errors);
        }

        return errors;
    }

    private void detectDuplicates(List<ValidationResult> results) {
        logger.info("Checking for duplicate records");

        Map<String, List<Integer>> recordGroups = new HashMap<>();

        for (int i = 0; i < results.size(); i++) {
            ValidationResult result = results.get(i);
            PriceRecord record = result.getPriceRecord();


            String key = String.join("|",
                    nullSafe(record.getInstrumentGuid()),
                    nullSafe(record.getTradeDate()),
                    nullSafe(record.getPrice()),
                    nullSafe(record.getExchange()),
                    nullSafe(record.getProductType())
            );
            recordGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }


        int duplicateGroupsFound = 0;
        for (Map.Entry<String, List<Integer>> entry : recordGroups.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() > 1) {
                duplicateGroupsFound++;

                Collections.sort(indices);

                int origIdx = indices.get(0);
                int origRow = origIdx+1;
                for(int i=1;i<indices.size();i++){
                    int dupIdx = indices.get(i);
                    int dupRow = dupIdx+1;
                    ValidationResult dupRec= results.get(dupIdx);

                    String errorMsg = "Duplicate Record of Row "+origRow +" at "+dupRow;

                    dupRec.getErrors().add(errorMsg);
                    dupRec.setIfValid(false);

                }

            }
        }

        logger.info("Found {} duplicate groups", duplicateGroupsFound);
    }

    public List<String> validateRecord(PriceRecordEntity record) {
        logger.debug("Validating entity record ID:",record.getId());

        PriceRecord rec = new PriceRecord(
                record.getInstrumentGuid(),
                record.getTradeDate(),
                record.getPrice(),
                record.getExchange(),
                record.getProductType()
        );
        List<String> errors = validateSingleRecord(rec,record.getRowNumber()==null?0:record.getRowNumber());

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
    private boolean isInteger(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidPrice(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return false;

        try {

            LocalDate.parse(dateStr, validDateFormat);
            return true;

        } catch (DateTimeException e) {
            return false;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

}
