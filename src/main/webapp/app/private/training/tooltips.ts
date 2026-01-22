// Run Basics
export const BASE_RUN_NAME = `Name of the training job`;
export const BASE_MODEL_REPOSITORY = `Model to start from`;
export const EVAL_MODEL_REPOSITORY = `Model to evaluate`;
export const NEW_MODEL_REPOSITORY = `Repository to create for the trained model`;
export const BASE_NEW_BRANCH = `Branch name to create for the resulting model`;
export const BASE_CHECKPOINT = `Start from a specific checkpoint of the base model`;

// Training Recipe
export const TRAIN_PRECISION = `
<p>Use mixed-precision training to reduce memory usage and speed up computation. BF16 is recommended for modern GPUs (NVIDIA Ampere series like A100 or newer).</p>
`;

// Reward Model
export const REWARD_PRM = `
<p>Process Reward Models (PRMs) are a type of AI model designed to evaluate the quality of each individual step in a thought process, rather than just judging the final outcome. This approach helps train more reliable and transparent AI systems, especially for complex, multi-step reasoning tasks.</p>
`;

// LoRA
export const LORA_RANK = `
<p>Determines the number of trainable parameters, essentially controlling how much new information the new model can learn.</p>
<p><span class="font-bold">Recommended Values:</span> between <code>8</code> and <code>64</code> is generally a good starting point, with lower values like 8 or 16 for small stylistic tweaks and higher values like 32 or 64 for learning more complex concepts.</p>
`;

export const LORA_ALPHA = `
<p>A scaling factor that modulates the magnitude of the changes applied by the trained LoRA weights, acting like a volume knob for the fine-tuning effect.</p>
<p><span class="font-bold">Recommended Values:</span> it's standard practice to set <span class="font-semibold">Alpha equal to the Rank</span> (e.g., Rank=32, Alpha=32); a common alternative is to set Alpha to half the Rank (e.g., Rank=32, Alpha=16) to make the learned effect more subtle.</p>
`;

export const LORA_DROPOUT = `
<p>LoRA Dropout is a regularization technique that randomly ignores a fraction of the trainable LoRA parameters during each training step. This process prevents the model from becoming too reliant on any single learned feature, which helps to reduce overfitting and improve the model's ability to generalize to new, unseen data.</p>
<p>Think of it as randomly covering up parts of your notes while studying for a test; it forces you to understand the underlying concepts more deeply instead of just memorizing specific sentences.</p>
<p>The dropout value is a probability ranging from 0.0 (no dropout) to 1.0 (drop everything, which you'd never do).</p>

<ul>
<li><code>0.0</code>: <span class="font-semibold">(Default and Recommended Starting Point)</span> no dropout is applied. For most use cases, especially with a reasonably sized dataset, this is the best place to start. Only add dropout if you have a clear reason to, such as evidence of overfitting.</li>
<li><code>0.05 - 0.1</code>: <span class="font-semibold"> (Common Range for Regularization):</span> This is a good range to use if you are fine-tuning on a very small dataset or if you notice your model is overfitting (i.e., it performs perfectly on the training data but poorly on new data). A value of 0.1 means 10% of the connections within the LoRA layers are randomly set to zero during each training update.</li>
<li><code>&gt; 0.1</code>: <span class="font-semibold"> (High Dropout):</span> Values above 0.1 are less common for LoRA and should be used with caution. A high dropout rate can sometimes hinder the learning process altogether by discarding too much of the new information the model is trying to learn.</li>
</ul>

<p><span class="font-bold">Rule of Thumb</span>: Start with dropout set to 0.0. If your model shows signs of overfitting, introduce a small amount of dropout like 0.05 and gradually increase it to 0.1 if needed.</p>
`;

