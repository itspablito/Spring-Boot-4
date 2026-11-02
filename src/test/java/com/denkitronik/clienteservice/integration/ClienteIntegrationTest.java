package com.denkitronik.clienteservice.integration;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;

import org.junit.jupiter.api.Order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("E2E tests — flujo completo HTTP → PostgreSQL con Testcontainers")
class ClienteIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String BASE = "/api/v1/cliente-service";

    private static final AtomicBoolean seeded = new AtomicBoolean(false);

    private static Long regionId;
    private static Long idCreado;

    @BeforeEach
    void seedDatosIniciales() {
        if (seeded.compareAndSet(false, true)) {
            jdbcTemplate.execute(
                    "INSERT INTO regiones(nombre) VALUES ('América del Sur')");

            regionId = jdbcTemplate.queryForObject(
                    "SELECT id FROM regiones WHERE nombre = 'América del Sur'",
                    Long.class);
        }
    }

    // TEST 1: POST - Crear cliente
    @Test
    @Order(1)
    @DisplayName("POST /clientes → 201 crea el cliente y retorna el ID")
    void crearCliente_debeRetornar201() {

        Region region = new Region();
        region.setId(regionId);

        Cliente nuevo = new Cliente();
        nuevo.setNombre("Margaret");
        nuevo.setApellido("Hamilton");
        nuevo.setEmail("margaret@apollo.nasa");
        nuevo.setRegion(region);

        ResponseEntity<Cliente> respuesta =
                restTemplate.postForEntity(
                        BASE + "/clientes", nuevo, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getId()).isNotNull();
        assertThat(respuesta.getBody().getNombre()).isEqualTo("Margaret");

        idCreado = respuesta.getBody().getId();
    }

    // TEST 2: GET - Buscar cliente por ID
    @Test
    @Order(2)
    @DisplayName("GET /clientes/{id} → 200 encuentra el cliente creado")
    void buscarClienteCreado_debeRetornar200() {

        ResponseEntity<Cliente> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/" + idCreado, Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getEmail())
                .isEqualTo("margaret@apollo.nasa");
    }

    // TEST 3: GET - Listar todos los clientes
    @Test
    @Order(3)
    @DisplayName("GET /clientes → 200 lista todos los clientes")
    void listarClientes_debeRetornar200() {

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("Margaret");
    }

    // TEST 4: PUT - Actualizar cliente
    @Test
    @Order(4)
    @DisplayName("PUT /clientes/{id} → 201 actualiza los datos")
    void actualizarCliente_debeRetornar201() {

        Region region = new Region();
        region.setId(regionId);

        Cliente actualizado = new Cliente();
        actualizado.setNombre("Margaret");
        actualizado.setApellido("Hamilton-Updated");
        actualizado.setEmail("margaret@apollo.nasa");
        actualizado.setRegion(region);

        HttpEntity<Cliente> entity =
                new HttpEntity<>(actualizado, headersJson());

        ResponseEntity<Cliente> respuesta =
                restTemplate.exchange(
                        BASE + "/clientes/" + idCreado,
                        HttpMethod.PUT,
                        entity,
                        Cliente.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getApellido())
                .isEqualTo("Hamilton-Updated");
    }

    // TEST 5: GET - Verificar ID inexistente
    @Test
    @Order(5)
    @DisplayName("GET /clientes/9999 → 404 ID inexistente")
    void idInexistente_debeRetornar404() {

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/9999", String.class);

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // TEST 6: DELETE - Eliminar cliente
    @Test
    @Order(6)
    @DisplayName("DELETE /clientes/{id} → 204 elimina el cliente")
    void eliminarCliente_debeRetornar204() {

        ResponseEntity<Void> respuesta =
                restTemplate.exchange(
                        BASE + "/clientes/" + idCreado,
                        HttpMethod.DELETE,
                        null,
                        Void.class);

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    // TEST 7: GET - Confirmar que el cliente fue eliminado
    @Test
    @Order(7)
    @DisplayName("GET /clientes/{id} → 404 confirma que fue eliminado")
    void clienteEliminado_debeRetornar404() {

        ResponseEntity<String> respuesta =
                restTemplate.getForEntity(
                        BASE + "/clientes/" + idCreado, String.class);

        assertThat(respuesta.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders headersJson() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}