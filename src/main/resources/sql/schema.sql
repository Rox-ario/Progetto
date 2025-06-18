CREATE DATABASE IF NOT EXISTS trenical;
USE trenical;

--Tabella Clienti
CREATE TABLE clienti
 (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    is_fedelta BOOLEAN DEFAULT FALSE,
    ricevi_notifiche BOOLEAN DEFAULT TRUE,
    ricevi_promozioni BOOLEAN DEFAULT FALSE
);

-- Tabella Stazioni
CREATE TABLE stazioni
 (
    id VARCHAR(10) PRIMARY KEY,
    citta VARCHAR(100) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    latitudine DOUBLE NOT NULL,
    longitudine DOUBLE NOT NULL,
    binari VARCHAR(255) NOT NULL
 );

-- Tabella Tratte
CREATE TABLE tratte
(
    id VARCHAR(10) PRIMARY KEY,
    stazione_partenza_id VARCHAR(10) NOT NULL,
    stazione_arrivo_id VARCHAR(10) NOT NULL,
    FOREIGN KEY (stazione_partenza_id) REFERENCES stazioni(id),
    FOREIGN KEY (stazione_arrivo_id) REFERENCES stazioni(id)
);

-- Tabella Treni
CREATE TABLE treni
(
    id VARCHAR(20) PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL
);

-- Tabella Viaggi
CREATE TABLE viaggi
(
    id VARCHAR(36) PRIMARY KEY,
    treno_id VARCHAR(20) NOT NULL,
    tratta_id VARCHAR(10) NOT NULL,
    orario_partenza DATETIME NOT NULL,
    orario_arrivo DATETIME NOT NULL,
    stato VARCHAR(20) DEFAULT 'PROGRAMMATO',
    ritardo_minuti INT DEFAULT 0,
    FOREIGN KEY (treno_id) REFERENCES treni(id),
    FOREIGN KEY (tratta_id) REFERENCES tratte(id)
);

-- Tabella Biglietti
CREATE TABLE biglietti
 (
    id VARCHAR(36) PRIMARY KEY,
    viaggio_id VARCHAR(36) NOT NULL,
    cliente_id VARCHAR(36) NOT NULL,
    classe_servizio VARCHAR(20) NOT NULL,
    prezzo DECIMAL(10,2) NOT NULL,
    stato VARCHAR(20) DEFAULT 'NON_PAGATO',
    data_acquisto TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (viaggio_id) REFERENCES viaggi(id),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id)
);

-- Tabella Banca
CREATE TABLE clienti_banca
(
    cliente_id VARCHAR(36) PRIMARY KEY,
    cliente_nome VARCHAR(36) NOT NULL,
    cliente_cognome VARCHAR(36) NOT NULL,
    banca_cliente VARCHAR(100) NOT NULL,
    cliente_numeroCarta VARCHAR(100) NOT NULL,
    saldo DECIMAL(10,2) DEFAULT 1000.00,
    FOREIGN KEY (cliente_id) REFERENCES clienti(id)
);

CREATE TABLE promozioni
(
    id VARCHAR(36) PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    data_inizio DATE NOT NULL,
    data_fine DATE NOT NULL,
    percentuale_sconto DOUBLE NOT NULL,
    stato VARCHAR(20) DEFAULT 'PROGRAMMATA',

    -- Campi opzionali
    tratta_id VARCHAR(10), -- solo per tipo TRATTA
    tipo_treno VARCHAR(20), -- solo per tipo TRENO

    FOREIGN KEY (tratta_id) REFERENCES tratte(id)
);