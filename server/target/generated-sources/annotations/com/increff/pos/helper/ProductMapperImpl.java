package com.increff.pos.helper;

import com.increff.pos.entity.Client;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.data.ProductData;
import com.increff.pos.model.form.ProductForm;
import com.increff.pos.model.result.PaginatedResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-30T08:41:21+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public Product convert(ProductForm productForm) {
        if ( productForm == null ) {
            return null;
        }

        Product product = new Product();

        product.setBarcode( productForm.getBarcode() );
        product.setName( productForm.getName() );
        product.setCategory( productForm.getCategory() );
        product.setMrp( productForm.getMrp() );
        product.setImageUrl( productForm.getImageUrl() );
        product.setClientId( productForm.getClientId() );

        return product;
    }

    @Override
    public ProductData convert(Product product, Client client, Inventory inventory) {
        if ( product == null && client == null && inventory == null ) {
            return null;
        }

        ProductData productData = new ProductData();

        if ( product != null ) {
            productData.setId( product.getId() );
            productData.setBarcode( product.getBarcode() );
            productData.setName( product.getName() );
            productData.setCategory( product.getCategory() );
            productData.setMrp( product.getMrp() );
            productData.setImageUrl( product.getImageUrl() );
            productData.setClientId( product.getClientId() );
        }
        if ( client != null ) {
            productData.setClientName( client.getClientName() );
        }
        if ( inventory != null ) {
            productData.setQuantity( inventory.getQuantity() );
        }

        return productData;
    }

    @Override
    public PaginationData<ProductData> convert(PaginatedResult<Product> paginatedResult, Map<Integer, Client> clientMap, Map<Integer, Inventory> inventoryMap) {
        if ( paginatedResult == null ) {
            return null;
        }

        PaginationData<ProductData> paginationData = new PaginationData<ProductData>();

        paginationData.setContent( convert( paginatedResult.getResults(), clientMap, inventoryMap ) );
        paginationData.setTotalPages( paginatedResult.getTotalPages() );
        paginationData.setTotalElements( paginatedResult.getTotalElements() );

        return paginationData;
    }

    @Override
    public List<ProductData> convert(List<Product> products, Map<Integer, Client> clientMap, Map<Integer, Inventory> inventoryMap) {
        if ( products == null ) {
            return null;
        }

        List<ProductData> list = new ArrayList<ProductData>( products.size() );
        for ( Product product : products ) {
            list.add( convert( product, clientMap, inventoryMap ) );
        }

        return list;
    }

    @Override
    public ProductData convert(Product product, Map<Integer, Client> clientMap, Map<Integer, Inventory> inventoryMap) {
        if ( product == null ) {
            return null;
        }

        ProductData productData = new ProductData();

        productData.setClientName( mapClientName( product.getClientId(), clientMap ) );
        productData.setQuantity( mapQuantity( product.getId(), inventoryMap ) );
        productData.setId( product.getId() );
        productData.setBarcode( product.getBarcode() );
        productData.setName( product.getName() );
        productData.setCategory( product.getCategory() );
        productData.setMrp( product.getMrp() );
        productData.setImageUrl( product.getImageUrl() );
        productData.setClientId( product.getClientId() );

        return productData;
    }
}
