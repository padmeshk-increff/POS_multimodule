package com.increff.pos.utils;

import com.increff.pos.entity.Client;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.result.PaginatedResult;

import java.util.ArrayList;
import java.util.List;

public class ClientUtil extends BaseUtil{

    public static Client convert(ClientForm clientForm){
        Client clientPojo = new Client();
        clientPojo.setClientName(clientForm.getClientName());
        return clientPojo;
    }

    public static List<ClientData> convert(List<Client> clients){
        List<ClientData> clientsData = new ArrayList<>();
        for(Client clientPojo:clients){
            clientsData.add(convert(clientPojo));
        }
        return clientsData;
    }

    public static ClientData convert(Client client){
        ClientData clientData = new ClientData();
        clientData.setId(client.getId());
        clientData.setClientName(client.getClientName());
        return clientData;
    }

    public static PaginationData<ClientData> convert(PaginatedResult<Client> paginatedResult){
        PaginationData<ClientData> paginationData = new PaginationData<>();
        paginationData.setTotalElements(paginatedResult.getTotalElements());
        paginationData.setTotalPages(paginatedResult.getTotalPages());
        paginationData.setContent(convert(paginatedResult.getResults()));
        return paginationData;
    }
}
