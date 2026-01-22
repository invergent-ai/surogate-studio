# Surogate Studio

## Development

Before you can build this project, you must install and configure the following dependencies on your machine:

1. [Node.js][]: We use Node to run a development web server and build the project.
   Depending on your system, you can install Node either from source or as a pre-packaged bundle.

After installing Node, you should be able to run the following command to install development tools.
You will only need to run this command when dependencies change in [package.json](package.json).

```
npm install
```

We use npm scripts and [Angular CLI][] with [Webpack][] as our build system.

Run the following commands in two separate terminals to create a blissful development experience where your browser
auto-refreshes when files change on your hard drive.

```
./mvnw
npm start
```


## Building for production

### Packaging as jar

To build the final jar and optimize the Surogate application for production, run:

```
./mvnw -Pprod clean package
```

To ensure everything worked, run:

```
java -jar target/*.jar
```


## Others

### CI/CD - Build and push docker image for production

For the first time login to dockerhub with your credentials:
```
docker login registry.densemax.local
```

then

```
npm run docker:push
```

If not already, install the application on a k8s cluster

```
export KUBECONFIG=<kube config path>
cd <PROJECT_ROOT>/src/main/helm

kubectl create namespace surogate
helm install --namespace surogate surogate surogate
```


