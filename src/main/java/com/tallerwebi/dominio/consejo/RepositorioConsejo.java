package com.tallerwebi.dominio.consejo;

import com.tallerwebi.dominio.Usuario;
import java.time.YearMonth;
import java.util.List;

public interface RepositorioConsejo {
  void guardar(Consejo consejo);

  void modificar(Consejo consejo);

  Consejo buscarPorId(Long id);

  List<Consejo> obtenerDelMes(Usuario usuario, YearMonth mes);
}
