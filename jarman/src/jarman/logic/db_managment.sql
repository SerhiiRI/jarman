SHOW DATABASES;

USE `jarman`;

SHOW TABLES;

SELECT * FROM user;

SELECT * FROM repair_contract 
INNER JOIN cache_register ON cache_register.id=repair_contract.id_cache_register
INNER JOIN point_of_sale ON point_of_sale.id=cache_register.id_point_of_sale
INNER JOIN enterpreneur ON enterpreneur.id=point_of_sale.id_enterpreneur
INNER JOIN seal old_seal ON old_seal.id=repair_contract.id_old_seal
INNER JOIN seal new_seal ON new_seal.id=repair_contract.id_new_seal;



