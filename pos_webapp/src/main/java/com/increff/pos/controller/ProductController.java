package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.data.UploadStatusData;
import com.increff.pos.model.form.ProductForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
public class ProductController {

    @Autowired
    private ProductDto productDto;

    //todo: pagination
    @RequestMapping(value="/products", method = RequestMethod.GET)
    public List<ProductData> getAll() throws ApiException{
        return productDto.getAll();
    }

    @RequestMapping(value="/products/barcode/{barcode}", method = RequestMethod.GET)
    public ProductData getByBarcode(@PathVariable(value="barcode") String barcode) throws ApiException{
        return productDto.getByBarcode(barcode);
    }

    @RequestMapping(value="/products", method = RequestMethod.POST)
    public ProductData addProduct(@Valid @RequestBody ProductForm form) throws ApiException {
        return productDto.add(form);
    }

    @RequestMapping(value="/products/{id}", method = RequestMethod.GET)
    public ProductData getById(@PathVariable(value="id") Integer id) throws ApiException{
        return productDto.getById(id);
    }

    @RequestMapping(value="/products/{id}", method = RequestMethod.PUT)
    public ProductData updateById(@PathVariable(value="id") Integer id, @Valid @RequestBody ProductForm form) throws ApiException{
        return productDto.updateById(id,form);
    }

    @RequestMapping(value="/products/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)//return 204 code
    public void deleteById(@PathVariable(value="id") Integer id) throws ApiException {
        productDto.deleteById(id);
    }

    //TODO: make it return a tsv file with error msgs corresponding to the failed rows
    @RequestMapping(value="/products/upload",method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadProducts(@RequestParam("file") MultipartFile file) throws ApiException{
        return productDto.uploadByFile(file);
    }
}
