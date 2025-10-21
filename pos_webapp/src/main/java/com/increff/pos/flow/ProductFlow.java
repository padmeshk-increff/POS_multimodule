package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.ProductUploadResult;
import com.increff.pos.utils.ProductUtil;
import com.increff.pos.utils.TsvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = ApiException.class)
public class ProductFlow {

    @Autowired
    private ProductApi productApi;

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private InventoryApi inventoryApi;

    public Product insert(Product product) throws ApiException {
        clientApi.getCheckById(product.getClientId());
        Product insertedProduct = productApi.insert(product);

        Inventory inventory = new Inventory();
        inventory.setProductId(product.getId());
        inventory.setQuantity(0); //create an inventory for the product by default

        inventoryApi.insert(inventory);
        return insertedProduct;
    }

    public ProductData convert(Product product) throws ApiException{
        ProductData productData = ProductUtil.convert(product);
        Inventory inventory = inventoryApi.getCheckByProductId(product.getId());
        Client client = clientApi.getById(product.getClientId());
        productData.setQuantity(inventory.getQuantity());
        productData.setClientName(client.getClientName());
        return productData;
    }

    public List<ProductData> convert(List<Product> products) throws ApiException{
        List<ProductData> productsData = new ArrayList<>();
        for(Product product:products){
            productsData.add(convert(product));
        }
        return productsData;
    }

    public void deleteById(Integer id) throws ApiException{
        productApi.deleteById(id);
        inventoryApi.deleteById(id);
    }

    public byte[] uploadByFile(ConversionResult<String[]> tsvResult) throws ApiException {
        // --- Step 1: Convert raw string arrays to structured DTOs ---
        ConversionResult<ProductUploadRow> conversionResult = ProductUtil.convertRows(tsvResult);
        List<ProductUploadRow> candidateRows = conversionResult.getValidRows();
        List<String> initialErrors = conversionResult.getErrors();

        // --- Step 2: High-Performance Bulk Lookups (The Orchestration) ---
        // This is the core responsibility of the Flow layer.
        Set<String> clientNamesInFile = candidateRows.stream().map(p -> p.getClientName().trim().toLowerCase()).collect(Collectors.toSet());
        Set<String> barcodesInFile = candidateRows.stream().map(p -> p.getBarcode().trim().toLowerCase()).collect(Collectors.toSet());

        // Call ClientApi to get client data
        Map<String, Client> clientMap = clientApi.getByNames(new ArrayList<>(clientNamesInFile))
                .stream().collect(Collectors.toMap(Client::getClientName, Function.identity()));

        // Call ProductApi to get existing product data
        Set<String> existingBarcodesInDb = productApi.getByBarcodes(new ArrayList<>(barcodesInFile))
                .stream().map(Product::getBarcode).collect(Collectors.toSet());

        // --- Step 3: Pass to the API layer for business logic processing ---
        // We now pass the pre-fetched data maps to the API layer.
        ProductUploadResult uploadResult = productApi.upload(candidateRows, clientMap, existingBarcodesInDb);

        List<Product> newProducts = uploadResult.getSuccessfullyInserted();
        if (!newProducts.isEmpty()) {
            // We pass this list to the InventoryApi to create their default inventory records.
            inventoryApi.initializeInventory(newProducts);
        }

        // --- Step 4: Generate the final TSV report file ---
        return TsvUtil.createProductUploadReport(uploadResult, candidateRows, initialErrors);
    }

}
