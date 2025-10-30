package com.increff.pos.flow;

import com.increff.pos.api.ClientApi;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.result.ConversionResult;
import com.increff.pos.model.result.ProductUploadResult;
import com.increff.pos.utils.ProductUtil;
import com.increff.pos.utils.TsvUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
        inventory.setQuantity(0);

        inventoryApi.insert(inventory);
        return insertedProduct;
    }

    public void deleteById(Integer id) throws ApiException{
        productApi.deleteById(id);
        inventoryApi.deleteById(id);
    }

    public byte[] uploadByFile(ConversionResult<String[]> tsvResult) throws ApiException {
        ConversionResult<ProductUploadRow> conversionResult = ProductUtil.convertRows(tsvResult);
        List<ProductUploadRow> candidateRows = conversionResult.getValidRows();
        List<String> initialErrors = conversionResult.getErrors();

        Set<String> clientNamesInFile = ProductUtil.getClientNames(candidateRows);
        Set<String> barcodesInFile = ProductUtil.getBarcodes(candidateRows,ProductUploadRow::getBarcode);

        List<Client> clients = clientApi.getByNames(new ArrayList<>(clientNamesInFile));

        Map<String, Client> clientMap = ProductUtil.mapByName(clients);

        List<Product> products = productApi.getByBarcodes(new ArrayList<>(barcodesInFile));
        Set<String> existingBarcodesInDb = ProductUtil.getBarcodes(products,Product::getBarcode);

        ProductUploadResult uploadResult = productApi.upload(candidateRows, clientMap, existingBarcodesInDb);

        List<Product> newProducts = uploadResult.getSuccessfullyInserted();
        if (!newProducts.isEmpty()) {
            inventoryApi.initializeInventory(newProducts);
        }

        return TsvUtil.createProductUploadReport(uploadResult, candidateRows, initialErrors);
    }

}
