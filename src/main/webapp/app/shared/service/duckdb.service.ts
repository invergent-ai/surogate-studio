import { Injectable, inject } from '@angular/core';
import * as duckdb from '@duckdb/duckdb-wasm';
import { BehaviorSubject, Observable } from 'rxjs';

import { LakeFsService } from './lake-fs.service';
import { DuckDBDataProtocol } from '@duckdb/duckdb-wasm';

const assetUrl = (p: string) => new URL(p, document.baseURI).toString();

@Injectable({
  providedIn: 'root',
})
export class DuckDbService {
  private db: duckdb.AsyncDuckDB | null = null;
  private connection: duckdb.AsyncDuckDBConnection | null = null;
  private isInitialized = new BehaviorSubject<boolean>(false);
  private lakeFsService = inject(LakeFsService);
  public isReady$: Observable<boolean> = this.isInitialized.asObservable();

  constructor() {
    this.initializeDb();
  }

  private async initializeDb(): Promise<void> {
    try {
      const MANUAL_BUNDLES: duckdb.DuckDBBundles = {
        mvp: {
          mainModule: assetUrl('assets/duckdb/duckdb-mvp.wasm'),
          mainWorker: assetUrl('assets/duckdb/duckdb-browser-mvp.worker.js'),
        },
        eh: {
          mainModule: assetUrl('assets/duckdb/duckdb-eh.wasm'),
          mainWorker: assetUrl('assets/duckdb/duckdb-browser-eh.worker.js'),
        },
      };

      const bundle = await duckdb.selectBundle(MANUAL_BUNDLES);
      const worker = new Worker(bundle.mainWorker!, { type: 'module' });
      const logger = new duckdb.VoidLogger();
      this.db = new duckdb.AsyncDuckDB(logger, worker);
      await this.db.instantiate(bundle.mainModule!, bundle.pthreadWorker);

      this.isInitialized.next(true);
    } catch (error) {
      console.error('Failed to initialize DuckDB:', error);
      this.isInitialized.next(false);
    }
  }

  async query(sql: string): Promise<any[]> {
    this.connection = await this.db.connect();

    try {
      await this.connection.query('INSTALL httpfs;');
      await this.connection.query('LOAD httpfs;');
      await this.connection.query('INSTALL nanoarrow FROM community;');
      await this.connection.query('LOAD nanoarrow;');
      await this.connection.query(`SET s3_region='us-east-1';`);
      await this.connection.query(`SET s3_access_key_id='densemax/${this.lakeFsService.s3Auth}';`);
      await this.connection.query(`SET s3_secret_access_key='dummy2';`);
      await this.connection.query(`SET s3_endpoint='${this.lakeFsService.s3Endpoint}';`);

      const fileMap = await this.extractFiles(sql);
      const fileNames = Object.getOwnPropertyNames(fileMap);
      await Promise.all(fileNames.map(fileName => this.db.registerFileURL(fileName, fileMap[fileName], DuckDBDataProtocol.S3, true)));

      const result = await this.connection.query(sql);

      await Promise.all(fileNames.map(fileName => this.db.dropFile(fileName)));

      return result.toArray().map(row => row.toJSON());
    } catch (error) {
      console.error('Query error:', error);
      throw error;
    } finally {
      await this.connection.close();
    }
  }

  async insertData(tableName: string, data: any[]): Promise<void> {
    if (!this.connection || !this.db) {
      throw new Error('DuckDB not initialized');
    }
    try {
      await this.db.registerFileText(`${tableName}.json`, JSON.stringify(data));
      await this.connection.query(`
        CREATE TABLE IF NOT EXISTS ${tableName} AS
        SELECT * FROM read_json_auto('${tableName}.json')
      `);
      console.log(`Data inserted into ${tableName} table`);
    } catch (error) {
      console.error('Insert error:', error);
      throw error;
    }
  }

  async createTable(tableName: string, schema: string): Promise<void> {
    if (!this.connection) {
      throw new Error('DuckDB not initialized');
    }
    try {
      const createTableSQL = `CREATE TABLE IF NOT EXISTS ${tableName} (${schema})`;
      await this.connection.query(createTableSQL);
      console.log(`Table ${tableName} created`);
    } catch (error) {
      console.error('Create table error:', error);
      throw error;
    }
  }

  async loadCSV(tableName: string, csvContent: string): Promise<void> {
    if (!this.connection || !this.db) {
      throw new Error('DuckDB not initialized');
    }
    try {
      await this.db.registerFileText(`${tableName}.csv`, csvContent);
      await this.connection.query(`
        CREATE TABLE IF NOT EXISTS ${tableName} AS
        SELECT * FROM read_csv_auto('${tableName}.csv')
      `);
      console.log(`CSV loaded into ${tableName} table`);
    } catch (error) {
      console.error('CSV load error:', error);
      throw error;
    }
  }

  async getTables(): Promise<string[]> {
    if (!this.connection) {
      throw new Error('DuckDB not initialized');
    }
    try {
      const result = await this.connection.query(`
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'main'
      `);
      return result.toArray().map(row => row.toJSON().table_name);
    } catch (error) {
      console.error('Get tables error:', error);
      throw error;
    }
  }

  async dropTable(tableName: string): Promise<void> {
    if (!this.connection) {
      throw new Error('DuckDB not initialized');
    }
    try {
      await this.connection.query(`DROP TABLE IF EXISTS ${tableName}`);
      console.log(`Table ${tableName} dropped`);
    } catch (error) {
      console.error('Drop table error:', error);
      throw error;
    }
  }

  async close(): Promise<void> {
    if (this.connection) {
      await this.connection.close();
    }
    if (this.db) {
      await this.db.terminate();
    }
    this.isInitialized.next(false);
  }

  async extractFiles(sql: string): Promise<{ [name: string]: string }> {
    const DUCKDB_STRING_CONSTANT = 2;
    const LAKEFS_URI_PATTERN = /^(['"]?)(lakefs:\/\/(.*))(['"])\s*$/;

    const tokenized = await this.connection.bindings.tokenize(sql);
    const r = Math.random();
    let prev = 0;
    const fileMap: { [name: string]: string } = {};
    tokenized.offsets.forEach((offset, i) => {
      let currentToken = sql.length;
      if (i < tokenized.offsets.length - 1) {
        currentToken = tokenized.offsets[i + 1];
      }
      const part = sql.substring(prev, currentToken);
      prev = currentToken;
      if (tokenized.types[i] === DUCKDB_STRING_CONSTANT) {
        const matches = part.match(LAKEFS_URI_PATTERN);
        if (matches !== null) {
          fileMap[matches[2]] = `s3://${matches[3]}?r=${r}`;
        }
      }
    });
    return fileMap;
  }
}
