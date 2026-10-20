package com.tallerwebi.dominio.recuperacion;

import com.tallerwebi.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

/**
 * Pedido para cambiar la contraseña desde el link del mail. Se guarda el hash del token, no el
 * token: con acceso a la base no alcanza para armar un link que funcione.
 */
@Entity
public class TokenRecuperacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String hash;

  @ManyToOne
  @JoinColumn(name = "usuario_id")
  private Usuario usuario;

  private LocalDateTime vence;

  // Un link sirve una sola vez
  private Boolean usado = false;

  public TokenRecuperacion() {}

  public TokenRecuperacion(String hash, Usuario usuario, LocalDateTime vence) {
    this.hash = hash;
    this.usuario = usuario;
    this.vence = vence;
  }

  public boolean esValido(LocalDateTime ahora) {
    return !Boolean.TRUE.equals(usado) && ahora.isBefore(vence);
  }

  public void marcarUsado() {
    this.usado = true;
  }

  public Long getId() {
    return id;
  }

  public String getHash() {
    return hash;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public LocalDateTime getVence() {
    return vence;
  }

  public boolean isUsado() {
    return Boolean.TRUE.equals(usado);
  }
}
