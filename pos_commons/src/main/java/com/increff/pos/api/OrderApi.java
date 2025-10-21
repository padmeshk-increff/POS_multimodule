package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.OrderDao;
import com.increff.pos.entity.Order;
import com.increff.pos.model.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class OrderApi extends AbstractApi{

    @Autowired
    private OrderDao orderDao;

    public Order insert(Order order) throws ApiException{
        checkNull(order,"Order object cannot be null");

        orderDao.insert(order);

        return order;
    }

    public List<Order> getAll(){
        return orderDao.selectAll();
    }

    public Order getCheckById(Integer id) throws ApiException{
        checkNull(id,"Id cannot null");

        Order existingOrder = orderDao.selectById(id);
        checkNull(existingOrder,"Order "+id + " doesn't exist");

        return existingOrder;
    }

    public Order updateInvoicePathById(Integer id, String filePath) throws ApiException{
        checkNull(id,"Id cannot be null");
        checkNull(filePath,"File Path cannot be null");

        Order existingOrder = orderDao.selectById(id);
        checkNull(existingOrder,"Order "+id+" doesn't exist");

        existingOrder.setInvoicePath(filePath);
        orderDao.update(existingOrder);
        return existingOrder;
    }

    public Order updateById(Integer id,Order order) throws ApiException{
        checkNull(id,"Id cannot be null");
        checkNull(order,"Order object cannot be null");

        Order existingOrder = orderDao.selectById(id);
        checkNull(existingOrder,"Order "+id+" doesn't exist");

        if(existingOrder.getOrderStatus() == OrderStatus.INVOICED){
            throw new ApiException("Cannot update an order that has already been invoiced");
        }

        if(existingOrder.getOrderStatus() == OrderStatus.CANCELLED){
            throw new ApiException("Cannot update an order that as been cancelled");
        }

        existingOrder.setOrderStatus(order.getOrderStatus());
        if(order.getCustomerName() != null)existingOrder.setCustomerName(order.getCustomerName());
        if(order.getCustomerPhone() != null)existingOrder.setCustomerPhone(order.getCustomerPhone());

        orderDao.update(existingOrder);
        return existingOrder;
    }

    public Order updateInvoiceOrder(Integer id) throws ApiException {
        Order order = getCheckById(id);

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new ApiException("Only an order with status CREATED can be invoiced. Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.INVOICED);
        return order;
    }

    public void updateAmountById(Integer orderId,Double oldSP,Double newSP) throws ApiException{
        checkNull(orderId,"Order id cannot be null");
        checkNull(oldSP,"Old SP can't be null");
        checkNull(newSP,"New SP cannot be null");

        Order existingOrder = getCheckById(orderId);
        Double priceAddOn = newSP-oldSP;
        Double updatedAmount = existingOrder.getTotalAmount() + priceAddOn;

        existingOrder.setTotalAmount(updatedAmount);
        orderDao.update(existingOrder);
    }

    public void deleteById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        Order existingOrder = orderDao.selectById(id);
        checkNull(existingOrder,"Order doesn't exist");

        orderDao.deleteById(id);
    }

    public List<Order> getByFilters(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status, Pageable pageable) throws ApiException{
        checkNull(pageable,"Pageable cannot be null");

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException("Start date cannot be after end date.");
        }

        return orderDao.findWithFilters(startDate,endDate,status,pageable);
    }

    public Long countWithFilters(ZonedDateTime startDate, ZonedDateTime endDate, OrderStatus status) throws ApiException {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException("Start date cannot be after end date.");
        }

        return orderDao.countWithFilters(startDate, endDate, status);
    }
}
