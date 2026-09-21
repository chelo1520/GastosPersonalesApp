package com.tallerwebi.dominio.gasto;

import java.time.LocalDate;

import javax.annotation.processing.Generated;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Gasto
 */
@Entity 
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double importe;
    private LocalDate fecha;
    private String descripcion;
    public Gasto(Double importe, LocalDate fecha, String descripcion) {
        this.importe = importe;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }
    public Double getImporte() {
        return importe;
    }
    public LocalDate getFecha() {
        return fecha;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setImporte(Double importe) {
        this.importe = importe;
    }
    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
	public Long getId() {
		return this.id;
	}

    

}
