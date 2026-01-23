// src/app/features/evaluation/constants/metrics.ts
import { Activity, Gauge, MessageSquare, Sparkles, Zap } from 'lucide-angular';
import {
  EVAL_LATENCY,
  EVAL_TOKEN_SPEED,
  EVAL_THROUGHPUT,
  EVAL_CORRECTNESS,
  EVAL_RELEVANCE,
  EVAL_COHERENCE,
  EVAL_CONV_QUALITY,
  EVAL_CONV_COHERENCE,
  EVAL_CONTEXT_RETENTION,
  EVAL_TURN_ANALYSIS,
} from '../../tooltips';

export const PERFORMANCE_METRICS = [
  {
    img: Zap,
    name: 'Latency',
    type: 'performance',
    tooltip: EVAL_LATENCY,
    hasThreshold: true,
    thresholdLabel: 'Max (ms)',
    thresholdDefault: 8000,
  },
  {
    img: Gauge,
    name: 'Token Generation Speed',
    type: 'performance',
    tooltip: EVAL_TOKEN_SPEED,
    hasThreshold: true,
    thresholdLabel: 'Min tokens/sec',
    thresholdDefault: 15,
  },
  {
    img: Activity,
    name: 'Throughput',
    type: 'performance',
    tooltip: EVAL_THROUGHPUT,
    hasThreshold: true,
    thresholdLabel: 'Min req/sec',
    thresholdDefault: 0.3,
  },
];

export const QUALITY_METRICS = [
  { img: Sparkles, name: 'Correctness', type: 'quality', tooltip: EVAL_CORRECTNESS, tasks: ['All'] },
  { img: Sparkles, name: 'Relevance', type: 'quality', tooltip: EVAL_RELEVANCE, tasks: ['All'] },
  { img: Sparkles, name: 'Coherence', type: 'quality', tooltip: EVAL_COHERENCE, tasks: ['All'] },
];

export const CONVERSATION_METRICS = [
  { img: MessageSquare, name: 'Conversation Quality', type: 'conversation', tooltip: EVAL_CONV_QUALITY },
  {
    img: MessageSquare,
    name: 'Conversation Coherence',
    type: 'conversation',
    tooltip: EVAL_CONV_COHERENCE,
    hasConfig: true,
    configLabel: 'Window Size',
    configTooltip: 'Number of recent turns to check for logical flow (default: 3)',
    configDefault: 3,
  },
  {
    img: MessageSquare,
    name: 'Context Retention',
    type: 'conversation',
    tooltip: EVAL_CONTEXT_RETENTION,
    hasConfig: true,
    configLabel: 'Threshold',
    configTooltip: 'Minimum % of key information the model must retain from earlier turns (0.0-1.0, default: 0.7)',
    configDefault: 0.7,
  },
  { img: MessageSquare, name: 'Turn Analysis', type: 'conversation', tooltip: EVAL_TURN_ANALYSIS },
];
