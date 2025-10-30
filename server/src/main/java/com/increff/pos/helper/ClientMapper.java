package com.increff.pos.helper;

import com.increff.pos.entity.Client;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.result.PaginatedResult;
import com.increff.pos.model.data.PaginationData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * This interface defines the contract for MapStruct to generate
 * conversion methods between Client-related objects.
 * It replaces the manual methods in ClientUtil.java.
 * componentModel = "spring" tells MapStruct to generate a Spring @Component
 * so you can @Autowired it directly in your Dto/Api/Flow classes.
 */
@Mapper(componentModel = "spring")
public interface ClientMapper {

    /**
     * Replaces ClientUtil.convert(ClientForm clientForm)
     */
    Client convert(ClientForm clientForm);

    /**
     * Replaces ClientUtil.convert(Client client)
     */
    ClientData convert(Client client);

    /**
     * Replaces ClientUtil.convert(List<Client> clients)
     * MapStruct automatically knows how to map a List if it
     * knows how to map a single item.
     */
    List<ClientData> convert(List<Client> clients);

    /**
     * Replaces ClientUtil.convert(PaginatedResult<Client> paginatedResult)
     * We use @Mapping to tell MapStruct that the field "results"
     * in PaginatedResult should be mapped to the field "content" in PaginationData.
     */
    @Mapping(source = "results", target = "content")
    PaginationData<ClientData> convert(PaginatedResult<Client> paginatedResult);
}

