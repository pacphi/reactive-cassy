# Azure CosmosDB Deployment

This document covers deploying the Reactive Cassandra Example application with Azure CosmosDB using the Cassandra API.

## Overview

Azure CosmosDB provides a globally distributed, multi-model database service that supports the Cassandra API. This allows you to use existing Cassandra tools and drivers while benefiting from Azure's managed infrastructure.

## Prerequisites

### Required Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **Azure CLI** | 2.73.0+ | Azure resource management |
| **Azure Account** | Active subscription | Cloud resources |
| **cqlsh** | Latest | Cassandra command-line interface |

### Installation

#### Azure CLI

```bash
# macOS (using Homebrew)
brew install azure-cli

# Windows (using Chocolatey)
choco install azure-cli

# Linux (using curl)
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
```

#### cqlsh on macOS

```bash
# Install via Homebrew
brew install cassandra

# This provides cqlsh without running Cassandra locally
```

## Azure Setup

### 1. Authentication

```bash
# Login to Azure
az login

# Set your subscription (replace with your subscription ID)
az account set --subscription "90c2a5c6-0aef-963d-b153-f44d21402d98"

# Verify current subscription
az account show --output table
```

### 2. Resource Group Creation

```bash
# Create resource group
az group create \
  --name spring-data-experiments \
  --location westus2

# Verify creation
az group show --name spring-data-experiments --output table
```

## CosmosDB Instance Setup

### 1. Create CosmosDB Account

#### Using Azure Portal

