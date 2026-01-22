import {IContainer} from "./container.model";
import {IBaseEntity} from './base-entity.model';
import {IDatabase} from "./database.model";
import {FirewallLevel} from "./enum/firewall-level.model";
import {PolicyType} from "./enum/policy-type.model";
import {RuleType} from "./enum/rule-type.model";

export interface IFirewallEntry extends IBaseEntity {
  cidr?: string | null;
  level?: FirewallLevel;
  policy?: PolicyType;
  rule?: RuleType;
  container?: Pick<IContainer, 'id'> | null;
  database?: Pick<IDatabase, 'id'> | null;
  containerIndex?: number;
}

export type NewFirewallEntry = Omit<IFirewallEntry, 'id'> & { id: null };
