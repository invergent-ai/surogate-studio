export const rfc1123Name = (name: string) => {
  return name
    .toLowerCase()
    .replace(/[^0-9a-z-]/g, "-")
    .replace(/\s/g, '-')
    .replace("-+", "-")
    .replace("-$", "")
    .replace("^-", "")
    .replace("--", "-")
    .slice(0, 63);
};

export const serviceName = (applicationName: string, port: string) => {
  return rfc1123Name(portName(internalApplicationName(applicationName), port) + "-Service");
};

export const internalApplicationName = (applicationName: string) => {
  return rfc1123Name(applicationName);
};

export const portName = (applicationName: string, portName: string) => {
  return (applicationName.slice(0, 7) + "-" + rfc1123Name(portName)).slice(0, 15);
};

export const containerName = (applicationName: string, imageName: string) => {
  return rfc1123Name(
    applicationName + "-" + (imageName ? rfc1123Name(imageName) : '')
  )
};

export const serviceNameForPort = (appName: string, portName: string, namespace?: string): string | null => {
  if (appName && portName) {
    const svc = serviceName(appName, portName);
    if (namespace) {
      return `${svc}.${namespace}`;
    } else {
      return svc;
    }
  } else {
    return null;
  }
}

export const repoDisplayNameOrId = (repoOrSignal: any): string => {
  const repo = typeof repoOrSignal === 'function'
    ? repoOrSignal()
    : repoOrSignal;

  return repo?.metadata?.displayName ?? repo?.id ?? "";
};

