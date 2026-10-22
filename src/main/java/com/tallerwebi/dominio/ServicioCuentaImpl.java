package com.tallerwebi.dominio;

import com.tallerwebi.dominio.excepcion.ContrasenaIncorrecta;
import com.tallerwebi.dominio.excepcion.ContrasenaInvalida;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ServicioCuentaImpl implements ServicioCuenta {

  private final RepositorioUsuario repositorioUsuario;

  @Autowired
  public ServicioCuentaImpl(RepositorioUsuario repositorioUsuario) {
    this.repositorioUsuario = repositorioUsuario;
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
    repositorioUsuario.eliminar(verificar(usuario, password));
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
