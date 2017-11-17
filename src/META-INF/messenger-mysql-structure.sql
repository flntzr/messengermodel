-- MySQL data definition script for the messenger schema
SET CHARACTER SET utf8;
DROP DATABASE IF EXISTS messenger;
CREATE DATABASE messenger CHARACTER SET utf8;
USE messenger;

-- define tables, indices, etc.
CREATE TABLE BaseEntity (
	identity BIGINT NOT NULL AUTO_INCREMENT,
	discriminator ENUM("Document", "Person", "Message") NOT NULL,
	version INTEGER NOT NULL DEFAULT 1,
	creationTimestamp BIGINT NOT NULL,
	PRIMARY KEY (identity),
	KEY (discriminator)
) ENGINE=InnoDB;

CREATE TABLE Document (
	documentIdentity BIGINT NOT NULL,
	contentHash BINARY(32) NOT NULL,
	contentType VARCHAR(63) NOT NULL,
	content MEDIUMBLOB NOT NULL,
	PRIMARY KEY (documentIdentity),
	UNIQUE KEY (contentHash),
	FOREIGN KEY (documentIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Person (
	personIdentity BIGINT NOT NULL,
	avatarReference BIGINT NOT NULL,
	email CHAR(128) NOT NULL,
	passwordHash BINARY(32) NOT NULL,
	groupAlias ENUM("USER", "ADMIN") NOT NULL,
	givenName VARCHAR(31) NOT NULL,
	familyName VARCHAR(31) NOT NULL,
	street VARCHAR(63) NULL,
	postcode VARCHAR(15) NULL,
	city VARCHAR(63) NOT NULL,
	PRIMARY KEY (personIdentity),
	UNIQUE KEY (email),
	FOREIGN KEY (personIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (avatarReference) REFERENCES Document (documentIdentity) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Message (
	messageIdentity BIGINT NOT NULL,
	subjectReference BIGINT NOT NULL,
	authorReference BIGINT NOT NULL,
	body VARCHAR(4093) NOT NULL,
	PRIMARY KEY (messageIdentity),
	FOREIGN KEY (messageIdentity) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (subjectReference) REFERENCES BaseEntity (identity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (authorReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE ObservationAssociation (
	observingReference BIGINT NOT NULL,
	observedReference BIGINT NOT NULL,
	PRIMARY KEY (observingReference, observedReference),
	FOREIGN KEY (observingReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (observedReference) REFERENCES Person (personIdentity) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- define views
CREATE ALGORITHM=MERGE VIEW JoinedEntity AS
SELECT *
FROM BaseEntity
LEFT OUTER JOIN Person ON personIdentity = identity
LEFT OUTER JOIN Document ON documentIdentity = identity
LEFT OUTER JOIN Message ON messageIdentity = identity;
