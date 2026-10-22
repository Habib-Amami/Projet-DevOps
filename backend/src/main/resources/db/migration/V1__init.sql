-- Baseline schema for the Achat application.
-- This matches the schema Hibernate created before Flyway was introduced.

CREATE TABLE categorie_produit (
    id_categorie_produit BIGINT NOT NULL AUTO_INCREMENT,
    code_categorie VARCHAR(255) NOT NULL,
    libelle_categorie VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_categorie_produit)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE detail_fournisseur (
    id_detail_fournisseur BIGINT NOT NULL AUTO_INCREMENT,
    adresse VARCHAR(255) NOT NULL,
    date_debut_collaboration DATE DEFAULT NULL,
    email VARCHAR(255) DEFAULT NULL,
    matricule VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_detail_fournisseur)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE stock (
    id_stock BIGINT NOT NULL AUTO_INCREMENT,
    libelle_stock VARCHAR(255) NOT NULL,
    qte INT NOT NULL,
    qte_min INT NOT NULL,
    PRIMARY KEY (id_stock)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE produit (
    id_produit BIGINT NOT NULL AUTO_INCREMENT,
    code_produit VARCHAR(255) NOT NULL,
    date_creation DATE DEFAULT NULL,
    date_derniere_modification DATE DEFAULT NULL,
    libelle_produit VARCHAR(255) NOT NULL,
    prix FLOAT NOT NULL,
    categorie_produit_id_categorie_produit BIGINT DEFAULT NULL,
    stock_id_stock BIGINT DEFAULT NULL,
    PRIMARY KEY (id_produit),
    KEY idx_produit_categorie (categorie_produit_id_categorie_produit),
    KEY idx_produit_stock (stock_id_stock),
    CONSTRAINT fk_produit_categorie FOREIGN KEY (categorie_produit_id_categorie_produit)
        REFERENCES categorie_produit (id_categorie_produit),
    CONSTRAINT fk_produit_stock FOREIGN KEY (stock_id_stock)
        REFERENCES stock (id_stock)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE secteur_activite (
    id_secteur_activite BIGINT NOT NULL AUTO_INCREMENT,
    code_secteur_activite VARCHAR(255) NOT NULL,
    libelle_secteur_activite VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_secteur_activite)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE fournisseur (
    id_fournisseur BIGINT NOT NULL AUTO_INCREMENT,
    categorie_fournisseur ENUM ('CONVENTIONNE', 'ORDINAIRE') NOT NULL,
    code VARCHAR(255) NOT NULL,
    libelle VARCHAR(255) NOT NULL,
    detail_fournisseur_id_detail_fournisseur BIGINT DEFAULT NULL,
    PRIMARY KEY (id_fournisseur),
    UNIQUE KEY uk_fournisseur_detail (detail_fournisseur_id_detail_fournisseur),
    CONSTRAINT fk_fournisseur_detail FOREIGN KEY (detail_fournisseur_id_detail_fournisseur)
        REFERENCES detail_fournisseur (id_detail_fournisseur)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE fournisseur_secteur_activites (
    fournisseurs_id_fournisseur BIGINT NOT NULL,
    secteur_activites_id_secteur_activite BIGINT NOT NULL,
    PRIMARY KEY (fournisseurs_id_fournisseur, secteur_activites_id_secteur_activite),
    KEY idx_fournisseur_secteur_secteur (secteur_activites_id_secteur_activite),
    CONSTRAINT fk_fournisseur_secteur_fournisseur FOREIGN KEY (fournisseurs_id_fournisseur)
        REFERENCES fournisseur (id_fournisseur),
    CONSTRAINT fk_fournisseur_secteur_secteur FOREIGN KEY (secteur_activites_id_secteur_activite)
        REFERENCES secteur_activite (id_secteur_activite)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE facture (
    id_facture BIGINT NOT NULL AUTO_INCREMENT,
    archivee BIT(1) DEFAULT NULL,
    date_creation_facture DATE DEFAULT NULL,
    date_derniere_modification_facture DATE DEFAULT NULL,
    montant_facture FLOAT NOT NULL,
    montant_remise FLOAT NOT NULL,
    fournisseur_id_fournisseur BIGINT DEFAULT NULL,
    PRIMARY KEY (id_facture),
    KEY idx_facture_fournisseur (fournisseur_id_fournisseur),
    CONSTRAINT fk_facture_fournisseur FOREIGN KEY (fournisseur_id_fournisseur)
        REFERENCES fournisseur (id_fournisseur)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE detail_facture (
    id_detail_facture BIGINT NOT NULL AUTO_INCREMENT,
    montant_remise FLOAT NOT NULL,
    pourcentage_remise INT DEFAULT NULL,
    prix_total_detail FLOAT NOT NULL,
    qte_commandee INT DEFAULT NULL,
    facture_id_facture BIGINT DEFAULT NULL,
    produit_id_produit BIGINT DEFAULT NULL,
    PRIMARY KEY (id_detail_facture),
    KEY idx_detail_facture_facture (facture_id_facture),
    KEY idx_detail_facture_produit (produit_id_produit),
    CONSTRAINT fk_detail_facture_facture FOREIGN KEY (facture_id_facture)
        REFERENCES facture (id_facture),
    CONSTRAINT fk_detail_facture_produit FOREIGN KEY (produit_id_produit)
        REFERENCES produit (id_produit)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE reglement (
    id_reglement BIGINT NOT NULL AUTO_INCREMENT,
    date_reglement DATE NOT NULL,
    montant_paye FLOAT NOT NULL,
    montant_restant FLOAT NOT NULL,
    payee BIT(1) NOT NULL,
    facture_id_facture BIGINT NOT NULL,
    PRIMARY KEY (id_reglement),
    KEY idx_reglement_facture (facture_id_facture),
    CONSTRAINT fk_reglement_facture FOREIGN KEY (facture_id_facture)
        REFERENCES facture (id_facture)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE operateur (
    id_operateur BIGINT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    prenom VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_operateur)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE operateur_factures (
    operateur_id_operateur BIGINT NOT NULL,
    factures_id_facture BIGINT NOT NULL,
    PRIMARY KEY (factures_id_facture),
    KEY idx_operateur_factures_operateur (operateur_id_operateur),
    CONSTRAINT fk_operateur_factures_operateur FOREIGN KEY (operateur_id_operateur)
        REFERENCES operateur (id_operateur),
    CONSTRAINT fk_operateur_factures_facture FOREIGN KEY (factures_id_facture)
        REFERENCES facture (id_facture)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
