// s3.model.ts
export interface S3Credentials {
  accessKey: string;
  secretKey: string;
  bucketUrl: string;
  region: string;
  applicationId: string;
  volumeName: string;
}

export interface S3ValidationResponse {
  valid: boolean;
  message: string;
}