export const LORA_TARGET_MODULES = `
<p>The specific layers that you choose to adapt with LoRA's trainable matrices. By selecting these modules, you are deciding precisely which parts of the model will be modified during fine-tuning. The most common targets are the layers involved in the self-attention mechanism, as they are crucial for how the model processes and relates information.</p>
<p>Common core modules:</p>
<ul>
<li><span class="font-bold">q_proj (Query Projection)</span>: This module generates the Query vector. The Query acts like a question from the current token, asking, "What other tokens in this sequence are relevant to me?" Targeting this module allows the model to learn what to ask and how to focus its attention based on the new data.</li>
<li><span class="font-bold">k_proj (Key Projection)</span>: This module generates the Key vector for each token in the sequence. The Key can be thought of as a label or an "advertisement" for its token, saying, "This is the kind of information I contain." The model compares the Query vector to all the Key vectors to find the most relevant tokens. Targeting this module helps the model learn what kind of information each token should signal as important.</li>
<li><span class="font-bold">v_proj (Value Projection)</span>: This module generates the Value vector. While the Query and Key determine which tokens to pay attention to, the Value vector contains the actual information that will be passed on. Once the attention scores are calculated using Queries and Keys, they are used to create a weighted sum of all Value vectors. Targeting this module allows the model to learn what information to pass along from each token.</li>
<li><span class="font-bold">o_proj (Output Projection)</span>: After the attention scores have been applied to the Value vectors, the o_proj (or output) layer combines the results from all attention heads. This module synthesizes the gathered information before passing it to the next layer in the network. Targeting it helps the model learn how to best integrate the contextual information it just gathered.</li>
</ul>

<p>Other common modules</p>
<ul>
<li><span class="font-bold">gate_proj, up_proj, down_proj</span>: These are components of the model's feed-forward or MLP (Multi-Layer Perceptron) blocks. The up_proj and gate_proj typically expand the dimensionality of the data, and the down_proj reduces it back down. Targeting these modules allows the fine-tuning to impact the model's knowledge and reasoning capabilities more directly, rather than just its attention patterns.</li>
</ul>

<p><span class="font-bold">Rule of Thumb:</span> For most fine-tuning tasks, targeting all four attention modules (q_proj, k_proj, v_proj, o_proj) provides a strong and effective baseline. If you need to instill more domain-specific knowledge or modify the model's reasoning, expanding the target modules to include the feed-forward layers can be beneficial, though it will increase the number of trainable parameters.</p>
`;

export const KD_NORMALIZE_TOP_K = `
<p>Whether to normalize student logits during Knowledge Distillation</p>
`;

// Evaluation
export const EVAL_JUDGE_URL = `
<p>Public or Internal endpoint URL of model that will be used as a judge.</p>
`;

export const EVAL_USE_GATEWAY_MODEL = `
<p>Check this if the Judge Model URL is a published model available on the gateway</p>
`;

export const EVAL_JUDGE_API_KEY = `
<p>API Key required to access the Judge model</p>
`;

export const EVAL_TASKS = `
<p>By default, the evaluation will be performed on all available tasks for a particular benchmark, but you can choose specific tasks to cover only specific subject areas.</p>
<p>A task for an LLM benchmark is a challenge or problem is designed to assess an LLM's capabilities on a specific area of focus.</p>
`;

export const EVAL_SHOTS = `
<p>The number of examples to provide to help guide accuracy or behavior</p>
<p>Few-shot learning, also known as in-context learning, is a prompting technique that involves supplying your LLM a few examples as part of the prompt template to help its generation.</p>
`;

// ============================================================================
// ACCURACY BENCHMARK TOOLTIPS
// ============================================================================

export const EVAL_MMLU = `
<p>MMLU (Massive Multitask Language Understanding) is a benchmark for evaluating LLMs through multiple-choice questions. These questions cover 57 subjects such as math, history, law, and ethics.</p>
<p>MMLU covers a broad variety and depth of subjects, and is good at detecting areas where a model may lack understanding in a certain topic.</p>
`;

export const EVAL_HELLASWAG = `
<p>HellaSwag is a benchmark designed to evaluate language models' commonsense reasoning through sentence completion tasks. It provides 10,000 challenges spanning various subject areas.</p>
<p>Hellaswag emphasizes commonsense reasoning and depth of understanding in real-world situations, making it an excellent tool for pinpointing where models might struggle with nuanced or complex contexts.</p>
`;

export const EVAL_BIG_BENCH_HARD = `
<p>The BIG-Bench Hard (BBH) benchmark comprises 23 challenging BIG-Bench tasks where prior language model evaluations have not outperformed the average human rater. BBH evaluates models using both few-shot and chain-of-thought (CoT) prompting techniques.</p>
`;

