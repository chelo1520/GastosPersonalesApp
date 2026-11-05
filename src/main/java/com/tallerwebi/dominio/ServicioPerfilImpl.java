package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ServicioPerfilImpl
 */
@Service
@Transactional
public class ServicioPerfilImpl implements ServicioPerfil {

  private RepositorioUsuario repositorioUsuario;
  private ServicioPresupuesto servicioPresupuesto;

  @Autowired
  public ServicioPerfilImpl(
    RepositorioUsuario repositorioUsuario,
    ServicioPresupuesto servicioPresupuesto
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.servicioPresupuesto = servicioPresupuesto;
  }

  @Override
  public void definirPresupuesto(Usuario usuario, Double ingresoMensual, Double porcentaje) {
    if (ingresoMensual == null || ingresoMensual <= 0) {
      throw new PresupuestoInvalido("El ingreso debe ser mayor a cero");
    }
    if (porcentaje == null || porcentaje <= 0 || porcentaje > 100) {
      throw new PresupuestoInvalido("El porcentaje debe estar entre 1 y 100");
    }

    servicioPresupuesto.validarPresupuestoPorDefecto(usuario, (ingresoMensual * porcentaje) / 100);

    usuario.setIngresoMensual(ingresoMensual);
    usuario.setPorcentajePresupuesto(porcentaje);
    repositorioUsuario.modificar(usuario);
  }
}
