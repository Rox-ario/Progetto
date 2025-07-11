CREATE DATABASE IF NOT EXISTS trenical_dev;
USE trenical_dev;

CREATE TABLE IF NOT EXISTS clienti
 (
    id VARCHAR(255) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    is_fedelta BOOLEAN DEFAULT FALSE,
    ricevi_notifiche BOOLEAN DEFAULT TRUE,
    ricevi_promozioni BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS stazioni
 (
    id VARCHAR(255) PRIMARY KEY,
    citta VARCHAR(100) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    latitudine DOUBLE NOT NULL,
    longitudine DOUBLE NOT NULL,
    binari VARCHAR(255) NOT NULL
 );

CREATE TABLE IF NOT EXISTS tratte
(
    id VARCHAR(255) PRIMARY KEY,
    stazione_partenza_id VARCHAR(255) NOT NULL,
    stazione_arrivo_id VARCHAR(255) NOT NULL,
    FOREIGN KEY (stazione_partenza_id) REFERENCES stazioni(id),
    FOREIGN KEY (stazione_arrivo_id) REFERENCES stazioni(id)
);

CREATE TABLE IF NOT EXISTS treni
(
    id VARCHAR(255) PRIMARY KEY,
    tipo VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS viaggi
(
    id VARCHAR(255) PRIMARY KEY,
    treno_id VARCHAR(255) NOT NULL,
    tratta_id VARCHAR(255) NOT NULL,
    orario_partenza DATETIME NOT NULL,
    orario_arrivo DATETIME NOT NULL,
    stato VARCHAR(100) DEFAULT 'PROGRAMMATO',
    ritardo_minuti INT DEFAULT 0,
    binario_partenza INT,
    binario_arrivo INT,
    FOREIGN KEY (treno_id) REFERENCES treni(id),
    FOREIGN KEY (tratta_id) REFERENCES tratte(id)
);

CREATE TABLE IF NOT EXISTS biglietti
 (
    id VARCHAR(255) PRIMARY KEY,
    viaggio_id VARCHAR(255) NOT NULL,
    cliente_id VARCHAR(255) NOT NULL,
    classe_servizio VARCHAR(100) NOT NULL,
    prezzo DECIMAL(10,2) NOT NULL,
    stato VARCHAR(100) DEFAULT 'NON_PAGATO',
    data_acquisto TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (viaggio_id) REFERENCES viaggi(id),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id)
);

CREATE TABLE IF NOT EXISTS clienti_banca
(
    cliente_id VARCHAR(255) PRIMARY KEY,
    cliente_nome VARCHAR(100) NOT NULL,
    cliente_cognome VARCHAR(100) NOT NULL,
    banca_cliente VARCHAR(100) NOT NULL,
    cliente_numeroCarta VARCHAR(100) NOT NULL,
    saldo DECIMAL(10,2) DEFAULT 1000.00,
    FOREIGN KEY (cliente_id) REFERENCES clienti(id)
);

CREATE TABLE IF NOT EXISTS promozioni
(
    id VARCHAR(255) PRIMARY KEY,
    tipo VARCHAR(100) NOT NULL,
    data_inizio DATE NOT NULL,
    data_fine DATE NOT NULL,
    percentuale_sconto DOUBLE NOT NULL,
    stato VARCHAR(20) DEFAULT 'PROGRAMMATA',
    tratta_id VARCHAR(255),
    tipo_treno VARCHAR(100),
    FOREIGN KEY (tratta_id) REFERENCES tratte(id)
);

CREATE TABLE IF NOT EXISTS notifiche
(
    id VARCHAR(255) PRIMARY KEY,
    cliente_id VARCHAR(255) NOT NULL,
    messaggio TEXT NOT NULL,
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clienti(id)
);

CREATE TABLE IF NOT EXISTS iscrizioni_treni (
    cliente_id VARCHAR(255) NOT NULL,
    treno_id VARCHAR(255) NOT NULL,
    data_iscrizione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cliente_id, treno_id),
    FOREIGN KEY (cliente_id) REFERENCES clienti(id),
    FOREIGN KEY (treno_id) REFERENCES treni(id)
);