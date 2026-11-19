package com.tallerwebi.dominio;

import com.tallerwebi.dominio.comprobante.ServicioComprobante;
import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioCuentaImpl implements ServicioCuenta {

  private final RepositorioUsuario repositorioUsuario;
  private final ServicioComprobante servicioComprobante;

  @Autowired
  public ServicioCuentaImpl(
    RepositorioUsuario repositorioUsuario,
    ServicioComprobante servicioComprobante
  ) {
    this.repositorioUsuario = repositorioUsuario;
    this.servicioComprobante = servicioComprobante;
  }

  @Override
  public Usuario cambiarContrasena(Usuario usuario, String actual, String nueva) {
    Usuario guardado = verificar(usuario, actual);
    if (!ReglasDeContrasena.cumple(nueva)) {
      throw new ContrasenaInvalida();
    }
    guardado.setPassword(nueva);
    repositorioUsuario.modificar(guardado);
    return guardado;
  }

  @Override
  public void eliminarCuenta(Usuario usuario, String password) {
    Usuario guardado = verificar(usuario, password);
    // Las claves se leen antes de borrar los registros; los archivos se borran después, así un
    // error al eliminar la cuenta no deja comprobantes sin su archivo
    List<String> archivos = servicioComprobante.archivosDe(guardado);
    repositorioUsuario.eliminar(guardado);
    servicioComprobante.eliminarArchivos(archivos);
  }

  // Se compara contra la base y no contra la sesión, que puede estar desactualizada
  private Usuario verificar(Usuario usuario, String password) {
    Usuario guardado = usuario == null ? null : repositorioUsuario.buscar(usuario.getEmail());
    if (guardado == null || password == null || !password.equals(guardado.getPassword())) {
      throw new ContrasenaIncorrecta();
    }
    return guardado;
  }
}
