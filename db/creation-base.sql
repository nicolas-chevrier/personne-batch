-- À exécuter une fois avec un superutilisateur : psql -U postgres -f db/creation-base.sql
create role personne login password 'personne';
create database personnes owner personne;
-- Base des tests d'intégration : ses tables sont vidées à chaque exécution.
create database personnes_test owner personne;
