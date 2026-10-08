-- Se ejecuta en cada arranque: solo inserta el usuario de prueba si todavía no existe
INSERT INTO Usuario(email, password, rol, activo) SELECT 'test@unlam.edu.ar', 'test', 'ADMIN', true FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE email = 'test@unlam.edu.ar');
