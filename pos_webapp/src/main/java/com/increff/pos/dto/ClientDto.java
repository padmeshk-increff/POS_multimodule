package com.increff.pos.dto;

import com.increff.pos.api.ClientApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Client;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.utils.ClientUtil;
import com.increff.pos.utils.NormalizeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientDto {

    @Autowired
    private ClientApi clientApi;

    public ClientData add(ClientForm clientForm) throws ApiException {
        NormalizeUtil.normalize(clientForm);

        Client clientPojo = ClientUtil.convert(clientForm);
        clientApi.insert(clientPojo);

        return ClientUtil.convert(clientPojo);
    }

    public List<ClientData> getAll(){
        List<Client> clientsPojo = clientApi.getAll();

        return ClientUtil.convert(clientsPojo);
    }

    public ClientData getById(Integer id) throws ApiException{
        Client clientPojo = clientApi.getCheckById(id);

        return ClientUtil.convert(clientPojo);
    }

    public ClientData updateById(Integer id,ClientForm clientForm) throws ApiException{
        NormalizeUtil.normalize(clientForm);

        Client clientPojo = ClientUtil.convert(clientForm);
        Client updatedClientPojo = clientApi.updateById(id,clientPojo);

        return ClientUtil.convert(updatedClientPojo);
    }

}
