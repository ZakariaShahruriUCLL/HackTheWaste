using './storage.bicep'

// Storage account name must be globally unique across all of Azure.
// Change this to something unique before deploying.
param storageAccountName = 'leuvengo2026photos'

// Subscription allowed regions: polandcentral | francecentral | swedencentral | germanywestcentral | switzerlandnorth
param location = 'francecentral'

param environment = 'dev'

// Tighten to your real domain in production, e.g.:
// param corsAllowedOrigins = ['https://leuvengo.azurewebsites.net']
param corsAllowedOrigins = ['*']
