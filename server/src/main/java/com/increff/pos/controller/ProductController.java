package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.ProductDto;
import com.increff.pos.model.data.PaginationData;
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
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductDto productDto;

    @RequestMapping(method = RequestMethod.GET)
    public PaginationData<ProductData> getFilteredProducts(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minMrp,
            @RequestParam(required = false) Double maxMrp,
            @RequestParam(defaultValue = "10")Integer size,
            @RequestParam(defaultValue = "0")Integer page
    ) throws ApiException{
        return productDto.getFilteredProducts(searchTerm,clientName,category,minMrp,maxMrp,size,page);
    }

    @RequestMapping(value="/barcode/{barcode}", method = RequestMethod.GET)
    public ProductData getByBarcode(@PathVariable(value="barcode") String barcode) throws ApiException{
        return productDto.getByBarcode(barcode);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ProductData addProduct(@Valid @RequestBody ProductForm form) throws ApiException {
        return productDto.add(form);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.GET)
    public ProductData getById(@PathVariable(value="id") Integer id) throws ApiException{
        return productDto.getById(id);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.PUT)
    public ProductData updateById(@PathVariable(value="id") Integer id, @Valid @RequestBody ProductForm form) throws ApiException{
        return productDto.updateById(id,form);
    }

    @RequestMapping(value="/{id}", method = RequestMethod.DELETE)
    public void deleteById(@PathVariable(value="id") Integer id) throws ApiException {
        productDto.deleteById(id);
    }

    @RequestMapping(value="/upload",method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> uploadProducts(@RequestParam("file") MultipartFile file) throws ApiException{
        return productDto.uploadByFile(file);
    }
}
