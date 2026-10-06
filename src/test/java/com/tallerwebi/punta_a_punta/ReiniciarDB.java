package com.tallerwebi.punta_a_punta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class ReiniciarDB {

  private static final String EMAIL_USUARIO_DE_PRUEBA = "test@unlam.edu.ar";

  public static void limpiarBaseDeDatos() {
    // Primero los gastos: tienen una FK a Usuario y bloquearían el borrado de usuarios
    String sqlCommands =
      "DELETE FROM Gasto;\n" +
      "ALTER TABLE Gasto AUTO_INCREMENT = 1;\n" +
      "DELETE FROM Usuario;\n" +
      "ALTER TABLE Usuario AUTO_INCREMENT = 1;\n" +
      "INSERT INTO Usuario(id, email, password, rol, activo) VALUES(null, '" +
      EMAIL_USUARIO_DE_PRUEBA +
      "', 'test', 'ADMIN', true);";

    if (ejecutarSql(sqlCommands)) {
      System.out.println("Base de datos limpiada exitosamente");
    } else {
      System.err.println("Error al limpiar la base de datos");
    }
  }

  /**
   * Inserta un gasto directo en la base, sin pasar por la app. Sirve para preparar gastos de
   * meses anteriores, que el formulario de registro no permite cargar.
   */
  public static void insertarGastoDelUsuarioDePrueba(
    LocalDate fecha,
    double importe,
    String descripcion
  ) {
    String sql =
      "INSERT INTO Gasto(descripcion, fecha, importe, usuario_id) " +
      "SELECT '" +
      descripcion.replace("'", "''") +
      "', '" +
      fecha +
      "', " +
      importe +
      ", id FROM Usuario WHERE email = '" +
      EMAIL_USUARIO_DE_PRUEBA +
      "';";

    if (!ejecutarSql(sql)) {
      throw new IllegalStateException("No se pudo insertar el gasto de prueba: " + descripcion);
    }
  }

  private static boolean ejecutarSql(String sql) {
    try {
      String dbHost = System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "localhost";
      String dbPort = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "3306";
      String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "tallerwebi";
      String dbUser = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user";
      String dbPassword = System.getenv("DB_PASSWORD") != null
        ? System.getenv("DB_PASSWORD")
        : "user";

      // Se invoca docker directamente (sin /bin/bash ni cmd) para que funcione en cualquier SO
      List<String> comando = List.of(
        "docker",
        "exec",
        "tallerwebi-mysql",
        "mysql",
        "-h",
        dbHost,
        "-P",
        dbPort,
        "-u",
        dbUser,
        "-p" + dbPassword,
        dbName,
        "-e",
        sql
      );

      Process process = new ProcessBuilder(comando).redirectErrorStream(true).start();
      String salida = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      int exitCode = process.waitFor();

      if (exitCode != 0) {
        System.err.println("Error ejecutando SQL. Exit code: " + exitCode);
        System.err.println(salida);
      }
      return exitCode == 0;
    } catch (IOException | InterruptedException e) {
      System.err.println("Error ejecutando SQL: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }
}
