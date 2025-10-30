package com.increff.pos.unit.api;

import com.increff.pos.api.ClientApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dao.ClientDao;
import com.increff.pos.entity.Client;
import com.increff.pos.model.result.PaginatedResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.increff.pos.factory.ClientFactory.mockNewObject;
import static com.increff.pos.factory.ClientFactory.mockPersistedObject;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ClientApi class.
 * This class mocks the ClientDao to isolate and test all business logic,
 * validation, and edge cases based on the provided API's logic.
 */
public class ClientApiTest {

    @Mock
    private ClientDao clientDao;

    @InjectMocks
    private ClientApi clientApi; // The API class we are testing

    @Before
    public void setUp() {
        // Initializes all @Mock and @InjectMocks fields
        MockitoAnnotations.openMocks(this);
    }

    // --- insert() Tests ---

    @Test
    public void insert_validClient_shouldSucceed() throws ApiException {
        // Given
        Client newClient = mockNewObject("new-client");
        // Mock: No duplicate name found
        when(clientDao.selectByName("new-client")).thenReturn(null);

        // When
        Client savedClient = clientApi.insert(newClient);

        // Then
        assertNotNull(savedClient);
        assertEquals("new-client", savedClient.getClientName());
        // Verify DAO interactions
        verify(clientDao, times(1)).selectByName("new-client");
        verify(clientDao, times(1)).insert(newClient);
    }

