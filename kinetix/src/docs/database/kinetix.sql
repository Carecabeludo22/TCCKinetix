-- Execute no SQL Server Management Studio conectado ao servidor (master).
-- A API cria as tabelas e os índices pelas migrations Flyway em db/migration.
IF DB_ID(N'Kinetix') IS NULL
BEGIN
    CREATE DATABASE [Kinetix];
END;
GO
