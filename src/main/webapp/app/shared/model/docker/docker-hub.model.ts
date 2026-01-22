// docker-hub.model.ts
export interface DockerHubImage {
  name: string;
  description: string;
  stars: number;
  official: boolean;
  automated: boolean;
  logo_url: string | null;
  tags?: string[]
}

export interface DockerHubTag {
  name: string;
  last_updated: string;
}

interface DockerHubSearchResponseItem {
  name: string;
  short_description: string;
  star_count: number;
  source: string;
  logo_url: {
    small: string;
    large: string;
  };
  tags?: string[]
}

export interface DockerHubSearchResponse {
  results: DockerHubSearchResponseItem[];
}
export interface SelectedDockerImage extends DockerHubImage {
  tag: string;
}
