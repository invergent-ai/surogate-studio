insert into sm_authority(name)
values ('ROLE_ADMIN'),
       ('ROLE_USER');

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

CREATE
    EXTENSION IF NOT EXISTS "uuid-ossp";
COMMIT;



INSERT INTO public.app_template (id, category, description, icon, long_description, name, template, zorder, provider_id,
                                 hashtags)
VALUES ('26b3e4bf-ca7e-43a3-b6dd-a5edd204e698',
        'Models',
        'Compact 0.6B parameter model with AWQ quantization and CPU-optimized deployment. Features 2K context, lightweight architecture, and efficient resource usage ideal for edge computing and development environments.',
        'https://sm-console-assets.s3.eu-central-1.amazonaws.com/model4.webp',
        '### Overview
Qwen3-0.6B is a compact and efficient language model deployment featuring 600 million parameters with AWQ quantization, designed for lightweight applications and resource-constrained environments.

### Key Specifications
- **Model**: Qwen/Qwen3-0.6B
- **Parameters**: 600 million
- **Context Length**: 2,048 tokens
- **Architecture**: Qwen3ForCausalLM
- **Hidden Size**: 4,096 dimensions
- **Layers**: 36 hidden layers

### Lightweight Architecture
**3-Container Setup:**
- **Router**: Minimal orchestration (1 CPU, 256-500MB RAM)
- **Worker**: CPU-optimized processing (1-5 CPU, 256MB-4GB RAM)
- **Cache**: Basic caching system (1-4 CPU, 256MB-2GB RAM)

### Efficient Features
- **AWQ Quantization**: 4-bit weights with zero-point optimization
- **Group Size**: 128 for optimal compression-quality balance
- **Attention Heads**: 32 attention heads with 8 key-value heads
- **Head Dimension**: 128 for efficient computation
- **Vocabulary**: 151,936 tokens

### Performance Features
- **L1 Cache**: 1GB lightweight caching
- **Memory Footprint**: ~2GB total model size
- **CPU Optimized**: No GPU requirements
- **Development Mode**: Enhanced debugging capabilities
- **Marlin Optimization**: Atomic operations for efficiency

### Use Cases
- Edge computing applications
- Development and prototyping
- Resource-constrained environments
- Educational and learning projects
- Lightweight chatbot applications
- Local inference deployments

### Benefits
- Minimal resource requirements
- Fast inference on CPU
- Efficient memory usage
- Development-friendly configuration
- OpenAI-compatible API interface',
        'Qwen3-0.6B',
        '{
  "name": "qwen3-06b",
  "fromTemplate": true,
  "type": "UI",
  "mode": "MODEL",
  "status": "CREATED",
  "workloadType": "DEPLOYMENT",
  "replicas": 1,
  "updateStrategy": "ROLLING",
  "schedulingRule": "DECENTRALIZED",
  "extraConfig": "{\n  \"modelName\": \"Qwen3-0.6B/main\",\n  \"hfModelName\": \"Qwen/Qwen3-0.6B\",\n  \"maxContextSize\": 2048,\n  \"l1Cache\": true,\n  \"l1CacheSize\": 1,\n  \"gpuMemory\": 4096,\n  \"hfTotalSafetensors\": 2174235648,\n  \"hfConfig\": {\n    \"architectures\": [\n      \"Qwen3ForCausalLM\"\n    ],\n    \"attention_bias\": false,\n    \"attention_dropout\": 0,\n    \"bos_token_id\": 151643,\n    \"eos_token_id\": 151645,\n    \"head_dim\": 128,\n    \"hidden_act\": \"silu\",\n    \"hidden_size\": 4096,\n    \"initializer_range\": 0.02,\n    \"intermediate_size\": 12288,\n    \"max_position_embeddings\": 40960,\n    \"max_window_layers\": 36,\n    \"model_type\": \"qwen3\",\n    \"num_attention_heads\": 32,\n    \"num_hidden_layers\": 36,\n    \"num_key_value_heads\": 8,\n    \"quantization_config\": {\n      \"backend\": \"autoawq\",\n      \"bits\": 4,\n      \"do_fuse\": false,\n      \"exllama_config\": null,\n      \"fuse_max_seq_len\": null,\n      \"group_size\": 128,\n      \"modules_to_fuse\": null,\n      \"modules_to_not_convert\": null,\n      \"quant_method\": \"awq\",\n      \"version\": \"gemm\",\n      \"zero_point\": true\n    },\n    \"rms_norm_eps\": 0.000001,\n    \"rope_scaling\": null,\n    \"rope_theta\": 1000000,\n    \"sliding_window\": null,\n    \"tie_word_embeddings\": false,\n    \"torch_dtype\": \"float16\",\n    \"transformers_version\": \"4.51.3\",\n    \"use_cache\": true,\n    \"use_sliding_window\": false,\n    \"vocab_size\": 151936\n  }\n}",
  "containers": [
    {
      "displayName": "Router",
      "imageName": "lmcache/lmstack-router",
      "imageTag": "latest",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 2,
      "cpuLimit": 2,
      "memRequest": "1024",
      "memLimit": "2048",
      "envVars": [
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "INFO"
        }
      ],
      "ports": [
        {
          "name": "router",
          "containerPort": 8000,
          "ingressPort": true,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "cache",
          "containerPort": 9000,
          "servicePort": 9000,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 5,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Worker",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2026-01-14",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 4,
      "gpuLimit": 1,
      "memRequest": "1024",
      "memLimit": "8192",
      "startCommand": "/opt/venv/bin/vllm",
      "startParameters": null,
      "envVars": [
        {
          "key": "VLLM_USE_V1",
          "value": "1"
        },
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_LOGGING_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_MARLIN_USE_ATOMIC_ADD",
          "value": "1"
        },
        {
          "key": "TORCH_CUDA_ARCH_LIST",
          "value": "8.9"
        },
        {
          "key": "VLLM_SERVER_DEV_MODE",
          "value": "1"
        }
      ],
      "volumeMounts": [
        {
          "containerPath": "/models",
          "volume": {
            "name": "hf-cache",
            "type": "HOST_PATH",
            "path": "/models"
          }
        },
        {
          "containerPath": "/root/.cache/vllm",
          "volume": {
            "name": "torch-cache",
            "type": "HOST_PATH",
            "path": "/torch"
          }
        }
      ],
      "ports": [
        {
          "name": "llm",
          "containerPort": 8000,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "zmq",
          "containerPort": 55555,
          "servicePort": 55555,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "ucx",
          "containerPort": 9999,
          "servicePort": 9999,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 300,
          "periodSeconds": 20,
          "timeoutSeconds": 3,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 20,
          "timeoutSeconds": 5,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Cache",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2025-08-27",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 4,
      "memRequest": "256",
      "memLimit": "2048",
      "startCommand": "/opt/venv/bin/lmcache_server",
      "startParameters": null,
      "envVars": [],
      "volumeMounts": [],
      "ports": [
        {
          "name": "cache",
          "containerPort": 9090,
          "servicePort": 81,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ]
    }
  ]
}',
        0,
        null,
        'Qwen3');

COMMIT;

INSERT INTO public.app_template (id, category, description, icon, long_description, name, template, zorder, provider_id, hashtags)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479',
        'Models',
        'Qwen3-VL-30B-A3B multimodal vision-language model with FP8 quantization and thinking capabilities. Features 32K context, 2-GPU deployment, and advanced visual reasoning.',
        'https://sm-console-assets.s3.eu-central-1.amazonaws.com/model4.webp',
        '### Overview
Qwen3-VL-30B-A3B-Thinking is a multimodal vision-language model with 30 billion parameters, FP8 quantization, and enhanced reasoning capabilities.

### Key Specifications
- **Model**: Qwen/Qwen3-VL-30B-A3B-Thinking-FP8
- **Parameters**: 30 billion (3.3B activated)
- **Context Length**: 32,768 tokens
- **Quantization**: FP8
- **GPUs**: 2

### Use Cases
- Visual question answering
- Image analysis and reasoning
- Multimodal chat applications
- Document understanding',
        'Qwen3-VL-30B',
        '{
  "name": "qwen3-vl-30b",
  "fromTemplate": true,
  "type": "UI",
  "mode": "MODEL",
  "status": "CREATED",
  "workloadType": "DEPLOYMENT",
  "replicas": 1,
  "updateStrategy": "ROLLING",
  "schedulingRule": "DECENTRALIZED",
  "extraConfig": "{\n  \"modelName\": \"Qwen3-VL-30B-A3B-Thinking-FP8/main\",\n  \"hfModelName\": \"Qwen/Qwen3-VL-30B-A3B-Thinking-FP8\",\n  \"maxContextSize\": 32768,\n  \"enablePartitioning\": true,\n  \"partitions\": 2,\n  \"l1Cache\": true,\n  \"l1CacheSize\": 4,\n  \"gpuMemory\": 49152,\n  \"hfTotalSafetensors\": 30500000000,\n  \"hfConfig\": {\n    \"architectures\": [\"Qwen3VLMoeForConditionalGeneration\"],\n    \"model_type\": \"qwen3_vl_moe\",\n    \"num_attention_heads\": 32,\n    \"num_key_value_heads\": 4,\n    \"num_hidden_layers\": 48,\n    \"head_dim\": 128,\n    \"hidden_size\": 2048,\n    \"intermediate_size\": 6144,\n    \"vocab_size\": 151936,\n    \"bos_token_id\": 151643,\n    \"eos_token_id\": 151645,\n    \"hidden_act\": \"silu\",\n    \"rms_norm_eps\": 0.000001,\n    \"rope_theta\": 5000000,\n    \"max_position_embeddings\": 262144,\n    \"tie_word_embeddings\": false\n  }\n}",
  "containers": [
    {
      "displayName": "Router",
      "imageName": "lmcache/lmstack-router",
      "imageTag": "latest",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 2,
      "cpuLimit": 2,
      "memRequest": "1024",
      "memLimit": "2048",
      "envVars": [
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "INFO"
        }
      ],
      "ports": [
        {
          "name": "router",
          "containerPort": 8000,
          "ingressPort": true,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "cache",
          "containerPort": 9000,
          "servicePort": 9000,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 5,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Worker",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2026-01-14",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 4,
      "memRequest": "256",
      "memLimit": "65536",
      "gpuLimit": 2,
      "startCommand": "/opt/venv/bin/vllm",
      "startParameters": null,
      "envVars": [
        {
          "key": "VLLM_USE_V1",
          "value": "1"
        },
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_LOGGING_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_MARLIN_USE_ATOMIC_ADD",
          "value": "1"
        },
        {
          "key": "TORCH_CUDA_ARCH_LIST",
          "value": "8.9"
        },
        {
          "key": "VLLM_SERVER_DEV_MODE",
          "value": "1"
        }
      ],
      "volumeMounts": [
        {
          "containerPath": "/models",
          "volume": {
            "name": "hf-cache",
            "type": "HOST_PATH",
            "path": "/models"
          }
        },
        {
          "containerPath": "/root/.cache/vllm",
          "volume": {
            "name": "torch-cache",
            "type": "HOST_PATH",
            "path": "/torch"
          }
        }
      ],
      "ports": [
        {
          "name": "llm",
          "containerPort": 8000,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "zmq",
          "containerPort": 55555,
          "servicePort": 55555,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "ucx",
          "containerPort": 9999,
          "servicePort": 9999,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 300,
          "periodSeconds": 20,
          "timeoutSeconds": 3,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 20,
          "timeoutSeconds": 5,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Cache",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2025-08-27",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 4,
      "memRequest": "256",
      "memLimit": "2048",
      "startCommand": "/opt/venv/bin/lmcache_server",
      "startParameters": null,
      "envVars": [],
      "volumeMounts": [],
      "ports": [
        {
          "name": "cache",
          "containerPort": 9090,
          "servicePort": 81,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ]
    }
  ]
}',
        0,
        null,
        'Qwen3,VL,Vision');

COMMIT;

INSERT INTO public.app_template (id, category, description, icon, long_description, name, template, zorder, provider_id, hashtags)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'Models',
        'Qwen3-8B dense model with 8 billion parameters, 32K native context, and thinking capabilities. Single GPU deployment optimized for RTX 5090.',
        'https://sm-console-assets.s3.eu-central-1.amazonaws.com/model4.webp',
        '### Overview
Qwen3-8B is a dense language model with 8 billion parameters, featuring native thinking capabilities and 32K context length.

### Key Specifications
- **Model**: Qwen/Qwen3-8B
- **Parameters**: 8 billion
- **Context Length**: 32,768 tokens (131K with YaRN)
- **Architecture**: Qwen3ForCausalLM
- **GPUs**: 1

### Use Cases
- Complex reasoning and coding
- Multi-turn dialogue
- Tool calling and agents
- Creative writing and role-playing',
        'Qwen3-8B',
        '{
  "name": "qwen3-8b",
  "fromTemplate": true,
  "type": "UI",
  "mode": "MODEL",
  "status": "CREATED",
  "workloadType": "DEPLOYMENT",
  "replicas": 1,
  "updateStrategy": "ROLLING",
  "schedulingRule": "DECENTRALIZED",
  "extraConfig": "{\n  \"modelName\": \"Qwen3-8B/main\",\n  \"hfModelName\": \"Qwen/Qwen3-8B\",\n  \"maxContextSize\": 32768,\n  \"l1Cache\": true,\n  \"l1CacheSize\": 2,\n  \"gpuMemory\": 32768,\n  \"hfTotalSafetensors\": 8000000000,\n  \"hfConfig\": {\n    \"architectures\": [\n      \"Qwen3ForCausalLM\"\n    ],\n    \"attention_bias\": false,\n    \"attention_dropout\": 0.0,\n    \"bos_token_id\": 151643,\n    \"eos_token_id\": 151645,\n    \"head_dim\": 128,\n    \"hidden_act\": \"silu\",\n    \"hidden_size\": 4096,\n    \"initializer_range\": 0.02,\n    \"intermediate_size\": 12288,\n    \"max_position_embeddings\": 40960,\n    \"max_window_layers\": 36,\n    \"model_type\": \"qwen3\",\n    \"num_attention_heads\": 32,\n    \"num_hidden_layers\": 36,\n    \"num_key_value_heads\": 8,\n    \"rms_norm_eps\": 0.000001,\n    \"rope_scaling\": null,\n    \"rope_theta\": 1000000,\n    \"sliding_window\": null,\n    \"tie_word_embeddings\": false,\n    \"torch_dtype\": \"bfloat16\",\n    \"transformers_version\": \"4.51.0\",\n    \"use_cache\": true,\n    \"use_sliding_window\": false,\n    \"vocab_size\": 151936\n  }\n}",
  "containers": [
    {
      "displayName": "Router",
      "imageName": "lmcache/lmstack-router",
      "imageTag": "latest",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 2,
      "cpuLimit": 2,
      "memRequest": "1024",
      "memLimit": "2048",
      "envVars": [
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "INFO"
        }
      ],
      "ports": [
        {
          "name": "router",
          "containerPort": 8000,
          "ingressPort": true,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "cache",
          "containerPort": 9000,
          "servicePort": 9000,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 5,
          "periodSeconds": 5,
          "failureThreshold": 3,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Worker",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2026-01-14",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 5,
      "gpuLimit": 1,
      "memRequest": "256",
      "memLimit": "16384",
      "startCommand": "/opt/venv/bin/vllm",
      "startParameters": null,
      "envVars": [
        {
          "key": "VLLM_USE_V1",
          "value": "1"
        },
        {
          "key": "LMCACHE_LOG_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_LOGGING_LEVEL",
          "value": "DEBUG"
        },
        {
          "key": "VLLM_MARLIN_USE_ATOMIC_ADD",
          "value": "1"
        },
        {
          "key": "TORCH_CUDA_ARCH_LIST",
          "value": "8.9"
        },
        {
          "key": "VLLM_SERVER_DEV_MODE",
          "value": "1"
        }
      ],
      "volumeMounts": [
        {
          "containerPath": "/models",
          "volume": {
            "name": "hf-cache",
            "type": "HOST_PATH",
            "path": "/models"
          }
        },
        {
          "containerPath": "/root/.cache/vllm",
          "volume": {
            "name": "torch-cache",
            "type": "HOST_PATH",
            "path": "/torch"
          }
        }
      ],
      "ports": [
        {
          "name": "llm",
          "containerPort": 8000,
          "servicePort": 80,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "zmq",
          "containerPort": 55555,
          "servicePort": 55555,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        },
        {
          "name": "ucx",
          "containerPort": 9999,
          "servicePort": 9999,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ],
      "probes": [
        {
          "type": "LIVENESS",
          "initialDelaySeconds": 300,
          "periodSeconds": 20,
          "timeoutSeconds": 3,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        },
        {
          "type": "READINESS",
          "initialDelaySeconds": 30,
          "periodSeconds": 20,
          "timeoutSeconds": 5,
          "successThreshold": 1,
          "failureThreshold": 10,
          "httpPath": "/health",
          "httpPort": 8000
        }
      ]
    },
    {
      "displayName": "Cache",
      "imageName": "lmcache/vllm-openai",
      "imageTag": "nightly-2025-08-27",
      "type": "WORKER",
      "pullImageMode": "PULL",
      "cpuRequest": 1,
      "cpuLimit": 4,
      "memRequest": "256",
      "memLimit": "2048",
      "startCommand": "/opt/venv/bin/lmcache_server",
      "startParameters": null,
      "envVars": [],
      "volumeMounts": [],
      "ports": [
        {
          "name": "cache",
          "containerPort": 9090,
          "servicePort": 81,
          "protocol": {
            "id": "bb7804cd-8818-4c48-b7a6-4f61ae3cefac",
            "code": "TCP"
          }
        }
      ]
    }
  ]
}',
        0,
        null,
        'Qwen3');

COMMIT;
