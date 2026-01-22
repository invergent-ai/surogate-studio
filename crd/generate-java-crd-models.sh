TRAEFIK_MANIFEST_FILE="$(pwd)"/traefik.yml
docker run \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$TRAEFIK_MANIFEST_FILE":"$TRAEFIK_MANIFEST_FILE" \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  statemesh/generate-crd:latest \
  /generate.sh \
  -u $TRAEFIK_MANIFEST_FILE \
  -n io.traefik \
  -p net.statemesh.k8s.crd.traefik \
  -o "$(pwd)"

TEKTON_MANIFEST_FILE="$(pwd)"/tekton-crd.yml
docker run \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$TEKTON_MANIFEST_FILE":"$TEKTON_MANIFEST_FILE" \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  statemesh/generate-crd:latest \
  /generate.sh \
  -u $TEKTON_MANIFEST_FILE \
  -n dev.tekton \
  -p net.statemesh.k8s.crd.tekton \
  -o "$(pwd)"

RAYCLUSTER_MANIFEST_FILE="$(pwd)"/rayclusters.yaml
docker run \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$RAYCLUSTER_MANIFEST_FILE":"$RAYCLUSTER_MANIFEST_FILE" \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  statemesh/generate-crd:latest \
  /generate.sh \
  -u $RAYCLUSTER_MANIFEST_FILE \
  -n io.ray \
  -p net.statemesh.k8s.crd.raycluster \
  -o "$(pwd)"

RAYSERVICE_MANIFEST_FILE="$(pwd)"/rayservices.yaml
docker run \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$RAYSERVICE_MANIFEST_FILE":"$RAYSERVICE_MANIFEST_FILE" \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  statemesh/generate-crd:latest \
  /generate.sh \
  -u $RAYSERVICE_MANIFEST_FILE \
  -n io.ray \
  -p net.statemesh.k8s.crd.rayservice \
  -o "$(pwd)"

RAYJOB_MANIFEST_FILE="$(pwd)"/rayjobs.yaml
docker run \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "$RAYJOB_MANIFEST_FILE":"$RAYJOB_MANIFEST_FILE" \
  -v "$(pwd)":"$(pwd)" \
  -ti \
  --network host \
  statemesh/generate-crd:latest \
  /generate.sh \
  -u $RAYJOB_MANIFEST_FILE \
  -n io.ray \
  -p net.statemesh.k8s.crd.rayjob \
  -o "$(pwd)"

sudo chown -R $USER:$USER "$(pwd)"/src/main/java/net/statemesh/k8s/crd
sudo mv "$(pwd)"/src/main/java/net/statemesh/k8s/crd/* "$(pwd)"/../src/main/java/net/statemesh/k8s/crd/
