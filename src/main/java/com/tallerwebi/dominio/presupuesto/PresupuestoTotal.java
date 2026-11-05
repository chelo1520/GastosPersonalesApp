package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.YearMonth;

/**
 * Presupuesto total que el usuario definió para un mes; reemplaza al valor por defecto del perfil
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "mes" }))
public class PresupuestoTotal {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  // Se guarda como "AAAA-MM"
  private String mes;

  private Double monto;

  public PresupuestoTotal() {}

  public PresupuestoTotal(Usuario usuario, YearMonth mes, Double monto) {
    this.usuario = usuario;
    this.mes = mes.toString();
    this.monto = monto;
  }

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public YearMonth getMes() {
    return mes == null ? null : YearMonth.parse(mes);
  }

  public Double getMonto() {
    return monto;
  }

  public void setMonto(Double monto) {
    this.monto = monto;
  }
}
