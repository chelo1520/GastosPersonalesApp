package com.tallerwebi.dominio.gasto;

import com.tallerwebi.dominio.Usuario;

/**
 * GastoServicio
 */
public interface GastoServicio {

    void registrarGasto(Gasto gasto, Usuario usuario);

}
