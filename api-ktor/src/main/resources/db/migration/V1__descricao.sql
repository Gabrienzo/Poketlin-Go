-- V1__criar_carta.sql
CREATE TABLE carta (
    id         TEXT PRIMARY KEY,
    nome       TEXT NOT NULL,
    edicao     TEXT NOT NULL,
    imagem_url TEXT
);