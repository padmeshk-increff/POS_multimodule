package com.increff.pos.helper;

import com.increff.pos.entity.Order;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.OrderData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.enums.OrderStatus;
import com.increff.pos.model.form.OrderForm;
import com.increff.pos.model.form.OrderUpdateForm;
import com.increff.pos.model.result.OrderResult;
import com.increff.pos.model.result.PaginatedResult;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-03T08:45:18+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public Order convert(OrderForm orderForm) {
        if ( orderForm == null ) {
            return null;
        }

        Order order = new Order();

        order.setCustomerName( orderForm.getCustomerName() );
        order.setCustomerPhone( orderForm.getCustomerPhone() );

        order.setOrderStatus( OrderStatus.CREATED );

        return order;
    }

    @Override
    public Order convert(OrderUpdateForm orderUpdateForm) {
        if ( orderUpdateForm == null ) {
            return null;
        }

        Order order = new Order();

        order.setOrderStatus( orderUpdateForm.getOrderStatus() );
        order.setCustomerName( orderUpdateForm.getCustomerName() );
        order.setCustomerPhone( orderUpdateForm.getCustomerPhone() );

        return order;
    }

    @Override
    public PaginationData<OrderData> convert(PaginatedResult<OrderResult> result, Map<Integer, Product> productMap) {
        if ( result == null ) {
            return null;
        }

        PaginationData<OrderData> paginationData = new PaginationData<OrderData>();

        paginationData.setContent( convert( result.getResults(), productMap ) );
        paginationData.setTotalPages( result.getTotalPages() );
        paginationData.setTotalElements( result.getTotalElements() );

        return paginationData;
    }

    @Override
    public List<OrderData> convert(List<OrderResult> orderResults, Map<Integer, Product> productMap) {
        if ( orderResults == null ) {
            return null;
        }

        List<OrderData> list = new ArrayList<OrderData>( orderResults.size() );
        for ( OrderResult orderResult : orderResults ) {
            list.add( convert( orderResult, productMap ) );
        }

        return list;
    }

    @Override
    public OrderData convert(OrderResult orderResult, Map<Integer, Product> productMap) {
        if ( orderResult == null ) {
            return null;
        }

        OrderData orderData = new OrderData();

        orderData.setId( orderResultOrderId( orderResult ) );
        orderData.setOrderStatus( orderResultOrderOrderStatus( orderResult ) );
        orderData.setCustomerName( orderResultOrderCustomerName( orderResult ) );
        orderData.setCustomerPhone( orderResultOrderCustomerPhone( orderResult ) );
        orderData.setTotalAmount( orderResultOrderTotalAmount( orderResult ) );
        orderData.setCreatedAt( orderResultOrderCreatedAt( orderResult ) );
        orderData.setOrderItemDataList( orderItemMapper.convert( orderResult.getOrderItems(), productMap ) );

        return orderData;
    }

    private Integer orderResultOrderId(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        Integer id = order.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private OrderStatus orderResultOrderOrderStatus(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        OrderStatus orderStatus = order.getOrderStatus();
        if ( orderStatus == null ) {
            return null;
        }
        return orderStatus;
    }

    private String orderResultOrderCustomerName(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        String customerName = order.getCustomerName();
        if ( customerName == null ) {
            return null;
        }
        return customerName;
    }

    private String orderResultOrderCustomerPhone(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        String customerPhone = order.getCustomerPhone();
        if ( customerPhone == null ) {
            return null;
        }
        return customerPhone;
    }

    private Double orderResultOrderTotalAmount(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        Double totalAmount = order.getTotalAmount();
        if ( totalAmount == null ) {
            return null;
        }
        return totalAmount;
    }

    private ZonedDateTime orderResultOrderCreatedAt(OrderResult orderResult) {
        if ( orderResult == null ) {
            return null;
        }
        Order order = orderResult.getOrder();
        if ( order == null ) {
            return null;
        }
        ZonedDateTime createdAt = order.getCreatedAt();
        if ( createdAt == null ) {
            return null;
        }
        return createdAt;
    }
}
