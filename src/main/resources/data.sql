-- Contenu initial de la base : quelques adresses et personnes présentes avant l'import.
-- Certaines figurent aussi dans le fichier CSV, d'autres non. Rejouable sans créer de doublon.

insert into adresse (id, numero, rue, code_postal, ville)
select nextval('adresse_seq'), v.numero, v.rue, v.code_postal, v.ville
from (values
    ('12', 'rue de la Paix',     '75002', 'Paris'),
    ('5',  'avenue Jean Jaurès', '69007', 'Lyon'),
    ('8',  'place du Capitole',  '31000', 'Toulouse'),
    ('3',  'impasse des Lilas',  '29200', 'Brest')
) as v (numero, rue, code_postal, ville)
where not exists (
    select 1 from adresse a
    where a.numero = v.numero and a.rue = v.rue and a.code_postal = v.code_postal and a.ville = v.ville
);

insert into personne (id, nom, prenom, age, profession, adresse_id)
select nextval('personne_seq'), v.nom, v.prenom, v.age, v.profession, a.id
from (values
    ('Dupont', 'Marie',  34, 'Infirmière',  '12', 'rue de la Paix',     '75002', 'Paris'),
    ('Dupont', 'Paul',   36, 'Enseignant',  '12', 'rue de la Paix',     '75002', 'Paris'),
    ('Durand', 'Luc',    52, 'Boulanger',   '5',  'avenue Jean Jaurès', '69007', 'Lyon'),
    ('Petit',  'Hugo',   28, 'Développeur', '5',  'avenue Jean Jaurès', '69007', 'Lyon'),
    ('Moreau', 'Jeanne', 67, 'Retraitée',   '3',  'impasse des Lilas',  '29200', 'Brest')
) as v (nom, prenom, age, profession, numero, rue, code_postal, ville)
join adresse a
    on a.numero = v.numero and a.rue = v.rue and a.code_postal = v.code_postal and a.ville = v.ville
where not exists (
    select 1 from personne p
    where p.nom = v.nom and p.prenom = v.prenom
);
