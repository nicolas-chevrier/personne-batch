create sequence if not exists adresse_seq;
create sequence if not exists personne_seq;

create table if not exists adresse (
    id          bigint       primary key,
    numero      varchar(10)  not null,
    rue         varchar(150) not null,
    code_postal varchar(10)  not null,
    ville       varchar(100) not null,
    constraint adresse_uk unique (numero, rue, code_postal, ville)
);

create table if not exists personne (
    id         bigint       primary key,
    nom        varchar(100) not null,
    prenom     varchar(100) not null,
    age        integer      not null,
    profession varchar(100) not null,
    adresse_id bigint       not null references adresse (id),
    constraint personne_uk unique (nom, prenom)
);

create index if not exists personne_adresse_idx on personne (adresse_id);

-- Table intermédiaire : les chunks écrivent ici, et son contenu n'est recopié vers
-- adresse et personne qu'à la fin, en une seule transaction.
create table if not exists personne_import (
    numero_ligne integer      primary key,
    nom          varchar(100) not null,
    prenom       varchar(100) not null,
    age          integer      not null,
    profession   varchar(100) not null,
    numero       varchar(10)  not null,
    rue          varchar(150) not null,
    code_postal  varchar(10)  not null,
    ville        varchar(100) not null
);
