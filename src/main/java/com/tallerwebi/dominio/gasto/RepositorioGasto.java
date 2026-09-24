package com.tallerwebi.dominio.gasto;

import java.util.List;

import com.tallerwebi.dominio.Usuario;

/**
 * RepositorioGasto
 */
public interface RepositorioGasto {

    void guardar(Gasto gasto);

    List<Gasto> BuscarGastosPorUsuario(Usuario usuario);

}
