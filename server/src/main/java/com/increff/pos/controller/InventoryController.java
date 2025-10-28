package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.InventoryDto;
import com.increff.pos.model.data.InventoryData;
import com.increff.pos.model.form.InventoryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryDto inventoryDto;

    @RequestMapping(method=RequestMethod.GET)
    public List<InventoryData> getAll(){
        return inventoryDto.getAll();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public InventoryData getById(@PathVariable(value="id")Integer id) throws ApiException{
        return inventoryDto.getById(id);
    }

    @RequestMapping(value="/{id}",method = RequestMethod.PUT)
    public InventoryData updateById(@PathVariable(value="id")Integer id, InventoryForm form) throws ApiException{
        return inventoryDto.updateById(id,form);
    }

    @RequestMapping(value="/product/{productId}",method = RequestMethod.PUT)
    public InventoryData updateByProductId(@PathVariable(value="productId")Integer productId,@RequestBody InventoryForm inventoryForm) throws ApiException{
        return inventoryDto.updateByProductId(productId,inventoryForm);
    }

    @RequestMapping(value="/upload",method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadInventory(@RequestParam("file") MultipartFile file) throws ApiException{
        return inventoryDto.uploadByFile(file);
    }
}
