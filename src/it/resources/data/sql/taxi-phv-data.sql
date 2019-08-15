
---- all combination of (wheelchair accessible, active) values for 'BD51SMR' VRM : begin

-- wheelchair accessible active licence for BD51SMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('BD51SMR',
        'TAXI',
        '2019-04-30',
        current_date + integer '7',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-1'),
        'plate-no-11',
        'y',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

-- wheelchair accessible inactive licence for BD51SMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('BD51SMR',
        'TAXI',
        '2018-03-30',
        '2018-08-22',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-1'),
        'plate-no-22',
        'y',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

-- wheelchair inaccessible active licence for BD51SMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('BD51SMR',
        'TAXI',
        '2019-05-31',
        current_date + integer '7',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-3'),
        'plate-no-33',
        'n',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

-- wheelchair inaccessible inactive licence for BD51SMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('BD51SMR',
        'TAXI',
        '2019-05-31',
        '2019-06-23',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-4'),
        'plate-no-44',
        'n',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

---- all combination of (wheelchair accessible, active) values for 'BD51SMR' VRM : end

---- inactive licences for 'AB51PMR' VMR : begin

-- wheelchair accessible inactive taxi licence for AB51PMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('AB51PMR',
        'TAXI',
        '2019-05-31',
        '2019-06-23',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-4'),
        'plate-no-55',
        'y',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

-- wheelchair accessible inactive phv licence for AB51PMR
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('AB51PMR',
        'PHV',
        '2019-04-30',
        '2019-05-23',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-3'),
        'plate-no-56',
        'y',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);

---- inactive licences for 'AB51PMR' VMR : end

---- wheelchair inaccessible active licence for 'CB51QMR' VMR : begin
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('CB51QMR',
        'PHV',
        '2019-05-24',
        current_date + integer '7',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-1'),
        'plate-no-61',
        'n',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);
---- wheelchair inaccessible active licence for 'CB51QMR' VMR : end


----wheelchair inaccessible inactive licence for 'DA51QMR' VMR : begin
INSERT INTO t_md_taxi_phv(vrm,
                          taxi_phv_type,
                          licence_start_date,
                          licence_end_date,
                          licence_authority_id,
                          licence_plate_number,
                          wheelchair_access_flag,
                          uploader_id,
                          insert_timestmp)
VALUES ('DA51QMR',
        'TAXI',
        '2019-05-24',
        '2019-05-26',
        (SELECT t_md_licensing_authority.licence_authority_id
         FROM T_MD_LICENSING_AUTHORITY
         where licence_authority_name = 'la-2'),
        'plate-no-72',
        'y',
        '8023312c-a9e7-11e9-a2a3-2a2ae2dbcce4',
        CURRENT_TIMESTAMP);
---- wheelchair inaccessible inactive licence for 'DA51QMR' VMR : end