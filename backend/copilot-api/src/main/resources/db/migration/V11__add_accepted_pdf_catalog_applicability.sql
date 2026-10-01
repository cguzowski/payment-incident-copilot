ALTER TABLE knowledge_document_version
    ADD COLUMN applicable_families TEXT[] NOT NULL DEFAULT '{}',
    ADD COLUMN catalog_version VARCHAR(100);

UPDATE knowledge_document_version SET applicable_families = ARRAY[incident_family];

ALTER TABLE knowledge_document_version ADD CONSTRAINT knowledge_applicable_family_bounds
    CHECK (cardinality(applicable_families) BETWEEN 1 AND 7);

-- Legacy explicit ingestion omits this column; preserve single-family metadata.
CREATE FUNCTION set_knowledge_default_family() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF cardinality(NEW.applicable_families) = 0 THEN
        NEW.applicable_families := ARRAY[NEW.incident_family];
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER knowledge_default_family BEFORE INSERT ON knowledge_document_version
    FOR EACH ROW EXECUTE FUNCTION set_knowledge_default_family();
