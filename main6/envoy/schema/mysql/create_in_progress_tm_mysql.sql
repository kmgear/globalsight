--  Create in-progress TM tables

INSERT INTO SEQUENCE VALUES('IP_TM_SRC_T_SEQ', 1000);
INSERT INTO SEQUENCE VALUES('IP_TM_SRC_L_SEQ', 1000);
INSERT INTO SEQUENCE VALUES('IP_TM_TRG_T_SEQ', 1000);
INSERT INTO SEQUENCE VALUES('IP_TM_TRG_L_SEQ', 1000);
COMMIT;

--  in-progress tm source translatable
DROP TABLE IF EXISTS IP_TM_SRC_T CASCADE;
CREATE TABLE IP_TM_SRC_T
(
  ID BIGINT
     AUTO_INCREMENT
     PRIMARY KEY,
  JOB_ID  BIGINT,
  POPULATION_TM_ID  INTEGER,
--   FORMAT  VARCHAR2(20),
  LOCALE_ID BIGINT,
  `TYPE` VARCHAR(50)
     NOT NULL,
  SEGMENT_STRING TEXT,
  SEGMENT_CLOB TEXT,
  EXACT_MATCH_KEY BIGINT,
  CONSTRAINT IP_TM_SRC_T_JOB FOREIGN KEY(JOB_ID) REFERENCES JOB(ID), 
  CONSTRAINT IP_TM_SRC_T_LOCALE FOREIGN KEY(LOCALE_ID) REFERENCES LOCALE(ID) 
) AUTO_INCREMENT = 1000;


--  in-progress tm source localizable
DROP TABLE IF EXISTS IP_TM_SRC_L CASCADE;
CREATE TABLE IP_TM_SRC_L
(
  ID BIGINT
     AUTO_INCREMENT
     PRIMARY KEY,
  JOB_ID BIGINT,
  POPULATION_TM_ID INTEGER,
--   FORMAT  VARCHAR2(20),
  LOCALE_ID BIGINT,
  TYPE VARCHAR(50)
     NOT NULL,
  SEGMENT_STRING TEXT,
  SEGMENT_CLOB TEXT,
  EXACT_MATCH_KEY BIGINT,
  CONSTRAINT IP_TM_SRC_L_JOB FOREIGN KEY(JOB_ID) REFERENCES JOB(ID), 
  CONSTRAINT IP_TM_SRC_L_LOCALE FOREIGN KEY(LOCALE_ID) REFERENCES LOCALE(ID) 
) AUTO_INCREMENT = 1000;


--  in-progress tm target translatable
DROP TABLE IF EXISTS IP_TM_TRG_T CASCADE;
CREATE TABLE IP_TM_TRG_T
(
  ID BIGINT
     AUTO_INCREMENT
     PRIMARY KEY,
  SRC_ID BIGINT,
  TU_ID BIGINT,
  LOCALE_ID BIGINT,
  SEGMENT_STRING TEXT,
  SEGMENT_CLOB TEXT,
  CONSTRAINT IP_TM_TRG_T_SRC_ID FOREIGN KEY(SRC_ID) REFERENCES IP_TM_SRC_T(ID), 
  CONSTRAINT IP_TM_SRC_L_TU_ID FOREIGN KEY(TU_ID) REFERENCES TRANSLATION_UNIT(ID),
  CONSTRAINT IP_TM_SRC_L_LOCALE_ID FOREIGN KEY(LOCALE_ID) REFERENCES LOCALE(ID) 
) AUTO_INCREMENT = 1000;


--  in-progress tm target localizable
DROP TABLE IF EXISTS IP_TM_TRG_L CASCADE;
CREATE TABLE IP_TM_TRG_L
(
  ID BIGINT
     AUTO_INCREMENT
     PRIMARY KEY,
  SRC_ID BIGINT,
  TU_ID BIGINT,
  LOCALE_ID BIGINT,
  SEGMENT_STRING TEXT,
  SEGMENT_CLOB TEXT,
  CONSTRAINT IP_TM_TRG_L_SRC_ID FOREIGN KEY(SRC_ID) REFERENCES IP_TM_SRC_L(ID),
  CONSTRAINT IP_TM_TRG_L_LOCALE_ID FOREIGN KEY(LOCALE_ID) REFERENCES LOCALE(ID) 
) AUTO_INCREMENT = 1000;


--  in-progress tm index
DROP TABLE IF EXISTS IP_TM_INDEX CASCADE;
CREATE TABLE IP_TM_INDEX
(
  TOKEN VARCHAR(300) NOT NULL,
  SRC_ID BIGINT,
  JOB_ID BIGINT,
  POPULATION_TM_ID INTEGER,
  LOCALE_ID BIGINT,
  REPETITION INTEGER,
  TOTAL_TOKEN_COUNT INTEGER,
  CONSTRAINT IP_TM_INDEX_LOCALE_ID FOREIGN KEY(SRC_ID) REFERENCES IP_TM_SRC_T(ID), 
  CONSTRAINT IP_TM_INDEX_JOB_ID FOREIGN KEY(JOB_ID) REFERENCES JOB(ID), 
  CONSTRAINT IP_TM_INDEX_SRC_ID FOREIGN KEY(LOCALE_ID) REFERENCES LOCALE(ID) 
);



--  index creation

CREATE INDEX IP_S_T_JOB_ID ON IP_TM_SRC_T(JOB_ID);
CREATE INDEX IP_S_T_POPULATION_TM_ID ON IP_TM_SRC_T(POPULATION_TM_ID);
CREATE INDEX IP_S_T_LOCALE_ID ON IP_TM_SRC_T(LOCALE_ID);
CREATE INDEX IP_S_T_TYPE ON IP_TM_SRC_T(TYPE);
CREATE INDEX IP_S_T_EXACT_MATCH_KEY ON IP_TM_SRC_T(EXACT_MATCH_KEY);

CREATE INDEX IP_S_L_JOB_ID ON IP_TM_SRC_L(JOB_ID);
CREATE INDEX IP_S_L_POPULATION_TM_ID ON IP_TM_SRC_L(POPULATION_TM_ID);
CREATE INDEX IP_S_L_LOCALE_ID ON IP_TM_SRC_L(LOCALE_ID);
CREATE INDEX IP_S_L_TYPE ON IP_TM_SRC_L(TYPE);
CREATE INDEX IP_S_L_EXACT_MATCH_KEY ON IP_TM_SRC_L(EXACT_MATCH_KEY);

CREATE INDEX IP_T_T_SRC_ID ON IP_TM_TRG_T(SRC_ID);
CREATE INDEX IP_T_T_LOCALE_ID ON IP_TM_TRG_T(LOCALE_ID);

CREATE INDEX IP_T_L_SRC_ID ON IP_TM_TRG_L(SRC_ID);
CREATE INDEX IP_T_L_LOCALE_ID ON IP_TM_TRG_L(LOCALE_ID);

CREATE INDEX IP_IDX_LOCALE_ID ON IP_TM_INDEX(LOCALE_ID);
CREATE INDEX IP_IDX_JOB_ID ON IP_TM_INDEX(JOB_ID);
CREATE INDEX IP_IDX_POPULATION_TM_ID ON IP_TM_INDEX(POPULATION_TM_ID);
CREATE INDEX IP_IDX_TOKEN ON IP_TM_INDEX(TOKEN);