    @Test
    public void insert_nullClient_shouldThrowApiException() {
        // Test: checkNull(client, ...)
        try {
            clientApi.insert(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client object cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectByName(anyString());
        verify(clientDao, never()).insert(any(Client.class));
    }

    @Test
    public void insert_clientWithId_shouldThrowApiException() {
        // Test: checkNotNull(client.getId(), ...)
        Client clientWithId = mockPersistedObject(1, "test-client");
        try {
            clientApi.insert(clientWithId);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Cannot insert with a pre-existing ID. ID must be null.", e.getMessage());
        }
        verify(clientDao, never()).selectByName(anyString());
        verify(clientDao, never()).insert(any(Client.class));
    }

    @Test
    public void insert_duplicateName_shouldThrowApiException() {
        // Test: checkNotNull(existingClient, ...)
        Client newClient = mockNewObject("duplicate-name");
        Client existingClient = mockPersistedObject(1, "duplicate-name");
        // Mock: Duplicate name IS found
        when(clientDao.selectByName("duplicate-name")).thenReturn(existingClient);

        try {
            clientApi.insert(newClient);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client already exists", e.getMessage());
        }

        verify(clientDao, times(1)).selectByName("duplicate-name");
        verify(clientDao, never()).insert(any(Client.class));
    }

    // --- getCheckById() Tests ---

    @Test
    public void getCheckById_existingId_shouldReturnClient() throws ApiException {
        // Given
        Integer clientId = 1;
        Client expectedClient = mockPersistedObject(clientId, "test-client");
        when(clientDao.selectById(clientId)).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getCheckById(clientId);

        // Then
        assertNotNull(actualClient);
        assertEquals(clientId, actualClient.getId());
        verify(clientDao, times(1)).selectById(clientId);
    }

    @Test
    public void getCheckById_nullId_shouldThrowApiException() {
        // Test: checkNull(id, ...)
        try {
            clientApi.getCheckById(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Id cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectById(anyInt());
    }

    @Test
    public void getCheckById_nonExistentId_shouldThrowApiException() {
        // Test: checkNull(existingClient, ...)
        Integer clientId = 999;
        when(clientDao.selectById(clientId)).thenReturn(null);

        try {
            clientApi.getCheckById(clientId);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client 999 doesn't exist", e.getMessage());
        }
        verify(clientDao, times(1)).selectById(clientId);
    }

    // --- getById() Tests ---

    @Test
    public void getById_existingId_shouldReturnClient() throws ApiException {
        // Given
        Client expectedClient = mockPersistedObject(1);
        when(clientDao.selectById(1)).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getById(1);

        // Then
        assertNotNull(actualClient);
        verify(clientDao, times(1)).selectById(1);
    }

    @Test
    public void getById_nonExistentId_shouldReturnNull() throws ApiException {
        // Test: This method should return null, not throw
        Integer clientId = 999;
        when(clientDao.selectById(clientId)).thenReturn(null);

        // When
        Client actualClient = clientApi.getById(clientId);

        // Then
        assertNull(actualClient); // Assert that it is null
        verify(clientDao, times(1)).selectById(clientId);
    }

    @Test
    public void getById_nullId_shouldThrowApiException() {
        // Test: checkNull(id, ...)
        try {
            clientApi.getById(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Id cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectById(anyInt());
    }

    // --- getByIds() Tests ---

    @Test
    public void getByIds_validList_shouldReturnList() throws ApiException {
        // Given
        List<Integer> ids = Arrays.asList(1, 2);
        List<Client> clientList = Arrays.asList(mockPersistedObject(1), mockPersistedObject(2));
        when(clientDao.selectByIds(ids)).thenReturn(clientList);

        // When
        List<Client> result = clientApi.getByIds(ids);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(clientDao, times(1)).selectByIds(ids);
    }

    @Test
    public void getByIds_nullList_shouldThrowApiException() {
        // Test: checkNull(ids, ...)
        try {
            clientApi.getByIds(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Ids cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectByIds(anyList());
    }

    @Test
    public void getByIds_emptyList_shouldReturnEmptyList() throws ApiException {
        // Test: The DAO should be called with an empty list
        List<Integer> ids = Collections.emptyList();
        when(clientDao.selectByIds(ids)).thenReturn(Collections.emptyList());

        // When
        List<Client> result = clientApi.getByIds(ids);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // Verify DAO is still called
        verify(clientDao, times(1)).selectByIds(ids);
    }

    // --- getCheckByName() Tests ---

    @Test
    public void getCheckByName_existingName_shouldReturnClient() throws ApiException {
        // Given
        Client expectedClient = mockPersistedObject(1, "test-client");
        when(clientDao.selectByName("test-client")).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getCheckByName("test-client");

        // Then
        assertNotNull(actualClient);
        verify(clientDao, times(1)).selectByName("test-client");
    }

    @Test
    public void getCheckByName_nullName_shouldThrowApiException() {
        // Test: checkNull(clientName, ...)
        try {
            clientApi.getCheckByName(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client name cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectByName(anyString());
    }

    @Test
    public void getCheckByName_nonExistentName_shouldThrowApiException() {
        // Test: checkNull(existingClient, ...)
        when(clientDao.selectByName("fake-client")).thenReturn(null);
        try {
            clientApi.getCheckByName("fake-client");
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client doesn't exist", e.getMessage());
        }
        verify(clientDao, times(1)).selectByName("fake-client");
    }

    // --- getByName() Tests ---

    @Test
    public void getByName_existingName_shouldReturnClient() throws ApiException {
        // Given
        Client expectedClient = mockPersistedObject(1, "test-client");
        when(clientDao.selectByName("test-client")).thenReturn(expectedClient);

        // When
        Client actualClient = clientApi.getByName("test-client");

        // Then
        assertNotNull(actualClient);
        verify(clientDao, times(1)).selectByName("test-client");
    }

    @Test
    public void getByName_nonExistentName_shouldReturnNull() throws ApiException {
        // Test: This method should return null, not throw
        when(clientDao.selectByName("fake-client")).thenReturn(null);

        // When
        Client actualClient = clientApi.getByName("fake-client");

        // Then
        assertNull(actualClient); // Assert that it is null
        verify(clientDao, times(1)).selectByName("fake-client");
    }

    @Test
    public void getByName_nullName_shouldThrowApiException() {
        // Test: checkNull(clientName, ...)
        try {
            clientApi.getByName(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client name cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectByName(anyString());
    }

    // --- getByNames() Tests ---

    @Test
    public void getByNames_validList_shouldReturnList() throws ApiException {
        // Given
        List<String> names = Arrays.asList("client1", "client2");
        List<Client> clientList = Arrays.asList(mockPersistedObject(1), mockPersistedObject(2));
        when(clientDao.selectByNames(names)).thenReturn(clientList);

        // When
        List<Client> result = clientApi.getByNames(names);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(clientDao, times(1)).selectByNames(names);
    }

    @Test
    public void getByNames_nullList_shouldThrowApiException() {
        // Test: checkNull(clientNames, ...)
        try {
            clientApi.getByNames(null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client names cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectByNames(anyList());
    }

    // --- getFilteredClients() Tests ---

    @Test
    public void getFilteredClients_clientsFound_shouldReturnPaginatedResult() throws ApiException {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        String clientNameFilter = "test";
        List<Client> clientList = Collections.singletonList(mockPersistedObject(1, "test-client"));

        when(clientDao.countWithFilters(clientNameFilter)).thenReturn(10L);
        when(clientDao.selectWithFilters(clientNameFilter, pageable)).thenReturn(clientList);

        // When
        PaginatedResult<Client> result = clientApi.getFilteredClients(clientNameFilter, pageable);

        // Then
        assertNotNull(result);
        assertEquals(10L, (long) result.getTotalElements());
        assertEquals(2, (int) result.getTotalPages());
        assertEquals(1, result.getResults().size());
        verify(clientDao, times(1)).countWithFilters(clientNameFilter);
        verify(clientDao, times(1)).selectWithFilters(clientNameFilter, pageable);
    }

    @Test
    public void getFilteredClients_noClientsFound_shouldReturnEmptyResult() throws ApiException {
        // Test: if (totalElements == 0) ...
        Pageable pageable = PageRequest.of(0, 5);
        when(clientDao.countWithFilters(null)).thenReturn(0L);

        // When
        PaginatedResult<Client> result = clientApi.getFilteredClients(null, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0L, (long) result.getTotalElements());
        // We assume ClientUtil.createEmptyResult() correctly sets this to 0
        assertEquals(0, (int) result.getTotalPages());
        assertTrue(result.getResults().isEmpty());
        // Verify the optimization: selectWithFilters is *not* called
        verify(clientDao, times(1)).countWithFilters(null);
        verify(clientDao, never()).selectWithFilters(any(), any());
    }

    @Test
    public void getFilteredClients_nullPageable_shouldThrowApiException() {
        // Test: checkNull(pageable, ...)
        try {
            clientApi.getFilteredClients("test", null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Pageable object cannot be null", e.getMessage());
        }
        verify(clientDao, never()).countWithFilters(anyString());
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_validUpdate_shouldSucceed() throws ApiException {
        // Given
        Integer clientId = 1;
        Client clientUpdates = mockNewObject("updated-name");
        Client existingClient = mockPersistedObject(clientId, "original-name");

        when(clientDao.selectById(clientId)).thenReturn(existingClient);
        when(clientDao.selectByName("updated-name")).thenReturn(null); // No duplicate

        // When
        Client updatedClient = clientApi.updateById(clientId, clientUpdates);

        // Then
        assertNotNull(updatedClient);
        assertEquals(clientId, updatedClient.getId());
        assertEquals("updated-name", updatedClient.getClientName()); // Name is updated
        verify(clientDao, times(1)).selectById(clientId);
        verify(clientDao, times(1)).selectByName("updated-name");
        verify(clientDao, times(1)).update(existingClient);
    }

    @Test
    public void updateById_clientNotFound_shouldThrowApiException() {
        // Test: checkNull(existingClient, ...)
        Integer clientId = 999;
        Client clientUpdates = mockNewObject("updated-name");
        when(clientDao.selectById(clientId)).thenReturn(null);

        // When & Then
        try {
            clientApi.updateById(clientId, clientUpdates);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client 999 doesn't exist", e.getMessage());
        }

        verify(clientDao, times(1)).selectById(clientId);
        verify(clientDao, never()).selectByName(anyString());
        verify(clientDao, never()).update(any(Client.class));
    }

    @Test
    public void updateById_duplicateName_shouldThrowApiException() {
        // Test: checkNotNull(duplicateClient, ...)
        Integer clientId = 1;
        Client clientUpdates = mockNewObject("duplicate-name");
        Client existingClient = mockPersistedObject(clientId, "original-name");

        // This is the *other* client that already has the name
        Client duplicateClient = mockPersistedObject(2, "duplicate-name");

        when(clientDao.selectById(clientId)).thenReturn(existingClient);
        when(clientDao.selectByName("duplicate-name")).thenReturn(duplicateClient);

        // When & Then
        try {
            clientApi.updateById(clientId, clientUpdates);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client duplicate-name already exists", e.getMessage());
        }

        verify(clientDao, times(1)).selectById(clientId);
        verify(clientDao, times(1)).selectByName("duplicate-name");
        verify(clientDao, never()).update(any(Client.class));
    }

    @Test
    public void updateById_sameName_shouldThrowApiException() {
        // Test: This tests the "bug" in the provided API logic.
        Integer clientId = 1;
        Client clientUpdates = mockNewObject("same-name");
        Client existingClient = mockPersistedObject(clientId, "same-name");

        when(clientDao.selectById(clientId)).thenReturn(existingClient);
        // The duplicate check finds the *existing client itself*
        when(clientDao.selectByName("same-name")).thenReturn(existingClient);

        // When & Then
        try {
            clientApi.updateById(clientId, clientUpdates);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            // The API's logic (checkNotNull) incorrectly identifies the client as a duplicate
            assertEquals("Client same-name already exists", e.getMessage());
        }

        verify(clientDao, times(1)).selectById(clientId);
        verify(clientDao, times(1)).selectByName("same-name");
        verify(clientDao, never()).update(any(Client.class));
    }

    @Test
    public void updateById_nullClient_shouldThrowApiException() {
        // Test: checkNull(client, ...)
        try {
            clientApi.updateById(1, null);
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Client object cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectById(anyInt());
    }

    @Test
    public void updateById_nullId_shouldThrowApiException() {
        // Test: checkNull(id, ...)
        try {
            clientApi.updateById(null, mockNewObject());
            fail("Should have thrown ApiException");
        } catch (ApiException e) {
            assertEquals("Id cannot be null", e.getMessage());
        }
        verify(clientDao, never()).selectById(anyInt());
    }
}