export const EVAL_DROP = `
<p>DROP (Discrete Reasoning Over Paragraphs) is a benchmark designed to evaluate language models' advanced reasoning capabilities through complex question answering tasks. It encompasses over 9500 intricate challenges that demand numerical manipulations, multi-step reasoning, and the interpretation of text-based data.</p>
<p>DROP challenges models to process textual data, perform numerical reasoning tasks such as addition, subtraction, and counting, and also to comprehend and analyze text to extract or infer answers from paragraphs about NFL and history.</p>
`;

export const EVAL_TRUTHFUL_QA = `
<p>TruthfulQA assesses the accuracy of language models in answering questions truthfully. It includes 817 questions across 38 topics like health, law, finance, and politics. The questions target common misconceptions that some humans would falsely answer due to false belief or misconception.</p>
<p>TruthfulQA consists of multiple modes using the same set of questions. MC1 mode involves selecting one correct answer from 4-5 options, focusing on identifying the singular truth among choices. MC2 (Multi-true) mode, on the other hand, requires identifying multiple correct answers from a set. Both MC1 and MC2 are multiple choice evaluations.</p>
`;

export const EVAL_IFEVAL = `
<p>IFEval (Instruction-Following Evaluation for Large Language Models) is a benchmark for evaluating instruction-following capabilities of language models. It tests various aspects of instruction following including format compliance, constraint adherence, output structure requirements, and specific instruction types.</p>
`;

export const EVAL_GSM_8K = `
<p>The GSM8K benchmark comprises 1,319 grade school math word problems, each crafted by expert human problem writers. These problems involve elementary arithmetic operations (+ − ×÷) and require between 2 to 8 steps to solve. The dataset is designed to evaluate an LLM's ability to perform multi-step mathematical reasoning.</p>
`;

export const EVAL_MATH_QA = `
<p>MathQA is a large-scale benchmark consisting of 37K English multiple-choice math word problems across diverse domains such as probability and geometry. It is designed to assess an LLM's capability for multi-step mathematical reasoning.</p>
`;

export const EVAL_LOGIQA = `
<p>LogiQA is a comprehensive dataset designed to assess an LLM's logical reasoning capabilities, encompassing various types of deductive reasoning, including categorical and disjunctive reasoning. It features 8,678 multiple-choice questions, each paired with a reading passage.</p>
`;

export const EVAL_ARC = `
<p>ARC or AI2 Reasoning Challenge is a dataset used to benchmark language models' reasoning abilities. The benchmark consists of 8,000 multiple-choice questions from science exams for grades 3 to 9. The dataset includes two modes: easy and challenge, with the latter featuring more difficult questions that require advanced reasoning.</p>
`;

export const EVAL_WINNOGRANDE = `
<p>Winogrande is a dataset consisting of 44K binary-choice problems, inspired by the original WinoGrad Schema Challenge (WSC) benchmark for commonsense reasoning. It has been adjusted to enhance both scale and difficulty.</p>
`;

export const EVAL_HUMANEVAL = `
<p>HumanEval evaluates code generation capabilities by testing the model's ability to write Python functions from docstrings. It consists of 164 programming problems with unit tests to measure functional correctness using pass@k metric.</p>
`;

export const EVAL_MBPP = `
<p>MBPP (Mostly Basic Python Problems) is a benchmark of 974 crowd-sourced Python programming problems designed to evaluate code generation. Each problem includes a task description, code solution, and automated test cases.</p>
`;

export const EVAL_PIQA = `
<p>PIQA (Physical Interaction QA) tests physical commonsense reasoning with 16K questions about everyday physical interactions. Models must choose the most sensible solution to physical scenarios.</p>
`;

export const EVAL_SIQA = `
<p>SIQA (Social Interaction QA) evaluates social and emotional intelligence with 38K multiple-choice questions about people's actions and their social implications.</p>
`;

export const EVAL_COMMONSENSEQA = `
<p>CommonsenseQA is a challenging benchmark that requires different types of commonsense knowledge to predict correct answers. It contains 12K questions with complex semantics that often require prior knowledge.</p>
`;

export const EVAL_TRIVIAQA = `
<p>TriviaQA is a large-scale reading comprehension dataset containing over 650K question-answer-evidence triples. Questions were authored by trivia enthusiasts and evidence documents were gathered from Wikipedia and the web.</p>
`;

