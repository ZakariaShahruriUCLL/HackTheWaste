// =============================================================================
// Leuven Go — Azure Blob Storage
// =============================================================================
// Provisions the storage layer for trash photos captured via the WhatsApp bot.
//
// Data flow:
//   WhatsApp user → Twilio webhook → Spring Boot backend
//     → download photo from Twilio CDN
//     → upload to `trash-photos` container (this file)
//     → store blob URL on the Report entity (imageRef field)
//     → frontend gallery reads blobs directly via public URL
//
// Future use:
//   `ml-data` container accumulates labelled photos + hotspot metadata
//   for training a cleanliness classifier and predictive heatmap model.
// =============================================================================

@description('Azure region for all resources. Defaults to the resource group location.')
param location string = resourceGroup().location

@description('''
  Globally unique storage account name.
  Rules: 3–24 characters, lowercase letters and digits only.
  Example: "leuvengo2026photos"
''')
@minLength(3)
@maxLength(24)
param storageAccountName string

@description('Deployment environment — used for tagging only.')
@allowed(['dev', 'staging', 'prod'])
param environment string = 'dev'

@description('CORS allowed origins for the photo gallery frontend. Use ["*"] during development.')
param corsAllowedOrigins array = ['*']

// ---------------------------------------------------------------------------
// Storage account
// ---------------------------------------------------------------------------

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: storageAccountName
  location: location
  kind: 'StorageV2'
  sku: {
    // LRS = cheapest; upgrade to GRS/ZRS for production resilience
    name: 'Standard_LRS'
  }
  tags: {
    project: 'leuven-go'
    environment: environment
    purpose: 'trash-photo-storage'
    team: 'ReCode'
  }
  properties: {
    // Photos start hot (active gallery access); lifecycle policy cools them over time
    accessTier: 'Hot'

    // Blob-level public access is enabled so the gallery can serve photos
    // directly via URL without a backend proxy. Set to false and use SAS
    // tokens if you need per-request access control later.
    allowBlobPublicAccess: true

    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true

    encryption: {
      services: {
        blob: {
          enabled: true
          keyType: 'Account'
        }
      }
      keySource: 'Microsoft.Storage'
    }
  }
}

// ---------------------------------------------------------------------------
// Blob service — CORS so the React gallery can fetch photos cross-origin
// ---------------------------------------------------------------------------

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    cors: {
      corsRules: [
        {
          // Tighten allowedOrigins to your frontend domain in production,
          // e.g. ['https://leuvengo.azurewebsites.net']
          allowedOrigins: corsAllowedOrigins
          allowedMethods: ['GET', 'HEAD', 'OPTIONS']
          allowedHeaders: ['*']
          exposedHeaders: ['Content-Length', 'Content-Type', 'ETag']
          maxAgeInSeconds: 3600
        }
      ]
    }
    deleteRetentionPolicy: {
      enabled: true
      days: 7
    }
    containerDeleteRetentionPolicy: {
      enabled: true
      days: 7
    }
  }
}

// ---------------------------------------------------------------------------
// Container: trash-photos
// Public blob access → gallery can embed <img src="https://…blob.core…" />
// Blob path convention: {YYYY-MM-DD}/{pseudoId}/{uuid}.jpg
// ---------------------------------------------------------------------------

resource trashPhotosContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: 'trash-photos'
  properties: {
    // 'Blob' = individual blobs are publicly readable; container listing is not
    publicAccess: 'Blob'
    metadata: {
      purpose: 'whatsapp-trash-reports'
      managedBy: 'leuven-go-backend'
    }
  }
}

// ---------------------------------------------------------------------------
// Container: ml-data  (private — backend writes only)
// Stores labelled exports: photo URL + coordinates + severity + tags.
// Feed this to Azure ML or a Jupyter notebook for the predictive heatmap.
// Format: newline-delimited JSON, one record per photo.
// ---------------------------------------------------------------------------

resource mlDataContainer 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = {
  parent: blobService
  name: 'ml-data'
  properties: {
    publicAccess: 'None'
    metadata: {
      purpose: 'ml-training-data'
      schema: 'photoUrl,lat,lng,severity,tags,reportedAt,facultyCode'
    }
  }
}

// ---------------------------------------------------------------------------
// Lifecycle management
// Keeps storage costs low as the photo backlog grows.
// ---------------------------------------------------------------------------

resource lifecyclePolicy 'Microsoft.Storage/storageAccounts/managementPolicies@2023-05-01' = {
  parent: storageAccount
  name: 'default'
  properties: {
    policy: {
      rules: [
        {
          name: 'trash-photos-tiering'
          enabled: true
          type: 'Lifecycle'
          definition: {
            filters: {
              blobTypes: ['blockBlob']
              prefixMatch: ['trash-photos/']
            }
            actions: {
              baseBlob: {
                // Gallery hits drop off after ~30 days — move to Cool
                tierToCool: {
                  daysAfterModificationGreaterThan: 30
                }
                // Archive after 6 months; still retrievable for ML exports
                tierToArchive: {
                  daysAfterModificationGreaterThan: 180
                }
              }
              snapshot: {
                delete: {
                  daysAfterCreationGreaterThan: 90
                }
              }
            }
          }
        }
        {
          name: 'ml-data-tiering'
          enabled: true
          type: 'Lifecycle'
          definition: {
            filters: {
              blobTypes: ['blockBlob']
              prefixMatch: ['ml-data/']
            }
            actions: {
              baseBlob: {
                // Training data accessed infrequently — Cool from the start
                tierToCool: {
                  daysAfterModificationGreaterThan: 7
                }
              }
            }
          }
        }
      ]
    }
  }
}

// ---------------------------------------------------------------------------
// Outputs
// ---------------------------------------------------------------------------

@description('Storage account name — set AZURE_STORAGE_ACCOUNT_NAME in your app config.')
output storageAccountName string = storageAccount.name

@description('Blob service endpoint — base URL for constructing photo URLs.')
output blobEndpoint string = storageAccount.properties.primaryEndpoints.blob

@description('Full URL of the trash-photos container. Photos live at {this}/{date}/{pseudoId}/{uuid}.jpg')
output trashPhotosContainerUrl string = '${storageAccount.properties.primaryEndpoints.blob}trash-photos'

@description('Full URL of the ml-data container.')
output mlDataContainerUrl string = '${storageAccount.properties.primaryEndpoints.blob}ml-data'

@description('Resource ID — use this to assign a Managed Identity role (Storage Blob Data Contributor) to your backend app service.')
output storageAccountResourceId string = storageAccount.id
