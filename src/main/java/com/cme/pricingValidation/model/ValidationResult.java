package com.cme.pricingValidation.model;

import java.util.List;

public class ValidationResult {
    private PriceRecord record;
    private boolean valid;
    private List<String> errors;

    ValidationResult(){}
    public ValidationResult(PriceRecord record, boolean valid, List<String> errors){
        this.record = record;
        this.valid = valid;
        this.errors = errors;
    }

    public PriceRecord getPriceRecord(){return record;}
    public boolean getIfValid(){return valid;}
    public List<String> getErrors(){return  errors;}

    public void setPriceRecord(PriceRecord record){
        this.record = record;
    }
    public void setIfValid(boolean valid){
        this.valid =valid;
    }

    public void setErrors(List<String> errors){
        this.errors = errors;
    }


}
