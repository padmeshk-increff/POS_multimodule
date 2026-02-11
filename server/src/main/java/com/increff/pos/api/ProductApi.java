package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.ProductDao;
import com.increff.pos.entity.Client;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.FailedUploadRow;
import com.increff.pos.model.data.ProductUploadRow;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.model.result.ProductUploadResult;
import com.increff.pos.utils.ProductUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = ApiException.class)
public class ProductApi extends AbstractApi {

    @Autowired
    private ProductDao productDao;


    public Product insert(Product product) throws ApiException {
        checkNull(product, "Product object cannot be null");

        Product existingProduct = productDao.selectByBarcode(product.getBarcode());
        checkNotNull(existingProduct, "Product already exists");

        productDao.insert(product);
        return product;
    }

    public PaginatedResult<Product> getFilteredProducts(
            String searchTerm, String clientName, String category, Double minMrp, Double maxMrp, Pageable pageable)
            throws ApiException {

        checkNull(pageable, "Pageable object cannot be null");

        Long totalElements = productDao.countWithFilters(searchTerm, clientName, category, minMrp, maxMrp);

        if (totalElements == 0) {
            return ProductUtil.createEmptyResult();
        }

        List<Product> results = productDao.selectWithFilters(searchTerm, clientName, category, minMrp, maxMrp, pageable);

        PaginatedResult<Product> paginatedResult = new PaginatedResult<>();
        paginatedResult.setResults(results);
        paginatedResult.setTotalElements(totalElements);

        if (pageable.getPageSize() > 0) {
            paginatedResult.setTotalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()));
        } else {
            paginatedResult.setTotalPages(1);
        }

        return paginatedResult;
    }

    public Product getById(Integer id) throws ApiException {
        checkNull(id, "Id cannot be null");

        return productDao.selectById(id);
    }

    public List<Product> getByIds(List<Integer> ids) throws ApiException{
        checkNull(ids,"Ids cannot be null");

        return productDao.selectByIds(ids);
    }

    public List<Product> getCheckByIds(List<Integer> productIds) throws ApiException {
        checkNull(productIds, "Product IDs list cannot be null");
        
        if (productIds.isEmpty()) {
            throw new ApiException("Product IDs list cannot be empty");
        }
        
        List<Product> products = productDao.selectByIds(productIds);
        
        // Check if all productIds have corresponding products
        Set<Integer> foundProductIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        
        for (Integer productId : productIds) {
            if (!foundProductIds.contains(productId)) {
                throw new ApiException("Product doesn't exist with id " + productId);
            }
        }
        
        return products;
    }

    public Product getCheckById(Integer id) throws ApiException {
        checkNull(id, "Id cannot be null");

        Product existingProduct = productDao.selectById(id);
        checkNull(existingProduct, "Product " + id + " doesn't exist");

        return existingProduct;
    }

    public Product getCheckByBarcode(String barcode) throws ApiException {
        checkNull(barcode, "Barcode cannot be null");

        Product existingProduct = productDao.selectByBarcode(barcode);
        checkNull(existingProduct, "Product with barcode " + barcode + " doesn't exist");

        return existingProduct;
    }

    public Product getByBarcode(String barcode) throws ApiException {
        checkNull(barcode, "Barcode cannot be null");

        return productDao.selectByBarcode(barcode);
    }

    public List<Product> getByBarcodes(List<String> barcodes) throws ApiException{
        checkNull(barcodes,"Barcodes cannot be null");

        return productDao.selectByBarcodes(barcodes);
    }

    public Product updateById(Integer id, Product product) throws ApiException {
        checkNull(id, "Id cannot be null");
        checkNull(product, "Product object cannot be null");

        Product existingProduct = productDao.selectById(id);
        checkNull(existingProduct, "Product doesn't exist");

        if (!Objects.equals(existingProduct.getClientId(), product.getClientId())) {
            throw new ApiException("Client id of a product can't be changed");
        }

        if (!existingProduct.getBarcode().equals(product.getBarcode())) {
            Product productWithSameBarcode = productDao.selectByBarcode(product.getBarcode());
            checkNotNull(productWithSameBarcode,"Another product with the barcode '" + product.getBarcode() + "' already exists.");
        }

        existingProduct.setBarcode(product.getBarcode());
        existingProduct.setName(product.getName());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setMrp(product.getMrp());
        existingProduct.setImageUrl(product.getImageUrl());

        productDao.update(existingProduct);
        return existingProduct;
    }

    public ProductUploadResult upload(List<ProductUploadRow> candidateRows, Map<String, Client> clientMap, Set<String> existingBarcodesInDb) {

        List<Product> productsToInsert = new ArrayList<>();
        List<FailedUploadRow> failedRows = new ArrayList<>();

        // --- THE FIX: Step 1 - Pre-processing Pass to Find In-File Duplicates ---
        // We group the rows by their barcode and count the occurrences of each.
        Set<String> duplicateBarcodesInFile = ProductUtil.findDuplicateBarcodesInFile(candidateRows);

        // --- Step 2: Main Processing Pass ---
        for (ProductUploadRow row : candidateRows) {
            try {
                String normalizedBarcode = row.getBarcode().trim().toLowerCase();

                // THE FIX: The first check is now against our "blacklist" of duplicates.
                if (duplicateBarcodesInFile.contains(normalizedBarcode)) {
                    throw new ApiException("Duplicate barcode '" + row.getBarcode() + "' found within the file. All entries with this barcode are rejected.");
                }

                // If the barcode is unique within the file, proceed with existing validation.
                Product product = ProductUtil.validateAndConvert(row, clientMap, existingBarcodesInDb);
                productsToInsert.add(product);

            } catch (ApiException e) {
                FailedUploadRow fail = new FailedUploadRow();
                fail.setRow(row);
                fail.setErrorMessage(e.getMessage());
                failedRows.add(fail);
            }
        }

        // --- Step 3: High-Performance Bulk Insert ---
        if (!productsToInsert.isEmpty()) {
            productDao.insertAll(productsToInsert);
        }

        ProductUploadResult result = new ProductUploadResult();
        result.setSuccessfullyInserted(productsToInsert);
        result.setFailedRows(failedRows);
        return result;
    }

    public void deleteById(Integer id) throws ApiException {
        checkNull(id, "Id cannot be null");

        Product existingProduct = productDao.selectById(id);
        checkNull(existingProduct, "Product " + id + " doesn't exist");
        productDao.deleteById(id);
    }
}