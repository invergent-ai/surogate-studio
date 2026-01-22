insert into sm_authority(name)
values ('ROLE_ADMIN'),
       ('ROLE_USER'),
       ('ROLE_TRAINING'),
       ('ROLE_EVALUATE'),
       ('ROLE_QUANTIZATION'),
       ('ROLE_DEPLOY_APP'),
       ('ROLE_DEPLOY_MODEL'),
       ('ROLE_DATAHUB');

insert into organization(id, name, type)
values ('c5f14f05-b2fe-4588-b763-02a8dfa1afda', 'Invergent', 'PUBLIC');

insert into sm_user(id, login, password_hash, full_name, activated, lang_key, created_by, last_modified_by)
values ('c16b2eea-bc26-4d8e-a75d-9d609efcc22c',
        'admin@admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', 'Administrator',
        true, 'en', 'system', 'system'),
       ('2e5cb94f-292d-49ea-8a56-45622872fbf4', 'user@user',
        '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K',
        'User', true, 'en', 'system', 'system');

insert into sm_user_authority(user_id, authority_name)
values ('c16b2eea-bc26-4d8e-a75d-9d609efcc22c', 'ROLE_ADMIN'),
       ('c16b2eea-bc26-4d8e-a75d-9d609efcc22c', 'ROLE_USER'),
       ('2e5cb94f-292d-49ea-8a56-45622872fbf4', 'ROLE_USER');

insert into user_x_organization(id, user_id, organization_id)
values ('4cabd638-7da0-4455-8029-7fc781b1be31', 'c16b2eea-bc26-4d8e-a75d-9d609efcc22c',
        'c5f14f05-b2fe-4588-b763-02a8dfa1afda'),
       ('dcf8b7bd-0c94-45cc-87cb-d79900437a0c', '2e5cb94f-292d-49ea-8a56-45622872fbf4',
        'c5f14f05-b2fe-4588-b763-02a8dfa1afda');

insert into zone(id, name, zone_id, vpn_api_key, iperf_ip, organization_id)
values ('63502e7b-3ea1-49a2-960d-d2d5e81878b9', 'DenseMAX', 'densemax', '', '', 'c5f14f05-b2fe-4588-b763-02a8dfa1afda');

insert into system_configuration(id, web_domain)
values ('4a8d59ce-427e-45b2-98aa-a235a8c168eb', '.local');

insert into cluster(id, name, cid, master_ip, public_ip, description, zone_id, prometheus_url,
                    request_vs_limits_coefficient_cpu,
                    request_vs_limits_coefficient_memory)
values ('8a9be459-602a-4912-81f8-28c34196c2d0', 'densemax', 'densemax', '127.0.0.1', '127.0.0.1',
        'DenseMAX Cluster', '63502e7b-3ea1-49a2-960d-d2d5e81878b9',
        'http://127.0.0.1:31001/prometheus', 0.75, 1);

insert into protocol(id, p_code, port, p_value)
values ('bb7804cd-8818-4c48-b7a6-4f61ae3cefac', 'TCP', 0, 0),
       ('4c2ee147-05f0-4936-8874-89cd00b95f6b', 'UDP', 0, 0);

update cluster
set kube_config=''
where id = '8a9be459-602a-4912-81f8-28c34196c2d0';

CREATE
    EXTENSION IF NOT EXISTS "uuid-ossp";
COMMIT;
