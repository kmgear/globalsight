#For GBS-781
ALTER TABLE ms_office_doc_filter ADD COLUMN UNEXTRACTABLE_WORD_PARAGRAPH_STYLES varchar(4000) NOT NULL;
ALTER TABLE ms_office_doc_filter ADD COLUMN UNEXTRACTABLE_WORD_CHARACTER_STYLES varchar(4000) NOT NULL;