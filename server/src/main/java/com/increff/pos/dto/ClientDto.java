package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.helper.ClientMapper;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ClientDto extends AbstractDto{

    @Autowired
    private ClientApi clientApi;

    @Autowired
    private ClientMapper clientMapper;

    public ClientData add(ClientForm clientForm) throws ApiException {
        ValidationUtil.validate(clientForm);
        normalize(clientForm,null);

        Client clientPojo = clientMapper.convert(clientForm);
        clientApi.insert(clientPojo);

        return clientMapper.convert(clientPojo);
    }

    public PaginationData<ClientData> getFilteredClients(String clientName, Integer page, Integer size) throws ApiException{
        Pageable pageable = PageRequest.of(page,size, Sort.by("id").ascending());

        PaginatedResult<Client> paginatedResult = clientApi.getFilteredClients(clientName,pageable);

        return clientMapper.convert(paginatedResult);
    }

    public ClientData getById(Integer id) throws ApiException{
        Client clientPojo = clientApi.getCheckById(id);

        return clientMapper.convert(clientPojo);
    }

    public ClientData updateById(Integer id,ClientForm clientForm) throws ApiException{
        ValidationUtil.validate(clientForm);
        normalize(clientForm,null);

        Client clientPojo = clientMapper.convert(clientForm);
        Client updatedClientPojo = clientApi.updateById(id,clientPojo);

        return clientMapper.convert(updatedClientPojo);
    }

}
