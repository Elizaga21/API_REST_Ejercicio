package com.ejercicio.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ejercicio.entities.Cliente;

public interface ClienteDao extends JpaRepository<Cliente, Long> {
    
}
