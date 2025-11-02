package com.increff.pos.dto;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.flow.InventoryFlow;
import com.increff.pos.helper.InventoryMapper;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.utils.ResponseEntityUtil;
import com.increff.pos.utils.TsvUtil;
import com.increff.pos.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@Component
public class InventoryDto extends AbstractDto{

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private InventoryFlow inventoryFlow;

    @Autowired
    private InventoryMapper inventoryMapper;

    public InventoryData getById(Integer id) throws ApiException{
        Inventory inventoryPojo = inventoryApi.getCheckById(id);

        return inventoryMapper.convert(inventoryPojo);
    }

    public List<InventoryData> getAll(){
        List<Inventory> inventoryPojoList = inventoryApi.getAll();

        return inventoryMapper.convert(inventoryPojoList);
    }

    public InventoryData updateById(Integer id, InventoryForm inventoryForm) throws ApiException{
        ValidationUtil.validate(inventoryForm);

        Inventory inventoryPojo = inventoryMapper.convert(inventoryForm);

        Inventory updatedInventoryPojo = inventoryApi.updateById(id,inventoryPojo);

        return inventoryMapper.convert(updatedInventoryPojo);
    }

    public InventoryData updateByProductId(Integer productId,InventoryForm inventoryForm) throws ApiException{
        ValidationUtil.validate(inventoryForm);

        Inventory inventoryPojo = inventoryMapper.convert(inventoryForm);

        Inventory updatedInventoryPojo = inventoryApi.updateByProductId(productId,inventoryPojo);

        return inventoryMapper.convert(updatedInventoryPojo);
    }

    public ResponseEntity<byte[]> uploadByFile(MultipartFile file) throws ApiException{
        List<String> expectedHeaders = Arrays.asList("barcode","quantity");
        ConversionResult<String[]> tsvResult = TsvUtil.validateAndParse(file,expectedHeaders);
        TsvUtil.normalizeRows(tsvResult,expectedHeaders);

        byte[] reportBytes = inventoryFlow.uploadByFile(tsvResult);

        return ResponseEntityUtil.buildTsvResponse(reportBytes, "inventory-upload-report.tsv");
    }
}