1. Navigate to [Azure Portal](https://portal.azure.com)
2. Click **"Create a resource"**
3. Search for **"Azure Cosmos DB"**
4. Select **"Cassandra"** as the API
5. Configure the instance:
   - **Account Name**: `reactive-cassy-cosmos`
   - **Resource Group**: `spring-data-experiments`
   - **Location**: `West US 2`
   - **Capacity Mode**: `Provisioned throughput`
   - **Apply Free Tier Discount**: `Yes` (if available)

#### Using Azure CLI

```bash
# Create CosmosDB account with Cassandra API
az cosmosdb create \
  --name reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --locations regionName=westus2 \
  --kind GlobalDocumentDB \
  --capabilities EnableCassandra \
  --default-consistency-level Session \
  --enable-free-tier true

# Verify creation
az cosmosdb show \
  --name reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --output table
```

### 2. Get Connection Details

#### Using Azure Portal

1. Navigate to your CosmosDB instance
2. Go to **Settings → Connection String**
3. Note the following values:
   - **Contact Point**: `reactive-cassy-cosmos.cassandra.cosmosdb.azure.com`
   - **Port**: `10350`
   - **Username**: `reactive-cassy-cosmos`
   - **Primary Password**: `[generated password]`

#### Using Azure CLI

```bash
# Get connection strings
az cosmosdb keys list \
  --name reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --type connection-strings

# Get primary key only
az cosmosdb keys list \
  --name reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --type keys \
  --query "primaryMasterKey" \
  --output tsv
```

## Database Setup

### 1. Connect with cqlsh

#### Environment Variables

```bash
# Set SSL configuration for cqlsh
export SSL_VERSION=TLSv1_2
export SSL_VALIDATE=false

# Connect to CosmosDB (replace with your values)
cqlsh reactive-cassy-cosmos.cassandra.cosmosdb.azure.com 10350 \
  -u reactive-cassy-cosmos \
  -p "your-primary-password-here" \
  --ssl
```

#### Connection String Example

```bash
cqlsh cassy.cassandra.cosmosdb.azure.com 10350 \
  -u cassy \
  -p XJuTjJZLHaAVcAqrVYH4aVu6lhfDk9vOMJ9ePHosv3p4Tpa19N0ZBydmpDtBwpnaaMQeGLzBvKCD7ObEjGXgTU== \
  --ssl
```

### 2. Create Keyspace and Tables

#### Keyspace Creation

```cql
-- Note: CosmosDB uses different replication settings
CREATE KEYSPACE IF NOT EXISTS customers
WITH replication = {
  'class': 'SimpleStrategy',
  'replication_factor': '3'
};

USE customers;
```

#### Table Creation

```cql
-- Create customer table
CREATE TABLE customer (
  id TimeUUID PRIMARY KEY,
  firstname text,
  lastname text
);

-- Note: CosmosDB may not support CREATE INDEX as of some versions
-- Check current limitations in Azure documentation
```

### 3. CosmosDB Limitations

#### Secondary Indexes

As of certain versions, Azure CosmosDB Cassandra API does not support `CREATE INDEX`. Check the [current limitations](https://docs.microsoft.com/en-us/azure/cosmos-db/cassandra-support) in Azure documentation.

**Workaround Options:**

1. Use ALLOW FILTERING in queries (performance impact)
2. Design table structure to avoid secondary indexes
3. Use composite primary keys

#### Query Modifications for CosmosDB

```java
// Repository method that works with CosmosDB limitations
@Repository
public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {

    // This may require ALLOW FILTERING
    @AllowFiltering
    Flux<Customer> findByFirstName(String firstName);

    @AllowFiltering
    Flux<Customer> findByLastName(String lastName);

    // More efficient: use primary key or partition key
    Mono<Customer> findById(UUID id);
}
```

## Application Configuration

### 1. Azure Profile Configuration

Create an Azure-specific profile in `application.yml`:

```yaml
---
spring:
  config:
    activate:
      on-profile: azure
  cassandra:
    ssl:
      enabled: true
    contact-points: ${AZURE_COSMOS_CONTACT_POINT}
    port: ${AZURE_COSMOS_PORT:10350}
    username: ${AZURE_COSMOS_USERNAME}
    password: ${AZURE_COSMOS_PASSWORD}
    connection:
      init-query-timeout: PT30S
      connect-timeout: PT30S
    request:
      timeout: PT30S
    session-name: reactive-cassy-azure
    local-datacenter: datacenter1
```

### 2. Environment Variables

#### Local Development

```bash
# Set environment variables
export AZURE_COSMOS_CONTACT_POINT=reactive-cassy-cosmos.cassandra.cosmosdb.azure.com
export AZURE_COSMOS_PORT=10350
export AZURE_COSMOS_USERNAME=reactive-cassy-cosmos
export AZURE_COSMOS_PASSWORD=your-primary-password-here

# Run application
gradle bootRun -Dspring.profiles.active=azure
```

#### Docker Deployment

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=azure \
  -e AZURE_COSMOS_CONTACT_POINT=reactive-cassy-cosmos.cassandra.cosmosdb.azure.com \
  -e AZURE_COSMOS_PORT=10350 \
  -e AZURE_COSMOS_USERNAME=reactive-cassy-cosmos \
  -e AZURE_COSMOS_PASSWORD=your-primary-password-here \
  reactive-cassy
```

### 3. Secrets Management

#### Azure Key Vault Integration

```yaml
# application-azure.yml
spring:
  cloud:
    azure:
      keyvault:
        secret:
          endpoint: https://your-keyvault.vault.azure.net/
  cassandra:
    password: ${cosmos-password}  # Retrieved from Key Vault
```

#### Using Azure Service Principal

```bash
# Create service principal
az ad sp create-for-rbac --name reactive-cassy-sp

# Assign Key Vault permissions
az keyvault set-policy \
  --name your-keyvault \
  --spn <service-principal-id> \
  --secret-permissions get list
```

## Deployment to Azure

### 1. Azure Container Instances

#### Create Container Instance

```bash
# Build and push Docker image to Azure Container Registry
az acr create --resource-group spring-data-experiments --name reactivecassy --sku Basic
az acr login --name reactivecassy
docker tag reactive-cassy reactivecassy.azurecr.io/reactive-cassy:latest
docker push reactivecassy.azurecr.io/reactive-cassy:latest

# Deploy to Container Instances
az container create \
  --resource-group spring-data-experiments \
  --name reactive-cassy-app \
  --image reactivecassy.azurecr.io/reactive-cassy:latest \
  --dns-name-label reactive-cassy \
  --ports 8080 \
  --environment-variables \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_COSMOS_CONTACT_POINT=reactive-cassy-cosmos.cassandra.cosmosdb.azure.com \
    AZURE_COSMOS_PORT=10350 \
    AZURE_COSMOS_USERNAME=reactive-cassy-cosmos \
  --secure-environment-variables \
    AZURE_COSMOS_PASSWORD=your-primary-password-here
```

### 2. Azure App Service

#### Deploy with Azure CLI

```bash
# Create App Service plan
az appservice plan create \
  --name reactive-cassy-plan \
  --resource-group spring-data-experiments \
  --sku B1 \
  --is-linux

# Create web app
az webapp create \
  --resource-group spring-data-experiments \
  --plan reactive-cassy-plan \
  --name reactive-cassy-app \
  --deployment-container-image-name reactivecassy.azurecr.io/reactive-cassy:latest

# Configure app settings
az webapp config appsettings set \
  --resource-group spring-data-experiments \
  --name reactive-cassy-app \
  --settings \
    SPRING_PROFILES_ACTIVE=azure \
    AZURE_COSMOS_CONTACT_POINT=reactive-cassy-cosmos.cassandra.cosmosdb.azure.com \
    AZURE_COSMOS_PORT=10350 \
    AZURE_COSMOS_USERNAME=reactive-cassy-cosmos \
    AZURE_COSMOS_PASSWORD=your-primary-password-here
```

### 3. Azure Kubernetes Service (AKS)

#### Create AKS Cluster

```bash
# Create AKS cluster
az aks create \
  --resource-group spring-data-experiments \
  --name reactive-cassy-cluster \
  --node-count 2 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials \
  --resource-group spring-data-experiments \
  --name reactive-cassy-cluster
```

#### Kubernetes Deployment

```yaml
# k8s-azure-deployment.yml
apiVersion: v1
kind: Secret
metadata:
  name: cosmos-secrets
type: Opaque
stringData:
  password: "your-primary-password-here"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reactive-cassy
spec:
  replicas: 3
  selector:
    matchLabels:
      app: reactive-cassy
  template:
    metadata:
      labels:
        app: reactive-cassy
    spec:
      containers:
      - name: reactive-cassy
        image: reactivecassy.azurecr.io/reactive-cassy:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "azure"
        - name: AZURE_COSMOS_CONTACT_POINT
          value: "reactive-cassy-cosmos.cassandra.cosmosdb.azure.com"
        - name: AZURE_COSMOS_PORT
          value: "10350"
        - name: AZURE_COSMOS_USERNAME
          value: "reactive-cassy-cosmos"
        - name: AZURE_COSMOS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: cosmos-secrets
              key: password
```

## Monitoring and Management

### 1. Azure Monitor Integration

#### Application Insights

```yaml
# application-azure.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      azure-monitor:
        instrumentation-key: ${APPINSIGHTS_INSTRUMENTATIONKEY}
```

### 2. CosmosDB Metrics

#### Monitor Through Azure Portal

- Request Units (RU) consumption
- Storage usage
- Latency metrics
- Availability statistics

#### CLI Monitoring

```bash
# Get CosmosDB metrics
az monitor metrics list \
  --resource reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --resource-type Microsoft.DocumentDB/databaseAccounts \
  --metric TotalRequestUnits
```

## Cost Optimization

### 1. Request Units (RU) Management

#### Autoscale Configuration

```bash
# Enable autoscale for the database
az cosmosdb cassandra keyspace throughput update \
  --account-name reactive-cassy-cosmos \
  --resource-group spring-data-experiments \
  --name customers \
  --max-throughput 4000
```

### 2. Connection Optimization

#### Connection Pooling

```yaml
# Optimize for CosmosDB
spring:
  cassandra:
    connection:
      pool:
        local:
          size: 4
        remote:
          size: 2
    request:
      timeout: PT10S
      consistency: LOCAL_QUORUM
```

## Troubleshooting

### Common Issues

#### SSL/TLS Connection Problems

```bash
# Verify SSL configuration
openssl s_client -connect reactive-cassy-cosmos.cassandra.cosmosdb.azure.com:10350

# Check cqlsh SSL settings
export SSL_VERSION=TLSv1_2
export SSL_VALIDATE=false
```

#### Request Units Exhaustion

```
Error: Request rate is large. ActivityId: <id>
```

**Solutions:**

- Increase provisioned throughput
- Enable autoscale
- Optimize queries to reduce RU consumption
- Implement retry logic with exponential backoff

#### Authentication Failures

```text
AuthenticationException: Failed to authenticate
```

**Solutions:**

- Verify username and password
- Check connection string format
- Ensure SSL is enabled
- Regenerate access keys if needed

### Performance Tuning

#### Query Optimization

```java
// Avoid ALLOW FILTERING when possible
@Query("SELECT * FROM customer WHERE id = ?0")
Mono<Customer> findByIdOptimized(UUID id);

// Use pagination for large result sets
@Query("SELECT * FROM customer WHERE token(id) > token(?0) LIMIT ?1")
Flux<Customer> findWithPagination(UUID lastId, int limit);
```

#### Connection Tuning

```yaml
spring:
  cassandra:
    connection:
      connect-timeout: PT30S
      init-query-timeout: PT30S
    request:
      timeout: PT30S
      page-size: 5000
```

## Best Practices

### 1. Security

- Use Azure Key Vault for secret management
- Enable SSL/TLS for all connections
- Implement proper authentication and authorization
- Regularly rotate access keys

### 2. Performance

- Design partition keys carefully
- Avoid secondary indexes when possible
- Use appropriate consistency levels
- Monitor RU consumption

### 3. Cost Management

- Use autoscale for variable workloads
- Monitor and optimize RU usage
- Consider reserved capacity for predictable workloads
- Implement proper data lifecycle management

### 4. Reliability

- Implement retry logic for transient failures
- Use circuit breaker patterns
- Monitor application and database health
- Set up proper alerting and notifications
