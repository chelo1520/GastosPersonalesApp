package com.tallerwebi.dominio.presupuesto;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.categoria.Categoria;
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
 * Monto que el usuario planea gastar en una categoría durante un mes
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "categoria_id", "mes" }))
public class Presupuesto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  @ManyToOne
  @JoinColumn(name = "categoria_id")
  private Categoria categoria;

  // Se guarda como "AAAA-MM" (YearMonth no tiene mapeo nativo en Hibernate)
  private String mes;

  private Double monto;

  public Presupuesto() {}

  public Presupuesto(Usuario usuario, Categoria categoria, YearMonth mes, Double monto) {
    this.usuario = usuario;
    this.categoria = categoria;
    this.mes = mes.toString();
    this.monto = monto;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public void setUsuario(Usuario usuario) {
    this.usuario = usuario;
  }

  public Categoria getCategoria() {
    return categoria;
  }

  public void setCategoria(Categoria categoria) {
    this.categoria = categoria;
  }

  public YearMonth getMes() {
    return mes == null ? null : YearMonth.parse(mes);
  }

  public void setMes(YearMonth mes) {
    this.mes = mes.toString();
  }

  public Double getMonto() {
    return monto;
  }

  public void setMonto(Double monto) {
    this.monto = monto;
  }
}
