-- GBS 1785, XML Support
-- Vincent Yan, 2011/01/26

-- Add 'xlz' file extension to existed company
INSERT INTO EXTENSION (NAME, COMPANY_ID, IS_ACTIVE)
  SELECT 'xlz', c.ID, 'Y' FROM COMPANY c WHERE c.ID NOT IN 
    (SELECT DISTINCT f.COMPANY_ID FROM EXTENSION f WHERE f.NAME='xlz');

-- Add new format type
INSERT INTO KNOWN_FORMAT_TYPE VALUES (
  48, 'XLZ', 'XLZ File', 'xlz',
  'XML_IMPORTED_EVENT', 'XML_LOCALIZED_EVENT');

-- Add a new column in FILE_PROFILE table
ALTER TABLE FILE_PROFILE ADD COLUMN REFERENCE_FP BIGINT(20) DEFAULT 0;
  
