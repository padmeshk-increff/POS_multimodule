package com.increff.pos.api;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.entity.Client;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.utils.ClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = ApiException.class)
public class ClientApi extends AbstractApi{
    
    @Autowired
    private ClientDao clientDao;

    public Client insert(Client client) throws ApiException {
        checkNull(client,"Client object cannot be null");

        Client existingClient = clientDao.selectByName(client.getClientName());
        checkNotNull(existingClient,"Client already exists");

        clientDao.insert(client);
        return client;
    }

    public Client getCheckById(Integer id) throws ApiException{
        checkNull(id,"Id cannot be null");

        Client existingClient = clientDao.selectById(id);
        checkNull(existingClient,"Client "+ id+ " doesn't exist");

        return existingClient;
    }

    public Client getById(Integer id) throws ApiException {
        checkNull(id,"Id cannot be null");

        return clientDao.selectById(id);
    }

    public Client getCheckByName(String clientName) throws ApiException{
        checkNull(clientName,"Client name cannot be null");

        Client existingClient = clientDao.selectByName(clientName);
        checkNull(existingClient,"Client doesn't exist");

        return existingClient;
    }

    public Client getByName(String clientName) throws ApiException{
        checkNull(clientName,"Client name cannot be null");

        return clientDao.selectByName(clientName);
    }

    public List<Client> getByNames(List<String> clientNames) throws ApiException{
        checkNull(clientNames,"Client names cannot be null");

        return clientDao.selectByNames(clientNames);
    }

    public PaginatedResult<Client> getFilteredClients(String clientName, Pageable pageable) throws ApiException {
        checkNull(pageable, "Pageable object cannot be null");

        Long totalElements = clientDao.countWithFilters(clientName);

        if (totalElements == 0) {
            return ClientUtil.createEmptyResult();
        }

        List<Client> results = clientDao.selectWithFilters(clientName, pageable);

        PaginatedResult<Client> paginatedResult = new PaginatedResult<>();
        paginatedResult.setResults(results);
        paginatedResult.setTotalElements(totalElements);

        if (pageable.getPageSize() > 0) {
            paginatedResult.setTotalPages((int) Math.ceil((double) totalElements / pageable.getPageSize()));
        } else {
            paginatedResult.setTotalPages(1);
        }

        return paginatedResult;
    }

    public Client updateById(Integer id,Client client) throws ApiException{
        checkNull(client,"Client object cannot be null");
        checkNull(id,"Id cannot be null");

        Client existingClient = clientDao.selectById(id);
        checkNull(existingClient,"Client "+id+" doesn't exist");

        Client duplicateClient = clientDao.selectByName(client.getClientName());
        checkNotNull(duplicateClient,"Client "+client.getClientName()+" already exists");

        existingClient.setClientName(client.getClientName());

        clientDao.update(existingClient);
        return existingClient;
    }

}