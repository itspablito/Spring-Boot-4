package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;      // Spring Boot 3.x
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;  // Spring Boot 4.x
// import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;     // Spring Boot 3.x
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager; // Spring Boot 4.x
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DisplayName("Integration tests (data slice) — IClienteDao con H2")
class IClienteDaoTest {

    @Autowired
    private TestEntityManager em;     // para preparar datos de prueba

    @Autowired
    private IClienteDao clienteDao;   // el repositorio que estamos probando

    private Region region;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        // Preparamos datos con TestEntityManager, no con el repositorio bajo prueba
        region = new Region();
        region.setNombre("Asia");
        em.persist(region);

        cliente = new Cliente();
        cliente.setNombre("Grace");
        cliente.setApellido("Hopper");
        cliente.setEmail("grace@navy.mil");
        cliente.setRegion(region);
        em.persist(cliente);

        em.flush();  // fuerza el INSERT antes de que los tests lean datos
    }

    @Test
    @DisplayName("findById — ID existente → devuelve Optional con cliente")
    void findById_idExistente_debeRetornarCliente() {
        Optional<Cliente> resultado = clienteDao.findById(cliente.getId());

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Grace");
        assertThat(resultado.get().getEmail()).isEqualTo("grace@navy.mil");
    }

    @Test
    @DisplayName("findAll — devuelve todos los clientes persistidos")
    void findAll_debeRetornarListaDeClientes() {
        List<Cliente> clientes = clienteDao.findAll();

        assertThat(clientes).isNotEmpty().hasSize(1);
        assertThat(clientes.get(0).getApellido()).isEqualTo("Hopper");
    }

    @Test
    @DisplayName("save — guarda nuevo cliente y genera ID")
    void save_debePersistirClienteYGenerarId() {
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setNombre("Margaret");
        nuevoCliente.setApellido("Hamilton");
        nuevoCliente.setEmail("margaret@mit.edu");
        nuevoCliente.setRegion(region);

        Cliente guardado = clienteDao.save(nuevoCliente);
        em.flush();

        assertThat(guardado.getId()).isNotNull();
        assertThat(em.find(Cliente.class, guardado.getId()).getNombre()).isEqualTo("Margaret");
    }

    @Test
    @DisplayName("deleteById — elimina el cliente verificado con em.find")
    void deleteById_debeEliminarElCliente() {
        Long id = cliente.getId();

        clienteDao.deleteById(id);
        em.flush();

        // Verificamos directamente en el contexto de persistencia
        assertThat(em.find(Cliente.class, id)).isNull();
    }

    @Test
    @DisplayName("findAllRegiones — devuelve las regiones persistidas")
    void findAllRegiones_debeRetornarListaDeRegiones() {
        // Act: llamamos a nuestra query personalizada
        List<Region> regiones = clienteDao.findAllRegiones();

        // Assert
        assertThat(regiones).hasSize(1);
        assertThat(regiones.get(0).getNombre()).isEqualTo("Asia");
    }

    @Test
    @DisplayName("findAll (Pageable) — retorna página paginada correctamente")
    void findAll_pageable_debeRetornarPaginaDeClientes() {
        Page<Cliente> pagina = clienteDao.findAll(PageRequest.of(0, 10));

        assertThat(pagina).isNotNull();
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getNombre()).isEqualTo("Grace");
    }
}