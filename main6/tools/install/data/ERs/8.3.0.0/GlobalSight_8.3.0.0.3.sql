# GBS-2726 Rename User

delete from PERMISSIONGROUP_USER where USER_ID='GlobalSightSystem';

CREATE TABLE IF NOT EXISTS USER_ID_USER_NAME
(
  USER_ID VARCHAR(80) PRIMARY KEY,
  USER_NAME VARCHAR(80) NOT NULL
);
