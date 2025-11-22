package com.cme.pricingValidation.controller;

import com.cme.pricingValidation.entity.PriceRecordEntity;
import com.cme.pricingValidation.model.PriceRecord;
import com.cme.pricingValidation.model.ValidationResult;
import com.cme.pricingValidation.repository.PriceRecordRepository;
import com.cme.pricingValidation.service.CSVReader;
import com.cme.pricingValidation.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(
        origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000","https://price-validation-cme-frontend.vercel.app"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowedHeaders = "*",
        allowCredentials = "true"
)
public class PriceController {
    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);

    private final CSVReader csvReader;
    private final ValidationService validationService;
    private final PriceRecordRepository repository;
    public PriceController(CSVReader csvReader,ValidationService validationService,PriceRecordRepository repository){
        this.csvReader = csvReader;
        this.validationService = validationService;
        this.repository = repository;
    }

    @PostMapping("/validate-file")
    public ResponseEntity<?> validateFile(@RequestParam("file") MultipartFile file){
        logger.debug("/validate-file route hit with file",file);
        try{
            List<PriceRecord> records = csvReader.read(file);
            List<ValidationResult> result = validationService.validateAll(records);
            Map<String,Object> summary = validationService.summary(result);

            logger.debug("Clearing data");
            repository.deleteAll();
            int rowNumber=1;
            for(ValidationResult r : result){
                PriceRecord rec = r.getPriceRecord();
                boolean isDuplicate = r.getErrors().stream().anyMatch(msg->msg.startsWith("Duplicate Record of Row"));
                if(isDuplicate){

                    logger.debug("Duplicate Detected Skipping this record");
                    continue;
                }

                PriceRecordEntity entity = new PriceRecordEntity(
                    rec.getInstrumentGuid(),rec.getTradeDate(),rec.getPrice(),rec.getExchange().toUpperCase(),rec.getProductType().toUpperCase(),
                        rowNumber++,r.getIfValid(),String.join(",",r.getErrors())
                );
                repository.save(entity);
            }


            return ResponseEntity.ok(Map.of("summary",summary,"result",result));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Could not process file", "message", e.getMessage()));
        }
    }
    @PostMapping("/validate-json")
    public ResponseEntity<?> validateJson(@RequestBody List<PriceRecord> records){
        logger.info("Json data is being Entered ");
        List<ValidationResult> result = validationService.validateAll(records);
        Map<String,Object> summary = validationService.summary(result);

        logger.debug("Clearing Previous Data");
        repository.deleteAll();
        int row=1;
        for(ValidationResult r : result){
            PriceRecord rec = r.getPriceRecord();
            boolean isDuplicate = r.getErrors().stream().anyMatch(msg->msg.startsWith("Duplicate Record of Row"));
            if(isDuplicate){

                logger.debug("Duplicate Detected Skipping this record");
                continue;
            }

            PriceRecordEntity entity = new PriceRecordEntity(
                    rec.getInstrumentGuid(),rec.getTradeDate(),rec.getPrice(),rec.getExchange().toUpperCase(),rec.getProductType().toUpperCase(),
                    row++,r.getIfValid(),String.join(",",r.getErrors())
            );
            logger.info("Record Saved ");
            repository.save(entity);
        }
        return ResponseEntity.ok(Map.of("summary",summary,"result",result));
    }

    @PostMapping("/validate-record")
    public ResponseEntity<?> validateSingle(@RequestBody PriceRecord record) {
        List<ValidationResult> results = validationService.validateAll(List.of(record));
        return ResponseEntity.ok(results.get(0));
    }
    @GetMapping("/all")
    public List<PriceRecordEntity> getAllRecords() {
        logger.info("Fetching All Records");
        return repository.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecordsById(@PathVariable Long id){
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/export-report")
    public ResponseEntity<byte[]> exportValidationReport() {


        List<PriceRecordEntity> allRecords = repository.findAll();
        List<PriceRecordEntity> invalidRecords = repository.findByValidFalse();

        long total = allRecords.size();
        long valid = allRecords.stream().filter(PriceRecordEntity::isValid).count();
        long invalid = total - valid;
        double percentAcc = total == 0 ? 0.0 : ((double) valid / total) * 100d;

        String percentAccStr = String.format("%.2f", percentAcc);

        List<Integer> invalidRows = new ArrayList<>();
        for (PriceRecordEntity r : invalidRecords) {
            if (r.getRowNumber() != null) {
                invalidRows.add(r.getRowNumber());
            }
        }

        StringBuilder sb = new StringBuilder();


        sb.append("Summary\n");
        sb.append("Total Records,").append(total).append("\n");
        sb.append("Valid Records,").append(valid).append("\n");
        sb.append("Invalid Records,").append(invalid).append("\n");
        sb.append("Valid Percentage, ").append(percentAccStr).append("\n");

        sb.append("Invalid Row Numbers,");
        if (invalidRows.isEmpty()) {
            sb.append("None\n");
        } else {
            sb.append(invalidRows.stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(" ")))
                    .append("\n");
        }

        sb.append("\n");
        sb.append("Invalid Records Detail\n");
        sb.append("rowNumber,instrumentGuid,tradeDate,price,exchange,productType,errors\n");

        for (PriceRecordEntity e : invalidRecords) {
            sb.append(e.getRowNumber() == null ? "" : e.getRowNumber()).append(",");
            sb.append(nullSafe(e.getInstrumentGuid())).append(",");
            sb.append(nullSafe(e.getTradeDate())).append(",");
            sb.append(nullSafe(e.getPrice())).append(",");
            sb.append(nullSafe(e.getExchange())).append(",");
            sb.append(nullSafe(e.getProductType())).append(",");

            String errors = e.getErrors();
            if (errors != null) {
                String escaped = errors.replace("\"", "\"\"");
                sb.append("\"").append(escaped).append("\"");
            } else {
                sb.append("");
            }
            sb.append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pricing-validation-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecordsById(@PathVariable Long id, @RequestBody PriceRecordEntity p){
        return repository.findById(id).map(val ->{
            if(p.getInstrumentGuid()!=null) val.setInstrumentGuid(p.getInstrumentGuid());
            if(p.getPrice()!=null) val.setPrice(p.getPrice());
            if(p.getExchange()!=null) val.setExchange(p.getExchange());
            if(p.getProductType()!=null) val.setProductType(p.getProductType());
            if(p.getTradeDate()!=null) val.setTradeDate(p.getTradeDate());

            List<String> newErrors = validationService.validateRecord(val);

            if(canBeDuplicate(val)){
                return ResponseEntity.badRequest().body(
                        Map.of("error","This action will create a duplicate record")
                );
            }

            val.setValid(newErrors.isEmpty());
            val.setErrors(newErrors.isEmpty()?null:String.join(",",newErrors));

            repository.save(val);
            return ResponseEntity.ok(val);

        }).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id){
        logger.info("User Deleted record with ID",id);
        if(!repository.existsById(id)){
            logger.debug("Could not find the Record");
            return ResponseEntity.status(HttpStatusCode.valueOf(404)).body(Map.of("errors","Not Found any Records"));
        }else{
            logger.warn("User Deleted Record: ",repository.findById(id));
            repository.deleteById(id);
            return ResponseEntity.ok(Map.of("message","Deleted record !"));
        }
    }

    @GetMapping("/invalid")
    public List<PriceRecordEntity> getInvalidRecords(){
        return repository.findByValidFalse();
    }
    @GetMapping("/valid")
    public List<PriceRecordEntity> getValidRecords(){
        return repository.findByValidTrue();
    }

    @GetMapping("/summary")
    public Map<String,Object> getSummary(){
        List<PriceRecordEntity> allRecords = repository.findAll();
        List<PriceRecordEntity> p = repository.findByValidFalse();
        List<String> allErrors = new ArrayList<>();
        for(PriceRecordEntity r:p){
            allErrors.add(r.getErrors());
        }

        long total = allRecords.size();
        long valid = allRecords.stream().filter(PriceRecordEntity::isValid).count();
        long invalid = total-valid;

        List<Integer> invalidRows = allRecords.stream().filter(x->!x.isValid()).map(PriceRecordEntity::getRowNumber).toList();

        return Map.of(
                "total",total,"valid",valid,"invalid",invalid,"invalidRows",invalidRows
        ,"errors",allErrors);
    }

    private boolean canBeDuplicate(PriceRecordEntity e){
        List<PriceRecordEntity> same = repository.findByInstrumentGuidAndTradeDateAndPriceAndExchangeAndProductType(
                e.getInstrumentGuid(),
                e.getTradeDate(),
                e.getPrice(),
                e.getExchange(),
                e.getProductType()
        );
        return same.stream().anyMatch(k -> !k.getId().equals(e.getId()));
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

}
