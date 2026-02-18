#!/bin/bash

helm repo add kuberay https://ray-project.github.io/kuberay-helm/
helm repo add lakefs https://charts.lakefs.io
helm repo update

install_local_storage() {
  kubectl apply -f ./local-storage/local-storage.yml
}

install_aim() {
  kubectl apply -f ./aim/aim-pvc.yaml
  kubectl apply -f ./aim/aim-deployment.yaml
  kubectl apply -f ./aim/aim-service.yaml
}

install_lakefs() {
  kubectl apply -f ./lakefs/shared-volume.yml
  helm install lakefs lakefs/lakefs -f ./lakefs/values.yml -n default
  kubectl apply -f ./lakefs/s3-service.yml
}

install_tekton() {
  kubectl apply -f https://storage.googleapis.com/tekton-releases/pipeline/latest/release.yaml
}

install_kuberay() {
  helm install kuberay-operator kuberay/kuberay-operator --version 1.4.2 \
  --set image.repository=docker.io/statemesh/kuberay-operator --set image.tag=1.4.2 \
  --set env[1].name=ENABLE_PROBES_INJECTION --set-string env[1].value=false \
  --set fullnameOverride=kuberay-operator \
  --set featureGates[0].name=RayClusterStatusConditions --set featureGates[0].enabled=true
}

install_local_storage
install_aim
install_lakefs
install_tekton
install_kuberay

