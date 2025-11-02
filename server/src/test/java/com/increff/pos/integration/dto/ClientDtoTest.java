package com.increff.pos.integration.dto;

import com.increff.pos.config.SpringConfig;
import com.increff.pos.dto.ClientDto;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.model.data.ClientData;
import com.increff.pos.model.data.PaginationData;
import com.increff.pos.model.form.ClientForm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;

import static org.junit.Assert.*;

/**
 * Integration Tests for the ClientDto class.
 * This test file validates the functional contract of the DTO layer
 * by interacting with a real (test) database.
 * It is "loosely coupled" as it does not mock internals like the API or DAO.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
@TestPropertySource("classpath:test.properties")
@Transactional
public class ClientDtoTest {

    @Autowired
    private ClientDto clientDto;

    // --- Helper to create a form ---
    private ClientForm createClientForm(String name) {
        ClientForm form = new ClientForm();
        form.setClientName(name);
        return form;
    }

    // --- add() Tests ---

    @Test
    public void add_validClient_shouldSaveToDatabase() throws ApiException {
        // GIVEN
        ClientForm form = createClientForm("test-client");

        // WHEN
        ClientData createdClient = clientDto.add(form);

        // THEN
        // 1. Check the returned object
        assertNotNull(createdClient.getId());
        assertEquals("test-client", createdClient.getClientName());

        // 2. Verify by fetching it back from the DB
        ClientData fromDb = clientDto.getById(createdClient.getId());
        assertEquals("test-client", fromDb.getClientName());
    }

    @Test
    public void add_duplicateName_shouldThrowException() throws ApiException {
        // GIVEN
        // Insert the first client
        clientDto.add(createClientForm("duplicate-name"));

        // WHEN
        // Create a new form with the same name
        ClientForm duplicateForm = createClientForm("duplicate-name");

        // THEN
        // Assert that the API layer's validation throws the correct exception
        ApiException ex = assertThrows(ApiException.class,
                () -> clientDto.add(duplicateForm)
        );
        assertEquals("Client already exists", ex.getMessage());
    }

    @Test
    public void add_nullName_shouldThrowException() {
        // GIVEN
        ClientForm form = createClientForm(null);

        // WHEN / THEN
        // This fails at the DAO/DB level (NOT NULL constraint)
        // Spring wraps this in a PersistenceException or ConstraintViolationException
        assertThrows(PersistenceException.class,
                () -> clientDto.add(form)
        );
    }

    @Test
    public void add_blankName_shouldSucceed() throws ApiException {
        // GIVEN
        // The DTO layer's normalize() method will turn a blank name into an empty string.
        // The API/DAO layer does not have a check for this, so it will be inserted.
        ClientForm form = createClientForm("   ");

        // WHEN
        ClientData createdClient = clientDto.add(form);

        // THEN
        assertNotNull(createdClient.getId());
        assertEquals("", createdClient.getClientName()); // DTO normalize() trims to empty string
    }

    // --- getFilteredClients() Tests ---

    @Test
    public void getFilteredClients_withFilter_shouldReturnFilteredList() throws ApiException {
        // GIVEN
        clientDto.add(createClientForm("Apple Inc."));
        clientDto.add(createClientForm("Apricot Co."));
        clientDto.add(createClientForm("Banana Ltd."));

        // WHEN
        PaginationData<ClientData> result = clientDto.getFilteredClients("ap", 0, 5);

        // Log the result for debugging
        System.out.println("Result totalElements: " + result.getTotalElements());
        System.out.println("Result totalPages: " + result.getTotalPages());
        System.out.println("Result content size: " + result.getContent().size());
        for (ClientData data : result.getContent()) {
            System.out.println("Client: ID=" + data.getId() + ", Name=" + data.getClientName());
        }

        // THEN
        assertEquals(2L, (long) result.getTotalElements());
        assertEquals(1, (int) result.getTotalPages());
        assertEquals(2, result.getContent().size());
        assertEquals("apple inc.", result.getContent().get(0).getClientName());
        assertEquals("apricot co.", result.getContent().get(1).getClientName());
    }

    @Test
    public void getFilteredClients_noFilter_shouldReturnAll() throws ApiException {
        // GIVEN
        clientDto.add(createClientForm("Client A"));
        clientDto.add(createClientForm("Client B"));

        // WHEN
        PaginationData<ClientData> result = clientDto.getFilteredClients(null, 0, 5);

        // THEN
        assertEquals(2L, (long) result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    public void getFilteredClients_pagination_shouldWork() throws ApiException {
        // GIVEN
        // Insertion order matters for the default ID sorting
        ClientData clientA = clientDto.add(createClientForm("Client A"));
        ClientData clientB = clientDto.add(createClientForm("Client B"));
        ClientData clientC = clientDto.add(createClientForm("Client C"));

        // WHEN: Get Page 1 (index 1), Size 2. (Sort is by ID asc)
        PaginationData<ClientData> result = clientDto.getFilteredClients(null, 1, 2);

        // THEN
        assertEquals(3L, (long) result.getTotalElements());
        assertEquals(2, (int) result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals("client c", result.getContent().get(0).getClientName());
    }

    // --- getById() Tests ---

    @Test
    public void getById_nonExisting_shouldThrowException() {
        // GIVEN: An empty database

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> clientDto.getById(999)
        );
        assertEquals("Client 999 doesn't exist", ex.getMessage());
    }

    // --- updateById() Tests ---

    @Test
    public void updateById_valid_shouldUpdateInDatabase() throws ApiException {
        // GIVEN
        ClientData clientA = clientDto.add(createClientForm("Client A"));
        ClientForm updateForm = createClientForm("Client B");

        // WHEN
        ClientData updatedClient = clientDto.updateById(clientA.getId(), updateForm);

        // THEN
        assertEquals("client b", updatedClient.getClientName());

        // Verify by fetching from DB
        ClientData fromDb = clientDto.getById(clientA.getId());
        assertEquals("client b", fromDb.getClientName());
    }

    @Test
    public void updateById_nonExisting_shouldThrowException() {
        // GIVEN
        ClientForm updateForm = createClientForm("Client B");

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> clientDto.updateById(999, updateForm)
        );
        assertEquals("Client 999 doesn't exist", ex.getMessage());
    }

    @Test
    public void updateById_duplicateName_shouldThrowException() throws ApiException {
        // GIVEN
        ClientData clientA = clientDto.add(createClientForm("Client A"));
        clientDto.add(createClientForm("Client B")); // Client B already exists

        // WHEN
        ClientForm updateForm = createClientForm("Client B"); // Try to rename A to B

        // THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> clientDto.updateById(clientA.getId(), updateForm)
        );
        assertEquals("Client client b already exists", ex.getMessage());
    }

    @Test
    public void updateById_sameName_shouldThrowException() throws ApiException {
        // This test correctly identifies the bug in your API layer
        // GIVEN
        ClientData clientA = clientDto.add(createClientForm("Client A"));
        ClientForm updateForm = createClientForm("Client A"); // Same name

        // WHEN / THEN
        ApiException ex = assertThrows(ApiException.class,
                () -> clientDto.updateById(clientA.getId(), updateForm)
        );
        // This asserts the known (buggy) logic of the API layer
        assertEquals("Client client a already exists", ex.getMessage());
    }
}