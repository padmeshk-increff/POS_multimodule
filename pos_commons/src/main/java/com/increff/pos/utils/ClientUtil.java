package com.increff.pos.utils;

import com.increff.pos.entity.Client;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;

import java.util.ArrayList;
import java.util.List;

public class ClientUtil{

    public static Client convert(ClientForm clientForm){
        Client clientPojo = new Client();
        clientPojo.setClientName(clientForm.getClientName());
        return clientPojo;
    }

    public static List<ClientData> convert(List<Client> clientsPojo){
        List<ClientData> clientsData = new ArrayList<>();
        for(Client clientPojo:clientsPojo){
            clientsData.add(convert(clientPojo));
        }
        return clientsData;
    }

    public static ClientData convert(Client clientPojo){
        ClientData clientData = new ClientData();
        clientData.setId(clientPojo.getId());
        clientData.setClientName(clientPojo.getClientName());
        return clientData;
    }

}
