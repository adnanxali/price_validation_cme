package com.cme.pricingValidation.controller;

import com.cme.pricingValidation.entity.PriceRecordEntity;
import com.cme.pricingValidation.model.PriceRecord;
import com.cme.pricingValidation.model.ValidationResult;
import com.cme.pricingValidation.repository.PriceRecordRepository;
import com.cme.pricingValidation.service.CSVReader;
import com.cme.pricingValidation.service.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing")
@CrossOrigin(
        origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"},
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
                PriceRecordEntity entity = new PriceRecordEntity(
                    rec.getInstrumentGuid(),rec.getTradeDate(),rec.getPrice(),rec.getExchange(),rec.getProductType(),
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
        List<ValidationResult> result = validationService.validateAll(records);
        Map<String,Object> summary = validationService.summary(result);

        repository.deleteAll();
        int row=1;
        for(ValidationResult r:result){
            PriceRecord rc = r.getPriceRecord();
            PriceRecordEntity e = new PriceRecordEntity(
                    rc.getInstrumentGuid(),
                    rc.getTradeDate(),
                    rc.getPrice(),
                    rc.getExchange(),
                    rc.getProductType(),
                    row++,
                    r.getIfValid(),
                    String.join(",",r.getErrors())
            );
            repository.save(e);
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
        return repository.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getRecordsById(@PathVariable Long id){
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecordsById(@PathVariable Long id, @RequestBody PriceRecordEntity p){
        return repository.findById(id).map(val ->{
            if(p.getInstrumentGuid()!=null) val.setInstrumentGuid(p.getInstrumentGuid());
            if(p.getPrice()!=null) val.setPrice(p.getPrice());
            if(p.getExchange()!=null) val.setInstrumentGuid(p.getInstrumentGuid());
            if(p.getProductType()!=null) val.setProductType(p.getProductType());
            if(p.getTradeDate()!=null) val.setTradeDate(p.getTradeDate());

            List<String> newErrors = validationService.validateRecord(val);
            val.setValid(newErrors.isEmpty());
            val.setErrors(newErrors.isEmpty()?null:String.join(",",newErrors));

            repository.save(val);
            return ResponseEntity.ok(val);

        }).orElse(ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long id){
        if(!repository.existsById(id)){
            return ResponseEntity.status(HttpStatusCode.valueOf(404)).body(Map.of("errors","Not Found any Records"));
        }else{
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
        long total = allRecords.size();
        long valid = allRecords.stream().filter(PriceRecordEntity::isValid).count();
        long invalid = total-valid;

        List<Integer> invalidRows = allRecords.stream().filter(x->!x.isValid()).map(PriceRecordEntity::getRowNumber).toList();

        return Map.of(
                "total",total,"valid",valid,"invalid",invalid,"invalidRows",invalidRows
        );
    }


}