export const EVAL_RACE = `
<p>RACE (ReAding Comprehension from Examinations) is a large-scale reading comprehension dataset collected from English examinations for middle and high school Chinese students. It contains nearly 28,000 passages and 100,000 questions.</p>
`;

export const EVAL_PUBMEDQA = `
<p>PubMedQA is a biomedical question answering dataset collected from PubMed abstracts. It requires reasoning over biomedical research to answer yes/no/maybe questions about research conclusions.</p>
`;

export const EVAL_SCIQ = `
<p>SciQ is a science question answering dataset containing 13.7K crowdsourced multiple choice science exam questions covering Physics, Chemistry, and Biology.</p>
`;

// ============================================================================
// SECURITY/SAFETY TOOLTIPS
// ============================================================================

export const EVA_PII_LEAKAGE = `
<p>The PII (Personally Identifiable Information) leakage vulnerability evaluates whether your LLM system can resist generating or disclosing sensitive personal information.</p>
<p>Passing this vulnerability implies that your LLM system is able to safeguard user privacy and avoids facilitating malicious activities.</p>
`;

export const EVA_PROMPT_LEAKAGE = `
<p>The prompt leakage vulnerability is designed to test whether an LLM can resist revealing sensitive or internal details defined within its system prompt.</p>
<p>This includes safeguarding secrets and credentials, internal instructions, guard mechanisms, and role-based access permissions embedded in the prompt.</p>
`;

export const EVAL_BIAS = `
<p>The bias vulnerability is designed to test whether an LLM can identify, mitigate, and avoid biases in its responses. More specifically, it focuses on testing whether an LLM can be influenced to generate outputs that perpetuate stereotypes, reinforce discriminatory perspectives, or display unfair treatment based on gender, race, religion, and politics.</p>
`;

export const EVAL_TOXICITY = `
<p>The toxicity vulnerability evaluates whether an LLM can resist generating or assisting in the creation of harmful, offensive, or demeaning content. This includes avoiding direct toxic outputs, such as insults or threats, and rejecting prompts that encourage or guide users in crafting harmful language or behaviors. The goal is to ensure the model actively discourages and prevents toxic interactions.</p>
`;

export const EVAL_ILLEGAL_ACTIVITY = `
<p>The illegal activity vulnerability is designed to test whether an LLM can resist generating responses that facilitate or promote unlawful actions. This includes ensuring that the LLM does not provide methods, plans, or instructions for engaging in illegal activities, such as creating weapons, committing crimes, or exploiting others.</p>
`;

export const EVAL_GRAPHIC_CONTENT = `
<p>The graphic content vulnerability is designed to test whether an LLM can resist generating responses that include explicit, inappropriate, or graphic material. This includes ensuring the system does not produce content that is sexual, graphic, or pornographic in nature, safeguarding against harmful or offensive outputs.</p>
`;

export const EVAL_PERSONAL_SAFETY = `
<p>The personal safety vulnerability is designed to test whether an LLM can resist generating responses that jeopardize the safety and well-being of individuals. This includes ensuring that the model does not facilitate or promote actions such as harassment, self-harm, dangerous challenges, or stalking. These vulnerabilities aim to ensure that the LLM acts responsibly, prioritizing personal safety and discouraging harmful behaviors.</p>
`;

export const EVAL_MISINFORMATION = `
<p>The misinformation vulnerability is designed to test whether an LLM can accurately provide information, verify claims, and avoid generating or amplifying false or misleading content. It evaluates the LLM's ability to resist producing outputs that contain factual inaccuracies, unsupported claims, or misrepresented expertize.</p>
`;

export const EVAL_INTELLECTUAL_PROPERTY = `
<p>The intellectual property vulnerability is designed to test whether an LLM can resist generating responses that infringe upon or misuse intellectual property rights. This includes ensuring the model does not produce content that violates copyrights, trademarks, or patents, or closely imitates proprietary works in a way that risks ethical or legal conflicts.</p>
`;

export const EVAL_COMPETITION = `
<p>The competition vulnerability is designed to test whether an LLM can resist generating responses that disclose sensitive competitive information or influence market perceptions unfairly. This includes avoiding content that discusses competitors, discloses confidential strategies, manipulates market perceptions, or discredits other companies.</p>
`;

// ============================================================================
// PERFORMANCE METRIC TOOLTIPS
// ============================================================================

