package com.increff.pos.helper;

import com.increff.pos.entity.Client;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.form.ClientForm;
import com.increff.pos.model.result.PaginatedResult;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-30T08:41:21+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 1.8.0_462 (Amazon.com Inc.)"
)
@Component
public class ClientMapperImpl implements ClientMapper {

    @Override
    public Client convert(ClientForm clientForm) {
        if ( clientForm == null ) {
            return null;
        }

        Client client = new Client();

        client.setClientName( clientForm.getClientName() );

        return client;
    }

    @Override
    public ClientData convert(Client client) {
        if ( client == null ) {
            return null;
        }

        ClientData clientData = new ClientData();

        clientData.setId( client.getId() );
        clientData.setClientName( client.getClientName() );

        return clientData;
    }

    @Override
    public List<ClientData> convert(List<Client> clients) {
        if ( clients == null ) {
            return null;
        }

        List<ClientData> list = new ArrayList<ClientData>( clients.size() );
        for ( Client client : clients ) {
            list.add( convert( client ) );
        }

        return list;
    }

    @Override
    public PaginationData<ClientData> convert(PaginatedResult<Client> paginatedResult) {
        if ( paginatedResult == null ) {
            return null;
        }

        PaginationData<ClientData> paginationData = new PaginationData<ClientData>();

        paginationData.setContent( convert( paginatedResult.getResults() ) );
        paginationData.setTotalPages( paginatedResult.getTotalPages() );
        paginationData.setTotalElements( paginatedResult.getTotalElements() );

        return paginationData;
    }
}
