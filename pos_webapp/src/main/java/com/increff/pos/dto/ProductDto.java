package com.increff.pos.dto;

import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.data.UploadStatusData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.utils.NormalizeUtil;
import com.increff.pos.utils.ProductUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.increff.pos.utils.TsvUtil;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductDto {

    @Autowired
    private ProductApi productApi;

    @Autowired
    private ProductFlow productFlow;

    public ProductData add(ProductForm productForm) throws ApiException {
        NormalizeUtil.normalize(productForm);
        Product productPojo = ProductUtil.convert(productForm);

        productFlow.insert(productPojo);

        return productFlow.convert(productPojo);
    }

    public List<ProductData> getAll() throws ApiException{
        List<Product> productsPojo = productApi.getAll();

        return productFlow.convert(productsPojo);
    }

    public ProductData getById(Integer id) throws ApiException{
        Product productPojo = productApi.getCheckById(id);

        return productFlow.convert(productPojo);
    }

    public ProductData getByBarcode(String barcode) throws ApiException{
        Product productPojo = productApi.getCheckByBarcode(barcode);

        return productFlow.convert(productPojo);
    }

    public ProductData updateById(Integer id, ProductForm productForm) throws ApiException{
        NormalizeUtil.normalize(productForm);
        Product productPojo = ProductUtil.convert(productForm);

        Product updatedPojo = productApi.updateById(id,productPojo);

        return productFlow.convert(updatedPojo);
    }

    public void deleteById(Integer id) throws ApiException{
        productFlow.deleteById(id);
    }

    //TODO: validate the file - make a template for file
    public ResponseEntity<byte[]> uploadByFile(MultipartFile file) throws ApiException{
        List<String> expectedHeaders = Arrays.asList("barcode","name","mrp","clientName","category");
        ConversionResult<String[]> tsvResult = TsvUtil.validateAndParse(file,expectedHeaders);

        byte[] reportBytes = productFlow.uploadByFile(tsvResult);

        return TsvUtil.buildTsvResponse(reportBytes, "product-upload-report.tsv");
    }
}