export const EVAL_LATENCY = `
<p>Response Latency - measures the time from request sent to response received. Lower latency indicates faster response times. Threshold in milliseconds.</p>
`;

export const EVAL_TOKEN_SPEED = `
<p>Token Generation Speed - measures how many tokens the model generates per second. Higher values indicate faster generation. Typical range: 15-40 tokens/sec.</p>
`;

export const EVAL_THROUGHPUT = `
<p>Request Throughput - measures how many requests can be processed per second. Higher values indicate better capacity. Typical API range: 0.1-1 req/sec.</p>
`;

// ============================================================================
// QUALITY METRIC TOOLTIPS
// ============================================================================

export const EVAL_CORRECTNESS = `
<p>Correctness - LLM-as-judge evaluation that assesses whether the model's output correctly and accurately answers the input question.</p>
`;

export const EVAL_RELEVANCE = `
<p>Relevance - LLM-as-judge evaluation that assesses whether the model's output is relevant and on-topic to the input question.</p>
`;

export const EVAL_COHERENCE = `
<p>Coherence - LLM-as-judge evaluation that assesses whether the model's output is well-structured, logically consistent, and easy to follow.</p>
`;

// ============================================================================
// CONVERSATION METRIC TOOLTIPS
// ============================================================================

export const EVAL_CONV_QUALITY = `
<p>Conversation Quality - evaluates overall dialogue quality across all turns, including helpfulness and appropriateness of responses.</p>
`;

export const EVAL_CONV_COHERENCE = `
<p>Conversation Coherence - evaluates the logical flow between consecutive turns. Window size determines how many previous turns to consider.</p>
`;

export const EVAL_CONTEXT_RETENTION = `
<p>Context Retention - evaluates whether the model remembers and correctly uses important information from earlier turns in the conversation.</p>
`;

export const EVAL_TURN_ANALYSIS = `
<p>Turn Analysis - provides per-turn quality scores, analyzing each response individually within a multi-turn conversation.</p>
`;

// ============================================================================
// DATASET TOOLTIPS
// ============================================================================

export const DATASET_REPOSITORY = `
<p>Data Hub repository containing the dataset</p>`;

export const DATASET_REF = `
<p>Branch/Tag/Commit containing the dataset</p>`;

export const DATASET_SPLIT = `
<p>The specific split of the dataset to use (e.g., 'train').</p>`;

export const DATASET_MAX_RECORDS = `
<p>(Optional) Maximum number of records to use from this dataset. Can be a number like <code>256</code> or a percentage like <code>20%</code>.</p>
<p>Leave empty to use the entire dataset.</p>
`;

export const DATASET_FORMAT = `
<p>Each dataset has its own structure, and you must map it to the current task (e.g., pre-training, fine-tuning, alignment, quantization).</p>
<p>Most datasets fall into one of the three formats below:</p>
<ul>
<li><span class="font-bold">Pre-training</span>: A single free-form text field (often <code>text</code> or <code>content</code>) with no prompt template or chat roles; used to build broad linguistic and world knowledge.</li>
<li><span class="font-bold">Instruction Tuning</span>: Fields such as <code>instruction</code> (or <code>prompt</code>), optional <code>input</code>, and a single <code>response</code>/<code>output</code>. Usually single-turn.</li>
<li><span class="font-bold">Conversation</span>: An ordered list of messages; each message has a <code>role</code> (e.g., system, user, assistant) and <code>content</code>. Supports multi-turn dialogue.</li>
</ul>
<p>If none of these fit, select <span class="font-bold">Custom</span> and manually map your fields.</p>
`;

export const DATASET_TEXT_COLUMN = `
<p>The name of the column in your dataset that contains the raw text.</p>
<p><span class="font-bold">content</span> or <span class="font-bold">text</span> are common defaults.</p>
<p>Check your dataset's structure to be sure.</p>`;

export const DATASET_MAX_SEQ_LEN = `
<p>The maximum number of tokens in a single training example. Longer sequences provide more context but require more VRAM.</p>
<p><span class="font-bold">Recommended Value:</span> <code>2048</code>, <code>4096</code> or <code>8192</code> are common values.</p>
<p>Ensure it doesn't exceed the model's maximum context window.</p>`;

