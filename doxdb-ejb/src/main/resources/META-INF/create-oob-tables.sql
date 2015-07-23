CREATE TABLE %1$sOOB (
	ID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY, 
	PARENTID BIGINT NOT NULL, 
	CONTENT BLOB(%2$d) NOT NULL, 
	REFERENCE VARCHAR(128) NOT NULL, 
	CREATEDBY VARCHAR(128) NOT NULL, 
	CREATEDON TIMESTAMP NOT NULL, 
	DOXID VARCHAR(32) NOT NULL, 
	LASTUPDATEDBY VARCHAR(128) NOT NULL, 
	LASTUPDATEDON TIMESTAMP NOT NULL, 
	VERSION INTEGER NOT NULL, 
	CONTENTVERSION INTEGER NOT NULL, 
	PRIMARY KEY (ID));

ALTER TABLE %1$sOOB add unique (DOXID, REFERENCE);
ALTER TABLE %1$sOOB add unique (PARENTID, REFERENCE);
ALTER TABLE %1$sOOB add foreign key (PARENTID, DOXID) references %1$s (ID, DOXID);

CREATE TABLE %1$sOOBTOMBSTONE (
	ID BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY, 
	CONTENT BLOB(%2$d) NOT NULL, 
	REFERENCE VARCHAR(128) NOT NULL, 
	CREATEDBY VARCHAR(128) NOT NULL, 
	CREATEDON TIMESTAMP NOT NULL, 
	DOXID VARCHAR(32) NOT NULL, 
	LASTUPDATEDBY VARCHAR(128) NOT NULL, 
	LASTUPDATEDON TIMESTAMP NOT NULL, 
	DELETEDBY VARCHAR(128) NOT NULL, 
	DELETEDON TIMESTAMP NOT NULL, 
	PRIMARY KEY (ID));

ALTER TABLE %1$sOOBTOMBSTONE add unique (DOXID, REFERENCE);
