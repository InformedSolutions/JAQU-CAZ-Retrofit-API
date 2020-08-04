-- Clean old triggers
DROP TRIGGER IF EXISTS T_VEHICLE_RETROFIT_AUDIT ON public.t_vehicle_retrofit;
DROP TRIGGER IF EXISTS T_MD_REGISTER_JOBS_AUDIT ON public.t_md_register_jobs;

ALTER TABLE audit.logged_actions
    RENAME TO logged_actions_archive;


CREATE TABLE audit.logged_actions
(
    schema_name      text                     NOT NULL,
    TABLE_NAME       text                     NOT NULL,
    user_name        text,
    action_tstamp    TIMESTAMP WITH TIME zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action           TEXT                     NOT NULL CHECK (action IN ('I', 'D', 'U')),
    original_data    jsonb,
    new_data         jsonb,
    query            text,
    modifier_id      varchar(256)
) WITH (fillfactor = 100);

REVOKE ALL ON audit.logged_actions FROM public;

GRANT SELECT ON audit.logged_actions TO public;

CREATE INDEX IF NOT EXISTS logged_actions_schema_table_idx
    ON audit.logged_actions (((schema_name || '.' || TABLE_NAME)::TEXT));

CREATE INDEX IF NOT EXISTS logged_actions_action_tstamp_idx
    ON audit.logged_actions (action_tstamp);

CREATE INDEX IF NOT EXISTS logged_actions_action_idx
    ON audit.logged_actions (action);

--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION audit.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data jsonb;
    v_new_data jsonb;
    modifier_id varchar(256);
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */

    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */


    SELECT ttm.modifier_id into modifier_id FROM audit.transaction_to_modifier as ttm where transaction_id = txid_current();
    IF (TG_OP = 'UPDATE') THEN
        v_old_data := to_jsonb(OLD.*);
        v_new_data := to_jsonb(NEW.*);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query,modifier_id)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query(), modifier_id);
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := to_jsonb(OLD.*);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,original_data,query,modifier_id)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query(), modifier_id);
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := to_jsonb(NEW.*);
        INSERT INTO audit.logged_actions (schema_name,table_name,user_name,action,new_data,query,modifier_id)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query(), modifier_id);
        RETURN NEW;
    ELSE
        RAISE WARNING '[audit.IF_MODIFIED_FUNC] - Other action occurred: %, at %',TG_OP,now();
        RETURN NULL;
    END IF;

EXCEPTION
    WHEN data_exception THEN
        RAISE WARNING '[audit.IF_MODIFIED_FUNC] - UDF ERROR [DATA EXCEPTION] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN unique_violation THEN
        RAISE WARNING '[audit.IF_MODIFIED_FUNC] - UDF ERROR [UNIQUE] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE WARNING '[audit.IF_MODIFIED_FUNC] - UDF ERROR [OTHER] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
END;
$body$
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = pg_catalog, audit;

-- Add new triggers
CREATE TRIGGER T_VEHICLE_RETROFIT_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON T_VEHICLE_RETROFIT
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();

CREATE TRIGGER T_MD_REGISTER_JOBS_AUDIT
AFTER INSERT OR UPDATE OR DELETE ON T_MD_REGISTER_JOBS
FOR EACH ROW EXECUTE PROCEDURE audit.if_modified_func();