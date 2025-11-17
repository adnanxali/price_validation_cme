package com.cme.pricingValidation.service;

import com.cme.pricingValidation.model.ValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportService {

    private final List<ValidationResult> reportStore = new ArrayList<>();

    public void saveResult(List<ValidationResult> results){
        reportStore.clear();
        reportStore.addAll(results);
    }

}
