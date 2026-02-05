package net.statemesh.config;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

    public static final String SYSTEM = "system";
    public static final String DEFAULT_LANGUAGE = "en";

    public static final String STATE_MESH_ORGANIZATION = "c5f14f05-b2fe-4588-b763-02a8dfa1afda";

    public static final String RATE_LIMIT_HEADER = "X-Api-Key";
    public static final String SM_ID_HEADER = "SM_ID";
    public static final String OPENCOST_AUTH_HEADER = "SM_OPENCOST_TOKEN";
    public static final String RATE_LIMIT_SECURE_END = "55c8c7bcda518d2d12872dedb9c809d7c781e4022ec43e12e592f94ec527248adeb04265e251ebd626bbb722cdc0e0b92532c700eb61cfec0de846b14df70dee953523d72548f0187795e945b69938e86bc46ba0bfe27a567e4bda93b6032512f931221555a2b04ddc56561b0c9c84739a7c214bb67413d3828bd31a5b7a4cffa096ce83ddab8196f042829d51be72224225ee41915bafb48434f8f089f6178a";
    public static final String NODE_BASE_NAME = "Node";
    public static final String DEFAULT_PROJECT_NAME = "My first project";
    public static final String DEFAULT_PROJECT_DESCRIPTION = "This is my first project on StateMesh";
    public static final Integer DEFAULT_FUTURE_TIMEOUT = 60;

    public static final String APP_ERROR_TECHNICAL_KEY = "app.error.technical";
    public static final String DOCKER_HUB_REGISTRY_NAME = "registry.hub.docker.com";
    public static final ZoneId DEFAULT_CLOUD_ZONE_ID = ZoneId.EU_CENTRAL;
    public static final ZoneId DEFAULT_DENSEMAX_ZONE_ID = ZoneId.DENSEMAX;

    public static final String CLI_SESSION_PASSWORD = "smcliPass2025@@";

    public static final String PAYMENT_METHOD_CARD = "card";

    public static final String TEMP_UPLOAD_PATH = "/tmp";

    // one minute
    public static final long ONE_MINUTE = 60;
    // one day in seconds
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    // one day in seconds
    public static final long ONE_DAY = 24 * ONE_HOUR;
    // one week in seconds
    public static final long ONE_WEEK = 7 * ONE_DAY;

    public static final String PROFILE_CLOUD = "smcloud";
    public static final String PROFILE_APPLIANCE = "appliance";
    public static final String ADMIN_LOGIN = "admin@admin";

    public static final String APPLIANCE_CLUSTER_CID = "densemax";

    public static final String MODEL_COMPONENT_ROUTER = "Router";
    public static final String MODEL_COMPONENT_WORKER = "Worker";
    public static final String MODEL_COMPONENT_CACHE = "Cache";

    public static final String RAY_JOB_ENV_JOB_ID = "JOB_ID";
    public static final String RAY_JOB_ENV_WORK_DIR = "WORK_DIR";
    public static final String RAY_JOB_ENV_BASE_MODEL = "BASE_MODEL";
    public static final String RAY_JOB_ENV_LAKECTL_SERVER_ENDPOINT_URL = "LAKECTL_SERVER_ENDPOINT_URL";
    public static final String RAY_JOB_ENV_LAKECTL_CREDENTIALS_ACCESS_KEY_ID = "LAKECTL_CREDENTIALS_ACCESS_KEY_ID";
    public static final String RAY_JOB_ENV_LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY = "LAKECTL_CREDENTIALS_SECRET_ACCESS_KEY";
    public static final String RAY_JOB_ENV_VLLM_TP = "VLLM_TP";
    public static final String RAY_JOB_ENV_AXOLOTL_CONFIG = "AXOLOTL_CONFIG";
    public static final String TASK_RUN_ENV_SKY_CONFIG = "SKY_CONFIG";
    public static final String TASK_RUN_ENV_USE_AXOLOTL = "USE_AXOLOTL_TRAINING_LIBRARY";
    public static final String TASK_RUN_ENV_SKYPILOT_ENDPOINT = "SKYPILOT_API_SERVER_ENDPOINT";
    public static final String TASK_RUN_ENV_KUBE_CONFIG = "KUBE_CONFIG";

    public static final Boolean USE_AXOLOTL_TRAINING_LIBRARY = true; // Axolotl vs Surogate training lib
    public static final Boolean USE_SKYPILOT_SERVER = false; // Set this to true only if the server has cloud providers preconfigured

    private Constants() {
    }
}
