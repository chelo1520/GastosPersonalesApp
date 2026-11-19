package com.tallerwebi.dominio.comprobante;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.gasto.Gasto;
import com.tallerwebi.dominio.gasto.GastoServicio;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioComprobanteImpl implements ServicioComprobante {

  private static final Logger LOG = Logger.getLogger(ServicioComprobanteImpl.class.getName());

  private final RepositorioComprobante repositorioComprobante;
  private final AlmacenComprobantes almacen;
  private final GastoServicio gastoServicio;
  private final Clock reloj;

  @Autowired
  public ServicioComprobanteImpl(
    RepositorioComprobante repositorioComprobante,
    AlmacenComprobantes almacen,
    GastoServicio gastoServicio
  ) {
    this(repositorioComprobante, almacen, gastoServicio, Clock.systemDefaultZone());
  }

  ServicioComprobanteImpl(
    RepositorioComprobante repositorioComprobante,
    AlmacenComprobantes almacen,
    GastoServicio gastoServicio,
    Clock reloj
  ) {
    this.repositorioComprobante = repositorioComprobante;
    this.almacen = almacen;
    this.gastoServicio = gastoServicio;
    this.reloj = reloj;
  }

  @Override
  public void verificarQueNoEsteCargado(ArchivoComprobante archivo, Usuario usuario) {
    Comprobante anterior = repositorioComprobante.buscar(usuario, archivo.huella());
    if (anterior == null) {
      return;
    }
    // Si se borraron todos sus gastos, se puede volver a cargar
    long gastos = repositorioComprobante.contarGastos(anterior);
    if (gastos > 0) {
      throw new ComprobanteYaCargado(anterior.getFechaCarga().toLocalDate(), gastos);
    }
  }

  @Override
  public void registrarGastos(List<Gasto> gastos, ArchivoComprobante archivo, Usuario usuario) {
    // Se vuelve a mirar: entre la lectura y la confirmación pudo cargarse en otra pestaña
    verificarQueNoEsteCargado(archivo, usuario);
    Comprobante comprobante = comprobanteDe(archivo, usuario);
    for (Gasto gasto : gastos) {
      gasto.setComprobante(comprobante);
    }
    // Valida y guarda; si un gasto es inválido no se guarda nada y tampoco se sube el archivo
    gastoServicio.registrarGastos(gastos, usuario);
    subir(comprobante, archivo, usuario);
  }

  // El mismo registro si ya existía (sus gastos se borraron), o uno nuevo
  private Comprobante comprobanteDe(ArchivoComprobante archivo, Usuario usuario) {
    LocalDateTime ahora = LocalDateTime.now(reloj);
    Comprobante comprobante = repositorioComprobante.buscar(usuario, archivo.huella());
    if (comprobante == null) {
      comprobante = new Comprobante(usuario, archivo.huella(), archivo.tipo(), ahora);
      repositorioComprobante.guardar(comprobante);
    } else {
      comprobante.setFechaCarga(ahora);
    }
    return comprobante;
  }

  private void subir(Comprobante comprobante, ArchivoComprobante archivo, Usuario usuario) {
    if (!almacen.disponible()) {
      return;
    }
    String clave = archivo.clave(usuario.getId());
    try {
      almacen.guardar(clave, archivo.contenido(), archivo.tipo());
      comprobante.setClave(clave);
    } catch (AlmacenNoDisponible e) {
      LOG.log(Level.WARNING, "No se pudo guardar el comprobante en el almacén", e);
    }
  }
}
