package com.increff.pos.dto;

import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.ProductUtil;
import com.increff.pos.utils.ResponseEntityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.increff.pos.utils.TsvUtil;
import java.util.Arrays;
import java.util.List;

@Component
public class ProductDto extends AbstractDto{

    @Autowired
    private ProductApi productApi;

    @Autowired
    private ProductFlow productFlow;

    public ProductData add(ProductForm productForm) throws ApiException {
        normalize(productForm,Arrays.asList("barcode","imageUrl"));

        Product productPojo = ProductUtil.convert(productForm);

        Product addedPojo = productFlow.insert(productPojo);

        return productFlow.convert(addedPojo);
    }

    public PaginationData<ProductData> getFilteredProducts(String searchTerm, String clientName, String category, Double minMrp, Double maxMrp,Integer size,Integer page) throws ApiException{
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").ascending());

        PaginatedResult<Product> paginatedResult = productApi.getFilteredProducts(searchTerm,clientName,category,minMrp,maxMrp,pageable);

        return productFlow.convert(paginatedResult);
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
        normalize(productForm,Arrays.asList("barcode","imageUrl"));

        Product productPojo = ProductUtil.convert(productForm);

        Product updatedPojo = productApi.updateById(id,productPojo);

        return productFlow.convert(updatedPojo);
    }

    public void deleteById(Integer id) throws ApiException{
        productFlow.deleteById(id);
    }

    public ResponseEntity<byte[]> uploadByFile(MultipartFile file) throws ApiException{
        List<String> expectedHeaders = Arrays.asList("barcode","name","mrp","clientName","category");
        ConversionResult<String[]> tsvResult = TsvUtil.validateAndParse(file,expectedHeaders);
        TsvUtil.normalizeRows(tsvResult,expectedHeaders);

        byte[] reportBytes = productFlow.uploadByFile(tsvResult);

        return ResponseEntityUtil.buildTsvResponse(reportBytes, "product-upload-report.tsv");
    }
}
