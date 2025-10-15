package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.ClientDto;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
public class ClientController {

    @Autowired
    private ClientDto clientDto;

    @RequestMapping(value = "/clients", method = RequestMethod.POST)
    public ClientData addClient(@Valid @RequestBody ClientForm form) throws ApiException {
        return clientDto.add(form);
    }

    @RequestMapping(value = "/clients", method = RequestMethod.GET)
    public List<ClientData> getAll(){
        return clientDto.getAll();
    }

    @RequestMapping(value = "/clients/{id}", method = RequestMethod.GET)
    public ClientData getById(@PathVariable(value = "id") Integer id) throws ApiException{
        return clientDto.getById(id);
    }

    @RequestMapping(value="/clients/{id}",method = RequestMethod.PUT)
    public ClientData updateById(@PathVariable(name="id") Integer id, @RequestBody ClientForm form) throws ApiException{
        return clientDto.updateById(id,form);
    }

}
