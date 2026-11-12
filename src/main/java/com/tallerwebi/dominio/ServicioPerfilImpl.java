package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.DatosPersonalesInvalidos;
import com.tallerwebi.dominio.excepcion.PresupuestoInvalido;
import com.tallerwebi.dominio.presupuesto.ServicioPresupuesto;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
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

  static final int LARGO_MAXIMO = 60;
  static final LocalDate NACIMIENTO_MINIMO = LocalDate.of(1900, 1, 1);

  @Override
  public void actualizarDatosPersonales(
    Usuario usuario,
    String nombre,
    String apellido,
    LocalDate fechaNacimiento
  ) {
    String nombreLimpio = limpiar(nombre, "El nombre");
    String apellidoLimpio = limpiar(apellido, "El apellido");
    if (fechaNacimiento != null) {
      if (fechaNacimiento.isAfter(LocalDate.now())) {
        throw new DatosPersonalesInvalidos("La fecha de nacimiento no puede ser futura");
      }
      if (fechaNacimiento.isBefore(NACIMIENTO_MINIMO)) {
        throw new DatosPersonalesInvalidos("Revisá la fecha de nacimiento");
      }
    }
    usuario.setNombre(nombreLimpio);
    usuario.setApellido(apellidoLimpio);
    usuario.setFechaNacimiento(fechaNacimiento);
    repositorioUsuario.modificar(usuario);
  }

  // Sin espacios de más; vacío es "no cargado"
  private static String limpiar(String valor, String campo) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    String limpio = valor.trim();
    if (limpio.length() > LARGO_MAXIMO) {
      throw new DatosPersonalesInvalidos(campo + " no puede superar los 60 caracteres");
    }
    return limpio;
  }
}
