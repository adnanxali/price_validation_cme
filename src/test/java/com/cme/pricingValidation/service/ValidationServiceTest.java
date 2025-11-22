package com.cme.pricingValidation.service;

import com.cme.pricingValidation.model.PriceRecord;
import com.cme.pricingValidation.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void validRecord_shouldHaveNoErrors_andBeMarkedValid() {
        PriceRecord record = new PriceRecord(
                "1001",
                "2025-01-10",
                "120.50",
                "CME",
                "FUT"
        );

        List<ValidationResult> results = validationService.validateAll(List.of(record));
        assertEquals(1, results.size());

        ValidationResult res = results.get(0);
        assertTrue(res.getIfValid(), "Record should be valid");
        assertTrue(res.getErrors().isEmpty(), "Expected no validation errors");
    }

    @Test
    void missingFields_shouldBeReportedAsErrors() {
        PriceRecord record = new PriceRecord(
                "",      // missing guid
                "",      // missing date
                "",      // missing price
                "",      // missing exchange
                ""       // missing productType
        );

        List<ValidationResult> results = validationService.validateAll(List.of(record));
        ValidationResult res = results.get(0);

        assertFalse(res.getIfValid(), "Record should be invalid");
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Missing instrument_guid")),
                "Expected missing instrument_guid error"
        );
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Missing trade_date")),
                "Expected missing trade_date error"
        );
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Missing price")),
                "Expected missing price error"
        );
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Missing exchange")),
                "Expected missing exchange error"
        );
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Missing product_type")),
                "Expected missing product_type error"
        );
    }

    @Test
    void invalidPriceFormat_shouldBeReported() {
        PriceRecord record = new PriceRecord(
                "1001",
                "2025-01-10",
                "abc",          // invalid numeric format
                "CME",
                "FUT"
        );

        List<ValidationResult> results = validationService.validateAll(List.of(record));
        ValidationResult res = results.get(0);

        assertFalse(res.getIfValid(), "Record should be invalid");
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Invalid price format")),
                "Expected invalid price format error"
        );
    }


    @Test
    void futureTradeDate_shouldBeReported() {
        PriceRecord record = new PriceRecord(
                "1001",
                "2099-01-01",  // future date
                "120.50",
                "CME",
                "FUT"
        );

        List<ValidationResult> results = validationService.validateAll(List.of(record));
        ValidationResult res = results.get(0);

        assertFalse(res.getIfValid(), "Record should be invalid");
        assertTrue(
                res.getErrors().stream().anyMatch(msg -> msg.contains("Trade date cannot be in future")),
                "Expected future trade date error"
        );
    }

    @Test
    void duplicateRecords_shouldBeMarkedInvalidWithDuplicateMessage() {
        PriceRecord r1 = new PriceRecord(
                "1001",
                "2025-01-10",
                "120.50",
                "CME",
                "FUT"
        );

        PriceRecord r2 = new PriceRecord(
                "1001",
                "2025-01-10",
                "120.50",
                "CME",
                "FUT"
        );

        List<ValidationResult> results = validationService.validateAll(List.of(r1, r2));

        assertEquals(2, results.size());

        ValidationResult res1 = results.get(0);
        ValidationResult res2 = results.get(1);

        // first one can still be valid, second one should be duplicate
        assertTrue(res1.getIfValid(), "First occurrence should be valid");
        assertTrue(res1.getErrors().isEmpty(), "First occurrence should have no errors");

        assertFalse(res2.getIfValid(), "Duplicate record should be invalid");
        assertTrue(
                res2.getErrors().stream().anyMatch(msg -> msg.contains("Duplicate Record of Row")),
                "Expected duplicate record error message"
        );
    }
}
