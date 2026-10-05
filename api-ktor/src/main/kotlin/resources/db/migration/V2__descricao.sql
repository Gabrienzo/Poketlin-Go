-- V2__criar_item_colecao.sql
CREATE TABLE item_colecao (
    id             UUID PRIMARY KEY,
    carta_id       TEXT NOT NULL REFERENCES carta(id),
    estado TEXT NOT NULL CHECK (estado IN ('MINT','NEAR_MINT','LIGHTLY_PLAYED','MODERATELY_PLAYED','HEAVILY_PLAYED','DAMAGED')),
    quantidade     INTEGER NOT NULL CHECK (quantidade >= 1),
    adicionado_em  TIMESTAMPTZ NOT NULL,
    UNIQUE (carta_id, estado)
);

CREATE INDEX idx_item_colecao_carta_id ON item_colecao (carta_id);