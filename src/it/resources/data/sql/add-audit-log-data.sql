CREATE TABLE table_for_audit_test(trigger VARCHAR(128) NOT NULL, job_name VARCHAR(256) NOT NULL, uploader_id UUID NOT NULL, status VARCHAR(128) NOT NULL);

CREATE TRIGGER table_for_audit_test_trigger
AFTER INSERT OR UPDATE OR DELETE ON table_for_audit_test
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();