export const DATASET_SYSTEM_PROMPT_TYPE = `
<p>The type of system prompt to use:</p>
<ul>
<li><span class="font-bold">None</span>: No system prompt</li>
<li><span class="font-bold">Field</span>: Use a dataset field</li>
<li><span class="font-bold">Fixed</span>: Use a fixed value</li>
</ul>
`;

export const DATASET_SYSTEM_PROMPT_FIELD = `
<p>The name of the column in your dataset that contains the System Prompt</p>
`;

export const DATASET_INSTRUCTION_FIELD = `
<p>The name of the column in your dataset that contains the <code>instruction</code>.</p>
`;

export const DATASET_INPUT_FIELD = `
<p>The name of the column in your dataset that contains the <code>input</code>. Leave empty if not <code>input</code> field is available.</p>
`;

export const DATASET_OUTPUT_FIELD = `
<p>The name of the column in your dataset that contains the <code>output</code>.</p>
`;

export const DATASET_INSTRUCTION_PROMPT_FORMAT = `
<p>Python string template for formatting the prompt.</p>
<p>Available variables:</p>
<ul>
<li><code class="font-bold">{instruction}</code>: value of the instruction field</li>
<li><code class="font-bold">{input}</code>: value of the input field</li>
<li><code class="font-bold">{output}</code>: value of the output field</li>
</ul>
`;

export const DATASET_INSTRUCTION_PROMPT_FORMAT_NO_INPUT = `
<p>Python string template for formatting the prompt when no input is used.</p>
<p>Available variables:</p>
<ul>
<li><code class="font-bold">{instruction}</code>: value of the instruction field</li>
<li><code class="font-bold">{output}</code>: value of the output field</li>
</ul>
`;

export const DATASET_CONVERSATION_FIELD_MESSAGES = `
<p>The name of the column in your dataset that contains the <code>messages</code> field.</p>
`;

export const DATASET_CONVERSATION_FIELD_TOOLS = `
<p>If the chat template supports tools, this should be the name of the column in your dataset that contains the available tools.</p>
`;

export const EVAL_LANGUAGE = `
<p>Select the language in which benchmarks will be evaluated</p>
`;

export const EVAL_QUALITY_DATASET = `
<p>Dataset for quality evaluation. Required format: JSONL with fields:</p>
<ul>
<li><code>input</code> - The prompt/question</li>
<li><code>expected_output</code> - The expected answer</li>
</ul>
`;

export const EVAL_CONVERSATION_DATASET = `
<p>Dataset for conversation evaluation. Required format: JSONL with:</p>
<ul>
<li><code>messages</code> - Array of <code>{role, content}</code> objects</li>
</ul>
`;

export const EVAL_CUSTOM_CRITERIA = `
<p>Optional custom criteria for the LLM judge to evaluate against.</p>
<p>If empty, default criteria for the metric type will be used.</p>
`;

// ============================================================================
// TRAINING TOOLTIPS
// ============================================================================

export const NUM_EPOCHS = `
<p>Number of epochs (training cycles) defines the number of complete passes through the entire dataset.</p>`;

export const MICRO_BATCH_SIZE = `
<p>The number of samples to include in each batch. This is the number of samples sent to each GPU. Batch size per gpu = micro_batch_size * gradient_accumulation_steps.</p>`;

export const GRADIENT_ACCUMULATION_STEPS = `
<p>If greater than 1, backpropagation will be skipped and the gradients will be accumulated for the given number of steps.</p>`;

export const LEARNING_RATE = `
<p>Controls the size of parameter updates (weights) as the model learns from new data, acting like a speed knob: too high, and it might overshoot the optimal solution (instability); too low, and training is slow or gets stuck.</p>`;

export const SEQUENCE_LENGTH = `
<p>The maximum length of an input to train with, this should typically be less than 2048 as most models have a token/context limit of 2048.</p>`;

export const OPTIMIZER = `
<p>The algorithm that adjusts the model's internal parameters (weights and biases) to minimize prediction errors (loss).</p>`;

export const TRAIN_ON_INPUTS = `
<p>Whether to mask out or include the human's prompt from the training labels.</p>`;

export const WEIGHT_DECAY = `
<p>Regularization technique that prevents overfitting by penalizing large weights.</p>`;

export const MAX_GRAD_NORM = `
<p>Maximum gradient norm for gradient clipping. 0.0 disables clipping.</p>`;

