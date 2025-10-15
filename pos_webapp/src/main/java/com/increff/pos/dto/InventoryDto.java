package com.increff.pos.dto;

import com.increff.pos.api.InventoryApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.flow.InventoryFlow;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.data.UploadStatusData;
import com.increff.pos.model.form.InventoryForm;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.utils.InventoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class InventoryDto {

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private InventoryFlow inventoryFlow;

    public InventoryData getById(Integer id) throws ApiException{
        Inventory inventoryPojo = inventoryApi.getCheckById(id);

        return InventoryUtil.convert(inventoryPojo);
    }

    public List<InventoryData> getAll(){
        List<Inventory> inventoryPojoList = inventoryApi.getAll();

        return InventoryUtil.convert(inventoryPojoList);
    }

    public InventoryData updateById(Integer id, InventoryForm inventoryForm) throws ApiException{
        Inventory inventoryPojo = InventoryUtil.convert(inventoryForm);

        Inventory updatedInventoryPojo = inventoryApi.updateById(id,inventoryPojo);

        return InventoryUtil.convert(updatedInventoryPojo);
    }

    public InventoryData updateByProductId(Integer productId,InventoryForm inventoryForm) throws ApiException{
        Inventory inventoryPojo = InventoryUtil.convert(inventoryForm);

        Inventory updatedInventoryPojo = inventoryApi.updateByProductId(productId,inventoryPojo);

        return InventoryUtil.convert(updatedInventoryPojo);
    }

    public UploadStatusData uploadByFile(MultipartFile file) throws ApiException{
        ConversionResult<Inventory> conversionResult = inventoryFlow.convert(file);

        List<Inventory> inventoryList = conversionResult.getValidRows();
        List<String> errors = conversionResult.getErrors();
        Integer totalRows = inventoryList.size() + errors.size();

        inventoryApi.upload(inventoryList,errors);

        return InventoryUtil.convert(totalRows, errors);
    }
}
