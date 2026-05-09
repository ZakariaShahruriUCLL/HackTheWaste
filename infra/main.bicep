// =============================================================================
// Leuven Go — Main orchestration template
// Deploys PostgreSQL + Blob Storage and wires up shared params.
// =============================================================================

@description('Short prefix used to name resources (e.g. "hackthewaste").')
param prefix string

@description('Azure region for all resources.')
@allowed(['polandcentral', 'francecentral', 'swedencentral', 'germanywestcentral', 'switzerlandnorth'])
param location string = 'francecentral'

@description('PostgreSQL administrator password.')
@secure()
param postgresPassword string

@description('Twilio Account SID — stored as an app secret, not deployed to Azure.')
@secure()
param twilioAccountSid string

@description('Twilio Auth Token — stored as an app secret, not deployed to Azure.')
@secure()
param twilioAuthToken string

@description('Gemini API key — stored as an app secret, not deployed to Azure.')
@secure()
param geminiApiKey string

@description('Azure Storage connection string (output of the storage module or pre-existing account).')
@secure()
param azureStorageConnectionString string

// ---------------------------------------------------------------------------
// PostgreSQL module
// ---------------------------------------------------------------------------

module postgres './postgresql.bicep' = {
  name: 'postgres'
  params: {
    location: location
    serverName: '${prefix}-pg'
    adminPassword: postgresPassword
  }
}

// ---------------------------------------------------------------------------
// Blob Storage module
// ---------------------------------------------------------------------------

module storage './storage.bicep' = {
  name: 'storage'
  params: {
    location: location
    storageAccountName: '${prefix}photos'
  }
}

// ---------------------------------------------------------------------------
// Outputs
// ---------------------------------------------------------------------------

output jdbcUrl string = postgres.outputs.jdbcUrl
output blobEndpoint string = storage.outputs.blobEndpoint
output trashPhotosContainerUrl string = storage.outputs.trashPhotosContainerUrl

// Suppress unused-param warnings — these are consumed by the app, not Azure resources.
output twilioAccountSidRef string = twilioAccountSid
output twilioAuthTokenRef string = twilioAuthToken
output geminiApiKeyRef string = geminiApiKey
output azureStorageConnectionStringRef string = azureStorageConnectionString
