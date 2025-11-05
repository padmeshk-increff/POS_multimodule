package com.increff.pos.helper;

import com.increff.pos.entity.OrderItem;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderItemData;
import com.increff.pos.model.form.OrderItemForm;
import com.increff.pos.model.form.OrderItemUpdateForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-05T10:07:47+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItem convert(OrderItemForm form, Integer orderId) {
        if ( form == null && orderId == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        if ( form != null ) {
            orderItem.setQuantity( form.getQuantity() );
            orderItem.setSellingPrice( form.getSellingPrice() );
            orderItem.setProductId( form.getProductId() );
        }
        orderItem.setOrderId( orderId );

        return orderItem;
    }

    @Override
    public List<OrderItem> convert(List<OrderItemForm> orderItemForms) {
        if ( orderItemForms == null ) {
            return null;
        }

        List<OrderItem> list = new ArrayList<OrderItem>( orderItemForms.size() );
        for ( OrderItemForm orderItemForm : orderItemForms ) {
            list.add( orderItemFormToOrderItem( orderItemForm ) );
        }

        return list;
    }

    @Override
    public OrderItem convert(OrderItemUpdateForm orderItemUpdateForm, Integer orderId, Integer itemId) {
        if ( orderItemUpdateForm == null && orderId == null && itemId == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        if ( orderItemUpdateForm != null ) {
            orderItem.setQuantity( orderItemUpdateForm.getQuantity() );
            orderItem.setSellingPrice( orderItemUpdateForm.getSellingPrice() );
        }
        orderItem.setOrderId( orderId );
        orderItem.setId( itemId );

        return orderItem;
    }

    @Override
    public OrderItemData convert(OrderItem orderItem, Product product) {
        if ( orderItem == null && product == null ) {
            return null;
        }

        OrderItemData orderItemData = new OrderItemData();

        if ( orderItem != null ) {
            orderItemData.setId( orderItem.getId() );
            orderItemData.setQuantity( orderItem.getQuantity() );
            orderItemData.setSellingPrice( orderItem.getSellingPrice() );
        }
        if ( product != null ) {
            orderItemData.setProductName( product.getName() );
        }

        return orderItemData;
    }

    @Override
    public List<OrderItemData> convert(List<OrderItem> items, Map<Integer, Product> productMap) {
        if ( items == null ) {
            return null;
        }

        List<OrderItemData> list = new ArrayList<OrderItemData>( items.size() );
        for ( OrderItem orderItem : items ) {
            list.add( convert( orderItem, productMap ) );
        }

        return list;
    }

    @Override
    public OrderItemData convert(OrderItem orderItem, Map<Integer, Product> productMap) {
        if ( orderItem == null ) {
            return null;
        }

        OrderItemData orderItemData = new OrderItemData();

        orderItemData.setProductName( mapProductName( orderItem, productMap ) );
        orderItemData.setId( orderItem.getId() );
        orderItemData.setQuantity( orderItem.getQuantity() );
        orderItemData.setSellingPrice( orderItem.getSellingPrice() );

        return orderItemData;
    }

    protected OrderItem orderItemFormToOrderItem(OrderItemForm orderItemForm) {
        if ( orderItemForm == null ) {
            return null;
        }

        OrderItem orderItem = new OrderItem();

        orderItem.setQuantity( orderItemForm.getQuantity() );
        orderItem.setSellingPrice( orderItemForm.getSellingPrice() );
        orderItem.setProductId( orderItemForm.getProductId() );

        return orderItem;
    }
}
