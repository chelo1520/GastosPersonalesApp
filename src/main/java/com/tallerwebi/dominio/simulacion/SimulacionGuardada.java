package com.tallerwebi.dominio.simulacion;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.YearMonth;

@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "mesBase", "mesSimulado" })
)
public class SimulacionGuardada {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Column(nullable = false)
  private String mesBase;

  @Column(nullable = false)
  private String mesSimulado;

  @Lob
  @Column(nullable = false, length = Integer.MAX_VALUE)
  private String estado;

  protected SimulacionGuardada() {}

  public SimulacionGuardada(Usuario usuario, YearMonth base, YearMonth simulado, String estado) {
    this.usuario = usuario;
    this.mesBase = base.toString();
    this.mesSimulado = simulado.toString();
    this.estado = estado;
  }

  public String getEstado() {
    return estado;
  }

  public void setEstado(String estado) {
    this.estado = estado;
  }
}
