package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.flow.ProductFlow;
import com.increff.pos.helper.ProductMapper;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.ResponseEntityUtil;
import com.increff.pos.utils.ValidationUtil;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProductDto extends AbstractDto{

    @Autowired
    private ProductApi productApi;

    @Autowired
    private ProductFlow productFlow;

    @Autowired
    private InventoryApi inventoryApi;

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private ProductMapper productMapper;

    public ProductData add(ProductForm productForm) throws ApiException {
        ValidationUtil.validate(productForm);
        normalize(productForm,Arrays.asList("barcode"));

        Product productPojo = productMapper.convert(productForm);

        Product addedPojo = productFlow.insert(productPojo);

        Inventory inventory = inventoryApi.getCheckByProductId(addedPojo.getId());
        Client client = clientApi.getById(addedPojo.getClientId());

        return productMapper.convert(addedPojo,client,inventory);
    }

    public PaginationData<ProductData> getFilteredProducts(String searchTerm, String clientName, String category, Double minMrp, Double maxMrp,Integer size,Integer page) throws ApiException{
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").ascending());

        PaginatedResult<Product> paginatedResult = productApi.getFilteredProducts(searchTerm,clientName,category,minMrp,maxMrp,pageable);

        List<Client> clients = clientApi.getByIds(paginatedResult.getResults().stream().map(Product::getClientId).collect(Collectors.toList()));
        List<Inventory> inventories = inventoryApi.getByProductIds(paginatedResult.getResults().stream().map(Product::getId).collect(Collectors.toList()));

        Map<Integer, Client> clientMap = clients.stream()
                .collect(Collectors.toMap(
                        Client::getId,         // Key
                        Function.identity(),   // Value (the client object itself)
                        (existing, replacement) -> existing // Handle duplicate keys
                ));

// 2. Convert List<Inventory> to Map<Integer, Inventory> (keyed by Product ID)
        Map<Integer, Inventory> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(
                        Inventory::getProductId, // Key
                        Function.identity(),     // Value (the inventory object itself)
                        (existing, replacement) -> existing // Handle duplicate keys
                ));

        return productMapper.convert(paginatedResult,clientMap,inventoryMap);
    }

    public ProductData getById(Integer id) throws ApiException{
        Product productPojo = productApi.getCheckById(id);

        Inventory inventory = inventoryApi.getCheckByProductId(productPojo.getId());
        Client client = clientApi.getById(productPojo.getClientId());

        return productMapper.convert(productPojo,client,inventory);
    }

    public ProductData getByBarcode(String barcode) throws ApiException{
        Product productPojo = productApi.getCheckByBarcode(barcode);

        Inventory inventory = inventoryApi.getCheckByProductId(productPojo.getId());
        Client client = clientApi.getById(productPojo.getClientId());

        return productMapper.convert(productPojo,client,inventory);
    }

    public ProductData updateById(Integer id, ProductForm productForm) throws ApiException{
        ValidationUtil.validate(productForm);
        normalize(productForm,Arrays.asList("barcode"));

        Product productPojo = productMapper.convert(productForm);
        Product updatedPojo = productApi.updateById(id,productPojo);

        Inventory inventory = inventoryApi.getCheckByProductId(updatedPojo.getId());
        Client client = clientApi.getById(updatedPojo.getClientId());

        return productMapper.convert(updatedPojo,client,inventory);
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