export const MAX_STEPS = `
<p>Maximum number of iterations to train for. It precedes number of epochs which means that if both are set, number of epochs will not be guaranteed.</p>`;

export const SAMPLE_PACKING = `
<p>Enable sample packing to fit multiple data samples into a single sequence. Packing reduces the number of samples in the dataset.</p><p>Adjust gradient accumulation steps and learning rate accordingly for packed datasets.</p>`;

export const VALIDATION_SPLIT_RATIO = `
<p>How much of the dataset to set aside as validation if no validation datasets are provided.</p>`;

export const EVAL_STEPS = `
<p>Run evaluation every N optimizer steps.</p>`;

export const LOGGING_STEPS = `
<p>Log (report to Aim) every N optimizer steps.</p>`;

export const LR_SCHEDULER = `
<p>Learning rate schedule function.</p>`;

export const WARMUP_STEPS = `
<p>Number of steps for linear warmup. Overrides warmup ratio if set.</p>`;

export const WARMUP_RATIO = `
<p>Ratio of total training steps used for linear warmup from 0 to learning rate.</p>`;

export const COOLDOWN_STEPS = `
<p>Number of steps for linear cooldown from learning_rate to final_lr_fraction * learning_rate.</p>`;

export const FINAL_LR_FRACTION = `
<p>Final learning rate as a fraction of the initial learning rate.</p>`;

export const GRADIENT_CHECKPOINTING = `
<p>Gradient checkpointing is a memory-saving technique that reduces GPU memory usage by selectively storing only key activations during the forward pass and recomputing the rest during the backpropagation (backward) pass. Increases computation time significantly.</p>`;

export const SKIP_QUANT_FIRST_LAYERS = `
<p>Skip quantization for the first N transformer layers (embedding layers kept in BF16).</p>`;

export const SKIP_QUANT_LAST_LAYERS = `
<p>Skip quantization for the last N transformer layers (lm_head layers kept in BF16).</p>`;

export const DEBUG_TIME_BREAKDOWN = `
<p>Enable detailed training timing breakdown for debugging.</p>`;

export const DEBUG_MEMORY_BREAKDOWN = `
<p>Print detailed memory breakdown after model allocation (useful for QLoRA optimization).</p>`;

export const RECIPE = `
<p>Mixed precision training recipe.</p>`;

export const ZERO_LEVEL = `
<p>ZeRO redundancy optimization level: 1 = sharded optimizer states (default), 2 = sharded gradients + optimizer states, 3 = sharded weights + gradients + optimizer states.</p>`;

export const LORA = `
<p>LoRA (Low-Rank Adaptation) is a technique that efficiently fine-tunes large AI models by freezing the original model and training only a small set of new parameters, drastically reducing the computational cost and memory required for customization.</p>`;

export const QLORA_FP8 = `
<p>Enable FP8 QLoRA mode (base weights quantized to FP8 with per-block scales).</p>`;

export const QLORA_FP4 = `
<p>Enable NVFP4 QLoRA mode (base weights quantized to FP4 E2M1). Requires Blackwell GPU (SM100+).</p>`;

export const QLORA_BNB = `
<p>Enable BitsAndBytes NF4 QLoRA mode (base weights quantized to NF4 with per-block absmax). Works on any CUDA GPU.</p>`;

export const RECOMPUTE_LORA = `
<p>Recompute ln1/ln2 activations during LoRA backward pass instead of storing per-layer.</p>`;

export const MERGE_LORA = `
<p>This permanently bakes your trained customizations (the adapter) into the original base model.</p>`;

export const LORA_MERGE_ITERATIVELY = `
<p>Merge LoRA adapters after training on each dataset, instead of training on all datasets and merging in the end.</p>`;

export const RAY_CLUSTER_SHAPE = `
<p>Distributed GPUs cluster format.</p>`;

export const NUM_NODES = `
<p>The number of different machines with GPUs to run on.</p>`;

export const GPUS_PER_WORKER = `
<p>The number of GPUs on each worker node.</p>`;

export const HEAD_GPUS = `
<p>The number of GPUs on head node.</p>`;

export const USE_HEAD_AS_WORKER = `
<p>Whether to use the head node as a worker also.</p>`;

export const TEST_VLLM_TP = `
<p>Degree of parallelism for the vLLM used for testing.</p>`;
